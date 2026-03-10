package com.milkteabot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CartItem {
    private String itemId;
    private String itemName;
    private String size;
    private Integer quantity;
    private List<String> toppings;
    private Integer price;

    public int getSubtotal() {
        return price * quantity;
    }

    public String display() {
        String toppingStr = toppings.isEmpty() ? "" : "\n   + " + String.join(", ", toppings);
        return String.format("• %s (%s) x%d — %,dđ%s",
                itemName, size, quantity, getSubtotal(), toppingStr);
    }
}