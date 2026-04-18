package cn.iocoder.gameai.service;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface AIService {

    /**
     * 获取AI分析结果
     * @param content 分析内容
     * @return 分析结果
     */
    String getAIAnalysis(String content);

    /**
     * 获取战斗策略
     * @param elfInfo 精灵信息
     * @param monsterInfo 怪物信息
     * @return 战斗策略
     */
    String getBattleStrategy(String elfInfo, String monsterInfo);
    
    /**
     * 获取AI行动决策
     * @param playerHp 玩家当前血量
     * @param playerMaxHp 玩家最大血量
     * @param enemyHp 敌人当前血量
     * @param enemyMaxHp 敌人最大血量
     * @param skills 敌人可用技能列表，格式为"技能名称,伤害/回血值,类型"，多个技能用分号分隔
     * @return 技能序号（从0开始）
     */
    Integer getAIAction(int playerHp, int playerMaxHp, int enemyHp, int enemyMaxHp, String skills);
    
    /**
     * 获取怪物嘲讽语言
     * @param monsterName 怪物名称
     * @param playerHp 玩家当前血量
     * @param playerMaxHp 玩家最大血量
     * @param monsterHp 怪物当前血量
     * @param monsterMaxHp 怪物最大血量
     * @param actionType 动作类型：attack（攻击）或 attacked（被攻击）
     * @return 嘲讽语言
     */
    String getMonsterTaunt(String monsterName, int playerHp, int playerMaxHp, int monsterHp, int monsterMaxHp, String actionType);
    
    /**
     * 获取战斗总结
     * @param battleLog 战斗日志
     * @param battleResult 战斗结果：胜利或失败
     * @return 战斗总结
     */
    String getBattleSummary(String battleLog, String battleResult);
    
    /**
     * 获取战斗评分和评星
     * @param battleLog 战斗日志
     * @param battleResult 战斗结果：胜利或失败
     * @return JSON格式的评分和评星，如：{"score": 85, "star": 2}
     */
    String getBattleScoreAndStar(String battleLog, String battleResult);
    
    /**
     * 获取战斗策略推荐
     * @param elfInfo 精灵信息，格式为"精灵名称,系别,技能1,技能2,..."
     * @param monsterInfo 怪物信息，格式为"怪物名称,系别,技能1,技能2,..."
     * @return 战斗策略推荐
     */
    String getStrategyRecommendation(String elfInfo, String monsterInfo);
    
    /**
     * 创建会话
     * @param userId 用户ID
     * @param title 会话标题
     * @param scene 使用场景
     * @return 会话ID
     */
    Long createSession(Long userId, String title, String scene);
    
    /**
     * 新增对话
     * @param sessionId 会话ID
     * @param content 用户输入内容
     * @return 对话响应
     */
    String addConversation(Long sessionId, String content);
    
    /**
     * 关闭会话
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 是否关闭成功
     */
    boolean closeSession(Long sessionId, Long userId);
    
    /**
     * 获取会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Map<String, Object>> getSessionList(Long userId);
    
    /**
     * 更新会话标题
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param title 新标题
     * @return 是否更新成功
     */
    boolean updateSessionTitle(Long sessionId, Long userId, String title);
    
    /**
     * 获取训练总结
     * @param userId 用户ID
     * @return 训练总结
     */
    String getTrainingSummary(Long userId);
    
    /**
     * SSE流式输出
     * @param sessionId 会话ID
     * @param content 用户输入内容
     * @param response HttpServletResponse
     * @param userId 用户ID
     */
    void streamAIAnalysis(String sessionId, String content, HttpServletResponse response, Long userId);
    
    /**
     * 检查用户AI调用次数
     * @param userId 用户ID
     * @return 是否可以调用
     */
    boolean checkAICallLimit(Long userId);
    
    /**
     * 获取用户今日AI调用次数
     * @param userId 用户ID
     * @return 今日调用次数
     */
    int getAITodayCallCount(Long userId);
}