package net.tfassbender.game.game;

import net.tfassbender.game.go.Position;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Move {
    public String player;           // "black" or "white"
    public String action;           // "place", "pass", "resign"
    public Position position;       // null for pass/resign
    public Instant timestamp;
    public List<Position> capturedStones;

    public Move() {
        this.timestamp = Instant.now();
        this.capturedStones = new ArrayList<>();
    }

    public Move(String player, String action, Position position) {
        this();
        this.player = player;
        this.action = action;
        this.position = position;
    }

    public Move(String player, String action) {
        this(player, action, null);
    }
}
