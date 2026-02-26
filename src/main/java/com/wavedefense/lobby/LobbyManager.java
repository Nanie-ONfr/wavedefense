package com.wavedefense.lobby;

import com.wavedefense.WaveDefensePlugin;
import com.wavedefense.arena.ArenaManager;
import com.wavedefense.arena.Difficulty;
import com.wavedefense.arena.Kit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class LobbyManager {
    private static final int LOBBY_X = 0;
    private static final int LOBBY_Y = 101;
    private static final int LOBBY_Z = 0;
    private boolean lobbyCreated = false;
    private final Map<UUID, Kit> selectedKits = new HashMap<>();
    private final Map<UUID, Difficulty> selectedDifficulties = new HashMap<>();

    /**
     * Gets the lobby world (same as arena world).
     */
    private World getLobbyWorld() {
        World world = ArenaManager.getOrCreateArenaWorld();
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        return world;
    }

    public void createLobby() {
        if (lobbyCreated) return;

        World world = getLobbyWorld();
        if (world == null) return;

        setupLobby(world);
        lobbyCreated = true;
    }

    /**
     * Creates the lobby structure in the given world.
     * - Circular platform (radius 15) of polished blackstone
     * - Gold block center (5x5)
     * - Sea lanterns underneath
     * - 8 armor stands in a circle, one per kit, on colored platforms
     * - Barrier walls around lobby
     */
    public void setupLobby(World world) {
        int centerX = LOBBY_X;
        int centerY = LOBBY_Y;
        int centerZ = LOBBY_Z;

        // Create lobby platform - circular design
        int radius = 15;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    // Main floor
                    world.getBlockAt(centerX + dx, centerY, centerZ + dz).setType(Material.POLISHED_BLACKSTONE);
                    // Light under floor
                    world.getBlockAt(centerX + dx, centerY - 1, centerZ + dz).setType(Material.SEA_LANTERN);
                }
            }
        }

        // Center platform (spawn) - 5x5 gold blocks
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(centerX + dx, centerY, centerZ + dz).setType(Material.GOLD_BLOCK);
            }
        }

        // Create kit selection areas in a circle
        Kit[] kits = Kit.values();
        double angleStep = 2 * Math.PI / kits.length;

        for (int i = 0; i < kits.length; i++) {
            Kit kit = kits[i];
            double angle = i * angleStep;
            int kitX = centerX + (int) (Math.cos(angle) * 10);
            int kitZ = centerZ + (int) (Math.sin(angle) * 10);

            // Kit platform - colored per kit
            Material platformMaterial = getKitPlatformMaterial(kit);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    world.getBlockAt(kitX + dx, centerY, kitZ + dz).setType(platformMaterial);
                }
            }

            // Spawn armor stand display for this kit
            spawnKitDisplay(world, kitX, centerY + 1, kitZ, kit);
        }

        // Barrier walls around lobby
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dy = 1; dy <= 10; dy++) {
                if (dx == -radius - 1 || dx == radius + 1) {
                    for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                        world.getBlockAt(centerX + dx, centerY + dy, centerZ + dz).setType(Material.BARRIER);
                    }
                } else {
                    world.getBlockAt(centerX + dx, centerY + dy, centerZ - radius - 1).setType(Material.BARRIER);
                    world.getBlockAt(centerX + dx, centerY + dy, centerZ + radius + 1).setType(Material.BARRIER);
                }
            }
        }
    }

    /**
     * Returns the platform Material color for a given kit.
     */
    private Material getKitPlatformMaterial(Kit kit) {
        return switch (kit) {
            case NODEBUFF -> Material.BLUE_CONCRETE;
            case BUILDUHC -> Material.GREEN_CONCRETE;
            case CRYSTAL -> Material.MAGENTA_CONCRETE;
            case BOXING -> Material.RED_CONCRETE;
            case GAPPLE -> Material.YELLOW_CONCRETE;
            case SUMO -> Material.LIME_CONCRETE;
            case COMBO -> Material.ORANGE_CONCRETE;
            case BRIDGE -> Material.BROWN_CONCRETE;
            case AXE_SHIELD -> Material.CYAN_CONCRETE;
            case MACE -> Material.PURPLE_CONCRETE;
            case ANCHOR -> Material.BLACK_CONCRETE;
            case ARCHER -> Material.WHITE_CONCRETE;
            case CLASSIC -> Material.LIGHT_GRAY_CONCRETE;
            case SOUP -> Material.PINK_CONCRETE;
            case DEBUFF -> Material.GRAY_CONCRETE;
        };
    }

    /**
     * Spawns the armor stand display for a kit (name + description lines).
     */
    private void spawnKitDisplay(World world, int x, int y, int z, Kit kit) {
        // Kit name armor stand
        world.spawn(new Location(world, x + 0.5, y + 1.5, z + 0.5), ArmorStand.class, stand -> {
            stand.customName(Component.text(kit.getName())
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD));
            stand.setCustomNameVisible(true);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
        });

        // Description line 1
        world.spawn(new Location(world, x + 0.5, y + 1.0, z + 0.5), ArmorStand.class, stand -> {
            stand.customName(Component.text("Rechtsklick = Spielen")
                    .color(NamedTextColor.GREEN));
            stand.setCustomNameVisible(true);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
        });

        // Description line 2
        world.spawn(new Location(world, x + 0.5, y + 0.7, z + 0.5), ArmorStand.class, stand -> {
            stand.customName(Component.text("Shift+Klick = Schwierigkeit")
                    .color(NamedTextColor.GRAY));
            stand.setCustomNameVisible(true);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
        });
    }

    /**
     * Teleports a player to the lobby spawn and resets their state.
     */
    public void teleportToLobby(Player player) {
        World lobbyWorld = getLobbyWorld();

        // Create lobby if not exists
        createLobby();

        // Clear inventory
        player.getInventory().clear();

        // Teleport to lobby spawn
        Location lobbySpawn = new Location(lobbyWorld, LOBBY_X + 0.5, LOBBY_Y + 1, LOBBY_Z + 0.5, 0, 0);
        player.teleport(lobbySpawn);

        // Stop velocity
        player.setVelocity(new Vector(0, 0, 0));

        // Reset health and food
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
        player.setFoodLevel(20);

        // Send welcome message
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== WAVE DEFENSE PVP ===")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Rechtsklick auf Kit-Schild = Spielen")
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Shift + Rechtsklick = Schwierigkeit aendern")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Oder nutze: /wd arena <kit> [easy/medium/hard]")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
    }

    public Kit getSelectedKit(UUID playerId) {
        return selectedKits.getOrDefault(playerId, Kit.NODEBUFF);
    }

    public void setSelectedKit(UUID playerId, Kit kit) {
        selectedKits.put(playerId, kit);
    }

    /**
     * Returns the lobby spawn location.
     */
    public Location getLobbySpawn() {
        World world = getLobbyWorld();
        return new Location(world, LOBBY_X + 0.5, LOBBY_Y + 1, LOBBY_Z + 0.5, 0, 0);
    }

    /**
     * Checks if a player is currently within the lobby area.
     */
    public boolean isInLobby(Player player) {
        Location loc = player.getLocation();
        int dx = Math.abs(loc.getBlockX() - LOBBY_X);
        int dz = Math.abs(loc.getBlockZ() - LOBBY_Z);
        int py = loc.getBlockY();
        return dx <= 20 && dz <= 20 && py >= LOBBY_Y - 5 && py <= LOBBY_Y + 15;
    }

    public Difficulty getSelectedDifficulty(UUID playerId) {
        return selectedDifficulties.getOrDefault(playerId, Difficulty.MEDIUM);
    }

    public void setSelectedDifficulty(UUID playerId, Difficulty difficulty) {
        selectedDifficulties.put(playerId, difficulty);
    }

    /**
     * Cycles the selected difficulty for a player and returns the new difficulty.
     */
    public void handleArmorStandInteraction(Player player, ArmorStand armorStand) {
        // Find which kit this armor stand represents
        for (Kit kit : Kit.values()) {
            Component kitName = Component.text(kit.getName())
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD);
            Component standName = armorStand.customName();
            if (standName != null && standName.equals(kitName)) {
                if (player.isSneaking()) {
                    // Cycle difficulty
                    Difficulty newDiff = cycleDifficulty(player.getUniqueId());
                    player.sendMessage(Component.text("Schwierigkeit: " + newDiff.getName())
                            .color(WaveDefensePlugin.getDifficultyColor(newDiff)));
                } else {
                    // Start arena with this kit
                    Difficulty diff = getSelectedDifficulty(player.getUniqueId());
                    WaveDefensePlugin.getInstance().getArenaManager().startArena(player, kit, diff);
                }
                return;
            }
        }
    }

    public Difficulty cycleDifficulty(UUID playerId) {
        Difficulty current = getSelectedDifficulty(playerId);
        Difficulty next = switch (current) {
            case PRACTICE -> Difficulty.EASY;
            case EASY -> Difficulty.MEDIUM;
            case MEDIUM -> Difficulty.HARD;
            case HARD -> Difficulty.PRACTICE;
        };
        selectedDifficulties.put(playerId, next);
        return next;
    }
}
