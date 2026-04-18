package cn.iocoder.gameweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())
                .xssProtection(xss -> xss.disable())
                .contentTypeOptions(contentType -> contentType.disable())
            )
            .authorizeHttpRequests(authorize -> authorize
                // 文档相关路径（公开）
                .requestMatchers("/doc.html", "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**", "/swagger-resources/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/doc.html", "/api/v3/api-docs/**", "/api/webjars/**", "/api/swagger-ui/**", "/api/swagger-resources/**", "/api/favicon.ico").permitAll()
                // 所有其他请求都放行，由AuthLoginInterceptor处理认证
                .anyRequest().permitAll()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}