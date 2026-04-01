package me.lekkernakkie.lekkeradmin.model.log;

import java.util.UUID;

public class ItemLogBatchEntry {

    private UUID entityUuid;
    private final LoggedItemData itemData;

    private boolean destroyed;
    private ItemDestroyCause destroyCause;

    private boolean pickedUp;

    public ItemLogBatchEntry(UUID entityUuid, LoggedItemData itemData) {
        this.entityUuid = entityUuid;
        this.itemData = itemData;
        this.destroyed = false;
        this.destroyCause = null;
        this.pickedUp = false;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public LoggedItemData getItemData() {
        return itemData;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void markDestroyed(ItemDestroyCause cause) {
        this.destroyed = true;
        this.destroyCause = cause == null ? ItemDestroyCause.UNKNOWN : cause;
    }

    public ItemDestroyCause getDestroyCause() {
        return destroyCause;
    }

    public boolean isPickedUp() {
        return pickedUp;
    }

    public void markPickedUp() {
        this.pickedUp = true;
    }
}