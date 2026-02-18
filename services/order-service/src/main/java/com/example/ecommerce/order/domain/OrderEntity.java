package com.example.ecommerce.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 訂單主檔：
 * - totalAmount 使用 BigDecimal（價格精準）
 */
@Entity
@Table(name = "order_header", indexes = {
        @Index(name = "idx_order_user_id", columnList = "userId")
})
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable=false, length = 20)
    private String status; // CREATED / PAID / CANCELLED（demo 先用 CREATED）

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    public OrderEntity() {}

    public OrderEntity(Long userId, BigDecimal totalAmount, String status) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public void addItem(OrderItemEntity item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItemEntity> getItems() { return items; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
