package net.tfassbender.game.game;

import net.tfassbender.game.go.Position;

import java.util.ArrayList;
import java.util.List;

public class GameResult {
    public String winner;    // "black" or "white"
    public String method;    // "resignation", "score", "timeout"
    public Score score;
    public Territory territory;
    public Captures captures;
    public DeadStones deadStones;

    public GameResult() {}

    public GameResult(String winner, String method) {
        this.winner = winner;
        this.method = method;
    }

    public static class Score {
        public double black;
        public double white;

        public Score() {}

        public Score(double black, double white) {
            this.black = black;
            this.white = white;
        }
    }

    public static class Territory {
        public List<Position> blackTerritory;
        public List<Position> whiteTerritory;

        public Territory() {
            this.blackTerritory = new ArrayList<>();
            this.whiteTerritory = new ArrayList<>();
        }

        public Territory(List<Position> blackTerritory, List<Position> whiteTerritory) {
            this.blackTerritory = blackTerritory;
            this.whiteTerritory = whiteTerritory;
        }
    }

    public static class Captures {
        public int black;  // Stones captured by black (white stones removed)
        public int white;  // Stones captured by white (black stones removed)

        public Captures() {}

        public Captures(int black, int white) {
            this.black = black;
            this.white = white;
        }
    }

    public static class DeadStones {
        public List<Position> blackDeadStones;  // Black stones marked as dead
        public List<Position> whiteDeadStones;  // White stones marked as dead

        public DeadStones() {
            this.blackDeadStones = new ArrayList<>();
            this.whiteDeadStones = new ArrayList<>();
        }

        public DeadStones(List<Position> blackDeadStones, List<Position> whiteDeadStones) {
            this.blackDeadStones = blackDeadStones;
            this.whiteDeadStones = whiteDeadStones;
        }
    }
}
