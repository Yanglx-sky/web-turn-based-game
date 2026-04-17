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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
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
    private cn.iocoder.gamemodules.mapper.MonsterSkillMapper monsterSkillMapper;
    
    @Autowired
    private BattleLogMapper battleLogMapper;
    
    @Autowired
    private BattleSecurityInterceptor battleSecurityInterceptor;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private cn.iocoder.gamemodules.service.AchievementService achievementService;
    
    @Autowired
    private cn.iocoder.gameai.service.AIService aiService;
    
    // ==================== Redis相关 ====================
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Result<Map<String, Object>> startBattle(Long userId, Integer levelId) {
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

        // 查询玩家配置的出战精灵列表（按fight_order排序）
        List<UserElf> battleElves = userElfMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserElf>()
                .eq("user_id", userId)
                .gt("fight_order", 0)
                .orderByAsc("fight_order")
        );

        if (battleElves.isEmpty()) {
            return Result.error("请先设置出战精灵");
        }

        // 初始化所有出战精灵状态到battle_record_elf
        UserElf firstElf = null;
        Elf firstElfTemplate = null;
        for (int i = 0; i < battleElves.size(); i++) {
            UserElf userElf = battleElves.get(i);
            BattleRecordElf battleElf = new BattleRecordElf();
            battleElf.setBattleId(battleId);
            battleElf.setElfId(userElf.getId());
            battleElf.setCurrentHp(userElf.getMaxHp());  // 战斗开始时满血
            battleElf.setCurrentMp(userElf.getMaxMp());  // 战斗开始时满蓝
            // 第一只为战斗中，其他为可上场
            battleElf.setElfState(i == 0 ? 0 : 1); // 0=战斗中, 1=可上场
            battleElf.setCreateTime(LocalDateTime.now());
            battleElf.setUpdateTime(LocalDateTime.now());
            battleRecordElfMapper.insert(battleElf);

            // 记录第一只精灵用于返回
            if (i == 0) {
                firstElf = userElf;
                firstElfTemplate = elfMapper.selectById(userElf.getElfId());
            }
        }

        // 初始化怪物状态
        Level level = levelMapper.selectById(levelId);
        if (level != null) {
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

        // 添加第一只精灵信息（当前战斗中）
        if (firstElf != null) {
            Map<String, Object> userElfInfo = new HashMap<>();
            userElfInfo.put("id", firstElf.getId());
            userElfInfo.put("elfId", firstElf.getElfId());
            userElfInfo.put("userId", firstElf.getUserId());
            userElfInfo.put("level", firstElf.getLevel());
            userElfInfo.put("exp", firstElf.getExp());
            userElfInfo.put("hp", firstElf.getMaxHp()); // 战斗开始时满血
            userElfInfo.put("maxHp", firstElf.getMaxHp());
            userElfInfo.put("mp", firstElf.getMaxMp()); // 战斗开始时满蓝
            userElfInfo.put("maxMp", firstElf.getMaxMp());
            userElfInfo.put("attack", firstElf.getAttack());
            userElfInfo.put("defense", firstElf.getDefense());
            userElfInfo.put("speed", firstElf.getSpeed());

            if (firstElfTemplate != null) {
                userElfInfo.put("elementType", firstElfTemplate.getElementType());
                userElfInfo.put("elfName", firstElfTemplate.getElfName());
                result.put("elfElementType", firstElfTemplate.getElementType());
                result.put("elfName", firstElfTemplate.getElfName());
            }

            result.put("userElf", userElfInfo);
            result.put("playerElfHp", firstElf.getMaxHp());
            result.put("elfMp", firstElf.getMaxMp());
        }

        // 添加所有出战精灵列表
        List<Map<String, Object>> allElvesInfo = new ArrayList<>();
        for (UserElf userElf : battleElves) {
            Map<String, Object> elfInfo = new HashMap<>();
            elfInfo.put("id", userElf.getId());
            elfInfo.put("elfId", userElf.getElfId());
            elfInfo.put("level", userElf.getLevel());
            elfInfo.put("maxHp", userElf.getMaxHp());
            elfInfo.put("maxMp", userElf.getMaxMp());
            elfInfo.put("attack", userElf.getAttack());
            elfInfo.put("defense", userElf.getDefense());
            elfInfo.put("speed", userElf.getSpeed());
            elfInfo.put("fightOrder", userElf.getFightOrder());

            Elf elf = elfMapper.selectById(userElf.getElfId());
            if (elf != null) {
                elfInfo.put("elementType", elf.getElementType());
                elfInfo.put("elfName", elf.getElfName());
            }
            allElvesInfo.add(elfInfo);
        }
        result.put("allBattleElves", allElvesInfo);

        // 添加monster信息
        if (level != null) {
            Monster monster = monsterMapper.selectById(level.getMonsterId());
            if (monster != null) {
                Map<String, Object> monsterInfo = new HashMap<>();
                monsterInfo.put("id", monster.getId());
                monsterInfo.put("monsterId", monster.getId());
                monsterInfo.put("name", monster.getMonsterName());
                monsterInfo.put("level", 1);
                monsterInfo.put("hp", monster.getHp());
                monsterInfo.put("maxHp", monster.getHp());
                monsterInfo.put("mp", monster.getMp());
                monsterInfo.put("maxMp", monster.getMp());
                monsterInfo.put("attack", monster.getAttack());
                monsterInfo.put("defense", monster.getDefense());
                monsterInfo.put("speed", monster.getSpeed());
                monsterInfo.put("elementType", monster.getElementType());
                result.put("monster", monsterInfo);
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
        return executePlayerAction(userId, "attack", null);
    }
    
    @Override
    @Transactional
    public Result<Map<String, Object>> useSkill(Long userId, Integer skillId) {
        return executePlayerAction(userId, "skill", skillId);
    }
    
    /**
     * 执行玩家行动（普通攻击或使用技能）
     * 只结算玩家攻击部分，不触发怪物反击
     */
    private Result<Map<String, Object>> executePlayerAction(Long userId, String actionType, Integer skillId) {
        try {
            // 1. 查询当前战斗
            BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
            if (battleRecord == null) {
                return Result.error("当前没有进行中的战斗");
            }
            
            // 2. 查询关卡信息
            Level level = levelMapper.selectById(battleRecord.getLevelId());
            
            // 3. 查询精灵战斗记录
            List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
            if (elves.isEmpty()) {
                return Result.error("精灵信息不存在");
            }
            
            // 查找当前战斗中的精灵（elfState=0）
            BattleRecordElf userElfRecord = null;
            for (BattleRecordElf elfRecord : elves) {
                if (elfRecord.getElfState() != null && elfRecord.getElfState() == 0) {
                    userElfRecord = elfRecord;
                    break;
                }
            }
            
            if (userElfRecord == null) {
                return Result.error("当前没有战斗中的精灵");
            }
            
            // 4. 查询怪物战斗记录
            List<BattleRecordMonster> monsters = battleRecordMonsterMapper.selectByBattleId(battleRecord.getBattleId());
            if (monsters.isEmpty()) {
                return Result.error("怪物信息不存在");
            }
            BattleRecordMonster monsterRecord = monsters.get(0);
            
            // 5. 查询用户精灵信息
            UserElf userElf = userElfMapper.selectById(userElfRecord.getElfId());
            if (userElf == null) {
                return Result.error("精灵不存在");
            }
            
            // 6. 查询精灵模板信息
            Elf elf = elfMapper.selectById(userElf.getElfId());
            if (elf == null) {
                return Result.error("精灵模板信息不存在");
            }
            
            // 7. 查询怪物信息
            Monster monster = monsterMapper.selectById(monsterRecord.getMonsterId());
            if (monster == null) {
                return Result.error("怪物不存在");
            }
            
            // 8. 查询技能信息（如果是使用技能）
            Skill skill = null;
            if ("skill".equals(actionType) && skillId != null) {
                skill = skillMapper.selectById(skillId);
                if (skill == null) {
                    return Result.error("技能不存在");
                }
            }
            
            List<String> logs = new ArrayList<>();
            
            // 9. 检查当前精灵是否已死亡（HP<=0或elfState=2），如果是则自动切换
            if (userElfRecord.getCurrentHp() == null || userElfRecord.getCurrentHp() <= 0 
                || (userElfRecord.getElfState() != null && userElfRecord.getElfState() == 2)) {
                // 当前精灵已死亡，需要切换到下一个精灵
                List<BattleRecordElf> allElves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
                BattleRecordElf nextElfRecord = null;
                
                // 查找下一个HP>0且elfState=1的精灵
                boolean foundCurrent = false;
                for (BattleRecordElf elfRecord : allElves) {
                    if (elfRecord.getElfId().equals(userElfRecord.getElfId())) {
                        foundCurrent = true;
                        continue;
                    }
                    if (foundCurrent && elfRecord.getCurrentHp() != null && elfRecord.getCurrentHp() > 0
                        && elfRecord.getElfState() != null && elfRecord.getElfState() == 1) {
                        nextElfRecord = elfRecord;
                        break;
                    }
                }
                
                // 如果没找到，从头开始找
                if (nextElfRecord == null) {
                    for (BattleRecordElf elfRecord : allElves) {
                        if (elfRecord.getCurrentHp() != null && elfRecord.getCurrentHp() > 0
                            && elfRecord.getElfState() != null && elfRecord.getElfState() == 1) {
                            nextElfRecord = elfRecord;
                            break;
                        }
                    }
                }
                
                if (nextElfRecord == null) {
                    // 没有可用的精灵，战斗失败
                    BattleUtils.handleDefeat(battleRecord, battleSecurityInterceptor, userId, battleRecordMapper);
                    logs.add("战斗失败！你的所有出战精灵都被击败了");
                    battleRecordMapper.updateById(battleRecord);
                    
                    Map<String, Object> result = BattleUtils.buildBattleResult(
                            battleRecord, userElfRecord, userElf, elf, monsterRecord, monster, level, false, logs);
                    return Result.success(result);
                }
                
                // 切换到新精灵
                UserElf nextElf = userElfMapper.selectById(nextElfRecord.getElfId());
                Elf nextElfTemplate = elfMapper.selectById(nextElf.getElfId());
                String nextElfName = nextElfTemplate != null ? nextElfTemplate.getElfName() : "未知精灵";
                
                logs.add("新回合开始！");
                logs.add(nextElfName + " 登场！");
                
                // 注意：如果精灵已经死亡（elfState=2），不要修改其状态为1
                // 只需要确保HP和MP为0即可
                if (userElfRecord.getElfState() != null && userElfRecord.getElfState() != 2) {
                    // 精灵还未标记为死亡，现在标记
                    userElfRecord.setElfState(2); // 2=死亡
                }
                userElfRecord.setCurrentHp(0);
                userElfRecord.setCurrentMp(0);
                userElfRecord.setUpdateTime(LocalDateTime.now());
                battleRecordElfMapper.updateById(userElfRecord);
                
                // 将下一个精灵标记为战斗中
                nextElfRecord.setElfState(0); // 0=战斗中
                nextElfRecord.setUpdateTime(LocalDateTime.now());
                battleRecordElfMapper.updateById(nextElfRecord);
                
                // 重新查询新精灵的信息，确保使用数据库中的最新数据
                userElfRecord = battleRecordElfMapper.selectById(nextElfRecord.getId());
                userElf = userElfMapper.selectById(userElfRecord.getElfId());
                elf = elfMapper.selectById(userElf.getElfId());
            }
            
            // 10. 根据速度判断出手顺序
            Integer playerSpeed = userElf.getSpeed();
            Integer monsterSpeed = monster.getSpeed();
            boolean playerFirst = (playerSpeed != null ? playerSpeed : 0) >= (monsterSpeed != null ? monsterSpeed : 0);
            boolean monsterDead = false; // 声明在外部，供后续使用
            
            logs.add("新回合开始！");
            
            if (playerFirst) {
                // 玩家先出手
                logs.add("你的精灵" + elf.getElfName() + "速度更快，先发起攻击！");
                
                // 执行玩家攻击
                monsterDead = executePlayerAttackRound(userId, actionType, skillId, skill,
                        userElfRecord, userElf, elf, monsterRecord, monster, battleRecord, logs);
                
                // 如果怪物死亡，处理胜利逻辑
                if (monsterDead) {
                    log.debug("怪物死亡，处理胜利逻辑, userId={}, levelId={}", userId, level.getId());
                    BattleUtils.handleVictory(battleRecord, userId, level, userElf, userElfMapper,
                            battleSecurityInterceptor, userService);
                    recordBattleVictory(userId, level.getId(), battleRecord.getBattleId());
                    
                    int expReward = level != null && level.getRewardExp() != null ? level.getRewardExp() : 100;
                    int goldReward = level != null && level.getRewardGold() != null ? level.getRewardGold() : 50;
                    String victoryLog = BattleUtils.generateVictoryLog(expReward, goldReward, expReward, goldReward);
                    logs.add(victoryLog);
                } else {
                    // 怪物存活，执行怪物反击
                    boolean playerDead = executeMonsterAttackRound(userId, userElfRecord, userElf, elf,
                            monsterRecord, monster, battleRecord, logs);
                    
                    // 如果玩家死亡，不再自动切换，等待前端确认
                    if (playerDead) {
                        log.debug("玩家精灵死亡，等待前端确认切换");
                        // 不继续执行玩家攻击，直接返回
                    } else {
                        // 玩家存活，重新查询最新的精灵信息
                        userElfRecord = battleRecordElfMapper.selectById(userElfRecord.getId());
                        userElf = userElfMapper.selectById(userElfRecord.getElfId());
                        elf = elfMapper.selectById(userElf.getElfId());
                    }
                }
            } else {
                // 怪物先出手
                logs.add("怪物" + monster.getMonsterName() + "速度更快，先发起攻击！");
                
                // 执行怪物攻击
                boolean playerDead = executeMonsterAttackRound(userId, userElfRecord, userElf, elf,
                        monsterRecord, monster, battleRecord, logs);
                
                // 如果玩家死亡，不再自动切换，等待前端确认
                if (playerDead) {
                    log.debug("玩家精灵死亡，等待前端确认切换");
                    // 不继续执行玩家攻击，直接返回
                } else {
                    // 玩家存活，执行玩家攻击
                    monsterDead = executePlayerAttackRound(userId, actionType, skillId, skill,
                            userElfRecord, userElf, elf, monsterRecord, monster, battleRecord, logs);
                    
                    if (monsterDead) {
                        log.debug("怪物死亡，处理胜利逻辑, userId={}, levelId={}", userId, level.getId());
                        BattleUtils.handleVictory(battleRecord, userId, level, userElf, userElfMapper,
                                battleSecurityInterceptor, userService);
                        recordBattleVictory(userId, level.getId(), battleRecord.getBattleId());
                        
                        int expReward = level != null && level.getRewardExp() != null ? level.getRewardExp() : 100;
                        int goldReward = level != null && level.getRewardGold() != null ? level.getRewardGold() : 50;
                        String victoryLog = BattleUtils.generateVictoryLog(expReward, goldReward, expReward, goldReward);
                        logs.add(victoryLog);
                    }
                }
            }
            
            // 增加回合数
            battleRecord.setCurrentRound(battleRecord.getCurrentRound() + 1);
            battleRecordMapper.updateById(battleRecord);
            
            // 保存战斗日志到数据库
            saveBattleLogs(battleRecord.getBattleId(), battleRecord.getCurrentRound() - 1, logs);
            
            // 11. 构建返回结果
            Map<String, Object> result = BattleUtils.buildBattleResult(
                    battleRecord, userElfRecord, userElf, elf, monsterRecord, monster, level, 
                    monsterRecord.getCurrentHp() <= 0, logs);
            
            // 12. 如果精灵死亡，添加切换标志和下一只精灵信息
            // 注意：如果战斗已经失败（status=2），则不再添加切换标志
            if (battleRecord.getStatus() != 2 && userElfRecord.getCurrentHp() != null && userElfRecord.getCurrentHp() <= 0) {
                System.out.println("[DEBUG] 检查精灵死亡 - userElfRecord.getCurrentHp(): " + userElfRecord.getCurrentHp());
                System.out.println("[DEBUG] 精灵已死亡，查找下一只可用精灵");
                // 查找下一只可用精灵
                List<BattleRecordElf> allElves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
                BattleRecordElf nextElfRecord = null;
                boolean foundDead = false;
                
                for (BattleRecordElf elfRecord : allElves) {
                    if (elfRecord.getElfId().equals(userElfRecord.getElfId())) {
                        foundDead = true;
                        continue;
                    }
                    if (foundDead && elfRecord.getCurrentHp() != null && elfRecord.getCurrentHp() > 0
                        && elfRecord.getElfState() != null && elfRecord.getElfState() == 1) {
                        nextElfRecord = elfRecord;
                        break;
                    }
                }
                
                // 如果没找到，从头开始找
                if (nextElfRecord == null) {
                    for (BattleRecordElf elfRecord : allElves) {
                        if (elfRecord.getCurrentHp() != null && elfRecord.getCurrentHp() > 0
                            && !elfRecord.getElfId().equals(userElfRecord.getElfId())
                            && elfRecord.getElfState() != null && elfRecord.getElfState() == 1) {
                            nextElfRecord = elfRecord;
                            break;
                        }
                    }
                }
                
                if (nextElfRecord != null) {
                    result.put("needSwitch", true);
                    result.put("nextElfId", nextElfRecord.getElfId());
                    result.put("nextElfHp", nextElfRecord.getCurrentHp());
                    result.put("nextElfMp", nextElfRecord.getCurrentMp());
                    System.out.println("[DEBUG] 精灵死亡，返回切换标志, nextElfId=" + nextElfRecord.getElfId());
                } else {
                    // 没有可用精灵，战斗失败（这个情况已经在handlePlayerDeath中处理）
                    System.out.println("[DEBUG] 没有可用精灵，战斗失败（已在handlePlayerDeath处理）");
                }
            }
            
            // 15. 判断是否显示御三家选择
            // 第一关胜利且只有1只精灵，或第二关胜利且只有2只精灵
            if (monsterDead && level != null) {
                int levelId = level.getId();
                long elfCount = userElfMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserElf>()
                        .eq("user_id", userId)
                );
                // 第一关胜利且只有1只精灵，或第二关胜利且只有2只精灵
                boolean showStarterSelection = (levelId == 1 && elfCount == 1) || (levelId == 2 && elfCount == 2);
                result.put("showStarterSelection", showStarterSelection);
            }
            
            return Result.success(result);
            
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 执行怪物行动（由前端在玩家行动后调用）
     * 只结算怪物攻击部分
     */
    @Override
    @Transactional
    public Result<Map<String, Object>> executeMonsterTurn(Long userId) {
        try {
            // 1. 查询当前战斗
            BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
            if (battleRecord == null) {
                return Result.error("当前没有进行中的战斗");
            }
            
            // 2. 查询精灵战斗记录
            List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
            if (elves.isEmpty()) {
                return Result.error("精灵信息不存在");
            }
            
            // 查找当前战斗中的精灵（elfState=0）
            BattleRecordElf userElfRecord = null;
            for (BattleRecordElf elfRecord : elves) {
                if (elfRecord.getElfState() != null && elfRecord.getElfState() == 0) {
                    userElfRecord = elfRecord;
                    break;
                }
            }
            
            if (userElfRecord == null) {
                return Result.error("当前没有战斗中的精灵");
            }
            
            // 3. 查询怪物战斗记录
            List<BattleRecordMonster> monsters = battleRecordMonsterMapper.selectByBattleId(battleRecord.getBattleId());
            if (monsters.isEmpty()) {
                return Result.error("怪物信息不存在");
            }
            BattleRecordMonster monsterRecord = monsters.get(0);
            
            // 4. 查询用户精灵信息
            UserElf userElf = userElfMapper.selectById(userElfRecord.getElfId());
            if (userElf == null) {
                return Result.error("精灵不存在");
            }
            
            // 5. 查询精灵模板信息
            Elf elf = elfMapper.selectById(userElf.getElfId());
            if (elf == null) {
                return Result.error("精灵模板信息不存在");
            }
            
            // 6. 查询怪物信息
            Monster monster = monsterMapper.selectById(monsterRecord.getMonsterId());
            if (monster == null) {
                return Result.error("怪物不存在");
            }
            
            List<String> logs = new ArrayList<>();
            
            // 7. 执行怪物攻击
            Map<String, Object> enemyResult = BattleUtils.executeMonsterAction(
                    userElfRecord, userElf, elf, monsterRecord, monster, 
                    battleRecordElfMapper, monsterSkillMapper, skillMapper, battleRecordMonsterMapper);
            int enemyDamage = (int) enemyResult.get("damage");
            String attackType = (String) enemyResult.get("attackType");
            String enemyAttackLog = BattleUtils.generateEnemyAttackLogWithDetail(monster.getMonsterName(), attackType, enemyDamage);
            logs.add(enemyAttackLog);
            
            // 8. 检查玩家是否死亡
            boolean playerDead = userElfRecord.getCurrentHp() <= 0;
            
            if (playerDead) {
                // 精灵死亡，标记为死亡状态，等待用户确认切换
                // 注意：executeMonsterAction已经设置了elfState=2和HP=0，这里只需要设置MP和更新时间
                userElfRecord.setCurrentMp(0);
                userElfRecord.setUpdateTime(LocalDateTime.now());
                battleRecordElfMapper.updateById(userElfRecord);
                
                // 同步更新userElf表中的HP为0，防止切换页面还能使用
                userElf.setHp(0);
                userElfMapper.updateById(userElf);
                
                logs.add("你的精灵" + elf.getElfName() + "被击败了！");
                
                // 查询是否还有可用的精灵（从数据库获取最新状态）
                List<BattleRecordElf> allElves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
                BattleRecordElf nextElfRecord = null;
                
                // 查找下一个HP>0且elfState=1的精灵
                boolean foundCurrent = false;
                for (BattleRecordElf elfRecord : allElves) {
                    if (elfRecord.getElfId().equals(userElfRecord.getElfId())) {
                        foundCurrent = true;
                        continue;
                    }
                    if (foundCurrent && elfRecord.getCurrentHp() != null && elfRecord.getCurrentHp() > 0
                        && elfRecord.getElfState() != null && elfRecord.getElfState() == 1) {
                        nextElfRecord = elfRecord;
                        break;
                    }
                }
                
                // 如果没找到，从头开始找
                if (nextElfRecord == null) {
                    for (BattleRecordElf elfRecord : allElves) {
                        if (elfRecord.getCurrentHp() != null && elfRecord.getCurrentHp() > 0
                            && elfRecord.getElfState() != null && elfRecord.getElfState() == 1) {
                            nextElfRecord = elfRecord;
                            break;
                        }
                    }
                }
                
                if (nextElfRecord == null) {
                    // 没有可用的精灵，战斗失败
                    BattleUtils.handleDefeat(battleRecord, battleSecurityInterceptor, userId, battleRecordMapper);
                    logs.add("战斗失败！你的所有出战精灵都被击败了");
                    
                    Map<String, Object> result = BattleUtils.buildBattleResult(
                            battleRecord, userElfRecord, userElf, elf, monsterRecord, monster, null, false, logs);
                    return Result.success(result);
                }
                
                // 有可用精灵，等待用户确认切换
                logs.add("请确认切换下一只精灵");
                
                // 增加回合数
                battleRecord.setCurrentRound(battleRecord.getCurrentRound() + 1);
                battleRecordMapper.updateById(battleRecord);
                
                // 注意：battle_record_elf.elfId存储的是user_elf的ID，不是elf表的ID
                // 所以nextElfRecord.getElfId()就是userElf的ID
                Long nextUserElfId = nextElfRecord.getElfId();
                
                // 返回结果，标记需要用户确认切换
                Map<String, Object> result = BattleUtils.buildBattleResult(
                        battleRecord, userElfRecord, userElf, elf, monsterRecord, monster, null, false, logs);
                result.put("needSwitch", true); // 标记需要切换精灵
                result.put("nextElfId", nextUserElfId); // 下一只精灵的userElf ID
                result.put("nextElfHp", nextElfRecord.getCurrentHp()); // 下一只精灵的HP
                result.put("nextElfMp", nextElfRecord.getCurrentMp()); // 下一只精灵的MP
                return Result.success(result);
            }
            
            // 9. 增加回合数
            battleRecord.setCurrentRound(battleRecord.getCurrentRound() + 1);
            battleRecordMapper.updateById(battleRecord);
            
            // 10. 构建返回结果
            Map<String, Object> result = BattleUtils.buildBattleResult(
                    battleRecord, userElfRecord, userElf, elf, monsterRecord, monster, null, false, logs);
            
            return Result.success(result);
            
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 执行玩家攻击回合
     * @return 怪物是否死亡
     */
    private boolean executePlayerAttackRound(Long userId, String actionType, Integer skillId, Skill skill,
            BattleRecordElf userElfRecord, UserElf userElf, Elf elf,
            BattleRecordMonster monsterRecord, Monster monster, BattleRecord battleRecord, List<String> logs) {
        
        int damage;
        String skillName = null;
        
        if ("attack".equals(actionType)) {
            damage = BattleUtils.executePlayerAttack("attack", null, userElfRecord, userElf, elf, monster, skillMapper);
        } else {
            damage = BattleUtils.executePlayerAttack("skill", skillId, userElfRecord, userElf, elf, monster, skillMapper);
            skillName = skill != null ? skill.getSkillName() : null;
            // 持久化MP扣除到数据库
            userElfRecord.setUpdateTime(LocalDateTime.now());
            battleRecordElfMapper.updateById(userElfRecord);
        }
        
        // 更新怪物HP
        int originalMonsterHp = monsterRecord.getCurrentHp();
        monsterRecord.setCurrentHp(Math.max(0, monsterRecord.getCurrentHp() - damage));
        monsterRecord.setUpdateTime(LocalDateTime.now());
        battleRecordMonsterMapper.updateById(monsterRecord);
        
        // 生成日志
        String attackLog = BattleUtils.generateAttackLog(elf.getElfName(), actionType, skillName, damage);
        logs.add(attackLog);
        logs.add(String.format("敌人HP: %d/%d → %d/%d", 
                originalMonsterHp, monster.getHp(), monsterRecord.getCurrentHp(), monster.getHp()));
        
        return monsterRecord.getCurrentHp() <= 0;
    }
    
    /**
     * 执行怪物攻击回合
     * @return 玩家是否死亡（如果切换了精灵则返回false）
     */
    private boolean executeMonsterAttackRound(Long userId, BattleRecordElf userElfRecord, UserElf userElf, Elf elf,
            BattleRecordMonster monsterRecord, Monster monster, BattleRecord battleRecord, List<String> logs) {
        
        Map<String, Object> enemyResult = BattleUtils.executeMonsterAction(
                userElfRecord, userElf, elf, monsterRecord, monster, 
                battleRecordElfMapper, monsterSkillMapper, skillMapper, battleRecordMonsterMapper);
        int enemyDamage = (int) enemyResult.get("damage");
        String attackType = (String) enemyResult.get("attackType");
        String enemyAttackLog = BattleUtils.generateEnemyAttackLogWithDetail(monster.getMonsterName(), attackType, enemyDamage);
        logs.add(enemyAttackLog);
        
        // 检查玩家是否死亡
        if (userElfRecord.getCurrentHp() > 0) {
            return false; // 玩家存活
        }
        
        // 玩家死亡，尝试切换精灵
        return handlePlayerDeath(userId, userElfRecord, userElf, elf, battleRecord, logs);
    }
    
    /**
     * 处理玩家精灵死亡，标记死亡状态，检查是否所有精灵都死亡
     * @return true=精灵已死亡（可能需要切换或战斗失败），false=精灵存活
     */
    private boolean handlePlayerDeath(Long userId, BattleRecordElf userElfRecord, UserElf userElf, Elf elf,
            BattleRecord battleRecord, List<String> logs) {
        
        Long deadElfId = userElfRecord.getElfId(); // 保存当前死亡的精灵ID
        
        // 将当前精灵的HP设置为0并标记为死亡
        userElfRecord.setCurrentHp(0);
        userElfRecord.setCurrentMp(0);
        userElfRecord.setElfState(2); // 2=死亡
        userElfRecord.setUpdateTime(LocalDateTime.now());
        battleRecordElfMapper.updateById(userElfRecord);
        
        // 同步更新userElf表中的HP为0
        userElf.setHp(0);
        userElfMapper.updateById(userElf);
        
        log.debug("精灵死亡, elfId={}, battleId={}", deadElfId, battleRecord.getBattleId());
        logs.add("你的精灵" + elf.getElfName() + "被击败了！");
        
        // 检查是否还有可用的精灵
        List<BattleRecordElf> allElves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
        boolean hasAvailableElf = false;
        
        for (BattleRecordElf elfRecord : allElves) {
            if (elfRecord.getCurrentHp() != null && elfRecord.getCurrentHp() > 0
                && elfRecord.getElfState() != null && elfRecord.getElfState() == 1) {
                hasAvailableElf = true;
                break;
            }
        }
        
        if (!hasAvailableElf) {
            // 所有精灵都死亡，战斗失败
            log.debug("所有出战精灵都死亡，战斗失败, userId={}, battleId={}", userId, battleRecord.getBattleId());
            BattleUtils.handleDefeat(battleRecord, battleSecurityInterceptor, userId, battleRecordMapper);
            logs.add("战斗失败！你的所有出战精灵都被击败了");
            battleRecordMapper.updateById(battleRecord);
            return true; // 返回true表示战斗结束
        }
        
        // 还有可用精灵，等待前端确认切换
        logs.add("请确认切换下一只精灵");
        return true; // 返回true表示精灵已死亡，需要前端处理切换
    }



    @Override
    @Transactional
    public Result<Map<String, Object>> switchElf(Long userId, Long elfId) {
        // 根据userId查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            return Result.error("当前没有进行中的战斗");
        }

        // 查询该战斗中所有的精灵记录
        List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
        if (elves.isEmpty()) {
            return Result.error("精灵信息不存在");
        }

        // 获取当前战斗中的精灵（elfState=0）
        // 注意：在精灵死亡切换的场景中，当前精灵可能已经是elfState=2（死亡）
        BattleRecordElf currentElfRecord = null;
        for (BattleRecordElf elfRecord : elves) {
            if (elfRecord.getElfState() != null && (elfRecord.getElfState() == 0 || elfRecord.getElfState() == 2)) {
                currentElfRecord = elfRecord;
                break;
            }
        }
        
        if (currentElfRecord == null) {
            return Result.error("当前没有战斗中的精灵");
        }
        
        // 验证目标精灵是否在出战列表中
        BattleRecordElf targetElfRecord = null;
        for (BattleRecordElf elfRecord : elves) {
            if (elfRecord.getElfId().equals(elfId)) {
                targetElfRecord = elfRecord;
                break;
            }
        }

        if (targetElfRecord == null) {
            return Result.error("该精灵不在出战列表中");
        }

        // 验证目标精灵是否存活且可上场（elfState=1）
        if (targetElfRecord.getCurrentHp() == null || targetElfRecord.getCurrentHp() <= 0
            || targetElfRecord.getElfState() == null || targetElfRecord.getElfState() != 1) {
            return Result.error("该精灵已死亡或不可上场，无法切换");
        }

        // 不能切换到当前精灵
        if (currentElfRecord.getElfId().equals(elfId)) {
            return Result.error("已经在战斗中");
        }

        // 查询用户的精灵信息
        UserElf targetUserElf = userElfMapper.selectById(elfId);
        if (targetUserElf == null || !targetUserElf.getUserId().equals(userId)) {
            return Result.error("精灵不存在或不属于当前用户");
        }

        // 查询精灵模板信息
        Elf targetElfTemplate = elfMapper.selectById(targetUserElf.getElfId());
        if (targetElfTemplate == null) {
            return Result.error("精灵信息不存在");
        }

        // 更新原精灵状态为可上场（如果原精灵已死亡，则不修改其状态）
        if (currentElfRecord.getElfState() != null && currentElfRecord.getElfState() != 2) {
            // 只有活着的精灵才能设置为可上场
            currentElfRecord.setElfState(1); // 1=可上场
            currentElfRecord.setUpdateTime(LocalDateTime.now());
            battleRecordElfMapper.updateById(currentElfRecord);
        }

        // 更新目标精灵状态为战斗中
        targetElfRecord.setElfState(0); // 0=战斗中
        targetElfRecord.setUpdateTime(LocalDateTime.now());
        battleRecordElfMapper.updateById(targetElfRecord);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", 0); // 0=战斗中
        
        // 构建精灵信息（使用HashMap避免Map.of的10个键值对限制）
        Map<String, Object> elfData = new HashMap<>();
        elfData.put("id", targetUserElf.getId());
        elfData.put("elfId", targetUserElf.getElfId());
        elfData.put("hp", targetElfRecord.getCurrentHp());
        elfData.put("maxHp", targetUserElf.getMaxHp());
        elfData.put("mp", targetElfRecord.getCurrentMp());
        elfData.put("maxMp", targetUserElf.getMaxMp());
        elfData.put("level", targetUserElf.getLevel());
        elfData.put("attack", targetUserElf.getAttack());
        elfData.put("defense", targetUserElf.getDefense());
        elfData.put("normalDamage", targetUserElf.getNormalDamage());
        elfData.put("speed", targetUserElf.getSpeed());
        elfData.put("elementType", targetElfTemplate.getElementType());
        
        result.put("elf", elfData);
        // 添加精灵名字和系别到结果顶层，方便前端直接使用
        result.put("elfName", targetElfTemplate.getElfName());
        result.put("elfElementType", targetElfTemplate.getElementType());

        return Result.success(result);
    }

    @Override
    public Result<List<Map<String, Object>>> getBattleElves(Long userId) {
        // 查询当前战斗
        BattleRecord battleRecord = battleRecordMapper.selectCurrentBattleByUserId(userId);
        if (battleRecord == null) {
            return Result.error("当前没有进行中的战斗");
        }

        // 从battle_record_elf查询战斗中的精灵
        List<BattleRecordElf> battleElves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
        if (battleElves == null || battleElves.isEmpty()) {
            return Result.error("精灵信息不存在");
        }

        // 转换为包含精灵名字的Map列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (BattleRecordElf battleElf : battleElves) {
            // 过滤掉已死亡的精灵（HP<=0或elfState=2）
            if (battleElf.getCurrentHp() == null || battleElf.getCurrentHp() <= 0
                || battleElf.getElfState() != null && battleElf.getElfState() == 2) {
                continue;
            }

            // 注意：battle_record_elf.elfId存储的是user_elf的ID，不是elf表的ID
            // 所以直接用selectById查询userElf
            UserElf userElf = userElfMapper.selectById(battleElf.getElfId());
            if (userElf == null) {
                continue;
            }

            // 查询精灵模板信息
            Elf elf = elfMapper.selectById(userElf.getElfId());
            if (elf == null) {
                continue;
            }

            Map<String, Object> elfMap = new HashMap<>();
            elfMap.put("id", userElf.getId());
            elfMap.put("userId", userElf.getUserId());
            elfMap.put("elfId", userElf.getElfId());
            elfMap.put("level", userElf.getLevel());
            elfMap.put("exp", userElf.getExp());
            elfMap.put("expNeed", userElf.getExpNeed());
            elfMap.put("maxHp", userElf.getMaxHp());
            elfMap.put("maxMp", userElf.getMaxMp());
            elfMap.put("hp", battleElf.getCurrentHp()); // 使用battle_record_elf中的HP
            elfMap.put("mp", battleElf.getCurrentMp()); // 使用battle_record_elf中的MP
            elfMap.put("attack", userElf.getAttack());
            elfMap.put("defense", userElf.getDefense());
            elfMap.put("normalDamage", userElf.getNormalDamage());
            elfMap.put("speed", userElf.getSpeed());
            elfMap.put("isFight", userElf.getIsFight());
            elfMap.put("fightOrder", userElf.getFightOrder());
            elfMap.put("status", userElf.getStatus());
            elfMap.put("elfState", battleElf.getElfState()); // 战斗中的状态

            // 添加精灵名字和系别
            elfMap.put("elfName", elf.getElfName());
            elfMap.put("elementType", elf.getElementType());

            result.add(elfMap);
        }

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
        log.info("领取战斗奖励, userId={}, levelId={}, battleId={}", userId, levelId, battleId);
        
        // 1. 校验战斗胜利标记（必须战斗胜利）
        String victoryKey = RedisKeyConstant.buildBattleVictoryKey(userId, levelId);
        String victoryBattleId = stringRedisTemplate.opsForValue().get(victoryKey);
        
        if (victoryBattleId == null || !victoryBattleId.equals(battleId)) {
            log.warn("奖励领取验证失败, userId={}, victoryBattleId={}, expectedBattleId={}", 
                userId, victoryBattleId, battleId);
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
            // 查询所有出战精灵并增加经验（每只出战精灵都获得完整经验，包括死亡的精灵）
            // 注意：战斗胜利后status=1，不能使用selectCurrentBattleByUserId查询
            // 应该通过battleId直接查询
            QueryWrapper<BattleRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("battle_id", battleId);
            BattleRecord battleRecord = battleRecordMapper.selectOne(wrapper);
                    
            if (battleRecord != null) {
                List<BattleRecordElf> elves = battleRecordElfMapper.selectByBattleId(battleRecord.getBattleId());
                        
                if (!elves.isEmpty()) {
                    log.debug("开始发放经验, 出战精灵数量={}", elves.size());
                            
                    // 遍历所有出战精灵，每只精灵都获得完整的经验奖励
                    for (BattleRecordElf elfRecord : elves) {
                        UserElf userElf = userElfMapper.selectById(elfRecord.getElfId());
                        if (userElf != null) {
                            userElf.setExp(userElf.getExp() + actualExp);
                                    
                            // 检查是否可以升级（可能连续升多级）
                            int levelUpCount = 0;
                            while (userElf.getExp() >= userElf.getExpNeed() && userElf.getLevel() < 10) {
                                levelUpCount++;
                                        
                                // 升级处理
                                userElf.setLevel(userElf.getLevel() + 1);
                                userElf.setExp(userElf.getExp() - userElf.getExpNeed());
                                userElf.setExpNeed(100L); // 经验需求怛为100
                                // 属性提升
                                userElf.setMaxHp(userElf.getMaxHp() + 20);
                                userElf.setMaxMp(userElf.getMaxMp() + 10);
                                userElf.setAttack(userElf.getAttack() + 5);
                                userElf.setDefense(userElf.getDefense() + 3);
                                userElf.setNormalDamage(userElf.getNormalDamage() + 2);
                                userElf.setSpeed(userElf.getSpeed() + 2);
                                // 满血满蓝
                                userElf.setHp(userElf.getMaxHp());
                                userElf.setMp(userElf.getMaxMp());
                            }
                                    
                            if (levelUpCount > 0) {
                                log.info("精灵升级, elfId={}, 升级{}级, 最终等级={}", 
                                    userElf.getId(), levelUpCount, userElf.getLevel());
                            }
                                    
                            userElfMapper.updateById(userElf);
                        }
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
        
        // 7. 清理用户战斗状态（在奖励领取完成后清理）
        battleSecurityInterceptor.updateBattleStatus(userId, false);
        
        // 8. 构建返回结果
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
        log.info("[胜利标记调试] recordBattleVictory方法被调用");
        String victoryKey = RedisKeyConstant.buildBattleVictoryKey(userId, levelId);
        log.info("[胜利标记调试] victoryKey={}, battleId={}", victoryKey, battleId);
        stringRedisTemplate.opsForValue().set(victoryKey, battleId, 
                Duration.ofSeconds(RedisKeyConstant.getSecondsUntilMidnight()));
        log.info("[胜利标记调试] Redis设置完成");
    }
    
    @Override
    public Result<Map<String, Object>> getBattleSummary(Long userId) {
        try {
            // 查询用户最近的已结束战斗（status=1胜利 或 2失败）
            List<BattleRecord> recentBattles = battleRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<BattleRecord>()
                    .eq("user_id", userId)
                    .in("status", 1, 2) // 1=胜利, 2=失败
                    .orderByDesc("create_time")
                    .last("LIMIT 1")
            );
            
            if (recentBattles == null || recentBattles.isEmpty()) {
                return Result.error("没有已结束的战斗记录");
            }
            
            BattleRecord battleRecord = recentBattles.get(0);
            String battleId = battleRecord.getBattleId();
            
            // 从battle_log表查询完整的战斗日志
            List<BattleLog> battleLogs = battleLogMapper.selectByBattleId(battleId);
            
            if (battleLogs == null || battleLogs.isEmpty()) {
                return Result.error("战斗日志不存在");
            }
            
            // 构建完整的战斗日志文本
            StringBuilder battleLogText = new StringBuilder();
            battleLogText.append("战斗ID: ").append(battleId).append("\n");
            battleLogText.append("关卡ID: ").append(battleRecord.getLevelId()).append("\n");
            battleLogText.append("总回合数: ").append(battleRecord.getCurrentRound()).append("\n\n");
            battleLogText.append("=== 战斗过程 ===\n");
            
            int currentRound = 0;
            for (BattleLog log : battleLogs) {
                // 如果是新回合，添加回合分隔线
                if (!log.getRound().equals(currentRound)) {
                    currentRound = log.getRound();
                    battleLogText.append("\n--- 第").append(currentRound).append("回合 ---\n");
                }
                battleLogText.append(log.getLogText()).append("\n");
            }
            
            String battleResult = battleRecord.getStatus() == 1 ? "胜利" : "失败";
            battleLogText.append("\n=== 战斗结果 ===\n");
            battleLogText.append(battleResult);
            
            // 调用AI服务生成总结
            String aiReport = aiService.getBattleSummary(battleLogText.toString(), battleResult);
            
            Map<String, Object> result = new HashMap<>();
            result.put("summary", aiReport);
            result.put("battleId", battleId);
            result.put("levelId", battleRecord.getLevelId());
            result.put("totalRounds", battleRecord.getCurrentRound());
            result.put("battleResult", battleResult);
            result.put("logCount", battleLogs.size());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取AI战报总结失败", e);
            return Result.error("获取AI战报总结失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存战斗日志到数据库
     * @param battleId 战斗ID
     * @param round 回合数
     * @param logs 日志列表
     */
    private void saveBattleLogs(String battleId, Integer round, List<String> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        
        try {
            LocalDateTime now = LocalDateTime.now();
            for (String logText : logs) {
                BattleLog battleLog = new BattleLog();
                battleLog.setBattleId(battleId);
                battleLog.setRound(round);
                battleLog.setLogText(logText);
                battleLog.setCreateTime(now);
                battleLogMapper.insert(battleLog);
            }
            log.debug("保存战斗日志成功, battleId={}, round={}, count={}", battleId, round, logs.size());
        } catch (Exception e) {
            log.error("保存战斗日志失败, battleId={}, round={}", battleId, round, e);
        }
    }
    
    @Override
    public Result<List<Map<String, Object>>> getBattleLogs(String battleId) {
        try {
            // 查询战斗日志
            List<BattleLog> battleLogs = battleLogMapper.selectByBattleId(battleId);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = new ArrayList<>();
            for (BattleLog log : battleLogs) {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("id", log.getId());
                logMap.put("round", log.getRound());
                logMap.put("logText", log.getLogText());
                logMap.put("createTime", log.getCreateTime());
                result.add(logMap);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取战斗日志失败, battleId={}", battleId, e);
            return Result.error("获取战斗日志失败: " + e.getMessage());
        }
    }
}