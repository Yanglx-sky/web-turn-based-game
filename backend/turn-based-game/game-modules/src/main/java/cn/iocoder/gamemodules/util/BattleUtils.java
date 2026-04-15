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

    // 判定先后手
    public static boolean isPlayerFirst(int playerSpeed, int enemySpeed) {
        return playerSpeed >= enemySpeed;
    }

    /**
     * 查询并验证战斗所需的所有实体信息
     * @return Map包含: battleRecord, userElfRecord, monsterRecord, userElf, elf, monster, level
     */
    public static Map<String, Object> loadBattleEntities(
            Long userId,
            BattleRecordMapper battleRecordMapper,
            BattleRecordElfMapper battleRecordElfMapper,
            BattleRecordMonsterMapper battleRecordMonsterMapper,
            UserElfMapper userElfMapper,
            ElfMapper elfMapper,
            MonsterMapper monsterMapper,
            LevelMapper levelMapper) {
        
        // 1. 查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            throw new RuntimeException("当前没有进行中的战斗");
        }
        
        // 2. 查询关卡信息
        Level level = levelMapper.selectById(battleRecord.getLevelId());
        
        // 3. 查询精灵状态
        List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
        if (elves.isEmpty()) {
            throw new RuntimeException("精灵信息不存在");
        }
        BattleRecordElf userElfRecord = elves.get(0);
        
        // 4. 查询怪物状态
        List<BattleRecordMonster> monsters = battleRecordMonsterMapper.selectByBattleId(battleRecord.getBattleId());
        if (monsters.isEmpty()) {
            throw new RuntimeException("怪物信息不存在");
        }
        BattleRecordMonster monsterRecord = monsters.get(0);
        
        // 5. 查询用户精灵信息
        UserElf userElf = userElfMapper.selectById(userElfRecord.getElfId());
        if (userElf == null) {
            throw new RuntimeException("精灵信息不存在");
        }
        
        // 6. 查询精灵模板
        Elf elf = elfMapper.selectById(userElf.getElfId());
        if (elf == null) {
            throw new RuntimeException("精灵模板不存在");
        }
        
        // 7. 查询怪物实体
        Monster monster = monsterMapper.selectById(monsterRecord.getMonsterId());
        if (monster == null) {
            throw new RuntimeException("怪物信息不存在");
        }
        
        Map<String, Object> entities = new HashMap<>();
        entities.put("battleRecord", battleRecord);
        entities.put("level", level);
        entities.put("userElfRecord", userElfRecord);
        entities.put("monsterRecord", monsterRecord);
        entities.put("userElf", userElf);
        entities.put("elf", elf);
        entities.put("monster", monster);
        
        return entities;
    }
    
    /**
     * 执行玩家攻击（普通攻击或技能）
     * @return 伤害值
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
            damage = calculateNormalDamage(userElf.getAttack(), monster.getDefense());
        } else if ("skill".equals(actionType)) {
            Skill skill = skillMapper.selectById(skillId);
            if (skill == null) {
                throw new RuntimeException("技能不存在");
            }
            
            // 检查MP
            int mpCost = skill.getCostMp();
            if (userElfRecord.getCurrentMp() < mpCost) {
                throw new RuntimeException("MP不足");
            }
            
            // 扣除MP
            userElfRecord.setCurrentMp(userElfRecord.getCurrentMp() - mpCost);
            userElfRecord.setUpdateTime(LocalDateTime.now());
            
            damage = calculateSkillDamage(skill, userElf.getAttack(), monster.getDefense(), 
                    elf.getElementType(), monster.getElementType());
        } else {
            throw new RuntimeException("无效的攻击类型");
        }
        
        return damage;
    }
    
    /**
     * 更新怪物血量并返回原始血量
     * @return 原始血量
     */
    public static int updateMonsterHp(BattleRecordMonster monsterRecord, int damage, 
                                      BattleRecordMonsterMapper monsterMapper) {
        int originalHp = monsterRecord.getCurrentHp();
        monsterRecord.setCurrentHp(Math.max(0, monsterRecord.getCurrentHp() - damage));
        monsterRecord.setUpdateTime(LocalDateTime.now());
        monsterMapper.updateById(monsterRecord);
        return originalHp;
    }
    
    /**
     * 执行敌人反击
     * @return 敌人造成的伤害
     */
    public static int executeEnemyCounterattack(BattleRecordElf userElfRecord, UserElf userElf,
                                                Monster monster, BattleRecordElfMapper elfMapper) {
        int enemyDamage = calculateNormalDamage(monster.getAttack(), userElf.getDefense());
        System.out.println("[DEBUG] 敌人攻击 - 怪物攻击力:" + monster.getAttack() + 
                          ", 玩家防御力:" + userElf.getDefense() + ", 计算伤害:" + enemyDamage);
        
        userElfRecord.setCurrentHp(Math.max(0, userElfRecord.getCurrentHp() - enemyDamage));
        userElfRecord.setUpdateTime(LocalDateTime.now());
        elfMapper.updateById(userElfRecord);
        
        return enemyDamage;
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
            BattleRecordMonster monsterRecord,
            Monster monster,
            Level level,
            boolean monsterDead,
            List<String> logs) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", monsterDead ? 1 : 0);
        result.put("roundLogs", Arrays.asList(createRoundData(battleRecord.getCurrentRound(), logs)));
        result.put("playerElfHp", userElfRecord.getCurrentHp());
        result.put("playerElfHpMax", userElf.getMaxHp());
        result.put("elfMp", userElfRecord.getCurrentMp());
        result.put("elfMpMax", userElf.getMaxMp());
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
            cn.iocoder.gamemodules.service.UserService userService,
            java.util.function.BiFunction<Long, Integer[], int[]> dailyLimitFunction) {
        
        battleRecord.setStatus(1); // 1=胜利
        
        int expReward = level != null && level.getRewardExp() != null ? level.getRewardExp() : 100;
        int goldReward = level != null && level.getRewardGold() != null ? level.getRewardGold() : 50;
        
        // 检查并应用每日收益上限
        int[] actualReward = dailyLimitFunction.apply(userId, new Integer[]{expReward, goldReward});
        int actualExp = actualReward[0];
        int actualGold = actualReward[1];
        
        // 给精灵增加经验
        if (actualExp > 0) {
            userElf.setExp(userElf.getExp() + actualExp);
            userElfMapper.updateById(userElf);
        }
        
        // 发放金币奖励
        if (actualGold > 0) {
            userService.addGold(userId, (long) actualGold);
        }
        
        // 清理用户战斗状态
        battleSecurityInterceptor.updateBattleStatus(userId, false);
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