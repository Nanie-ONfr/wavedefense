package com.wavedefense.arena;

import com.wavedefense.WaveDefensePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saves and loads arena session data to/from disk using YamlConfiguration.
 */
public class ArenaDataStorage {

    private static File getDataFolder() {
        File folder = new File(WaveDefensePlugin.getInstance().getDataFolder(), "arena");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private static File getPlayerFile(UUID playerId) {
        return new File(getDataFolder(), playerId.toString() + ".yml");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> serializeItemList(List<ItemStack> items) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack != null && !stack.getType().isAir()) {
                list.add(stack.serialize());
            } else {
                // Use empty map to mark null/air slot
                list.add(new HashMap<>());
            }
        }
        return list;
    }

    private static List<ItemStack> deserializeItemList(List<?> rawList, int expectedSize) {
        List<ItemStack> items = new ArrayList<>();
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    if (map.isEmpty()) {
                        items.add(null);
                    } else {
                        items.add(ItemStack.deserialize(map));
                    }
                } else {
                    items.add(null);
                }
            }
        }
        // Pad to expected size
        while (items.size() < expectedSize) {
            items.add(null);
        }
        return items;
    }

    public static void savePlayerData(UUID playerId, ArenaSession session) {
        try {
            File file = getPlayerFile(playerId);
            YamlConfiguration yaml = new YamlConfiguration();

            // Save kit and difficulty
            yaml.set("kit", session.getKit().name());
            yaml.set("difficulty", session.getDifficulty().name());

            // Save original location
            Location loc = session.getOriginalLocation();
            yaml.set("original.world", loc.getWorld().getName());
            yaml.set("original.x", loc.getX());
            yaml.set("original.y", loc.getY());
            yaml.set("original.z", loc.getZ());
            yaml.set("original.yaw", (double) loc.getYaw());
            yaml.set("original.pitch", (double) loc.getPitch());

            // Save arena center
            if (session.getArenaCenter() != null) {
                Location center = session.getArenaCenter();
                yaml.set("arenaCenter.world", center.getWorld().getName());
                yaml.set("arenaCenter.x", center.getX());
                yaml.set("arenaCenter.y", center.getY());
                yaml.set("arenaCenter.z", center.getZ());
            }

            // Save inventory
            yaml.set("inventory", serializeItemList(session.getOriginalInventory()));

            // Save armor
            yaml.set("armor", serializeItemList(session.getOriginalArmor()));

            // Save offhand
            if (session.getOriginalOffhand() != null && !session.getOriginalOffhand().getType().isAir()) {
                yaml.set("offhand", session.getOriginalOffhand().serialize());
            }

            // Save health and food
            yaml.set("health", (double) session.getOriginalHealth());
            yaml.set("food", session.getOriginalFoodLevel());

            // Save bot ID if exists
            if (session.getBotId() != null) {
                yaml.set("botId", session.getBotId().toString());
            }

            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArenaSession loadPlayerData(UUID playerId) {
        File file = getPlayerFile(playerId);
        if (!file.exists()) {
            return null;
        }

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            // Load kit and difficulty
            Kit kit = Kit.valueOf(yaml.getString("kit", "NODEBUFF"));
            Difficulty difficulty = Difficulty.valueOf(yaml.getString("difficulty", "MEDIUM"));

            // Load original location
            String worldName = yaml.getString("original.world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                world = Bukkit.getWorlds().get(0); // Fallback to default world
            }
            double origX = yaml.getDouble("original.x", 0.0);
            double origY = yaml.getDouble("original.y", 100.0);
            double origZ = yaml.getDouble("original.z", 0.0);
            float origYaw = (float) yaml.getDouble("original.yaw", 0.0);
            float origPitch = (float) yaml.getDouble("original.pitch", 0.0);
            Location originalLocation = new Location(world, origX, origY, origZ, origYaw, origPitch);

            // Load inventory
            List<?> inventoryRaw = yaml.getList("inventory");
            List<ItemStack> inventory = deserializeItemList(inventoryRaw, 36);

            // Load armor
            List<?> armorRaw = yaml.getList("armor");
            List<ItemStack> armor = deserializeItemList(armorRaw, 4);

            // Load offhand
            ItemStack offhand = null;
            if (yaml.contains("offhand")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> offhandMap = (Map<String, Object>) yaml.get("offhand");
                if (offhandMap != null && !offhandMap.isEmpty()) {
                    offhand = ItemStack.deserialize(offhandMap);
                }
            }

            // Load health and food
            float health = (float) yaml.getDouble("health", 20.0);
            int food = yaml.getInt("food", 20);

            // Create session from loaded data
            ArenaSession session = new ArenaSession(playerId, kit, difficulty, originalLocation,
                    inventory, armor, offhand, health, food);

            // Load arena center
            if (yaml.contains("arenaCenter.world")) {
                String centerWorldName = yaml.getString("arenaCenter.world", "world");
                World centerWorld = Bukkit.getWorld(centerWorldName);
                if (centerWorld == null) {
                    centerWorld = Bukkit.getWorlds().get(0);
                }
                double cx = yaml.getDouble("arenaCenter.x", 0.0);
                double cy = yaml.getDouble("arenaCenter.y", 200.0);
                double cz = yaml.getDouble("arenaCenter.z", 0.0);
                session.setArenaCenter(new Location(centerWorld, cx, cy, cz));
            }

            // Load bot ID
            String botIdStr = yaml.getString("botId", null);
            if (botIdStr != null && !botIdStr.isEmpty()) {
                session.setBotId(UUID.fromString(botIdStr));
            }

            return session;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deletePlayerData(UUID playerId) {
        File file = getPlayerFile(playerId);
        if (file.exists()) {
            file.delete();
        }
    }

    public static boolean hasPlayerData(UUID playerId) {
        return getPlayerFile(playerId).exists();
    }
}
