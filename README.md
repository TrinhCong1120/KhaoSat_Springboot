# BE_KhaoSat

Backend he thong khao sat duoc xay dung theo kien truc microservices voi Spring Boot, Spring Cloud Gateway, Spring Cloud Config, Eureka, PostgreSQL va Kafka.

## Kien truc tong quan

He thong gom cac thanh phan chinh:

| Thanh phan | Port | Vai tro |
| --- | --- | --- |
| config-service | 8888 | Config Server, doc cau hinh tu `config-repo` |
| discovery-service | 8761 | Eureka Server, dang ky va phat hien service |
| gateway-service | 8080 | API Gateway, route request vao cac service noi bo |
| auth-service | 8084 | Dang nhap, dang ky, JWT authentication |
| core-service | 8081 | Quan ly users, roles, menus, functions, permissions |
| survey-service | 8082 | Quan ly khao sat, cau hoi, cau tra loi, bao cao, dashboard |
| notification-service | 8083 | Xu ly notification qua Kafka va gui email |
| PostgreSQL | 5433:5432 | Database dung chung cho cac service |
| Kafka | 9092 | Message broker cho notification |

Luong chay co ban:

```text
Client / Frontend
      |
      v
gateway-service :8080
      |
      +--> auth-service :8084
      +--> core-service :8081
      +--> survey-service :8082
      +--> notification-service :8083

config-service :8888 cung cap cau hinh
discovery-service :8761 quan ly service discovery
PostgreSQL luu du lieu
Kafka truyen notification event
```

## Cong nghe su dung

- Java 17
- Spring Boot
- Spring Cloud Config
- Spring Cloud Netflix Eureka
- Spring Cloud Gateway WebFlux
- Spring Security va JWT
- Spring Data JPA
- PostgreSQL 16
- Apache Kafka 3.8.1
- Springdoc OpenAPI / Swagger UI
- Docker Compose

## Cau truc thu muc

```text
BE_KhaoSat/
+-- auth-service/
+-- config-service/
+-- core-service/
+-- discovery-service/
+-- gateway-service/
+-- notification-service/
+-- survey-service/
+-- config-repo/
+-- docker-compose.yml
+-- DOCKER.md
`-- API_FE.md
```

Trong do:

- `config-repo/`: chua file cau hinh YAML cho tung service.
- `docker-compose.yml`: chay toan bo he thong bang Docker.
- `API_FE.md`: tai lieu API phuc vu frontend.
- `DOCKER.md`: ghi chu rieng ve Docker va build image.

## Route qua Gateway

Tat ca API nen duoc goi qua gateway:

```text
http://localhost:8080
```

Route hien tai trong `config-repo/gateway-service.yml`:

| Path | Service dich |
| --- | --- |
| `/api/Auth/**` | auth-service |
| `/api/Users/**` | core-service |
| `/api/Roles/**` | core-service |
| `/api/Menus/**` | core-service |
| `/api/Functions/**` | core-service |
| `/api/Surveys/**` | survey-service |
| `/api/Pages/**` | survey-service |
| `/api/Responses/**` | survey-service |
| `/api/Dashboard/**` | survey-service |
| `/api/FailedSurveys/**` | survey-service |
| `/api/Questions/**` | survey-service |
| `/api/Reports/**` | survey-service |
| `/api/Conditions/**` | survey-service |
| `/api/public/surveys/**` | survey-service |
| `/api/notifications/**` | notification-service |

## Swagger UI

Swagger UI cua gateway:

```text
http://localhost:8080/swagger-ui.html
```

Gateway gom OpenAPI docs tu cac service:

| Service | OpenAPI qua gateway |
| --- | --- |
| auth-service | `/swagger/auth/v3/api-docs` |
| core-service | `/swagger/core/v3/api-docs` |
| survey-service | `/swagger/survey/v3/api-docs` |

## Yeu cau moi truong

Can cai dat:

- Docker va Docker Compose
- JDK 17 neu chay service truc tiep bang Maven
- Maven wrapper da co san trong tung service, vi du `auth-service/mvnw.cmd`

## Cau hinh moi truong

Tao file `.env` tu file mau:

```powershell
copy .env.example .env
```

Bien moi truong chinh:

```text
POSTGRES_DB=surveyDB
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
JWT_SECRET=change-me-to-at-least-32-characters
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

Luu y:

- `JWT_SECRET` nen co do dai toi thieu 32 ky tu.
- `MAIL_USERNAME` va `MAIL_PASSWORD` can dung thong tin email/app password neu muon gui mail that.
- Khong nen commit file `.env` chua thong tin that len Git.

## Chay bang Docker Compose

Chay toan bo he thong:

```powershell
docker compose up --build
```

Dung he thong:

```powershell
docker compose down
```

Dung va xoa volume database/Kafka:

```powershell
docker compose down -v
```

Sau khi chay thanh cong, co the truy cap:

| Dich vu | URL |
| --- | --- |
| Gateway | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Eureka Dashboard | `http://localhost:8761` |
| Config Server | `http://localhost:8888` |
| Gateway health | `http://localhost:8080/actuator/health` |
| Gateway routes | `http://localhost:8080/actuator/gateway/routes` |

## Thu tu khoi dong neu chay thu cong

Neu khong dung Docker Compose, nen khoi dong theo thu tu:

1. PostgreSQL
2. Kafka
3. config-service
4. discovery-service
5. auth-service
6. core-service
7. survey-service
8. notification-service
9. gateway-service

Vi du chay mot service bang Maven wrapper tren Windows:

```powershell
cd auth-service
.\mvnw.cmd spring-boot:run
```

## Config Server

`config-service` doc cau hinh tu `config-repo`.

Mot so file quan trong:

```text
config-repo/application.yml
config-repo/auth-service.yml
config-repo/core-service.yml
config-repo/survey-service.yml
config-repo/notification-service.yml
config-repo/gateway-service.yml
config-repo/discovery-service.yml
```

Khi chay bang Docker, `config-repo` duoc mount vao container `config-service` va cac service lay config tu:

```text
configserver:http://config-service:8888
```

## Database

PostgreSQL duoc khai bao trong `docker-compose.yml`.

Mac dinh:

```text
Database: surveyDB
User: postgres
Port host: 5433
Port container: 5432
```

Trong cac service, Hibernate dang de:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

## Kafka va notification

`core-service` co producer gui notification event vao Kafka.

`notification-service` consume event voi:

```text
group-id: notification-service
```

Kafka bootstrap server khi chay Docker:

```text
kafka:9092
```

Kafka bootstrap server khi chay local co the la:

```text
localhost:9092
```

## CORS

Gateway dang cho phep cac origin:

```text
http://localhost:3000
https://7e07-2001-df2-9b00-1995-8186-f2f7-af0b-ffe9.ngrok-free.app
```

Neu frontend doi domain hoac port, cap nhat:

```text
config-repo/gateway-service.yml
```

Tai muc:

```yaml
app:
  cors:
    allowed-origins:
```

## Kiem tra nhanh

Kiem tra gateway:

```powershell
curl http://localhost:8080/actuator/health
```

Kiem tra Eureka:

```text
http://localhost:8761
```

Kiem tra Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Kiem tra route API qua gateway:

```text
http://localhost:8080/api/Auth/...
http://localhost:8080/api/Users/...
http://localhost:8080/api/Surveys/...
http://localhost:8080/api/notifications/...
```

## Ghi chu phat trien

- Them API moi thi cap nhat controller trong service tuong ung va route trong `gateway-service.yml` neu path chua duoc gateway match.
- Them service moi thi can them config YAML trong `config-repo`, dang ky Eureka, them Dockerfile, them service vao `docker-compose.yml`, va them route gateway neu can expose API.
- Them Swagger cho service moi thi them route OpenAPI trong gateway va them entry vao `springdoc.swagger-ui.urls`.
- Khi doi bien moi truong, kiem tra lai `.env`, `docker-compose.yml` va cac file YAML trong `config-repo`.
