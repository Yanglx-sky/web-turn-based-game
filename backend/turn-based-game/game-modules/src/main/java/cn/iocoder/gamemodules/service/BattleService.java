package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.BattleRecord;
import cn.iocoder.gamemodules.entity.BattleRecordElf;
import cn.iocoder.gamemodules.entity.BattleRecordMonster;

import java.util.List;
import java.util.Map;

public interface BattleService {
    /**
     * 开始战斗，生成战斗记录
     */
    Result<Map<String, Object>> startBattle(Long userId, Long userElfId, Integer levelId);

    /**
     * 玩家退出/断线，更新战斗状态
     */
    Result<?> playerOffline(Long userId);

    /**
     * 玩家重连恢复战斗
     */
    Result<Map<String, Object>> reconnect(Long userId);

    /**
     * 处理超时未重连的战斗，自动判负
     */
    void handleTimeoutBattles();

    /**
     * 保存战斗状态
     */
    void saveBattleState(String battleId, List<BattleRecordElf> elves, List<BattleRecordMonster> monsters, Integer currentRound);
    
    /**
     * 玩家逃跑
     */
    Result<?> flee(Long userId);
    
    /**
     * 放弃战斗
     */
    Result<?> abandonBattle(Long userId);
    
    /**
     * 普通攻击
     */
    Result<Map<String, Object>> normalAttack(Long userId);
    
    /**
     * 使用技能
     */
    Result<Map<String, Object>> useSkill(Long userId, Integer skillId);
    
    /**
     * 切换精灵
     */
    Result<Map<String, Object>> switchElf(Long userId, Long elfId);
    
    // ==================== 以下为战斗监控+一致性+每日收益上限功能 ====================
    
    /**
     * 提交战斗行动（带回合校验和幂等性）
     * @param userId 用户ID
     * @param round 当前回合号
     * @param actionType 动作类型: attack/skill/switch
     * @param skillId 技能ID（可选）
     * @param elfId 精灵ID（可选）
     * @return 战斗结果
     */
    Result<Map<String, Object>> submitAction(Long userId, Integer round, String actionType, Integer skillId, Long elfId);
    
    /**
     * 领取战斗奖励（必须战斗胜利才能调用）
     * @param userId 用户ID
     * @param levelId 关卡ID
     * @param battleId 战斗ID
     * @return 奖励信息
     */
    Result<Map<String, Object>> claimReward(Long userId, Integer levelId, String battleId);
    
    /**
     * 获取今日收益信息
     * @param userId 用户ID
     * @return 今日经验、金币收益
     */
    Result<Map<String, Object>> getDailyRewardInfo(Long userId);
    
    /**
     * 校验出招冷却时间
     * @param userId 用户ID
     * @return true=可以出招，false=冷却中
     */
    boolean checkActionCooldown(Long userId);
    
    /**
     * 更新出招冷却时间
     * @param userId 用户ID
     */
    void updateActionCooldown(Long userId);
    
    /**
     * 检查并扣除每日收益配额
     * @param userId 用户ID
     * @param rewards 奖励数组 [经验值, 金币]
     * @return 实际可获得的经验和金币 [exp, gold]
     */
    int[] checkAndDeductDailyLimit(Long userId, Integer[] rewards);
}