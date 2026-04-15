package cn.iocoder.gamecommon.constants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * PVE战斗系统Redis Key常量类
 * 所有战斗监控、一致性校验、每日收益上限相关Key
 */
public class RedisKeyConstant {

    private RedisKeyConstant() {}

    // ==================== 战斗会话相关 ====================
    
    /** 战斗会话信息 Key: battle:session:{userId} */
    public static final String BATTLE_SESSION = "battle:session:";
    
    /** 战斗回合动作记录（幂等校验）Key: battle:action:{battleId}:{round} */
    public static final String BATTLE_ACTION = "battle:action:";
    
    /** 战斗结算状态（幂等校验）Key: battle:settle:{battleId} */
    public static final String BATTLE_SETTLE = "battle:settle:";
    
    /** 出招冷却时间戳（防连点）Key: battle:cooldown:{userId} */
    public static final String BATTLE_COOLDOWN = "battle:cooldown:";
    
    /** 战斗胜利标记（权限控制）Key: battle:victory:{userId}:{levelId}:{date} */
    public static final String BATTLE_VICTORY = "battle:victory:";
    
    /** 奖励已领取标记（防重复领奖）Key: battle:reward:{userId}:{levelId}:{date}:{battleId} */
    public static final String BATTLE_REWARD = "battle:reward:";

    // ==================== 每日收益上限相关 ====================
    
    /** 每日经验收益 Key: daily:exp:{userId}:{date} */
    public static final String DAILY_EXP = "daily:exp:";
    
    /** 每日金币收益 Key: daily:gold:{userId}:{date} */
    public static final String DAILY_GOLD = "daily:gold:";

    // ==================== 配置常量 ====================
    
    /** 每日经验上限 */
    public static final int DAILY_EXP_LIMIT = 1500;
    
    /** 每日金币上限 */
    public static final int DAILY_GOLD_LIMIT = 1500;
    
    /** 最小出招间隔（毫秒） */
    public static final long MIN_ACTION_INTERVAL_MS = 500;
    
    /** 战斗会话过期时间（秒）- 1小时 */
    public static final long BATTLE_SESSION_EXPIRE_SEC = 3600;
    
    /** 冷却时间过期（秒） */
    public static final long COOLDOWN_EXPIRE_SEC = 10;

    // ==================== 工具方法 ====================
    
    /** 获取今日日期字符串 yyyyMMdd */
    public static String getTodayStr() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    
    /** 计算到明天凌晨的秒数 */
    public static long getSecondsUntilMidnight() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        long seconds = tomorrow.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toEpochSecond() - System.currentTimeMillis() / 1000;
        return Math.max(seconds, 1);
    }
    
    /** 构建战斗会话Key */
    public static String buildBattleSessionKey(Long userId) {
        return BATTLE_SESSION + userId;
    }
    
    /** 构建战斗动作Key */
    public static String buildBattleActionKey(String battleId, Integer round) {
        return BATTLE_ACTION + battleId + ":" + round;
    }
    
    /** 构建战斗结算Key */
    public static String buildBattleSettleKey(String battleId) {
        return BATTLE_SETTLE + battleId;
    }
    
    /** 构建出招冷却Key */
    public static String buildCooldownKey(Long userId) {
        return BATTLE_COOLDOWN + userId;
    }
    
    /** 构建每日经验Key */
    public static String buildDailyExpKey(Long userId) {
        return DAILY_EXP + userId + ":" + getTodayStr();
    }
    
    /** 构建每日金币Key */
    public static String buildDailyGoldKey(Long userId) {
        return DAILY_GOLD + userId + ":" + getTodayStr();
    }
    
    /** 构建战斗胜利Key */
    public static String buildBattleVictoryKey(Long userId, Integer levelId) {
        return BATTLE_VICTORY + userId + ":" + levelId + ":" + getTodayStr();
    }
    
    /** 构建奖励领取Key */
    public static String buildBattleRewardKey(Long userId, Integer levelId, String battleId) {
        return BATTLE_REWARD + userId + ":" + levelId + ":" + getTodayStr() + ":" + battleId;
    }
}
