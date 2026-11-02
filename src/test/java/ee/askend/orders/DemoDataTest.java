package ee.askend.orders;

import ee.askend.orders.dto.OrderResponse;
import ee.askend.orders.dto.PageResponse;
import ee.askend.orders.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.liquibase.contexts=demo")
@Import(TestcontainersConfiguration.class)
class DemoDataTest {

    @Autowired
    OrderService service;

    @Test
    void loadsTheDemoOrders() {
        PageResponse<OrderResponse> page = service.list(null, 0, 100);

        assertThat(page.totalElements()).isEqualTo(7);
        assertThat(page.content().getFirst().orderNumber()).isEqualTo("ORD-2026-000007");
        assertThat(page.content()).allSatisfy(order -> assertThat(order.items()).isNotEmpty());
    }
}
