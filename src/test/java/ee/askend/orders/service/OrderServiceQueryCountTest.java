package ee.askend.orders.service;

import ee.askend.orders.IntegrationTest;
import ee.askend.orders.OrderTestData;
import ee.askend.orders.dto.OrderResponse;
import ee.askend.orders.dto.PageResponse;
import ee.askend.orders.entity.OrderStatus;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class OrderServiceQueryCountTest {

    @Autowired
    OrderService service;

    @Autowired
    OrderTestData data;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    Statistics statistics;

    @BeforeEach
    void setUp() {
        data.clear();
        // One more than a full page, so the count query runs and the largest page is read.
        for (int i = 1; i <= OrderService.MAX_PAGE_SIZE + 1; i++) {
            String createdAt = Instant.parse("2026-10-01T10:00:00Z").plusSeconds(60L * i).toString();
            long id = data.order("ORD-%03d".formatted(i), OrderStatus.NEW, createdAt);
            data.item(id, "Item A", 1, "1.00");
            data.item(id, "Item B", 2, "2.00");
        }
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void readsTheLargestPageWithThreeQueries() {
        PageResponse<OrderResponse> page = service.list(null, 0, OrderService.MAX_PAGE_SIZE);

        assertThat(page.content()).hasSize(OrderService.MAX_PAGE_SIZE);
        assertThat(page.content()).allSatisfy(order -> assertThat(order.items()).hasSize(2));
        // The page of orders, the total count, and the items of all its orders in one IN query.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void readsOneOrderWithTwoQueries() {
        OrderResponse order = service.find("ORD-007").orElseThrow();

        assertThat(order.items()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }
}
