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

    // 训练战斗状态 - 使用Map存储每个用户的训练状态
    private ConcurrentHashMap<Long, TrainState> userTrainStates = new ConcurrentHashMap<>();

    // 训练状态类
    private static class TrainState {
        boolean trainEnded;
        boolean trainWon;
        List<String> trainLog = new ArrayList<>();
        BattleUtils.BattleLogManager battleLogManager = new BattleUtils.BattleLogManager();
        TrainMannequin currentMannequin;
        UserElf currentPlayerElf;
        int playerElfHp;
        int mannequinHp;
        int mannequinMp;
    }

    @Override
    public Result<Map<String, Object>> createMannequin(Long userId, Integer attack, Integer defense, Integer hp, Integer mp, Integer type, Integer isAttack) {
        // 创建训练人偶
        TrainMannequin mannequin = new TrainMannequin();
        mannequin.setUserId(userId);
        mannequin.setAttack(attack);
        mannequin.setDefense(defense);
        mannequin.setHp(hp);
        mannequin.setMp(mp);
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
        trainState.trainEnded = false;
        trainState.trainWon = false;
        trainState.trainLog.clear();
        trainState.battleLogManager.reset();

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

        // 使用第一个精灵作为训练精灵
        Map<String, Object> elfMap = battleElvesResult.getData().get(0);
        Long elfId = ((Number) elfMap.get("id")).longValue();
        trainState.currentPlayerElf = userElfMapper.selectById(elfId);
        if (trainState.currentPlayerElf == null) {
            return Result.error("精灵不存在");
        }

        // 初始化生命值和魔法值（精灵进入训练时满血满蓝）
        trainState.playerElfHp = trainState.currentPlayerElf.getMaxHp();
        trainState.currentPlayerElf.setHp(trainState.currentPlayerElf.getMaxHp());
        trainState.currentPlayerElf.setMp(trainState.currentPlayerElf.getMaxMp());
        trainState.mannequinHp = trainState.currentMannequin.getHp();
        trainState.mannequinMp = trainState.currentMannequin.getMp();

        // 记录训练开始
        String startLog = "训练开始！";
        String elfLog = "你的精灵: " + elfMap.get("elfName") + " (HP: " + trainState.playerElfHp + ")";
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
        res.put("playerElf", elfMap);
        res.put("mannequin", trainState.currentMannequin);
        res.put("playerElfHp", trainState.playerElfHp);
        res.put("elfMp", trainState.currentPlayerElf.getMp());
        res.put("mannequinHp", trainState.mannequinHp);
        res.put("mannequinMp", trainState.mannequinMp);
        return Result.success(res);
    }

    // 计算系别克制倍数
    private double calculateElementMultiplier(int attackerElement, int defenderElement) {
        // 1: 火系, 2: 水系, 3: 草系
        double[][] multiplierMatrix = {
            {1.0, 0.5, 2.0}, // 火系攻击
            {2.0, 1.0, 0.5}, // 水系攻击
            {0.5, 2.0, 1.0}  // 草系攻击
        };
        if (attackerElement >= 1 && attackerElement <= 3 && defenderElement >= 1 && defenderElement <= 3) {
            return multiplierMatrix[attackerElement - 1][defenderElement - 1];
        }
        return 1.0;
    }

    @Override
    public Result<Map<String, Object>> normalAttack(Long userId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null || trainState.trainEnded) {
            return Result.error("未进入训练或训练已结束");
        }

        // 开始新回合
        trainState.battleLogManager.startNewRound();

        // 计算系别克制
        int playerElement = BattleUtils.getElfElementType(trainState.currentPlayerElf, elfMapper);
        int mannequinElement = trainState.currentMannequin.getType();
        
        // 1. 先手方（玩家）行动：先造成伤害
        // 玩家精灵普通攻击
        int finalDamage = BattleUtils.calculateNormalDamage(trainState.currentPlayerElf.getAttack(), trainState.currentMannequin.getDefense());
        double multiplier = BattleUtils.calculateElementMultiplier(playerElement, mannequinElement);
        finalDamage = (int) (finalDamage * multiplier);
        
        trainState.mannequinHp -= finalDamage;
        String attackLog = "你的精灵使用普通攻击";
        if (multiplier > 1) {
            attackLog += "，效果拔群";
        } else if (multiplier < 1) {
            attackLog += "，效果不佳";
        }
        attackLog += "，造成 " + finalDamage + " 点伤害";
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
            
            return trainSettlement(userId);
        }

        // 训练人偶反击（如果设置为主动攻击）
        if (trainState.currentMannequin.getIsAttack() == 1) {
            // 训练人偶使用技能
            QueryWrapper<TrainMannequinSkill> skillWrapper = new QueryWrapper<>();
            skillWrapper.eq("mannequin_id", trainState.currentMannequin.getId());
            List<TrainMannequinSkill> mannequinSkills = trainMannequinSkillMapper.selectList(skillWrapper);
            if (!mannequinSkills.isEmpty()) {
                // 随机选择一个技能
                TrainMannequinSkill mannequinSkill = mannequinSkills.get((int) (Math.random() * mannequinSkills.size()));
                Skill mannequinSkillInfo = skillMapper.selectById(mannequinSkill.getSkillId());
                if (mannequinSkillInfo != null) {
                    // 检查MP是否足够
                    if (trainState.mannequinMp >= mannequinSkillInfo.getCostMp()) {
                        // 消耗MP
                        trainState.mannequinMp -= mannequinSkillInfo.getCostMp();
                        
                        // 2. 后手方（训练人偶）行动：先消耗MP，再造成伤害
                        int finalMannequinDamage = BattleUtils.calculateSkillDamage(mannequinSkillInfo, trainState.currentMannequin.getAttack(), trainState.currentPlayerElf.getDefense(), mannequinElement, playerElement);
                        double mannequinMultiplier = BattleUtils.calculateElementMultiplier(mannequinElement, playerElement);
                        
                        trainState.playerElfHp -= finalMannequinDamage;
                        String skillLog = "训练人偶使用技能 " + mannequinSkillInfo.getSkillName();
                        if (mannequinMultiplier > 1) {
                            skillLog += "，效果拔群";
                        } else if (mannequinMultiplier < 1) {
                            skillLog += "，效果不佳";
                        }
                        skillLog += "，造成 " + finalMannequinDamage + " 点伤害";
                        trainState.trainLog.add(skillLog);
                        trainState.battleLogManager.addLog(skillLog);
                    } else {
                        // MP不足，使用普通攻击
                    // 2. 后手方（训练人偶）行动：使用普通攻击
                    int finalMannequinDamage = trainState.currentMannequin.getAttack();
                    double mannequinMultiplier = BattleUtils.calculateElementMultiplier(mannequinElement, playerElement);
                    finalMannequinDamage = (int) (finalMannequinDamage * mannequinMultiplier);
                        
                        trainState.playerElfHp -= finalMannequinDamage;
                        String attackLog2 = "训练人偶MP不足，使用普通攻击";
                        if (mannequinMultiplier > 1) {
                            attackLog2 += "，效果拔群";
                        } else if (mannequinMultiplier < 1) {
                            attackLog2 += "，效果不佳";
                        }
                        attackLog2 += "，造成 " + finalMannequinDamage + " 点伤害";
                        trainState.trainLog.add(attackLog2);
                        trainState.battleLogManager.addLog(attackLog2);
                    }
                }
            } else {
                // 普通攻击
            // 2. 后手方（训练人偶）行动：使用普通攻击
            int finalMannequinDamage = trainState.currentMannequin.getAttack();
            double mannequinMultiplier = BattleUtils.calculateElementMultiplier(mannequinElement, playerElement);
            finalMannequinDamage = (int) (finalMannequinDamage * mannequinMultiplier);
                
                trainState.playerElfHp -= finalMannequinDamage;
                String attackLog2 = "训练人偶反击";
                if (mannequinMultiplier > 1) {
                    attackLog2 += "，效果拔群";
                } else if (mannequinMultiplier < 1) {
                    attackLog2 += "，效果不佳";
                }
                attackLog2 += "，造成 " + finalMannequinDamage + " 点伤害";
                trainState.trainLog.add(attackLog2);
                trainState.battleLogManager.addLog(attackLog2);
            }

            // 检查玩家精灵是否被击败
            if (trainState.playerElfHp <= 0) {
                trainState.playerElfHp = 0;
                trainState.trainEnded = true;
                trainState.trainWon = false;
                String loseLog = "你的精灵被击败，训练失败！";
                trainState.trainLog.add(loseLog);
                trainState.battleLogManager.addLog(loseLog);
                
                return trainSettlement(userId);
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("trainLog", trainState.trainLog);
        res.put("roundLogs", trainState.battleLogManager.getRoundLogs());
        res.put("playerElfHp", trainState.playerElfHp);
        res.put("elfMp", trainState.currentPlayerElf.getMp());
        res.put("mannequinHp", trainState.mannequinHp);
        res.put("mannequinMp", trainState.mannequinMp);
        return Result.success(res);
    }

    @Override
    public Result<Map<String, Object>> useSkill(Long userId, Integer skillId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null || trainState.trainEnded) {
            return Result.error("未进入训练或训练已结束");
        }

        // 开始新回合
        trainState.battleLogManager.startNewRound();

        // 检查技能是否存在
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            return Result.error("技能不存在");
        }

        // 检查MP是否足够
        if (trainState.currentPlayerElf.getMp() < skill.getCostMp()) {
            return Result.error("MP不足，无法使用技能");
        }

        // 计算系别克制
        int playerElement = BattleUtils.getElfElementType(trainState.currentPlayerElf, elfMapper);
        int mannequinElement = trainState.currentMannequin.getType();
        
        // 消耗MP
        trainState.currentPlayerElf.setMp(trainState.currentPlayerElf.getMp() - skill.getCostMp());
        
        // 玩家精灵使用技能
        int finalDamage = BattleUtils.calculateSkillDamage(skill, trainState.currentPlayerElf.getAttack(), trainState.currentMannequin.getDefense(), playerElement, mannequinElement);
        double multiplier = BattleUtils.calculateElementMultiplier(playerElement, mannequinElement);
        
        trainState.mannequinHp -= finalDamage;
        String skillLog = "你的精灵使用技能 " + skill.getSkillName();
        if (multiplier > 1) {
            skillLog += "，效果拔群";
        } else if (multiplier < 1) {
            skillLog += "，效果不佳";
        }
        skillLog += "，造成 " + finalDamage + " 点伤害";
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
            
            return trainSettlement(userId);
        }

        // 训练人偶反击（如果设置为主动攻击）
        if (trainState.currentMannequin.getIsAttack() == 1) {
            // 训练人偶使用技能
            QueryWrapper<TrainMannequinSkill> skillWrapper = new QueryWrapper<>();
            skillWrapper.eq("mannequin_id", trainState.currentMannequin.getId());
            List<TrainMannequinSkill> mannequinSkills = trainMannequinSkillMapper.selectList(skillWrapper);
            if (!mannequinSkills.isEmpty()) {
                // 随机选择一个技能
                TrainMannequinSkill mannequinSkill = mannequinSkills.get((int) (Math.random() * mannequinSkills.size()));
                Skill mannequinSkillInfo = skillMapper.selectById(mannequinSkill.getSkillId());
                if (mannequinSkillInfo != null) {
                    // 检查MP是否足够
                    if (trainState.mannequinMp >= mannequinSkillInfo.getCostMp()) {
                        // 消耗MP
                        trainState.mannequinMp -= mannequinSkillInfo.getCostMp();
                        
                        // 2. 后手方（训练人偶）行动：先消耗MP，再造成伤害
                        int finalMannequinDamage = BattleUtils.calculateSkillDamage(mannequinSkillInfo, trainState.currentMannequin.getAttack(), trainState.currentPlayerElf.getDefense(), mannequinElement, playerElement);
                        double mannequinMultiplier = BattleUtils.calculateElementMultiplier(mannequinElement, playerElement);
                        
                        trainState.playerElfHp -= finalMannequinDamage;
                        String mannequinSkillLog = "训练人偶使用技能 " + mannequinSkillInfo.getSkillName();
                        if (mannequinMultiplier > 1) {
                            mannequinSkillLog += "，效果拔群";
                        } else if (mannequinMultiplier < 1) {
                            mannequinSkillLog += "，效果不佳";
                        }
                        mannequinSkillLog += "，造成 " + finalMannequinDamage + " 点伤害";
                        trainState.trainLog.add(mannequinSkillLog);
                        trainState.battleLogManager.addLog(mannequinSkillLog);
                    } else {
                        // MP不足，使用普通攻击
                        // 2. 后手方（训练人偶）行动：使用普通攻击
                        int finalMannequinDamage = BattleUtils.calculateNormalDamage(trainState.currentMannequin.getAttack(), trainState.currentPlayerElf.getDefense());
                        double mannequinMultiplier = BattleUtils.calculateElementMultiplier(mannequinElement, playerElement);
                        finalMannequinDamage = (int) (finalMannequinDamage * mannequinMultiplier);
                        
                        trainState.playerElfHp -= finalMannequinDamage;
                        String attackLog = "训练人偶MP不足，使用普通攻击";
                        if (mannequinMultiplier > 1) {
                            attackLog += "，效果拔群";
                        } else if (mannequinMultiplier < 1) {
                            attackLog += "，效果不佳";
                        }
                        attackLog += "，造成 " + finalMannequinDamage + " 点伤害";
                        trainState.trainLog.add(attackLog);
                        trainState.battleLogManager.addLog(attackLog);
                    }
                }
            } else {
                // 普通攻击
                // 2. 后手方（训练人偶）行动：使用普通攻击
                int finalMannequinDamage = BattleUtils.calculateNormalDamage(trainState.currentMannequin.getAttack(), trainState.currentPlayerElf.getDefense());
                double mannequinMultiplier = BattleUtils.calculateElementMultiplier(mannequinElement, playerElement);
                finalMannequinDamage = (int) (finalMannequinDamage * mannequinMultiplier);
                
                trainState.playerElfHp -= finalMannequinDamage;
                String attackLog = "训练人偶反击";
                if (mannequinMultiplier > 1) {
                    attackLog += "，效果拔群";
                } else if (mannequinMultiplier < 1) {
                    attackLog += "，效果不佳";
                }
                attackLog += "，造成 " + finalMannequinDamage + " 点伤害";
                trainState.trainLog.add(attackLog);
                trainState.battleLogManager.addLog(attackLog);
            }

            // 检查玩家精灵是否被击败
            if (trainState.playerElfHp <= 0) {
                trainState.playerElfHp = 0;
                trainState.trainEnded = true;
                trainState.trainWon = false;
                String loseLog = "你的精灵被击败，训练失败！";
                trainState.trainLog.add(loseLog);
                trainState.battleLogManager.addLog(loseLog);
                
                return trainSettlement(userId);
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("trainLog", trainState.trainLog);
        res.put("roundLogs", trainState.battleLogManager.getRoundLogs());
        res.put("playerElfHp", trainState.playerElfHp);
        res.put("elfMp", trainState.currentPlayerElf.getMp());
        res.put("mannequinHp", trainState.mannequinHp);
        res.put("mannequinMp", trainState.mannequinMp);
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

        // 获取用户的出战精灵
        Result<List<Map<String, Object>>> battleElvesResult = userElfService.getBattleElves(userId);
        if (battleElvesResult.getCode() != 200 || battleElvesResult.getData() == null || battleElvesResult.getData().isEmpty()) {
            return Result.error("获取出战精灵失败");
        }

        List<Map<String, Object>> battleElves = battleElvesResult.getData();
        Map<String, Object> targetElfMap = null;

        // 查找目标精灵
        for (Map<String, Object> elfMap : battleElves) {
            if (elfMap.get("id").equals(elfId)) {
                targetElfMap = elfMap;
                // 检查精灵是否已死亡
                int hp = ((Number) elfMap.get("hp")).intValue();
                if (hp <= 0) {
                    return Result.error("精灵已死亡，无法切换");
                }
                break;
            }
        }

        if (targetElfMap == null) {
            return Result.error("精灵不存在或未设置为出战");
        }

        // 从Map中提取UserElf信息（保留实际状态）
        UserElf newElf = new UserElf();
        newElf.setId(((Number) targetElfMap.get("id")).longValue());
        newElf.setUserId(((Number) targetElfMap.get("userId")).longValue());
        newElf.setElfId(((Number) targetElfMap.get("elfId")).intValue());
        newElf.setLevel(((Number) targetElfMap.get("level")).intValue());
        newElf.setExp(((Number) targetElfMap.get("exp")).longValue());
        newElf.setExpNeed(((Number) targetElfMap.get("expNeed")).longValue());
        newElf.setMaxHp(((Number) targetElfMap.get("maxHp")).intValue());
        newElf.setMaxMp(((Number) targetElfMap.get("maxMp")).intValue());
        newElf.setHp(((Number) targetElfMap.get("hp")).intValue()); // 保留实际HP
        newElf.setMp(((Number) targetElfMap.get("mp")).intValue()); // 保留实际MP
        newElf.setAttack(((Number) targetElfMap.get("attack")).intValue());
        newElf.setDefense(((Number) targetElfMap.get("defense")).intValue());
        newElf.setNormalDamage(((Number) targetElfMap.get("normalDamage")).intValue());
        newElf.setSpeed(((Number) targetElfMap.get("speed")).intValue());
        newElf.setIsFight(((Number) targetElfMap.get("isFight")).intValue());
        newElf.setFightOrder(((Number) targetElfMap.get("fightOrder")).intValue());

        // 切换到新精灵
        trainState.currentPlayerElf = newElf;
        trainState.playerElfHp = newElf.getHp();
        
        // 添加战斗日志
        String elfName = (String) targetElfMap.get("elfName");
        trainState.trainLog.add("切换精灵：" + (elfName != null ? elfName : "精灵 " + newElf.getElfId()) + "，等级: " + newElf.getLevel());

        Map<String, Object> res = new HashMap<>();
        res.put("elf", newElf);
        res.put("elfName", elfName);
        res.put("elfElementType", BattleUtils.getElfElementType(newElf, elfMapper));
        res.put("msg", "精灵切换成功");
        res.put("trainLog", trainState.trainLog);
        res.put("roundLogs", trainState.battleLogManager.getRoundLogs());
        res.put("playerElfHp", trainState.playerElfHp);
        res.put("elfMp", newElf.getMp());
        res.put("mannequinHp", trainState.mannequinHp);
        res.put("mannequinMp", trainState.mannequinMp);

        return Result.success(res);
    }

    @Override
    public Result<Map<String, Object>> trainSettlement(Long userId) {
        TrainState trainState = userTrainStates.get(userId);
        if (trainState == null || !trainState.trainEnded) {
            return Result.error("未进入训练或训练未结束");
        }

        // 构建训练日志字符串
        StringBuilder trainLogStr = new StringBuilder();
        for (String log : trainState.trainLog) {
            trainLogStr.append(log).append("\n");
        }

        // 调用AI服务获取训练评分报告
        String battleResult = trainState.trainWon ? "胜利" : "失败";
        String aiReport = aiService.getBattleSummary(trainLogStr.toString(), battleResult);

        // 生成AI评分（简单模拟）
        int aiScore = trainState.trainWon ? 80 + (int) (Math.random() * 20) : 40 + (int) (Math.random() * 30);

        // 保存训练记录
        TrainRecord trainRecord = new TrainRecord();
        trainRecord.setUserId(userId);
        trainRecord.setMannequinId(trainState.currentMannequin.getId());
        trainRecord.setAiScore(aiScore);
        trainRecord.setAiReport(aiReport);
        trainRecordMapper.insert(trainRecord);

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
}