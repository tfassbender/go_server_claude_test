package net.tfassbender.game.game.events;

/**
 * CDI event fired when the game turn changes to the next player.
 */
public class TurnChangedEvent {
    private final String gameId;
    private final String currentTurn;

    public TurnChangedEvent(String gameId, String currentTurn) {
        this.gameId = gameId;
        this.currentTurn = currentTurn;
    }

    public String getGameId() {
        return gameId;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }
}
