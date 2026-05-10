package cn.iocoder.gameai.advisor;

import cn.iocoder.gameai.entity.ChatMessage;
import cn.iocoder.gameai.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 会话记忆Advisor
 * 从数据库加载历史消息作为上下文
 */
@Slf4j
@Component
public class ChatMemoryAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final SessionService sessionService;

    public ChatMemoryAdvisor(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public String getName() {
        return "ChatMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 从请求中获取sessionId
        Map<String, Object> context = advisedRequest.adviseContext();
        Long sessionId = (Long) context.get("sessionId");
        
        if (sessionId != null) {
            // 加载历史消息
            List<ChatMessage> recentMessages = sessionService.getRecentMessages(sessionId, 10);
            
            // 构建历史消息内容
            StringBuilder historyContext = new StringBuilder();
            if (!recentMessages.isEmpty()) {
                historyContext.append("【对话历史】\n");
                // 倒序遍历消息（因为查询结果是降序），构建正序上下文（从旧到新）
                for (int i = recentMessages.size() - 1; i >= 0; i--) {
                    ChatMessage message = recentMessages.get(i);
                    if ("user".equals(message.getRole())) {
                        historyContext.append("训练师：").append(message.getContent()).append("\n");
                    } else if ("assistant".equals(message.getRole())) {
                        historyContext.append("AI：").append(message.getContent()).append("\n");
                    }
                }
                historyContext.append("\n");
            }
            
            // 将历史上下文添加到用户消息前面
            String originalUserText = advisedRequest.userText();
            String newUserText = historyContext.toString() + originalUserText;
            
            // 创建新的请求
            AdvisedRequest newRequest = AdvisedRequest.from(advisedRequest)
                    .userText(newUserText)
                    .build();
            
            log.info("[会话记忆] 加载了 {} 条历史消息", recentMessages.size());
            
            return chain.nextAroundCall(newRequest);
        }
        
        return chain.nextAroundCall(advisedRequest);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        // 从请求中获取sessionId
        Map<String, Object> context = advisedRequest.adviseContext();
        Long sessionId = (Long) context.get("sessionId");
        
        if (sessionId != null) {
            // 加载历史消息
            List<ChatMessage> recentMessages = sessionService.getRecentMessages(sessionId, 10);
            
            // 构建历史消息内容
            StringBuilder historyContext = new StringBuilder();
            if (!recentMessages.isEmpty()) {
                historyContext.append("【对话历史】\n");
                // 倒序遍历消息（因为查询结果是降序），构建正序上下文（从旧到新）
                for (int i = recentMessages.size() - 1; i >= 0; i--) {
                    ChatMessage message = recentMessages.get(i);
                    if ("user".equals(message.getRole())) {
                        historyContext.append("训练师：").append(message.getContent()).append("\n");
                    } else if ("assistant".equals(message.getRole())) {
                        historyContext.append("AI：").append(message.getContent()).append("\n");
                    }
                }
                historyContext.append("\n");
            }
            
            // 将历史上下文添加到用户消息前面
            String originalUserText = advisedRequest.userText();
            String newUserText = historyContext.toString() + originalUserText;
            
            // 创建新的请求
            AdvisedRequest newRequest = AdvisedRequest.from(advisedRequest)
                    .userText(newUserText)
                    .build();
            
            log.info("[会话记忆-流式] 加载了 {} 条历史消息", recentMessages.size());
            
            return chain.nextAroundStream(newRequest);
        }
        
        return chain.nextAroundStream(advisedRequest);
    }
}
