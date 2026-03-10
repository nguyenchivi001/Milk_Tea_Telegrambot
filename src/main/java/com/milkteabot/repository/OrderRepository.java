package com.milkteabot.repository;

import com.milkteabot.model.Order;
import com.milkteabot.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findTop5ByChatIdOrderByCreatedAtDesc(Long chatId);

    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Order> findByStatus(OrderStatus status);
}