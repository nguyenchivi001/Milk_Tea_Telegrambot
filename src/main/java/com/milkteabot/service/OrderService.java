package com.milkteabot.service;

import com.milkteabot.model.*;
import com.milkteabot.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // ─────────────────────────────────────────────
    // Create new order from cart
    // ─────────────────────────────────────────────
    public Order createOrder(Long chatId, String customerName,
                             String username, List<CartItem> cartItems, String note) {

        int total = cartItems.stream()
                .mapToInt(CartItem::getSubtotal)
                .sum();

        String orderCode = generateOrderCode();

        Order order = Order.builder()
                .orderCode(orderCode)
                .chatId(chatId)
                .customerName(customerName)
                .username(username)
                .status(OrderStatus.PENDING)
                .total(total)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();

        List<OrderItem> orderItems = cartItems.stream()
                .map(cart -> OrderItem.builder()
                        .order(order)
                        .itemId(cart.getItemId())
                        .itemName(cart.getItemName())
                        .size(cart.getSize())
                        .quantity(cart.getQuantity())
                        .toppings(String.join(", ", cart.getToppings()))
                        .price(cart.getSubtotal())
                        .build())
                .toList();

        order.setItems(orderItems);

        Order saved = orderRepository.save(order);
        log.info("Order created: {} - {}đ", orderCode, total);
        return saved;
    }

    // ─────────────────────────────────────────────
    // Update order status
    // ─────────────────────────────────────────────
    public void saveCustomerMessageId(Long orderId, Integer messageId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setCustomerMessageId(messageId);
            orderRepository.save(order);
        });
    }

    public Optional<Order> findByOrderCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode);
    }

    public boolean updateStatus(String orderCode, OrderStatus status) {
        Optional<Order> opt = orderRepository.findByOrderCode(orderCode);
        if (opt.isEmpty()) return false;

        Order order = opt.get();
        order.setStatus(status);
        orderRepository.save(order);
        log.info("Updated order {} → {}", orderCode, status);
        return true;
    }

    // ─────────────────────────────────────────────
    // Customer order history
    // ─────────────────────────────────────────────
    public List<Order> getHistory(Long chatId) {
        return orderRepository.findTop5ByChatIdOrderByCreatedAtDesc(chatId);
    }

    // ─────────────────────────────────────────────
    // Daily summary
    // ─────────────────────────────────────────────
    public String getDailySummary() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Order> orders = orderRepository.findByCreatedAtBetween(startOfDay, endOfDay);

        long confirmed = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .count();

        int totalRevenue = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .mapToInt(Order::getTotal)
                .sum();

        return String.format(
                "*TỔNG KẾT HÔM NAY*\n" +
                        "====================\n" +
                        "Tổng đơn: *%d đơn*\n" +
                        "Doanh thu: *%,dđ*\n" +
                        "Cập nhật: %s",
                confirmed,
                totalRevenue,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM"))
        );
    }

    // ─────────────────────────────────────────────
    // Format order info to send to owner
    // ─────────────────────────────────────────────
    public String formatOrderForOwner(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("*ĐƠN HÀNG MỚI ").append(order.getOrderCode()).append("*\n");
        sb.append("====================\n");
        sb.append(order.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))).append("\n");
        sb.append(order.getCustomerName());
        if (order.getUsername() != null) {
            sb.append(" (@").append(order.getUsername()).append(")");
        }
        sb.append("\n====================\n");

        order.getItems().forEach(item -> {
            sb.append("- ").append(item.getItemName())
                    .append(" (").append(item.getSize()).append(")")
                    .append(" x").append(item.getQuantity()).append("\n");
            if (item.getToppings() != null && !item.getToppings().isEmpty()) {
                sb.append("  + ").append(item.getToppings()).append("\n");
            }
            sb.append("  ").append(String.format("%,d", item.getPrice())).append("đ\n");
        });

        sb.append("====================\n");
        sb.append("*TỔNG: ").append(String.format("%,d", order.getTotal())).append("đ*\n");

        if (order.getNote() != null && !order.getNote().isEmpty()) {
            sb.append("Ghi chú: _\"").append(order.getNote()).append("\"_\n");
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // Generate unique order code
    // ─────────────────────────────────────────────
    private String generateOrderCode() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        long count = orderRepository.count() + 1;
        return String.format("#ORD%s%02d", timestamp, count);
    }
}