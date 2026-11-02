package ee.askend.orders.dto;

import ee.askend.orders.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {

    static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getName(), item.getQuantity(), item.getUnitPrice(), item.lineTotal());
    }
}
