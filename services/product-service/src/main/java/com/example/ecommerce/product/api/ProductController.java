package com.example.ecommerce.product.api;

import com.example.ecommerce.product.api.dto.ProductDtos;
import com.example.ecommerce.product.service.ProductService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 設計：
 * - /products：公開
 * - /admin/products：管理（ADMIN）
 * - /internal/products：內部呼叫（給 order-service）
 */
@RestController
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // ===== 公開 API =====

    @GetMapping("/products")
    public ResponseEntity<List<ProductDtos.ProductResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    // ===== 管理 API（ADMIN）=====

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/products")
    public ResponseEntity<ProductDtos.ProductResponse> create(@RequestBody ProductDtos.CreateProductRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/products/{id}/restock")
    public ResponseEntity<ProductDtos.ProductResponse> restock(@PathVariable("id") long id,
                                                               @RequestBody ProductDtos.RestockRequest req) {
        return ResponseEntity.ok(service.restock(id, req.amount()));
    }

    // ===== internal API（給 order-service）=====

    @PostMapping("/internal/products/{id}/reserve")
    public ResponseEntity<ProductDtos.ReserveResponse> reserve(@PathVariable("id") long id,
                                                               @RequestBody ProductDtos.ReserveRequest req) {
        return ResponseEntity.ok(service.reserve(id, req.amount()));
    }
}
