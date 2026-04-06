package me.lekkernakkie.lekkeradmin.config.logs;

public class LogFieldConfig {

    private final String player;
    private final String actor;
    private final String source;
    private final String world;
    private final String coordinates;
    private final String item;
    private final String amount;
    private final String itemName;
    private final String enchantments;
    private final String cause;
    private final String killer;
    private final String droppedItems;
    private final String pickupType;
    private final String dropType;
    private final String xp;
    private final String keepInventory;
    private final String reason;
    private final String health;
    private final String food;
    private final String gamemode;

    public LogFieldConfig(
            String player,
            String actor,
            String source,
            String world,
            String coordinates,
            String item,
            String amount,
            String itemName,
            String enchantments,
            String cause,
            String killer,
            String droppedItems,
            String pickupType,
            String dropType,
            String xp,
            String keepInventory,
            String reason,
            String health,
            String food,
            String gamemode
    ) {
        this.player = safe(player, "Speler");
        this.actor = safe(actor, "Staff");
        this.source = safe(source, "Actie");
        this.world = safe(world, "Wereld");
        this.coordinates = safe(coordinates, "Coördinaten");
        this.item = safe(item, "Item");
        this.amount = safe(amount, "Aantal");
        this.itemName = safe(itemName, "Item naam");
        this.enchantments = safe(enchantments, "Enchantments");
        this.cause = safe(cause, "Doodsoorzaak");
        this.killer = safe(killer, "Killer");
        this.droppedItems = safe(droppedItems, "Gedropte items");
        this.pickupType = safe(pickupType, "Pickup info");
        this.dropType = safe(dropType, "Drop info");
        this.xp = safe(xp, "XP");
        this.keepInventory = safe(keepInventory, "KeepInventory");
        this.reason = safe(reason, "Reden");
        this.health = safe(health, "Health");
        this.food = safe(food, "Food");
        this.gamemode = safe(gamemode, "Gamemode");
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public String getPlayer() {
        return player;
    }

    public String getActor() {
        return actor;
    }

    public String getSource() {
        return source;
    }

    public String getWorld() {
        return world;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public String getItem() {
        return item;
    }

    public String getAmount() {
        return amount;
    }

    public String getItemName() {
        return itemName;
    }

    public String getEnchantments() {
        return enchantments;
    }

    public String getCause() {
        return cause;
    }

    public String getKiller() {
        return killer;
    }

    public String getDroppedItems() {
        return droppedItems;
    }

    public String getPickupType() {
        return pickupType;
    }

    public String getDropType() {
        return dropType;
    }

    public String getXp() {
        return xp;
    }

    public String getKeepInventory() {
        return keepInventory;
    }

    public String getReason() {
        return reason;
    }

    public String getHealth() {
        return health;
    }

    public String getFood() {
        return food;
    }

    public String getGamemode() {
        return gamemode;
    }
}