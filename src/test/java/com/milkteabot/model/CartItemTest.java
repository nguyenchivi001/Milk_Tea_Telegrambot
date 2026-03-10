package com.milkteabot.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartItemTest {

    @Test
    void getSubtotal_shouldReturnPriceTimesQuantity() {
        CartItem item = new CartItem("TS01", "Trà Sữa", "M", 3, List.of(), 35000);
        assertThat(item.getSubtotal()).isEqualTo(105000);
    }

    @Test
    void getSubtotal_shouldHandleQuantityOfOne() {
        CartItem item = new CartItem("CF01", "Cà Phê Đen", "L", 1, List.of(), 30000);
        assertThat(item.getSubtotal()).isEqualTo(30000);
    }

    @Test
    void display_shouldIncludeToppingsWhenPresent() {
        CartItem item = new CartItem("TS01", "Trà Sữa", "M", 2,
                List.of("Trân Châu Đen", "Kem Tươi"), 43000);
        String display = item.display();
        assertThat(display).contains("Trân Châu Đen, Kem Tươi");
        assertThat(display).contains("x2");
        assertThat(display).contains("86,000đ");
    }

    @Test
    void display_shouldNotShowToppingsLineWhenEmpty() {
        CartItem item = new CartItem("CF01", "Cà Phê", "M", 1, List.of(), 25000);
        String display = item.display();
        assertThat(display).doesNotContain("+");
        assertThat(display).contains("25,000đ");
    }
}
