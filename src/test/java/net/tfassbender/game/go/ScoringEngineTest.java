package net.tfassbender.game.go;

import net.tfassbender.game.game.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ScoringEngine class.
 */
public class ScoringEngineTest {

    private ScoringEngine scoringEngine;
    private Board board;

    @BeforeEach
    void setUp() {
        scoringEngine = new ScoringEngine();
        board = new Board(9);
    }

    // ===== Empty Board Tests =====

    @Test
    void testEmptyBoardHasNoTerritory() {
        List<ScoringEngine.TerritoryRegion> territories = scoringEngine.calculateTerritories(board);

        // Empty board should have exactly one territory (all neutral)
        assertEquals(1, territories.size());
        ScoringEngine.TerritoryRegion region = territories.get(0);
        assertTrue(region.isNeutral(), "Empty board should be neutral territory");
        assertEquals(81, region.size(), "Empty 9x9 board should have 81 empty positions");
    }

    @Test
    void testEmptyBoardScoring() {
        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        assertEquals(0, result.blackTerritory);
        assertEquals(0, result.whiteTerritory);
        assertEquals(0, result.blackPrisoners);
        assertEquals(0, result.whitePrisoners);
        assertEquals(5.5, result.komi);
        assertEquals(0.0, result.blackScore);
        assertEquals(5.5, result.whiteScore);
        assertEquals("white", result.winner, "White should win on empty board due to komi");
    }

    // ===== Simple Territory Tests =====

    @Test
    void testSimpleBlackTerritory() {
        // Black surrounds a corner
        //   0 1 2
        // 0 . B .
        // 1 B . .
        // 2 . . .
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);

        List<ScoringEngine.TerritoryRegion> territories = scoringEngine.calculateTerritories(board);

        // Find the corner territory
        ScoringEngine.TerritoryRegion cornerTerritory = null;
        for (ScoringEngine.TerritoryRegion t : territories) {
            if (t.positions.contains(new Position(0, 0))) {
                cornerTerritory = t;
                break;
            }
        }

        assertNotNull(cornerTerritory);
        assertEquals(Stone.BLACK, cornerTerritory.owner);
        assertEquals(1, cornerTerritory.size());
    }

    @Test
    void testSimpleWhiteTerritory() {
        // White surrounds a corner
        board.setStone(1, 0, Stone.WHITE);
        board.setStone(0, 1, Stone.WHITE);

        List<ScoringEngine.TerritoryRegion> territories = scoringEngine.calculateTerritories(board);

        ScoringEngine.TerritoryRegion cornerTerritory = null;
        for (ScoringEngine.TerritoryRegion t : territories) {
            if (t.positions.contains(new Position(0, 0))) {
                cornerTerritory = t;
                break;
            }
        }

        assertNotNull(cornerTerritory);
        assertEquals(Stone.WHITE, cornerTerritory.owner);
        assertEquals(1, cornerTerritory.size());
    }

    @Test
    void testNeutralTerritoryBorderedByBothColors() {
        // Both colors border the same region
        //   0 1 2
        // 0 . B W
        // 1 . . .
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(2, 0, Stone.WHITE);

        List<ScoringEngine.TerritoryRegion> territories = scoringEngine.calculateTerritories(board);

        // There should be one neutral territory containing (0,0)
        ScoringEngine.TerritoryRegion mainRegion = null;
        for (ScoringEngine.TerritoryRegion t : territories) {
            if (t.positions.contains(new Position(0, 0))) {
                mainRegion = t;
                break;
            }
        }

        assertNotNull(mainRegion);
        assertTrue(mainRegion.isNeutral(), "Region bordered by both colors should be neutral");
    }

    // ===== Corner and Edge Territory Tests =====

    @Test
    void testCornerTerritoryWithTwoStones() {
        // Black territory in corner, White controls the rest
        //   0 1 2 3
        // 0 . . B W
        // 1 . . B W
        // 2 B B B W
        // 3 W W W W
        board.setStone(2, 0, Stone.BLACK);
        board.setStone(2, 1, Stone.BLACK);
        board.setStone(0, 2, Stone.BLACK);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(2, 2, Stone.BLACK);
        // White wall to separate territories
        board.setStone(3, 0, Stone.WHITE);
        board.setStone(3, 1, Stone.WHITE);
        board.setStone(3, 2, Stone.WHITE);
        board.setStone(0, 3, Stone.WHITE);
        board.setStone(1, 3, Stone.WHITE);
        board.setStone(2, 3, Stone.WHITE);
        board.setStone(3, 3, Stone.WHITE);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        // Black has 4 territory points in the corner (0,0), (1,0), (0,1), (1,1)
        assertEquals(4, result.blackTerritory);
    }

    @Test
    void testEdgeTerritory() {
        // Black controls edge territory
        //   0 1 2 3 4 5 6 7 8
        // 0 . . . B . . . . .
        // 1 B B B . . . . . .
        board.setStone(3, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(1, 1, Stone.BLACK);
        board.setStone(2, 1, Stone.BLACK);

        List<ScoringEngine.TerritoryRegion> territories = scoringEngine.calculateTerritories(board);

        ScoringEngine.TerritoryRegion edgeTerritory = null;
        for (ScoringEngine.TerritoryRegion t : territories) {
            if (t.positions.contains(new Position(0, 0))) {
                edgeTerritory = t;
                break;
            }
        }

        assertNotNull(edgeTerritory);
        assertEquals(Stone.BLACK, edgeTerritory.owner);
        assertEquals(3, edgeTerritory.size()); // (0,0), (1,0), (2,0)
    }

    // ===== Multiple Territory Tests =====

    @Test
    void testMultipleSeparateTerritories() {
        // Black has two separate territories in opposite corners
        // White controls the middle to separate them
        // Top-left corner for black
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(1, 1, Stone.BLACK);
        // Bottom-right corner for black
        board.setStone(7, 8, Stone.BLACK);
        board.setStone(8, 7, Stone.BLACK);
        board.setStone(7, 7, Stone.BLACK);
        // White controls the middle area
        board.setStone(2, 0, Stone.WHITE);
        board.setStone(2, 1, Stone.WHITE);
        board.setStone(0, 2, Stone.WHITE);
        board.setStone(1, 2, Stone.WHITE);
        board.setStone(2, 2, Stone.WHITE);
        board.setStone(6, 8, Stone.WHITE);
        board.setStone(6, 7, Stone.WHITE);
        board.setStone(8, 6, Stone.WHITE);
        board.setStone(7, 6, Stone.WHITE);
        board.setStone(6, 6, Stone.WHITE);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        // Black should have 2 territory points (one in each corner)
        assertEquals(2, result.blackTerritory);
    }

    @Test
    void testBothPlayersHaveTerritory() {
        // Black controls top-left, White controls top-right
        // Top-left for Black
        board.setStone(2, 0, Stone.BLACK);
        board.setStone(2, 1, Stone.BLACK);
        board.setStone(0, 2, Stone.BLACK);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(2, 2, Stone.BLACK);
        // Top-right for White
        board.setStone(6, 0, Stone.WHITE);
        board.setStone(6, 1, Stone.WHITE);
        board.setStone(6, 2, Stone.WHITE);
        board.setStone(7, 2, Stone.WHITE);
        board.setStone(8, 2, Stone.WHITE);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        assertEquals(4, result.blackTerritory); // (0,0), (1,0), (0,1), (1,1)
        assertEquals(4, result.whiteTerritory); // (7,0), (8,0), (7,1), (8,1)
    }

    // ===== Prisoner Counting Tests =====

    @Test
    void testPrisonerCountingFromMoves() {
        List<Move> moves = new ArrayList<>();

        // Black captures 2 white stones
        Move blackMove1 = new Move("black", "place", new Position(4, 4));
        blackMove1.capturedStones = List.of(new Position(3, 3));
        moves.add(blackMove1);

        Move blackMove2 = new Move("black", "place", new Position(5, 5));
        blackMove2.capturedStones = List.of(new Position(6, 6), new Position(7, 7));
        moves.add(blackMove2);

        // White captures 1 black stone
        Move whiteMove = new Move("white", "place", new Position(1, 1));
        whiteMove.capturedStones = List.of(new Position(0, 0));
        moves.add(whiteMove);

        int[] prisoners = scoringEngine.countPrisoners(moves);

        assertEquals(3, prisoners[0], "Black should have captured 3 prisoners");
        assertEquals(1, prisoners[1], "White should have captured 1 prisoner");
    }

    @Test
    void testPrisonerCountingEmptyMoves() {
        int[] prisoners = scoringEngine.countPrisoners(new ArrayList<>());

        assertEquals(0, prisoners[0]);
        assertEquals(0, prisoners[1]);
    }

    @Test
    void testPrisonersAffectScore() {
        List<Move> moves = new ArrayList<>();

        // Black captures 5 white stones
        Move blackMove = new Move("black", "place", new Position(4, 4));
        blackMove.capturedStones = List.of(
                new Position(1, 1), new Position(1, 2), new Position(1, 3),
                new Position(1, 4), new Position(1, 5)
        );
        moves.add(blackMove);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, moves);

        assertEquals(5, result.blackPrisoners);
        assertEquals(5.0, result.blackScore); // Just prisoners, no territory
        assertEquals(5.5, result.whiteScore); // Just komi
    }

    // ===== Komi Tests =====

    @Test
    void testKomiDecidesTieBreaker() {
        // Set up equal territory
        // Black corner
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        // White corner
        board.setStone(7, 8, Stone.WHITE);
        board.setStone(8, 7, Stone.WHITE);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        // Both have 1 territory
        assertEquals(1, result.blackTerritory);
        assertEquals(1, result.whiteTerritory);
        // White wins due to komi
        assertEquals(1.0, result.blackScore);
        assertEquals(6.5, result.whiteScore); // 1 + 5.5
        assertEquals("white", result.winner);
    }

    @Test
    void testCustomKomi() {
        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>(), 7.5);

        assertEquals(7.5, result.komi);
        assertEquals(7.5, result.whiteScore);
    }

    @Test
    void testBlackWinsWithEnoughTerritory() {
        // Black has large territory that overcomes komi
        // Create a black wall that encloses left side
        // A vertical wall from top to bottom creates two separate regions
        for (int y = 0; y < 9; y++) {
            board.setStone(3, y, Stone.BLACK);
        }

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        // A single vertical wall creates:
        // - Left region (columns 0-2) = 27 points, bordered only by BLACK -> BLACK territory
        // - Right region (columns 4-8) = 45 points, bordered only by BLACK -> BLACK territory
        // Total black territory = 72
        assertEquals(72, result.blackTerritory);
        assertTrue(result.blackScore > result.whiteScore, "Black should win with 72 territory");
        assertEquals("black", result.winner);
    }

    // ===== Dead Stones Tests =====

    @Test
    void testDeadStoneTerritoryAndPrisonerPoints() {
        // This test verifies that dead stone positions count as territory
        // Uses the same board setup as testSingleDeadStoneInTerritory
        // but with explicit assertions for the fixed behavior
        //
        //   0 1 2 3 4 5
        // 0 . . . . B W
        // 1 . W . . B W   <- Dead white stone at (1,1)
        // 2 . . . . B W
        // 3 . . . . B W
        // 4 B B B B B W
        // 5 W W W W W W
        //
        // Black territory: 4x4 = 16 positions (including dead stone at (1,1))
        board.setStone(4, 0, Stone.BLACK);
        board.setStone(4, 1, Stone.BLACK);
        board.setStone(4, 2, Stone.BLACK);
        board.setStone(4, 3, Stone.BLACK);
        board.setStone(0, 4, Stone.BLACK);
        board.setStone(1, 4, Stone.BLACK);
        board.setStone(2, 4, Stone.BLACK);
        board.setStone(3, 4, Stone.BLACK);
        board.setStone(4, 4, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE); // Dead white stone inside black territory
        // White wall to separate
        board.setStone(5, 0, Stone.WHITE);
        board.setStone(5, 1, Stone.WHITE);
        board.setStone(5, 2, Stone.WHITE);
        board.setStone(5, 3, Stone.WHITE);
        board.setStone(5, 4, Stone.WHITE);
        board.setStone(0, 5, Stone.WHITE);
        board.setStone(1, 5, Stone.WHITE);
        board.setStone(2, 5, Stone.WHITE);
        board.setStone(3, 5, Stone.WHITE);
        board.setStone(4, 5, Stone.WHITE);
        board.setStone(5, 5, Stone.WHITE);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        // After fix: Black territory = 16 (4x4 including dead stone position)
        // Dead white stones = 1
        // Black score = 16 territory + 1 dead white stone = 17
        assertEquals(16, result.blackTerritory, "Dead stone position should count as territory");
        assertEquals(1, result.whiteDeadStones, "Should detect 1 white dead stone");
        assertEquals(17.0, result.blackScore, "Score should include both territory and prisoner points");
    }

    @Test
    void testSingleDeadStoneInTerritory() {
        // White stone completely inside black territory
        // Black controls corner, white controls rest of board
        //   0 1 2 3 4 5
        // 0 . . . . B W
        // 1 . W . . B W
        // 2 . . . . B W
        // 3 . . . . B W
        // 4 B B B B B W
        // 5 W W W W W W
        board.setStone(4, 0, Stone.BLACK);
        board.setStone(4, 1, Stone.BLACK);
        board.setStone(4, 2, Stone.BLACK);
        board.setStone(4, 3, Stone.BLACK);
        board.setStone(0, 4, Stone.BLACK);
        board.setStone(1, 4, Stone.BLACK);
        board.setStone(2, 4, Stone.BLACK);
        board.setStone(3, 4, Stone.BLACK);
        board.setStone(4, 4, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE); // Dead white stone inside black territory
        // White wall to separate
        board.setStone(5, 0, Stone.WHITE);
        board.setStone(5, 1, Stone.WHITE);
        board.setStone(5, 2, Stone.WHITE);
        board.setStone(5, 3, Stone.WHITE);
        board.setStone(5, 4, Stone.WHITE);
        board.setStone(0, 5, Stone.WHITE);
        board.setStone(1, 5, Stone.WHITE);
        board.setStone(2, 5, Stone.WHITE);
        board.setStone(3, 5, Stone.WHITE);
        board.setStone(4, 5, Stone.WHITE);
        board.setStone(5, 5, Stone.WHITE);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        // Black territory = 16 positions (4x4 corner, including dead stone position)
        // In Japanese scoring, dead stone positions become territory after removal
        assertEquals(16, result.blackTerritory);
        // White dead stones = 1
        assertEquals(1, result.whiteDeadStones);
        // Black score = 16 territory + 1 (dead white stone as prisoner) = 17
        assertEquals(17.0, result.blackScore);
    }

    @Test
    void testDeadGroupInsideTerritory() {
        // Two connected white stones inside black territory
        // Black controls corner, white controls rest
        //   0 1 2 3 4 5
        // 0 . . . . B W
        // 1 . W W . B W
        // 2 . . . . B W
        // 3 . . . . B W
        // 4 B B B B B W
        // 5 W W W W W W
        board.setStone(4, 0, Stone.BLACK);
        board.setStone(4, 1, Stone.BLACK);
        board.setStone(4, 2, Stone.BLACK);
        board.setStone(4, 3, Stone.BLACK);
        board.setStone(0, 4, Stone.BLACK);
        board.setStone(1, 4, Stone.BLACK);
        board.setStone(2, 4, Stone.BLACK);
        board.setStone(3, 4, Stone.BLACK);
        board.setStone(4, 4, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE); // Dead white group
        board.setStone(2, 1, Stone.WHITE);
        // White wall to separate
        board.setStone(5, 0, Stone.WHITE);
        board.setStone(5, 1, Stone.WHITE);
        board.setStone(5, 2, Stone.WHITE);
        board.setStone(5, 3, Stone.WHITE);
        board.setStone(5, 4, Stone.WHITE);
        board.setStone(0, 5, Stone.WHITE);
        board.setStone(1, 5, Stone.WHITE);
        board.setStone(2, 5, Stone.WHITE);
        board.setStone(3, 5, Stone.WHITE);
        board.setStone(4, 5, Stone.WHITE);
        board.setStone(5, 5, Stone.WHITE);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        // 16 positions in corner (including 2 dead stone positions)
        // In Japanese scoring, dead stone positions become territory after removal
        assertEquals(16, result.blackTerritory);
        assertEquals(2, result.whiteDeadStones);
        // Black score = 16 territory + 2 dead white stones as prisoners = 18
        assertEquals(18.0, result.blackScore);
    }

    @Test
    void testLivingGroupWithEyes() {
        // White group with two eyes is alive
        //   0 1 2 3 4 5
        // 0 . . . . . B
        // 1 . W . W . B
        // 2 . W W W . B
        // 3 . . . . . B
        // 4 B B B B B B
        // White has a living shape with two eyes at (0,1) and (3,1)
        // Actually, that's not two eyes - let me make a proper two-eye shape

        // Simpler setup: white group that connects to dame (not dead)
        //   0 1 2 3 4
        // 0 B B B B .
        // 1 B W W W .  <- White connects to outside (dame)
        // 2 B B B . .
        board.setStone(0, 0, Stone.BLACK);
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(2, 0, Stone.BLACK);
        board.setStone(3, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(0, 2, Stone.BLACK);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(2, 2, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE);
        board.setStone(2, 1, Stone.WHITE);
        board.setStone(3, 1, Stone.WHITE);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        // White group has liberty at (4,1) which is in neutral territory (dame)
        // So the white group is NOT dead
        assertEquals(0, result.whiteDeadStones, "White group with liberties in dame should not be dead");
    }

    @Test
    void testGroupWithLibertiesInOwnTerritory() {
        // Black group with liberties inside own territory (alive)
        //   0 1 2 3 4
        // 0 . B . . W
        // 1 B . B . W
        // 2 . B . . W
        // 3 . . . . W
        // 4 W W W W W
        // Black has territory at (0,0) and (0,1)(0,2) - its own liberties
        board.setStone(1, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.BLACK);
        board.setStone(2, 1, Stone.BLACK);
        board.setStone(1, 2, Stone.BLACK);
        board.setStone(4, 0, Stone.WHITE);
        board.setStone(4, 1, Stone.WHITE);
        board.setStone(4, 2, Stone.WHITE);
        board.setStone(4, 3, Stone.WHITE);
        board.setStone(0, 4, Stone.WHITE);
        board.setStone(1, 4, Stone.WHITE);
        board.setStone(2, 4, Stone.WHITE);
        board.setStone(3, 4, Stone.WHITE);
        board.setStone(4, 4, Stone.WHITE);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        assertEquals(0, result.blackDeadStones, "Black group with liberties in own territory should be alive");
    }

    // ===== Complex Scoring Scenarios =====

    @Test
    void testComplexGameScoring() {
        // Set up a complex end-game position
        // Black controls left side, White controls right side
        for (int y = 0; y < 9; y++) {
            board.setStone(4, y, Stone.BLACK);
            board.setStone(5, y, Stone.WHITE);
        }

        // Add some captured stones during the game
        List<Move> moves = new ArrayList<>();
        Move blackCapture = new Move("black", "place", new Position(4, 4));
        blackCapture.capturedStones = List.of(new Position(5, 5), new Position(5, 6));
        moves.add(blackCapture);

        Move whiteCapture = new Move("white", "place", new Position(5, 5));
        whiteCapture.capturedStones = List.of(new Position(4, 5));
        moves.add(whiteCapture);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, moves);

        // Black territory: left 4 columns = 36 (4*9)
        assertEquals(36, result.blackTerritory);
        // White territory: right 3 columns = 27 (3*9)
        assertEquals(27, result.whiteTerritory);
        // Prisoners
        assertEquals(2, result.blackPrisoners);
        assertEquals(1, result.whitePrisoners);
        // Scores
        assertEquals(38.0, result.blackScore); // 36 + 2
        assertEquals(33.5, result.whiteScore); // 27 + 1 + 5.5
        assertEquals("black", result.winner);
    }

    @Test
    void testCloseGameWithKomiDeciding() {
        // Set up a close game where komi decides
        for (int y = 0; y < 9; y++) {
            board.setStone(4, y, Stone.BLACK);
        }

        // Black has 36 territory (left side), white has 36 (right side) if we add stones
        // But we only have black wall, so white territory connects to edge
        // Let's make it simpler

        board = new Board(9);
        // Black controls 40 points, White controls 35 points
        // After komi, white has 40.5 and black has 40

        // Actually let's set up exactly equal territory
        // Use a 9x9 board split in half
        for (int y = 0; y < 9; y++) {
            board.setStone(4, y, Stone.BLACK);
            board.setStone(5, y, Stone.WHITE);
        }
        // Now: Black = 36 territory, White = 27 territory + 5.5 komi = 32.5
        // Black wins

        // For komi to decide, we need very close scores
        // Let's use prisoners to balance
        List<Move> moves = new ArrayList<>();
        Move whiteCapture = new Move("white", "place", new Position(5, 5));
        whiteCapture.capturedStones = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            whiteCapture.capturedStones.add(new Position(i, i));
        }
        moves.add(whiteCapture);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, moves);

        // Black: 36 territory + 0 prisoners = 36
        // White: 27 territory + 4 prisoners + 5.5 komi = 36.5
        assertEquals(36.0, result.blackScore);
        assertEquals(36.5, result.whiteScore);
        assertEquals("white", result.winner);
    }

    // ===== Board.getLiberties helper test =====

    @Test
    void testGetLibertiesOfGroup() {
        // Set up a group and verify liberty counting
        board.setStone(4, 4, Stone.BLACK);
        board.setStone(4, 5, Stone.BLACK);

        Set<Position> group = board.getGroup(new Position(4, 4));
        int liberties = board.countLiberties(group);

        // Group of 2 stones should have 6 liberties
        assertEquals(6, liberties);
    }

    // ===== Default Komi Test =====

    @Test
    void testDefaultKomi() {
        assertEquals(5.5, ScoringEngine.DEFAULT_KOMI);

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());
        assertEquals(5.5, result.komi);
    }

    // ===== Edge Case: All One Color =====

    @Test
    void testOneColorDominatesBoard() {
        // Black completely dominates - place stones in a line
        for (int y = 0; y < 9; y++) {
            board.setStone(8, y, Stone.BLACK);
        }

        ScoringEngine.ScoringResult result = scoringEngine.calculateScore(board, new ArrayList<>());

        // Black should have 72 territory (8 columns x 9 rows)
        assertEquals(72, result.blackTerritory);
        assertEquals(0, result.whiteTerritory);
        assertEquals(72.0, result.blackScore);
        assertEquals(5.5, result.whiteScore);
        assertEquals("black", result.winner);
    }
}
