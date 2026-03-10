package com.milkteabot.service;

import com.milkteabot.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Value("${telegram.owner.chatid}")
    private Long ownerChatId;

    private final OrderService orderService;
    private TelegramLongPollingBot bot;

    public NotificationService(OrderService orderService) {
        this.orderService = orderService;
    }

    public void setBot(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public void notifyOwner(Order order) {
        if (bot == null) {
            log.warn("Bot not set, cannot send notification");
            return;
        }

        String msg = orderService.formatOrderForOwner(order);

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        InlineKeyboardButton.builder()
                                .text("Xác nhận")
                                .callbackData("OWNER_CONFIRM_" + order.getOrderCode())
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Từ chối")
                                .callbackData("OWNER_REJECT_" + order.getOrderCode())
                                .build()
                ))).build();

        try {
            bot.execute(SendMessage.builder()
                    .chatId(ownerChatId)
                    .text(msg)
                    .parseMode("Markdown")
                    .replyMarkup(keyboard)
                    .build());
            log.info("Order notification sent to owner: {}", order.getOrderCode());
        } catch (TelegramApiException e) {
            log.error("Failed to notify owner: {}", e.getMessage());
        }
    }

    public void notifyOwner_Cancel(String orderCode) {
        if (bot == null) return;
        try {
            bot.execute(SendMessage.builder()
                    .chatId(ownerChatId)
                    .text("Khach hang da huy don *" + orderCode + "*.")
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to notify owner cancel: {}", e.getMessage());
        }
    }

    public void sendCancelButton(String orderCode) {
        if (bot == null) return;
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        InlineKeyboardButton.builder()
                                .text("Huy don " + orderCode)
                                .callbackData("OWNER_CANCEL_" + orderCode)
                                .build()
                ))).build();
        try {
            bot.execute(SendMessage.builder()
                    .chatId(ownerChatId)
                    .text("Don *" + orderCode + "* da xac nhan. Nhan nut bên duoi neu muon huy.")
                    .parseMode("Markdown")
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send cancel button: {}", e.getMessage());
        }
    }

    public void notifyCustomer(long chatId, String message) {
        if (bot == null) return;
        try {
            bot.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to notify customer: {}", e.getMessage());
        }
    }
}
