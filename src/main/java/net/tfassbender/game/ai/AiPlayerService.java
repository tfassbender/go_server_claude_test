package net.tfassbender.game.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import net.tfassbender.game.game.Game;
import net.tfassbender.game.game.GameEventService;
import net.tfassbender.game.game.GameService;
import net.tfassbender.game.game.Move;
import net.tfassbender.game.game.events.GameCreatedEvent;
import net.tfassbender.game.game.events.TurnChangedEvent;
import net.tfassbender.game.go.Position;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service that handles AI player behavior in games.
 * Observes game events and generates AI moves when appropriate.
 */
@ApplicationScoped
public class AiPlayerService {
    private static final Logger LOG = LoggerFactory.getLogger(AiPlayerService.class);

    @Inject
    GnuGoService gnuGoService;

    @Inject
    GameService gameService;

    @Inject
    GameEventService gameEventService;

    @ConfigProperty(name = "ai.gnugo.enabled", defaultValue = "false")
    boolean gnugoEnabled;

    /**
     * Handles game creation events.
     * If the opponent is an AI bot, auto-accept the game invitation.
     */
    void onGameCreated(@Observes GameCreatedEvent event) {
        if (!gnugoEnabled) {
            return;
        }

        String gameId = event.getGameId();
        LOG.debug("Game created event received for game {}", gameId);

        try {
            Optional<Game> gameOpt = gameService.getGame(gameId);
            if (gameOpt.isEmpty()) {
                LOG.warn("Game {} not found after creation event", gameId);
                return;
            }

            Game game = gameOpt.get();

            // Check if either player is an AI bot
            boolean blackIsAi = gnuGoService.isAiBot(game.blackPlayer);
            boolean whiteIsAi = gnuGoService.isAiBot(game.whitePlayer);

            if (!blackIsAi && !whiteIsAi) {
                // Human vs human game, nothing to do
                return;
            }

            // Determine which player should auto-accept (the one who didn't create the game)
            String aiPlayer = null;
            if (blackIsAi && !game.createdBy.equals(game.blackPlayer)) {
                aiPlayer = game.blackPlayer;
            } else if (whiteIsAi && !game.createdBy.equals(game.whitePlayer)) {
                aiPlayer = game.whitePlayer;
            }

            if (aiPlayer != null) {
                LOG.info("AI bot {} auto-accepting game {}", aiPlayer, gameId);
                gameService.acceptGame(gameId, aiPlayer);
                // Note: acceptGame will fire TurnChangedEvent, which will trigger AI move if needed
            }

        } catch (Exception e) {
            LOG.error("Error handling game creation for AI bot in game {}", gameId, e);
        }
    }

    /**
     * Handles turn change events.
     * If it's an AI bot's turn, generate and execute a move.
     */
    void onTurnChanged(@Observes TurnChangedEvent event) {
        if (!gnugoEnabled) {
            return;
        }

        String gameId = event.getGameId();
        String currentTurn = event.getCurrentTurn();
        LOG.debug("Turn changed event received for game {}: {}'s turn", gameId, currentTurn);

        try {
            Optional<Game> gameOpt = gameService.getGame(gameId);
            if (gameOpt.isEmpty()) {
                LOG.warn("Game {} not found after turn change event", gameId);
                return;
            }

            Game game = gameOpt.get();

            // Only process if game is active
            if (!"active".equals(game.status)) {
                LOG.debug("Game {} is not active (status: {}), skipping AI move", gameId, game.status);
                return;
            }

            // Determine which player should move
            String playerToMove = "black".equals(currentTurn) ? game.blackPlayer : game.whitePlayer;

            // Check if it's an AI bot's turn
            if (!gnuGoService.isAiBot(playerToMove)) {
                LOG.debug("Current player {} is not an AI bot, skipping", playerToMove);
                return;
            }

            LOG.info("AI bot {}'s turn in game {}, generating move...", playerToMove, gameId);

            // Execute AI move asynchronously to avoid blocking the event observer
            executeAiMoveAsync(gameId, playerToMove);

        } catch (Exception e) {
            LOG.error("Error handling turn change for AI bot in game {}", gameId, e);
        }
    }

    /**
     * Executes an AI move in a background thread.
     */
    private void executeAiMoveAsync(String gameId, String botUsername) {
        CompletableFuture.runAsync(() -> {
            try {
                makeAiMove(gameId, botUsername);
            } catch (Exception e) {
                LOG.error("Error executing AI move for bot {} in game {}", botUsername, gameId, e);
            }
        });
    }

    /**
     * Generates and executes an AI move.
     */
    private void makeAiMove(String gameId, String botUsername) {
        try {
            // Fetch latest game state
            Optional<Game> gameOpt = gameService.getGame(gameId);
            if (gameOpt.isEmpty()) {
                LOG.error("Game {} not found when generating AI move", gameId);
                return;
            }

            Game game = gameOpt.get();

            // Double-check game is still active and it's still the AI's turn
            if (!"active".equals(game.status)) {
                LOG.warn("Game {} is no longer active, canceling AI move", gameId);
                return;
            }

            if (!game.isPlayerTurn(botUsername)) {
                LOG.warn("It's no longer {}'s turn in game {}, canceling AI move", botUsername, gameId);
                return;
            }

            // Generate move from KataGo
            Position position = gnuGoService.generateMove(botUsername, game);

            if (position == null) {
                // AI decided to pass
                LOG.info("AI bot {} passing in game {}", botUsername, gameId);
                gameService.pass(gameId, botUsername);
                broadcastPassEvent(gameId);
            } else {
                // AI made a move
                LOG.info("AI bot {} playing at {} in game {}", botUsername, position, gameId);
                GameService.MoveResponse response = gameService.makeMove(gameId, botUsername, position);

                if (!response.success) {
                    LOG.error("AI move failed for bot {} in game {}: {}", botUsername, gameId, response.error);
                    // If move failed, try passing instead as fallback
                    LOG.info("AI bot {} passing as fallback in game {}", botUsername, gameId);
                    gameService.pass(gameId, botUsername);
                    broadcastPassEvent(gameId);
                } else {
                    broadcastMoveEvent(gameId);
                }
            }

        } catch (Exception e) {
            LOG.error("Error making AI move for bot {} in game {}", botUsername, gameId, e);
            // On error, attempt to pass to keep the game moving
            try {
                LOG.info("AI bot {} passing due to error in game {}", botUsername, gameId);
                gameService.pass(gameId, botUsername);
                broadcastPassEvent(gameId);
            } catch (Exception passError) {
                LOG.error("Failed to pass after AI move error in game {}", gameId, passError);
            }
        }
    }

    /**
     * Broadcasts a move event to SSE clients.
     */
    private void broadcastMoveEvent(String gameId) {
        try {
            Optional<Game> gameOpt = gameService.getGame(gameId);
            if (gameOpt.isEmpty()) return;

            Game game = gameOpt.get();
            Move lastMove = game.moves.get(game.moves.size() - 1);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", lastMove.player);
            eventData.put("action", lastMove.action);
            eventData.put("position", lastMove.position);
            eventData.put("capturedStones", lastMove.capturedStones);
            eventData.put("currentTurn", game.currentTurn);

            gameEventService.broadcastEvent(gameId, "move", eventData);
        } catch (Exception e) {
            LOG.error("Error broadcasting move event for game {}", gameId, e);
        }
    }

    /**
     * Broadcasts a pass event to SSE clients.
     */
    private void broadcastPassEvent(String gameId) {
        try {
            Optional<Game> gameOpt = gameService.getGame(gameId);
            if (gameOpt.isEmpty()) return;

            Game game = gameOpt.get();
            Move lastMove = game.moves.get(game.moves.size() - 1);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", lastMove.player);
            eventData.put("passes", game.passes);
            eventData.put("currentTurn", game.currentTurn);
            eventData.put("status", game.status);

            gameEventService.broadcastEvent(gameId, "pass", eventData);

            // If game ended (two consecutive passes), broadcast gameEnd event
            if ("completed".equals(game.status)) {
                broadcastGameEndEvent(gameId, game);
            }
        } catch (Exception e) {
            LOG.error("Error broadcasting pass event for game {}", gameId, e);
        }
    }

    /**
     * Broadcasts a game end event to SSE clients.
     */
    private void broadcastGameEndEvent(String gameId, Game game) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("status", game.status);
            eventData.put("result", game.result);
            if (game.result != null) {
                eventData.put("winner", game.result.winner);
            }

            gameEventService.broadcastEvent(gameId, "gameEnd", eventData);
        } catch (Exception e) {
            LOG.error("Error broadcasting game end event for game {}", gameId, e);
        }
    }
}
