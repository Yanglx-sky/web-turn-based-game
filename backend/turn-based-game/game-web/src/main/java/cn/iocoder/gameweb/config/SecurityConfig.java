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
                // 文档相关路径
                .requestMatchers("/doc.html", "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**", "/swagger-resources/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/doc.html", "/api/v3/api-docs/**", "/api/webjars/**", "/api/swagger-ui/**", "/api/swagger-resources/**", "/api/favicon.ico").permitAll()
                // 认证相关路径
                .requestMatchers("/users/login", "/users/register", "/users/captcha").permitAll()
                .requestMatchers("/api/users/login", "/api/users/register", "/api/users/captcha").permitAll()
                // 关卡相关路径
                .requestMatchers("/levels/**").permitAll()
                .requestMatchers("/api/levels/**").permitAll()
                .requestMatchers("/level/**").permitAll()
                .requestMatchers("/api/level/**").permitAll()
                // 精灵相关路径
                .requestMatchers("/elf/**").permitAll()
                .requestMatchers("/api/elf/**").permitAll()
                .requestMatchers("/user-elf/**").permitAll()
                .requestMatchers("/api/user-elf/**").permitAll()
                // 战斗相关路径
                .requestMatchers("/battle/**").permitAll()
                .requestMatchers("/api/battle/**").permitAll()
                // 装备相关路径
                .requestMatchers("/equip/**").permitAll()
                .requestMatchers("/api/equip/**").permitAll()
                .requestMatchers("/equips/**").permitAll()
                .requestMatchers("/api/equips/**").permitAll()
                // 药水相关路径
                .requestMatchers("/potion/**").permitAll()
                .requestMatchers("/api/potion/**").permitAll()
                .requestMatchers("/potions/**").permitAll()
                .requestMatchers("/api/potions/**").permitAll()
                // 商店相关路径
                .requestMatchers("/shop/**").permitAll()
                .requestMatchers("/api/shop/**").permitAll()
                // 训练相关路径
                .requestMatchers("/train/**").permitAll()
                .requestMatchers("/api/train/**").permitAll()
                // 排行榜相关路径
                .requestMatchers("/rank/**").permitAll()
                .requestMatchers("/api/rank/**").permitAll()
                // 成就相关路径
                .requestMatchers("/achievement/**").permitAll()
                .requestMatchers("/api/achievement/**").permitAll()
                // AI相关路径
                .requestMatchers("/ai/**").permitAll()
                .requestMatchers("/api/ai/**").permitAll()
                // 用户相关路径
                .requestMatchers("/users/**").permitAll()
                .requestMatchers("/api/users/**").permitAll()
                // 聊天相关路径
                .requestMatchers("/chat/**").permitAll()
                .requestMatchers("/api/chat/**").permitAll()
                // 好友相关路径
                .requestMatchers("/friend/**").permitAll()
                .requestMatchers("/api/friend/**").permitAll()
                .requestMatchers("/friends/**").permitAll()
                .requestMatchers("/api/friends/**").permitAll()
                // 技能相关路径
                .requestMatchers("/skill/**").permitAll()
                .requestMatchers("/api/skill/**").permitAll()
                // 其他所有路径
                .anyRequest().permitAll()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}