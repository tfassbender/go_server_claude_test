package net.tfassbender.game.go;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Ko rule implementation.
 *
 * Ko occurs when a single stone capture could be immediately recaptured,
 * creating an infinite loop. The Ko rule prevents this by blocking the
 * immediate recapture.
 */
public class KoRuleTest {

    private GoRulesEngine rulesEngine;

    @BeforeEach
    void setUp() {
        rulesEngine = new GoRulesEngine();
    }

    /**
     * Test basic Ko detection.
     *
     * Setup (9x9 board, positions shown):
     *   0 1 2 3
     * 0 . B . .
     * 1 B W B .
     * 2 . B . .
     *
     * White stone at (1,1) has only one liberty at (2,1).
     * If Black plays at (2,1), White is captured.
     * Then if White plays at (1,1), Black at (2,1) would be captured.
     * This creates a Ko situation.
     */
    @Test
    void testKoRulePreventsImmediateRecapture() {
        Board board = new Board(9);

        // Set up Ko situation
        // Black stones surrounding position (1,1)
        board.setStone(new Position(1, 0), Stone.BLACK);  // Above
        board.setStone(new Position(0, 1), Stone.BLACK);  // Left
        board.setStone(new Position(2, 1), Stone.BLACK);  // Right (will be captured for Ko)
        board.setStone(new Position(1, 2), Stone.BLACK);  // Below

        // White stone at (1,1) - the Ko point
        board.setStone(new Position(1, 1), Stone.WHITE);

        // Additional white stone to make Black at (2,1) capturable
        board.setStone(new Position(3, 1), Stone.WHITE);

        // Black stone that will be captured in Ko
        // Actually, let me rethink this setup...

        // Better Ko setup:
        //   0 1 2 3
        // 0 . B W .
        // 1 B W . W
        // 2 . B W .
        //
        // Black plays at (2,1), captures White at (1,1)
        // White cannot immediately recapture at (1,1)

        board = new Board(9);
        board.setStone(new Position(1, 0), Stone.BLACK);
        board.setStone(new Position(2, 0), Stone.WHITE);
        board.setStone(new Position(0, 1), Stone.BLACK);
        board.setStone(new Position(1, 1), Stone.WHITE);  // This will be captured
        board.setStone(new Position(3, 1), Stone.WHITE);
        board.setStone(new Position(1, 2), Stone.BLACK);
        board.setStone(new Position(2, 2), Stone.WHITE);

        // Record the board state before Black's capturing move
        String hashBeforeBlackMove = board.getBoardHash();

        // Black captures at (2,1)
        GoRulesEngine.MoveResult blackMove = rulesEngine.validateAndExecuteMove(
            board, new Position(2, 1), Stone.BLACK, null
        );

        assertTrue(blackMove.success, "Black should be able to capture");
        assertEquals(1, blackMove.capturedStones.size(), "Should capture one white stone");
        assertEquals(new Position(1, 1), blackMove.capturedStones.get(0));

        // Now White tries to recapture immediately at (1,1)
        // This should be blocked by Ko rule
        GoRulesEngine.MoveResult whiteRecapture = rulesEngine.validateAndExecuteMove(
            board, new Position(1, 1), Stone.WHITE, hashBeforeBlackMove
        );

        assertFalse(whiteRecapture.success, "White should not be able to recapture due to Ko");
        assertEquals("Ko rule violation", whiteRecapture.error);
    }

    /**
     * Test that Ko restriction is lifted after a pass.
     */
    @Test
    void testKoAllowedAfterPass() {
        Board board = new Board(9);

        // Set up same Ko situation as above
        board.setStone(new Position(1, 0), Stone.BLACK);
        board.setStone(new Position(2, 0), Stone.WHITE);
        board.setStone(new Position(0, 1), Stone.BLACK);
        board.setStone(new Position(1, 1), Stone.WHITE);  // This will be captured
        board.setStone(new Position(3, 1), Stone.WHITE);
        board.setStone(new Position(1, 2), Stone.BLACK);
        board.setStone(new Position(2, 2), Stone.WHITE);

        String hashBeforeBlackMove = board.getBoardHash();

        // Black captures at (2,1)
        GoRulesEngine.MoveResult blackMove = rulesEngine.validateAndExecuteMove(
            board, new Position(2, 1), Stone.BLACK, null
        );
        assertTrue(blackMove.success);

        // Simulate a pass by clearing the Ko blocked hash
        // (In the actual game, pass sets koBlockedHash to null)
        String noKoRestriction = null;

        // Now White can recapture at (1,1) because Ko was released by the pass
        GoRulesEngine.MoveResult whiteRecapture = rulesEngine.validateAndExecuteMove(
            board, new Position(1, 1), Stone.WHITE, noKoRestriction
        );

        assertTrue(whiteRecapture.success, "White should be able to recapture after pass releases Ko");
    }

    /**
     * Test that normal moves (non-Ko) are not blocked.
     */
    @Test
    void testNormalMoveNotBlockedByKoCheck() {
        Board board = new Board(9);

        // Simple board with some stones
        board.setStone(new Position(0, 0), Stone.BLACK);
        board.setStone(new Position(1, 0), Stone.WHITE);

        String previousHash = board.getBoardHash();

        // Black plays at (0, 1) - a normal move
        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(0, 1), Stone.BLACK, previousHash
        );

        assertTrue(result.success, "Normal move should be allowed");
    }
}
