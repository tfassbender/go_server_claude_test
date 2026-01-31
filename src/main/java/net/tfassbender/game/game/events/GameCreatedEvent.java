package net.tfassbender.game.game.events;

/**
 * CDI event fired when a new game is created.
 */
public class GameCreatedEvent {
    private final String gameId;

    public GameCreatedEvent(String gameId) {
        this.gameId = gameId;
    }

    public String getGameId() {
        return gameId;
    }
}
