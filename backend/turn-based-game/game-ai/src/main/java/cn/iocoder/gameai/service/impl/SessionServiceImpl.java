package cn.iocoder.gameai.service.impl;

import cn.iocoder.gameai.entity.ChatSession;
import cn.iocoder.gameai.entity.ChatMessage;
import cn.iocoder.gameai.mapper.ChatSessionMapper;
import cn.iocoder.gameai.mapper.ChatMessageMapper;
import cn.iocoder.gameai.service.SessionService;
import cn.iocoder.gamecommon.util.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话管理服务实现类
 */
@Service
public class SessionServiceImpl implements SessionService {
    
    @Autowired
    private ChatSessionMapper chatSessionMapper;
    
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    
    @Autowired
    private RedisUtil redisUtil;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Long createSession(Long userId, String title, String scene) {
        // 创建会话对象
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title != null ? title : "新对话");
        session.setScene(scene != null ? scene : "common");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setIsDeleted(0);
        
        // 插入数据库
        chatSessionMapper.insert(session);
        
        // 缓存到Redis，过期时间1小时
        String sessionKey = "session:" + session.getId();
        redisUtil.set(sessionKey, session, 3600);
        
        // 清除用户会话列表缓存
        String sessionListKey = "session:list:" + userId;
        redisUtil.delete(sessionListKey);
        
        return session.getId();
    }
    
    @Override
    @Transactional
    public boolean deleteSession(Long sessionId, Long userId) {
        // 验证会话是否属于该用户
        ChatSession session = chatSessionMapper.selectByIdAndUserId(sessionId, userId);
        if (session == null) {
            return false;
        }
        
        // 软删除会话
        session.setIsDeleted(1);
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);
        
        // 删除会话相关的所有消息
        chatMessageMapper.deleteBySessionId(sessionId);
        
        // 清除Redis缓存
        String sessionKey = "session:" + sessionId;
        redisUtil.delete(sessionKey);
        
        // 清除用户会话列表缓存
        String sessionListKey = "session:list:" + userId;
        redisUtil.delete(sessionListKey);
        
        // 清除会话消息缓存
        String messageKey = "messages:" + sessionId;
        redisUtil.delete(messageKey);
        
        return true;
    }
    
    @Override
    public List<ChatSession> getSessionList(Long userId) {
        // 先从Redis缓存获取
        String sessionListKey = "session:list:" + userId;
        Object cachedList = redisUtil.get(sessionListKey);
        if (cachedList != null && cachedList instanceof List) {
            return (List<ChatSession>) cachedList;
        }
        
        // 缓存不存在，从数据库查询
        List<ChatSession> sessions = chatSessionMapper.selectByUserId(userId);
        
        // 缓存到Redis，过期时间1小时
        redisUtil.set(sessionListKey, sessions, 3600);
        
        return sessions;
    }
    
    @Override
    public ChatSession getSessionById(Long sessionId, Long userId) {
        // 先从Redis缓存获取
        String sessionKey = "session:" + sessionId;
        Object cachedSession = redisUtil.get(sessionKey);
        if (cachedSession != null && cachedSession instanceof ChatSession) {
            ChatSession session = (ChatSession) cachedSession;
            // 验证会话是否属于该用户
            if (userId == null || session.getUserId().equals(userId)) {
                return session;
            }
        }
        
        // 缓存不存在或验证失败，从数据库查询
        ChatSession session = chatSessionMapper.selectByIdAndUserId(sessionId, userId);
        if (session != null) {
            // 缓存到Redis，过期时间1小时
            redisUtil.set(sessionKey, session, 3600);
        }
        
        return session;
    }
    
    @Override
    public Long addMessage(Long sessionId, String role, String content, String contentType) {
        // 创建消息对象
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setContentType(contentType != null ? contentType : "text");
        message.setCreateTime(LocalDateTime.now());
        
        // 插入数据库
        chatMessageMapper.insert(message);
        
        // 更新会话的更新时间
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(session);
            
            // 更新会话缓存
            String sessionKey = "session:" + sessionId;
            redisUtil.set(sessionKey, session, 3600);
            
            // 清除用户会话列表缓存
            String sessionListKey = "session:list:" + session.getUserId();
            redisUtil.delete(sessionListKey);
        }
        
        // 清除会话消息缓存
        String messageKey = "messages:" + sessionId;
        redisUtil.delete(messageKey);
        
        return message.getId();
    }
    
    @Override
    public List<ChatMessage> getRecentMessages(Long sessionId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10; // 默认保留最近10条
        }
        
        // 先从Redis缓存获取
        String messageKey = "messages:" + sessionId + ":" + limit;
        Object cachedMessages = redisUtil.get(messageKey);
        if (cachedMessages != null && cachedMessages instanceof List) {
            return (List<ChatMessage>) cachedMessages;
        }
        
        // 缓存不存在，从数据库查询
        List<ChatMessage> messages = chatMessageMapper.selectBySessionIdOrderByCreateTimeDesc(sessionId, limit);
        
        // 缓存到Redis，过期时间1小时
        redisUtil.set(messageKey, messages, 3600);
        
        return messages;
    }
    
    @Override
    public boolean updateSessionTitle(Long sessionId, Long userId, String title) {
        // 验证会话是否属于该用户
        ChatSession session = chatSessionMapper.selectByIdAndUserId(sessionId, userId);
        if (session == null) {
            return false;
        }
        
        // 更新标题
        session.setTitle(title);
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);
        
        // 更新Redis缓存
        String sessionKey = "session:" + sessionId;
        redisUtil.set(sessionKey, session, 3600);
        
        // 清除用户会话列表缓存
        String sessionListKey = "session:list:" + userId;
        redisUtil.delete(sessionListKey);
        
        return true;
    }
}