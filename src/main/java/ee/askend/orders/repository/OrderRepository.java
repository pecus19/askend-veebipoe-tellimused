package ee.askend.orders.repository;

import ee.askend.orders.entity.Order;
import ee.askend.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDescIdDesc(OrderStatus status, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);
}
