package net.tfassbender.game.auth;

public class AuthResponse {
    public String token;
    public String username;
    public String message;

    public AuthResponse() {}

    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }

    public AuthResponse(String message) {
        this.message = message;
    }
}
