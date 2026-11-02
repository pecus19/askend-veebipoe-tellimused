package ee.askend.orders.controller;

import ee.askend.orders.dto.OrderResponse;
import ee.askend.orders.dto.PageResponse;
import ee.askend.orders.entity.OrderStatus;
import ee.askend.orders.service.OrderService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(OrderService.MAX_PAGE) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(OrderService.MAX_PAGE_SIZE) int size) {
        return orderService.list(status, page, size);
    }

    @GetMapping("/{orderNumber}")
    public OrderResponse get(@PathVariable String orderNumber) {
        return orderService.find(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order " + orderNumber + " was not found"));
    }
}
