package cn.iocoder.gamecommon.exception;

/**
 * 战斗业务异常类
 * 用于战斗逻辑中抛出可预期的错误
 */
public class BattleException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final int code;
    
    public BattleException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public BattleException(String message) {
        this(500, message);
    }
    
    public int getCode() {
        return code;
    }
    
    // ==================== 静态工厂方法 ====================
    
    /** 今日经验收益已达上限 */
    public static BattleException dailyExpLimitReached() {
        return new BattleException(4001, "今日经验收益已达上限");
    }
    
    /** 今日金币收益已达上限 */
    public static BattleException dailyGoldLimitReached() {
        return new BattleException(4002, "今日金币收益已达上限");
    }
    
    /** 出招间隔过短 */
    public static BattleException actionTooFast() {
        return new BattleException(4003, "出招间隔过短，请稍后再试");
    }
    
    /** 战斗不存在 */
    public static BattleException battleNotFound() {
        return new BattleException(4004, "战斗不存在或已结束");
    }
    
    /** 回合号错误 */
    public static BattleException invalidRound() {
        return new BattleException(4005, "回合号错误，请刷新重试");
    }
    
    /** 重复操作 */
    public static BattleException duplicateAction() {
        return new BattleException(4006, "请勿重复操作");
    }
    
    /** 无权领取奖励 */
    public static BattleException noRewardPermission() {
        return new BattleException(4007, "无权领取奖励，请先完成战斗");
    }
    
    /** MP不足 */
    public static BattleException mpNotEnough() {
        return new BattleException(4008, "MP不足");
    }
    
    /** 精灵不存在 */
    public static BattleException elfNotFound() {
        return new BattleException(4009, "精灵不存在");
    }
    
    /** 精灵已死亡 */
    public static BattleException elfAlreadyDead() {
        return new BattleException(4010, "精灵已死亡");
    }
    
    /** 战斗已结束 */
    public static BattleException battleEnded() {
        return new BattleException(4011, "战斗已结束");
    }
    
    /** 战斗尚未开始 */
    public static BattleException battleNotStarted() {
        return new BattleException(4012, "战斗尚未开始，请先开始战斗");
    }
    
    /** 奖励已领取 */
    public static BattleException rewardAlreadyClaimed() {
        return new BattleException(4013, "奖励已领取，请勿重复领取");
    }
}
