package com.r2s.auth.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthE2EFlowTest {

    // Tìm docker-compose.(yaml|yml) giống bên user-service
    private static File findComposeFile() {
        Path base   = Paths.get("").toAbsolutePath();        // thư mục module auth-service
        Path parent = base.getParent();                      // repo root
        Path grand  = parent != null ? parent.getParent() : null;

        String[] names = {"docker-compose.yaml", "docker-compose.yml"};
        Path[] bases   = {base, parent, grand};

        for (Path b : bases) {
            if (b == null) continue;
            for (String n : names) {
                Path p = b.resolve(n);
                if (java.nio.file.Files.exists(p)) return p.toFile();
            }
        }
        throw new IllegalStateException("Không tìm thấy docker-compose.(yaml|yml) từ " + base);
    }

    private static final File COMPOSE = findComposeFile();

    @Container
    static DockerComposeContainer<?> env = new DockerComposeContainer<>(COMPOSE)
            .withExposedService(
                    "auth-service", 8081,
                    Wait.forHttp("/actuator/health")
                            .forStatusCode(200)
                            .withStartupTimeout(java.time.Duration.ofMinutes(3))
            );

    static String authBase;
    static String bobToken;

    @BeforeAll
    static void beforeAll() {
        String host = env.getServiceHost("auth-service", 8081);
        Integer port = env.getServicePort("auth-service", 8081);

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        authBase = "http://" + host + ":" + port + "/api/v1/auth";
        System.out.println("authBase = " + authBase);
    }

    // 1) Đăng ký user mới
    @Test
    @Order(1)
    void register_user_should_200() {
        given().contentType("application/json")
                .body(Map.of(
                        "username", "tiendat",
                        "password", "Strong@123"
                ))
                .when().post(authBase + "/register")
                .then().statusCode(200);
    }

    // 2) Login user vừa đăng ký -> lấy token
    @Test
    @Order(2)
    void login_user_should_return_token() {
        bobToken = given().contentType("application/json")
                .body(Map.of(
                        "username", "tiendat",
                        "password", "Strong@123"
                ))
                .when().post(authBase + "/login")
                .then().statusCode(200)
                .body("token", not(emptyString()))
                .extract().path("token");
    }

    // 3) Login sai password -> 401 (hoặc 403 tuỳ cấu hình)
    @Test
    @Order(3)
    void login_with_wrong_password_should_401_or_403() {
        given().contentType("application/json")
                .body(Map.of(
                        "username", "bob",
                        "password", "wrong-password"
                ))
                .when().post(authBase + "/login")
                .then().statusCode(anyOf(is(401), is(403)));
    }

    // 4) Đăng ký trùng username -> 400/409 tuỳ bạn map
    @Test
    @Order(4)
    void register_duplicate_username_should_400_or_409() {
        given().contentType("application/json")
                .body(Map.of(
                        "username", "bob",
                        "password", "Another@123",
                        "role", "ROLE_USER"
                ))
                .when().post(authBase + "/register")
                .then().statusCode(anyOf(is(400), is(409)));
    }
}
