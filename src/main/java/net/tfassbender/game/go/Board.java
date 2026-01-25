package net.tfassbender.game.go;

import java.util.*;

public class Board {
    private final int size;
    private final Stone[][] grid;

    public Board(int size) {
        if (size != 9 && size != 13 && size != 19) {
            throw new IllegalArgumentException("Board size must be 9, 13, or 19");
        }
        this.size = size;
        this.grid = new Stone[size][size];
    }

    /**
     * Copy constructor for board state simulation
     */
    public Board(Board other) {
        this.size = other.size;
        this.grid = new Stone[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(other.grid[i], 0, this.grid[i], 0, size);
        }
    }

    public int getSize() {
        return size;
    }

    public Stone getStone(int x, int y) {
        if (!isValidPosition(x, y)) {
            return null;
        }
        return grid[x][y];
    }

    public Stone getStone(Position pos) {
        return getStone(pos.x, pos.y);
    }

    public void setStone(int x, int y, Stone stone) {
        if (!isValidPosition(x, y)) {
            throw new IllegalArgumentException("Invalid position: (" + x + "," + y + ")");
        }
        grid[x][y] = stone;
    }

    public void setStone(Position pos, Stone stone) {
        setStone(pos.x, pos.y, stone);
    }

    public void removeStone(int x, int y) {
        if (isValidPosition(x, y)) {
            grid[x][y] = null;
        }
    }

    public void removeStone(Position pos) {
        removeStone(pos.x, pos.y);
    }

    public boolean isEmpty(int x, int y) {
        return isValidPosition(x, y) && grid[x][y] == null;
    }

    public boolean isEmpty(Position pos) {
        return isEmpty(pos.x, pos.y);
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    public boolean isValidPosition(Position pos) {
        return isValidPosition(pos.x, pos.y);
    }

    /**
     * Get all adjacent positions (up, down, left, right)
     */
    public List<Position> getAdjacentPositions(Position pos) {
        List<Position> adjacent = new ArrayList<>();
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        for (int[] dir : directions) {
            int newX = pos.x + dir[0];
            int newY = pos.y + dir[1];
            if (isValidPosition(newX, newY)) {
                adjacent.add(new Position(newX, newY));
            }
        }

        return adjacent;
    }

    /**
     * Get all positions occupied by stones of a specific color
     */
    public List<Position> getStonesOfColor(Stone color) {
        List<Position> positions = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (grid[x][y] == color) {
                    positions.add(new Position(x, y));
                }
            }
        }
        return positions;
    }

    /**
     * Get the group of stones connected to the given position
     */
    public Set<Position> getGroup(Position pos) {
        Stone stone = getStone(pos);
        if (stone == null) {
            return new HashSet<>();
        }

        Set<Position> group = new HashSet<>();
        Set<Position> visited = new HashSet<>();
        Queue<Position> queue = new LinkedList<>();

        queue.add(pos);
        visited.add(pos);

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            group.add(current);

            for (Position adjacent : getAdjacentPositions(current)) {
                if (!visited.contains(adjacent) && getStone(adjacent) == stone) {
                    visited.add(adjacent);
                    queue.add(adjacent);
                }
            }
        }

        return group;
    }

    /**
     * Count liberties (empty adjacent intersections) for a group
     */
    public int countLiberties(Set<Position> group) {
        Set<Position> liberties = new HashSet<>();

        for (Position pos : group) {
            for (Position adjacent : getAdjacentPositions(pos)) {
                if (isEmpty(adjacent)) {
                    liberties.add(adjacent);
                }
            }
        }

        return liberties.size();
    }

    /**
     * Count liberties for the group containing the stone at the given position
     */
    public int countLiberties(Position pos) {
        Set<Position> group = getGroup(pos);
        return countLiberties(group);
    }

    /**
     * Get a simple hash of the board state for Ko detection
     */
    public String getBoardHash() {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Stone stone = grid[x][y];
                if (stone == null) {
                    sb.append('.');
                } else if (stone == Stone.BLACK) {
                    sb.append('B');
                } else {
                    sb.append('W');
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Stone stone = grid[x][y];
                if (stone == null) {
                    sb.append(". ");
                } else if (stone == Stone.BLACK) {
                    sb.append("B ");
                } else {
                    sb.append("W ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
