package com.example.ecommerce.product.service;

import com.example.ecommerce.product.api.dto.ProductDtos;
import com.example.ecommerce.product.domain.ProductEntity;
import com.example.ecommerce.product.domain.ProductType;
import com.example.ecommerce.product.repo.ProductRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 商品業務邏輯：
 * - listByType：取得不同商品類型的列表
 * - list：公開商品列表
 * - create：新增商品（ADMIN）
 * - restock：補貨（ADMIN）
 * - reserve：原子扣庫存（internal，給 order-service）
 * - getProductInfo：取得商品資訊（internal，給 order-service）
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ProductDtos.ProductResponse> listByType(ProductType type) {
        ProductType resolved = (type == null) ? ProductType.NORMAL : type;
        return repo.findByProductTypeOrderByIdAsc(resolved)
                .stream()
                .map(p -> new ProductDtos.ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock(), p.getProductType()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDtos.ProductResponse> list() {
        return repo.findAll().stream()
                .map(p -> new ProductDtos.ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock(), p.getProductType()))
                .toList();
    }

    @Transactional
    public ProductDtos.ProductResponse create(ProductDtos.CreateProductRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            log.warn("[products.create] 驗證失敗：商品名稱為空");
            throw new IllegalArgumentException("商品名稱不可為空");
        }
        if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("[products.create] 驗證失敗：價格不合法 price={}", req.price());
            throw new IllegalArgumentException("價格不可為空或小於 0");
        }
        if (req.stock() == null || req.stock() < 0) {
            log.warn("[products.create] 驗證失敗：庫存不合法 stock={}", req.stock());
            throw new IllegalArgumentException("庫存不可為空或小於 0");
        }

        ProductEntity saved = repo.save(new ProductEntity(req.name(), req.price(), req.stock()));
        log.info("[products.create] 商品已建立 id={}, name={}", saved.getId(), saved.getName());
        return new ProductDtos.ProductResponse(saved.getId(), saved.getName(), saved.getPrice(), saved.getStock(), saved.getProductType());
    }

    @Transactional
    public ProductDtos.ProductResponse restock(long id, int amount) {
        if (amount <= 0) {
            log.warn("[products.restock] 補貨數量不合法 id={}, amount={}", id, amount);
            throw new IllegalArgumentException("補貨數量需 > 0");
        }

        int updated = repo.restockAtomic(id, amount);
        if (updated != 1) {
            log.warn("[products.restock] 商品不存在 id={}", id);
            throw new IllegalArgumentException("商品不存在");
        }

        ProductEntity p = repo.findById(id).orElseThrow();
        log.info("[products.restock] 補貨完成 id={}, amount={}, 新庫存={}", id, amount, p.getStock());
        return new ProductDtos.ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock(), p.getProductType());
    }

    @Transactional
    public ProductDtos.ReserveResponse reserve(long id, int amount) {
        if (amount <= 0) {
            log.warn("[products.reserve] 扣庫存數量不合法 id={}, amount={}", id, amount);
            return new ProductDtos.ReserveResponse(false, "扣庫存數量需 > 0");
        }

        int updated = repo.reserveStockAtomic(id, amount);
        if (updated == 1) return new ProductDtos.ReserveResponse(true, "OK");

        // updated=0 表示庫存不足或商品不存在（demo 階段先用簡單訊息）
        log.warn("[products.reserve] 庫存不足或商品不存在 id={}, amount={}", id, amount);
        return new ProductDtos.ReserveResponse(false, "OUT_OF_STOCK_OR_NOT_FOUND");
    }

    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse getProductInfo(long id) {
        ProductEntity p = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("[products.info] 商品不存在 id={}", id);
                    return new IllegalArgumentException("商品不存在: " + id);
                });
        return new ProductDtos.ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock(), p.getProductType());
    }

    /**
     * 搶購商品（FLASH_SALE）的扣庫存：一次限購 1
     */
    @Transactional
    public ProductDtos.ReserveResponse reserveFlashSaleStock(long id, int amount) {
        if (amount != 1) {
            log.warn("[products.reserve-flash-sale] 搶購數量不合法 id={}, amount={}", id, amount);
            return new ProductDtos.ReserveResponse(false, "搶購商品數量需為1");
        }

        Optional<ProductEntity> productEntity = repo.findById(id);
        if (productEntity.isEmpty()) {
            log.warn("[products.reserve-flash-sale] 商品不存在 id={}", id);
            return new ProductDtos.ReserveResponse(false, "商品不存在: " + id);
        }

        ProductEntity product = productEntity.get();
        if (product.getProductType() != ProductType.FLASH_SALE) {
            log.warn("[products.reserve-flash-sale] 商品類型非 FLASH_SALE id={}, type={}", id, product.getProductType());
            return new ProductDtos.ReserveResponse(false, "搶購功能僅限搶購類型商品");
        }

        int updated = repo.reserveStockAtomic(id, amount);
        if (updated == 1) {
            log.info("[products.reserve-flash-sale] 搶購扣庫存成功 id={}", id);
           return new ProductDtos.ReserveResponse(true, "OK");
        } else {
            log.warn("[products.reserve-flash-sale] 搶購庫存不足 id={}", id);
            return new ProductDtos.ReserveResponse(false, "庫存不足");
        }
    }
}