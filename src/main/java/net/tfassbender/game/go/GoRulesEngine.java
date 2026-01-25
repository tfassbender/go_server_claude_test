package net.tfassbender.game.go;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

@ApplicationScoped
public class GoRulesEngine {

    /**
     * Result of a move validation/execution
     */
    public static class MoveResult {
        public boolean success;
        public String error;
        public List<Position> capturedStones;

        public MoveResult(boolean success) {
            this.success = success;
            this.capturedStones = new ArrayList<>();
        }

        public MoveResult(String error) {
            this.success = false;
            this.error = error;
            this.capturedStones = new ArrayList<>();
        }

        public static MoveResult success(List<Position> capturedStones) {
            MoveResult result = new MoveResult(true);
            result.capturedStones = capturedStones;
            return result;
        }

        public static MoveResult error(String message) {
            return new MoveResult(message);
        }
    }

    /**
     * Validate and execute a move on the board
     * Returns captured stones if move is valid
     */
    public MoveResult validateAndExecuteMove(Board board, Position position, Stone stone, String previousBoardHash) {
        // Check if position is valid
        if (!board.isValidPosition(position)) {
            return MoveResult.error("Position is outside the board");
        }

        // Check if intersection is empty
        if (!board.isEmpty(position)) {
            return MoveResult.error("Position already occupied");
        }

        // Create a copy of the board to simulate the move
        Board simBoard = new Board(board);
        simBoard.setStone(position, stone);

        // Check for captures (opponent stones with no liberties)
        List<Position> capturedStones = detectCaptures(simBoard, position, stone);

        // Remove captured stones from simulation
        for (Position captured : capturedStones) {
            simBoard.removeStone(captured);
        }

        // Check suicide rule (placed stone must have liberties or capture opponent stones)
        if (simBoard.countLiberties(position) == 0 && capturedStones.isEmpty()) {
            return MoveResult.error("Suicide move not allowed");
        }

        // Check Ko rule (board state cannot repeat immediately)
        if (previousBoardHash != null && !previousBoardHash.isEmpty()) {
            String newBoardHash = simBoard.getBoardHash();
            if (newBoardHash.equals(previousBoardHash)) {
                return MoveResult.error("Ko rule violation");
            }
        }

        // Move is valid - apply to actual board
        board.setStone(position, stone);
        for (Position captured : capturedStones) {
            board.removeStone(captured);
        }

        return MoveResult.success(capturedStones);
    }

    /**
     * Detect all captured stones after placing a stone
     * Returns list of positions of captured opponent stones
     */
    private List<Position> detectCaptures(Board board, Position placedStone, Stone color) {
        List<Position> captured = new ArrayList<>();
        Stone opponentColor = color.opposite();

        // Check all adjacent opponent groups
        for (Position adjacent : board.getAdjacentPositions(placedStone)) {
            if (board.getStone(adjacent) == opponentColor) {
                Set<Position> group = board.getGroup(adjacent);
                int liberties = board.countLiberties(group);

                // If group has no liberties, it's captured
                if (liberties == 0) {
                    captured.addAll(group);
                }
            }
        }

        return captured;
    }

    /**
     * Reconstruct board state from a list of moves
     */
    public Board reconstructBoard(int boardSize, List<MoveData> moves) {
        Board board = new Board(boardSize);
        String previousHash = null;

        for (MoveData move : moves) {
            if ("place".equals(move.action)) {
                Stone stone = Stone.fromString(move.player);
                MoveResult result = validateAndExecuteMove(board, move.position, stone, previousHash);

                if (!result.success) {
                    throw new IllegalStateException("Invalid move in history: " + result.error);
                }

                previousHash = board.getBoardHash();
            }
        }

        return board;
    }

    /**
     * Simple move data class for reconstruction
     */
    public static class MoveData {
        public String player; // "black" or "white"
        public String action; // "place", "pass", "resign"
        public Position position;

        public MoveData() {}

        public MoveData(String player, String action, Position position) {
            this.player = player;
            this.action = action;
            this.position = position;
        }
    }
}
