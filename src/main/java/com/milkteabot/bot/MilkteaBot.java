package com.milkteabot.bot;

import com.milkteabot.config.BotConfig;
import com.milkteabot.model.*;
import com.milkteabot.service.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MilkteaBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final MenuService menuService;
    private final SessionService sessionService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final KeyboardFactory keyboards;

    public MilkteaBot(BotConfig botConfig, MenuService menuService,
                      SessionService sessionService, OrderService orderService,
                      NotificationService notificationService, KeyboardFactory keyboards) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.menuService = menuService;
        this.sessionService = sessionService;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.keyboards = keyboards;
    }

    @PostConstruct
    public void init() {
        notificationService.setBot(this);
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    // ─────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update);
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text     = update.getMessage().getText().trim();
        long   chatId   = update.getMessage().getChatId();
        String name     = update.getMessage().getFrom().getFirstName();
        String username = update.getMessage().getFrom().getUserName();

        log.info("Message from {} ({}): {}", name, chatId, text);

        UserSession session = sessionService.getSession(chatId);
        switch (text) {
            case "/start"                   -> handleStart(chatId, name);
            case "Đặt hàng"               -> handleOrder(chatId);
            case "/menu",    "Menu"         -> handleMenu(chatId);
            case "/order"                   -> handleOrder(chatId);
            case "/cart",    "Giỏ hàng"   -> handleCart(chatId);
            case "/cancel"                  -> handleCancel(chatId);
            case "/history"                 -> handleHistory(chatId);
            case "/help",    "Hướng dẫn" -> handleHelp(chatId);
            default -> {
                if (session.getState() == UserSession.State.WAITING_NOTE) {
                    handleNote(chatId, name, username, text, session);
                } else {
                    handleUnknown(chatId);
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // HANDLE CALLBACK
    // ─────────────────────────────────────────────
    private void handleCallback(Update update) {
        String data      = update.getCallbackQuery().getData();
        Long   chatId    = update.getCallbackQuery().getMessage().getChatId();
        int    messageId = update.getCallbackQuery().getMessage().getMessageId();
        String name      = update.getCallbackQuery().getFrom().getFirstName();
        String username  = update.getCallbackQuery().getFrom().getUserName();

        log.info("Callback from {} ({}): {}", name, chatId, data);
        clearKeyboard(chatId, messageId);

        // Owner-only callbacks
        if (data.startsWith("OWNER_")) {
            if (!chatId.equals(botConfig.getOwnerChatId())) {
                send(chatId, "Bạn không có quyền thực hiện thao tác này.", null);
                return;
            }
        }

        UserSession session = sessionService.getSession(chatId);

        if (data.startsWith("CAT_")) {
            String category = data.substring(4);
            send(chatId, "*" + category + "* — Chọn món:", keyboards.itemKeyboard(category));

        } else if (data.equals("BACK_CATEGORY")) {
            handleOrder(chatId);

        } else if (data.startsWith("ITEM_")) {
            String itemId = data.substring(5);
            session.setPendingItemId(itemId);
            session.setState(UserSession.State.SELECTING_SIZE);
            menuService.findById(itemId).ifPresent(item ->
                    send(chatId, "Chọn size cho *" + item.getName() + "*:", keyboards.sizeKeyboard(item))
            );

        } else if (data.startsWith("SIZE_")) {
            session.setPendingSize(data.substring(5));
            session.setState(UserSession.State.SELECTING_QUANTITY);
            send(chatId, "Chọn số lượng:", keyboards.quantityKeyboard());

        } else if (data.startsWith("QTY_")) {
            session.setPendingQuantity(Integer.parseInt(data.substring(4)));
            session.clearPendingToppings();
            session.setState(UserSession.State.SELECTING_TOPPING);
            send(chatId, "Thêm topping không?\n_Có thể chọn nhiều, bấm Xong khi hoàn tất._",
                    keyboards.toppingKeyboard(session.getPendingToppings()));

        } else if (data.startsWith("TOPPING_")) {
            String topping = data.substring(8);
            List<String> selected = session.getPendingToppings();
            if (selected.contains(topping)) {
                selected.remove(topping);
            } else {
                selected.add(topping);
            }
            send(chatId, "Thêm topping không?\n_Có thể chọn nhiều, bấm Xong khi hoàn tất._",
                    keyboards.toppingKeyboard(selected));

        } else if (data.equals("DONE_TOPPING")) {
            addItemToCart(chatId, session);

        } else if (data.equals("ADD_MORE")) {
            session.setState(UserSession.State.BROWSING);
            handleOrder(chatId);

        } else if (data.equals("CONFIRM_ORDER")) {
            session.setState(UserSession.State.WAITING_NOTE);
            send(chatId, "Bạn có ghi chú gì không?\n"
                    + "_VD: ít đá, không đường, để lấy lúc 12h..._\n\n"
                    + "Hoặc nhắn *'không'* nếu không có ghi chú.", null);

        } else if (data.equals("CANCEL_ORDER")) {
            sessionService.resetSession(chatId);
            send(chatId, "Đã hủy đơn hàng.\n\nGõ /order để đặt lại nhé!", null);

        } else if (data.startsWith("CUSTOMER_CANCEL_")) {
            String orderCode = data.substring(16);
            orderService.findByOrderCode(orderCode).ifPresentOrElse(order -> {
                if (order.getStatus() == OrderStatus.PREPARING
                        || order.getStatus() == OrderStatus.DONE
                        || order.getStatus() == OrderStatus.CANCELLED) {
                    send(chatId, "Đơn *" + orderCode + "* đang ở trạng thái *"
                            + statusLabel(order.getStatus()) + "*, không thể hủy.", null);
                } else {
                    orderService.updateStatus(orderCode, OrderStatus.CANCELLED);
                    send(chatId, "Đã hủy đơn *" + orderCode + "*.\n\nGõ /order để đặt lại nhé!", null);
                    notificationService.notifyOwner_Cancel(orderCode);
                }
            }, () -> send(chatId, "Không tìm thấy đơn " + orderCode + ".", null));

        } else if (data.startsWith("OWNER_CONFIRM_")) {
            String orderCode = data.substring(14);
            orderService.findByOrderCode(orderCode).ifPresent(order -> {
                orderService.updateStatus(orderCode, OrderStatus.CONFIRMED);
                InlineKeyboardMarkup ownerKb = InlineKeyboardMarkup.builder()
                        .keyboard(List.of(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Đang chuẩn bị")
                                        .callbackData("OWNER_PREPARING_" + orderCode).build()
                        ))).build();
                send(chatId, "Đã xác nhận đơn *" + orderCode + "*.", ownerKb);

                InlineKeyboardMarkup customerKb = InlineKeyboardMarkup.builder()
                        .keyboard(List.of(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Hủy đơn")
                                        .callbackData("CUSTOMER_CANCEL_" + orderCode).build()
                        ))).build();
                if (order.getCustomerMessageId() != null) {
                    clearKeyboard(order.getChatId(), order.getCustomerMessageId());
                }
                Integer newMsgId = sendAndGetId(order.getChatId(),
                        "Đơn hàng *" + orderCode + "* đã được xác nhận!\n"
                        + "Tình trạng: _Đã xác nhận_\n\n"
                        + "Vui lòng chờ khoảng 10-15 phút nhé!\n"
                        + "_Bấm nút bên dưới nếu muốn hủy đơn._", customerKb);
                if (newMsgId != null) orderService.saveCustomerMessageId(order.getId(), newMsgId);
            });

        } else if (data.startsWith("OWNER_PREPARING_")) {
            String orderCode = data.substring(16);
            orderService.findByOrderCode(orderCode).ifPresent(order -> {
                orderService.updateStatus(orderCode, OrderStatus.PREPARING);
                if (order.getCustomerMessageId() != null) {
                    clearKeyboard(order.getChatId(), order.getCustomerMessageId());
                }
                InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                        .keyboard(List.of(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Hoàn thành")
                                        .callbackData("OWNER_DONE_" + orderCode).build()
                        ))).build();
                send(chatId, "Đơn *" + orderCode + "* đang được chuẩn bị.", kb);
                notificationService.notifyCustomer(order.getChatId(),
                        "Đơn hàng *" + orderCode + "* đang được chuẩn bị!\n"
                        + "Tình trạng: _Đang chuẩn bị_");
            });

        } else if (data.startsWith("OWNER_DONE_")) {
            String orderCode = data.substring(11);
            orderService.findByOrderCode(orderCode).ifPresent(order -> {
                orderService.updateStatus(orderCode, OrderStatus.DONE);
                send(chatId, "Đơn *" + orderCode + "* đã hoàn thành!", null);
                notificationService.notifyCustomer(order.getChatId(),
                        "Đơn hàng *" + orderCode + "* đã sẵn sàng!\n"
                        + "Tình trạng: _Hoàn thành_\n\n"
                        + "Cảm ơn bạn đã ủng hộ quán!");
            });

        } else if (data.startsWith("OWNER_REJECT_")) {
            String orderCode = data.substring(13);
            orderService.findByOrderCode(orderCode).ifPresent(order -> {
                orderService.updateStatus(orderCode, OrderStatus.CANCELLED);
                if (order.getCustomerMessageId() != null) {
                    clearKeyboard(order.getChatId(), order.getCustomerMessageId());
                }
                send(chatId, "Đã từ chối đơn *" + orderCode + "*.", null);
                notificationService.notifyCustomer(order.getChatId(),
                        "Rất tiếc, đơn hàng *" + orderCode + "* đã bị từ chối.\n\n"
                        + "Vui lòng liên hệ quán để biết thêm chi tiết.");
            });
        }
    }

    private String statusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING    -> "Đang chờ xác nhận";
            case CONFIRMED  -> "Đã xác nhận";
            case PREPARING  -> "Đang chuẩn bị";
            case DONE       -> "Hoàn thành";
            case CANCELLED  -> "Đã hủy";
        };
    }

    // ─────────────────────────────────────────────
    // COMMAND HANDLERS
    // ─────────────────────────────────────────────
    private void handleStart(long chatId, String name) {
        send(chatId, "Chào *" + name + "*!\n\n"
                + "Mình là bot đặt trà sữa tự động.\n\n"
                + "/order — Đặt hàng\n"
                + "/menu  — Xem menu & giá\n"
                + "/cart  — Xem giỏ hàng\n"
                + "/help  — Hướng dẫn",
                keyboards.mainKeyboard());
    }

    private void handleOrder(long chatId) {
        sessionService.setState(chatId, UserSession.State.BROWSING);
        send(chatId, "*Chọn danh mục món uống:*", keyboards.categoryKeyboard());
    }

    private void handleMenu(long chatId) {
        Map<String, List<MenuItem>> grouped = menuService.getGroupedByCategory();
        StringBuilder sb = new StringBuilder("*MENU TRÀ SỮA*\n====================\n\n");
        grouped.forEach((category, items) -> {
            if (category.equalsIgnoreCase("Topping")) return;
            sb.append("*").append(category).append("*\n");
            items.forEach(item -> sb
                    .append("  - ").append(item.getName())
                    .append("\n    M: `").append(String.format("%,d", item.getPriceM()))
                    .append("đ`  L: `").append(String.format("%,d", item.getPriceL())).append("đ`\n"));
            sb.append("\n");
        });
        sb.append("====================\nGõ /order để đặt hàng");
        send(chatId, sb.toString(), null);
    }

    private void handleCart(long chatId) {
        UserSession session = sessionService.getSession(chatId);
        if (session.isCartEmpty()) {
            send(chatId, "Giỏ hàng trống.\n\nGõ /order để đặt hàng nhé!", null);
            return;
        }
        send(chatId, "*GIỎ HÀNG CỦA BẠN*\n\n" + formatCart(session)
                + "\n\nGõ /cancel để hủy hoặc tiếp tục /order", null);
    }

    private void handleCancel(long chatId) {
        sessionService.resetSession(chatId);
        send(chatId, "Đã hủy đơn hàng.\n\nGõ /order để đặt lại nhé!", null);
    }

    private void handleHistory(long chatId) {
        List<Order> orders = orderService.getHistory(chatId);
        if (orders.isEmpty()) {
            send(chatId, "Bạn chưa có đơn hàng nào.", null);
            return;
        }
        StringBuilder sb = new StringBuilder("*5 ĐƠN HÀNG GẦN NHẤT*\n\n");
        orders.forEach(o -> sb.append("- ").append(o.getOrderCode())
                .append(" — ").append(String.format("%,d", o.getTotal())).append("đ")
                .append(" — ").append(statusLabel(o.getStatus())).append("\n"));
        send(chatId, sb.toString(), null);
    }

    private void handleHelp(long chatId) {
        send(chatId, "*HƯỚNG DẪN SỬ DỤNG*\n====================\n\n"
                + "/order — Bắt đầu đặt hàng\n"
                + "/menu  — Xem menu & giá\n"
                + "/cart  — Xem giỏ hàng\n"
                + "/cancel — Hủy đơn\n"
                + "/history — Lịch sử đơn hàng\n\n"
                + "_Chọn món theo từng bước, bot sẽ hướng dẫn bạn!_", null);
    }

    private void handleUnknown(long chatId) {
        send(chatId, "Mình chưa hiểu ý bạn.\n\nGõ /order để đặt hàng\nGõ /menu để xem menu", null);
    }

    // ─────────────────────────────────────────────
    // ORDER FLOW
    // ─────────────────────────────────────────────
    private void addItemToCart(long chatId, UserSession session) {
        menuService.findById(session.getPendingItemId()).ifPresent(item -> {
            String size = session.getPendingSize();
            int qty = session.getPendingQuantity();
            List<String> toppings = session.getPendingToppings();
            int basePrice = size.equals("M") ? item.getPriceM() : item.getPriceL();
            int toppingPrice = toppings.stream()
                    .mapToInt(t -> menuService.findByName(t).map(MenuItem::getPriceM).orElse(0))
                    .sum();

            session.addToCart(new CartItem(
                    item.getItemId(), item.getName(), size, qty,
                    List.copyOf(toppings),
                    basePrice + toppingPrice
            ));
            session.setState(UserSession.State.ADDING_ITEM);
            send(chatId, "Đã thêm *" + item.getName() + "* (" + size + ")\n\n"
                    + formatCart(session) + "\n\nBạn muốn làm gì tiếp?",
                    keyboards.postCartKeyboard());
        });
    }

    private void handleNote(long chatId, String name, String username,
                            String text, UserSession session) {
        if (text.length() > 200) {
            send(chatId, "Ghi chú quá dài (tối đa 200 ký tự).\nVui lòng nhập lại.", null);
            return;
        }

        String note = (text.equalsIgnoreCase("không")
                || text.equalsIgnoreCase("ko")
                || text.equalsIgnoreCase("k")) ? "" : text;

        Order order = orderService.createOrder(chatId, name, username, session.getCart(), note);

        InlineKeyboardMarkup cancelButton = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        InlineKeyboardButton.builder()
                                .text("Hủy đơn")
                                .callbackData("CUSTOMER_CANCEL_" + order.getOrderCode())
                                .build()
                ))).build();

        Integer msgId = sendAndGetId(chatId, "Đặt hàng thành công!\n\n"
                + "Mã đơn: *" + order.getOrderCode() + "*\n"
                + formatCart(session) + "\n"
                + "*Tổng: " + String.format("%,d", order.getTotal()) + "đ*\n\n"
                + "Tình trạng: _Đang chờ xác nhận..._\n"
                + "_Bấm nút bên dưới nếu muốn hủy đơn._", cancelButton);
        if (msgId != null) orderService.saveCustomerMessageId(order.getId(), msgId);
        notificationService.notifyOwner(order);
        sessionService.resetSession(chatId);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private String formatCart(UserSession session) {
        StringBuilder sb = new StringBuilder();
        session.getCart().forEach(item -> sb.append(item.display()).append("\n"));
        return sb.append("====================\n")
                 .append("Tổng: *").append(String.format("%,d", session.getCartTotal())).append("đ*")
                 .toString();
    }

    private void clearKeyboard(long chatId, int messageId) {
        try {
            execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .replyMarkup(InlineKeyboardMarkup.builder().keyboard(List.of()).build())
                    .build());
        } catch (TelegramApiException e) {
            log.debug("Could not clear keyboard: {}", e.getMessage());
        }
    }

    private Integer sendAndGetId(long chatId, String text, ReplyKeyboard markup) {
        try {
            return execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("Markdown")
                    .replyMarkup(markup)
                    .build()).getMessageId();
        } catch (TelegramApiException e) {
            log.error("Failed to send message: {}", e.getMessage());
            return null;
        }
    }

    public void send(long chatId, String text, ReplyKeyboard markup) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("Markdown")
                    .replyMarkup(markup)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send message: {}", e.getMessage());
        }
    }
}
