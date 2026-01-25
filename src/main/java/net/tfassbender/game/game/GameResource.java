package net.tfassbender.game.game;

import net.tfassbender.game.go.Board;
import net.tfassbender.game.go.Position;
import net.tfassbender.game.go.Stone;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Path("/api/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("User")
public class GameResource {

    private static final Logger LOG = LoggerFactory.getLogger(GameResource.class);

    @Inject
    GameService gameService;

    @Inject
    GameEventService eventService;

    @Inject
    JsonWebToken jwt;

    @jakarta.ws.rs.core.Context
    jakarta.ws.rs.sse.Sse sse;

    /**
     * Create a new game
     */
    @POST
    public Response createGame(CreateGameRequest request) {
        try {
            String username = jwt.getName();

            Game game = gameService.createGame(
                    username,
                    request.boardSize,
                    request.opponentUsername,
                    request.requestedColor
            );

            Map<String, Object> response = new HashMap<>();
            response.put("gameId", game.id);
            response.put("status", game.status);
            response.put("blackPlayer", game.blackPlayer);
            response.put("whitePlayer", game.whitePlayer);

            return Response.status(Response.Status.CREATED).entity(response).build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Game creation failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error creating game", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to create game"))
                    .build();
        }
    }

    /**
     * Get list of games for current user
     */
    @GET
    public Response getGames(@QueryParam("status") String status) {
        try {
            String username = jwt.getName();
            List<Game> games = gameService.getUserGames(username, status);

            List<Map<String, Object>> response = games.stream().map(game -> {
                Map<String, Object> gameInfo = new HashMap<>();
                gameInfo.put("id", game.id);
                gameInfo.put("opponent", username.equals(game.blackPlayer) ? game.whitePlayer : game.blackPlayer);
                gameInfo.put("yourColor", game.getPlayerColor(username));
                gameInfo.put("currentTurn", game.currentTurn);
                gameInfo.put("lastMoveAt", game.lastMoveAt);
                gameInfo.put("status", game.status);
                gameInfo.put("boardSize", game.boardSize);
                return gameInfo;
            }).collect(Collectors.toList());

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("Error retrieving games", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve games"))
                    .build();
        }
    }

    /**
     * Get specific game details
     */
    @GET
    @Path("/{gameId}")
    public Response getGame(@PathParam("gameId") String gameId) {
        try {
            String username = jwt.getName();
            Optional<Game> gameOpt = gameService.getGame(gameId);

            if (gameOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Game not found"))
                        .build();
            }

            Game game = gameOpt.get();

            // Verify user is a player
            if (!game.isPlayer(username)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(Map.of("error", "You are not a player in this game"))
                        .build();
            }

            // Get current board state
            Board board = gameService.getCurrentBoardState(gameId);
            List<Map<String, Object>> stones = new ArrayList<>();
            for (int x = 0; x < board.getSize(); x++) {
                for (int y = 0; y < board.getSize(); y++) {
                    Stone stone = board.getStone(x, y);
                    if (stone != null) {
                        Map<String, Object> stoneInfo = new HashMap<>();
                        stoneInfo.put("position", Map.of("x", x, "y", y));
                        stoneInfo.put("color", stone.toDisplayString());
                        stones.add(stoneInfo);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", game.id);
            response.put("boardSize", game.boardSize);
            response.put("blackPlayer", game.blackPlayer);
            response.put("whitePlayer", game.whitePlayer);
            response.put("currentTurn", game.currentTurn);
            response.put("status", game.status);
            response.put("createdAt", game.createdAt);
            response.put("lastMoveAt", game.lastMoveAt);
            response.put("moves", game.moves);
            response.put("passes", game.passes);
            response.put("result", game.result);
            response.put("boardState", Map.of("stones", stones));

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("Error retrieving game {}", gameId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve game"))
                    .build();
        }
    }

    /**
     * Accept a game invitation
     */
    @POST
    @Path("/{gameId}/accept")
    public Response acceptGame(@PathParam("gameId") String gameId) {
        try {
            String username = jwt.getName();
            Game game = gameService.acceptGame(gameId, username);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Game accepted");
            response.put("status", game.status);

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Game acceptance failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error accepting game {}", gameId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to accept game"))
                    .build();
        }
    }

    /**
     * Decline a game invitation
     */
    @POST
    @Path("/{gameId}/decline")
    public Response declineGame(@PathParam("gameId") String gameId) {
        try {
            String username = jwt.getName();
            gameService.declineGame(gameId, username);

            return Response.ok(Map.of("message", "Game declined", "status", "cancelled")).build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Game decline failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error declining game {}", gameId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to decline game"))
                    .build();
        }
    }

    /**
     * Make a move in a game
     */
    @POST
    @Path("/{gameId}/move")
    public Response makeMove(@PathParam("gameId") String gameId, MoveRequest request) {
        try {
            String username = jwt.getName();

            if (request.position == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Position is required"))
                        .build();
            }

            GameService.MoveResponse moveResponse = gameService.makeMove(gameId, username, request.position);

            if (!moveResponse.success) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("success", false, "error", moveResponse.error))
                        .build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("capturedStones", moveResponse.capturedStones);
            response.put("currentTurn", moveResponse.currentTurn);

            // Broadcast move event to SSE clients
            Optional<Game> gameOpt = gameService.getGame(gameId);
            if (gameOpt.isPresent()) {
                Game game = gameOpt.get();
                Move lastMove = game.moves.get(game.moves.size() - 1);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("player", lastMove.player);
                eventData.put("action", lastMove.action);
                eventData.put("position", lastMove.position);
                eventData.put("capturedStones", lastMove.capturedStones);
                eventData.put("currentTurn", game.currentTurn);
                eventService.broadcastEvent(gameId, "move", eventData, sse);
            }

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Move failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error making move in game {}", gameId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", "Failed to make move"))
                    .build();
        }
    }

    /**
     * Pass turn
     */
    @POST
    @Path("/{gameId}/pass")
    public Response pass(@PathParam("gameId") String gameId) {
        try {
            String username = jwt.getName();
            gameService.pass(gameId, username);

            Optional<Game> gameOpt = gameService.getGame(gameId);
            Game game = gameOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("passes", game.passes);
            response.put("currentTurn", game.currentTurn);
            response.put("status", game.status);

            // Broadcast pass event to SSE clients
            Move lastMove = game.moves.get(game.moves.size() - 1);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", lastMove.player);
            eventData.put("passes", game.passes);
            eventData.put("currentTurn", game.currentTurn);
            eventData.put("status", game.status);
            eventService.broadcastEvent(gameId, "pass", eventData, sse);

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Pass failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error passing in game {}", gameId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", "Failed to pass"))
                    .build();
        }
    }

    /**
     * Resign from game
     */
    @POST
    @Path("/{gameId}/resign")
    public Response resign(@PathParam("gameId") String gameId) {
        try {
            String username = jwt.getName();
            GameResult result = gameService.resign(gameId, username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", "completed");
            response.put("result", result);

            // Broadcast resign/game end event to SSE clients
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", username);
            eventData.put("winner", result.winner);
            eventData.put("method", result.method);
            eventService.broadcastEvent(gameId, "gameEnd", eventData, sse);

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Resign failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error resigning from game {}", gameId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", "Failed to resign"))
                    .build();
        }
    }

    // Request DTOs
    public static class CreateGameRequest {
        public int boardSize;
        public String opponentUsername;
        public String requestedColor;
    }

    public static class MoveRequest {
        public String action;
        public Position position;
    }
}
