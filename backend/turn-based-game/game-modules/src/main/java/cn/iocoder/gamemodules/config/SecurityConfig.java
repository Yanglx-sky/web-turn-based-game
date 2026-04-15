package cn.iocoder.gamemodules.config;

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
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/doc.html", "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**", "/swagger-resources/**", "/favicon.ico").permitAll()
                .requestMatchers("/users/login", "/users/register", "/users/captcha").permitAll()
                .requestMatchers("/user-elf/**").permitAll()
                .requestMatchers("/elf/**").permitAll()
                .requestMatchers("/level/**").permitAll()
                .requestMatchers("/skill/**").permitAll()
                .requestMatchers("/battle/**").permitAll()
                .requestMatchers("/equip/**").permitAll()
                .requestMatchers("/potion/**").permitAll()
                .requestMatchers("/train/**").permitAll()
                .requestMatchers("/rank/**").permitAll()
                .requestMatchers("/achievement/**").permitAll()
                .requestMatchers("/ai/**").permitAll()
                .requestMatchers("/users/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

}