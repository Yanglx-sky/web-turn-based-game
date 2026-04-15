package cn.iocoder.gameweb.config;

import cn.iocoder.gamecommon.interceptor.AILimitInterceptor;
import cn.iocoder.gamecommon.interceptor.AuthInterceptor;
import cn.iocoder.gamecommon.interceptor.BattleSecurityInterceptor;
import cn.iocoder.gamecommon.interceptor.LogInterceptor;
import cn.iocoder.gamecommon.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 注册所有拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private BattleSecurityInterceptor battleSecurityInterceptor;

    @Autowired
    private AILimitInterceptor aiLimitInterceptor;

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private LogInterceptor logInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局请求日志拦截器
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");

        // Knife4j文档路径
        String[] docPaths = {"/doc.html", "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**", "/swagger-resources/**", "/favicon.ico"};

        // 登录拦截器
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/users/login", "/users/register", "/users/captcha")
                .excludePathPatterns(docPaths);

        // 接口鉴权拦截器
        registry.addInterceptor(authInterceptor)
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
}