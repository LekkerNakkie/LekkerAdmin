package me.lekkernakkie.lekkeradmin.service.invsee;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

public class OfflinePlayerDataStore {

    public static class InventorySnapshot {
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack offhand;

        public InventorySnapshot(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
            this.contents = contents;
            this.armor = armor;
            this.offhand = offhand;
        }

        public ItemStack[] getContents() {
            return contents;
        }

        public ItemStack[] getArmor() {
            return armor;
        }

        public ItemStack getOffhand() {
            return offhand;
        }
    }

    private final File playerDataFolder;

    public OfflinePlayerDataStore(File worldFolder) {
        this.playerDataFolder = new File(worldFolder, "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public InventorySnapshot loadInventory(OfflinePlayer player) {
        ItemStack[] contents = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4]; // 0 boots, 1 leggings, 2 chestplate, 3 helmet
        ItemStack offhand = null;

        File file = getPlayerFile(player);
        if (!file.exists()) {
            return new InventorySnapshot(contents, armor, offhand);
        }

        try {
            Object root = readRoot(file);

            Object inventory = getList(root, "Inventory");
            if (inventory != null) {
                int size = listSize(inventory);
                for (int i = 0; i < size; i++) {
                    Object itemTag = getCompoundFromList(inventory, i);
                    if (itemTag == null || isEmptyCompound(itemTag)) {
                        continue;
                    }

                    int slot = normalizeSlot(getByte(itemTag, "Slot"));
                    ItemStack item = fromNbtItem(itemTag);
                    if (isAir(item)) {
                        continue;
                    }

                    if (slot >= 0 && slot <= 35) {
                        contents[slot] = item;
                    } else if (slot == 36 || slot == 100) {
                        armor[0] = item;
                    } else if (slot == 37 || slot == 101) {
                        armor[1] = item;
                    } else if (slot == 38 || slot == 102) {
                        armor[2] = item;
                    } else if (slot == 39 || slot == 103) {
                        armor[3] = item;
                    } else if (slot == 40 || slot == -106 || slot == 150) {
                        offhand = item;
                    }
                }
            }

            Object equipment = getCompound(root, "equipment");
            if (equipment != null && !isEmptyCompound(equipment)) {
                if (armor[3] == null) armor[3] = fromNamedCompound(equipment, "head");
                if (armor[2] == null) armor[2] = fromNamedCompound(equipment, "chest");
                if (armor[1] == null) armor[1] = fromNamedCompound(equipment, "legs");
                if (armor[0] == null) armor[0] = fromNamedCompound(equipment, "feet");
                if (offhand == null) offhand = fromNamedCompound(equipment, "offhand");
            }

            Object armorItems = getList(root, "ArmorItems");
            if (armorItems != null && !isEmptyList(armorItems)) {
                if (armor[0] == null && listSize(armorItems) > 0) armor[0] = fromNbtItem(getCompoundFromList(armorItems, 0));
                if (armor[1] == null && listSize(armorItems) > 1) armor[1] = fromNbtItem(getCompoundFromList(armorItems, 1));
                if (armor[2] == null && listSize(armorItems) > 2) armor[2] = fromNbtItem(getCompoundFromList(armorItems, 2));
                if (armor[3] == null && listSize(armorItems) > 3) armor[3] = fromNbtItem(getCompoundFromList(armorItems, 3));
            }

            Object handItems = getList(root, "HandItems");
            if (offhand == null && handItems != null && listSize(handItems) > 1) {
                offhand = fromNbtItem(getCompoundFromList(handItems, 1));
            }

            Object offhandList = getList(root, "Offhand");
            if (offhand == null && offhandList != null && listSize(offhandList) > 0) {
                offhand = fromNbtItem(getCompoundFromList(offhandList, 0));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Kon offline inventory niet laden uit playerdata: " + ex.getMessage(), ex);
        }

        return new InventorySnapshot(contents, armor, offhand);
    }

    public ItemStack[] loadEnderChest(OfflinePlayer player) {
        ItemStack[] contents = new ItemStack[27];

        File file = getPlayerFile(player);
        if (!file.exists()) {
            return contents;
        }

        try {
            Object root = readRoot(file);
            Object enderItems = getList(root, "EnderItems");

            if (enderItems != null) {
                int size = listSize(enderItems);
                for (int i = 0; i < size; i++) {
                    Object itemTag = getCompoundFromList(enderItems, i);
                    if (itemTag == null || isEmptyCompound(itemTag)) {
                        continue;
                    }

                    int slot = normalizeSlot(getByte(itemTag, "Slot"));
                    if (slot < 0 || slot >= 27) {
                        continue;
                    }

                    ItemStack item = fromNbtItem(itemTag);
                    if (!isAir(item)) {
                        contents[slot] = item;
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Kon offline enderchest niet laden uit playerdata: " + ex.getMessage(), ex);
        }

        return contents;
    }

    public void saveInventory(OfflinePlayer player, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        File file = getPlayerFile(player);

        try {
            Object root = file.exists() ? readRoot(file) : newCompoundTag();
            Object inventory = newListTag();

            for (int slot = 0; slot < 36; slot++) {
                ItemStack item = contents[slot];
                if (isAir(item)) {
                    continue;
                }

                Object itemTag = toNbtItem(item);
                putByte(itemTag, "Slot", (byte) slot);
                addToList(inventory, itemTag);
            }

            if (!isAir(armor[0])) {
                Object boots = toNbtItem(armor[0]);
                putByte(boots, "Slot", (byte) 100);
                addToList(inventory, boots);
            }

            if (!isAir(armor[1])) {
                Object leggings = toNbtItem(armor[1]);
                putByte(leggings, "Slot", (byte) 101);
                addToList(inventory, leggings);
            }

            if (!isAir(armor[2])) {
                Object chest = toNbtItem(armor[2]);
                putByte(chest, "Slot", (byte) 102);
                addToList(inventory, chest);
            }

            if (!isAir(armor[3])) {
                Object helmet = toNbtItem(armor[3]);
                putByte(helmet, "Slot", (byte) 103);
                addToList(inventory, helmet);
            }

            if (!isAir(offhand)) {
                Object offhandTag = toNbtItem(offhand);
                putByte(offhandTag, "Slot", (byte) -106);
                addToList(inventory, offhandTag);
            }

            put(root, "Inventory", inventory);

            Object equipment = newCompoundTag();
            if (!isAir(armor[3])) put(equipment, "head", toNbtItem(armor[3]));
            if (!isAir(armor[2])) put(equipment, "chest", toNbtItem(armor[2]));
            if (!isAir(armor[1])) put(equipment, "legs", toNbtItem(armor[1]));
            if (!isAir(armor[0])) put(equipment, "feet", toNbtItem(armor[0]));
            if (!isAir(offhand)) put(equipment, "offhand", toNbtItem(offhand));
            put(root, "equipment", equipment);

            Object armorItems = newListTag();
            addToList(armorItems, isAir(armor[0]) ? newCompoundTag() : toNbtItem(armor[0]));
            addToList(armorItems, isAir(armor[1]) ? newCompoundTag() : toNbtItem(armor[1]));
            addToList(armorItems, isAir(armor[2]) ? newCompoundTag() : toNbtItem(armor[2]));
            addToList(armorItems, isAir(armor[3]) ? newCompoundTag() : toNbtItem(armor[3]));
            put(root, "ArmorItems", armorItems);

            Object handItems = newListTag();
            addToList(handItems, newCompoundTag());
            addToList(handItems, isAir(offhand) ? newCompoundTag() : toNbtItem(offhand));
            put(root, "HandItems", handItems);

            writeRootAtomically(file, root);
        } catch (Exception ex) {
            throw new IllegalStateException("Kon offline inventory niet opslaan naar playerdata: " + ex.getMessage(), ex);
        }
    }

    public void saveEnderChest(OfflinePlayer player, ItemStack[] contents) {
        File file = getPlayerFile(player);

        try {
            Object root = file.exists() ? readRoot(file) : newCompoundTag();
            Object enderItems = newListTag();

            for (int slot = 0; slot < Math.min(contents.length, 27); slot++) {
                ItemStack item = contents[slot];
                if (isAir(item)) {
                    continue;
                }

                Object itemTag = toNbtItem(item);
                putByte(itemTag, "Slot", (byte) slot);
                addToList(enderItems, itemTag);
            }

            put(root, "EnderItems", enderItems);
            writeRootAtomically(file, root);
        } catch (Exception ex) {
            throw new IllegalStateException("Kon offline enderchest niet opslaan naar playerdata: " + ex.getMessage(), ex);
        }
    }

    private File getPlayerFile(OfflinePlayer player) {
        return new File(playerDataFolder, player.getUniqueId().toString() + ".dat");
    }

    private Object readRoot(File file) throws Exception {
        Class<?> nbtIoClass = getNmsClass("net.minecraft.nbt.NbtIo");
        try {
            return nbtIoClass.getMethod("readCompressed", java.nio.file.Path.class).invoke(null, file.toPath());
        } catch (NoSuchMethodException ignored) {
            return nbtIoClass.getMethod("read", java.nio.file.Path.class).invoke(null, file.toPath());
        }
    }

    private void writeRootAtomically(File file, Object root) throws Exception {
        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        Class<?> nbtIoClass = getNmsClass("net.minecraft.nbt.NbtIo");
        Class<?> compoundTagClass = getNmsClass("net.minecraft.nbt.CompoundTag");

        try {
            nbtIoClass.getMethod("writeCompressed", compoundTagClass, java.nio.file.Path.class).invoke(null, root, tempFile.toPath());
        } catch (NoSuchMethodException ignored) {
            nbtIoClass.getMethod("write", compoundTagClass, java.nio.file.Path.class).invoke(null, root, tempFile.toPath());
        }

        Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );
    }

    private ItemStack fromNamedCompound(Object parent, String key) throws Exception {
        Object nested = getCompound(parent, key);
        if (nested == null || isEmptyCompound(nested)) {
            return null;
        }
        return fromNbtItem(nested);
    }

    private ItemStack fromNbtItem(Object itemTag) throws Exception {
        Object wrapper = newCompoundTag();
        put(wrapper, "item", itemTag);

        Class<?> nmsItemStackClass = getNmsClass("net.minecraft.world.item.ItemStack");
        Object codec = nmsItemStackClass.getField("CODEC").get(null);

        Object parsed = null;
        try {
            parsed = unwrapOptional(wrapper.getClass().getMethod("read", String.class, codec.getClass().getInterfaces()[0]).invoke(wrapper, "item", codec));
        } catch (Exception ignored) {
        }

        if (parsed != null && nmsItemStackClass.isInstance(parsed)) {
            boolean empty = (boolean) nmsItemStackClass.getMethod("isEmpty").invoke(parsed);
            if (!empty) {
                return (ItemStack) nmsItemStackClass.getMethod("asBukkitCopy").invoke(parsed);
            }
        }

        try {
            Object quiet = unwrapOptional(wrapper.getClass().getMethod("readQuiet", String.class, codec.getClass().getInterfaces()[0]).invoke(wrapper, "item", codec));
            if (quiet != null && nmsItemStackClass.isInstance(quiet)) {
                boolean empty = (boolean) nmsItemStackClass.getMethod("isEmpty").invoke(quiet);
                if (!empty) {
                    return (ItemStack) nmsItemStackClass.getMethod("asBukkitCopy").invoke(quiet);
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private Object toNbtItem(ItemStack item) throws Exception {
        Class<?> nmsItemStackClass = getNmsClass("net.minecraft.world.item.ItemStack");
        Object nms = nmsItemStackClass.getMethod("fromBukkitCopy", ItemStack.class).invoke(null, item);

        Object wrapper = newCompoundTag();
        Object codec = nmsItemStackClass.getField("CODEC").get(null);

        storeWithCodec(wrapper, "item", codec, nms);

        Object stored = getCompound(wrapper, "item");
        return stored == null ? newCompoundTag() : stored;
    }

    private void storeWithCodec(Object compound, String key, Object codec, Object value) throws Exception {
        for (var method : compound.getClass().getMethods()) {
            if (!method.getName().equals("store")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 3 && params[0] == String.class) {
                method.invoke(compound, key, codec, value);
                return;
            }
        }
        throw new IllegalStateException("Kon CompoundTag.store(...) niet vinden.");
    }

    private Object getCompound(Object compound, String key) throws Exception {
        try {
            Object result = compound.getClass().getMethod("getCompoundOrEmpty", String.class).invoke(compound, key);
            return unwrapOptional(result);
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Object result = compound.getClass().getMethod("getCompound", String.class).invoke(compound, key);
            return unwrapOptional(result);
        } catch (NoSuchMethodException ignored) {
        }

        Object tag = compound.getClass().getMethod("get", String.class).invoke(compound, key);
        return unwrapOptional(tag);
    }

    private Object getList(Object compound, String key) throws Exception {
        try {
            Object result = compound.getClass().getMethod("getListOrEmpty", String.class).invoke(compound, key);
            return unwrapOptional(result);
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Object result = compound.getClass().getMethod("getList", String.class).invoke(compound, key);
            return unwrapOptional(result);
        } catch (NoSuchMethodException ignored) {
        }

        Object tag = compound.getClass().getMethod("get", String.class).invoke(compound, key);
        return unwrapOptional(tag);
    }

    private Object getCompoundFromList(Object list, int index) throws Exception {
        try {
            Object result = list.getClass().getMethod("getCompoundOrEmpty", int.class).invoke(list, index);
            return unwrapOptional(result);
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Object result = list.getClass().getMethod("getCompound", int.class).invoke(list, index);
            return unwrapOptional(result);
        } catch (NoSuchMethodException ignored) {
        }

        Object tag = list.getClass().getMethod("get", int.class).invoke(list, index);
        return unwrapOptional(tag);
    }

    private int listSize(Object list) throws Exception {
        return (int) list.getClass().getMethod("size").invoke(list);
    }

    private boolean isEmptyList(Object list) throws Exception {
        return list == null || listSize(list) == 0;
    }

    private boolean isEmptyCompound(Object compound) throws Exception {
        return compound == null || (boolean) compound.getClass().getMethod("isEmpty").invoke(compound);
    }

    private int getByte(Object compound, String key) throws Exception {
        try {
            Object value = compound.getClass().getMethod("getByteOr", String.class, byte.class).invoke(compound, key, (byte) 0);
            return ((Number) value).intValue();
        } catch (NoSuchMethodException ignored) {
        }

        Object value = compound.getClass().getMethod("getByte", String.class).invoke(compound, key);
        Object unwrapped = unwrapOptional(value);
        return unwrapped instanceof Number n ? n.intValue() : 0;
    }

    private void putByte(Object compound, String key, byte value) throws Exception {
        compound.getClass().getMethod("putByte", String.class, byte.class).invoke(compound, key, value);
    }

    private void put(Object compound, String key, Object tag) throws Exception {
        Class<?> tagClass = getNmsClass("net.minecraft.nbt.Tag");
        compound.getClass().getMethod("put", String.class, tagClass).invoke(compound, key, tag);
    }

    private void addToList(Object list, Object tag) throws Exception {
        try {
            list.getClass().getMethod("add", Object.class).invoke(list, tag);
            return;
        } catch (NoSuchMethodException ignored) {
        }

        Class<?> tagClass = getNmsClass("net.minecraft.nbt.Tag");
        try {
            int size = listSize(list);
            list.getClass().getMethod("add", int.class, tagClass).invoke(list, size, tag);
            return;
        } catch (NoSuchMethodException ignored) {
        }

        int size = listSize(list);
        list.getClass().getMethod("addTag", int.class, tagClass).invoke(list, size, tag);
    }

    private Object newCompoundTag() throws Exception {
        return getNmsClass("net.minecraft.nbt.CompoundTag").getConstructor().newInstance();
    }

    private Object newListTag() throws Exception {
        return getNmsClass("net.minecraft.nbt.ListTag").getConstructor().newInstance();
    }

    private int normalizeSlot(int raw) {
        return raw > 127 ? raw - 256 : raw;
    }

    private boolean isAir(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private Object unwrapOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    private Class<?> getNmsClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    public static File resolveDefaultWorldFolder() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            throw new IllegalStateException("Geen world geladen om playerdata uit te lezen.");
        }
        return worlds.get(0).getWorldFolder();
    }
}