package cn.iocoder.gameai.service;

import cn.iocoder.gameai.entity.ChatSession;
import cn.iocoder.gameai.entity.ChatMessage;

import java.util.List;

/**
 * 会话管理服务接口
 */
public interface SessionService {
    
    /**
     * 创建会话
     * @param userId 用户ID
     * @param title 会话标题
     * @param scene 使用场景
     * @return 会话ID
     */
    Long createSession(Long userId, String title, String scene);
    
    /**
     * 删除会话
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteSession(Long sessionId, Long userId);
    
    /**
     * 查询用户的会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> getSessionList(Long userId);
    
    /**
     * 获取会话详情
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 会话对象
     */
    ChatSession getSessionById(Long sessionId, Long userId);
    
    /**
     * 添加消息到会话
     * @param sessionId 会话ID
     * @param role 消息角色
     * @param content 消息内容
     * @param contentType 消息类型
     * @return 消息ID
     */
    Long addMessage(Long sessionId, String role, String content, String contentType);
    
    /**
     * 获取会话的最近消息
     * @param sessionId 会话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<ChatMessage> getRecentMessages(Long sessionId, Integer limit);
    
    /**
     * 更新会话标题
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param title 新标题
     * @return 是否更新成功
     */
    boolean updateSessionTitle(Long sessionId, Long userId, String title);
}