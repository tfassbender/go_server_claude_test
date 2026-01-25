package net.tfassbender.game.game;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.UUID;

/**
 * Integration tests for the GameResource REST endpoints.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GameResourceTest {

    private String player1Username;
    private String player2Username;
    private String player1Token;
    private String player2Token;
    private String lifecycleGameId; // Game used for sequential lifecycle tests
    private boolean setupDone = false;
    private boolean lifecycleGameCreated = false;
    private boolean lifecycleGameAccepted = false;

    @BeforeEach
    void setUp() {
        if (setupDone) {
            return;
        }

        // Create unique usernames
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        player1Username = "player1_" + uniqueId;
        player2Username = "player2_" + uniqueId;

        // Register player 1
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + player1Username + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        // Register player 2
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + player2Username + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        // Login player 1
        Response loginResponse1 = given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + player1Username + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().response();
        player1Token = loginResponse1.jsonPath().getString("token");

        // Login player 2
        Response loginResponse2 = given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"" + player2Username + "\", \"password\": \"password123\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract().response();
        player2Token = loginResponse2.jsonPath().getString("token");

        setupDone = true;
    }

    /**
     * Helper method to create a new game and return its ID.
     */
    private String createGame(String creatorToken, String opponentUsername, String color) {
        Response response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + creatorToken)
            .body("{\"boardSize\": 9, \"opponentUsername\": \"" + opponentUsername + "\", \"requestedColor\": \"" + color + "\"}")
        .when()
            .post("/api/games")
        .then()
            .statusCode(201)
            .extract().response();
        return response.jsonPath().getString("gameId");
    }

    /**
     * Ensures the lifecycle game is created (for ordered tests).
     */
    private void ensureLifecycleGameCreated() {
        if (!lifecycleGameCreated) {
            lifecycleGameId = createGame(player1Token, player2Username, "black");
            lifecycleGameCreated = true;
        }
    }

    /**
     * Ensures the lifecycle game is accepted (for move tests).
     */
    private void ensureLifecycleGameAccepted() {
        ensureLifecycleGameCreated();
        if (!lifecycleGameAccepted) {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + player2Token)
            .when()
                .post("/api/games/" + lifecycleGameId + "/accept")
            .then()
                .statusCode(200);
            lifecycleGameAccepted = true;
        }
    }

    // ===== Game Creation Tests =====

    @Test
    void testCreateGame() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
            .body("{\"boardSize\": 9, \"opponentUsername\": \"" + player2Username + "\", \"requestedColor\": \"black\"}")
        .when()
            .post("/api/games")
        .then()
            .statusCode(201)
            .body("gameId", notNullValue())
            .body("status", equalTo("pending"))
            .body("blackPlayer", equalTo(player1Username))
            .body("whitePlayer", equalTo(player2Username));
    }

    @Test
    void testCreateGameWithWhiteColor() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
            .body("{\"boardSize\": 13, \"opponentUsername\": \"" + player2Username + "\", \"requestedColor\": \"white\"}")
        .when()
            .post("/api/games")
        .then()
            .statusCode(201)
            .body("blackPlayer", equalTo(player2Username))
            .body("whitePlayer", equalTo(player1Username));
    }

    @Test
    void testCreateGameInvalidBoardSize() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
            .body("{\"boardSize\": 10, \"opponentUsername\": \"" + player2Username + "\", \"requestedColor\": \"black\"}")
        .when()
            .post("/api/games")
        .then()
            .statusCode(400)
            .body("error", equalTo("Board size must be 9, 13, or 19"));
    }

    @Test
    void testCreateGameNonExistentOpponent() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
            .body("{\"boardSize\": 9, \"opponentUsername\": \"nonexistent_user_xyz\", \"requestedColor\": \"black\"}")
        .when()
            .post("/api/games")
        .then()
            .statusCode(400)
            .body("error", equalTo("Opponent user not found"));
    }

    @Test
    void testCreateGameAgainstSelf() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
            .body("{\"boardSize\": 9, \"opponentUsername\": \"" + player1Username + "\", \"requestedColor\": \"black\"}")
        .when()
            .post("/api/games")
        .then()
            .statusCode(400)
            .body("error", equalTo("Cannot create game with yourself"));
    }

    @Test
    void testCreateGameWithoutAuth() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"boardSize\": 9, \"opponentUsername\": \"" + player2Username + "\", \"requestedColor\": \"black\"}")
        .when()
            .post("/api/games")
        .then()
            .statusCode(401);
    }

    // ===== Game Listing Tests =====

    @Test
    void testGetPendingGames() {
        // Create a game to ensure there's at least one pending
        createGame(player1Token, player2Username, "black");

        given()
            .header("Authorization", "Bearer " + player1Token)
        .when()
            .get("/api/games?status=pending")
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    void testGetGameDetails() {
        String gameId = createGame(player1Token, player2Username, "black");

        given()
            .header("Authorization", "Bearer " + player1Token)
        .when()
            .get("/api/games/" + gameId)
        .then()
            .statusCode(200)
            .body("id", equalTo(gameId))
            .body("boardSize", equalTo(9))
            .body("status", equalTo("pending"));
    }

    @Test
    void testGetNonExistentGame() {
        given()
            .header("Authorization", "Bearer " + player1Token)
        .when()
            .get("/api/games/nonexistent-game-id")
        .then()
            .statusCode(404)
            .body("error", equalTo("Game not found"));
    }

    // ===== Accept/Decline Game Tests =====

    @Test
    void testCreatorCannotAcceptOwnGame() {
        String gameId = createGame(player1Token, player2Username, "black");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
        .when()
            .post("/api/games/" + gameId + "/accept")
        .then()
            .statusCode(400)
            .body("error", equalTo("Cannot accept your own game invitation"));
    }

    @Test
    void testAcceptGame() {
        String gameId = createGame(player1Token, player2Username, "black");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player2Token)
        .when()
            .post("/api/games/" + gameId + "/accept")
        .then()
            .statusCode(200)
            .body("message", equalTo("Game accepted"))
            .body("status", equalTo("active"));
    }

    @Test
    void testAcceptAlreadyActiveGame() {
        String gameId = createGame(player1Token, player2Username, "black");

        // Accept the game first
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player2Token)
        .when()
            .post("/api/games/" + gameId + "/accept")
        .then()
            .statusCode(200);

        // Try to accept again
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player2Token)
        .when()
            .post("/api/games/" + gameId + "/accept")
        .then()
            .statusCode(400)
            .body("error", equalTo("Game is not pending"));
    }

    @Test
    void testDeclineGame() {
        String gameId = createGame(player1Token, player2Username, "black");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player2Token)
        .when()
            .post("/api/games/" + gameId + "/decline")
        .then()
            .statusCode(200)
            .body("message", equalTo("Game declined"));
    }

    // ===== Move Tests (Sequential - use ordered lifecycle game) =====

    @Test
    @Order(1)
    void testMakeMove() {
        ensureLifecycleGameAccepted();

        // Black (player1) plays first
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
            .body("{\"position\": {\"x\": 4, \"y\": 4}}")
        .when()
            .post("/api/games/" + lifecycleGameId + "/move")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("currentTurn", equalTo("white"));
    }

    @Test
    @Order(2)
    void testMakeMoveWrongTurn() {
        ensureLifecycleGameAccepted();

        // It's white's turn, but black tries to play
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
            .body("{\"position\": {\"x\": 5, \"y\": 5}}")
        .when()
            .post("/api/games/" + lifecycleGameId + "/move")
        .then()
            .statusCode(400)
            .body("error", equalTo("It's not your turn"));
    }

    @Test
    @Order(3)
    void testMakeMoveWhite() {
        ensureLifecycleGameAccepted();

        // White (player2) plays
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player2Token)
            .body("{\"position\": {\"x\": 3, \"y\": 3}}")
        .when()
            .post("/api/games/" + lifecycleGameId + "/move")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("currentTurn", equalTo("black"));
    }

    @Test
    @Order(4)
    void testMakeMoveOnOccupiedPosition() {
        ensureLifecycleGameAccepted();

        // Try to play on occupied position
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
            .body("{\"position\": {\"x\": 4, \"y\": 4}}")  // Already occupied
        .when()
            .post("/api/games/" + lifecycleGameId + "/move")
        .then()
            .statusCode(400)
            .body("error", equalTo("Position already occupied"));
    }

    @Test
    @Order(5)
    void testMakeMoveOutsideBoard() {
        ensureLifecycleGameAccepted();

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
            .body("{\"position\": {\"x\": 10, \"y\": 10}}")  // Outside 9x9 board
        .when()
            .post("/api/games/" + lifecycleGameId + "/move")
        .then()
            .statusCode(400)
            .body("error", equalTo("Position is outside the board"));
    }

    // ===== Pass Tests (Sequential - continue lifecycle game) =====

    @Test
    @Order(6)
    void testPass() {
        ensureLifecycleGameAccepted();

        // It's black's turn (player1)
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
        .when()
            .post("/api/games/" + lifecycleGameId + "/pass")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("currentTurn", equalTo("white"));
    }

    @Test
    @Order(7)
    void testPassWrongTurn() {
        ensureLifecycleGameAccepted();

        // It's white's turn, black tries to pass
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
        .when()
            .post("/api/games/" + lifecycleGameId + "/pass")
        .then()
            .statusCode(400)
            .body("error", equalTo("It's not your turn"));
    }

    // ===== Resign Tests =====

    @Test
    void testResign() {
        String gameId = createGame(player1Token, player2Username, "black");

        // Accept the game
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player2Token)
        .when()
            .post("/api/games/" + gameId + "/accept")
        .then()
            .statusCode(200);

        // Player 1 (black) resigns
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
        .when()
            .post("/api/games/" + gameId + "/resign")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo("completed"))
            .body("result.winner", equalTo("white"))
            .body("result.method", equalTo("resignation"));
    }

    @Test
    void testResignPendingGame() {
        String gameId = createGame(player1Token, player2Username, "black");

        // Try to resign from pending game
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + player1Token)
        .when()
            .post("/api/games/" + gameId + "/resign")
        .then()
            .statusCode(400)
            .body("error", equalTo("Game is not active"));
    }
}
