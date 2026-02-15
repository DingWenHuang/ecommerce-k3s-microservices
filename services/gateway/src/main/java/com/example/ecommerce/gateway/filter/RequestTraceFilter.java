package com.example.ecommerce.gateway.filter;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Request Trace Filter（加分用但很實用）：
 * - 產生一個 X-Request-Id
 * - 轉發到下游服務，方便追 log / debug
 */
@Component
public class RequestTraceFilter implements GlobalFilter, Ordered {

    private static final String HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put("requestId", requestId);

        ServerHttpRequest mutated = exchange.getRequest()
                .mutate()
                .header(HEADER, requestId)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build())
                .doFinally(signal -> MDC.remove("requestId"));
    }

    @Override
    public int getOrder() {
        return -100; // 越小越先執行
    }
}