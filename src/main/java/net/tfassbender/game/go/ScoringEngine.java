package net.tfassbender.game.go;

import jakarta.enterprise.context.ApplicationScoped;
import net.tfassbender.game.game.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Scoring engine for Go games using Japanese-style rules (territory + prisoners + komi).
 */
@ApplicationScoped
public class ScoringEngine {

    private static final Logger LOG = LoggerFactory.getLogger(ScoringEngine.class);
    public static final double DEFAULT_KOMI = 5.5;

    /**
     * Represents a connected region of empty intersections (territory).
     */
    public static class TerritoryRegion {
        public final Set<Position> positions;
        public final Stone owner; // null if neutral (dame)

        public TerritoryRegion(Set<Position> positions, Stone owner) {
            this.positions = positions;
            this.owner = owner;
        }

        public int size() {
            return positions.size();
        }

        public boolean isNeutral() {
            return owner == null;
        }
    }

    /**
     * Represents the dead stones found on the board.
     */
    public static class DeadStones {
        public final Set<Position> blackDeadStones;
        public final Set<Position> whiteDeadStones;

        public DeadStones(Set<Position> blackDeadStones, Set<Position> whiteDeadStones) {
            this.blackDeadStones = blackDeadStones;
            this.whiteDeadStones = whiteDeadStones;
        }
    }

    /**
     * Result of scoring calculation.
     */
    public static class ScoringResult {
        public final double blackScore;
        public final double whiteScore;
        public final int blackTerritory;
        public final int whiteTerritory;
        public final int blackPrisoners;
        public final int whitePrisoners;
        public final int blackDeadStones;
        public final int whiteDeadStones;
        public final double komi;
        public final String winner;
        public final List<Position> blackTerritoryPositions;
        public final List<Position> whiteTerritoryPositions;
        public final List<Position> blackDeadStonePositions;
        public final List<Position> whiteDeadStonePositions;

        public ScoringResult(int blackTerritory, int whiteTerritory,
                            int blackPrisoners, int whitePrisoners,
                            int blackDeadStones, int whiteDeadStones,
                            double komi,
                            List<Position> blackTerritoryPositions,
                            List<Position> whiteTerritoryPositions,
                            List<Position> blackDeadStonePositions,
                            List<Position> whiteDeadStonePositions) {
            this.blackTerritory = blackTerritory;
            this.whiteTerritory = whiteTerritory;
            this.blackPrisoners = blackPrisoners;
            this.whitePrisoners = whitePrisoners;
            this.blackDeadStones = blackDeadStones;
            this.whiteDeadStones = whiteDeadStones;
            this.komi = komi;
            this.blackTerritoryPositions = blackTerritoryPositions;
            this.whiteTerritoryPositions = whiteTerritoryPositions;
            this.blackDeadStonePositions = blackDeadStonePositions;
            this.whiteDeadStonePositions = whiteDeadStonePositions;

            // Black total = territory + prisoners captured + dead white stones
            this.blackScore = blackTerritory + blackPrisoners + whiteDeadStones;
            // White total = territory + prisoners captured + dead black stones + komi
            this.whiteScore = whiteTerritory + whitePrisoners + blackDeadStones + komi;

            this.winner = blackScore > whiteScore ? "black" : "white";
        }
    }

    /**
     * Calculate all territory regions on the board.
     */
    public List<TerritoryRegion> calculateTerritories(Board board) {
        List<TerritoryRegion> territories = new ArrayList<>();
        Set<Position> visited = new HashSet<>();

        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                Position pos = new Position(x, y);
                if (!visited.contains(pos) && board.isEmpty(pos)) {
                    TerritoryRegion region = floodFillEmptyRegion(board, pos, visited);
                    territories.add(region);
                }
            }
        }

        return territories;
    }

    /**
     * Flood-fill to find a connected region of empty intersections
     * and determine its owner based on bordering stones.
     */
    private TerritoryRegion floodFillEmptyRegion(Board board, Position start, Set<Position> globalVisited) {
        Set<Position> region = new HashSet<>();
        Set<Stone> borderingColors = new HashSet<>();
        Queue<Position> queue = new LinkedList<>();

        queue.add(start);
        globalVisited.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            region.add(current);

            for (Position adjacent : board.getAdjacentPositions(current)) {
                Stone stone = board.getStone(adjacent);
                if (stone != null) {
                    // Found a bordering stone
                    borderingColors.add(stone);
                } else if (!globalVisited.contains(adjacent)) {
                    // Empty position we haven't visited
                    globalVisited.add(adjacent);
                    queue.add(adjacent);
                }
            }
        }

        // Determine owner
        Stone owner = null;
        if (borderingColors.size() == 1) {
            owner = borderingColors.iterator().next();
        }
        // If borderingColors has 0 or 2 colors, owner remains null (neutral)

        return new TerritoryRegion(region, owner);
    }

    /**
     * Find dead stone groups on the board.
     * A group is dead if all its liberties are within enemy territory.
     */
    public DeadStones findDeadStones(Board board, List<TerritoryRegion> territories) {
        Set<Position> blackDeadStones = new HashSet<>();
        Set<Position> whiteDeadStones = new HashSet<>();
        Set<Position> processedGroups = new HashSet<>();

        // Build a map of positions to their territory (if any)
        Map<Position, TerritoryRegion> positionToTerritory = new HashMap<>();
        for (TerritoryRegion territory : territories) {
            for (Position pos : territory.positions) {
                positionToTerritory.put(pos, territory);
            }
        }

        // Check each stone on the board
        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                Position pos = new Position(x, y);
                Stone stone = board.getStone(pos);
                if (stone != null && !processedGroups.contains(pos)) {
                    Set<Position> group = board.getGroup(pos);
                    processedGroups.addAll(group);

                    if (isGroupDead(board, group, stone, positionToTerritory)) {
                        if (stone == Stone.BLACK) {
                            blackDeadStones.addAll(group);
                        } else {
                            whiteDeadStones.addAll(group);
                        }
                    }
                }
            }
        }

        return new DeadStones(blackDeadStones, whiteDeadStones);
    }

    /**
     * Check if a group is dead.
     * A group is dead if ALL of its liberties are within enemy territory.
     */
    private boolean isGroupDead(Board board, Set<Position> group, Stone groupColor,
                               Map<Position, TerritoryRegion> positionToTerritory) {
        Stone enemyColor = groupColor.opposite();

        // Get all liberties of the group
        Set<Position> liberties = new HashSet<>();
        for (Position pos : group) {
            for (Position adjacent : board.getAdjacentPositions(pos)) {
                if (board.isEmpty(adjacent)) {
                    liberties.add(adjacent);
                }
            }
        }

        // If the group has no liberties, it should already be captured
        // (this shouldn't happen in a valid end-game position)
        if (liberties.isEmpty()) {
            return false;
        }

        // Check if ALL liberties are in enemy territory
        for (Position liberty : liberties) {
            TerritoryRegion territory = positionToTerritory.get(liberty);
            if (territory == null) {
                // Liberty is not part of any territory calculation (shouldn't happen)
                return false;
            }
            if (territory.isNeutral() || territory.owner != enemyColor) {
                // Liberty is in neutral territory (dame) or own territory
                return false;
            }
        }

        // All liberties are in enemy territory - the group is dead
        return true;
    }

    /**
     * Find dead stones by checking if stone groups are completely enclosed by opponent.
     * A group is dead if it's completely surrounded by opponent stones with no escape.
     */
    public DeadStones findDeadStonesUsingEnclosure(Board board) {
        Set<Position> blackDeadStones = new HashSet<>();
        Set<Position> whiteDeadStones = new HashSet<>();
        Set<Position> processedGroups = new HashSet<>();

        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                Position pos = new Position(x, y);
                Stone stone = board.getStone(pos);
                if (stone != null && !processedGroups.contains(pos)) {
                    Set<Position> group = board.getGroup(pos);
                    processedGroups.addAll(group);

                    if (isGroupEnclosed(board, group, stone)) {
                        if (stone == Stone.BLACK) {
                            blackDeadStones.addAll(group);
                        } else {
                            whiteDeadStones.addAll(group);
                        }
                    }
                }
            }
        }

        return new DeadStones(blackDeadStones, whiteDeadStones);
    }

    /**
     * Check if a group is enclosed by opponent stones (dead).
     * Uses flood-fill from the group's liberties to see if it's in a small enclosed area.
     * A group is considered dead only if:
     * 1. All reachable empty space is bordered only by enemy stones
     * 2. The enclosed area is small (can't form two eyes)
     */
    private boolean isGroupEnclosed(Board board, Set<Position> group, Stone groupColor) {
        Stone enemyColor = groupColor.opposite();

        // Get all liberties of the group
        Set<Position> liberties = new HashSet<>();
        for (Position pos : group) {
            for (Position adjacent : board.getAdjacentPositions(pos)) {
                if (board.isEmpty(adjacent)) {
                    liberties.add(adjacent);
                }
            }
        }

        // If no liberties, group is already captured (shouldn't happen in end-game)
        if (liberties.isEmpty()) {
            return false;
        }

        // Flood-fill from liberties to find all reachable empty spaces
        // Also track what colors of stones we encounter
        Set<Position> reachableEmpty = new HashSet<>();
        Set<Stone> borderingColors = new HashSet<>();
        Queue<Position> queue = new LinkedList<>(liberties);
        Set<Position> visited = new HashSet<>(liberties);

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            reachableEmpty.add(current);

            for (Position adjacent : board.getAdjacentPositions(current)) {
                Stone adjStone = board.getStone(adjacent);
                if (adjStone != null) {
                    // Skip stones that are part of the group being checked
                    if (!group.contains(adjacent)) {
                        borderingColors.add(adjStone);
                    }
                } else if (!visited.contains(adjacent)) {
                    visited.add(adjacent);
                    queue.add(adjacent);
                }
            }
        }

        // Group is dead only if:
        // 1. All reachable empty space is bordered only by enemy
        // 2. The total enclosed area is reasonable (group could be dead in larger areas)
        //    Use board size squared / 4 as threshold (quarter of the board)
        int totalEnclosedArea = group.size() + reachableEmpty.size();
        int boardArea = board.getSize() * board.getSize();
        int maxDeadAreaSize = boardArea / 4; // Up to quarter of board can contain dead stones

        boolean enclosedByEnemy = borderingColors.size() == 1 && borderingColors.contains(enemyColor);
        boolean reasonableArea = totalEnclosedArea <= maxDeadAreaSize;

        return enclosedByEnemy && reasonableArea;
    }

    /**
     * Calculate territories, treating dead stones as empty positions for connectivity
     * but only counting truly empty positions as territory.
     */
    private List<TerritoryRegion> calculateTerritoriesIgnoringDeadStones(Board board, DeadStones deadStones) {
        Set<Position> allDeadStones = new HashSet<>();
        allDeadStones.addAll(deadStones.blackDeadStones);
        allDeadStones.addAll(deadStones.whiteDeadStones);

        List<TerritoryRegion> territories = new ArrayList<>();
        Set<Position> visited = new HashSet<>();

        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                Position pos = new Position(x, y);
                // Only start flood fill from empty positions (not dead stones)
                if (!visited.contains(pos) && board.isEmpty(pos)) {
                    TerritoryRegion region = floodFillEmptyRegionIgnoringDead(board, pos, visited, allDeadStones);
                    territories.add(region);
                }
            }
        }

        return territories;
    }

    /**
     * Flood-fill treating dead stones as passable but only counting empty positions as territory.
     */
    private TerritoryRegion floodFillEmptyRegionIgnoringDead(Board board, Position start,
                                                             Set<Position> globalVisited,
                                                             Set<Position> deadStones) {
        Set<Position> region = new HashSet<>();
        Set<Stone> borderingColors = new HashSet<>();
        Queue<Position> queue = new LinkedList<>();

        queue.add(start);
        globalVisited.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.poll();

            // Add both empty positions AND dead stone positions to territory
            // (In Japanese scoring, dead stone positions become territory after removal)
            if (board.isEmpty(current) || deadStones.contains(current)) {
                region.add(current);
            }

            for (Position adjacent : board.getAdjacentPositions(current)) {
                Stone stone = board.getStone(adjacent);
                boolean isDeadStone = deadStones.contains(adjacent);

                if (stone != null && !isDeadStone) {
                    // Found a living bordering stone
                    borderingColors.add(stone);
                } else if (!globalVisited.contains(adjacent)) {
                    // Empty position or dead stone we haven't visited - continue flood
                    boolean isEffectivelyEmpty = board.isEmpty(adjacent) || isDeadStone;
                    if (isEffectivelyEmpty) {
                        globalVisited.add(adjacent);
                        queue.add(adjacent);
                    }
                }
            }
        }

        // Determine owner
        Stone owner = null;
        if (borderingColors.size() == 1) {
            owner = borderingColors.iterator().next();
        }

        return new TerritoryRegion(region, owner);
    }

    /**
     * Count prisoners captured during the game from move history.
     * Returns [black prisoners captured, white prisoners captured].
     */
    public int[] countPrisoners(List<Move> moves) {
        int blackPrisoners = 0; // Stones captured by black (white stones)
        int whitePrisoners = 0; // Stones captured by white (black stones)

        for (Move move : moves) {
            if (move.capturedStones != null && !move.capturedStones.isEmpty()) {
                if ("black".equals(move.player)) {
                    blackPrisoners += move.capturedStones.size();
                } else if ("white".equals(move.player)) {
                    whitePrisoners += move.capturedStones.size();
                }
            }
        }

        return new int[]{blackPrisoners, whitePrisoners};
    }

    /**
     * Calculate the final score for a completed game.
     */
    public ScoringResult calculateScore(Board board, List<Move> moves, double komi) {
        // First, identify dead stones using enclosure detection
        DeadStones deadStones = findDeadStonesUsingEnclosure(board);

        // Calculate territories, treating dead stones as empty
        List<TerritoryRegion> territories = calculateTerritoriesIgnoringDeadStones(board, deadStones);

        // Sum up territory for each player and collect positions
        int blackTerritory = 0;
        int whiteTerritory = 0;
        List<Position> blackTerritoryPositions = new ArrayList<>();
        List<Position> whiteTerritoryPositions = new ArrayList<>();

        for (TerritoryRegion territory : territories) {
            if (territory.owner == Stone.BLACK) {
                blackTerritory += territory.size();
                blackTerritoryPositions.addAll(territory.positions);
            } else if (territory.owner == Stone.WHITE) {
                whiteTerritory += territory.size();
                whiteTerritoryPositions.addAll(territory.positions);
            }
        }

        // Count prisoners from move history
        int[] prisoners = countPrisoners(moves);
        int blackPrisoners = prisoners[0];
        int whitePrisoners = prisoners[1];

        // Count dead stones
        int blackDeadCount = deadStones.blackDeadStones.size();
        int whiteDeadCount = deadStones.whiteDeadStones.size();

        LOG.info("Scoring: Black territory={}, White territory={}, " +
                "Black prisoners={}, White prisoners={}, " +
                "Black dead={}, White dead={}, Komi={}",
                blackTerritory, whiteTerritory,
                blackPrisoners, whitePrisoners,
                blackDeadCount, whiteDeadCount, komi);

        return new ScoringResult(
                blackTerritory, whiteTerritory,
                blackPrisoners, whitePrisoners,
                blackDeadCount, whiteDeadCount,
                komi,
                blackTerritoryPositions,
                whiteTerritoryPositions,
                new ArrayList<>(deadStones.blackDeadStones),
                new ArrayList<>(deadStones.whiteDeadStones)
        );
    }

    /**
     * Calculate score with default komi (5.5).
     */
    public ScoringResult calculateScore(Board board, List<Move> moves) {
        return calculateScore(board, moves, DEFAULT_KOMI);
    }

    /**
     * Calculate the final score using manually marked dead stones instead of auto-detection.
     */
    public ScoringResult calculateScoreWithManualDeadStones(
            Board board,
            List<Move> moves,
            double komi,
            List<Position> manuallyMarkedDeadStones
    ) {
        // Separate marked positions into black/white dead stones based on stone color at each position
        Set<Position> blackDeadStones = new HashSet<>();
        Set<Position> whiteDeadStones = new HashSet<>();

        for (Position pos : manuallyMarkedDeadStones) {
            Stone stone = board.getStone(pos);
            if (stone == Stone.BLACK) {
                blackDeadStones.add(pos);
            } else if (stone == Stone.WHITE) {
                whiteDeadStones.add(pos);
            }
            // Ignore positions without stones (shouldn't happen if validated)
        }

        DeadStones deadStones = new DeadStones(blackDeadStones, whiteDeadStones);

        // Calculate territories, treating dead stones as empty
        List<TerritoryRegion> territories = calculateTerritoriesIgnoringDeadStones(board, deadStones);

        // Sum up territory for each player and collect positions
        int blackTerritory = 0;
        int whiteTerritory = 0;
        List<Position> blackTerritoryPositions = new ArrayList<>();
        List<Position> whiteTerritoryPositions = new ArrayList<>();

        for (TerritoryRegion territory : territories) {
            if (territory.owner == Stone.BLACK) {
                blackTerritory += territory.size();
                blackTerritoryPositions.addAll(territory.positions);
            } else if (territory.owner == Stone.WHITE) {
                whiteTerritory += territory.size();
                whiteTerritoryPositions.addAll(territory.positions);
            }
        }

        // Count prisoners from move history
        int[] prisoners = countPrisoners(moves);
        int blackPrisoners = prisoners[0];
        int whitePrisoners = prisoners[1];

        // Count dead stones
        int blackDeadCount = blackDeadStones.size();
        int whiteDeadCount = whiteDeadStones.size();

        LOG.info("Manual scoring: Black territory={}, White territory={}, " +
                "Black prisoners={}, White prisoners={}, " +
                "Black dead={}, White dead={}, Komi={}",
                blackTerritory, whiteTerritory,
                blackPrisoners, whitePrisoners,
                blackDeadCount, whiteDeadCount, komi);

        return new ScoringResult(
                blackTerritory, whiteTerritory,
                blackPrisoners, whitePrisoners,
                blackDeadCount, whiteDeadCount,
                komi,
                blackTerritoryPositions,
                whiteTerritoryPositions,
                new ArrayList<>(blackDeadStones),
                new ArrayList<>(whiteDeadStones)
        );
    }
}
