package com.example.ecommerce.order.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 內部 API 的簡易驗證（demo）
 *
 * - 只要 header X-Internal-Token 正確，就允許存取 /internal/**
 * - 避免你 port-forward 後被外部呼叫到
 */
@Component
public class InternalApiAuth {

    private final String internalToken;
    private final HttpServletRequest request;

    public InternalApiAuth(
            @Value("${internal.api.token:}") String internalToken,
            HttpServletRequest request
    ) {
        this.internalToken = internalToken;
        this.request = request;
    }

    public void requireInternalToken() {
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_TOKEN is not configured");
        }

        String header = request.getHeader("X-Internal-Token");
        if (header == null || header.isBlank() || !internalToken.equals(header)) {
            throw new IllegalStateException("Unauthorized internal request");
        }
    }
}