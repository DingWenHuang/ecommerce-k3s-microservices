package com.example.ecommerce.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;

/**
 * product-service 安全策略：
 * - 公開：GET /products
 * - 管理：/admin/** 需要 ADMIN（透過 Gateway header 判斷）
 * - internal：/internal/** 僅允許 cluster 內呼叫（本步先放行，由 gateway 不對外暴露即可）
 *
 * 之後 Step 9 我們會再強化 internal endpoint 的保護方式。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, GatewayHeaderAuthFilter gatewayHeaderAuthFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("GET", "/products/**").permitAll()
                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayHeaderAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
