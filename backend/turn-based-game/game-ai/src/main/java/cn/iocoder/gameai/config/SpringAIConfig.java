package cn.iocoder.gameai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI配置类
 * 配置ChatClient和Advisor
 */
@Configuration
public class SpringAIConfig {

    /**
     * 创建ChatClient Bean
     * 使用Spring AI的ChatClient进行AI调用
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
