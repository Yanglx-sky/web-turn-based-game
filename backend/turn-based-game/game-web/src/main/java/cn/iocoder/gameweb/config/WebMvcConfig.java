package cn.iocoder.gameweb.config;

import cn.iocoder.gamecommon.interceptor.AILimitInterceptor;
import cn.iocoder.gamecommon.interceptor.AuthLoginInterceptor;
import cn.iocoder.gamecommon.interceptor.BattleSecurityInterceptor;
import cn.iocoder.gamecommon.interceptor.LogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 注册所有拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private BattleSecurityInterceptor battleSecurityInterceptor;

    @Autowired
    private AILimitInterceptor aiLimitInterceptor;

    @Autowired
    private AuthLoginInterceptor authLoginInterceptor;

    @Autowired
    private LogInterceptor logInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局请求日志拦截器
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/favicon.ico");

        // Knife4j文档路径
        String[] docPaths = {"/doc.html", "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**", "/swagger-resources/**", "/favicon.ico"};

        // 登录和鉴权拦截器
        registry.addInterceptor(authLoginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/users/login", "/users/register", "/users/captcha")
                .excludePathPatterns(docPaths);

        // 战斗安全拦截器
        registry.addInterceptor(battleSecurityInterceptor)
                .addPathPatterns("/battle/**");

        // AI调用限流拦截器
        registry.addInterceptor(aiLimitInterceptor)
                .addPathPatterns("/ai/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}