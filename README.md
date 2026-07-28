# User Access Management

## 📚 Giới thiệu

Mono-repo JWT Authentication System được xây dựng với **Spring Boot 3.5.6** và **Java 17**, áp dụng đầy đủ **SOLID principles** và **Design Patterns** (Strategy, Builder, Factory).

### Modules
- `core` – Shared entities, repositories, security components & utilities
- `auth-service` – Authentication service (Register/Login/JWT) - Port 8081
- `user-service` – User management & profile service - Port 8082

### Tính năng nổi bật
- ✅ **Multi-module Maven architecture** với dependency management
- ✅ **JWT-based stateless authentication** với BCrypt password encoding
- ✅ **Full Docker support** (Postgres + 2 microservices)
- ✅ **GitLab CI/CD pipeline** (Build → Test → Deploy)
- ✅ **Comprehensive testing** (Unit tests + Integration tests + E2E tests với Testcontainers)
- ✅ **SOLID principles** implementation throughout codebase
- ✅ **API Documentation** với Springdoc OpenAPI (Swagger UI)
- ✅ **Database per service** pattern (auth_service & user_service DBs)

---

## 🧭 Mục lục
- [Yêu cầu](#yêu-cầu)
- [Cấu trúc](#cấu-trúc)
- [Biến môi trường](#biến-môi-trường)
- [Build & chạy](#build--chạy)
- [Kiểm tra nhanh (smoke tests)](#kiểm-tra-nhanh-smoke-tests)
- [Kết nối DB trong container](#kết-nối-db-trong-container)
    - [pgAdmin (khuyến nghị)](#pgadmin-khuyến-nghị)
    - [DBeaver (lưu ý TimeZone)](#dbeaver-lưu-ý-timezone)
    - [psql (dòng lệnh)](#psql-dòng-lệnh)
- [Volumes & dữ liệu bền vững](#volumes--dữ-liệu-bền-vững)
- [Troubleshooting](#troubleshooting)
- [Ghi chú phát triển](#ghi-chú-phát-triển)

---

## Yêu cầu
- **JDK 17+**
- **Maven 3.9+**
- **Docker Desktop** (Compose v2)
- (Tùy chọn) **pgAdmin 4** hoặc **DBeaver**

---

## 📁 Cấu trúc Project

```
user-access-management/
├── 📦 core/                           # Shared module
│   ├── src/main/java/com/r2s/core/
│   │   ├── config/                   # OpenAPI, Security constants
│   │   ├── dto/                      # ApiResponse, AuthResponse
│   │   ├── entity/                   # User, Role (enum)
│   │   ├── exception/                # Custom exceptions & GlobalExceptionHandler
│   │   ├── repository/               # UserRepository
│   │   ├── security/                 # JwtService, JwtFilter, CustomUserDetailsService
│   │   └── utils/                    # CommonConstants, LoggerUtil, ResponseBuilder
│   ├── src/test/java/                # Unit tests cho security components
│   └── pom.xml
│
├── 🔐 auth-service/                   # Authentication Service (Port 8081)
│   ├── src/main/java/com/r2s/auth/
│   │   ├── config/                   # SecurityConfig, JpaConfig
│   │   ├── controller/               # AuthController, RoleController
│   │   ├── dto/                      # LoginRequest, RegisterRequest
│   │   ├── exception/                # ApiExceptionHandler
│   │   ├── security/                 # JwtClaimsBuilder (Builder pattern)
│   │   ├── service/                  # AuthenticationService, RegistrationService
│   │   └── strategy/                 # AuthenticationStrategy (Strategy pattern)
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── application-docker.properties
│   ├── src/test/java/                # WebMvcTest, Integration tests, E2E tests
│   │   ├── controller/               # AuthControllerWebMvcTest
│   │   ├── service/                  # Service layer tests
│   │   └── e2e/                      # AuthE2EFlowTest (Testcontainers)
│   ├── Dockerfile
│   └── pom.xml
│
├── 👤 user-service/                   # User Management Service (Port 8082)
│   ├── src/main/java/com/r2s/user/
│   │   ├── config/                   # SecurityConfig
│   │   ├── controller/               # UserController
│   │   ├── dto/                      # DTOs for user operations
│   │   ├── entity/                   # UserProfile (nếu có entity riêng)
│   │   ├── repository/               # Repositories
│   │   └── service/                  # UserService (SOLID: Single Responsibility)
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── application-docker.properties
│   ├── src/test/java/                # Tests
│   ├── Dockerfile
│   └── pom.xml
│
├── 🐘 postgres/
│   └── init.sql                      # Creates auth_service & user_service databases
│
├── 📚 docs/
│   └── project_review.md             # Comprehensive code review & recommendations
│
├── 🔧 Configuration Files
│   ├── docker-compose.yaml           # Multi-container orchestration
│   ├── .gitlab-ci.yml                # CI/CD pipeline (build, test, deploy)
│   ├── .dockerignore
│   ├── .gitignore
│   ├── .env                          # Environment variables (not in git)
│   └── pom.xml                       # Parent POM with dependency management
│
└── 📖 README.md                       # This file
```

### 🎯 Design Patterns Implemented (Task 10: SOLID)

| Pattern | Location | Purpose |
|---------|----------|---------|
| **Strategy** | `auth-service/strategy/` | Multiple authentication methods (Password, OAuth planned) |
| **Builder** | `auth-service/security/JwtClaimsBuilder` | Clean JWT claims construction |
| **Factory** | Service layer injection | AuthenticationService selects strategy |
| **Repository** | `core/repository/` | Data access abstraction |
| **DTO** | All `dto/` packages | Data transfer without exposing entities |

### 🔐 SOLID Principles Application

- **S** (Single Responsibility): Each service has one clear purpose
- **O** (Open/Closed): Strategy pattern allows extending auth methods without modifying existing code
- **L** (Liskov Substitution): All strategies implement `AuthenticationStrategy` interface
- **I** (Interface Segregation): Separate interfaces for different services
- **D** (Dependency Inversion): Depends on abstractions (interfaces), not concrete implementations


## Biến môi trường
Tạo file `.env` (đã có mẫu trong repo). Điều chỉnh nếu cần:
```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=REPLACE_WITH_A_SECRET
POSTGRES_DB=postgres

# Port mapping cho Postgres container (host:container)
POSTGRES_PORT_HOST=55432
POSTGRES_PORT_CONTAINER=5432
Mặc định map 55432 → 5432 để tránh đụng CSDL Postgres đang chạy local.

Build & chạy
1) Build jar (bỏ qua test nếu muốn)
bash
Sao chép mã
mvn -q -DskipTests clean package
2) Dựng toàn bộ stack
bash
Sao chép mã
docker compose up --build
Sau khi thành công:

auth-service: http://localhost:8081

user-service: http://localhost:8082

postgres: lắng nghe trên 127.0.0.1:55432

3) Dừng & xóa container (giữ data)
bash
Sao chép mã
docker compose down
Muốn xóa cả data volume: docker compose down -v

Kiểm tra nhanh (smoke tests)
1) Đăng ký & đăng nhập
bash
Sao chép mã
# Register
curl -s -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test user1","password":"123456"}'

curl -s -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test user2","password":"123456","role":"ROLE_ADMIN"}'

# Login (nhận JWT)
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test user1","password":"123456"}' | jq -r .token)
echo "$TOKEN"
2) Gọi API cần Bearer token
bash
Sao chép mã
curl -s http://localhost:8082/users \
  -H "Authorization: Bearer $TOKEN"
Kết nối DB trong container
pgAdmin (khuyến nghị)
Mở pgAdmin → Register → Server…

Tab General

Name: docker-postgres (55432) (tuỳ)

Tab Connection

Host name/address: 127.0.0.1

Port: 55432

Maintenance DB: postgres

Username: postgres

Password: lấy từ `.env`; không lưu mật khẩu thật trong tài liệu hoặc Git

Save → Connect

Mở database auth_service hoặc user_service → Query Tool:

sql
Sao chép mã
select * from users;
Bạn sẽ thấy các user vừa đăng ký (test user1, test user2) nếu tạo qua API.

DBeaver (lưu ý TimeZone)
KHÔNG đặt TimeZone=Asia/Saigon (PostgreSQL không nhận).

Nếu cần TZ, dùng Asia/Ho_Chi_Minh hoặc bỏ hẳn tham số TimeZone.

Thông số kết nối:

Host: 127.0.0.1

Port: 55432

Database: auth_service (hoặc user_service)

User/Pass: theo .env

psql (dòng lệnh)
bash
Sao chép mã
# Liệt kê DB
docker exec -it postgres-db psql -U postgres -c "\l"

# Query nhanh
docker exec -it postgres-db psql -U postgres -d auth_service \
  -c "select id, username from users order by id desc limit 5;"
Volumes & dữ liệu bền vững
Compose tạo volume tên: user-access-manament_postgres_data

Xem vị trí (host):

bash
Sao chép mã
docker volume inspect user-access-manament_postgres_data
Xóa sạch dữ liệu DB:

bash
Sao chép mã
docker compose down -v
Troubleshooting
1) no main manifest attribute, in /app/app.jar
Đảm bảo dùng Spring Boot Maven Plugin (đóng gói kiểu Boot jar).

MANIFEST kiểm tra phải có:

pgsql
Sao chép mã
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: com.r2s.auth.AuthServiceApplication
Lệnh kiểm tra:

bash
Sao chép mã
jar tf auth-service/target/auth-service-1.0.0-SNAPSHOT.jar | Select-String MANIFEST.MF
jar xf auth-service/target/auth-service-1.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF
type META-INF/MANIFEST.MF
2) Lỗi Docker build: lstat /target: no such file or directory
Bạn chạy docker compose up --build khi chưa build Maven.
→ Chạy lại: mvn -q -DskipTests clean package rồi mới compose.

3) DBeaver báo: FATAL: invalid value for parameter "TimeZone": "Asia/Saigon"
Dùng Asia/Ho_Chi_Minh hoặc bỏ tham số TimeZone trong Driver Properties.

4) Không kết nối được Postgres từ pgAdmin/DBeaver
Đảm bảo container postgres-db đang Up:

bash
Sao chép mã
docker ps
Đúng port: 127.0.0.1:55432

Warinings Connection refused → coi lại firewall/antivirus chặn port.

5) DB hiển thị dữ liệu “cũ”
Bạn có thể đang kết nối nhầm local 5432 thay vì container 55432.

Kiểm tra lại server config trong tool, hoặc \conninfo (psql).

Ghi chú phát triển
Hồ sơ Docker:

Base image: eclipse-temurin:17-jre

Copy final jar → /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]

Hồ sơ Compose:

postgres (15), port map 55432:5432

auth-service (8081), user-service (8082)

Volume user-access-manament_postgres_data giữ data lâu dài

License
Internal project. All rights reserved.

css
Sao chép mã

> Nếu bạn muốn mình sinh thêm file `docs/db-admin/PGADMIN.md`/`postgres/README.md` tách riêng theo đúng format nội bộ, nói mình viết tiếp nhé.
