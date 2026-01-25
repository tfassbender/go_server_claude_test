package net.tfassbender.game.user;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.UUID;

/**
 * Integration tests for the UserResource REST endpoints.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserResourceTest {

    private String testUsername;
    private String otherUsername;
    private String testToken;
    private String otherToken;
    private boolean setupDone = false;

    @BeforeEach
    void setUp() {
        if (setupDone) {
            return;
        }

        // Create unique usernames
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        testUsername = "usertest_" + uniqueId;
        otherUsername = "other_" + uniqueId;

        // Register test user
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + testUsername + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        // Register other user
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + otherUsername + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        // Login test user
        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + testUsername + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().response();
        testToken = loginResponse.jsonPath().getString("token");

        // Login other user
        Response otherLoginResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + otherUsername + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().response();
        otherToken = otherLoginResponse.jsonPath().getString("token");

        setupDone = true;
    }

    // ===== Get Current User Tests =====

    @Test
    void testGetCurrentUser() {
        given()
            .header("Authorization", "Bearer " + testToken)
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(200)
            .body("username", equalTo(testUsername))
            .body("statistics", notNullValue())
            .body("createdAt", notNullValue());
    }

    @Test
    void testGetCurrentUserWithoutAuth() {
        given()
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(401);
    }

    @Test
    void testGetCurrentUserInvalidToken() {
        given()
            .header("Authorization", "Bearer invalid_token")
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(401);
    }

    // ===== Get User Profile Tests =====

    @Test
    void testGetUserProfile() {
        given()
            .header("Authorization", "Bearer " + testToken)
        .when()
            .get("/api/users/" + otherUsername)
        .then()
            .statusCode(200)
            .body("username", equalTo(otherUsername))
            .body("statistics", notNullValue());
    }

    @Test
    void testGetUserProfileNonExistent() {
        given()
            .header("Authorization", "Bearer " + testToken)
        .when()
            .get("/api/users/nonexistent_user_xyz")
        .then()
            .statusCode(404)
            .body("error", equalTo("User not found"));
    }

    @Test
    void testGetUserProfileWithoutAuth() {
        given()
        .when()
            .get("/api/users/" + otherUsername)
        .then()
            .statusCode(401);
    }

    // ===== Search Users Tests =====

    @Test
    void testSearchUsers() {
        given()
            .header("Authorization", "Bearer " + testToken)
        .when()
            .get("/api/users/search")
        .then()
            .statusCode(200)
            .body("users", notNullValue())
            .body("users", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    void testSearchUsersWithQuery() {
        given()
            .header("Authorization", "Bearer " + testToken)
        .when()
            .get("/api/users/search?q=" + otherUsername.substring(0, 5))
        .then()
            .statusCode(200)
            .body("users", notNullValue());
    }

    @Test
    void testSearchUsersExcludesCurrentUser() {
        Response response = given()
            .header("Authorization", "Bearer " + testToken)
        .when()
            .get("/api/users/search")
        .then()
            .statusCode(200)
            .extract().response();

        // The current user should not be in the results
        java.util.List<String> users = response.jsonPath().getList("users");
        org.junit.jupiter.api.Assertions.assertFalse(
            users.contains(testUsername),
            "Search results should not include the current user"
        );
    }

    @Test
    void testSearchUsersWithoutAuth() {
        given()
        .when()
            .get("/api/users/search")
        .then()
            .statusCode(401);
    }

    @Test
    void testSearchUsersNoResults() {
        given()
            .header("Authorization", "Bearer " + testToken)
        .when()
            .get("/api/users/search?q=xyznonexistentpattern123")
        .then()
            .statusCode(200)
            .body("users", hasSize(0));
    }
}
