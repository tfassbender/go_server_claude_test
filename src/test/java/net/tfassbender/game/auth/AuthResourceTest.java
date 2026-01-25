package net.tfassbender.game.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import java.util.UUID;

/**
 * Integration tests for the AuthResource REST endpoints.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthResourceTest {

    private static String testUsername;

    @BeforeEach
    void setUp() {
        // Generate unique username for each test run to avoid conflicts
        if (testUsername == null) {
            testUsername = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    // ===== Registration Tests =====

    @Test
    @Order(1)
    void testRegisterSuccess() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + testUsername + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201)
            .body("message", equalTo("User created successfully"));
    }

    @Test
    @Order(2)
    void testRegisterDuplicateUsername() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + testUsername + "\", \"password\": \"password456\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(409)
            .body("message", equalTo("Username already exists"));
    }

    @Test
    void testRegisterMissingUsername() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"password\": \"password123\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(400)
            .body("message", equalTo("Username and password are required"));
    }

    @Test
    void testRegisterMissingPassword() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"newuser\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(400)
            .body("message", equalTo("Username and password are required"));
    }

    @Test
    void testRegisterUsernameTooShort() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"ab\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(409)
            .body("message", equalTo("Username must be between 3 and 20 characters"));
    }

    @Test
    void testRegisterPasswordTooShort() {
        String uniqueUser = "shortpw_" + UUID.randomUUID().toString().substring(0, 8);
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + uniqueUser + "\", \"password\": \"short\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(409)
            .body("message", equalTo("Password must be at least 8 characters"));
    }

    // ===== Login Tests =====

    @Test
    @Order(3)
    void testLoginSuccess() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + testUsername + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("username", equalTo(testUsername));
    }

    @Test
    void testLoginInvalidPassword() {
        // First register a user
        String user = "logintest_" + UUID.randomUUID().toString().substring(0, 8);
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + user + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        // Try to login with wrong password
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + user + "\", \"password\": \"wrongpassword\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401)
            .body("error", equalTo("Invalid username or password"));
    }

    @Test
    void testLoginNonExistentUser() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"nonexistent_user_xyz\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401)
            .body("error", equalTo("Invalid username or password"));
    }

    @Test
    void testLoginMissingCredentials() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(400)
            .body("error", equalTo("Username and password are required"));
    }
}
