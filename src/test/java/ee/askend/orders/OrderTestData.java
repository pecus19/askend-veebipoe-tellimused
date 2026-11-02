package ee.askend.orders;

import ee.askend.orders.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class OrderTestData {

    private final JdbcClient jdbc;

    public void clear() {
        jdbc.sql("DELETE FROM orders").update();
    }

    public long order(String orderNumber, OrderStatus status, String createdAt) {
        return jdbc.sql("INSERT INTO orders (order_number, status, created_at) VALUES (?, ?, ?) RETURNING id")
                .params(orderNumber, status.name(), OffsetDateTime.ofInstant(Instant.parse(createdAt), ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }

    public void item(long orderId, String name, int quantity, String unitPrice) {
        jdbc.sql("INSERT INTO order_items (order_id, name, quantity, unit_price) VALUES (?, ?, ?, ?)")
                .params(orderId, name, quantity, new BigDecimal(unitPrice))
                .update();
    }
}
