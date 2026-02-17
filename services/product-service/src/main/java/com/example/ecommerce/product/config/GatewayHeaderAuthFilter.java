package com.example.ecommerce.product.config;

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
 * 簡化版身份來源：
 * - Gateway 解析 JWT 後，轉發 header：
 *   X-User-Id / X-User-Roles
 *
 * product-service 只要信任 gateway（同 cluster 內），就能用這些 header 建立 Authentication。
 *
 * 注意：
 * - 這不是最嚴謹的零信任做法，但 demo 足夠展示 RBAC 與 Gateway 統一入口。
 */
@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String roles = request.getHeader("X-User-Roles");
        String userId = request.getHeader("X-User-Id");

        if (roles != null && !roles.isBlank()) {
            // roles 目前只放單一角色字串（USER 或 ADMIN）
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roles));
            var auth = new UsernamePasswordAuthenticationToken(userId == null ? "unknown" : userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
