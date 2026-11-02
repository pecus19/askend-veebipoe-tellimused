package ee.askend.orders.controller;

import ee.askend.orders.IntegrationTest;
import ee.askend.orders.OrderTestData;
import ee.askend.orders.entity.OrderStatus;
import ee.askend.orders.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class OrderControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    OrderTestData data;

    @BeforeEach
    void seed() {
        data.clear();
        long first = data.order("ORD-1", OrderStatus.DELIVERED, "2026-10-20T09:15:00Z");
        data.item(first, "Coffee beans", 2, "14.90");
        data.item(first, "Milk frother", 1, "24.50");

        long second = data.order("ORD-2", OrderStatus.PAID, "2026-10-28T08:05:00Z");
        data.item(second, "Bottle", 4, "12.50");

        long third = data.order("ORD-3", OrderStatus.PAID, "2026-10-30T17:30:00Z");
        data.item(third, "Running shoes", 1, "119.00");
    }

    @Test
    void listsOrdersNewestFirstWithTheirItems() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[*].orderNumber", contains("ORD-3", "ORD-2", "ORD-1")))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void returnsOrderFieldsItemsAndTotals() throws Exception {
        mvc.perform(get("/api/orders/ORD-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-1"))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-20T09:15:00Z"))
                .andExpect(jsonPath("$.total").value(54.30))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("Coffee beans"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(14.90))
                .andExpect(jsonPath("$.items[0].lineTotal").value(29.80))
                .andExpect(jsonPath("$.items[1].name").value("Milk frother"));
    }

    @Test
    void filtersByStatus() throws Exception {
        mvc.perform(get("/api/orders").param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].orderNumber", contains("ORD-3", "ORD-2")))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void pagesThroughOrders() throws Exception {
        mvc.perform(get("/api/orders").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].orderNumber", contains("ORD-1")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void returnsAnEmptyPageWhenNothingMatches() throws Exception {
        mvc.perform(get("/api/orders").param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void answersNotFoundForAnUnknownOrder() throws Exception {
        mvc.perform(get("/api/orders/ORD-404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Order ORD-404 was not found"));
    }

    @Test
    void refusesAnUnknownStatus() throws Exception {
        mvc.perform(get("/api/orders").param("status", "SENT"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Failed to convert 'status' with value: 'SENT'"));
    }

    @Test
    void showsMoneyWithTwoDecimals() throws Exception {
        long id = data.order("ORD-ROUND", OrderStatus.NEW, "2026-10-31T10:00:00Z");
        data.item(id, "Gift card", 2, "5");

        mvc.perform(get("/api/orders/ORD-ROUND"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"unitPrice\":5.00")))
                .andExpect(content().string(containsString("\"total\":10.00")));
    }

    @Test
    void ordersCreatedAtTheSameTimeKeepTheirPlaceBetweenPages() throws Exception {
        long older = data.order("ORD-A", OrderStatus.NEW, "2026-10-31T10:00:00Z");
        data.item(older, "Item", 1, "1.00");
        long newer = data.order("ORD-B", OrderStatus.NEW, "2026-10-31T10:00:00Z");
        data.item(newer, "Item", 1, "1.00");

        mvc.perform(get("/api/orders").param("page", "0").param("size", "1"))
                .andExpect(jsonPath("$.content[*].orderNumber", contains("ORD-B")));
        mvc.perform(get("/api/orders").param("page", "1").param("size", "1"))
                .andExpect(jsonPath("$.content[*].orderNumber", contains("ORD-A")));
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void storesAndFiltersEveryStatusOfTheEnum(OrderStatus status) throws Exception {
        data.clear();
        long id = data.order("ORD-" + status, status, "2026-10-31T10:00:00Z");
        data.item(id, "Item", 1, "1.00");

        mvc.perform(get("/api/orders").param("status", status.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].orderNumber", contains("ORD-" + status)));
    }

    @Test
    void refusesAPageLargerThanTheLimit() throws Exception {
        mvc.perform(get("/api/orders").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid request parameters: size must be less than or equal to 100"));
    }

    @Test
    void answersInEnglishWhateverTheClientLanguage() throws Exception {
        mvc.perform(get("/api/orders").param("size", "101").header("Accept-Language", "ru"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid request parameters: size must be less than or equal to 100"));
    }

    @Test
    void refusesAnEmptyPageSize() throws Exception {
        mvc.perform(get("/api/orders").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid request parameters: size must be greater than or equal to 1"));
    }

    @Test
    void refusesANegativePage() throws Exception {
        mvc.perform(get("/api/orders").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid request parameters: page must be greater than or equal to 0"));
    }

    @Test
    void servesTheLastAllowedPageAsEmpty() throws Exception {
        mvc.perform(get("/api/orders")
                        .param("page", String.valueOf(OrderService.MAX_PAGE))
                        .param("size", String.valueOf(OrderService.MAX_PAGE_SIZE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void refusesAPageWhoseOffsetWouldOverflow() throws Exception {
        mvc.perform(get("/api/orders").param("page", String.valueOf(OrderService.MAX_PAGE + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Invalid request parameters: page must be less than or equal to " + OrderService.MAX_PAGE));
    }

    @Test
    void refusesANonNumericPage() throws Exception {
        mvc.perform(get("/api/orders").param("page", "first"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("page")));
    }
}
