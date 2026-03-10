package com.milkteabot.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserSession {

    public enum State {
        IDLE,
        BROWSING,
        ADDING_ITEM,
        SELECTING_SIZE,
        SELECTING_QUANTITY,
        SELECTING_TOPPING,
        CONFIRMING,
        WAITING_NOTE
    }

    private State state = State.IDLE;
    private List<CartItem> cart = new ArrayList<>();
    private String pendingItemId;
    private String pendingSize;
    private int pendingQuantity = 1;
    private List<String> pendingToppings = new ArrayList<>();

    public void reset() {
        state = State.IDLE;
        cart = new ArrayList<>();
        pendingItemId = null;
        pendingSize = null;
        pendingQuantity = 1;
        pendingToppings = new ArrayList<>();
    }

    public void clearPendingToppings() {
        pendingToppings = new ArrayList<>();
    }

    public int getCartTotal() {
        return cart.stream().mapToInt(CartItem::getSubtotal).sum();
    }

    public boolean isCartEmpty() {
        return cart.isEmpty();
    }

    public void addToCart(CartItem item) {
        cart.stream()
                .filter(c -> c.getItemId().equals(item.getItemId())
                        && c.getSize().equals(item.getSize())
                        && c.getToppings().equals(item.getToppings()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + item.getQuantity()),
                        () -> cart.add(item)
                );
    }
}
