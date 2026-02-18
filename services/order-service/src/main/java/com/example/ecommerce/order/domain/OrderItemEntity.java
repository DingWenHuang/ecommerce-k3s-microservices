package com.example.ecommerce.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 訂單明細：
 * - unitPrice / lineAmount 使用 BigDecimal
 */
@Entity
@Table(name = "order_item", indexes = {
        @Index(name = "idx_order_item_product_id", columnList = "productId")
})
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_id", nullable=false)
    private OrderEntity order;

    @Column(nullable=false)
    private Long productId;

    @Column(nullable=false)
    private Integer quantity;

    @Column(nullable=false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable=false, precision = 19, scale = 2)
    private BigDecimal lineAmount;

    public OrderItemEntity() {}

    public OrderItemEntity(Long productId, Integer quantity, BigDecimal unitPrice, BigDecimal lineAmount) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineAmount = lineAmount;
    }

    public Long getId() { return id; }
    public OrderEntity getOrder() { return order; }
    public Long getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineAmount() { return lineAmount; }

    public void setId(Long id) { this.id = id; }
    public void setOrder(OrderEntity order) { this.order = order; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setLineAmount(BigDecimal lineAmount) { this.lineAmount = lineAmount; }
}
