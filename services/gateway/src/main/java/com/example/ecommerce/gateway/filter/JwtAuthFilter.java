package com.example.ecommerce.gateway.filter;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * JWT 驗證 Filter（骨架先做好，Step 7 會完成真正的安全流程）
 *
 * 目前策略：
 * - 放行：/actuator/** 以及 /auth/**（登入註冊）
 * - 其他路徑：若帶 Authorization: Bearer <token> 就嘗試驗證
 *   - 驗證失敗回 401
 *   - 驗證成功把 userId/roles 透過 header 傳給下游（之後可改成更嚴謹）
 *
 * 注意：目前還沒有 auth-service 發 token，所以你暫時可以先不啟用強制驗證
 *       我們用一個開關（JWT_ENFORCE）來控制。
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    @Value("${security.jwt.enforce:false}")
    private boolean enforce;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 放行健康檢查與 auth API
        if (matcher.match("/actuator/**", path) || matcher.match("/auth/**", path)) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            if (!enforce) return chain.filter(exchange); // demo 階段先不強制
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        if (jwtSecret == null || jwtSecret.isBlank()) {
            // 代表你沒有把 JWT_SECRET 注入，若 enforce=true 會直接拒絕
            if (!enforce) return chain.filter(exchange);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = auth.substring("Bearer ".length());
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 你可以在 Step 7 再把 claims 設計完整（userId, roles）
            String userId = String.valueOf(claims.get("userId", Object.class));
            String roles = String.valueOf(claims.get("roles", Object.class));

            var mutatedReq = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", roles)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedReq).build());
        } catch (Exception e) {
            if (!enforce) return chain.filter(exchange);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -50; // 在 trace filter 之後、路由之前
    }
}
