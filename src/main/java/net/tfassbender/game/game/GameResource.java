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

    @Inject
    net.tfassbender.game.ai.GnuGoService gnuGoService;

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
                    request.requestedColor,
                    request.komi,
                    request.allowUndo
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
                gameInfo.put("isCreator", username.equals(game.createdBy));
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
            response.put("komi", game.komi);
            response.put("allowUndo", game.allowUndo);

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

    /**
     * Undo the last move(s) in a game
     */
    @POST
    @Path("/{gameId}/undo")
    public Response undoMove(@PathParam("gameId") String gameId) {
        try {
            String username = jwt.getName();
            gameService.undoMove(gameId, username);

            Optional<Game> gameOpt = gameService.getGame(gameId);
            Game game = gameOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("currentTurn", game.currentTurn);
            response.put("movesCount", game.moves.size());

            // Broadcast undo event to SSE clients
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("currentTurn", game.currentTurn);
            eventData.put("movesCount", game.moves.size());
            eventService.broadcastEvent(gameId, "undo", eventData, sse);

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Undo failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error undoing move in game {}", gameId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", "Failed to undo move"))
                    .build();
        }
    }

    /**
     * Recalculate score with manually marked dead stones
     */
    @POST
    @Path("/{gameId}/recalculate-score")
    public Response recalculateScore(@PathParam("gameId") String gameId, RecalculateScoreRequest request) {
        try {
            String username = jwt.getName();

            if (request.manuallyMarkedDeadStones == null) {
                request.manuallyMarkedDeadStones = new ArrayList<>();
            }

            GameResult result = gameService.recalculateScore(gameId, username, request.manuallyMarkedDeadStones);

            // Broadcast scoreUpdate event to SSE clients
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("winner", result.winner);
            eventData.put("method", result.method);
            eventData.put("score", result.score);
            eventData.put("territory", result.territory);
            eventData.put("captures", result.captures);
            eventService.broadcastEvent(gameId, "scoreUpdate", eventData, sse);

            return Response.ok(result).build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Recalculate score failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error recalculating score for game {}", gameId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to recalculate score"))
                    .build();
        }
    }

    /**
     * Calculate score for a fork (non-persisted game state).
     * This endpoint calculates the score based on provided moves without requiring a saved game.
     */
    @POST
    @Path("/calculate-fork-score")
    public Response calculateForkScore(CalculateForkScoreRequest request) {
        try {
            if (request.boardSize != 9 && request.boardSize != 13 && request.boardSize != 19) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Board size must be 9, 13, or 19"))
                        .build();
            }

            if (request.moves == null) {
                request.moves = new ArrayList<>();
            }

            if (request.manuallyMarkedDeadStones == null) {
                request.manuallyMarkedDeadStones = new ArrayList<>();
            }

            GameResult result = gameService.calculateForkScore(
                    request.boardSize,
                    request.moves,
                    request.komi,
                    request.manuallyMarkedDeadStones
            );

            return Response.ok(result).build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Fork score calculation failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error calculating fork score", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to calculate fork score"))
                    .build();
        }
    }

    /**
     * Get AI move suggestion for a given board position.
     * This is used in the fork/analysis tool to get AI suggestions.
     */
    @POST
    @Path("/ai-move-suggestion")
    public Response getAiMoveSuggestion(AiMoveSuggestionRequest request) {
        try {
            if (request.boardSize != 9 && request.boardSize != 13 && request.boardSize != 19) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Board size must be 9, 13, or 19"))
                        .build();
            }

            if (request.level < 1 || request.level > 10) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "AI level must be between 1 and 10"))
                        .build();
            }

            if (request.moves == null) {
                request.moves = new ArrayList<>();
            }

            if (!"black".equals(request.colorToMove) && !"white".equals(request.colorToMove)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "colorToMove must be 'black' or 'white'"))
                        .build();
            }

            Position suggestedMove = gnuGoService.generateMoveForPosition(
                    request.boardSize,
                    request.moves,
                    request.level,
                    request.colorToMove,
                    request.komi
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("position", suggestedMove);
            response.put("isPass", suggestedMove == null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("Error generating AI move suggestion", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to generate AI move suggestion"))
                    .build();
        }
    }

    // Request DTOs
    public static class CreateGameRequest {
        public int boardSize;
        public String opponentUsername;
        public String requestedColor;
        public double komi = 5.5;  // Default komi
        public boolean allowUndo = false;  // Default false
    }

    public static class MoveRequest {
        public String action;
        public Position position;
    }

    public static class RecalculateScoreRequest {
        public List<Position> manuallyMarkedDeadStones;
    }

    public static class CalculateForkScoreRequest {
        public int boardSize;
        public List<Move> moves;
        public double komi = 5.5;
        public List<Position> manuallyMarkedDeadStones;
    }

    public static class AiMoveSuggestionRequest {
        public int boardSize;
        public List<Move> moves;
        public double komi = 5.5;
        public int level;  // AI difficulty level (1-10)
        public String colorToMove;  // "black" or "white"
    }
}
