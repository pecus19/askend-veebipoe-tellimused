package ee.askend.orders.service;

import ee.askend.orders.dto.OrderResponse;
import ee.askend.orders.dto.PageResponse;
import ee.askend.orders.entity.Order;
import ee.askend.orders.entity.OrderStatus;
import ee.askend.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    public static final int MAX_PAGE_SIZE = Order.ITEMS_BATCH_SIZE;

    public static final int MAX_PAGE = Integer.MAX_VALUE / MAX_PAGE_SIZE - 1;

    private final OrderRepository orders;

    public PageResponse<OrderResponse> list(OrderStatus status, int page, int size) {
        PageRequest request = PageRequest.of(page, size);
        Page<Order> found = status == null
                ? orders.findAllByOrderByCreatedAtDescIdDesc(request)
                : orders.findByStatusOrderByCreatedAtDescIdDesc(status, request);
        return PageResponse.from(found, OrderResponse::from);
    }

    public Optional<OrderResponse> find(String orderNumber) {
        return orders.findByOrderNumber(orderNumber).map(OrderResponse::from);
    }
}
