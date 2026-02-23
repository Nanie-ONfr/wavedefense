package com.wavedefense.arena;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Saves and loads arena session data to/from disk
 */
public class ArenaDataStorage {
    private static final String ARENA_DATA_FOLDER = "wavedefense_arena";

    private static NbtCompound itemToNbt(ItemStack stack) {
        NbtCompound nbt = new NbtCompound();
        if (stack.isEmpty()) {
            nbt.putString("id", "minecraft:air");
            nbt.putInt("count", 0);
        } else {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            nbt.putString("id", id.toString());
            nbt.putInt("count", stack.getCount());
        }
        return nbt;
    }

    private static ItemStack itemFromNbt(NbtCompound nbt) {
        String idStr = nbt.getString("id").orElse("minecraft:air");
        int count = nbt.getInt("count").orElse(0);

        if (count == 0 || idStr.equals("minecraft:air")) {
            return ItemStack.EMPTY;
        }

        var item = Registries.ITEM.get(Identifier.of(idStr));
        return new ItemStack(item, count);
    }

    public static void savePlayerData(MinecraftServer server, UUID playerId, ArenaSession session, RegistryWrapper.WrapperLookup registries) {
        try {
            Path worldPath = server.getSavePath(WorldSavePath.ROOT);
            File dataFolder = worldPath.resolve(ARENA_DATA_FOLDER).toFile();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File playerFile = new File(dataFolder, playerId.toString() + ".dat");
            NbtCompound nbt = new NbtCompound();

            // Save kit and difficulty
            nbt.putString("kit", session.getKit().name());
            nbt.putString("difficulty", session.getDifficulty().name());

            // Save original position
            nbt.putDouble("origX", session.getOriginalPosition().x);
            nbt.putDouble("origY", session.getOriginalPosition().y);
            nbt.putDouble("origZ", session.getOriginalPosition().z);
            nbt.putFloat("origYaw", session.getOriginalYaw());
            nbt.putFloat("origPitch", session.getOriginalPitch());

            // Save arena center
            if (session.getArenaCenter() != null) {
                nbt.putInt("arenaX", session.getArenaCenter().getX());
                nbt.putInt("arenaY", session.getArenaCenter().getY());
                nbt.putInt("arenaZ", session.getArenaCenter().getZ());
            }

            // Save arena world
            if (session.getArenaWorld() != null) {
                nbt.putString("arenaWorld", session.getArenaWorld().getValue().toString());
            }

            // Save inventory
            NbtList inventoryList = new NbtList();
            List<ItemStack> inventory = session.getOriginalInventory();
            for (int i = 0; i < inventory.size(); i++) {
                NbtCompound itemNbt = itemToNbt(inventory.get(i));
                itemNbt.putInt("Slot", i);
                inventoryList.add(itemNbt);
            }
            nbt.put("inventory", inventoryList);

            // Save armor
            NbtList armorList = new NbtList();
            List<ItemStack> armor = session.getOriginalArmor();
            for (int i = 0; i < armor.size(); i++) {
                NbtCompound itemNbt = itemToNbt(armor.get(i));
                itemNbt.putInt("Slot", i);
                armorList.add(itemNbt);
            }
            nbt.put("armor", armorList);

            // Save offhand
            nbt.put("offhand", itemToNbt(session.getOriginalOffhand()));

            // Save health and food
            nbt.putFloat("health", session.getOriginalHealth());
            nbt.putInt("food", session.getOriginalFoodLevel());

            // Save bot ID if exists
            if (session.getBotId() != null) {
                nbt.putString("botId", session.getBotId().toString());
            }

            NbtIo.writeCompressed(nbt, playerFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArenaSession loadPlayerData(MinecraftServer server, UUID playerId, RegistryWrapper.WrapperLookup registries) {
        try {
            Path worldPath = server.getSavePath(WorldSavePath.ROOT);
            File playerFile = worldPath.resolve(ARENA_DATA_FOLDER).resolve(playerId.toString() + ".dat").toFile();

            if (!playerFile.exists()) {
                return null;
            }

            NbtCompound nbt = NbtIo.readCompressed(playerFile.toPath(), NbtSizeTracker.ofUnlimitedBytes());

            // Load kit and difficulty
            Kit kit = Kit.valueOf(nbt.getString("kit").orElse("SWORD"));
            Difficulty difficulty = Difficulty.valueOf(nbt.getString("difficulty").orElse("MEDIUM"));

            // Load original position
            double origX = nbt.getDouble("origX").orElse(0.0);
            double origY = nbt.getDouble("origY").orElse(100.0);
            double origZ = nbt.getDouble("origZ").orElse(0.0);
            float origYaw = nbt.getFloat("origYaw").orElse(0.0f);
            float origPitch = nbt.getFloat("origPitch").orElse(0.0f);

            // Load inventory
            List<ItemStack> inventory = new ArrayList<>();
            NbtElement inventoryElement = nbt.get("inventory");
            if (inventoryElement instanceof NbtList inventoryList) {
                for (int i = 0; i < inventoryList.size(); i++) {
                    NbtCompound itemNbt = inventoryList.getCompoundOrEmpty(i);
                    inventory.add(itemFromNbt(itemNbt));
                }
            }
            // Pad to 36 slots
            while (inventory.size() < 36) {
                inventory.add(ItemStack.EMPTY);
            }

            // Load armor
            List<ItemStack> armor = new ArrayList<>();
            NbtElement armorElement = nbt.get("armor");
            if (armorElement instanceof NbtList armorList) {
                for (int i = 0; i < armorList.size(); i++) {
                    NbtCompound itemNbt = armorList.getCompoundOrEmpty(i);
                    armor.add(itemFromNbt(itemNbt));
                }
            }
            // Pad to 4 slots
            while (armor.size() < 4) {
                armor.add(ItemStack.EMPTY);
            }

            // Load offhand
            NbtCompound offhandNbt = nbt.getCompoundOrEmpty("offhand");
            ItemStack offhand = itemFromNbt(offhandNbt);

            // Load health and food
            float health = nbt.getFloat("health").orElse(20.0f);
            int food = nbt.getInt("food").orElse(20);

            // Create session from loaded data
            ArenaSession session = new ArenaSession(playerId, kit, difficulty, origX, origY, origZ, origYaw, origPitch,
                    inventory, armor, offhand, health, food);

            // Load arena center
            if (nbt.contains("arenaX")) {
                int arenaX = nbt.getInt("arenaX").orElse(0);
                int arenaY = nbt.getInt("arenaY").orElse(200);
                int arenaZ = nbt.getInt("arenaZ").orElse(0);
                session.setArenaCenter(new net.minecraft.util.math.BlockPos(arenaX, arenaY, arenaZ));
            }

            // Load bot ID
            String botIdStr = nbt.getString("botId").orElse(null);
            if (botIdStr != null && !botIdStr.isEmpty()) {
                session.setBotId(UUID.fromString(botIdStr));
            }

            return session;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deletePlayerData(MinecraftServer server, UUID playerId) {
        try {
            Path worldPath = server.getSavePath(WorldSavePath.ROOT);
            File playerFile = worldPath.resolve(ARENA_DATA_FOLDER).resolve(playerId.toString() + ".dat").toFile();
            if (playerFile.exists()) {
                playerFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean hasPlayerData(MinecraftServer server, UUID playerId) {
        Path worldPath = server.getSavePath(WorldSavePath.ROOT);
        File playerFile = worldPath.resolve(ARENA_DATA_FOLDER).resolve(playerId.toString() + ".dat").toFile();
        return playerFile.exists();
    }
}
