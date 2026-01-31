package net.tfassbender.game.game;

import net.tfassbender.game.game.events.GameCreatedEvent;
import net.tfassbender.game.game.events.TurnChangedEvent;
import net.tfassbender.game.go.Board;
import net.tfassbender.game.go.GoRulesEngine;
import net.tfassbender.game.go.Position;
import net.tfassbender.game.go.ScoringEngine;
import net.tfassbender.game.go.Stone;
import net.tfassbender.game.user.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@ApplicationScoped
public class GameService {

    private static final Logger LOG = LoggerFactory.getLogger(GameService.class);

    @Inject
    GameRepository gameRepository;

    @Inject
    UserService userService;

    @Inject
    GoRulesEngine rulesEngine;

    @Inject
    ScoringEngine scoringEngine;

    @Inject
    Event<GameCreatedEvent> gameCreatedEvent;

    @Inject
    Event<TurnChangedEvent> turnChangedEvent;

    /**
     * Create a new game
     */
    public Game createGame(String creatorUsername, int boardSize, String opponentUsername, String requestedColor, double komi) throws IOException {
        // Validate board size
        if (boardSize != 9 && boardSize != 13 && boardSize != 19) {
            throw new IllegalArgumentException("Board size must be 9, 13, or 19");
        }

        // Validate opponent exists
        if (userService.getUser(opponentUsername).isEmpty()) {
            throw new IllegalArgumentException("Opponent user not found");
        }

        // Cannot play against self
        if (creatorUsername.equals(opponentUsername)) {
            throw new IllegalArgumentException("Cannot create game with yourself");
        }

        // Determine colors (case-insensitive comparison)
        String blackPlayer, whitePlayer;
        if ("random".equalsIgnoreCase(requestedColor)) {
            // Random assignment
            if (new Random().nextBoolean()) {
                blackPlayer = creatorUsername;
                whitePlayer = opponentUsername;
            } else {
                blackPlayer = opponentUsername;
                whitePlayer = creatorUsername;
            }
        } else if ("white".equalsIgnoreCase(requestedColor)) {
            blackPlayer = opponentUsername;
            whitePlayer = creatorUsername;
        } else if ("black".equalsIgnoreCase(requestedColor)) {
            blackPlayer = creatorUsername;
            whitePlayer = opponentUsername;
        } else {
            throw new IllegalArgumentException("Invalid color: must be 'black', 'white', or 'random'");
        }

        // Create game
        Game game = new Game(boardSize, blackPlayer, whitePlayer, komi);
        game.createdBy = creatorUsername;
        gameRepository.save(game);

        LOG.info("Created game {}: {} (black) vs {} (white)", game.id, blackPlayer, whitePlayer);

        // Fire game created event
        gameCreatedEvent.fire(new GameCreatedEvent(game.id));

        return game;
    }

    /**
     * Accept a game invitation
     */
    public Game acceptGame(String gameId, String username) throws IOException {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            throw new IllegalArgumentException("Game not found");
        }

        Game game = gameOpt.get();

        // Validate user is a player
        if (!game.isPlayer(username)) {
            throw new IllegalArgumentException("You are not a player in this game");
        }

        // Validate game is pending
        if (!"pending".equals(game.status)) {
            throw new IllegalArgumentException("Game is not pending");
        }

        // Validate user is not the creator
        if (username.equals(game.createdBy)) {
            throw new IllegalArgumentException("Cannot accept your own game invitation");
        }

        String oldStatus = game.status;
        game.status = "active";
        gameRepository.moveGameFile(game, oldStatus);

        LOG.info("Game {} accepted by {}", gameId, username);

        // Fire turn changed event (game just started, black's turn)
        turnChangedEvent.fire(new TurnChangedEvent(game.id, game.currentTurn));

        return game;
    }

    /**
     * Decline a game invitation
     */
    public void declineGame(String gameId, String username) throws IOException {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            throw new IllegalArgumentException("Game not found");
        }

        Game game = gameOpt.get();

        // Validate user is a player
        if (!game.isPlayer(username)) {
            throw new IllegalArgumentException("You are not a player in this game");
        }

        // Validate game is pending
        if (!"pending".equals(game.status)) {
            throw new IllegalArgumentException("Game is not pending");
        }

        // Delete the game
        gameRepository.delete(gameId);

        LOG.info("Game {} declined by {}", gameId, username);
    }

    /**
     * Make a move in a game
     */
    public MoveResponse makeMove(String gameId, String username, Position position) throws IOException {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            throw new IllegalArgumentException("Game not found");
        }

        Game game = gameOpt.get();

        // Validate game is active
        if (!"active".equals(game.status)) {
            throw new IllegalArgumentException("Game is not active");
        }

        // Validate user is a player
        if (!game.isPlayer(username)) {
            throw new IllegalArgumentException("You are not a player in this game");
        }

        // Validate it's user's turn
        if (!game.isPlayerTurn(username)) {
            throw new IllegalArgumentException("It's not your turn");
        }

        // Reconstruct board from moves
        Board board = reconstructBoard(game);

        // Get current board hash BEFORE the move (this becomes the Ko-blocked state for opponent)
        String boardHashBeforeMove = board.getBoardHash();

        // Validate and execute move
        Stone stone = Stone.fromString(game.currentTurn);
        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
                board, position, stone, game.koBlockedHash
        );

        if (!result.success) {
            return new MoveResponse(false, result.error);
        }

        // Create and add move to game
        Move move = new Move(game.currentTurn, "place", position);
        move.capturedStones = result.capturedStones;
        game.addMove(move);

        // Update Ko blocked hash - the opponent cannot recreate the board state from before this move
        game.koBlockedHash = boardHashBeforeMove;

        // Switch turn
        game.switchTurn();

        // Save game
        gameRepository.save(game);

        LOG.info("Move made in game {} by {} at {}", gameId, username, position);

        // Fire turn changed event
        turnChangedEvent.fire(new TurnChangedEvent(game.id, game.currentTurn));

        return new MoveResponse(true, result.capturedStones, game.currentTurn);
    }

    /**
     * Pass turn
     */
    public void pass(String gameId, String username) throws IOException {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            throw new IllegalArgumentException("Game not found");
        }

        Game game = gameOpt.get();

        // Validate game is active
        if (!"active".equals(game.status)) {
            throw new IllegalArgumentException("Game is not active");
        }

        // Validate user is a player and it's their turn
        if (!game.isPlayerTurn(username)) {
            throw new IllegalArgumentException("It's not your turn");
        }

        // Add pass move
        Move move = new Move(game.currentTurn, "pass");
        game.addMove(move);

        // Clear Ko restriction - passing releases the Ko threat
        game.koBlockedHash = null;

        // Switch turn if game is still active
        boolean turnSwitched = false;
        if ("active".equals(game.status)) {
            game.switchTurn();
            turnSwitched = true;
        }

        // Save game (might have changed to completed if 2 passes)
        String oldStatus = "active";
        if ("completed".equals(game.status)) {
            // Calculate final score
            Board board = reconstructBoard(game);
            ScoringEngine.ScoringResult scoringResult = scoringEngine.calculateScore(
                    board, game.moves, game.komi
            );

            // Set game result
            game.result = new GameResult();
            game.result.winner = scoringResult.winner;
            game.result.method = "score";
            game.result.score = new GameResult.Score(scoringResult.blackScore, scoringResult.whiteScore);
            game.result.territory = new GameResult.Territory(
                    scoringResult.blackTerritoryPositions,
                    scoringResult.whiteTerritoryPositions
            );
            game.result.captures = new GameResult.Captures(
                    scoringResult.blackPrisoners,
                    scoringResult.whitePrisoners
            );
            game.result.deadStones = new GameResult.DeadStones(
                    scoringResult.blackDeadStonePositions,
                    scoringResult.whiteDeadStonePositions
            );

            // Update user statistics
            String winnerUsername = "black".equals(scoringResult.winner) ? game.blackPlayer : game.whitePlayer;
            String loserUsername = "black".equals(scoringResult.winner) ? game.whitePlayer : game.blackPlayer;
            userService.updateStatistics(winnerUsername, true);
            userService.updateStatistics(loserUsername, false);

            gameRepository.moveGameFile(game, oldStatus);
            LOG.info("Game {} completed by scoring: Black={}, White={}, Winner={}",
                    gameId, scoringResult.blackScore, scoringResult.whiteScore, scoringResult.winner);
        } else {
            gameRepository.save(game);
        }

        LOG.info("Pass in game {} by {} (passes: {})", gameId, username, game.passes);

        // Fire turn changed event if turn switched
        if (turnSwitched) {
            turnChangedEvent.fire(new TurnChangedEvent(game.id, game.currentTurn));
        }
    }

    /**
     * Resign from game
     */
    public GameResult resign(String gameId, String username) throws IOException {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            throw new IllegalArgumentException("Game not found");
        }

        Game game = gameOpt.get();

        // Validate game is active
        if (!"active".equals(game.status)) {
            throw new IllegalArgumentException("Game is not active");
        }

        // Validate user is a player
        if (!game.isPlayer(username)) {
            throw new IllegalArgumentException("You are not a player in this game");
        }

        // Determine winner
        String playerColor = game.getPlayerColor(username);
        String winner = "black".equals(playerColor) ? "white" : "black";

        // Add resign move
        Move move = new Move(playerColor, "resign");
        game.addMove(move);

        // Update user statistics
        String winnerUsername = "black".equals(winner) ? game.blackPlayer : game.whitePlayer;
        userService.updateStatistics(winnerUsername, true);
        userService.updateStatistics(username, false);

        // Move game to completed
        gameRepository.moveGameFile(game, "active");

        LOG.info("Game {}: {} resigned, {} wins", gameId, username, winnerUsername);
        return game.result;
    }

    /**
     * Get game by ID
     */
    public Optional<Game> getGame(String gameId) {
        return gameRepository.findById(gameId);
    }

    /**
     * Get games for a user
     */
    public List<Game> getUserGames(String username, String status) {
        if (status == null || "all".equals(status)) {
            return gameRepository.findAllByUser(username);
        }
        return gameRepository.findByUser(username, status);
    }

    /**
     * Get current board state for a game
     */
    public Board getCurrentBoardState(String gameId) {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            throw new IllegalArgumentException("Game not found");
        }

        Game game = gameOpt.get();
        return reconstructBoard(game);
    }

    /**
     * Reconstruct board from game moves
     */
    private Board reconstructBoard(Game game) {
        List<GoRulesEngine.MoveData> movesData = new ArrayList<>();
        for (Move move : game.moves) {
            if ("place".equals(move.action)) {
                movesData.add(new GoRulesEngine.MoveData(move.player, move.action, move.position));
            }
        }

        return rulesEngine.reconstructBoard(game.boardSize, movesData);
    }

    /**
     * Recalculate the score of a completed game with manually marked dead stones.
     */
    public GameResult recalculateScore(String gameId, String username, List<Position> manuallyMarkedDeadStones) throws IOException {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            throw new IllegalArgumentException("Game not found");
        }

        Game game = gameOpt.get();

        // Validate game is completed
        if (!"completed".equals(game.status)) {
            throw new IllegalArgumentException("Game is not completed");
        }

        // Validate user is a player
        if (!game.isPlayer(username)) {
            throw new IllegalArgumentException("You are not a player in this game");
        }

        // Reconstruct board from moves
        Board board = reconstructBoard(game);

        // Validate all marked positions contain actual stones
        for (Position pos : manuallyMarkedDeadStones) {
            if (board.getStone(pos) == null) {
                throw new IllegalArgumentException("Position " + pos + " does not contain a stone");
            }
        }

        // Calculate score with manual dead stones
        ScoringEngine.ScoringResult scoringResult = scoringEngine.calculateScoreWithManualDeadStones(
                board, game.moves, game.komi, manuallyMarkedDeadStones
        );

        // Update game result
        game.result = new GameResult();
        game.result.winner = scoringResult.winner;
        game.result.method = "score";
        game.result.score = new GameResult.Score(scoringResult.blackScore, scoringResult.whiteScore);
        game.result.territory = new GameResult.Territory(
                scoringResult.blackTerritoryPositions,
                scoringResult.whiteTerritoryPositions
        );
        game.result.captures = new GameResult.Captures(
                scoringResult.blackPrisoners,
                scoringResult.whitePrisoners
        );
        game.result.deadStones = new GameResult.DeadStones(
                scoringResult.blackDeadStonePositions,
                scoringResult.whiteDeadStonePositions
        );

        // Save game
        gameRepository.save(game);

        LOG.info("Score recalculated for game {} by {}: Black={}, White={}, Winner={}",
                gameId, username, scoringResult.blackScore, scoringResult.whiteScore, scoringResult.winner);

        return game.result;
    }

    /**
     * Calculate score for a fork (non-persisted game state).
     * This reconstructs the board from the provided moves and calculates the score.
     */
    public GameResult calculateForkScore(int boardSize, List<Move> moves, double komi, List<Position> manuallyMarkedDeadStones) {
        // Reconstruct board from moves
        List<GoRulesEngine.MoveData> movesData = new ArrayList<>();
        for (Move move : moves) {
            if ("place".equals(move.action)) {
                movesData.add(new GoRulesEngine.MoveData(move.player, move.action, move.position));
            }
        }

        Board board = rulesEngine.reconstructBoard(boardSize, movesData);

        // Validate all marked positions contain actual stones
        for (Position pos : manuallyMarkedDeadStones) {
            if (board.getStone(pos) == null) {
                throw new IllegalArgumentException("Position " + pos + " does not contain a stone");
            }
        }

        // Calculate score
        ScoringEngine.ScoringResult scoringResult;
        if (manuallyMarkedDeadStones.isEmpty()) {
            // Use automatic dead stone detection
            scoringResult = scoringEngine.calculateScore(board, moves, komi);
        } else {
            // Use manually marked dead stones
            scoringResult = scoringEngine.calculateScoreWithManualDeadStones(board, moves, komi, manuallyMarkedDeadStones);
        }

        // Create and return game result
        GameResult result = new GameResult();
        result.winner = scoringResult.winner;
        result.method = "score";
        result.score = new GameResult.Score(scoringResult.blackScore, scoringResult.whiteScore);
        result.territory = new GameResult.Territory(
                scoringResult.blackTerritoryPositions,
                scoringResult.whiteTerritoryPositions
        );
        result.captures = new GameResult.Captures(
                scoringResult.blackPrisoners,
                scoringResult.whitePrisoners
        );
        result.deadStones = new GameResult.DeadStones(
                scoringResult.blackDeadStonePositions,
                scoringResult.whiteDeadStonePositions
        );

        LOG.info("Fork score calculated: boardSize={}, moves={}, Black={}, White={}, Winner={}",
                boardSize, moves.size(), scoringResult.blackScore, scoringResult.whiteScore, scoringResult.winner);

        return result;
    }

    /**
     * Response object for move operations
     */
    public static class MoveResponse {
        public boolean success;
        public String error;
        public List<Position> capturedStones;
        public String currentTurn;

        public MoveResponse(boolean success, String error) {
            this.success = success;
            this.error = error;
            this.capturedStones = new ArrayList<>();
        }

        public MoveResponse(boolean success, List<Position> capturedStones, String currentTurn) {
            this.success = success;
            this.capturedStones = capturedStones;
            this.currentTurn = currentTurn;
        }
    }
}
