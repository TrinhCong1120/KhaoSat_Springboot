# Docker

Dự án chạy bằng Docker Compose gồm Postgres, Kafka, MinIO, Config Server, Eureka, Gateway và các service Spring Boot.

## Khởi động

```powershell
docker compose up --build
```

- **Nginx (Cổng chính cho cả Web & API)**: `http://localhost` (port 80)
- **Gateway (Trực tiếp)**: `http://localhost:8080`
- **Eureka**: `http://localhost:8761`
- **Config Server**: `http://localhost:8888`
- **MinIO Console**: `http://localhost:19000`

> Nginx tự động điều hướng: các request bắt đầu bằng `/api`, `/v3/api-docs`, `/swagger`, `/webjars`, `/survey-media` sẽ đi vào **Gateway**; các request còn lại sẽ đi vào **Next.js Frontend** (hỗ trợ đầy đủ WebSocket HMR).

## Khởi tạo và cập nhật Database

Đối với database mới hoặc database đang chạy cần cập nhật toàn bộ bảng/cột mới (Telegram, Zalo, nhóm khảo sát, báo cáo, hộp thư phản hồi), chỉ cần chạy duy nhất 1 lệnh:

```powershell
Get-Content -Raw -Encoding utf8 'init-db-migrations.sql' | docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
```

Script trên có tính chất idempotent (an toàn khi chạy lại nhiều lần).

## Cấu hình Webhook & Thông báo

1. **Email / IMAP**: Đọc thư hoàn lại bằng IMAPS từ hòm thư `MAIL_USERNAME` / `MAIL_PASSWORD`.
2. **Telegram Bot**:
   - Điền `TELEGRAM_BOT_TOKEN`, `TELEGRAM_WEBHOOK_URL` (`https://<domain>/api/telegram/webhook`) và `TELEGRAM_WEBHOOK_SECRET` vào file `.env`.
   - Bot tự động đăng ký webhook khi notification-service khởi động. Người dùng gửi `/getid` để lấy Chat ID gán vào tài khoản.
3. **Zalo Bot**:
   - Điền `ZALO_BOT_TOKEN`, `ZALO_WEBHOOK_URL` (`https://<domain>/api/zalo/webhook`) và `ZALO_WEBHOOK_SECRET` vào file `.env`.
   - Lấy token từ [Zalo Bot Platform](https://bot.zaloplatforms.com/docs).
4. **Cập nhật Route Gateway**:
   - Sau khi thay đổi route trong `config-repo/gateway-service.yml`, chạy:
     ```powershell
     docker compose restart gateway-service
     ```

## Biến môi trường

Khai báo trong file `.env` ở thư mục gốc (xem mẫu chi tiết tại `.env.example`).
