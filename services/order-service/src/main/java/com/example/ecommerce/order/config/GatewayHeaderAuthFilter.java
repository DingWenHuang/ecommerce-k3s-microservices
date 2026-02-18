package com.example.ecommerce.order.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 與 product-service 相同：從 Gateway 轉發 header 建立 Authentication
 * - X-User-Id: 使用者 id（字串）
 * - X-User-Roles: USER / ADMIN
 */
@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String roles = request.getHeader("X-User-Roles");
        String userId = request.getHeader("X-User-Id");

        if (userId != null && !userId.isBlank()) {
            var authorities = (roles == null || roles.isBlank())
                    ? List.<SimpleGrantedAuthority>of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + roles));

            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
