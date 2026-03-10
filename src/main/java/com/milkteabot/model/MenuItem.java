package com.milkteabot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MenuItem {
    private String category;
    private String itemId;
    private String name;
    private String description;
    private int priceM;
    private int priceL;
    private boolean available;
}
