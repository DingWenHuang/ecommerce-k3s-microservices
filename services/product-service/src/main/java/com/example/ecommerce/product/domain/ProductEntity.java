package com.example.ecommerce.product.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 商品資料表：
 * - price 使用 BigDecimal，避免浮點誤差（金融/金額必備）
 * - stock：庫存數量（整數）
 * - productType 用來區分一般/搶購邏輯
 */
@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length = 100)
    private String name;

    @Column(nullable=false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable=false)
    private Integer stock;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType = ProductType.NORMAL;

    public ProductEntity() {}

    public ProductEntity(String name, BigDecimal price, Integer stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.productType = productType == null ? ProductType.NORMAL : productType;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public Integer getStock() { return stock; }
    public Instant getCreatedAt() { return createdAt; }
    public ProductType getProductType() { return productType; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setProductType(ProductType productType) {this.productType = productType; }
}
