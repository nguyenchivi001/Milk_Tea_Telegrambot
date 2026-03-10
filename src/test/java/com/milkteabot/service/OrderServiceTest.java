package com.milkteabot.service;

import com.milkteabot.model.*;
import com.milkteabot.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private CartItem cartItem(String id, String name, int price, int qty, List<String> toppings) {
        return new CartItem(id, name, "M", qty, toppings, price);
    }

    // ─────────────────────────────────────────────
    // createOrder
    // ─────────────────────────────────────────────

    @Test
    void createOrder_shouldCalculateTotalFromCartItems() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<CartItem> cart = List.of(
                cartItem("TS01", "Trà Sữa Trân Châu Đen", 35000, 2, List.of()),  // 70000
                cartItem("CF01", "Cà Phê Đen", 25000, 1, List.of())               // 25000
        );

        Order order = orderService.createOrder(123L, "Nguyen Van A", "userA", cart, "");

        assertThat(order.getTotal()).isEqualTo(95000);
    }

    @Test
    void createOrder_shouldSetStatusToPending() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(123L, "A", null,
                List.of(cartItem("TS01", "Trà Sữa", 35000, 1, List.of())), null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_shouldMapCartItemsToOrderItems() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<CartItem> cart = List.of(
                cartItem("TS01", "Trà Sữa Trân Châu Đen", 35000, 2,
                        List.of("Trân Châu Đen", "Kem Tươi"))
        );

        Order order = orderService.createOrder(123L, "A", null, cart, "ít đường");

        assertThat(order.getItems()).hasSize(1);
        OrderItem item = order.getItems().get(0);
        assertThat(item.getItemId()).isEqualTo("TS01");
        assertThat(item.getItemName()).isEqualTo("Trà Sữa Trân Châu Đen");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getToppings()).isEqualTo("Trân Châu Đen, Kem Tươi");
    }

    @Test
    void createOrder_orderCodeShouldMatchPattern() {
        // count() = 4 → code suffix = 05
        when(orderRepository.count()).thenReturn(4L);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(123L, "A", null,
                List.of(cartItem("TS01", "X", 35000, 1, List.of())), null);

        // Pattern: #ORD + yyMMddHHmm (10 digits) + 05
        assertThat(order.getOrderCode()).matches("#ORD\\d{10}05");
    }

    // ─────────────────────────────────────────────
    // updateStatus
    // ─────────────────────────────────────────────

    @Test
    void updateStatus_shouldReturnTrueAndUpdateWhenOrderExists() {
        Order order = Order.builder().orderCode("ORD001").status(OrderStatus.PENDING).build();
        when(orderRepository.findByOrderCode("ORD001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = orderService.updateStatus("ORD001", OrderStatus.CONFIRMED);

        assertThat(result).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    void updateStatus_shouldReturnFalseWhenOrderNotFound() {
        when(orderRepository.findByOrderCode("NOT_FOUND")).thenReturn(Optional.empty());

        boolean result = orderService.updateStatus("NOT_FOUND", OrderStatus.CONFIRMED);

        assertThat(result).isFalse();
        verify(orderRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    // formatOrderForOwner
    // ─────────────────────────────────────────────

    @Test
    void formatOrderForOwner_shouldContainOrderCodeCustomerAndTotal() {
        Order order = Order.builder()
                .orderCode("#ORD001")
                .customerName("Nguyen Van A")
                .username("userA")
                .total(95000)
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        OrderItem.builder()
                                .itemName("Trà Sữa Trân Châu Đen")
                                .size("M").quantity(2)
                                .toppings("Trân Châu Đen")
                                .price(70000)
                                .build()
                ))
                .build();

        String result = orderService.formatOrderForOwner(order);

        assertThat(result).contains("#ORD001");
        assertThat(result).contains("Nguyen Van A");
        assertThat(result).contains("@userA");
        assertThat(result).contains("95,000đ");
        assertThat(result).contains("Trà Sữa Trân Châu Đen");
        assertThat(result).contains("Trân Châu Đen");
    }

    @Test
    void formatOrderForOwner_shouldIncludeNoteWhenPresent() {
        Order order = Order.builder()
                .orderCode("#ORD002")
                .customerName("B")
                .username(null)
                .total(25000)
                .note("ít đường nhiều đá")
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        OrderItem.builder()
                                .itemName("Cà Phê Đen").size("M").quantity(1)
                                .toppings("").price(25000).build()
                ))
                .build();

        String result = orderService.formatOrderForOwner(order);

        assertThat(result).contains("ít đường nhiều đá");
        assertThat(result).doesNotContain("@"); // không có username
    }

    @Test
    void formatOrderForOwner_shouldNotShowNoteLineWhenNoteIsEmpty() {
        Order order = Order.builder()
                .orderCode("#ORD003")
                .customerName("C")
                .username(null)
                .total(35000)
                .note("")
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        OrderItem.builder()
                                .itemName("Trà Xoài").size("L").quantity(1)
                                .toppings(null).price(35000).build()
                ))
                .build();

        String result = orderService.formatOrderForOwner(order);

        assertThat(result).doesNotContain("Ghi chú:");
    }
}
