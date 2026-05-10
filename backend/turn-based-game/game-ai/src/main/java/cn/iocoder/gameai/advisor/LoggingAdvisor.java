package cn.iocoder.gameai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 日志记录Advisor
 * 记录AI调用的输入和输出日志
 */
@Slf4j
@Component
public class LoggingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    @Override
    public String getName() {
        return "LoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        log.info("[AI调用] 输入: {}", advisedRequest.userText());
        
        AdvisedResponse response = chain.nextAroundCall(advisedRequest);
        
        log.info("[AI调用] 输出: {}", response.response().getResult().getOutput().getText());
        
        return response;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        log.info("[AI流式调用] 输入: {}", advisedRequest.userText());
        
        Flux<AdvisedResponse> flux = chain.nextAroundStream(advisedRequest);
        
        return flux.doOnNext(response -> {
            if (response != null && response.response() != null && response.response().getResult() != null 
                && response.response().getResult().getOutput() != null) {
                log.info("[AI流式调用] 输出: {}", response.response().getResult().getOutput().getText());
            }
        }).doOnComplete(() -> {
            log.info("[AI流式调用] 完成");
        });
    }
}
