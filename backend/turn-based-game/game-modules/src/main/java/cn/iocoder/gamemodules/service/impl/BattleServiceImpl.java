package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.constants.RedisKeyConstant;
import cn.iocoder.gamecommon.exception.BattleException;
import cn.iocoder.gamecommon.interceptor.BattleSecurityInterceptor;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.*;
import cn.iocoder.gamemodules.mapper.*;
import cn.iocoder.gamemodules.service.BattleService;
import cn.iocoder.gamemodules.service.UserService;
import cn.iocoder.gamemodules.util.BattleUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    
    // ==================== Redis相关 ====================
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
        try {
            // 1. 加载战斗实体
            Map<String, Object> entities = BattleUtils.loadBattleEntities(
                    userId, battleRecordMapper, battleRecordElfMapper, battleRecordMonsterMapper,
                    userElfMapper, elfMapper, monsterMapper, levelMapper);
            
            BattleRecord battleRecord = (BattleRecord) entities.get("battleRecord");
            Level level = (Level) entities.get("level");
            BattleRecordElf userElfRecord = (BattleRecordElf) entities.get("userElfRecord");
            BattleRecordMonster monsterRecord = (BattleRecordMonster) entities.get("monsterRecord");
            UserElf userElf = (UserElf) entities.get("userElf");
            Elf elf = (Elf) entities.get("elf");
            Monster monster = (Monster) entities.get("monster");
            
            // 2. 执行普通攻击
            int damage = BattleUtils.executePlayerAttack("attack", null, userElfRecord, userElf, elf, monster, skillMapper);
            
            // 3. 更新怪物血量
            int originalMonsterHp = BattleUtils.updateMonsterHp(monsterRecord, damage, battleRecordMonsterMapper);
            
            // 4. 增加回合数
            battleRecord.setCurrentRound(battleRecord.getCurrentRound() + 1);
            battleRecordMapper.updateById(battleRecord);
            
            // 5. 判断怪物是否死亡
            boolean monsterDead = monsterRecord.getCurrentHp() <= 0;
            
            // 6. 构建战斗日志
            String attackLog = BattleUtils.generateAttackLog(elf.getElfName(), "attack", null, damage);
            List<String> logs;
            
            if (monsterDead) {
                // 处理胜利
                BattleUtils.handleVictory(battleRecord, userId, level, userElf, userElfMapper,
                        battleSecurityInterceptor, userService, this::checkAndDeductDailyLimit);
                
                int expReward = level != null && level.getRewardExp() != null ? level.getRewardExp() : 100;
                int goldReward = level != null && level.getRewardGold() != null ? level.getRewardGold() : 50;
                int[] actualReward = checkAndDeductDailyLimit(userId, new Integer[]{expReward, goldReward});
                
                String victoryLog = BattleUtils.generateVictoryLog(actualReward[0], actualReward[1], expReward, goldReward);
                logs = BattleUtils.buildBattleLogs(attackLog, originalMonsterHp, monster.getHp(), 
                        monsterRecord.getCurrentHp(), null, true, false);
                logs.add(victoryLog);
            } else {
                // 敌人反击
                int enemyDamage = BattleUtils.executeEnemyCounterattack(userElfRecord, userElf, monster, battleRecordElfMapper);
                String enemyAttackLog = BattleUtils.generateEnemyAttackLog(monster.getMonsterName(), enemyDamage);
                
                boolean playerDead = userElfRecord.getCurrentHp() <= 0;
                if (playerDead) {
                    battleRecord.setStatus(2); // 2=失败
                    battleRecordMapper.updateById(battleRecord);
                    battleSecurityInterceptor.updateBattleStatus(userId, false);
                }
                
                logs = BattleUtils.buildBattleLogs(attackLog, originalMonsterHp, monster.getHp(),
                        monsterRecord.getCurrentHp(), enemyAttackLog, false, playerDead);
            }
            
            // 7. 构建返回结果
            Map<String, Object> result = BattleUtils.buildBattleResult(
                    battleRecord, userElfRecord, userElf, monsterRecord, monster, level, monsterDead, logs);
            
            return Result.success(result);
            
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> useSkill(Long userId, Integer skillId) {
        try {
            // 1. 加载战斗实体
            Map<String, Object> entities = BattleUtils.loadBattleEntities(
                    userId, battleRecordMapper, battleRecordElfMapper, battleRecordMonsterMapper,
                    userElfMapper, elfMapper, monsterMapper, levelMapper);
            
            BattleRecord battleRecord = (BattleRecord) entities.get("battleRecord");
            Level level = (Level) entities.get("level");
            BattleRecordElf userElfRecord = (BattleRecordElf) entities.get("userElfRecord");
            BattleRecordMonster monsterRecord = (BattleRecordMonster) entities.get("monsterRecord");
            UserElf userElf = (UserElf) entities.get("userElf");
            Elf elf = (Elf) entities.get("elf");
            Monster monster = (Monster) entities.get("monster");
            
            // 2. 查询技能信息
            Skill skill = skillMapper.selectById(skillId);
            if (skill == null) {
                return Result.error("技能不存在");
            }
            
            // 3. 执行技能攻击
            int damage = BattleUtils.executePlayerAttack("skill", skillId, userElfRecord, userElf, elf, monster, skillMapper);
            
            // 4. 更新怪物血量（MP已在executePlayerAttack中扣除）
            battleRecordElfMapper.updateById(userElfRecord);
            int originalMonsterHp = BattleUtils.updateMonsterHp(monsterRecord, damage, battleRecordMonsterMapper);
            
            // 5. 增加回合数
            battleRecord.setCurrentRound(battleRecord.getCurrentRound() + 1);
            battleRecordMapper.updateById(battleRecord);
            
            // 6. 判断怪物是否死亡
            boolean monsterDead = monsterRecord.getCurrentHp() <= 0;
            
            // 7. 构建战斗日志
            String attackLog = BattleUtils.generateAttackLog(elf.getElfName(), "skill", skill.getSkillName(), damage);
            List<String> logs;
            
            if (monsterDead) {
                // 处理胜利
                BattleUtils.handleVictory(battleRecord, userId, level, userElf, userElfMapper,
                        battleSecurityInterceptor, userService, this::checkAndDeductDailyLimit);
                
                int expReward = level != null && level.getRewardExp() != null ? level.getRewardExp() : 100;
                int goldReward = level != null && level.getRewardGold() != null ? level.getRewardGold() : 50;
                int[] actualReward = checkAndDeductDailyLimit(userId, new Integer[]{expReward, goldReward});
                
                String victoryLog = BattleUtils.generateVictoryLog(actualReward[0], actualReward[1], expReward, goldReward);
                logs = BattleUtils.buildBattleLogs(attackLog, originalMonsterHp, monster.getHp(),
                        monsterRecord.getCurrentHp(), null, true, false);
                logs.add(victoryLog);
            } else {
                // 敌人反击
                int enemyDamage = BattleUtils.executeEnemyCounterattack(userElfRecord, userElf, monster, battleRecordElfMapper);
                String enemyAttackLog = BattleUtils.generateEnemyAttackLog(monster.getMonsterName(), enemyDamage);
                
                boolean playerDead = userElfRecord.getCurrentHp() <= 0;
                if (playerDead) {
                    battleRecord.setStatus(2); // 2=失败
                    battleRecordMapper.updateById(battleRecord);
                    battleSecurityInterceptor.updateBattleStatus(userId, false);
                }
                
                logs = BattleUtils.buildBattleLogs(attackLog, originalMonsterHp, monster.getHp(),
                        monsterRecord.getCurrentHp(), enemyAttackLog, false, playerDead);
            }
            
            // 8. 构建返回结果
            Map<String, Object> result = BattleUtils.buildBattleResult(
                    battleRecord, userElfRecord, userElf, monsterRecord, monster, level, monsterDead, logs);
            
            return Result.success(result);
            
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
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

    // ==================== 以下为战斗监控+一致性+每日收益上限功能实现 ====================

    /**
     * 提交战斗行动（带回合校验和幂等性）
     * 核心：防连点、防重复扣血、回合顺序校验
     */
    @Override
    @Transactional
    public Result<Map<String, Object>> submitAction(Long userId, Integer round, String actionType, Integer skillId, Long elfId) {
        // 1. 校验出招冷却时间（防连点、防脚本）
        if (!checkActionCooldown(userId)) {
            throw BattleException.actionTooFast();
        }
        
        // 2. 查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            throw BattleException.battleNotFound();
        }
        
        String battleId = battleRecord.getBattleId();
        
        // 3. 校验战斗状态
        if (battleRecord.getStatus() != 0) {
            throw BattleException.battleEnded();
        }
        
        // 4. 校验回合顺序（拒绝乱序/重复）
        Integer currentRound = battleRecord.getCurrentRound();
        if (round == null || !round.equals(currentRound)) {
            throw BattleException.invalidRound();
        }
        
        // 5. 幂等性校验：检查该回合是否已提交过动作
        String actionKey = RedisKeyConstant.buildBattleActionKey(battleId, round);
        String existingAction = stringRedisTemplate.opsForValue().get(actionKey);
        if (existingAction != null) {
            throw BattleException.duplicateAction();
        }
        
        // 6. 执行对应的战斗动作
        Result<Map<String, Object>> result;
        switch (actionType) {
            case "attack":
                result = normalAttack(userId);
                break;
            case "skill":
                if (skillId == null) {
                    return Result.error("缺少skillId参数");
                }
                result = useSkill(userId, skillId);
                break;
            case "switch":
                if (elfId == null) {
                    return Result.error("缺少elfId参数");
                }
                result = switchElf(userId, elfId);
                break;
            default:
                return Result.error("无效的动作类型");
        }
        
        // 7. 记录已执行的动作（幂等标记）
        stringRedisTemplate.opsForValue().set(actionKey, actionType, 
                Duration.ofSeconds(RedisKeyConstant.BATTLE_SESSION_EXPIRE_SEC));
        
        // 8. 更新出招冷却时间
        updateActionCooldown(userId);
        
        return result;
    }

    /**
     * 领取战斗奖励（必须战斗胜利才能调用）
     * 核心：权限控制、幂等发奖、每日收益上限
     */
    @Override
    @Transactional
    public Result<Map<String, Object>> claimReward(Long userId, Integer levelId, String battleId) {
        // 1. 校验战斗胜利标记（必须战斗胜利）
        String victoryKey = RedisKeyConstant.buildBattleVictoryKey(userId, levelId);
        String victoryBattleId = stringRedisTemplate.opsForValue().get(victoryKey);
        
        if (victoryBattleId == null || !victoryBattleId.equals(battleId)) {
            throw BattleException.noRewardPermission();
        }
        
        // 2. 幂等性校验：检查是否已领取奖励
        String rewardKey = RedisKeyConstant.buildBattleRewardKey(userId, levelId, battleId);
        Boolean claimed = stringRedisTemplate.hasKey(rewardKey);
        if (Boolean.TRUE.equals(claimed)) {
            throw BattleException.rewardAlreadyClaimed();
        }
        
        // 3. 查询关卡奖励配置
        Level level = levelMapper.selectById(levelId);
        if (level == null) {
            return Result.error("关卡不存在");
        }
        
        int expReward = level.getRewardExp() != null ? level.getRewardExp() : 100;
        int goldReward = level.getRewardGold() != null ? level.getRewardGold() : 50;
        
        // 4. 检查并扣除每日收益配额
        int[] actualReward = checkAndDeductDailyLimit(userId, new Integer[]{expReward, goldReward});
        int actualExp = actualReward[0];
        int actualGold = actualReward[1];
        
        // 5. 发放奖励
        if (actualExp > 0) {
            // 查询当前出战精灵并增加经验
            BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
            if (battleRecord != null) {
                List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
                if (!elves.isEmpty()) {
                    UserElf userElf = userElfMapper.selectById(elves.get(0).getElfId());
                    if (userElf != null) {
                        userElf.setExp(userElf.getExp() + actualExp);
                        userElfMapper.updateById(userElf);
                    }
                }
            }
        }
        
        if (actualGold > 0) {
            userService.addGold(userId, (long) actualGold);
        }
        
        // 6. 标记奖励已领取（幂等标记）
        stringRedisTemplate.opsForValue().set(rewardKey, "1", 
                Duration.ofSeconds(RedisKeyConstant.getSecondsUntilMidnight()));
        
        // 7. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("expReward", expReward);
        result.put("goldReward", goldReward);
        result.put("actualExp", actualExp);
        result.put("actualGold", actualGold);
        result.put("expLimitReached", actualExp < expReward);
        result.put("goldLimitReached", actualGold < goldReward);
        
        return Result.success(result);
    }

    /**
     * 获取今日收益信息
     */
    @Override
    public Result<Map<String, Object>> getDailyRewardInfo(Long userId) {
        String expKey = RedisKeyConstant.buildDailyExpKey(userId);
        String goldKey = RedisKeyConstant.buildDailyGoldKey(userId);
        
        String expStr = stringRedisTemplate.opsForValue().get(expKey);
        String goldStr = stringRedisTemplate.opsForValue().get(goldKey);
        
        int todayExp = expStr != null ? Integer.parseInt(expStr) : 0;
        int todayGold = goldStr != null ? Integer.parseInt(goldStr) : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("todayExp", todayExp);
        result.put("todayGold", todayGold);
        result.put("expLimit", RedisKeyConstant.DAILY_EXP_LIMIT);
        result.put("goldLimit", RedisKeyConstant.DAILY_GOLD_LIMIT);
        result.put("expRemaining", RedisKeyConstant.DAILY_EXP_LIMIT - todayExp);
        result.put("goldRemaining", RedisKeyConstant.DAILY_GOLD_LIMIT - todayGold);
        
        return Result.success(result);
    }

    /**
     * 校验出招冷却时间
     * 返回true表示可以出招，false表示冷却中
     */
    @Override
    public boolean checkActionCooldown(Long userId) {
        String cooldownKey = RedisKeyConstant.buildCooldownKey(userId);
        String lastActionTime = stringRedisTemplate.opsForValue().get(cooldownKey);
        
        if (lastActionTime == null) {
            return true;
        }
        
        long lastTime = Long.parseLong(lastActionTime);
        long currentTime = System.currentTimeMillis();
        
        return (currentTime - lastTime) >= RedisKeyConstant.MIN_ACTION_INTERVAL_MS;
    }

    /**
     * 更新出招冷却时间
     */
    @Override
    public void updateActionCooldown(Long userId) {
        String cooldownKey = RedisKeyConstant.buildCooldownKey(userId);
        stringRedisTemplate.opsForValue().set(cooldownKey, 
                String.valueOf(System.currentTimeMillis()), 
                Duration.ofSeconds(RedisKeyConstant.COOLDOWN_EXPIRE_SEC));
    }

    /**
     * 检查并扣除每日收益配额
     * 返回实际可获得的 [经验, 金币]
     */
    @Override
    public int[] checkAndDeductDailyLimit(Long userId, Integer[] rewards) {
        int exp = rewards[0];
        int gold = rewards[1];
        
        String expKey = RedisKeyConstant.buildDailyExpKey(userId);
        String goldKey = RedisKeyConstant.buildDailyGoldKey(userId);
        
        // 获取今日已获得收益
        String expStr = stringRedisTemplate.opsForValue().get(expKey);
        String goldStr = stringRedisTemplate.opsForValue().get(goldKey);
        
        int currentExp = expStr != null ? Integer.parseInt(expStr) : 0;
        int currentGold = goldStr != null ? Integer.parseInt(goldStr) : 0;
        
        // 计算实际可获得的收益
        int actualExp = Math.min(exp, RedisKeyConstant.DAILY_EXP_LIMIT - currentExp);
        int actualGold = Math.min(gold, RedisKeyConstant.DAILY_GOLD_LIMIT - currentGold);
        
        // 不能为负数
        actualExp = Math.max(actualExp, 0);
        actualGold = Math.max(actualGold, 0);
        
        // 更新Redis中的每日收益（使用increment原子操作）
        if (actualExp > 0) {
            stringRedisTemplate.opsForValue().increment(expKey, actualExp);
            stringRedisTemplate.expire(expKey, Duration.ofSeconds(RedisKeyConstant.getSecondsUntilMidnight()));
        }
        if (actualGold > 0) {
            stringRedisTemplate.opsForValue().increment(goldKey, actualGold);
            stringRedisTemplate.expire(goldKey, Duration.ofSeconds(RedisKeyConstant.getSecondsUntilMidnight()));
        }
        
        return new int[]{actualExp, actualGold};
    }
    
    /**
     * 记录战斗胜利标记（内部方法，供战斗胜利时调用）
     */
    private void recordBattleVictory(Long userId, Integer levelId, String battleId) {
        String victoryKey = RedisKeyConstant.buildBattleVictoryKey(userId, levelId);
        stringRedisTemplate.opsForValue().set(victoryKey, battleId, 
                Duration.ofSeconds(RedisKeyConstant.getSecondsUntilMidnight()));
    }
}