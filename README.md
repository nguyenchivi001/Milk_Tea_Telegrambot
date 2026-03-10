# MilkteaBot

Telegram bot đặt trà sữa tự động, thay thế quy trình nhận đơn thủ công qua Zalo/điện thoại.

## Tính năng

**Phía khách hàng**
- Xem menu đầy đủ với giá theo size M/L
- Đặt hàng theo từng bước: chọn món → size → số lượng → topping → ghi chú
- Xem và hủy đơn hàng (chỉ được hủy khi đơn hàng chưa được chuẩn bị (PREPARING))
- Xem lịch sử 5 đơn gần nhất

**Phía chủ quán**
- Nhận thông báo ngay khi có đơn mới
- Xác nhận hoặc từ chối đơn bằng 1 nút bấm
- Cập nhật trạng thái: Đang chuẩn bị → Hoàn thành
- Nhận thông báo khi khách hủy đơn

## Công nghệ

| Thành phần | Công nghệ |
|------------|-----------|
| Backend | Spring Boot 3.x + Java 21 |
| Telegram SDK | telegrambots-spring-boot-starter 6.9.7.1 |
| Database | H2 (file mode) |
| ORM | Spring Data JPA + Hibernate |
| Build | Maven |
| Deploy | Docker |

## Cấu trúc project

```
src/main/java/com/milkteabot/
├── MilkteabotApplication.java     # Entry point
├── bot/
│   ├── MilkteaBot.java            # Xử lý toàn bộ message và callback từ Telegram
│   └── KeyboardFactory.java       # Tạo inline/reply keyboard cho từng bước đặt hàng
├── model/
│   ├── MenuItem.java              # Món trong menu (đọc từ CSV)
│   ├── CartItem.java              # Món trong giỏ hàng (in-memory)
│   ├── Order.java                 # Đơn hàng (lưu DB)
│   ├── OrderItem.java             # Chi tiết từng món trong đơn (lưu DB)
│   ├── OrderStatus.java           # Enum trạng thái đơn
│   └── UserSession.java           # Trạng thái hội thoại từng user (in-memory)
├── service/
│   ├── MenuService.java           # Đọc menu từ CSV, tra cứu món
│   ├── SessionService.java        # Quản lý session theo chatId
│   ├── OrderService.java          # Tạo/cập nhật đơn, thống kê
│   └── NotificationService.java   # Gửi thông báo cho chủ quán và khách
├── repository/
│   └── OrderRepository.java       # JPA repository cho Order
└── config/
    └── BotConfig.java             # Đọc config token/username từ properties
```

## Luồng đặt hàng

```
Khách
  /start → chọn "Đặt hàng"
  → Chọn danh mục → Chọn món → Chọn size (M/L)
  → Chọn số lượng (1-5) → Chọn topping (có thể nhiều)
  → Nhập ghi chú → Xác nhận đơn
  → Nhận thông báo "Đang chờ xác nhận" + nút Hủy đơn

Chủ quán
  Nhận thông báo đơn mới + [Xác nhận] [Từ chối]
  → Xác nhận → [Đang chuẩn bị]    (khách vẫn hủy được)
  → Đang chuẩn bị → [Hoàn thành]  (khách không hủy được nữa)
  → Hoàn thành → thông báo khách lấy đồ
```

### Trạng thái đơn hàng

```
PENDING → CONFIRMED → PREPARING → DONE
              │
              └→ CANCELLED  (chủ từ chối hoặc khách hủy)
```

## Menu

| Danh mục | Số món | Giá từ |
|----------|--------|--------|
| Trà Sữa | 5 món | 30,000đ |
| Trà Trái Cây | 5 món | 30,000đ |
| Cà Phê | 5 món | 25,000đ |
| Đá Xay | 4 món | 35,000đ |
| Topping | 8 loại | 3,000đ – 8,000đ |

Để thêm/sửa/xóa món, chỉnh sửa file `src/main/resources/Menu.csv`.

## Cài đặt và chạy local

### Yêu cầu
- Java 21+
- Maven 3.9+
- VPN (nếu chạy tại Việt Nam — Telegram bị chặn)

### Các bước

**1. Clone project**
```bash
git clone https://github.com/<your-username>/milkteabot.git
cd milkteabot
```

**2. Cấu hình bot**

Sửa `src/main/resources/application.properties`:
```properties
telegram.bot.token=YOUR_BOT_TOKEN
telegram.bot.username=YOUR_BOT_USERNAME
telegram.owner.chatid=YOUR_CHAT_ID
```

> Lấy token từ [@BotFather](https://t.me/BotFather).
> Lấy chat ID của bạn từ [@userinfobot](https://t.me/userinfobot).

**3. Build và chạy**
```bash
mvn clean package -DskipTests
java -jar target/milkteabot-0.0.1-SNAPSHOT.jar
```

## Deploy với Docker (Railway / VPS)

**1. Build image**
```bash
docker build -t milkteabot .
```

**2. Chạy container**
```bash
docker run -d \
  -e TELEGRAM_TOKEN=your_token \
  -e TELEGRAM_USERNAME=your_username \
  -e OWNER_CHAT_ID=your_chat_id \
  -v milkteabot-data:/app/data \
  milkteabot
```

### Deploy lên Railway

1. Push code lên GitHub
2. Vào [railway.app](https://railway.app) → **New Project → Deploy from GitHub repo**
3. Chọn repo → Railway tự detect Dockerfile và build
4. Thêm biến môi trường trong tab **Variables**:

| Key | Giá trị |
|-----|---------|
| `TELEGRAM_TOKEN` | Token bot |
| `TELEGRAM_USERNAME` | Username bot |
| `OWNER_CHAT_ID` | Chat ID chủ quán |

5. Vào tab **Volumes** → Add Volume → Mount path: `/app/data`

## Các lệnh bot

| Lệnh | Chức năng |
|------|-----------|
| `/start` | Khởi động bot, hiện menu chính |
| `/order` | Bắt đầu đặt hàng |
| `/menu` | Xem toàn bộ menu và giá |
| `/cart` | Xem giỏ hàng hiện tại |
| `/cancel` | Hủy phiên đặt hàng đang thực hiện |
| `/history` | Xem 5 đơn hàng gần nhất |
| `/help` | Hướng dẫn sử dụng |
