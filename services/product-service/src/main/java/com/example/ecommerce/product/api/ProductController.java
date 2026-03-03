package com.example.ecommerce.product.api;

import com.example.ecommerce.product.api.dto.ProductDtos;
import com.example.ecommerce.product.domain.ProductType;
import com.example.ecommerce.product.service.ProductService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /**
     * 公開商品查詢 API
     * 前端分區會用：
     * - /products?type=NORMAL
     * - /products?type=FLASH_SALE
     */

    @GetMapping("/products")
    public ResponseEntity<List<ProductDtos.ProductResponse>> list(@RequestParam(name = "type", required = false) ProductType type) {
        return ResponseEntity.ok(service.listByType(type));
    }

    // ===== 管理 API（ADMIN）=====

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/products")
    public ResponseEntity<ProductDtos.ProductResponse> create(@RequestBody ProductDtos.CreateProductRequest req) {
        log.info("[admin.products.create] 建立商品 name={}, price={}, stock={}, type={}",
                req.name(), req.price(), req.stock(), req.productType());
        ProductDtos.ProductResponse result = service.create(req);
        log.info("[admin.products.create] 建立成功 id={}", result.id());
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/products/{id}/restock")
    public ResponseEntity<ProductDtos.ProductResponse> restock(@PathVariable("id") long id,
                                                               @RequestBody ProductDtos.RestockRequest req) {
        log.info("[admin.products.restock] 補貨 id={}, amount={}", id, req.amount());
        ProductDtos.ProductResponse result = service.restock(id, req.amount());
        log.info("[admin.products.restock] 補貨後庫存 id={}, stock={}", id, result.stock());
        return ResponseEntity.ok(result);
    }

    // ===== internal API（給 order-service）=====

    @PostMapping("/internal/products/{id}/reserve")
    public ResponseEntity<ProductDtos.ReserveResponse> reserve(@PathVariable("id") long id,
                                                               @RequestBody ProductDtos.ReserveRequest req) {
        ProductDtos.ReserveResponse result = service.reserve(id, req.amount());
        if (!result.success()) {
            log.warn("[internal.products.reserve] 扣庫存失敗 id={}, amount={}, reason={}", id, req.amount(), result.message());
        } else {
            log.info("[internal.products.reserve] 扣庫存成功 id={}, amount={}", id, req.amount());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/internal/products/{id}")
    public ResponseEntity<ProductDtos.ProductResponse> getProductInfo(@PathVariable("id") long id) {
        return ResponseEntity.ok(service.getProductInfo(id));
    }

    @PostMapping("/internal/products/{id}/reserve-flash-sale")
    public ResponseEntity<ProductDtos.ReserveResponse> reserveFlashSale(@PathVariable("id") long id, @RequestBody ProductDtos.ReserveRequest req) {
        ProductDtos.ReserveResponse result = service.reserveFlashSaleStock(id, req.amount());
        if (!result.success()) {
            log.warn("[internal.products.reserve-flash-sale] 搶購扣庫存失敗 id={}, reason={}", id, result.message());
        } else {
            log.info("[internal.products.reserve-flash-sale] 搶購扣庫存成功 id={}", id);
        }
        return ResponseEntity.ok(result);
    }
}
