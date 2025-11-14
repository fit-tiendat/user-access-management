package com.r2s.user.e2e;

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
public class E2EFlowTest {

    // trỏ tới file compose của repo (đường dẫn tùy repo bạn)
    // Lấy thư mục repo root ổn định cho multi-module
    private static final Path ROOT =
            Paths.get(System.getProperty("maven.multiModuleProjectDirectory", ".")).toAbsolutePath();

    private static java.io.File findComposeFile() {
        java.nio.file.Path base = java.nio.file.Paths.get("").toAbsolutePath(); // thư mục module
        java.nio.file.Path parent = base.getParent();                           // repo root (thường là cái này)
        java.nio.file.Path grand = parent != null ? parent.getParent() : null;

        String[] names = {"docker-compose.yaml", "docker-compose.yml"};
        java.nio.file.Path[] bases = {base, parent, grand};

        for (java.nio.file.Path b : bases) {
            if (b == null) continue;
            for (String n : names) {
                java.nio.file.Path p = b.resolve(n);
                if (java.nio.file.Files.exists(p)) return p.toFile();
            }
        }
        throw new IllegalStateException("Không tìm thấy docker-compose.(yaml|yml) từ " + base);
    }

    private static final java.io.File COMPOSE = findComposeFile();


    @Container
    public static DockerComposeContainer<?> env = new DockerComposeContainer<>(COMPOSE)

            .withExposedService("user-service", 8082,
                    Wait.forHttp("/actuator/health").forStatusCode(200)
                            .withStartupTimeout(java.time.Duration.ofMinutes(3)))
            .withExposedService("auth-service", 8081,
                    Wait.forHttp("/actuator/health").forStatusCode(200)
                            .withStartupTimeout(java.time.Duration.ofMinutes(3)));


    static String authBase;
    static String userBase;

    @BeforeAll
    static void beforeAll() {
        String ah = env.getServiceHost("auth-service", 8081);
        Integer ap = env.getServicePort("auth-service", 8081);
        String uh = env.getServiceHost("user-service", 8082);
        Integer up = env.getServicePort("user-service", 8082);

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // ✅ Thêm base path đúng với app
        authBase = "http://" + ah + ":" + ap + "/api/v1/auth";
        userBase = "http://" + uh + ":" + up + "/api/v1/users";
    }



    static String aliceToken;
    static String adminToken;

    @Test
    @Order(1)
    void register_and_login_user() {
        // register alice (ROLE_USER mặc định)
        given().contentType("application/json")
                .body(Map.of("username", "alice", "password", "secret123"))
                .when().post(authBase + "/register")
                .then().statusCode(200);

        // login -> lấy token
        aliceToken =
                given().contentType("application/json")
                        .body(Map.of("username", "alice", "password", "secret123"))
                        .when().post(authBase + "/login")
                        .then().statusCode(200)
                        .body("token", not(emptyString()))
                        .extract().path("token");
    }

    @Test
    @Order(2)
    void user_can_upsert_own_profile_but_cannot_list_all() {
        // upsert /users/me
        given().contentType("application/json")
                .header("Authorization", "Bearer " + aliceToken)
                .body(Map.of(
                        "username", "ignored",
                        "fullName", "Alice A",
                        "email", "alice@mail.com"))
                .when().put(userBase + "/me")        // => /api/v1/users/me ✅
                .then().statusCode(200)
                .body("username", equalTo("alice"))
                .body("fullName", equalTo("Alice A"))
                .body("email", equalTo("alice@mail.com"));

        // user gọi GET /users -> 403
        given().header("Authorization", "Bearer " + aliceToken)
                .when().get(userBase)               // => GET /api/v1/users ✅
                .then().statusCode(403);
    }


    @Test
    @Order(3)
    void register_and_login_admin_then_list_and_delete() {
        // register admin (nếu API cho set role)
        given().contentType("application/json")
                .body(Map.of("username", "admin", "password", "admin123", "role", "ROLE_ADMIN"))
                .when().post(authBase + "/register")
                .then().statusCode(200);

        adminToken =
                given().contentType("application/json")
                        .body(Map.of("username", "admin", "password", "admin123"))
                        .when().post(authBase + "/login")
                        .then().statusCode(200)
                        .body("token", not(emptyString()))
                        .extract().path("token");

        // admin GET /users -> 200
        given().header("Authorization", "Bearer " + adminToken)
                .when().get(userBase)               // => GET /api/v1/users ✅
                .then().statusCode(200)
                .body("$", notNullValue());

        // admin DELETE /users/alice -> 204
        given().header("Authorization", "Bearer " + adminToken)
                .when().delete(userBase + "/alice") // => DELETE /api/v1/users/alice ✅
                .then().statusCode(204);
    }


    @Test
    @Order(4)
    void token_signature_or_expiry_invalid_should_401() {
        String badToken = "bad.jwt.token";
        given().header("Authorization", "Bearer " + badToken)
                .when().get(userBase + "/me")       // => GET /api/v1/users/me ✅
                .then().statusCode(anyOf(is(401), is(403)));
    }
}
