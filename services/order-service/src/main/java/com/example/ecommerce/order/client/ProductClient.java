package com.example.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 呼叫 product-service 的 internal API
 * 注意：此 API 不經由 Gateway 對外暴露（更像實戰）
 */
@FeignClient(name = "productClient", url = "${PRODUCT_SERVICE_URL:http://product-service:8080}")
public interface ProductClient {

    record ReserveRequest(Integer amount) {}
    record ReserveResponse(boolean success, String message) {}

    // 內部扣庫存
    @PostMapping("/internal/products/{id}/reserve")
    ReserveResponse reserve(@PathVariable("id") long id, @RequestBody ReserveRequest req);

    // （可選）後面要算 totalAmount 需要拿 price
    // Demo 先不做這個 endpoint，暫時由下單 request 帶 unitPrice
}
