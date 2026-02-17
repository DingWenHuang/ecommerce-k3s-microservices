package com.example.ecommerce.product.service;

import com.example.ecommerce.product.api.dto.ProductDtos;
import com.example.ecommerce.product.domain.ProductEntity;
import com.example.ecommerce.product.repo.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品業務邏輯：
 * - list：公開商品列表
 * - create：新增商品（ADMIN）
 * - restock：補貨（ADMIN）
 * - reserve：原子扣庫存（internal，給 order-service）
 */
@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ProductDtos.ProductResponse> list() {
        return repo.findAll().stream()
                .map(p -> new ProductDtos.ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock()))
                .toList();
    }

    @Transactional
    public ProductDtos.ProductResponse create(ProductDtos.CreateProductRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("商品名稱不可為空");
        }
        if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("價格不可為空或小於 0");
        }
        if (req.stock() == null || req.stock() < 0) {
            throw new IllegalArgumentException("庫存不可為空或小於 0");
        }

        ProductEntity saved = repo.save(new ProductEntity(req.name(), req.price(), req.stock()));
        return new ProductDtos.ProductResponse(saved.getId(), saved.getName(), saved.getPrice(), saved.getStock());
    }

    @Transactional
    public ProductDtos.ProductResponse restock(long id, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("補貨數量需 > 0");

        int updated = repo.restockAtomic(id, amount);
        if (updated != 1) throw new IllegalArgumentException("商品不存在");

        ProductEntity p = repo.findById(id).orElseThrow();
        return new ProductDtos.ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock());
    }

    @Transactional
    public ProductDtos.ReserveResponse reserve(long id, int amount) {
        if (amount <= 0) return new ProductDtos.ReserveResponse(false, "扣庫存數量需 > 0");

        int updated = repo.reserveStockAtomic(id, amount);
        if (updated == 1) return new ProductDtos.ReserveResponse(true, "OK");

        // updated=0 表示庫存不足或商品不存在（demo 階段先用簡單訊息）
        return new ProductDtos.ReserveResponse(false, "OUT_OF_STOCK_OR_NOT_FOUND");
    }
}