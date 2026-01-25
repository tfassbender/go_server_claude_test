package net.tfassbender.game.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class User {
    public String username;

    @JsonProperty("passwordHash")
    private String passwordHash;

    public Instant createdAt;
    public UserStatistics statistics;

    public User() {
        this.statistics = new UserStatistics();
        this.createdAt = Instant.now();
    }

    public User(String username, String passwordHash) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public static class UserStatistics {
        public int gamesPlayed = 0;
        public int wins = 0;
        public int losses = 0;

        public UserStatistics() {}
    }
}
