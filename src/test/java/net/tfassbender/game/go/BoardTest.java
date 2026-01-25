package net.tfassbender.game.go;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

/**
 * Tests for the Board class.
 */
public class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(9);
    }

    // ===== Board Creation Tests =====

    @Test
    void testBoardCreation9x9() {
        Board b = new Board(9);
        assertEquals(9, b.getSize());
    }

    @Test
    void testBoardCreation13x13() {
        Board b = new Board(13);
        assertEquals(13, b.getSize());
    }

    @Test
    void testBoardCreation19x19() {
        Board b = new Board(19);
        assertEquals(19, b.getSize());
    }

    @Test
    void testBoardCreationInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> new Board(10));
        assertThrows(IllegalArgumentException.class, () -> new Board(5));
        assertThrows(IllegalArgumentException.class, () -> new Board(0));
    }

    @Test
    void testBoardCopyConstructor() {
        board.setStone(0, 0, Stone.BLACK);
        board.setStone(1, 1, Stone.WHITE);

        Board copy = new Board(board);

        assertEquals(Stone.BLACK, copy.getStone(0, 0));
        assertEquals(Stone.WHITE, copy.getStone(1, 1));
        assertTrue(copy.isEmpty(2, 2));

        // Verify it's a deep copy
        board.setStone(2, 2, Stone.BLACK);
        assertTrue(copy.isEmpty(2, 2), "Copy should not be affected by changes to original");
    }

    // ===== Position Validation Tests =====

    @Test
    void testValidPositions() {
        assertTrue(board.isValidPosition(0, 0));
        assertTrue(board.isValidPosition(8, 8));
        assertTrue(board.isValidPosition(4, 4));
        assertTrue(board.isValidPosition(new Position(0, 0)));
    }

    @Test
    void testInvalidPositions() {
        assertFalse(board.isValidPosition(-1, 0));
        assertFalse(board.isValidPosition(0, -1));
        assertFalse(board.isValidPosition(9, 0));
        assertFalse(board.isValidPosition(0, 9));
        assertFalse(board.isValidPosition(new Position(-1, -1)));
    }

    // ===== Stone Placement Tests =====

    @Test
    void testSetAndGetStone() {
        board.setStone(3, 3, Stone.BLACK);
        assertEquals(Stone.BLACK, board.getStone(3, 3));

        board.setStone(new Position(4, 4), Stone.WHITE);
        assertEquals(Stone.WHITE, board.getStone(new Position(4, 4)));
    }

    @Test
    void testSetStoneInvalidPosition() {
        assertThrows(IllegalArgumentException.class, () -> board.setStone(-1, 0, Stone.BLACK));
        assertThrows(IllegalArgumentException.class, () -> board.setStone(9, 0, Stone.BLACK));
    }

    @Test
    void testGetStoneInvalidPosition() {
        assertNull(board.getStone(-1, 0));
        assertNull(board.getStone(9, 0));
    }

    @Test
    void testRemoveStone() {
        board.setStone(3, 3, Stone.BLACK);
        assertEquals(Stone.BLACK, board.getStone(3, 3));

        board.removeStone(3, 3);
        assertNull(board.getStone(3, 3));
        assertTrue(board.isEmpty(3, 3));
    }

    @Test
    void testIsEmpty() {
        assertTrue(board.isEmpty(0, 0));
        board.setStone(0, 0, Stone.BLACK);
        assertFalse(board.isEmpty(0, 0));
    }

    // ===== Adjacent Positions Tests =====

    @Test
    void testAdjacentPositionsCenter() {
        List<Position> adjacent = board.getAdjacentPositions(new Position(4, 4));
        assertEquals(4, adjacent.size());
        assertTrue(adjacent.contains(new Position(4, 3))); // up
        assertTrue(adjacent.contains(new Position(4, 5))); // down
        assertTrue(adjacent.contains(new Position(3, 4))); // left
        assertTrue(adjacent.contains(new Position(5, 4))); // right
    }

    @Test
    void testAdjacentPositionsCorner() {
        List<Position> adjacent = board.getAdjacentPositions(new Position(0, 0));
        assertEquals(2, adjacent.size());
        assertTrue(adjacent.contains(new Position(1, 0)));
        assertTrue(adjacent.contains(new Position(0, 1)));
    }

    @Test
    void testAdjacentPositionsEdge() {
        List<Position> adjacent = board.getAdjacentPositions(new Position(4, 0));
        assertEquals(3, adjacent.size());
        assertTrue(adjacent.contains(new Position(3, 0)));
        assertTrue(adjacent.contains(new Position(5, 0)));
        assertTrue(adjacent.contains(new Position(4, 1)));
    }

    // ===== Group Detection Tests =====

    @Test
    void testSingleStoneGroup() {
        board.setStone(4, 4, Stone.BLACK);
        Set<Position> group = board.getGroup(new Position(4, 4));
        assertEquals(1, group.size());
        assertTrue(group.contains(new Position(4, 4)));
    }

    @Test
    void testConnectedGroup() {
        // Create a connected group of 4 black stones
        board.setStone(4, 4, Stone.BLACK);
        board.setStone(4, 5, Stone.BLACK);
        board.setStone(5, 4, Stone.BLACK);
        board.setStone(5, 5, Stone.BLACK);

        Set<Position> group = board.getGroup(new Position(4, 4));
        assertEquals(4, group.size());
        assertTrue(group.contains(new Position(4, 4)));
        assertTrue(group.contains(new Position(4, 5)));
        assertTrue(group.contains(new Position(5, 4)));
        assertTrue(group.contains(new Position(5, 5)));
    }

    @Test
    void testGroupNotConnectedDiagonally() {
        // Diagonal stones are NOT connected
        board.setStone(4, 4, Stone.BLACK);
        board.setStone(5, 5, Stone.BLACK);

        Set<Position> group = board.getGroup(new Position(4, 4));
        assertEquals(1, group.size());
        assertFalse(group.contains(new Position(5, 5)));
    }

    @Test
    void testGroupsOfDifferentColors() {
        board.setStone(4, 4, Stone.BLACK);
        board.setStone(4, 5, Stone.WHITE);

        Set<Position> blackGroup = board.getGroup(new Position(4, 4));
        Set<Position> whiteGroup = board.getGroup(new Position(4, 5));

        assertEquals(1, blackGroup.size());
        assertEquals(1, whiteGroup.size());
        assertFalse(blackGroup.contains(new Position(4, 5)));
        assertFalse(whiteGroup.contains(new Position(4, 4)));
    }

    @Test
    void testGroupOfEmptyPosition() {
        Set<Position> group = board.getGroup(new Position(4, 4));
        assertTrue(group.isEmpty());
    }

    // ===== Liberty Counting Tests =====

    @Test
    void testLibertiesSingleStoneCenter() {
        board.setStone(4, 4, Stone.BLACK);
        assertEquals(4, board.countLiberties(new Position(4, 4)));
    }

    @Test
    void testLibertiesSingleStoneCorner() {
        board.setStone(0, 0, Stone.BLACK);
        assertEquals(2, board.countLiberties(new Position(0, 0)));
    }

    @Test
    void testLibertiesSingleStoneEdge() {
        board.setStone(4, 0, Stone.BLACK);
        assertEquals(3, board.countLiberties(new Position(4, 0)));
    }

    @Test
    void testLibertiesConnectedGroup() {
        // Two connected stones share some liberties
        board.setStone(4, 4, Stone.BLACK);
        board.setStone(4, 5, Stone.BLACK);
        // Group has 6 liberties (3 for each minus shared)
        assertEquals(6, board.countLiberties(new Position(4, 4)));
    }

    @Test
    void testLibertiesReducedByOpponent() {
        board.setStone(4, 4, Stone.BLACK);
        assertEquals(4, board.countLiberties(new Position(4, 4)));

        // Place opponent stone adjacent
        board.setStone(4, 3, Stone.WHITE);
        assertEquals(3, board.countLiberties(new Position(4, 4)));

        board.setStone(3, 4, Stone.WHITE);
        assertEquals(2, board.countLiberties(new Position(4, 4)));
    }

    @Test
    void testLibertiesZero() {
        // Surround a stone completely
        board.setStone(4, 4, Stone.BLACK);
        board.setStone(4, 3, Stone.WHITE);
        board.setStone(4, 5, Stone.WHITE);
        board.setStone(3, 4, Stone.WHITE);
        board.setStone(5, 4, Stone.WHITE);

        assertEquals(0, board.countLiberties(new Position(4, 4)));
    }

    // ===== Get Stones Of Color Tests =====

    @Test
    void testGetStonesOfColor() {
        board.setStone(0, 0, Stone.BLACK);
        board.setStone(1, 1, Stone.BLACK);
        board.setStone(2, 2, Stone.WHITE);

        List<Position> blackStones = board.getStonesOfColor(Stone.BLACK);
        List<Position> whiteStones = board.getStonesOfColor(Stone.WHITE);

        assertEquals(2, blackStones.size());
        assertEquals(1, whiteStones.size());
        assertTrue(blackStones.contains(new Position(0, 0)));
        assertTrue(blackStones.contains(new Position(1, 1)));
        assertTrue(whiteStones.contains(new Position(2, 2)));
    }

    // ===== Board Hash Tests =====

    @Test
    void testBoardHashEmpty() {
        String hash = board.getBoardHash();
        assertEquals(81, hash.length()); // 9x9 = 81
        assertTrue(hash.matches("\\.+"), "Empty board hash should be all dots");
    }

    @Test
    void testBoardHashWithStones() {
        board.setStone(0, 0, Stone.BLACK);
        board.setStone(0, 1, Stone.WHITE);

        String hash = board.getBoardHash();
        // Hash iterates x first, then y, so:
        // index 0 = (0,0), index 1 = (0,1), index 2 = (0,2), etc.
        assertEquals('B', hash.charAt(0)); // Black at (0,0)
        assertEquals('W', hash.charAt(1)); // White at (0,1)
    }

    @Test
    void testBoardHashDeterministic() {
        board.setStone(3, 3, Stone.BLACK);
        board.setStone(4, 4, Stone.WHITE);

        String hash1 = board.getBoardHash();
        String hash2 = board.getBoardHash();

        assertEquals(hash1, hash2, "Hash should be deterministic");
    }

    @Test
    void testBoardHashDifferentStates() {
        String emptyHash = board.getBoardHash();

        board.setStone(0, 0, Stone.BLACK);
        String withStoneHash = board.getBoardHash();

        assertNotEquals(emptyHash, withStoneHash);
    }
}
