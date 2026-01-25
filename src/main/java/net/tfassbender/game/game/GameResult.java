package net.tfassbender.game.game;

public class GameResult {
    public String winner;    // "black" or "white"
    public String method;    // "resignation", "score", "timeout"
    public Score score;

    public GameResult() {}

    public GameResult(String winner, String method) {
        this.winner = winner;
        this.method = method;
    }

    public static class Score {
        public int black;
        public int white;

        public Score() {}

        public Score(int black, int white) {
            this.black = black;
            this.white = white;
        }
    }
}
