package cn.iocoder.gamemodules.util;

import cn.iocoder.gamemodules.entity.*;
import cn.iocoder.gamemodules.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.time.LocalDateTime;
import java.util.*;

public class BattleUtils {

    // 计算系别克制倍数
    public static double calculateElementMultiplier(int attackerElement, int defenderElement) {
        // 1: 火系, 2: 水系, 3: 草系, 4: 光系
        double[][] multiplierMatrix = {
            {1.0, 0.5, 2.0, 1.0}, // 火系攻击
            {2.0, 1.0, 0.5, 1.0}, // 水系攻击
            {0.5, 2.0, 1.0, 2.0}, // 草系攻击（草系克制光系）
            {1.0, 1.0, 0.5, 1.0}  // 光系攻击（光系被草系克制）
        };
        if (attackerElement >= 1 && attackerElement <= 4 && defenderElement >= 1 && defenderElement <= 4) {
            return multiplierMatrix[attackerElement - 1][defenderElement - 1];
        }
        return 1.0;
    }

    // 计算技能伤害
    public static int calculateSkillDamage(Skill skill, int attackerAttack, int defenderDefense, int attackerElement, int defenderElement) {
        int skillPower = skill.getSkillDamage();
        
        // 计算基础伤害
        int baseDamage = (attackerAttack * skillPower) / (attackerAttack + defenderDefense);
        baseDamage = Math.max(baseDamage, 1);
        
        // 计算系别克制
        double multiplier = calculateElementMultiplier(attackerElement, defenderElement);
        
        return (int) (baseDamage * multiplier);
    }

    // 计算普通攻击伤害
    public static int calculateNormalDamage(int attackerAttack, int defenderDefense) {
        int damage = attackerAttack - defenderDefense / 2;
        return Math.max(damage, 1);
    }

    // 获取精灵系别
    public static int getElfElementType(UserElf elf, ElfMapper elfMapper) {
        Elf elfTemplate = elfMapper.selectById(elf.getElfId());
        return elfTemplate != null ? elfTemplate.getElementType() : 0;
    }

    /**
     * 判断玩家是否先手（速度高者先手）
     */
    public static boolean isPlayerFirst(int playerSpeed, int monsterSpeed) {
        return playerSpeed >= monsterSpeed;
    }
    
    /**
     * 执行玩家攻击
     * @return 造成的伤害
     */
    public static int executePlayerAttack(
            String actionType,
            Integer skillId,
            BattleRecordElf userElfRecord,
            UserElf userElf,
            Elf elf,
            Monster monster,
            SkillMapper skillMapper) {
        
        int damage;
        
        if ("attack".equals(actionType)) {
            // 普通攻击为真实伤害，直接返回normalDamage
            damage = userElf.getNormalDamage() != null ? userElf.getNormalDamage() : 1;
            
            System.out.println("[DEBUG] 玩家普通攻击 - 真实伤害:" + damage);
        } else if ("skill".equals(actionType)) {
            // 技能伤害需要公式计算和系别克制
            Skill skill = skillMapper.selectById(skillId);
            if (skill == null) {
                throw new RuntimeException("技能不存在");
            }
            
            // 检查MP是否足够
            if (userElfRecord.getCurrentMp() < skill.getCostMp()) {
                throw new RuntimeException("MP不足，无法使用技能");
            }
            
            // 扣除MP
            userElfRecord.setCurrentMp(userElfRecord.getCurrentMp() - skill.getCostMp());
            
            // 计算基础伤害
            damage = calculateSkillDamage(skill, userElf.getAttack(), monster.getDefense(), 
                    elf.getElementType(), monster.getElementType());
            
            System.out.println("[DEBUG] 玩家技能攻击 - 技能名:" + skill.getSkillName() + 
                              ", MP消耗:" + skill.getCostMp() + 
                              ", 剩余MP:" + userElfRecord.getCurrentMp() +
                              ", 造成伤害:" + damage);
        } else {
            throw new RuntimeException("无效的攻击类型");
        }
        
        return damage;
    }
    
    /**
     * 执行怪物行动（优先使用技能）
     * @return Map包含伤害值和攻击类型
     */
    public static Map<String, Object> executeMonsterAction(BattleRecordElf userElfRecord, UserElf userElf, Elf elf,
                                                           BattleRecordMonster monsterRecord, Monster monster, 
                                                           BattleRecordElfMapper elfMapper, 
                                                           cn.iocoder.gamemodules.mapper.MonsterSkillMapper monsterSkillMapper,
                                                           SkillMapper skillMapper,
                                                           cn.iocoder.gamemodules.mapper.BattleRecordMonsterMapper monsterRecordMapper) {
        int enemyDamage = 0;
        String attackType = "普通攻击";
        
        // 查询怪物的技能
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<cn.iocoder.gamemodules.entity.MonsterSkill> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("monster_id", monster.getId());
        List<cn.iocoder.gamemodules.entity.MonsterSkill> monsterSkills = monsterSkillMapper.selectList(wrapper);
        
        boolean skillUsed = false;
        
        // 如果有技能且MP足够，优先使用技能
        if (monsterSkills != null && !monsterSkills.isEmpty() && monsterRecord.getCurrentMp() > 0) {
            // 随机选择一个技能
            cn.iocoder.gamemodules.entity.MonsterSkill randomSkill = monsterSkills.get((int) (Math.random() * monsterSkills.size()));
            Skill skill = skillMapper.selectById(randomSkill.getSkillId());
            
            if (skill != null && monsterRecord.getCurrentMp() >= skill.getCostMp()) {
                // 使用技能，扣除MP
                monsterRecord.setCurrentMp(monsterRecord.getCurrentMp() - skill.getCostMp());
                monsterRecord.setUpdateTime(LocalDateTime.now());
                
                // 计算技能伤害（包含系别克制）
                int baseDamage = calculateSkillDamage(skill, monster.getAttack(), userElf.getDefense(),
                        monster.getElementType(), elf.getElementType());
                enemyDamage = baseDamage;
                attackType = "技能 " + skill.getSkillName();
                skillUsed = true;
                
                System.out.println("[DEBUG] 怪物使用技能 - 技能名:" + skill.getSkillName() + 
                                  ", MP消耗:" + skill.getCostMp() + 
                                  ", 剩余MP:" + monsterRecord.getCurrentMp() +
                                  ", 造成伤害:" + enemyDamage);
            }
        }
        
        // 如果没有使用技能，则使用普通攻击（真实伤害）
        if (!skillUsed) {
            Integer normalDamage = monster.getNormalDamage();
            enemyDamage = normalDamage != null ? normalDamage : 1;
            attackType = "普通攻击";
            
            System.out.println("[DEBUG] 怪物使用普通攻击 - 真实伤害:" + enemyDamage);
        }
        
        // 保存怪物状态到数据库（包括MP消耗）
        if (monsterRecordMapper != null) {
            monsterRecordMapper.updateById(monsterRecord);
        }
        
        // 扣除玩家HP
        userElfRecord.setCurrentHp(Math.max(0, userElfRecord.getCurrentHp() - enemyDamage));
        
        // 如果HP<=0，设置elfState为2（死亡）
        if (userElfRecord.getCurrentHp() <= 0) {
            userElfRecord.setElfState(2); // 2=死亡
        }
        
        userElfRecord.setUpdateTime(LocalDateTime.now());
        elfMapper.updateById(userElfRecord);  // elfMapper参数类型是BattleRecordElfMapper
        
        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("damage", enemyDamage);
        result.put("attackType", attackType);
        
        return result;
    }
    
    /**
     * 构建战斗日志
     */
    public static List<String> buildBattleLogs(String attackLog, int originalMonsterHp, 
                                               int maxMonsterHp, int currentMonsterHp,
                                               String enemyAttackLog, boolean monsterDead,
                                               boolean playerDead) {
        List<String> logs = new ArrayList<>();
        logs.add(attackLog);
        logs.add(String.format("敌人HP: %d/%d → %d/%d", 
                originalMonsterHp, maxMonsterHp, currentMonsterHp, maxMonsterHp));
        
        if (enemyAttackLog != null) {
            logs.add(enemyAttackLog);
        }
        
        if (monsterDead) {
            logs.add("战斗胜利！");
        } else if (playerDead) {
            logs.add("战斗失败！你的精灵被击败了");
        }
        
        return logs;
    }
    
    /**
     * 构建战斗响应结果
     */
    public static Map<String, Object> buildBattleResult(
            BattleRecord battleRecord,
            BattleRecordElf userElfRecord,
            UserElf userElf,
            Elf elf,
            BattleRecordMonster monsterRecord,
            Monster monster,
            Level level,
            boolean monsterDead,
            List<String> logs) {
        
        Map<String, Object> result = new HashMap<>();
        // status: 0=战斗中, 1=胜利, 2=失败
        int status = battleRecord.getStatus() != null ? battleRecord.getStatus() : 0;
        result.put("status", status);
        result.put("roundLogs", Arrays.asList(createRoundData(battleRecord.getCurrentRound(), logs)));
        
        // 精灵信息（包含切换后的精灵）
        result.put("playerElfId", userElfRecord.getElfId());
        result.put("playerElfHp", userElfRecord.getCurrentHp());
        result.put("playerElfHpMax", userElf.getMaxHp());
        result.put("elfMp", userElfRecord.getCurrentMp());
        result.put("elfMpMax", userElf.getMaxMp());
        
        // 添加精灵名字和系别，方便前端直接使用
        if (elf != null) {
            result.put("elfName", elf.getElfName());
            result.put("elfElementType", elf.getElementType());
        }
        
        // 怪物信息
        result.put("monsterHp", monsterRecord.getCurrentHp());
        result.put("monsterMaxHp", monster.getHp());
        result.put("monsterMp", monsterRecord.getCurrentMp());
        result.put("monsterMaxMp", monster.getMp());
        
        if (monsterDead && level != null) {
            result.put("expReward", level.getRewardExp() != null ? level.getRewardExp() : 100);
            result.put("goldReward", level.getRewardGold() != null ? level.getRewardGold() : 50);
        }
        
        return result;
    }
    
    /**
     * 创建回合数据
     */
    private static Map<String, Object> createRoundData(Integer round, List<String> logs) {
        Map<String, Object> roundData = new HashMap<>();
        roundData.put("round", round);
        roundData.put("logs", logs);
        return roundData;
    }
    
    /**
     * 处理战斗胜利逻辑
     */
    public static void handleVictory(
            BattleRecord battleRecord,
            Long userId,
            Level level,
            UserElf userElf,
            UserElfMapper userElfMapper,
            cn.iocoder.gamecommon.interceptor.BattleSecurityInterceptor battleSecurityInterceptor,
            cn.iocoder.gamemodules.service.UserService userService) {
        
        battleRecord.setStatus(1); // 1=胜利
        
        // 注意：经验发放统一在claimReward中处理，这里不再发放经验
        // 原因：claimReward会给所有出战精灵发放经验，而不是只给当前精灵
        
        // 解锁下一关：更新用户的current_level为当前关卡ID+1
        if (level != null && level.getId() != null) {
            userService.updateUserLevel(userId, level.getId() + 1);
        }
        
        // 注意：不要在这里清理战斗状态！
        // 原因：claimReward需要查询战斗记录来发放经验
        // 战斗状态清理应该在claimReward完成后执行
        // battleSecurityInterceptor.updateBattleStatus(userId, false);
    }
    
    /**
     * 生成攻击日志
     */
    public static String generateAttackLog(String elfName, String actionType, String skillName, int damage) {
        if ("attack".equals(actionType)) {
            return String.format("你的精灵%s使用普通攻击，造成了%d点伤害", elfName, damage);
        } else {
            return String.format("你的精灵%s使用技能 %s，造成了%d点伤害", elfName, skillName, damage);
        }
    }
    
    /**
     * 生成敌人反击日志
     */
    public static String generateEnemyAttackLog(String monsterName, int damage) {
        return String.format("怪物%s使用普通攻击，造成了%d点伤害", monsterName, damage);
    }
    
    /**
     * 生成敌人攻击日志（区分技能和普通攻击）
     */
    public static String generateEnemyAttackLogWithDetail(String monsterName, String attackType, int damage) {
        // 判断是否是训练人偶
        if (monsterName.contains("训练人偶")) {
            // 如果monsterName已经包含"训练人偶"，直接使用
            return String.format("%s使用%s，造成了%d点伤害", monsterName, attackType, damage);
        }
        return String.format("怪物%s使用%s，造成了%d点伤害", monsterName, attackType, damage);
    }
    
    /**
     * 生成胜利日志（包含每日上限提示）
     */
    public static String generateVictoryLog(int actualExp, int actualGold, int expReward, int goldReward) {
        StringBuilder victoryLog = new StringBuilder("战斗胜利！");
        if (actualExp > 0 || actualGold > 0) {
            victoryLog.append("获得经验：").append(actualExp).append("，金币：").append(actualGold);
        }
        if (actualExp < expReward) {
            victoryLog.append("（今日经验收益已达上限）");
        }
        if (actualGold < goldReward) {
            victoryLog.append("（今日金币收益已达上限）");
        }
        return victoryLog.toString();
    }

    /**
     * 检查玩家是否还有存活的出战精灵
     * @param userId 用户ID
     * @param battleRecordElfMapper 战斗记录精灵Mapper
     * @param battleRecordMapper 战斗记录Mapper
     * @return true=还有存活精灵，false=所有出战精灵都死亡
     */
    public static boolean hasAliveBattleElves(Long userId, 
                                               cn.iocoder.gamemodules.mapper.BattleRecordElfMapper battleRecordElfMapper,
                                               cn.iocoder.gamemodules.mapper.BattleRecordMapper battleRecordMapper) {
        // 查询当前战斗
        BattleRecord currentBattle = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (currentBattle == null) {
            return false;
        }
        
        // 查询该战斗中所有的精灵记录
        List<BattleRecordElf> battleElves = battleRecordElfMapper.selectByBattleId(currentBattle.getBattleId());
        
        // 检查是否有HP>0的精灵
        for (BattleRecordElf elf : battleElves) {
            if (elf.getCurrentHp() != null && elf.getCurrentHp() > 0) {
                return true; // 还有存活的精灵
            }
        }
        
        return false; // 所有精灵都死亡
    }

    /**
     * 获取下一个存活的出战精灵（按battle_record_elf中的顺序）
     * @param userId 用户ID
     * @param currentElfId 当前死亡的精灵ID（user_elf的id）
     * @param battleId 战斗ID
     * @param userElfMapper 用户精灵Mapper
     * @param battleRecordElfMapper 战斗精灵记录Mapper
     * @return 下一个存活的精灵，如果没有则返回null
     */
    public static UserElf getNextAliveElf(Long userId, Long currentElfId, String battleId,
                                           cn.iocoder.gamemodules.mapper.UserElfMapper userElfMapper,
                                           cn.iocoder.gamemodules.mapper.BattleRecordElfMapper battleRecordElfMapper) {
        // 查询该战斗中所有的精灵记录（按插入顺序，即fight_order顺序）
        List<BattleRecordElf> battleElves = battleRecordElfMapper.selectByBattleId(battleId);
        
        // 找到当前精灵的位置，返回下一个HP>0的精灵
        boolean foundCurrent = false;
        for (BattleRecordElf battleElf : battleElves) {
            if (foundCurrent && battleElf.getCurrentHp() != null && battleElf.getCurrentHp() > 0) {
                // 找到下一个存活的精灵
                return userElfMapper.selectById(battleElf.getElfId());
            }
            if (battleElf.getElfId().equals(currentElfId)) {
                foundCurrent = true;
            }
        }
        
        // 如果当前是最后一个，从头开始找
        if (foundCurrent) {
            for (BattleRecordElf battleElf : battleElves) {
                if (battleElf.getCurrentHp() != null && battleElf.getCurrentHp() > 0 
                    && !battleElf.getElfId().equals(currentElfId)) {
                    return userElfMapper.selectById(battleElf.getElfId());
                }
            }
        }
        
        return null; // 没有可用的精灵
    }

    /**
     * 处理玩家战败
     */
    public static void handleDefeat(BattleRecord battleRecord,
                                     cn.iocoder.gamecommon.interceptor.BattleSecurityInterceptor battleSecurityInterceptor,
                                     Long userId,
                                     cn.iocoder.gamemodules.mapper.BattleRecordMapper battleRecordMapper) {
        battleRecord.setStatus(2); // 2=失败
        
        // 保存战斗记录到数据库
        if (battleRecordMapper != null) {
            battleRecordMapper.updateById(battleRecord);
        }
        
        // 清理用户战斗状态
        battleSecurityInterceptor.updateBattleStatus(userId, false);
    }

    // 战斗日志管理类
    public static class BattleLogManager {
        private int currentRound = 1;
        private List<Map<String, Object>> roundLogs = new ArrayList<>();
        private List<String> currentRoundLogs = new ArrayList<>();

        // 开始新回合
        public void startNewRound() {
            // 保存当前回合的日志
            if (!currentRoundLogs.isEmpty()) {
                Map<String, Object> roundLog = new HashMap<>();
                roundLog.put("round", currentRound);
                roundLog.put("logs", new ArrayList<>(currentRoundLogs));
                roundLogs.add(roundLog);
                currentRoundLogs.clear();
            }
            currentRound++;
        }

        // 添加日志
        public void addLog(String log) {
            currentRoundLogs.add(log);
        }

        // 获取所有回合日志
        public List<Map<String, Object>> getRoundLogs() {
            // 保存当前回合的日志（如果有）
            if (!currentRoundLogs.isEmpty()) {
                Map<String, Object> roundLog = new HashMap<>();
                roundLog.put("round", currentRound);
                roundLog.put("logs", new ArrayList<>(currentRoundLogs));
                roundLogs.add(roundLog);
            }
            return roundLogs;
        }

        // 获取当前回合数
        public int getCurrentRound() {
            return currentRound;
        }

        // 重置日志管理器
        public void reset() {
            currentRound = 1;
            roundLogs.clear();
            currentRoundLogs.clear();
        }
    }
}