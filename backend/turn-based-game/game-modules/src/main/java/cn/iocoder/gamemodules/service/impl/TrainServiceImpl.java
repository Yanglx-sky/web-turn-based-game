package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gameai.service.AIService;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.*;
import cn.iocoder.gamemodules.mapper.*;
import cn.iocoder.gamemodules.service.BattleService;
import cn.iocoder.gamemodules.service.TrainService;
import cn.iocoder.gamemodules.service.UserElfService;
import cn.iocoder.gamemodules.util.BattleUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TrainServiceImpl implements TrainService {

    @Autowired
    private TrainMannequinMapper trainMannequinMapper;
    @Autowired
    private TrainRecordMapper trainRecordMapper;
    @Autowired
    private TrainMannequinSkillMapper trainMannequinSkillMapper;
    @Autowired
    private TrainRecordElfMapper trainRecordElfMapper;
    @Autowired
    private UserElfMapper userElfMapper;
    @Autowired
    private SkillMapper skillMapper;
    @Autowired
    private AIService aiService;
    @Autowired
    private UserElfService userElfService;
    @Autowired
    private ElfMapper elfMapper;
    @Autowired
    private BattleService battleService;
    
    @Autowired
    private TrainLogMapper trainLogMapper;
    @Autowired
    private PotionConfigMapper potionConfigMapper;
    @Autowired
    private UserPotionMapper userPotionMapper;

    // 训练战斗状态 - 使用Map存储每个用户的训练状态
    private ConcurrentHashMap<Long, TrainState> userTrainStates = new ConcurrentHashMap<>();

    // 训练状态类
    private static class TrainState {
        String trainId;  // 训练ID
        boolean trainEnded;
        boolean trainWon;
        List<String> trainLog = new ArrayList<>();
        BattleUtils.BattleLogManager battleLogManager = new BattleUtils.BattleLogManager();
        TrainMannequin currentMannequin;
        UserElf currentPlayerElf;
        List<TrainRecordElf> trainRecordElves = new ArrayList<>(); // 所有出战精灵的记录
        int playerElfHp;
        int mannequinHp;
        int mannequinMp;
        boolean elfSwitched; // 标记是否发生了精灵切换
        int currentRound = 1; // 当前回合数
    }

    @Override
    public Result<Map<String, Object>> createMannequin(Long userId, Integer attack, Integer defense, Integer hp, Integer mp, Integer speed, Integer type, Integer isAttack) {
        // 创建训练人偶
        TrainMannequin mannequin = new TrainMannequin();
        mannequin.setUserId(userId);
        mannequin.setAttack(attack);
        mannequin.setDefense(defense);
        mannequin.setHp(hp);
        mannequin.setMp(mp);
        mannequin.setSpeed(speed);
        mannequin.setType(type);
        mannequin.setIsAttack(isAttack);
        trainMannequinMapper.insert(mannequin);

        // 为训练人偶添加对应系别的技能
        QueryWrapper<Skill> skillWrapper = new QueryWrapper<>();
        skillWrapper.eq("element_type", type);
        List<Skill> skills = skillMapper.selectList(skillWrapper);
        for (Skill skill : skills) {
            TrainMannequinSkill mannequinSkill = new TrainMannequinSkill();
            mannequinSkill.setMannequinId(mannequin.getId());
            mannequinSkill.setSkillId(skill.getId());
            trainMannequinSkillMapper.insert(mannequinSkill);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("mannequinId", mannequin.getId());
        res.put("mannequin", mannequin);
        return Result.success(res);
    }

    @Override
    public Result<Map<String, Object>> startTrain(Long userId, Long mannequinId) {
        // 重置训练状态
        TrainState trainState = new TrainState();
        trainState.trainId = java.util.UUID.randomUUID().toString();
        trainState.trainEnded = false;
        trainState.trainWon = false;
        trainState.trainLog.clear();
        trainState.battleLogManager.reset();
        trainState.currentRound = 1;

        // 创建训练记录
        TrainRecord trainRecord = new TrainRecord();
        trainRecord.setTrainId(trainState.trainId);
        trainRecord.setUserId(userId);
        trainRecord.setMannequinId(mannequinId);
        trainRecord.setStatus(0); // 0=战斗中
        trainRecordMapper.insert(trainRecord);

        // 获取训练人偶信息
        trainState.currentMannequin = trainMannequinMapper.selectById(mannequinId);
        if (trainState.currentMannequin == null || !trainState.currentMannequin.getUserId().equals(userId)) {
            return Result.error("训练人偶不存在或不属于该用户");
        }

        // 获取用户的出战精灵
        Result<List<Map<String, Object>>> battleElvesResult = userElfService.getBattleElves(userId);
        if (battleElvesResult.getCode() != 200 || battleElvesResult.getData() == null || battleElvesResult.getData().isEmpty()) {
            return Result.error("获取出战精灵失败");
        }

        // 为每个出战精灵创建train_record_elf记录并插入数据库
        List<TrainRecordElf> trainRecordElves = new ArrayList<>();
        TrainRecordElf firstElfRecord = null;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        for (int i = 0; i < battleElvesResult.getData().size(); i++) {
            Map<String, Object> elfMap = battleElvesResult.getData().get(i);
            Long elfId = ((Number) elfMap.get("id")).longValue();
            UserElf userElf = userElfMapper.selectById(elfId);
            
            if (userElf == null) continue;
            
            // 创建训练记录
            TrainRecordElf trainRecordElf = new TrainRecordElf();
            trainRecordElf.setElfId(elfId);
            trainRecordElf.setMannequinId(mannequinId); // 使用真实的mannequinId
            trainRecordElf.setCurrentHp(userElf.getMaxHp());
            trainRecordElf.setCurrentMp(userElf.getMaxMp());
            trainRecordElf.setElfState(i == 0 ? 0 : 1); // 第一个精灵为战斗中，其他为可上场
            trainRecordElf.setCreateTime(now);
            trainRecordElf.setUpdateTime(now);
            
            // 插入数据库
            trainRecordElfMapper.insert(trainRecordElf);
            
            trainRecordElves.add(trainRecordElf);
            
            // 第一个精灵作为当前战斗精灵
            if (i == 0) {
                firstElfRecord = trainRecordElf;
                trainState.currentPlayerElf = userElf;
            }
        }
        
        if (trainState.currentPlayerElf == null) {
            return Result.error("精灵不存在");
        }
        
        // 保存所有精灵记录到状态中
        trainState.trainRecordElves = trainRecordElves;

        // 初始化生命值和魔法值（精灵进入训练时满血满蓝）
        trainState.playerElfHp = firstElfRecord.getCurrentHp();
        trainState.currentPlayerElf.setHp(firstElfRecord.getCurrentHp());
        trainState.currentPlayerElf.setMp(firstElfRecord.getCurrentMp());
        trainState.mannequinHp = trainState.currentMannequin.getHp();
        trainState.mannequinMp = trainState.currentMannequin.getMp();

        // 记录训练开始
        String startLog = "训练开始！";
        Elf firstElf = elfMapper.selectById(trainState.currentPlayerElf.getElfId());
        String elfName = firstElf != null ? firstElf.getElfName() : "未知精灵";
        String elfLog = "你的精灵: " + elfName + " (HP: " + trainState.playerElfHp + ")";
        String mannequinLog = "训练人偶: " + getMannequinTypeName(trainState.currentMannequin.getType()) + " (HP: " + trainState.mannequinHp + ")";
        trainState.trainLog.add(startLog);
        trainState.trainLog.add(elfLog);
        trainState.trainLog.add(mannequinLog);
        trainState.battleLogManager.addLog(startLog);
        trainState.battleLogManager.addLog(elfLog);
        trainState.battleLogManager.addLog(mannequinLog);

        // 存储训练状态
        userTrainStates.put(userId, trainState);

        Map<String, Object> res = new HashMap<>();
        res.put("trainLog", trainState.trainLog);
        res.put("roundLogs", trainState.battleLogManager.getRoundLogs());
        
        Map<String, Object> firstElfMap = new HashMap<>();
        firstElfMap.put("id", trainState.currentPlayerElf.getId());
        firstElfMap.put("elfName", elfName);
        firstElfMap.put("hp", trainState.playerElfHp);
        firstElfMap.put("maxHp", trainState.currentPlayerElf.getMaxHp());
        firstElfMap.put("mp", trainState.currentPlayerElf.getMp());
        firstElfMap.put("maxMp", trainState.currentPlayerElf.getMaxMp());
        if (firstElf != null) {
            firstElfMap.put("elementType", firstElf.getElementType());
        }
        
        res.put("playerElf", firstElfMap);
        res.put("mannequin", trainState.currentMannequin);
        res.put("playerElfHp", trainState.playerElfHp);
        res.put("elfMp", trainState.currentPlayerElf.getMp());
        res.put("mannequinHp", trainState.mannequinHp);
        res.put("mannequinMp", trainState.mannequinMp);
        return Result.success(res);
    }

    /**
     * 开始训练（直接传递训练人偶属性，不从数据库读取）
     */
    public Result<Map<String, Object>> startTrainWithMannequinParams(Long userId, Integer attack, Integer defense, Integer hp, Integer mp, Integer speed, Integer type, Integer isAttack) {
        // 重置训练状态
        TrainState trainState = new TrainState();
        trainState.trainId = java.util.UUID.randomUUID().toString();
        trainState.trainEnded = false;
        trainState.trainWon = false;
        trainState.trainLog.clear();
        trainState.battleLogManager.reset();
        trainState.currentRound = 1;

        // 创建训练人偶对象并保存到数据库
        TrainMannequin mannequin = new TrainMannequin();
        mannequin.setUserId(userId);
        mannequin.setAttack(attack);
        mannequin.setDefense(defense);
        mannequin.setHp(hp);
        mannequin.setMp(mp);
        mannequin.setSpeed(speed);
        mannequin.setType(type);
        mannequin.setIsAttack(isAttack);
        mannequin.setCreateTime(new java.util.Date());
        mannequin.setUpdateTime(new java.util.Date());
        
        // 插入数据库
        trainMannequinMapper.insert(mannequin);
        
        // 创建训练记录
        TrainRecord trainRecord = new TrainRecord();
        trainRecord.setTrainId(trainState.trainId);
        trainRecord.setUserId(userId);
        trainRecord.setMannequinId(mannequin.getId());
        trainRecord.setStatus(0); // 0=战斗中
        trainRecordMapper.insert(trainRecord);
        
        // 为训练人偶添加对应系别的技能
        QueryWrapper<Skill> skillWrapper = new QueryWrapper<>();
        skillWrapper.eq("element_type", type);
        List<Skill> skills = skillMapper.selectList(skillWrapper);
        for (Skill skill : skills) {
            TrainMannequinSkill mannequinSkill = new TrainMannequinSkill();
            mannequinSkill.setMannequinId(mannequin.getId());
            mannequinSkill.setSkillId(skill.getId());
            trainMannequinSkillMapper.insert(mannequinSkill);
        }
        
        trainState.currentMannequin = mannequin;

        // 获取用户的出战精灵
        Result<List<Map<String, Object>>> battleElvesResult = userElfService.getBattleElves(userId);
        if (battleElvesResult.getCode() != 200 || battleElvesResult.getData() == null || battleElvesResult.getData().isEmpty()) {
            return Result.error("获取出战精灵失败");
        }

        // 为每个出战精灵创建train_record_elf记录并插入数据库
        List<TrainRecordElf> trainRecordElves = new ArrayList<>();
        TrainRecordElf firstElfRecord = null;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        for (int i = 0; i < battleElvesResult.getData().size(); i++) {
            Map<String, Object> elfMap = battleElvesResult.getData().get(i);
            Long elfId = ((Number) elfMap.get("id")).longValue();
            UserElf userElf = userElfMapper.selectById(elfId);
            
            if (userElf == null) continue;
            
            // 创建训练记录
            TrainRecordElf trainRecordElf = new TrainRecordElf();
            trainRecordElf.setElfId(elfId);
            trainRecordElf.setMannequinId(mannequin.getId()); // 使用真实的mannequinId
            trainRecordElf.setCurrentHp(userElf.getMaxHp());
            trainRecordElf.setCurrentMp(userElf.getMaxMp());
            trainRecordElf.setElfState(i == 0 ? 0 : 1); // 第一个精灵为战斗中，其他为可上场
            trainRecordElf.setCreateTime(now);
            trainRecordElf.setUpdateTime(now);
            
            // 插入数据库
            trainRecordElfMapper.insert(trainRecordElf);
            
            trainRecordElves.add(trainRecordElf);
            
            // 第一个精灵作为当前战斗精灵
            if (i == 0) {
                firstElfRecord = trainRecordElf;
                trainState.currentPlayerElf = userElf;
            }
        }
        
        if (trainState.currentPlayerElf == null) {
            return Result.error("精灵不存在");
        }
        
        // 保存所有精灵记录到状态中
        trainState.trainRecordElves = trainRecordElves;

        // 初始化生命值和魔法值（精灵进入训练时满血满蓝）
        trainState.playerElfHp = firstElfRecord.getCurrentHp();
        trainState.currentPlayerElf.setHp(firstElfRecord.getCurrentHp());
        trainState.currentPlayerElf.setMp(firstElfRecord.getCurrentMp());
        trainState.mannequinHp = hp;
        trainState.mannequinMp = mp;

        // 记录训练开始
        String startLog = "训练开始！";
        Map<String, Object> firstElfMap = battleElvesResult.getData().get(0);
        String elfLog = "你的精灵: " + firstElfMap.get("elfName") + " (HP: " + trainState.playerElfHp + ")";
        String mannequinLog = "训练人偶: " + getMannequinTypeName(type) + " (HP: " + trainState.mannequinHp + ")";
        trainState.trainLog.add(startLog);
        trainState.trainLog.add(elfLog);
        trainState.trainLog.add(mannequinLog);
        trainState.battleLogManager.addLog(startLog);
        trainState.battleLogManager.addLog(elfLog);
        trainState.battleLogManager.addLog(mannequinLog);

        // 存储训练状态
        userTrainStates.put(userId, trainState);

        Map<String, Object> res = new HashMap<>();
        res.put("trainLog", trainState.trainLog);
        res.put("roundLogs", trainState.battleLogManager.getRoundLogs());
        res.put("playerElf", firstElfMap);
        res.put("mannequin", trainState.currentMannequin);
        res.put("playerElfHp", trainState.playerElfHp);
        res.put("elfMp", trainState.currentPlayerElf.getMp());
        res.put("mannequinHp", trainState.mannequinHp);
        res.put("mannequinMp", trainState.mannequinMp);
        return Result.success(res);
    }

    @Override
    public Result<Map<String, Object>> normalAttack(Long userId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null || trainState.trainEnded) {
            return Result.error("未进入训练或训练已结束");
        }

        // 检查当前精灵是否已死亡，如果是则自动切换
        if (trainState.playerElfHp <= 0) {
            String elfName = getElfName(trainState.currentPlayerElf);
            trainState.trainLog.add("新回合开始！");
            trainState.battleLogManager.addLog("新回合开始！");
            
            boolean hasNextElf = checkAndPrepareNextElf(userId, trainState);
            
            if (!hasNextElf) {
                trainState.trainEnded = true;
                trainState.trainWon = false;
                String loseLog = "训练失败！你的所有出战精灵都被击败了！";
                trainState.trainLog.add(loseLog);
                trainState.battleLogManager.addLog(loseLog);
                return trainSettlement(userId);
            }
            
            String newElfName = getElfName(trainState.currentPlayerElf);
            trainState.trainLog.add(newElfName + " 登场！");
            trainState.battleLogManager.addLog(newElfName + " 登场！");
            
            // 标记发生了精灵切换，用于前端显示弹窗
            trainState.elfSwitched = true;
        }

        // 开始新回合
        trainState.battleLogManager.startNewRound();

        // 比较速度决定先后手
        int playerSpeed = trainState.currentPlayerElf.getSpeed() != null ? trainState.currentPlayerElf.getSpeed() : 0;
        int mannequinSpeed = trainState.currentMannequin.getSpeed() != null ? trainState.currentMannequin.getSpeed() : 0;
        boolean playerFirst = BattleUtils.isPlayerFirst(playerSpeed, mannequinSpeed);
        
        System.out.println("[DEBUG] 普通攻击-速度比较 - 玩家速度: " + playerSpeed + ", 训练人偶速度: " + mannequinSpeed + ", 玩家先手: " + playerFirst);
        
        if (playerFirst) {
            // 玩家先手：玩家攻击
            int damage = trainState.currentPlayerElf.getNormalDamage() != null ? trainState.currentPlayerElf.getNormalDamage() : 1;
            
            trainState.mannequinHp -= damage;
            String attackLog = BattleUtils.generateAttackLog(
                    getElfName(trainState.currentPlayerElf), 
                    "attack", 
                    null, 
                    damage);
            
            trainState.trainLog.add(attackLog);
            trainState.battleLogManager.addLog(attackLog);

            // 检查训练人偶是否被击败
            if (trainState.mannequinHp <= 0) {
                trainState.mannequinHp = 0;
                trainState.trainEnded = true;
                trainState.trainWon = true;
                String winLog = "训练人偶被击败，训练胜利！";
                trainState.trainLog.add(winLog);
                trainState.battleLogManager.addLog(winLog);
                
                // 更新人偶HP到数据库
                trainState.currentMannequin.setHp(0);
                trainState.currentMannequin.setUpdateTime(new java.util.Date());
                trainMannequinMapper.updateById(trainState.currentMannequin);
                
                return trainSettlement(userId);
            }
        } else {
            // 训练人偶先手：先执行训练人偶行动（如果设置为主动攻击）
            System.out.println("[DEBUG] 训练人偶先手-普通攻击, playerFirst=" + playerFirst + ", isAttack=" + trainState.currentMannequin.getIsAttack());
            if (trainState.currentMannequin.getIsAttack() == 1) {
                executeMannequinAction(trainState);
                
                // 更新人偶MP到数据库
                trainState.currentMannequin.setMp(trainState.mannequinMp);
                trainState.currentMannequin.setUpdateTime(new java.util.Date());
                trainMannequinMapper.updateById(trainState.currentMannequin);
                
                // 检查玩家精灵是否被击败
                if (trainState.playerElfHp <= 0) {
                    trainState.playerElfHp = 0;
                    
                    String elfName = getElfName(trainState.currentPlayerElf);
                    trainState.trainLog.add("你的精灵" + elfName + "被击败了！");
                    trainState.battleLogManager.addLog("你的精灵" + elfName + "被击败了！");
                    
                    // 检查是否还有可用的精灵
                    boolean hasNextElf = checkAndPrepareNextElf(userId, trainState);
                    
                    if (!hasNextElf) {
                        // 没有可用精灵，训练失败
                        trainState.trainEnded = true;
                        trainState.trainWon = false;
                        String loseLog = "训练失败！你的所有出战精灵都被击败了！";
                        trainState.trainLog.add(loseLog);
                        trainState.battleLogManager.addLog(loseLog);
                        
                        return trainSettlement(userId);
                    }
                    
                    // 标记发生了精灵切换，用于前端显示弹窗
                    trainState.elfSwitched = true;
                }
            }
            
            // 玩家仍然执行攻击（即使训练人偶先手）
            int damage = trainState.currentPlayerElf.getNormalDamage() != null ? trainState.currentPlayerElf.getNormalDamage() : 1;
            
            trainState.mannequinHp -= damage;
            String attackLog = BattleUtils.generateAttackLog(
                    getElfName(trainState.currentPlayerElf), 
                    "attack", 
                    null, 
                    damage);
            
            System.out.println("[DEBUG] 训练人偶先手-玩家攻击日志: " + attackLog);
            trainState.trainLog.add(attackLog);
            trainState.battleLogManager.addLog(attackLog);

            // 检查训练人偶是否被击败
            if (trainState.mannequinHp <= 0) {
                trainState.mannequinHp = 0;
                trainState.trainEnded = true;
                trainState.trainWon = true;
                String winLog = "训练人偶被击败，训练胜利！";
                trainState.trainLog.add(winLog);
                trainState.battleLogManager.addLog(winLog);
                
                // 更新人偶HP到数据库
                trainState.currentMannequin.setHp(0);
                trainState.currentMannequin.setUpdateTime(new java.util.Date());
                trainMannequinMapper.updateById(trainState.currentMannequin);
                
                return trainSettlement(userId);
            }
        }

        // 不再自动执行训练人偶反击，等待前端调用executeMannequinTurn（仅当玩家先手时）
        
        // 更新trainRecordElf中的HP到数据库
        TrainRecordElf currentRecord = findTrainRecordElf(trainState, trainState.currentPlayerElf.getId());
        if (currentRecord != null) {
            currentRecord.setCurrentHp(trainState.playerElfHp);
            currentRecord.setUpdateTime(LocalDateTime.now());
            trainRecordElfMapper.updateById(currentRecord);  // 持久化到数据库
        }
        
        // 更新人偶HP到数据库
        trainState.currentMannequin.setHp(trainState.mannequinHp);
        trainState.currentMannequin.setUpdateTime(new java.util.Date());
        trainMannequinMapper.updateById(trainState.currentMannequin);
        
        Map<String, Object> res = buildTrainResult(trainState);
        // 添加标志位，告诉前端是否需要调用executeMannequinTurn
        res.put("needMannequinTurn", playerFirst && trainState.currentMannequin.getIsAttack() == 1);
        return Result.success(res);
    }

    @Override
    public Result<Map<String, Object>> useSkill(Long userId, Integer skillId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null || trainState.trainEnded) {
            return Result.error("未进入训练或训练已结束");
        }
    
        // 检查当前精灵是否已死亡，如果是则自动切换
        if (trainState.playerElfHp <= 0) {
            String elfName = getElfName(trainState.currentPlayerElf);
            trainState.trainLog.add("新回合开始！");
            trainState.battleLogManager.addLog("新回合开始！");
                
            boolean hasNextElf = checkAndPrepareNextElf(userId, trainState);
                
            if (!hasNextElf) {
                trainState.trainEnded = true;
                trainState.trainWon = false;
                String loseLog = "训练失败！你的所有出战精灵都被击败了！";
                trainState.trainLog.add(loseLog);
                trainState.battleLogManager.addLog(loseLog);
                return trainSettlement(userId);
            }
                
            String newElfName = getElfName(trainState.currentPlayerElf);
            trainState.trainLog.add(newElfName + " 登场！");
            trainState.battleLogManager.addLog(newElfName + " 登场！");
                
            // 标记发生了精灵切换，用于前端显示弹窗
            trainState.elfSwitched = true;
        }
    
        // 开始新回合
        trainState.battleLogManager.startNewRound();
    
        // 检查技能是否存在
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            return Result.error("技能不存在");
        }
    
        // 检查 MP是否足够
        if (trainState.currentPlayerElf.getMp() < skill.getCostMp()) {
            return Result.error("MP不足，无法使用技能");
        }
    
        // 比较速度决定先后手
        int playerSpeed = trainState.currentPlayerElf.getSpeed() != null ? trainState.currentPlayerElf.getSpeed() : 0;
        int mannequinSpeed = trainState.currentMannequin.getSpeed() != null ? trainState.currentMannequin.getSpeed() : 0;
        boolean playerFirst = BattleUtils.isPlayerFirst(playerSpeed, mannequinSpeed);
            
        // 计算系别克制
        int playerElement = BattleUtils.getElfElementType(trainState.currentPlayerElf, elfMapper);
        int mannequinElement = trainState.currentMannequin.getType();
            
        // 消耗MP并更新trainRecordElf
        trainState.currentPlayerElf.setMp(trainState.currentPlayerElf.getMp() - skill.getCostMp());
        TrainRecordElf currentRecord = findTrainRecordElf(trainState, trainState.currentPlayerElf.getId());
        if (currentRecord != null) {
            currentRecord.setCurrentMp(trainState.currentPlayerElf.getMp());
            currentRecord.setCurrentHp(trainState.playerElfHp);  // 同时更新HP
            currentRecord.setUpdateTime(LocalDateTime.now());
            trainRecordElfMapper.updateById(currentRecord);  // 持久化到数据库
        }
            
        if (playerFirst) {
            // 玩家先手：玩家使用技能
            int damage = BattleUtils.calculateSkillDamage(skill, trainState.currentPlayerElf.getAttack(), 
                    trainState.currentMannequin.getDefense(), playerElement, mannequinElement);
            double multiplier = BattleUtils.calculateElementMultiplier(playerElement, mannequinElement);
            int finalDamage = (int) (damage * multiplier);
                
            trainState.mannequinHp -= finalDamage;
            String skillLog = BattleUtils.generateAttackLog(
                    getElfName(trainState.currentPlayerElf), 
                    "skill", 
                    skill.getSkillName(), 
                    finalDamage);
                
            if (multiplier > 1) {
                skillLog += "，效果拔群";
            } else if (multiplier < 1) {
                skillLog += "，效果不佳";
            }
                
            trainState.trainLog.add(skillLog);
            trainState.battleLogManager.addLog(skillLog);
    
            // 检查训练人偶是否被击败
            if (trainState.mannequinHp <= 0) {
                trainState.mannequinHp = 0;
                trainState.trainEnded = true;
                trainState.trainWon = true;
                String winLog = "训练人偶被击败，训练胜利！";
                trainState.trainLog.add(winLog);
                trainState.battleLogManager.addLog(winLog);
                    
                // 更新trainRecordElf中的HP到数据库（复用前面的currentRecord变量）
                if (currentRecord != null) {
                    currentRecord.setCurrentHp(trainState.playerElfHp);
                    currentRecord.setUpdateTime(LocalDateTime.now());
                    trainRecordElfMapper.updateById(currentRecord);  // 持久化到数据库
                }
                    
                // 更新人偶HP到数据库
                trainState.currentMannequin.setHp(0);
                trainState.currentMannequin.setUpdateTime(new java.util.Date());
                trainMannequinMapper.updateById(trainState.currentMannequin);
                    
                return trainSettlement(userId);
            }
        } else {
            // 训练人偶先手：先执行训练人偶行动（如果设置为主动攻击）
            if (trainState.currentMannequin.getIsAttack() == 1) {
                executeMannequinAction(trainState);
                    
                // 更新人偶MP到数据库
                trainState.currentMannequin.setMp(trainState.mannequinMp);
                trainState.currentMannequin.setUpdateTime(new java.util.Date());
                trainMannequinMapper.updateById(trainState.currentMannequin);
                    
                // 检查玩家精灵是否被击败
                if (trainState.playerElfHp <= 0) {
                    trainState.playerElfHp = 0;
                        
                    String elfName = getElfName(trainState.currentPlayerElf);
                    trainState.trainLog.add("你的精灵" + elfName + "被击败了！");
                    trainState.battleLogManager.addLog("你的精灵" + elfName + "被击败了！");
                        
                    // 检查是否还有可用的精灵
                    boolean hasNextElf = checkAndPrepareNextElf(userId, trainState);
                        
                    if (!hasNextElf) {
                        // 没有可用精灵，训练失败
                        trainState.trainEnded = true;
                        trainState.trainWon = false;
                        String loseLog = "训练失败！你的所有出战精灵都被击败了！";
                        trainState.trainLog.add(loseLog);
                        trainState.battleLogManager.addLog(loseLog);
                            
                        return trainSettlement(userId);
                    }
                    
                    // 标记发生了精灵切换，用于前端显示弹窗
                    trainState.elfSwitched = true;
                }
            }
                
            // 玩家仍然使用技能（即使训练人偶先手）
            int damage = BattleUtils.calculateSkillDamage(skill, trainState.currentPlayerElf.getAttack(), 
                    trainState.currentMannequin.getDefense(), playerElement, mannequinElement);
            double multiplier = BattleUtils.calculateElementMultiplier(playerElement, mannequinElement);
            int finalDamage = (int) (damage * multiplier);
                
            trainState.mannequinHp -= finalDamage;
            String skillLog = BattleUtils.generateAttackLog(
                    getElfName(trainState.currentPlayerElf), 
                    "skill", 
                    skill.getSkillName(), 
                    finalDamage);
                
            if (multiplier > 1) {
                skillLog += "，效果拔群";
            } else if (multiplier < 1) {
                skillLog += "，效果不佳";
            }
                
            trainState.trainLog.add(skillLog);
            trainState.battleLogManager.addLog(skillLog);
    
            // 检查训练人偶是否被击败
            if (trainState.mannequinHp <= 0) {
                trainState.mannequinHp = 0;
                trainState.trainEnded = true;
                trainState.trainWon = true;
                String winLog = "训练人偶被击败，训练胜利！";
                trainState.trainLog.add(winLog);
                trainState.battleLogManager.addLog(winLog);
                    
                // 更新trainRecordElf中的HP到数据库（复用前面的currentRecord变量）
                if (currentRecord != null) {
                    currentRecord.setCurrentHp(trainState.playerElfHp);
                    currentRecord.setUpdateTime(LocalDateTime.now());
                    trainRecordElfMapper.updateById(currentRecord);  // 持久化到数据库
                }
                    
                // 更新人偶HP到数据库
                trainState.currentMannequin.setHp(0);
                trainState.currentMannequin.setUpdateTime(new java.util.Date());
                trainMannequinMapper.updateById(trainState.currentMannequin);
                    
                return trainSettlement(userId);
            }
        }
    
        // 不再自动执行训练人偶反击，等待前端调用executeMannequinTurn（仅当玩家先手时）
            
        // 更新人偶HP到数据库
        trainState.currentMannequin.setHp(trainState.mannequinHp);
        trainState.currentMannequin.setUpdateTime(new java.util.Date());
        trainMannequinMapper.updateById(trainState.currentMannequin);
            
        Map<String, Object> res = buildTrainResult(trainState);
        // 添加标志位，告诉前端是否需要调用executeMannequinTurn
        res.put("needMannequinTurn", playerFirst && trainState.currentMannequin.getIsAttack() == 1);
        return Result.success(res);
    }

    @Override
    public Result<Map<String, Object>> executeMannequinTurn(Long userId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null || trainState.trainEnded) {
            return Result.error("未进入训练或训练已结束");
        }

        // 训练人偶行动（如果设置为主动攻击）
        if (trainState.currentMannequin.getIsAttack() == 1) {
            executeMannequinAction(trainState);
            
            // 更新人偶MP到数据库
            trainState.currentMannequin.setMp(trainState.mannequinMp);
            trainState.currentMannequin.setUpdateTime(new java.util.Date());
            trainMannequinMapper.updateById(trainState.currentMannequin);
            
            // 更新trainRecordElf中的HP和MP到数据库
            TrainRecordElf currentRecord = findTrainRecordElf(trainState, trainState.currentPlayerElf.getId());
            if (currentRecord != null) {
                currentRecord.setCurrentHp(Math.max(0, trainState.playerElfHp));
                currentRecord.setCurrentMp(trainState.currentPlayerElf.getMp());
                
                // 如果HP<=0，设置elfState为2（死亡）
                if (currentRecord.getCurrentHp() <= 0) {
                    currentRecord.setElfState(2);
                    currentRecord.setCurrentMp(0);
                }
                currentRecord.setUpdateTime(LocalDateTime.now());
                trainRecordElfMapper.updateById(currentRecord);  // 持久化到数据库
            }
            
            // 检查玩家精灵是否被击败
            if (trainState.playerElfHp <= 0) {
                trainState.playerElfHp = 0;
                
                String elfName = getElfName(trainState.currentPlayerElf);
                trainState.trainLog.add("你的精灵" + elfName + "被击败了！");
                trainState.battleLogManager.addLog("你的精灵" + elfName + "被击败了！");
                
                // 检查是否还有可用的精灵
                boolean hasNextElf = checkAndPrepareNextElf(userId, trainState);
                
                if (!hasNextElf) {
                    // 没有可用精灵，训练失败
                    trainState.trainEnded = true;
                    trainState.trainWon = false;
                    String loseLog = "训练失败！你的所有出战精灵都被击败了！";
                    trainState.trainLog.add(loseLog);
                    trainState.battleLogManager.addLog(loseLog);
                    
                    return trainSettlement(userId);
                }
                
                // 标记发生了精灵切换，用于前端显示弹窗
                trainState.elfSwitched = true;
                
                // 有可用精灵，但不立即切换，等待下一回合
                trainState.trainLog.add("回合结束！");
                trainState.battleLogManager.addLog("回合结束！");
            }
        } else {
            // 如果人偶不主动攻击，直接返回
            trainState.trainLog.add("训练人偶没有反击");
            trainState.battleLogManager.addLog("训练人偶没有反击");
        }

        Map<String, Object> res = buildTrainResult(trainState);
        return Result.success(res);
    }

    @Override
    public Result<Map<String, Object>> flee(Long userId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null || trainState.trainEnded) {
            return Result.error("未进入训练或训练已结束");
        }

        // 开始新回合
        trainState.battleLogManager.startNewRound();

        // 记录逃跑
        String fleeLog = "你逃跑了！";
        trainState.trainLog.add(fleeLog);
        trainState.battleLogManager.addLog(fleeLog);
        
        trainState.trainEnded = true;
        trainState.trainWon = false;

        return trainSettlement(userId);
    }

    @Override
    public Result<Map<String, Object>> switchElf(Long userId, Long elfId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null || trainState.trainEnded) {
            return Result.error("未进入训练或训练已结束");
        }

        List<TrainRecordElf> trainRecordElves = trainState.trainRecordElves;
        if (trainRecordElves == null || trainRecordElves.isEmpty()) {
            return Result.error("获取出战精灵失败");
        }

        // 获取当前战斗中的精灵
        Long currentElfId = trainState.currentPlayerElf.getId();
        TrainRecordElf currentRecord = null;
        TrainRecordElf targetRecord = null;
        
        for (TrainRecordElf elfRecord : trainRecordElves) {
            if (elfRecord.getElfId().equals(currentElfId)) {
                currentRecord = elfRecord;
            }
            if (elfRecord.getElfId().equals(elfId)) {
                targetRecord = elfRecord;
            }
        }

        if (targetRecord == null) {
            return Result.error("该精灵不在出战列表中");
        }

        // 验证目标精灵是否存活且可上场（elfState=1）
        if (targetRecord.getCurrentHp() == null || targetRecord.getCurrentHp() <= 0
            || targetRecord.getElfState() == null || targetRecord.getElfState() != 1) {
            return Result.error("该精灵已死亡或不可上场，无法切换");
        }

        // 不能切换到当前精灵
        if (currentElfId.equals(elfId)) {
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
        if (currentRecord != null && currentRecord.getElfState() != null && currentRecord.getElfState() != 2) {
            currentRecord.setElfState(1);
            currentRecord.setUpdateTime(LocalDateTime.now());
            trainRecordElfMapper.updateById(currentRecord);  // 持久化到数据库
        }

        // 更新目标精灵状态为战斗中
        targetRecord.setElfState(0);
        targetRecord.setUpdateTime(LocalDateTime.now());
        trainRecordElfMapper.updateById(targetRecord);  // 持久化到数据库

        // 切换到新精灵
        trainState.currentPlayerElf = targetUserElf;
        trainState.playerElfHp = targetRecord.getCurrentHp();
        // 同步更新MP值，确保使用train_record_elf中的MP而不是user_elf中的旧值
        trainState.currentPlayerElf.setMp(targetRecord.getCurrentMp());
        
        // 构建返回结果
        Map<String, Object> res = buildTrainResult(trainState);
        
        Map<String, Object> elfData = new HashMap<>();
        elfData.put("id", targetUserElf.getId());
        elfData.put("elfId", targetUserElf.getElfId());
        elfData.put("hp", targetRecord.getCurrentHp());
        elfData.put("maxHp", targetUserElf.getMaxHp());
        elfData.put("mp", targetRecord.getCurrentMp());
        elfData.put("maxMp", targetUserElf.getMaxMp());
        elfData.put("level", targetUserElf.getLevel());
        elfData.put("attack", targetUserElf.getAttack());
        elfData.put("defense", targetUserElf.getDefense());
        elfData.put("normalDamage", targetUserElf.getNormalDamage());
        elfData.put("speed", targetUserElf.getSpeed());
        elfData.put("elementType", targetElfTemplate.getElementType());
        
        res.put("elf", elfData);
        res.put("elfName", targetElfTemplate.getElfName());
        res.put("elfElementType", targetElfTemplate.getElementType());
        res.put("msg", "精灵切换成功");

        return Result.success(res);
    }

    @Override
    public Result<Map<String, Object>> trainSettlement(Long userId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null || !trainState.trainEnded) {
            return Result.error("未进入训练或训练未结束");
        }

        // 先保存训练日志到数据库
        saveTrainLogs(trainState);
        
        // 从train_log表查询完整的训练日志
        List<TrainLog> trainLogs = trainLogMapper.selectByTrainId(trainState.trainId);
        
        // 构建完整的训练日志文本
        StringBuilder trainLogText = new StringBuilder();
        trainLogText.append("训练ID: ").append(trainState.trainId).append("\n");
        trainLogText.append("训练人偶: ").append(getMannequinTypeName(trainState.currentMannequin.getType())).append("\n\n");
        trainLogText.append("=== 训练过程 ===\n");
        
        int currentRound = 0;
        for (TrainLog log : trainLogs) {
            // 如果是新回合，添加回合分隔线
            if (!log.getRound().equals(currentRound)) {
                currentRound = log.getRound();
                trainLogText.append("\n--- 第").append(currentRound).append("回合 ---\n");
            }
            trainLogText.append(log.getLogText()).append("\n");
        }
        
        String battleResult = trainState.trainWon ? "胜利" : "失败";
        trainLogText.append("\n=== 训练结果 ===\n");
        trainLogText.append(battleResult);

        // 调用AI服务获取训练评分报告
        String aiReport = aiService.getBattleSummary(trainLogText.toString(), battleResult);

        // 生成AI评分（简单模拟）
        int aiScore = trainState.trainWon ? 80 + (int) (Math.random() * 20) : 40 + (int) (Math.random() * 30);

        // 保存训练记录（更新已有的记录）
        QueryWrapper<TrainRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("train_id", trainState.trainId);
        TrainRecord trainRecord = trainRecordMapper.selectOne(recordWrapper);
        
        if (trainRecord == null) {
            // 如果没有记录，创建新的
            trainRecord = new TrainRecord();
            trainRecord.setTrainId(trainState.trainId);
            trainRecord.setUserId(userId);
            trainRecord.setMannequinId(trainState.currentMannequin.getId());
        }
        
        trainRecord.setAiScore(aiScore);
        trainRecord.setAiReport(aiReport);
        
        // 设置训练状态
        if (trainState.trainWon) {
            trainRecord.setStatus(1); // 1=胜利
        } else {
            trainRecord.setStatus(2); // 2=失败
        }
        
        if (trainRecord.getId() == null) {
            trainRecordMapper.insert(trainRecord);
        } else {
            trainRecordMapper.updateById(trainRecord);
        }

        // 训练胜利时给精灵添加经验
        if (trainState.trainWon) {
            // 给当前训练精灵添加经验
            Long elfId = trainState.currentPlayerElf.getId();
            UserElf elf = userElfService.getById(elfId);
            if (elf != null && elf.getLevel() < 10) {
                // 增加经验（训练获得的经验较少）
                int rewardExp = 50; // 训练获得的经验
                elf.setExp(elf.getExp() + rewardExp);
                userElfService.updateById(elf);
                
                // 循环检查是否可以连续升级
                while (true) {
                    // 重新获取精灵信息，确保包含升级后的状态
                    elf = userElfService.getById(elfId);
                    if (elf == null || elf.getLevel() >= 10) {
                        break;
                    }
                    
                    // 检查是否可以升级
                    if (elf.getExp() >= elf.getExpNeed()) {
                        userElfService.upgradeElf(elf.getId());
                    } else {
                        break;
                    }
                }
                
                // 添加获得经验的日志
                trainState.trainLog.add("获得经验: " + rewardExp);
            }
        }

        // 清理训练状态
        userTrainStates.remove(userId);

        Map<String, Object> res = new HashMap<>();
        res.put("trainLog", trainState.trainLog);
        res.put("roundLogs", trainState.battleLogManager.getRoundLogs());
        res.put("trainResult", trainState.trainLog.contains("你逃跑了！") ? "逃跑" : battleResult);
        res.put("status", trainState.trainWon ? 1 : 2); // 添加status字段：1=胜利 2=失败
        res.put("aiScore", aiScore);
        res.put("aiReport", aiReport);
        return Result.success(res);
    }

    @Override
    public Result<Map<String, Object>> getTrainRecords(Long userId) {
        // 获取用户的训练记录
        QueryWrapper<TrainRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("user_id", userId);
        List<TrainRecord> records = trainRecordMapper.selectList(recordWrapper);

        // 构建返回数据
        List<Map<String, Object>> recordList = new ArrayList<>();
        for (TrainRecord record : records) {
            Map<String, Object> recordMap = new HashMap<>();
            recordMap.put("id", record.getId());
            recordMap.put("mannequinId", record.getMannequinId());
            recordMap.put("aiScore", record.getAiScore());
            recordMap.put("aiReport", record.getAiReport());
            
            // 获取训练人偶信息
            TrainMannequin mannequin = trainMannequinMapper.selectById(record.getMannequinId());
            if (mannequin != null) {
                recordMap.put("mannequinType", getMannequinTypeName(mannequin.getType()));
                recordMap.put("mannequinAttack", mannequin.getAttack());
                recordMap.put("mannequinDefense", mannequin.getDefense());
                recordMap.put("mannequinHp", mannequin.getHp());
            }
            
            recordList.add(recordMap);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("records", recordList);
        return Result.success(res);
    }

    /**
     * 执行训练人偶的行动（技能或普通攻击）
     * 参照BattleUtils.executeMonsterAction实现
     */
    private void executeMannequinAction(TrainState trainState) {
        QueryWrapper<TrainMannequinSkill> skillWrapper = new QueryWrapper<>();
        skillWrapper.eq("mannequin_id", trainState.currentMannequin.getId());
        List<TrainMannequinSkill> mannequinSkills = trainMannequinSkillMapper.selectList(skillWrapper);
        
        int damage = 0;
        String attackType = "普通攻击";
        boolean skillUsed = false;
        
        // 如果有技能且MP足够，优先使用技能
        if (mannequinSkills != null && !mannequinSkills.isEmpty() && trainState.mannequinMp > 0) {
            // 随机选择一个技能
            TrainMannequinSkill mannequinSkill = mannequinSkills.get((int) (Math.random() * mannequinSkills.size()));
            Skill mannequinSkillInfo = skillMapper.selectById(mannequinSkill.getSkillId());
            
            if (mannequinSkillInfo != null && trainState.mannequinMp >= mannequinSkillInfo.getCostMp()) {
                // 使用技能，扣除MP
                trainState.mannequinMp -= mannequinSkillInfo.getCostMp();
                
                // 计算技能伤害（包含系别克制）
                int playerElement = BattleUtils.getElfElementType(trainState.currentPlayerElf, elfMapper);
                int mannequinElement = trainState.currentMannequin.getType();
                
                damage = BattleUtils.calculateSkillDamage(mannequinSkillInfo, 
                        trainState.currentMannequin.getAttack(), 
                        trainState.currentPlayerElf.getDefense(), 
                        mannequinElement, playerElement);
                
                attackType = "技能 " + mannequinSkillInfo.getSkillName();
                skillUsed = true;
                
                System.out.println("[DEBUG] 训练人偶使用技能 - 技能ID:" + mannequinSkillInfo.getId() + 
                                  ", 技能名:" + mannequinSkillInfo.getSkillName() + 
                                  ", MP消耗:" + mannequinSkillInfo.getCostMp() + 
                                  ", 剩余MP:" + trainState.mannequinMp +
                                  ", 造成伤害:" + damage +
                                  ", 人偶系别:" + mannequinElement);
            }
        }
        
        // 如果没有使用技能，则使用普通攻击（真实伤害）
        if (!skillUsed) {
            // 训练人偶使用attack字段作为普通攻击伤害（真实伤害）
            Integer attackDamage = trainState.currentMannequin.getAttack();
            damage = attackDamage != null ? attackDamage : 1;
            attackType = "普通攻击";
            
            System.out.println("[DEBUG] 训练人偶使用普通攻击 - 真实伤害:" + damage);
        }
        
        // 扣除玩家HP
        trainState.playerElfHp = Math.max(0, trainState.playerElfHp - damage);
        
        // 生成日志（参照怪物的日志格式）
        String mannequinName = getMannequinTypeName(trainState.currentMannequin.getType());
        String actionLog = BattleUtils.generateEnemyAttackLogWithDetail(mannequinName, attackType, damage);
        
        trainState.trainLog.add(actionLog);
        trainState.battleLogManager.addLog(actionLog);
    }

    /**
     * 获取精灵名称
     */
    private String getElfName(UserElf userElf) {
        if (userElf == null) {
            return "未知精灵";
        }
        Elf elf = elfMapper.selectById(userElf.getElfId());
        return elf != null ? elf.getElfName() : "精灵 " + userElf.getElfId();
    }

    /**
     * 查找精灵的train_record_elf记录
     */
    private TrainRecordElf findTrainRecordElf(TrainState trainState, Long elfId) {
        if (trainState.trainRecordElves == null) return null;
        for (TrainRecordElf record : trainState.trainRecordElves) {
            if (record.getElfId().equals(elfId)) {
                return record;
            }
        }
        return null;
    }

    /**
     * 检查并准备下一个可用精灵（基于trainRecordElves）
     * @return true=有可用精灵并已切换，false=没有可用精灵
     */
    private boolean checkAndPrepareNextElf(Long userId, TrainState trainState) {
        List<TrainRecordElf> trainRecordElves = trainState.trainRecordElves;
        if (trainRecordElves == null || trainRecordElves.isEmpty()) {
            return false;
        }
        
        Long currentElfId = trainState.currentPlayerElf.getId();
        
        // 查找下一个HP>0且elfState=1的精灵
        TrainRecordElf nextElfRecord = null;
        boolean foundCurrent = false;
        
        for (TrainRecordElf elfRecord : trainRecordElves) {
            if (elfRecord.getElfId().equals(currentElfId)) {
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
            for (TrainRecordElf elfRecord : trainRecordElves) {
                if (elfRecord.getCurrentHp() != null && elfRecord.getCurrentHp() > 0
                    && !elfRecord.getElfId().equals(currentElfId)
                    && elfRecord.getElfState() != null && elfRecord.getElfState() == 1) {
                    nextElfRecord = elfRecord;
                    break;
                }
            }
        }
        
        if (nextElfRecord == null) {
            return false;
        }
        
        // 查询新精灵的详细信息
        UserElf newElf = userElfMapper.selectById(nextElfRecord.getElfId());
        if (newElf == null) {
            return false;
        }
        
        // 标记当前精灵为死亡（如果还未标记）
        TrainRecordElf currentRecord = null;
        for (TrainRecordElf elfRecord : trainRecordElves) {
            if (elfRecord.getElfId().equals(currentElfId)) {
                currentRecord = elfRecord;
                break;
            }
        }
        if (currentRecord != null && currentRecord.getElfState() != null && currentRecord.getElfState() != 2) {
            currentRecord.setElfState(2);
            currentRecord.setCurrentHp(0);
            currentRecord.setCurrentMp(0);
            currentRecord.setUpdateTime(LocalDateTime.now());
            trainRecordElfMapper.updateById(currentRecord);  // 持久化到数据库
        }
        
        // 将下一个精灵标记为战斗中
        nextElfRecord.setElfState(0);
        nextElfRecord.setUpdateTime(LocalDateTime.now());
        trainRecordElfMapper.updateById(nextElfRecord);  // 持久化到数据库
        
        // 切换到新精灵
        trainState.currentPlayerElf = newElf;
        trainState.playerElfHp = nextElfRecord.getCurrentHp();
        // 同步更新MP值，确保使用train_record_elf中的MP而不是user_elf中的旧值
        trainState.currentPlayerElf.setMp(nextElfRecord.getCurrentMp());
        
        return true;
    }

    /**
     * 构建训练结果响应
     */
    private Map<String, Object> buildTrainResult(TrainState trainState) {
        Map<String, Object> res = new HashMap<>();
        res.put("trainLog", trainState.trainLog);
        res.put("roundLogs", trainState.battleLogManager.getRoundLogs());
        res.put("playerElfHp", trainState.playerElfHp);
        res.put("elfMp", trainState.currentPlayerElf.getMp());
        res.put("mannequinHp", trainState.mannequinHp);
        res.put("mannequinMp", trainState.mannequinMp);
        
        // 添加训练人偶的完整信息，包括type（系别）
        if (trainState.currentMannequin != null) {
            res.put("mannequin", trainState.currentMannequin);
            res.put("mannequinMaxHp", trainState.currentMannequin.getHp());
            res.put("mannequinMaxMp", trainState.currentMannequin.getMp());
        }
        
        // 添加精灵信息
        if (trainState.currentPlayerElf != null) {
            res.put("playerElf", trainState.currentPlayerElf);
            res.put("playerElfId", trainState.currentPlayerElf.getId());  // 添加playerElfId用于前端检测精灵切换
            res.put("playerElfMaxHp", trainState.currentPlayerElf.getMaxHp());
            res.put("playerElfMaxMp", trainState.currentPlayerElf.getMaxMp());
            
            // 查询精灵模板获取名字和系别
            Elf elf = elfMapper.selectById(trainState.currentPlayerElf.getElfId());
            if (elf != null) {
                res.put("elfName", elf.getElfName());
                res.put("elfElementType", elf.getElementType());
            }
        }
        
        // 添加needSwitch标记（精灵死亡时需要切换）
        if (trainState.playerElfHp <= 0 && trainState.trainRecordElves != null) {
            // 查找下一个可用的精灵
            Long currentElfId = trainState.currentPlayerElf != null ? trainState.currentPlayerElf.getId() : null;
            for (TrainRecordElf elfRecord : trainState.trainRecordElves) {
                if (currentElfId != null && elfRecord.getElfId().equals(currentElfId)) continue;
                if (elfRecord.getCurrentHp() != null && elfRecord.getCurrentHp() > 0
                    && elfRecord.getElfState() != null && elfRecord.getElfState() == 1) {
                    res.put("needSwitch", true);
                    res.put("nextElfId", elfRecord.getElfId());
                    res.put("nextElfHp", elfRecord.getCurrentHp());
                    res.put("nextElfMp", elfRecord.getCurrentMp());
                    break;
                }
            }
        }
        
        // 添加elfSwitched标记（告诉前端刚刚发生了精灵切换，需要显示弹窗）
        if (trainState.elfSwitched) {
            res.put("elfSwitched", true);
            // 重置标志
            trainState.elfSwitched = false;
        }
        
        return res;
    }

    @Override
    public Result<List<Map<String, Object>>> getBattleElves(Long userId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null) {
            return Result.error("未进入训练");
        }

        List<TrainRecordElf> trainRecordElves = trainState.trainRecordElves;
        if (trainRecordElves == null || trainRecordElves.isEmpty()) {
            return Result.error("精灵信息不存在");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainRecordElf battleElf : trainRecordElves) {
            // 过滤掉已死亡的精灵
            if (battleElf.getCurrentHp() == null || battleElf.getCurrentHp() <= 0
                || battleElf.getElfState() != null && battleElf.getElfState() == 2) {
                continue;
            }

            UserElf userElf = userElfMapper.selectById(battleElf.getElfId());
            if (userElf == null) continue;

            Elf elf = elfMapper.selectById(userElf.getElfId());
            if (elf == null) continue;

            Map<String, Object> elfMap = new HashMap<>();
            elfMap.put("id", userElf.getId());
            elfMap.put("userId", userElf.getUserId());
            elfMap.put("elfId", userElf.getElfId());
            elfMap.put("level", userElf.getLevel());
            elfMap.put("exp", userElf.getExp());
            elfMap.put("expNeed", userElf.getExpNeed());
            elfMap.put("maxHp", userElf.getMaxHp());
            elfMap.put("maxMp", userElf.getMaxMp());
            elfMap.put("hp", battleElf.getCurrentHp());
            elfMap.put("mp", battleElf.getCurrentMp());
            elfMap.put("attack", userElf.getAttack());
            elfMap.put("defense", userElf.getDefense());
            elfMap.put("normalDamage", userElf.getNormalDamage());
            elfMap.put("speed", userElf.getSpeed());
            elfMap.put("isFight", userElf.getIsFight());
            elfMap.put("fightOrder", userElf.getFightOrder());
            elfMap.put("elfState", battleElf.getElfState());
            elfMap.put("elfName", elf.getElfName());
            elfMap.put("elementType", elf.getElementType());

            result.add(elfMap);
        }

        return Result.success(result);
    }

    private String getMannequinTypeName(Integer type) {
        switch (type) {
            case 1:
                return "火系训练人偶";
            case 2:
                return "水系训练人偶";
            case 3:
                return "草系训练人偶";
            default:
                return "训练人偶";
        }
    }
    
    /**
     * 保存训练日志到数据库
     * @param trainState 训练状态
     */
    private void saveTrainLogs(TrainState trainState) {
        List<Map<String, Object>> roundLogs = trainState.battleLogManager.getRoundLogs();
        
        if (roundLogs == null || roundLogs.isEmpty()) {
            return;
        }
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            for (Map<String, Object> roundData : roundLogs) {
                Integer round = (Integer) roundData.get("round");
                List<String> logs = (List<String>) roundData.get("logs");
                
                if (logs == null || logs.isEmpty()) {
                    continue;
                }
                
                for (String logText : logs) {
                    TrainLog trainLog = new TrainLog();
                    trainLog.setTrainId(trainState.trainId);
                    trainLog.setUserId(trainState.currentMannequin.getUserId());
                    trainLog.setRound(round);
                    trainLog.setLogText(logText);
                    trainLog.setCreateTime(now);
                    trainLogMapper.insert(trainLog);
                }
            }
            
            log.debug("保存训练日志成功, trainId={}, rounds={}", trainState.trainId, roundLogs.size());
        } catch (Exception e) {
            log.error("保存训练日志失败, trainId={}", trainState.trainId, e);
        }
    }
    
    @Override
    public Result<List<Map<String, Object>>> getTrainLogs(String trainId) {
        try {
            // 查询训练日志
            List<TrainLog> trainLogs = trainLogMapper.selectByTrainId(trainId);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = new ArrayList<>();
            for (TrainLog log : trainLogs) {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("id", log.getId());
                logMap.put("round", log.getRound());
                logMap.put("logText", log.getLogText());
                logMap.put("createTime", log.getCreateTime());
                result.add(logMap);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取训练日志失败, trainId={}", trainId, e);
            return Result.error("获取训练日志失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> usePotion(Long userId, Long elfId, Long potionId) {
        try {
            // 检查是否有进行中的训练
            TrainState trainState = userTrainStates.get(userId);
            if (trainState == null || trainState.trainEnded) {
                return Result.error("未进入训练或训练已结束");
            }

            // 检查精灵是否存在且属于该用户
            QueryWrapper<UserElf> elfWrapper = new QueryWrapper<>();
            elfWrapper.eq("id", elfId).eq("user_id", userId);
            UserElf userElf = userElfMapper.selectOne(elfWrapper);
            if (userElf == null) {
                return Result.error("精灵不存在或不属于该用户");
            }

            System.out.println("[DEBUG] 训练模式-使用药品前 - HP: " + trainState.playerElfHp + "/" + userElf.getMaxHp() + ", MP: " + trainState.currentPlayerElf.getMp() + "/" + userElf.getMaxMp());

            // 检查药品是否存在
            PotionConfig potion = potionConfigMapper.selectById(potionId);
            if (potion == null) {
                return Result.error("药品不存在");
            }

            System.out.println("[DEBUG] 训练模式-药品信息 - 名称: " + potion.getName() + ", 类型: " + potion.getType() + ", 恢复值: " + potion.getHealValue());

            // 检查用户是否拥有该药品
            QueryWrapper<UserPotion> userPotionWrapper = new QueryWrapper<>();
            userPotionWrapper.eq("user_id", userId);
            userPotionWrapper.eq("potion_config_id", potionId);
            UserPotion userPotion = userPotionMapper.selectOne(userPotionWrapper);
            if (userPotion == null || userPotion.getCount() <= 0) {
                return Result.error("您没有该药品");
            }

            // 检查是否是当前出战的精灵
            if (!trainState.currentPlayerElf.getId().equals(elfId)) {
                return Result.error("只能对当前出战的精灵使用药品");
            }

            // 根据药品类型恢复对应的属性
            int healValue = potion.getHealValue();
            if (potion.getType() == 1) {
                // 血瓶，恢复生命值
                int oldHp = trainState.playerElfHp;
                trainState.playerElfHp = Math.min(trainState.playerElfHp + healValue, userElf.getMaxHp());
                System.out.println("[DEBUG] 训练模式-血瓶 - HP从 " + oldHp + " 增加到 " + trainState.playerElfHp);
                
                // 更新train_record_elf表
                TrainRecordElf currentRecord = findTrainRecordElf(trainState, elfId);
                if (currentRecord != null) {
                    currentRecord.setCurrentHp(trainState.playerElfHp);
                    currentRecord.setUpdateTime(LocalDateTime.now());
                    trainRecordElfMapper.updateById(currentRecord);
                }
            } else if (potion.getType() == 2) {
                // 蓝瓶，恢复魔法值
                int oldMp = trainState.currentPlayerElf.getMp();
                trainState.currentPlayerElf.setMp(Math.min(trainState.currentPlayerElf.getMp() + healValue, userElf.getMaxMp()));
                System.out.println("[DEBUG] 训练模式-蓝瓶 - MP从 " + oldMp + " 增加到 " + trainState.currentPlayerElf.getMp());
                
                // 更新train_record_elf表
                TrainRecordElf currentRecord = findTrainRecordElf(trainState, elfId);
                if (currentRecord != null) {
                    currentRecord.setCurrentMp(trainState.currentPlayerElf.getMp());
                    currentRecord.setUpdateTime(LocalDateTime.now());
                    trainRecordElfMapper.updateById(currentRecord);
                }
            }

            // 同步更新 user_elf 表（保持数据一致性）
            if (potion.getType() == 1) {
                userElf.setHp(trainState.playerElfHp);
            } else if (potion.getType() == 2) {
                userElf.setMp(trainState.currentPlayerElf.getMp());
            }
            userElfMapper.updateById(userElf);

            System.out.println("[DEBUG] 训练模式-使用药品后 - HP: " + trainState.playerElfHp + "/" + userElf.getMaxHp() + ", MP: " + trainState.currentPlayerElf.getMp() + "/" + userElf.getMaxMp());

            // 减少药品数量
            userPotion.setCount(userPotion.getCount() - 1);
            userPotion.setUpdateTime(LocalDateTime.now());
            if (userPotion.getCount() <= 0) {
                userPotionMapper.deleteById(userPotion.getId());
            } else {
                userPotionMapper.updateById(userPotion);
            }

            // 添加训练日志
            String potionLog;
            if (potion.getType() == 1) {
                potionLog = "使用了" + potion.getName() + "，恢复了" + healValue + "点生命值";
            } else {
                potionLog = "使用了" + potion.getName() + "，恢复了" + healValue + "点魔法值";
            }
            trainState.trainLog.add(potionLog);
            trainState.battleLogManager.addLog(potionLog);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> elfData = new HashMap<>();
            elfData.put("id", userElf.getId());
            elfData.put("elfId", userElf.getElfId());
            elfData.put("userId", userElf.getUserId());
            elfData.put("level", userElf.getLevel());
            elfData.put("exp", userElf.getExp());
            elfData.put("hp", trainState.playerElfHp);
            elfData.put("maxHp", userElf.getMaxHp());
            elfData.put("mp", trainState.currentPlayerElf.getMp());
            elfData.put("maxMp", userElf.getMaxMp());
            elfData.put("attack", userElf.getAttack());
            elfData.put("defense", userElf.getDefense());
            elfData.put("speed", userElf.getSpeed());
            
            result.put("elf", elfData);
            result.put("potion", potion);
            result.put("playerElfHp", trainState.playerElfHp);
            result.put("elfMp", trainState.currentPlayerElf.getMp());
            result.put("trainLog", trainState.trainLog);
            result.put("roundLogs", trainState.battleLogManager.getRoundLogs());
            result.put("msg", "使用药品成功");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("训练模式使用药品失败", e);
            return Result.error("使用药品失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> playerOffline(Long userId) {
        try {
            // 检查是否有进行中的训练
            TrainState trainState = userTrainStates.get(userId);
            if (trainState == null || trainState.trainEnded) {
                return Result.error("未进入训练或训练已结束");
            }

            // 查询训练记录
            QueryWrapper<TrainRecord> recordWrapper = new QueryWrapper<>();
            recordWrapper.eq("train_id", trainState.trainId);
            TrainRecord trainRecord = trainRecordMapper.selectOne(recordWrapper);
            
            if (trainRecord == null) {
                return Result.error("训练记录不存在");
            }

            // 更新训练状态为断线暂停
            trainRecord.setStatus(3); // 3=断线暂停
            trainRecord.setOfflineTime(LocalDateTime.now());
            trainRecordMapper.updateById(trainRecord);

            log.info("用户 {} 的训练 {} 已暂停", userId, trainState.trainId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("msg", "训练已暂停");
            return Result.success(result);
        } catch (Exception e) {
            log.error("训练模式玩家离线失败", e);
            return Result.error("训练暂停失败: " + e.getMessage());
        }
    }
}