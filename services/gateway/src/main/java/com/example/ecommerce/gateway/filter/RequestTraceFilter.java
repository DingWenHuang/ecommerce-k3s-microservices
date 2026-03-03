package com.example.ecommerce.gateway.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);
    private static final String HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String finalRequestId = requestId;
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        log.debug("[trace] 收到請求 requestId={} method={} path={}", finalRequestId, method, path);

        MDC.put("requestId", finalRequestId);

        ServerHttpRequest mutated = exchange.getRequest()
                .mutate()
                .header(HEADER, finalRequestId)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build())
                .doFinally(signal -> {
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.debug("[trace] 回應完成 requestId={} method={} path={} status={} signal={}",
                            finalRequestId, method, path, statusCode, signal);
                    MDC.remove("requestId");
                });
    }

    @Override
    public int getOrder() {
        return -100; // 越小越先執行
    }
}