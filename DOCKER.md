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
```

Nguoi dung nhan `/start` cho bot trong private chat de lay Chat ID. Admin gan
Chat ID do vao tai khoan trong man hinh quan ly nguoi dung.

Config trong `config-repo` da duoc doi sang dung host noi bo Docker qua bien moi
truong, vi vay cac service goi nhau bang `postgres`, `kafka`,
`config-service`, va `discovery-service` trong Compose.
