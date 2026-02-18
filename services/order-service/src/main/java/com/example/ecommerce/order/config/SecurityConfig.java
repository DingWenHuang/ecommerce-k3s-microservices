package com.example.ecommerce.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;

/**
 * order-service 安全策略：
 * - /orders/** 需要登入（由 Gateway 解析 JWT 後轉 header）
 * - demo 先不做更複雜的權限（例如 ADMIN 查全部訂單），先把「我的訂單」做完整
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, GatewayHeaderAuthFilter filter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/orders/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(filter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
