package net.tfassbender.game.go;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for the GoRulesEngine class.
 */
public class GoRulesEngineTest {

    private GoRulesEngine rulesEngine;
    private Board board;

    @BeforeEach
    void setUp() {
        rulesEngine = new GoRulesEngine();
        board = new Board(9);
    }

    // ===== Basic Move Validation Tests =====

    @Test
    void testValidMoveOnEmptyBoard() {
        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(4, 4), Stone.BLACK, null
        );

        assertTrue(result.success);
        assertTrue(result.capturedStones.isEmpty());
        assertEquals(Stone.BLACK, board.getStone(4, 4));
    }

    @Test
    void testMoveOutsideBoard() {
        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(-1, 0), Stone.BLACK, null
        );

        assertFalse(result.success);
        assertEquals("Position is outside the board", result.error);
    }

    @Test
    void testMoveOnOccupiedPosition() {
        board.setStone(4, 4, Stone.WHITE);

        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(4, 4), Stone.BLACK, null
        );

        assertFalse(result.success);
        assertEquals("Position already occupied", result.error);
    }

    // ===== Capture Detection Tests =====

    @Test
    void testCaptureSingleStone() {
        // Set up a white stone with one liberty
        //   0 1 2
        // 0 . B .
        // 1 B W .  <- White at (1,1) has only one liberty at (2,1)
        // 2 . B .
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE);

        // Black plays at (2,1) to capture
        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(2, 1), Stone.BLACK, null
        );

        assertTrue(result.success);
        assertEquals(1, result.capturedStones.size());
        assertTrue(result.capturedStones.contains(new Position(1, 1)));
        assertNull(board.getStone(1, 1), "Captured stone should be removed");
    }

    @Test
    void testCaptureMultipleStones() {
        // Set up two connected white stones
        //   0 1 2 3
        // 0 . B B .
        // 1 B W W B
        // 2 . B B .
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(2, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(3, 1, Stone.BLACK);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(2, 2, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE);
        board.setStone(2, 1, Stone.WHITE);

        // The white group has no liberties and should already be in atari
        // But we need to reduce it to 0 liberties first
        // Actually, the setup above already has the white group with 0 liberties
        // Let me fix this - I need the group to have one liberty first

        board = new Board(9);
        //   0 1 2 3
        // 0 . B B .
        // 1 B W W .  <- White group has one liberty at (3,1)
        // 2 . B B .
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(2, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(2, 2, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE);
        board.setStone(2, 1, Stone.WHITE);

        // Black plays at (3,1) to capture both white stones
        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(3, 1), Stone.BLACK, null
        );

        assertTrue(result.success);
        assertEquals(2, result.capturedStones.size());
        assertTrue(result.capturedStones.contains(new Position(1, 1)));
        assertTrue(result.capturedStones.contains(new Position(2, 1)));
    }

    @Test
    void testCaptureInCorner() {
        // Corner capture - white at (0,0) with black surrounding
        //   0 1
        // 0 W B
        // 1 B .
        board.setStone(0, 0, Stone.WHITE);
        board.setStone(1, 0, Stone.BLACK);

        // Black plays at (0,1) to capture
        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(0, 1), Stone.BLACK, null
        );

        assertTrue(result.success);
        assertEquals(1, result.capturedStones.size());
        assertTrue(result.capturedStones.contains(new Position(0, 0)));
    }

    @Test
    void testNoCaptureWhenLibertiesRemain() {
        // White stone with liberties remaining
        board.setStone(4, 4, Stone.WHITE);
        board.setStone(4, 3, Stone.BLACK);

        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(3, 4), Stone.BLACK, null
        );

        assertTrue(result.success);
        assertTrue(result.capturedStones.isEmpty());
        assertEquals(Stone.WHITE, board.getStone(4, 4), "White stone should not be captured");
    }

    // ===== Suicide Rule Tests =====

    @Test
    void testSuicideMoveNotAllowed() {
        // Set up a position where black would have no liberties
        //   0 1 2
        // 0 . W .
        // 1 W . W  <- Black playing at (1,1) would be suicide
        // 2 . W .
        board.setStone(1, 0, Stone.WHITE);
        board.setStone(0, 1, Stone.WHITE);
        board.setStone(2, 1, Stone.WHITE);
        board.setStone(1, 2, Stone.WHITE);

        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(1, 1), Stone.BLACK, null
        );

        assertFalse(result.success);
        assertEquals("Suicide move not allowed", result.error);
        assertNull(board.getStone(1, 1), "Suicide move should not place stone");
    }

    @Test
    void testSuicideAllowedIfCapturesOpponent() {
        // Set up a position where black fills last liberty but captures white
        //   0 1 2 3
        // 0 . B W .
        // 1 B W . W  <- Black at (2,1) captures white at (1,1) so it's not suicide
        // 2 . B W .
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(2, 0, Stone.WHITE);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE);
        board.setStone(3, 1, Stone.WHITE);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(2, 2, Stone.WHITE);

        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(2, 1), Stone.BLACK, null
        );

        assertTrue(result.success, "Move should be allowed because it captures");
        assertFalse(result.capturedStones.isEmpty());
    }

    @Test
    void testSuicideInCorner() {
        //   0 1
        // 0 . W
        // 1 W .
        // Black playing at (0,0) would be suicide
        board.setStone(1, 0, Stone.WHITE);
        board.setStone(0, 1, Stone.WHITE);

        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(0, 0), Stone.BLACK, null
        );

        assertFalse(result.success);
        assertEquals("Suicide move not allowed", result.error);
    }

    // ===== Board Reconstruction Tests =====

    @Test
    void testReconstructEmptyBoard() {
        Board reconstructed = rulesEngine.reconstructBoard(9, List.of());
        assertEquals(9, reconstructed.getSize());

        // Verify board is empty
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                assertTrue(reconstructed.isEmpty(x, y));
            }
        }
    }

    @Test
    void testReconstructWithMoves() {
        List<GoRulesEngine.MoveData> moves = Arrays.asList(
            new GoRulesEngine.MoveData("black", "place", new Position(4, 4)),
            new GoRulesEngine.MoveData("white", "place", new Position(3, 3)),
            new GoRulesEngine.MoveData("black", "place", new Position(5, 5))
        );

        Board reconstructed = rulesEngine.reconstructBoard(9, moves);

        assertEquals(Stone.BLACK, reconstructed.getStone(4, 4));
        assertEquals(Stone.WHITE, reconstructed.getStone(3, 3));
        assertEquals(Stone.BLACK, reconstructed.getStone(5, 5));
    }

    @Test
    void testReconstructWithCapture() {
        // Create moves that result in a capture
        //   0 1 2
        // 0 . B .
        // 1 B W B  <- Black captures White at (1,1)
        // 2 . B .
        List<GoRulesEngine.MoveData> moves = Arrays.asList(
            new GoRulesEngine.MoveData("black", "place", new Position(1, 0)),
            new GoRulesEngine.MoveData("white", "place", new Position(1, 1)),
            new GoRulesEngine.MoveData("black", "place", new Position(0, 1)),
            new GoRulesEngine.MoveData("white", "place", new Position(8, 8)), // White plays elsewhere
            new GoRulesEngine.MoveData("black", "place", new Position(1, 2)),
            new GoRulesEngine.MoveData("white", "place", new Position(7, 7)), // White plays elsewhere
            new GoRulesEngine.MoveData("black", "place", new Position(2, 1))  // Captures
        );

        Board reconstructed = rulesEngine.reconstructBoard(9, moves);

        assertNull(reconstructed.getStone(1, 1), "Captured stone should not be on board");
        assertEquals(Stone.BLACK, reconstructed.getStone(2, 1));
    }

    @Test
    void testReconstructWithPass() {
        List<GoRulesEngine.MoveData> moves = Arrays.asList(
            new GoRulesEngine.MoveData("black", "place", new Position(4, 4)),
            new GoRulesEngine.MoveData("white", "pass", null),
            new GoRulesEngine.MoveData("black", "place", new Position(5, 5))
        );

        Board reconstructed = rulesEngine.reconstructBoard(9, moves);

        assertEquals(Stone.BLACK, reconstructed.getStone(4, 4));
        assertEquals(Stone.BLACK, reconstructed.getStone(5, 5));
    }

    @Test
    void testReconstructInvalidMoveThrows() {
        // Try to place on occupied position
        List<GoRulesEngine.MoveData> moves = Arrays.asList(
            new GoRulesEngine.MoveData("black", "place", new Position(4, 4)),
            new GoRulesEngine.MoveData("white", "place", new Position(4, 4)) // Same position!
        );

        assertThrows(IllegalStateException.class, () -> {
            rulesEngine.reconstructBoard(9, moves);
        });
    }

    // ===== Complex Scenarios =====

    @Test
    void testSnapbackCapture() {
        // Snapback is a common Go pattern where capturing leads to immediate recapture
        // This tests that the rules engine handles complex captures correctly

        // Set up a snapback position
        //   0 1 2 3 4
        // 0 . B B B .
        // 1 B W W W B
        // 2 . B . B .
        //
        // If Black plays at (2,2), it captures the 3 white stones
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(2, 0, Stone.BLACK);
        board.setStone(3, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(4, 1, Stone.BLACK);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(3, 2, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE);
        board.setStone(2, 1, Stone.WHITE);
        board.setStone(3, 1, Stone.WHITE);

        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(2, 2), Stone.BLACK, null
        );

        assertTrue(result.success);
        assertEquals(3, result.capturedStones.size());
    }

    @Test
    void testLadderCapture() {
        // Simple ladder setup where a stone is captured step by step
        // Just testing that sequential captures work correctly
        board.setStone(2, 2, Stone.WHITE);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(2, 1, Stone.BLACK);

        // White has 2 liberties at (3,2) and (2,3)
        assertEquals(2, board.countLiberties(new Position(2, 2)));

        // Black plays at (3,2)
        GoRulesEngine.MoveResult result1 = rulesEngine.validateAndExecuteMove(
            board, new Position(3, 2), Stone.BLACK, null
        );
        assertTrue(result1.success);
        assertEquals(1, board.countLiberties(new Position(2, 2)));

        // Black plays at (2,3) to capture
        GoRulesEngine.MoveResult result2 = rulesEngine.validateAndExecuteMove(
            board, new Position(2, 3), Stone.BLACK, null
        );
        assertTrue(result2.success);
        assertEquals(1, result2.capturedStones.size());
        assertTrue(result2.capturedStones.contains(new Position(2, 2)));
    }

    @Test
    void testEyeFormation() {
        // Test that playing inside own eye is possible but usually unwise
        //   0 1 2
        // 0 B B B
        // 1 B . B  <- Black playing at (1,1) is valid but fills own eye
        // 2 B B B
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (x != 1 || y != 1) {
                    board.setStone(x, y, Stone.BLACK);
                }
            }
        }

        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(1, 1), Stone.BLACK, null
        );

        assertTrue(result.success, "Playing in own eye should be allowed (even if unwise)");
    }

    @Test
    void testCaptureBeforeSuicideCheck() {
        // The rules should check captures BEFORE suicide
        // So a move that would be suicide if no capture, but captures enemy, is valid
        //   0 1 2
        // 0 B W .
        // 1 W . .  <- Black at (1,1) captures W(1,0) and W(0,1)
        // 2 . . .
        board.setStone(0, 0, Stone.BLACK);
        board.setStone(1, 0, Stone.WHITE);
        board.setStone(0, 1, Stone.WHITE);

        // If Black plays at (1,1), it would have no liberties IF white stones remained
        // But Black captures both white stones first, so it gains liberties

        // Actually this setup doesn't quite work. Let me fix it:
        //   0 1 2
        // 0 . B .
        // 1 B W B  <- White at (1,1) surrounded
        // 2 . B .
        // Black playing at any empty adjacent to (1,1) should work fine

        board = new Board(9);
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(2, 1, Stone.BLACK);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE);

        // White is already captured... Let me try a different scenario
        // A scenario where black fills its last liberty but captures white

        board = new Board(9);
        //   0 1 2 3
        // 0 W B . .
        // 1 B . . .
        // Black at (0,0) has one liberty at (0,1) but let's surround it more
        //   0 1 2
        // 0 B W .
        // 1 W . .
        // If black plays at (1,1)... no that gives black a liberty

        // Let's do: snap-back style
        //   0 1 2
        // 0 . W B
        // 1 W B .  <- If white plays (0,0), black at (1,0) has no liberties BUT captures W(0,1)
        // 2 B . .
        board = new Board(9);
        board.setStone(1, 0, Stone.WHITE);
        board.setStone(2, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.WHITE);
        board.setStone(1, 1, Stone.BLACK);
        board.setStone(0, 2, Stone.BLACK);

        // Now Black plays at (0,0)
        // Black at (0,0) adjacent to W(1,0) and W(0,1)
        // W(1,0) has liberties? (0,0) and (2,0)->blocked by B, (1,1)->blocked by B = only (0,0)
        // W(0,1) has liberties? (0,0), (0,2)->blocked by B, (1,1)->blocked by B = only (0,0)
        // So both white stones have exactly 1 liberty at (0,0)
        // If Black plays (0,0), Black would have 0 liberties BUT captures both whites
        GoRulesEngine.MoveResult result = rulesEngine.validateAndExecuteMove(
            board, new Position(0, 0), Stone.BLACK, null
        );

        assertTrue(result.success, "Move should succeed because it captures opponent stones");
        assertEquals(2, result.capturedStones.size());
    }
}
