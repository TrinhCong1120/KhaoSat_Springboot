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

## Bien moi truong

Compose tu dong doc file `.env` o thu muc goc. Cac bien dang dung:

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
JWT_SECRET
MAIL_USERNAME
MAIL_PASSWORD
```

Config trong `config-repo` da duoc doi sang dung host noi bo Docker qua bien moi
truong, vi vay cac service goi nhau bang `postgres`, `kafka`,
`config-service`, va `discovery-service` trong Compose.
