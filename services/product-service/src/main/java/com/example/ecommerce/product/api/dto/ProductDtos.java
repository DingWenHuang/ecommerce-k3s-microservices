package com.example.ecommerce.product.api.dto;

import java.math.BigDecimal;

/**
 * DTO 集合（示範用集中在同檔案）
 */
public class ProductDtos {

    public record ProductResponse(Long id, String name, BigDecimal price, Integer stock) {}

    public record CreateProductRequest(String name, BigDecimal price, Integer stock) {}

    public record RestockRequest(Integer amount) {}

    public record ReserveRequest(Integer amount) {}

    public record ReserveResponse(boolean success, String message) {}
}
