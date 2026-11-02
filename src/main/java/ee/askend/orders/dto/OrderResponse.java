package ee.askend.orders.dto;

import ee.askend.orders.entity.Order;
import ee.askend.orders.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String orderNumber,
        OrderStatus status,
        Instant createdAt,
        BigDecimal total,
        List<OrderItemResponse> items) {

    private static final BigDecimal MONEY_ZERO = BigDecimal.ZERO.setScale(2);

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        BigDecimal total = items.stream()
                .map(OrderItemResponse::lineTotal)
                .reduce(MONEY_ZERO, BigDecimal::add);
        return new OrderResponse(order.getOrderNumber(), order.getStatus(), order.getCreatedAt(), total, items);
    }
}
