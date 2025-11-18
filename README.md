# User Access Management

Mono-repo quản lý **người dùng** và **phân quyền** gồm 3 module chính:

- `core` – entity, repository, security (JWT), exception dùng chung
- `auth-service` – đăng ký / đăng nhập / phát hành JWT
- `user-service` – quản lý hồ sơ người dùng (profile), API cho USER & ADMIN

Repo đã **dockerize đầy đủ** (Postgres + 2 service) và có test **unit / webmvc / integration với Testcontainers**.

---

## 🧭 Mục lục

- [1. Yêu cầu](#1-yêu-cầu)
- [2. Công nghệ sử dụng](#2-công-nghệ-sử-dụng)
- [3. Cấu trúc thư mục](#3-cấu-trúc-thư-mục)
- [4. Biến môi trường](#4-biến-môi-trường)
- [5. Build & chạy bằng Docker](#5-build--chạy-bằng-docker)
- [6. Smoke test nhanh](#6-smoke-test-nhanh)
- [7. Kết nối vào Postgres trong container](#7-kết-nối-vào-postgres-trong-container)
- [8. Test & chất lượng code](#8-test--chất-lượng-code)
- [9. Ghi chú phát triển](#9-ghi-chú-phát-triển)

---

## 1. Yêu cầu

- **JDK 17+**
- **Maven 3.9+**
- **Docker Desktop** (Compose v2)
- Tuỳ chọn:
  - **pgAdmin 4** hoặc **DBeaver** để xem DB
  - **curl / Postman / Insomnia** để call API

---

## 2. Công nghệ sử dụng

- **Spring Boot 3.5.x**
- **Spring Security 6** + JWT (stateless)
- **Spring Data JPA** (Hibernate)
- **PostgreSQL**
- **Testcontainers** (integration test với PostgreSQL thật)
- **JUnit 5**, **Mockito**, **Spring MVC Test (MockMvc)**
- **Docker / Docker Compose**
- **springdoc-openapi** (Swagger UI)

---

## 3. Cấu trúc thư mục

```text
user-access-manament/
├─ core/
│  ├─ src/main/java/com/r2s/core/
│  │  ├─ entity/           # User, Role, Profile base...
│  │  ├─ repository/
│  │  ├─ security/         # JwtService, JwtFilter, CustomUserDetailsService
│  │  └─ exception/        # CustomException, GlobalExceptionHandler, ...
│  └─ src/test/java/com/r2s/core/security/
│     ├─ JwtServiceTest.java
│     ├─ JwtFilterTest.java
│     └─ CustomUserDetailsServiceTest.java
│
├─ auth-service/
│  ├─ src/main/java/com/r2s/auth/
│  │  ├─ config/           # SecurityConfig
│  │  ├─ controller/       # AuthController, RoleController
│  │  ├─ dto/
│  │  ├─ exception/        # ApiExceptionHandler, SecurityExceptionHandler
│  │  └─ service/          # AuthServiceImpl
│  ├─ src/test/java/com/r2s/auth/
│  │  ├─ controller/       # AuthControllerWebMvcTest, AuthControllerJwtFilterIT
│  │  ├─ service/          # AuthServiceImplTest
│  │  └─ e2e/              # AuthE2EFlowTest (RestAssured + Testcontainers)
│  ├─ src/main/resources/
│  │  ├─ application.properties
│  │  ├─ application-docker.properties
│  │  └─ application-test.properties
│  ├─ Dockerfile
│  └─ .dockerignore
│
├─ user-service/
│  ├─ src/main/java/com/r2s/user/
│  │  ├─ controller/       # ProfileController
│  │  ├─ dto/              # ProfileDto, ProfileResponse
│  │  ├─ entity/           # Profile
│  │  ├─ exception/        # ApiExceptionHandler (dịch NotFoundException...)
│  │  └─ service/          # ProfileService, ProfileServiceImpl
│  ├─ src/test/java/com/r2s/user/
│  │  ├─ controller/       # ProfileControllerWebMvcTest
│  │  └─ service/          # ProfileServiceImplIT (Testcontainers)
│  ├─ src/main/resources/
│  │  ├─ application.properties
│  │  └─ application-docker.properties
│  ├─ Dockerfile
│  └─ .dockerignore
│
├─ postgres/
│  └─ init.sql             # tạo DB auth_service, user_service, user mẫu...
│
├─ docker-compose.yaml
├─ .env                    # cấu hình Postgres (host)
└─ README.md
4. Biến môi trường
4.1. File .env (cho Docker Compose)
File .env nằm ở root, ví dụ:

env
Sao chép mã
POSTGRES_USER=postgres
POSTGRES_PASSWORD=d433221dat
POSTGRES_DB=postgres

# Port mapping cho Postgres container (host:container)
POSTGRES_PORT_HOST=55432
POSTGRES_PORT_CONTAINER=5432
Mặc định map 55432 → 5432 để tránh đụng Postgres local.

4.2. Biến ứng dụng (JWT, profile…)
Các service dùng chung một số biến:

env
Sao chép mã
# JWT
JWT_SECRET=w5h2Yk1Gd3dtdkFzT0h0bTFwM05XUnJvV0V1NHVZbU5yZFRyWW9oUUNmQ1hLcw==
JWT_EXP_MINUTES=120

# Spring profile khi chạy Docker
SPRING_PROFILES_ACTIVE=docker
Khi chạy bằng Docker Compose, các biến này thường được set trong
application-docker.properties + docker-compose.yaml.
Khi chạy local, có thể override qua VM options / ENV.

5. Build & chạy bằng Docker
5.1. Build Maven (tất cả module)
bash
Sao chép mã
mvn clean package -DskipTests
5.2. Dựng toàn bộ stack
bash
Sao chép mã
docker compose up --build
Sau khi lên thành công:

auth-service chạy ở: http://localhost:8081

user-service chạy ở: http://localhost:8082

Postgres lắng nghe ở: 127.0.0.1:55432

5.3. Dừng container
Giữ lại data:

bash
Sao chép mã
docker compose down
Xoá luôn data volume:

bash
Sao chép mã
docker compose down -v
6. Smoke test nhanh
Mặc định base-path API là /api/v1.

6.1. Đăng ký & đăng nhập (auth-service)
bash
Sao chép mã
# Register USER
curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"123456"}'

# Register ADMIN
curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"123456","role":"ROLE_ADMIN"}'

# Login (lấy JWT)
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"123456"}' | jq -r .token)

echo "$TOKEN"
6.2. Gọi API có bảo vệ JWT
Ví dụ: ADMIN xem danh sách profile trong user-service:

bash
Sao chép mã
curl -s http://localhost:8082/api/v1/users \
  -H "Authorization: Bearer $TOKEN"
USER tự xem profile của mình:

bash
Sao chép mã
curl -s http://localhost:8082/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
7. Kết nối vào Postgres trong container
Container DB được đặt tên (ví dụ) là postgres-db trong docker-compose.yaml.

7.1. Kết nối bằng pgAdmin (khuyến nghị)
Mở pgAdmin → Register → Server…

Tab General

Name: docker-postgres (55432) (tuỳ bạn đặt)

Tab Connection

Host name/address: 127.0.0.1

Port: 55432

Maintenance DB: postgres

Username: postgres

Password: giống trong .env (d433221dat)

Save → Connect

Mở DB auth_service hoặc user_service → Query Tool:

sql
Sao chép mã
SELECT * FROM users;
7.2. Kết nối bằng DBeaver
Lưu ý TimeZone:

Không dùng Asia/Saigon (Postgres không chấp nhận)

Nếu cần, dùng Asia/Ho_Chi_Minh hoặc bỏ hẳn tham số TimeZone

Thông số:

Host: 127.0.0.1

Port: 55432

Database: auth_service hoặc user_service

User/Pass: trong .env

7.3. Kết nối bằng psql (dòng lệnh)
Liệt kê DB:

bash
Sao chép mã
docker exec -it postgres-db psql -U postgres -c "\l"
Query nhanh:

bash
Sao chép mã
docker exec -it postgres-db psql -U postgres -d auth_service \
  -c "SELECT id, username, role FROM users ORDER BY id DESC LIMIT 5;"
8. Test & chất lượng code
8.1. Chạy toàn bộ test
bash
Sao chép mã
mvn test
8.2. Các nhóm test chính
core module

JwtServiceTest – sinh / verify JWT, claim role

JwtFilterTest – behavior filter khi:

thiếu token

token invalid / hết hạn

token hợp lệ → set Authentication vào SecurityContext

CustomUserDetailsServiceTest – load user từ DB

auth-service

AuthServiceImplTest – unit test cho đăng ký / đăng nhập

AuthControllerWebMvcTest – slice test cho /api/v1/auth/** (validation, response)

AuthControllerJwtFilterIT – integration test:

thiếu Authorization → 401 Unauthorized

token hợp lệ nhưng sai role → 403 Forbidden

token hợp lệ + đúng role → 200 OK

user-service

ProfileControllerWebMvcTest

GET /api/v1/users – chỉ ADMIN; kiểm tra format JSON

GET /api/v1/users/me – dùng @WithMockUser

PUT /api/v1/users/me – validate email, ép username từ token

DELETE /api/v1/users/{username} – mapping NotFoundException → 404

ProfileServiceImplIT – integration test với Testcontainers Postgres

Tất cả integration test profile test đều sử dụng Testcontainers → không phụ thuộc vào DB local.

9. Ghi chú phát triển
Security

Stateless JWT, SessionCreationPolicy.STATELESS

Chỉ mở:

/api/v1/auth/register, /api/v1/auth/login

/actuator/health, /actuator/info

swagger: /swagger-ui/**, /v3/api-docs/**

Còn lại yêu cầu JWT hợp lệ.

Thiếu token / token invalid → 401 Unauthorized
Có token nhưng thiếu quyền (sai role) → 403 Forbidden.

Swagger UI

Auth-service: http://localhost:8081/swagger-ui.html

User-service: http://localhost:8082/swagger-ui.html

Dockerfile (các service)

Base image: eclipse-temurin:17-jre

Copy jar → /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]