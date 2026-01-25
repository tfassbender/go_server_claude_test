package net.tfassbender.game.game;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Game {
    public String id;
    public int boardSize;
    public String blackPlayer;
    public String whitePlayer;
    public String currentTurn;      // "black" or "white"
    public String status;           // "pending", "active", "completed"
    public Instant createdAt;
    public Instant lastMoveAt;
    public List<Move> moves;
    public int passes;              // Consecutive passes (game ends after 2)
    public GameResult result;
    public String previousBoardHash; // For Ko detection

    public Game() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.lastMoveAt = createdAt;
        this.moves = new ArrayList<>();
        this.passes = 0;
        this.status = "pending";
        this.currentTurn = "black";
    }

    public Game(int boardSize, String blackPlayer, String whitePlayer) {
        this();
        this.boardSize = boardSize;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
    }

    public boolean isPlayerTurn(String username) {
        if ("black".equals(currentTurn)) {
            return username.equals(blackPlayer);
        } else {
            return username.equals(whitePlayer);
        }
    }

    public boolean isPlayer(String username) {
        return username.equals(blackPlayer) || username.equals(whitePlayer);
    }

    public String getPlayerColor(String username) {
        if (username.equals(blackPlayer)) {
            return "black";
        } else if (username.equals(whitePlayer)) {
            return "white";
        }
        return null;
    }

    public void switchTurn() {
        currentTurn = "black".equals(currentTurn) ? "white" : "black";
    }

    public void addMove(Move move) {
        moves.add(move);
        lastMoveAt = Instant.now();

        // Update pass counter
        if ("pass".equals(move.action)) {
            passes++;
            // Game ends after two consecutive passes
            if (passes >= 2) {
                status = "completed";
                // For MVP, winner would be determined manually or by scoring system
            }
        } else if ("place".equals(move.action)) {
            passes = 0; // Reset pass counter on stone placement
        } else if ("resign".equals(move.action)) {
            status = "completed";
            String winner = "black".equals(move.player) ? "white" : "black";
            result = new GameResult(winner, "resignation");
        }
    }
}
