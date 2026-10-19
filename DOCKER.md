# Docker

Du an chay bang Docker Compose gom Postgres, Kafka, Config Server, Eureka,
Gateway va cac service Spring Boot.

## Chay

```powershell
docker compose up --build
```

Gateway:

```text
http://localhost:8080
```

Neu dung Nginx truoc Gateway (vi du qua ngrok), giu nguyen tien to `/api`
khi proxy. Cau hinh `location /api/` voi `proxy_pass http://127.0.0.1:8080;`
(khong co dau `/` cuoi); neu dung `:8080/`, Nginx se bo `/api` va
Gateway tra 404.

Eureka:

```text
http://localhost:8761
```

Config Server:

```text
http://localhost:8888
```

## Dockerfile tung service

Moi service co Dockerfile rieng trong thu muc service:

```text
auth-service/Dockerfile
config-service/Dockerfile
core-service/Dockerfile
discovery-service/Dockerfile
gateway-service/Dockerfile
notification-service/Dockerfile
survey-service/Dockerfile
```

Compose build moi service voi context la thu muc service tuong ung.

Sau khi khoi tao schema va `survey-reporting.sql`, chay them
`survey-service/src/main/resources/db/survey-dashboard-reporting.sql` de tao
bang khach website theo ngay, cot dem cau tra loi va danh muc 6 loai cau hoi.
Chay script nay truoc khi khoi dong survey-service va truoc
`validation-rules-relational.sql`. DB dang chay can
`pg_dump -Fc` va doi chieu so dong truoc/sau migration; xem `SURVEY_REPORTING.md`.

## IPv6 cho Maven

Moi Dockerfile da dat:

```text
MAVEN_OPTS=-Djava.net.preferIPv6Addresses=true -Djava.net.preferIPv4Stack=false
```

Compose cung dat `build.network: host` cho cac service de Maven trong luc build
dung network stack cua may host. Neu Docker Desktop van bao `Network is
unreachable` khi Maven tai dependency, hay bat IPv6 cho Docker/WSL va thu lai:

```powershell
docker compose build --no-cache config-service
```

Sau khi build duoc mot service, chay ca he thong:

```powershell
docker compose up --build
```

Truoc khi khoi dong phien ban co thong bao Telegram tren database da ton tai,
chay migration:

```powershell
Get-Content -Raw 'survey-service/src/main/resources/db/add-telegram-chat-id.sql' | docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
```

Truoc khi khoi dong phien ban co trang quan ly thong bao, chay migration
tao lich su gui, quyen va menu (an toan khi chay lai):

```powershell
Get-Content -Raw -Encoding utf8 'notification-service/src/main/resources/db/notification-management.sql' | docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
Get-Content -Raw -Encoding utf8 'notification-service/src/main/resources/db/notification-bounces.sql' | docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
Get-Content -Raw -Encoding utf8 'notification-service/src/main/resources/db/telegram-chat-history.sql' | docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
Get-Content -Raw -Encoding utf8 'notification-service/src/main/resources/db/notification-inbox.sql' | docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
```

Dich vu doc thu hoan lai bang IMAPS tu hop thu `MAIL_USERNAME` voi mat khau
`MAIL_PASSWORD`. Hop thu can cho phep truy cap IMAP (voi Gmail, dung app
password cho tai khoan gui). Thu moi co ma luot gui trong header; thu cu chi
duoc doi soat khi dia chi nhan, tieu de, tai khoan gui va thoi gian khop duy
nhat. Cac bien `NOTIFICATION_BOUNCE_ENABLED`, `NOTIFICATION_BOUNCE_HOST`,
`NOTIFICATION_BOUNCE_PORT`, `NOTIFICATION_BOUNCE_FOLDER` va
`NOTIFICATION_BOUNCE_POLL_DELAY_MS` cho phep dieu chinh viec doc hop thu.
Email moi trong Inbox va tin nhan Telegram chat rieng duoc luu trong hop thu
phan hoi. Tep dinh kem duoc luu trong bucket MinIO rieng tu
`notification-attachments`; gioi han mac dinh 20 MB, doi qua
`NOTIFICATION_ATTACHMENT_MAX_BYTES`. Khi doi dia chi ngrok, cap nhat
`TELEGRAM_WEBHOOK_URL` trong `.env` va khoi dong lai notification-service.

Dang nhap lai sau khi cap quyen moi de JWT nhan `notification_view` va
`notification_send`. Mac dinh toi da 3 lan gui (tinh ca lan dau), cho 1 phut
roi 5 phut. Co the doi qua `NOTIFICATION_MAX_ATTEMPTS`,
`NOTIFICATION_RETRY_DELAY_1`, `NOTIFICATION_RETRY_DELAY_2` trong moi truong
(khoang cho theo cu phap Duration, vi du `PT2M`).

## Bien moi truong

Compose tu dong doc file `.env` o thu muc goc. Cac bien dang dung:

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
JWT_SECRET
MAIL_USERNAME
MAIL_PASSWORD
TELEGRAM_BOT_TOKEN
TELEGRAM_WEBHOOK_URL
TELEGRAM_WEBHOOK_SECRET
```

Truoc khi bat webhook, chay `notification-service/src/main/resources/db/telegram-chat-history.sql`
tren PostgreSQL. `TELEGRAM_WEBHOOK_URL` la URL HTTPS cong khai cua gateway
(duong dan `/api/telegram/webhook`); `TELEGRAM_WEBHOOK_SECRET` la chuoi ngau nhien
chi gom chu, so, `_` hoac `-`. Khi notification-service khoi dong, bot dang ky
webhook va menu lenh voi Telegram. Nguoi dung nhan `/start` de mo hai nut
kiem tra thong tin va dang ky lien ket; `/getid` tra Chat ID cho Admin gan vao
tai khoan trong man hinh quan ly nguoi dung. Lich su tuong tac hien trong
bang `telegram_chat_messages`; tin den hien trong Hop thu phan hoi va can
quyen `notification_view`. Sau khi sua route trong
`config-repo/gateway-service.yml`, chay `docker compose restart gateway-service`
de gateway nap lai route; neu khong, Telegram co the nhan 404 du webhook da
dang ky thanh cong.

Config trong `config-repo` da duoc doi sang dung host noi bo Docker qua bien moi
truong, vi vay cac service goi nhau bang `postgres`, `kafka`,
`config-service`, va `discovery-service` trong Compose.
