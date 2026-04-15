package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.interceptor.BattleSecurityInterceptor;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.*;
import cn.iocoder.gamemodules.mapper.*;
import cn.iocoder.gamemodules.service.BattleService;
import cn.iocoder.gamemodules.service.UserService;
import cn.iocoder.gamemodules.util.BattleUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BattleServiceImpl implements BattleService {

    @Autowired
    private BattleRecordMapper battleRecordMapper;

    @Autowired
    private BattleRecordElfMapper battleRecordElfMapper;

    @Autowired
    private BattleRecordMonsterMapper battleRecordMonsterMapper;

    @Autowired
    private UserElfMapper userElfMapper;

    @Autowired
    private ElfMapper elfMapper;

    @Autowired
    private MonsterMapper monsterMapper;

    @Autowired
    private LevelMapper levelMapper;
    
    @Autowired
    private SkillMapper skillMapper;
    
    @Autowired
    private BattleSecurityInterceptor battleSecurityInterceptor;
    
    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public Result<Map<String, Object>> startBattle(Long userId, Long userElfId, Integer levelId) {
        // 生成唯一的battleId
        String battleId = UUID.randomUUID().toString();

        // 插入battle_record表
        BattleRecord battleRecord = new BattleRecord();
        battleRecord.setUserId(userId);
        battleRecord.setLevelId(levelId);
        battleRecord.setGetExp(0);
        battleRecord.setCreateTime(LocalDateTime.now());
        battleRecord.setBattleId(battleId);
        battleRecord.setCurrentRound(1);
        battleRecord.setStatus(0); // 0=战斗中
        battleRecordMapper.insert(battleRecord);

        // 初始化精灵状态
        UserElf userElf = userElfMapper.selectById(userElfId);
        if (userElf != null) {
            BattleRecordElf battleElf = new BattleRecordElf();
            battleElf.setBattleId(battleId);
            battleElf.setElfId(userElfId);
            battleElf.setCurrentHp(userElf.getHp());
            battleElf.setCurrentMp(userElf.getMp());
            battleElf.setElfState(0); // 0=战斗中
            battleElf.setCreateTime(LocalDateTime.now());
            battleElf.setUpdateTime(LocalDateTime.now());
            battleRecordElfMapper.insert(battleElf);
        }

        // 初始化怪物状态
        Level level = levelMapper.selectById(levelId);
        if (level != null) {
            // 假设每个关卡只有一个怪物，实际项目中可能需要从配置中获取多个怪物
            Monster monster = monsterMapper.selectById(level.getMonsterId());
            if (monster != null) {
                BattleRecordMonster battleMonster = new BattleRecordMonster();
                battleMonster.setBattleId(battleId);
                battleMonster.setMonsterId(monster.getId());
                battleMonster.setCurrentHp(monster.getHp());
                battleMonster.setCurrentMp(monster.getMp());
                battleMonster.setIsAlive(1); // 1=存活
                battleMonster.setCreateTime(LocalDateTime.now());
                battleMonster.setUpdateTime(LocalDateTime.now());
                battleRecordMonsterMapper.insert(battleMonster);
            }
        }

        // 设置用户战斗状态为true
        battleSecurityInterceptor.updateBattleStatus(userId, true);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("battleId", battleId);
        result.put("battleInfo", battleRecord);
        
        // 添加userElf信息
        if (userElf != null) {
            Map<String, Object> userElfInfo = new HashMap<>();
            userElfInfo.put("id", userElf.getId());
            userElfInfo.put("elfId", userElf.getElfId());
            userElfInfo.put("userId", userElf.getUserId());
            userElfInfo.put("level", userElf.getLevel());
            userElfInfo.put("exp", userElf.getExp());
            userElfInfo.put("hp", userElf.getMaxHp());
            userElfInfo.put("maxHp", userElf.getMaxHp());
            userElfInfo.put("mp", userElf.getMaxMp());
            userElfInfo.put("maxMp", userElf.getMaxMp());
            userElfInfo.put("attack", userElf.getAttack());
            userElfInfo.put("defense", userElf.getDefense());
            userElfInfo.put("speed", userElf.getSpeed());
            
            // 从Elf实体中获取elementType
            Elf elf = elfMapper.selectById(userElf.getElfId());
            if (elf != null) {
                userElfInfo.put("elementType", elf.getElementType());
            }
            
            result.put("userElf", userElfInfo);
        }

        // 添加monster信息
        if (level != null) {
            Monster monster = monsterMapper.selectById(level.getMonsterId());
            if (monster != null) {
                Map<String, Object> monsterInfo = new HashMap<>();
                monsterInfo.put("id", monster.getId());
                monsterInfo.put("monsterId", monster.getId());
                monsterInfo.put("name", monster.getMonsterName());
                monsterInfo.put("level", 1); // 怪物等级默认为1
                monsterInfo.put("hp", monster.getHp());
                monsterInfo.put("maxHp", monster.getHp());
                monsterInfo.put("mp", monster.getMp());
                monsterInfo.put("maxMp", monster.getMp());
                monsterInfo.put("attack", monster.getAttack());
                monsterInfo.put("defense", monster.getDefense());
                monsterInfo.put("speed", monster.getSpeed());
                monsterInfo.put("elementType", monster.getElementType());
                result.put("monster", monsterInfo);
                // 添加前端需要的字段
                result.put("monsterName", monster.getMonsterName());
                result.put("monsterElementType", monster.getElementType());
                result.put("monsterHp", monster.getHp());
                result.put("monsterMaxHp", monster.getHp());
                result.put("monsterMp", monster.getMp());
                result.put("monsterMaxMp", monster.getMp());
            }
        }

        return Result.success(result);
    }

    @Override
    @Transactional
    public Result<?> playerOffline(Long userId) {
        // 根据userId查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            return Result.error("当前没有进行中的战斗");
        }

        // 更新战斗状态为断线暂停
        battleRecord.setStatus(3); // 3=断线暂停
        battleRecord.setOfflineTime(LocalDateTime.now());
        battleRecordMapper.updateById(battleRecord);

        return Result.success("战斗已暂停");
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> reconnect(Long userId) {
        // 根据userId查询当前战斗（状态为3的战斗）
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            return Result.error("没有需要重连的战斗");
        }

        // 判断是否超时（>90秒）
        LocalDateTime offlineTime = battleRecord.getOfflineTime();
        LocalDateTime now = LocalDateTime.now();
        if (offlineTime.plusSeconds(90).isBefore(now)) {
            // 超时，改为战斗失败
            battleRecord.setStatus(2); // 2=失败
            battleRecordMapper.updateById(battleRecord);
            return Result.error("战斗已超时，自动判定为失败");
        }

        // 未超时，恢复战斗状态
        battleRecord.setStatus(0); // 0=战斗中
        battleRecordMapper.updateById(battleRecord);

        // 查询精灵状态
        List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());

        // 查询怪物状态
        List<BattleRecordMonster> monsters = battleRecordMonsterMapper.selectByBattleId(battleRecord.getBattleId());

        // 查询怪物信息获取怪物名称和系别
        String monsterName = "敌人";
        Integer monsterElementType = 0;
        if (monsters != null && !monsters.isEmpty()) {
            Monster monster = monsterMapper.selectById(monsters.get(0).getMonsterId());
            if (monster != null) {
                monsterName = monster.getMonsterName();
                monsterElementType = monster.getElementType();
            }
        }

        // 构建返回结果，与前端期望的数据结构一致
        Map<String, Object> result = new HashMap<>();
        result.put("battleId", battleRecord.getBattleId());
        result.put("currentRound", battleRecord.getCurrentRound());
        result.put("levelId", battleRecord.getLevelId());
        result.put("status", battleRecord.getStatus());
        result.put("elves", elves);
        result.put("monsters", monsters);
        result.put("monsterName", monsterName);
        result.put("monsterElementType", monsterElementType);

        return Result.success(result);
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> flee(Long userId) {
        // 根据userId查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            return Result.error("当前没有进行中的战斗");
        }

        // 更新战斗状态为失败（逃跑视为失败）
        battleRecord.setStatus(2); // 2=失败
        battleRecordMapper.updateById(battleRecord);
        
        // 清理用户战斗状态
        battleSecurityInterceptor.updateBattleStatus(userId, false);

        // 构建返回结果，包含战斗日志
        Map<String, Object> roundData = new HashMap<>();
        roundData.put("round", battleRecord.getCurrentRound());
        List<String> logs = new ArrayList<>();
        logs.add("你逃跑了！");
        roundData.put("logs", logs);
        
        List<Map<String, Object>> roundLogsList = new ArrayList<>();
        roundLogsList.add(roundData);

        Map<String, Object> result = new HashMap<>();
        result.put("roundLogs", roundLogsList);
        result.put("battleLog", Arrays.asList("你逃跑了！"));
        
        return Result.success(result);
    }

    @Override
    @Transactional
    public Result<?> abandonBattle(Long userId) {
        // 根据userId查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            return Result.error("当前没有进行中的战斗");
        }

        // 更新战斗状态为失败（放弃视为失败）
        battleRecord.setStatus(2); // 2=失败
        battleRecordMapper.updateById(battleRecord);
        
        // 清理用户战斗状态
        battleSecurityInterceptor.updateBattleStatus(userId, false);

        return Result.success("放弃战斗成功");
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> normalAttack(Long userId) {
        // 根据userId查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            return Result.error("当前没有进行中的战斗");
        }

        // 查询精灵状态
        List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
        if (elves.isEmpty()) {
            return Result.error("精灵信息不存在");
        }
        BattleRecordElf userElfRecord = elves.get(0);

        // 查询怪物状态
        List<BattleRecordMonster> monsters = battleRecordMonsterMapper.selectByBattleId(battleRecord.getBattleId());
        if (monsters.isEmpty()) {
            return Result.error("怪物信息不存在");
        }
        BattleRecordMonster monsterRecord = monsters.get(0);

        // 查询用户精灵信息，获取攻击属性
        UserElf userElf = userElfMapper.selectById(userElfRecord.getElfId());
        if (userElf == null) {
            return Result.error("精灵信息不存在");
        }

        // 查询精灵模板，获取精灵名称
        Elf elf = elfMapper.selectById(userElf.getElfId());
        if (elf == null) {
            return Result.error("精灵模板不存在");
        }

        // 查询怪物实体，获取防御属性
        Monster monster = monsterMapper.selectById(monsterRecord.getMonsterId());
        if (monster == null) {
            return Result.error("怪物信息不存在");
        }

        // 计算普通攻击伤害（使用用户精灵的攻击力）
        int damage = BattleUtils.calculateNormalDamage(userElf.getAttack(), monster.getDefense());
        
        // 记录原始血量用于战斗日志
        int originalMonsterHp = monsterRecord.getCurrentHp();

        // 更新怪物血量
        monsterRecord.setCurrentHp(monsterRecord.getCurrentHp() - damage);
        if (monsterRecord.getCurrentHp() < 0) {
            monsterRecord.setCurrentHp(0);
        }
        monsterRecord.setUpdateTime(LocalDateTime.now());
        battleRecordMonsterMapper.updateById(monsterRecord);

        // 检查怪物是否死亡
        boolean monsterDead = monsterRecord.getCurrentHp() <= 0;
        
        // 查询关卡信息用于奖励计算
        Level level = levelMapper.selectById(battleRecord.getLevelId());
        
        // 构建战斗日志
        String attackLog = String.format("你的精灵%s使用普通攻击，造成了%d点伤害", 
            elf.getElfName(), damage);
        
        String hpLog = String.format("敌人HP: %d/%d → %d/%d", 
            originalMonsterHp, monster.getHp(), monsterRecord.getCurrentHp(), monster.getHp());
        
        // 增加回合数
        battleRecord.setCurrentRound(battleRecord.getCurrentRound() + 1);
        battleRecordMapper.updateById(battleRecord);

        // 构建回合日志
        Map<String, Object> roundData = new HashMap<>();
        roundData.put("round", battleRecord.getCurrentRound());
        List<String> logs = new ArrayList<>();
        logs.add(attackLog);
        logs.add(hpLog);

        if (monsterDead) {
            // 更新战斗状态为胜利
            battleRecord.setStatus(1); // 1=胜利
            battleRecordMapper.updateById(battleRecord);
            
            // 计算奖励
            int expReward = level != null && level.getRewardExp() != null ? level.getRewardExp() : 100;
            int goldReward = level != null && level.getRewardGold() != null ? level.getRewardGold() : 50;
            
            // 给精灵增加经验
            userElf.setExp(userElf.getExp() + expReward);
            userElfMapper.updateById(userElf);
            
            // 发放金币奖励
            userService.addGold(userId, (long) goldReward);
            
            // 清理用户战斗状态
            battleSecurityInterceptor.updateBattleStatus(userId, false);
            
            logs.add("战斗胜利！获得经验：" + expReward + "，金币：" + goldReward);
        } else {
            // 敌人反击
            int enemyDamage = BattleUtils.calculateNormalDamage(monster.getAttack(), userElf.getDefense());
            System.out.println("[DEBUG] 敌人攻击 - 怪物攻击力:" + monster.getAttack() + ", 玩家防御力:" + userElf.getDefense() + ", 计算伤害:" + enemyDamage);
            userElfRecord.setCurrentHp(userElfRecord.getCurrentHp() - enemyDamage);
            if (userElfRecord.getCurrentHp() < 0) {
                userElfRecord.setCurrentHp(0);
            }
            userElfRecord.setUpdateTime(LocalDateTime.now());
            battleRecordElfMapper.updateById(userElfRecord);
            
            String enemyAttackLog = String.format("怪物%s使用普通攻击，造成了%d点伤害",
                monster.getMonsterName(), enemyDamage);
            logs.add(enemyAttackLog);
            
            // 检查玩家精灵是否死亡
            if (userElfRecord.getCurrentHp() <= 0) {
                battleRecord.setStatus(2); // 2=失败
                battleRecordMapper.updateById(battleRecord);
                battleSecurityInterceptor.updateBattleStatus(userId, false);
                logs.add("战斗失败！你的精灵被击败了");
            }
        }
        
        roundData.put("logs", logs);
        
        List<Map<String, Object>> roundLogsList = new ArrayList<>();
        roundLogsList.add(roundData);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", monsterDead ? 1 : 0); // 1=胜利，0=战斗中
        result.put("roundLogs", roundLogsList);
        result.put("monsterHp", monsterRecord.getCurrentHp());
        result.put("monsterMaxHp", monster.getHp());
        result.put("playerElfHp", userElfRecord.getCurrentHp());
        result.put("playerElfHpMax", userElf.getMaxHp());
        result.put("elfMp", userElfRecord.getCurrentMp());
        result.put("elfMpMax", userElf.getMaxMp());
        
        if (monsterDead && level != null) {
            result.put("expReward", level.getRewardExp() != null ? level.getRewardExp() : 100);
            result.put("goldReward", level.getRewardGold() != null ? level.getRewardGold() : 50);
        }

        return Result.success(result);
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> useSkill(Long userId, Integer skillId) {
        // 根据userId查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            return Result.error("当前没有进行中的战斗");
        }

        // 查询关卡信息
        Level level = levelMapper.selectById(battleRecord.getLevelId());
        
        // 查询精灵状态
        List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
        if (elves.isEmpty()) {
            return Result.error("精灵信息不存在");
        }
        BattleRecordElf userElfRecord = elves.get(0);

        // 查询怪物状态
        List<BattleRecordMonster> monsters = battleRecordMonsterMapper.selectByBattleId(battleRecord.getBattleId());
        if (monsters.isEmpty()) {
            return Result.error("怪物信息不存在");
        }
        BattleRecordMonster monsterRecord = monsters.get(0);

        // 查询用户精灵信息，获取攻击属性
        UserElf userElf = userElfMapper.selectById(userElfRecord.getElfId());
        if (userElf == null) {
            return Result.error("精灵信息不存在");
        }

        // 查询精灵模板，获取系别属性
        Elf elf = elfMapper.selectById(userElf.getElfId());
        if (elf == null) {
            return Result.error("精灵信息不存在");
        }

        // 查询怪物实体，获取防御属性和系别属性
        Monster monster = monsterMapper.selectById(monsterRecord.getMonsterId());
        if (monster == null) {
            return Result.error("怪物信息不存在");
        }

        // 根据skillId查询技能信息
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            return Result.error("技能不存在");
        }

        // 获取技能MP消耗
        int mpCost = skill.getCostMp();

        // 检查MP是否足够
        if (userElfRecord.getCurrentMp() < mpCost) {
            return Result.error("MP不足");
        }

        // 记录原始血量用于战斗日志
        int originalElfMp = userElfRecord.getCurrentMp();
        int originalMonsterHp = monsterRecord.getCurrentHp();

        // 更新精灵MP
        userElfRecord.setCurrentMp(userElfRecord.getCurrentMp() - mpCost);
        userElfRecord.setUpdateTime(LocalDateTime.now());
        battleRecordElfMapper.updateById(userElfRecord);

        // 计算技能伤害（使用用户精灵的攻击力）
        int damage = BattleUtils.calculateSkillDamage(skill, userElf.getAttack(), monster.getDefense(), elf.getElementType(), monster.getElementType());

        // 更新怪物血量
        monsterRecord.setCurrentHp(monsterRecord.getCurrentHp() - damage);
        if (monsterRecord.getCurrentHp() < 0) {
            monsterRecord.setCurrentHp(0);
        }
        monsterRecord.setUpdateTime(LocalDateTime.now());
        battleRecordMonsterMapper.updateById(monsterRecord);

        // 增加回合数
        battleRecord.setCurrentRound(battleRecord.getCurrentRound() + 1);
        battleRecordMapper.updateById(battleRecord);

        // 构建战斗日志
        String skillLog = String.format("你的精灵%s使用技能 %s，造成了%d点伤害", 
            elf.getElfName(), skill.getSkillName(), damage);
        
        String hpLog = String.format("敌人HP: %d/%d → %d/%d", 
            originalMonsterHp, monster.getHp(), monsterRecord.getCurrentHp(), monster.getHp());

        // 检查怪物是否死亡
        boolean monsterDead = monsterRecord.getCurrentHp() <= 0;
        
        // 构建回合日志
        Map<String, Object> roundData = new HashMap<>();
        roundData.put("round", battleRecord.getCurrentRound());
        List<String> logs = new ArrayList<>();
        logs.add(skillLog);
        logs.add(hpLog);
        
        if (monsterDead) {
            // 更新战斗状态为胜利
            battleRecord.setStatus(1); // 1=胜利
            battleRecordMapper.updateById(battleRecord);
            
            // 计算奖励
            int expReward = level != null && level.getRewardExp() != null ? level.getRewardExp() : 100;
            int goldReward = level != null && level.getRewardGold() != null ? level.getRewardGold() : 50;
            
            // 给精灵增加经验
            userElf.setExp(userElf.getExp() + expReward);
            userElfMapper.updateById(userElf);
            
            // 发放金币奖励
            userService.addGold(userId, (long) goldReward);
            
            // 清理用户战斗状态
            battleSecurityInterceptor.updateBattleStatus(userId, false);
            
            skillLog += "\n战斗胜利！获得经验：" + expReward + "，金币：" + goldReward;
        } else {
            // 敌人反击
            int enemyDamage = BattleUtils.calculateNormalDamage(monster.getAttack(), userElf.getDefense());
            System.out.println("[DEBUG] 敌人攻击 - 怪物攻击力:" + monster.getAttack() + ", 玩家防御力:" + userElf.getDefense() + ", 计算伤害:" + enemyDamage);
            userElfRecord.setCurrentHp(userElfRecord.getCurrentHp() - enemyDamage);
            if (userElfRecord.getCurrentHp() < 0) {
                userElfRecord.setCurrentHp(0);
            }
            userElfRecord.setUpdateTime(LocalDateTime.now());
            battleRecordElfMapper.updateById(userElfRecord);
            
            String enemyAttackLog = String.format("怪物%s使用普通攻击，造成了%d点伤害",
                monster.getMonsterName(), enemyDamage);
            logs.add(enemyAttackLog);
            
            // 检查玩家精灵是否死亡
            if (userElfRecord.getCurrentHp() <= 0) {
                battleRecord.setStatus(2); // 2=失败
                battleRecordMapper.updateById(battleRecord);
                battleSecurityInterceptor.updateBattleStatus(userId, false);
                logs.add("战斗失败！你的精灵被击败了");
            }
        }
        
        roundData.put("logs", logs);
        
        List<Map<String, Object>> roundLogsList = new ArrayList<>();
        roundLogsList.add(roundData);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", monsterDead ? 1 : 0); // 1=胜利，0=战斗中
        result.put("roundLogs", roundLogsList);
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

        return Result.success(result);
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> switchElf(Long userId, Long elfId) {
        // 根据userId查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            return Result.error("当前没有进行中的战斗");
        }

        // 查询用户的精灵
        UserElf userElf = userElfMapper.selectById(elfId);
        if (userElf == null || !userElf.getUserId().equals(userId)) {
            return Result.error("精灵不存在或不属于当前用户");
        }

        // 查询精灵信息
        Elf elf = elfMapper.selectById(userElf.getElfId());
        if (elf == null) {
            return Result.error("精灵信息不存在");
        }

        // 查询原精灵状态
        List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
        if (elves.isEmpty()) {
            return Result.error("精灵信息不存在");
        }

        // 替换精灵
        BattleRecordElf oldElf = elves.get(0);
        oldElf.setElfId(elfId);
        oldElf.setCurrentHp(userElf.getHp());
        oldElf.setCurrentMp(userElf.getMp());
        oldElf.setElfState(0); // 0=战斗中
        oldElf.setUpdateTime(LocalDateTime.now());
        battleRecordElfMapper.updateById(oldElf);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", 0); // 0=战斗中
        result.put("elf", Map.of(
            "id", userElf.getId(),
            "elfId", userElf.getElfId(),
            "hp", userElf.getHp(),
            "maxHp", userElf.getMaxHp(),
            "mp", userElf.getMp(),
            "maxMp", userElf.getMaxMp(),
            "level", userElf.getLevel(),
            "elementType", elf.getElementType()
        ));

        return Result.success(result);
    }

    @Override
    @Transactional
    public void handleTimeoutBattles() {
        // 计算90秒前的时间
        LocalDateTime thresholdTime = LocalDateTime.now().minusSeconds(90);

        // 查询超时的断线战斗
        List<BattleRecord> timeoutBattles = battleRecordMapper.selectTimeoutBattles(thresholdTime);

        // 将超时的战斗改为失败
        for (BattleRecord battleRecord : timeoutBattles) {
            battleRecord.setStatus(2); // 2=失败
            battleRecordMapper.updateById(battleRecord);
        }
    }

    @Override
    @Transactional
    public void saveBattleState(String battleId, List<BattleRecordElf> elves, List<BattleRecordMonster> monsters, Integer currentRound) {
        // 更新战斗记录的当前回合
        QueryWrapper<BattleRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("battle_id", battleId);
        BattleRecord battleRecord = battleRecordMapper.selectOne(wrapper);
        if (battleRecord != null) {
            battleRecord.setCurrentRound(currentRound);
            battleRecordMapper.updateById(battleRecord);
        }

        // 更新精灵状态
        for (BattleRecordElf elf : elves) {
            elf.setUpdateTime(LocalDateTime.now());
            battleRecordElfMapper.updateById(elf);
        }

        // 更新怪物状态
        for (BattleRecordMonster monster : monsters) {
            monster.setUpdateTime(LocalDateTime.now());
            battleRecordMonsterMapper.updateById(monster);
        }
    }
}