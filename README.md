# User Access Management (Multi-Module)

## 📁 Modules
- **core** – Library dùng chung giữa các service
- **auth-service** – Cổng **8081**, endpoint test: [`/api/auth/hello`](http://localhost:8081/api/auth/hello)
- **user-service** – Cổng **8082**, endpoint test: [`/api/users/hello`](http://localhost:8082/api/users/hello)

---

## ⚙️ Build
```bash
./mvnw clean install
▶️ Run Services
Chạy từng service bằng Maven Wrapper:
# Auth Service
./mvnw -pl auth-service spring-boot:run

# User Service
./mvnw -pl user-service spring-boot:run

Hoặc chạy trực tiếp trong IntelliJ:

Run class AuthServiceApplication và UserServiceApplication.
✅ Verify

Sau khi chạy cả hai service:

Mở trình duyệt hoặc Postman:

Auth Service: http://localhost:8081/api/auth/hello

Hello from Auth Service - v1.0.0


User Service: http://localhost:8082/api/users/hello

Hello from User Service - v1.0.0

🧩 Project Structure
user-access-management/
├── pom.xml                # Parent POM
├── core/
├── auth-service/
└── user-service/
👨‍💻 Author

Nguyễn Tiến Đạt
R2S - User Access Management Project


-