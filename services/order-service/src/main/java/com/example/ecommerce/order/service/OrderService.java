package com.example.ecommerce.order.service;

import com.example.ecommerce.order.api.dto.OrderDtos;
import com.example.ecommerce.order.client.ProductClient;
import com.example.ecommerce.order.domain.OrderEntity;
import com.example.ecommerce.order.domain.OrderItemEntity;
import com.example.ecommerce.order.repo.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public OrderService(OrderRepository orderRepository,
                        ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    /**
     * 搶購下單：單品項、單件
     * - 由 FlashSale worker 呼叫
     * - 金額由 product-service 查到的 price 決定（BigDecimal）
     */
    @Transactional
    public long createFlashSaleOrder(Long userId, Long productId) {
        ProductClient.ProductInfo product = productClient.getProductInfo(productId);
        if (!"FLASH_SALE".equals(product.productType())) {
            throw new IllegalStateException("非搶購類型商品");
        }

        BigDecimal unitPrice = product.price().setScale(2, RoundingMode.HALF_UP);
        BigDecimal lineAmount = unitPrice; // quantity=1
        BigDecimal totalAmount = lineAmount;

        OrderEntity order = new OrderEntity(userId, totalAmount, "CREATED");
        order.addItem(new OrderItemEntity(productId, 1, unitPrice, lineAmount));

        OrderEntity saved = orderRepository.save(order);
        return saved.getId();
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
