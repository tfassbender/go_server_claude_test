package net.tfassbender.game.ai;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.tfassbender.game.ai.gtp.GtpClient;
import net.tfassbender.game.ai.gtp.GtpResponse;
import net.tfassbender.game.game.Game;
import net.tfassbender.game.game.Move;
import net.tfassbender.game.go.Position;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * High-level service for GNU Go AI operations.
 * Manages move generation, board state synchronization, and difficulty configuration.
 */
@ApplicationScoped
public class GnuGoService {
    private static final Logger LOG = LoggerFactory.getLogger(GnuGoService.class);

    @Inject
    GtpClient gtpClient;

    @ConfigProperty(name = "ai.bots.config")
    String botsConfig;

    @ConfigProperty(name = "ai.gnugo.enabled", defaultValue = "false")
    boolean gnugoEnabled;

    // Map of bot username to GNU Go level (1-10)
    private final Map<String, Integer> botDifficulties = new HashMap<>();

    @PostConstruct
    public void initialize() {
        if (!gnugoEnabled) {
            LOG.info("GNU Go AI is disabled, skipping bot configuration");
            return;
        }

        LOG.info("Initializing GNU Go service with bot configurations...");
        parseBotConfig();
        LOG.info("Configured {} AI bots", botDifficulties.size());
    }

    /**
     * Parses bot configuration from application.properties.
     * Format: "username:level,username:level,..." where level is 1-10
     */
    private void parseBotConfig() {
        String[] botEntries = botsConfig.split(",");
        for (String entry : botEntries) {
            String[] parts = entry.trim().split(":");
            if (parts.length == 2) {
                String username = parts[0].trim();
                int level = Integer.parseInt(parts[1].trim());
                botDifficulties.put(username, level);
                LOG.debug("Registered AI bot: {} with level {}", username, level);
            }
        }
    }

    /**
     * Generates an AI move for the given game.
     *
     * @param botUsername The username of the AI bot (determines difficulty)
     * @param game The current game state
     * @return The position to play, or null if the AI decides to pass
     */
    public Position generateMove(String botUsername, Game game) {
        if (!gnugoEnabled) {
            LOG.warn("GNU Go is disabled, cannot generate move");
            return null;
        }

        Integer level = botDifficulties.get(botUsername);
        if (level == null) {
            LOG.error("Unknown bot username: {}", botUsername);
            return null;
        }

        try {
            // 1. Sync board state with GNU Go
            syncBoardState(game);

            // 2. Set difficulty level
            setDifficulty(level);

            // 3. Determine which color should move
            String colorToMove = game.currentTurn;

            // 4. Request move from GNU Go
            GtpResponse response = gtpClient.sendCommand("genmove " + colorToMove);
            if (!response.isSuccess()) {
                LOG.error("Failed to generate move: {}", response.getError());
                return null;
            }

            // 5. Parse the response
            String moveStr = response.getResult().trim();
            LOG.info("GNU Go suggested move: {}", moveStr);

            // Check for pass
            if ("pass".equalsIgnoreCase(moveStr) || "PASS".equalsIgnoreCase(moveStr)) {
                return null; // null indicates a pass
            }

            // 6. Convert GTP coordinate to Position
            return gtpToPosition(moveStr, game.boardSize);

        } catch (Exception e) {
            LOG.error("Error generating AI move for bot {}", botUsername, e);
            return null;
        }
    }

    /**
     * Syncs the current board state with GNU Go by clearing the board
     * and replaying all moves from the game history.
     */
    private void syncBoardState(Game game) {
        // Clear the board
        gtpClient.sendCommand("clear_board");

        // Set board size
        gtpClient.sendCommand("boardsize " + game.boardSize);

        // Set komi
        gtpClient.sendCommand("komi " + game.komi);

        // Replay all moves
        for (Move move : game.moves) {
            if ("place".equals(move.action) && move.position != null) {
                String gtpMove = positionToGtp(move.position, game.boardSize);
                String command = "play " + move.player + " " + gtpMove;
                GtpResponse response = gtpClient.sendCommand(command);
                if (!response.isSuccess()) {
                    LOG.warn("Failed to sync move {}: {}", gtpMove, response.getError());
                }
            } else if ("pass".equals(move.action)) {
                String command = "play " + move.player + " pass";
                gtpClient.sendCommand(command);
            }
        }

        LOG.debug("Synced board state: {} moves on {}x{} board", game.moves.size(), game.boardSize, game.boardSize);
    }

    /**
     * Sets the AI difficulty by configuring the GNU Go level (1-10).
     */
    private void setDifficulty(int level) {
        String command = "level " + level;
        GtpResponse response = gtpClient.sendCommand(command);
        if (response.isSuccess()) {
            LOG.debug("Set difficulty to level {}", level);
        } else {
            LOG.warn("Failed to set difficulty: {}", response.getError());
        }
    }

    /**
     * Converts a Position to GTP coordinate format.
     * GTP format: Letter (A-T, skip I) + number (1-19)
     * Example: Position(3, 15) on 19x19 board -> "D4"
     *
     * @param pos The position (0-based, y=0 is top)
     * @param boardSize The board size
     * @return GTP coordinate string (e.g., "D4")
     */
    public String positionToGtp(Position pos, int boardSize) {
        // Convert x coordinate to letter (skip 'I')
        int xAdjusted = pos.x >= 8 ? pos.x + 1 : pos.x;
        char col = (char) ('A' + xAdjusted);

        // Convert y coordinate (flip: y=0 is top in our system, but bottom in GTP)
        int row = boardSize - pos.y;

        return "" + col + row;
    }

    /**
     * Converts a GTP coordinate to a Position.
     * GTP format: Letter (A-T, skip I) + number (1-19)
     * Example: "D4" -> Position(3, 15) on 19x19 board
     *
     * @param gtp The GTP coordinate (e.g., "D4")
     * @param boardSize The board size
     * @return Position object
     */
    public Position gtpToPosition(String gtp, int boardSize) {
        if (gtp == null || gtp.length() < 2) {
            throw new IllegalArgumentException("Invalid GTP coordinate: " + gtp);
        }

        // Extract column letter and row number
        char colChar = gtp.charAt(0);
        int row = Integer.parseInt(gtp.substring(1));

        // Convert letter to x coordinate (skip 'I')
        int x = colChar - 'A';
        if (x >= 8) { // 'I' is skipped, so J becomes 8, not 9
            x--;
        }

        // Convert row to y coordinate (flip: GTP counts from bottom, we count from top)
        int y = boardSize - row;

        return new Position(x, y);
    }

    /**
     * Generates an AI move suggestion for a given board position.
     * This is used for analysis/fork mode where we want AI suggestions for any position.
     *
     * @param boardSize The size of the board (9, 13, or 19)
     * @param moves The list of moves that define the current position
     * @param level The AI difficulty level (1-10)
     * @param colorToMove The color that should make the next move ("black" or "white")
     * @param komi The komi value
     * @return The suggested position, or null if AI suggests passing
     */
    public Position generateMoveForPosition(int boardSize, java.util.List<Move> moves, int level, String colorToMove, double komi) {
        if (!gnugoEnabled) {
            LOG.warn("GNU Go is disabled, cannot generate move suggestion");
            return null;
        }

        try {
            // 1. Clear and set up the board
            gtpClient.sendCommand("clear_board");
            gtpClient.sendCommand("boardsize " + boardSize);
            gtpClient.sendCommand("komi " + komi);

            // 2. Replay all moves to reach the desired position
            for (Move move : moves) {
                if ("place".equals(move.action) && move.position != null) {
                    String gtpMove = positionToGtp(move.position, boardSize);
                    String command = "play " + move.player + " " + gtpMove;
                    GtpResponse response = gtpClient.sendCommand(command);
                    if (!response.isSuccess()) {
                        LOG.warn("Failed to sync move {}: {}", gtpMove, response.getError());
                    }
                } else if ("pass".equals(move.action)) {
                    String command = "play " + move.player + " pass";
                    gtpClient.sendCommand(command);
                }
            }

            // 3. Set difficulty level
            setDifficulty(level);

            // 4. Request move from GNU Go for the specified color
            GtpResponse response = gtpClient.sendCommand("genmove " + colorToMove);
            if (!response.isSuccess()) {
                LOG.error("Failed to generate move suggestion: {}", response.getError());
                return null;
            }

            // 5. Parse the response
            String moveStr = response.getResult().trim();
            LOG.info("GNU Go suggested move for {} at level {}: {}", colorToMove, level, moveStr);

            // Check for pass
            if ("pass".equalsIgnoreCase(moveStr) || "PASS".equalsIgnoreCase(moveStr)) {
                return null; // null indicates a pass
            }

            // 6. Convert GTP coordinate to Position
            return gtpToPosition(moveStr, boardSize);

        } catch (Exception e) {
            LOG.error("Error generating AI move suggestion at level {}", level, e);
            return null;
        }
    }

    /**
     * Checks if a given username is an AI bot.
     */
    public boolean isAiBot(String username) {
        return botDifficulties.containsKey(username);
    }

    /**
     * Gets the list of all configured AI bot usernames.
     */
    public Map<String, Integer> getBotDifficulties() {
        return new HashMap<>(botDifficulties);
    }
}
