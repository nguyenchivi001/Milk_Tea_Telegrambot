package com.milkteabot.bot;

import com.milkteabot.model.MenuItem;
import com.milkteabot.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KeyboardFactory {

    private final MenuService menuService;

    public InlineKeyboardMarkup categoryKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        menuService.getGroupedByCategory().keySet().stream()
                .filter(cat -> !cat.equalsIgnoreCase("Topping"))
                .forEach(cat -> rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text(cat)
                        .callbackData("CAT_" + cat)
                        .build()
        )));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup itemKeyboard(String category) {
        List<MenuItem> items = menuService.getGroupedByCategory().get(category);
        if (items == null) return InlineKeyboardMarkup.builder().keyboard(List.of()).build();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        items.forEach(item -> rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text(item.getName() + " | M:" + String.format("%,d", item.getPriceM())
                                + "đ L:" + String.format("%,d", item.getPriceL()) + "đ")
                        .callbackData("ITEM_" + item.getItemId())
                        .build()
        )));
        rows.add(List.of(InlineKeyboardButton.builder()
                .text("« Quay lại danh mục").callbackData("BACK_CATEGORY").build()));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup sizeKeyboard(MenuItem item) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        InlineKeyboardButton.builder()
                                .text("M — " + String.format("%,d", item.getPriceM()) + "đ")
                                .callbackData("SIZE_M").build(),
                        InlineKeyboardButton.builder()
                                .text("L — " + String.format("%,d", item.getPriceL()) + "đ")
                                .callbackData("SIZE_L").build()
                ))).build();
    }

    public InlineKeyboardMarkup quantityKeyboard() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            row.add(InlineKeyboardButton.builder()
                    .text(String.valueOf(i))
                    .callbackData("QTY_" + i)
                    .build());
        }
        return InlineKeyboardMarkup.builder().keyboard(List.of(row)).build();
    }

    public InlineKeyboardMarkup toppingKeyboard(List<String> selected) {
        List<MenuItem> toppings = menuService.getGroupedByCategory().get("Topping");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (toppings != null) {
            toppings.forEach(t -> {
                boolean isSelected = selected.contains(t.getName());
                String label = (isSelected ? "[x] " : "") + t.getName()
                        + " +" + String.format("%,d", t.getPriceM()) + "đ";
                rows.add(List.of(InlineKeyboardButton.builder()
                        .text(label)
                        .callbackData("TOPPING_" + t.getName()).build()));
            });
        }
        rows.add(List.of(InlineKeyboardButton.builder()
                .text(selected.isEmpty() ? "Không thêm topping" : "Xong (" + selected.size() + " topping)")
                .callbackData("DONE_TOPPING").build()));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup postCartKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder().text("Thêm món").callbackData("ADD_MORE").build(),
                                InlineKeyboardButton.builder().text("Xác nhận đơn").callbackData("CONFIRM_ORDER").build()
                        ),
                        List.of(
                                InlineKeyboardButton.builder().text("Hủy đơn").callbackData("CANCEL_ORDER").build()
                        )
                )).build();
    }

    public ReplyKeyboardMarkup mainKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Đặt hàng"));
        row1.add(new KeyboardButton("Menu"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Giỏ hàng"));
        row2.add(new KeyboardButton("Hướng dẫn"));
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .resizeKeyboard(true).build();
    }
}
