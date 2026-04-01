package me.lekkernakkie.lekkeradmin.model.log;

public class PlayerJoinLeaveLogContext {

    private final String playerName;
    private final String reason;
    private final String worldName;
    private final String coordinates;
    private final String health;
    private final int food;
    private final String gameMode;

    public PlayerJoinLeaveLogContext(
            String playerName,
            String reason,
            String worldName,
            String coordinates,
            String health,
            int food,
            String gameMode
    ) {
        this.playerName = playerName == null ? "-" : playerName;
        this.reason = reason == null ? "-" : reason;
        this.worldName = worldName == null ? "-" : worldName;
        this.coordinates = coordinates == null ? "-" : coordinates;
        this.health = health == null ? "-" : health;
        this.food = food;
        this.gameMode = gameMode == null ? "-" : gameMode;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getReason() {
        return reason;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public String getHealth() {
        return health;
    }

    public int getFood() {
        return food;
    }

    public String getGameMode() {
        return gameMode;
    }

    public String toPlainText(String type) {
        return "[" + type + "] "
                + playerName
                + " | reason=" + reason
                + " | world=" + worldName
                + " | coords=" + coordinates
                + " | health=" + health
                + " | food=" + food
                + " | gamemode=" + gameMode;
    }
}