package com.example.ecommerce.order.service;

import com.example.ecommerce.order.api.dto.OrderDtos;
import com.example.ecommerce.order.client.ProductClient;
import com.example.ecommerce.order.domain.OrderEntity;
import com.example.ecommerce.order.domain.OrderItemEntity;
import com.example.ecommerce.order.redis.RedisLockService;
import com.example.ecommerce.order.repo.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 下單核心流程（搶購重點）：
 * 1) 取得商品鎖（Redis）
 * 2) 呼叫 product-service internal reserve（DB 原子扣庫存）
 * 3) 成功才寫入訂單（PostgreSQL）
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final RedisLockService lockService;

    public OrderService(OrderRepository orderRepository,
                        ProductClient productClient,
                        RedisLockService lockService) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.lockService = lockService;
    }

    // TODO 這個方法會在下一步被修改為搶購邏輯，這邊先不改動
    @Transactional
    public OrderDtos.CreateOrderResult createOrder(Long userId, OrderDtos.CreateOrderRequest req) {
        if (req.productId() == null || req.quantity() == null || req.unitPrice() == null) {
            return new OrderDtos.CreateOrderResult(false, "BAD_REQUEST", null);
        }
        if (req.quantity() <= 0) {
            return new OrderDtos.CreateOrderResult(false, "QTY_MUST_BE_POSITIVE", null);
        }
        if (req.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            return new OrderDtos.CreateOrderResult(false, "PRICE_INVALID", null);
        }

        String lockKey = "lock:product:" + req.productId();
        String lockValue = lockService.tryLock(lockKey, Duration.ofSeconds(3));
        if (lockValue == null) {
            // 代表目前有其他人正在搶同一商品（demo 可回 429 或 409）
            return new OrderDtos.CreateOrderResult(false, "BUSY_TRY_AGAIN", null);
        }

        try {
            // 1) 扣庫存（DB 原子）
            ProductClient.ReserveResponse reserve = productClient.reserve(
                    req.productId(),
                    new ProductClient.ReserveRequest(req.quantity())
            );

            if (!reserve.success()) {
                return new OrderDtos.CreateOrderResult(false, reserve.message(), null);
            }

            // 2) 建立訂單（BigDecimal 計算）
            BigDecimal lineAmount = req.unitPrice().multiply(BigDecimal.valueOf(req.quantity()));
            OrderEntity order = new OrderEntity(userId, lineAmount, "CREATED");
            order.addItem(new OrderItemEntity(req.productId(), req.quantity(), req.unitPrice(), lineAmount));

            OrderEntity saved = orderRepository.save(order);

            var resp = new OrderDtos.OrderResponse(
                    saved.getId(),
                    saved.getTotalAmount(),
                    saved.getStatus(),
                    saved.getItems().stream()
                            .map(i -> new OrderDtos.OrderItemResponse(i.getProductId(), i.getQuantity(), i.getUnitPrice(), i.getLineAmount()))
                            .toList()
            );

            return new OrderDtos.CreateOrderResult(true, "OK", resp);
        } finally {
            lockService.unlock(lockKey, lockValue);
        }
    }

    @Transactional
    public OrderDtos.CreateOrderResult createNormalOrder(Long userId, OrderDtos.CreateNormalOrderRequest request) {
        OrderDtos.CreateOrderResult validatedRequest = validateRequest(request);
        if (!"VALID_REQUEST".equals(validatedRequest.message())) {
            return validatedRequest;
        }

        // 1) 讀取商品資訊（price/type）
        List<ResolvedItem> resolvedItems = new ArrayList<>();
        for (OrderDtos.CreateNormalOrderItem item : request.items()) {
            ProductClient.ProductInfo product = productClient.getProductInfo(item.productId());

            if (!"NORMAL".equals(product.productType())) {
                return new OrderDtos.CreateOrderResult(false, "ILLEGAL_PRODUCT_TYPE", null);
            }

            int qty = item.quantity();
            BigDecimal unitPrice = product.price().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineAmount = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

            resolvedItems.add(new ResolvedItem(item.productId(), qty, unitPrice, lineAmount));
        }

        // 2) 逐筆扣庫存（若中途失敗，這版先丟錯；後續可加 release 補償）
        for (ResolvedItem item : resolvedItems) {
            productClient.reserve(item.productId(), new ProductClient.ReserveRequest(item.quantity()));
        }

        // 3) 計算總金額並寫入訂單
        BigDecimal totalAmount = resolvedItems.stream()
                .map(ResolvedItem::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        OrderEntity order = new OrderEntity(userId, totalAmount, "CREATED");
        for (ResolvedItem item : resolvedItems) {
            order.addItem(new OrderItemEntity(item.productId(), item.quantity(), item.unitPrice(), item.lineAmount()));
        }

        OrderEntity saved = orderRepository.save(order);
        var resp = new OrderDtos.OrderResponse(
                saved.getId(),
                saved.getTotalAmount(),
                saved.getStatus(),
                saved.getItems().stream()
                        .map(i -> new OrderDtos.OrderItemResponse(i.getProductId(), i.getQuantity(), i.getUnitPrice(), i.getLineAmount()))
                        .toList()
        );

        return new OrderDtos.CreateOrderResult(true, "OK", resp);
    }

    private OrderDtos.CreateOrderResult validateRequest(OrderDtos.CreateNormalOrderRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            return new OrderDtos.CreateOrderResult(false, "BAD_REQUEST", null);
        }
        for (OrderDtos.CreateNormalOrderItem item : request.items()) {
            if (item.productId() == null) {
                return new OrderDtos.CreateOrderResult(false, "BAD_REQUEST", null);
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                return new OrderDtos.CreateOrderResult(false, "QTY_MUST_BE_POSITIVE", null);
            }
        }
        return new OrderDtos.CreateOrderResult(false, "VALID_REQUEST", null);
    }

    private record ResolvedItem(Long productId, int quantity, BigDecimal unitPrice, BigDecimal lineAmount) {}

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> listMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(o -> new OrderDtos.OrderResponse(
                        o.getId(),
                        o.getTotalAmount(),
                        o.getStatus(),
                        o.getItems().stream()
                                .map(i -> new OrderDtos.OrderItemResponse(i.getProductId(), i.getQuantity(), i.getUnitPrice(), i.getLineAmount()))
                                .toList()
                ))
                .toList();
    }
}
