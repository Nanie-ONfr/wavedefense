package com.wavedefense.arena;

import com.wavedefense.lobby.PlayerStats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.generator.ChunkGenerator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.UUID;

public class ArenaManager {
    private final Map<UUID, ArenaSession> activeSessions = new HashMap<>();
    private final Map<UUID, Kit> lastPlayedKit = new HashMap<>();
    private final Map<UUID, Difficulty> lastPlayedDifficulty = new HashMap<>();

    // Arena world name
    public static final String ARENA_WORLD_NAME = "wavedefense_arena";

    /**
     * Gets or creates the dedicated arena void world.
     * Creates a flat void world with no mobs, no daylight cycle, no weather.
     */
    public static World getOrCreateArenaWorld() {
        World arenaWorld = Bukkit.getWorld(ARENA_WORLD_NAME);
        if (arenaWorld != null) {
            return arenaWorld;
        }

        // Create a void world with a custom empty chunk generator
        WorldCreator creator = new WorldCreator(ARENA_WORLD_NAME);
        creator.type(WorldType.FLAT);
        creator.generator(new VoidChunkGenerator());
        creator.generateStructures(false);

        arenaWorld = Bukkit.createWorld(creator);
        if (arenaWorld != null) {
            // Set game rules
            arenaWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            arenaWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            arenaWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            arenaWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
            arenaWorld.setGameRule(GameRule.MOB_GRIEFING, false);
            arenaWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            arenaWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            arenaWorld.setGameRule(GameRule.KEEP_INVENTORY, false);
            arenaWorld.setGameRule(GameRule.NATURAL_REGENERATION, true);

            // Set time to noon for good lighting
            arenaWorld.setTime(6000);
            arenaWorld.setStorm(false);
            arenaWorld.setThundering(false);
        }

        return arenaWorld;
    }

    /**
     * Custom ChunkGenerator that generates empty/void chunks.
     */
    private static class VoidChunkGenerator extends ChunkGenerator {
        @Override
        public boolean shouldGenerateNoise() {
            return false;
        }

        @Override
        public boolean shouldGenerateSurface() {
            return false;
        }

        @Override
        public boolean shouldGenerateBedrock() {
            return false;
        }

        @Override
        public boolean shouldGenerateCaves() {
            return false;
        }

        @Override
        public boolean shouldGenerateDecorations() {
            return false;
        }

        @Override
        public boolean shouldGenerateMobs() {
            return false;
        }

        @Override
        public boolean shouldGenerateStructures() {
            return false;
        }
    }

    public boolean startArena(Player player, Kit kit, Difficulty difficulty) {
        UUID playerId = player.getUniqueId();

        if (activeSessions.containsKey(playerId)) {
            player.sendMessage(Component.text("Du bist bereits in einer Arena!")
                    .color(NamedTextColor.RED));
            return false;
        }

        // Check if player has saved arena data (disconnected during arena)
        if (ArenaDataStorage.hasPlayerData(playerId)) {
            player.sendMessage(Component.text("Du hast noch eine aktive Arena! Nutze /wd leave um sie zu verlassen.")
                    .color(NamedTextColor.YELLOW));
            return false;
        }

        // Save original state
        ArenaSession session = new ArenaSession(player, kit, difficulty);
        activeSessions.put(playerId, session);

        // Save last played for rematch
        lastPlayedKit.put(playerId, kit);
        lastPlayedDifficulty.put(playerId, difficulty);

        // Get or create arena world
        World arenaWorld = getOrCreateArenaWorld();
        if (arenaWorld == null) {
            // Fallback to main world
            arenaWorld = Bukkit.getWorlds().get(0);
        }

        // Create arena
        Location arenaCenter = createArena(arenaWorld, playerId);
        session.setArenaCenter(arenaCenter);

        // Save to disk immediately
        ArenaDataStorage.savePlayerData(playerId, session);

        // Teleport player to arena (blue spawn)
        player.teleport(new Location(arenaWorld,
                arenaCenter.getBlockX() - 15 + 0.5,
                arenaCenter.getBlockY() + 1,
                arenaCenter.getBlockZ() + 0.5,
                90, 0));

        // Apply kit to player
        kit.applyToPlayer(player);

        // Spawn bot
        spawnBot(player, session, kit, difficulty, arenaCenter, arenaWorld);

        // Initialize combat tracking
        session.initHealthTracking((float) player.getHealth(), difficulty.getHealth());

        // Create bossbar for bot health
        session.createBossBar(player);

        // Show title (Adventure API)
        Title.Times times = Title.Times.times(
                Duration.ofMillis(5 * 50),   // fadeIn: 5 ticks
                Duration.ofMillis(40 * 50),  // stay: 40 ticks
                Duration.ofMillis(10 * 50)   // fadeOut: 10 ticks
        );
        player.showTitle(Title.title(
                Component.text("\u2694 " + kit.getName().toUpperCase() + " \u2694")
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD),
                Component.text(difficulty.getName() + " Schwierigkeit")
                        .color(NamedTextColor.YELLOW),
                times));

        player.sendMessage(Component.text("=== ARENA GESTARTET ===")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Kit: " + kit.getName() + " | Schwierigkeit: " + difficulty.getName())
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Warmup: 3 Sekunden...")
                .color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("Nutze /wd leave um die Arena zu verlassen")
                .color(NamedTextColor.GRAY));

        return true;
    }

    private Location createArena(World world, UUID playerId) {
        // Create unique arena position based on player UUID hash
        int hash = playerId.hashCode();
        int x = (hash & 0xFFFF) * 200;
        int z = ((hash >> 16) & 0xFFFF) * 200;
        int y = 100;

        int radius = 20; // 41x41 arena

        // Create main floor - checkerboard pattern
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Checkerboard pattern
                if ((dx + dz) % 2 == 0) {
                    world.getBlockAt(x + dx, y, z + dz).setType(Material.WHITE_CONCRETE);
                } else {
                    world.getBlockAt(x + dx, y, z + dz).setType(Material.LIGHT_GRAY_CONCRETE);
                }
            }
        }

        // Create border ring
        for (int dx = -radius; dx <= radius; dx++) {
            world.getBlockAt(x + dx, y, z - radius).setType(Material.RED_CONCRETE);
            world.getBlockAt(x + dx, y, z + radius).setType(Material.RED_CONCRETE);
            world.getBlockAt(x - radius, y, z + dx).setType(Material.RED_CONCRETE);
            world.getBlockAt(x + radius, y, z + dx).setType(Material.RED_CONCRETE);
        }

        // Create spawn platforms
        // Player spawn (blue)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(x + dx - 15, y, z + dz).setType(Material.BLUE_CONCRETE);
            }
        }
        // Bot spawn (red)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(x + dx + 15, y, z + dz).setType(Material.RED_CONCRETE);
            }
        }

        // Create invisible walls - very high (50 blocks)
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = 1; dy <= 50; dy++) {
                world.getBlockAt(x + dx, y + dy, z - radius).setType(Material.BARRIER);
                world.getBlockAt(x + dx, y + dy, z + radius).setType(Material.BARRIER);
                world.getBlockAt(x - radius, y + dy, z + dx).setType(Material.BARRIER);
                world.getBlockAt(x + radius, y + dy, z + dx).setType(Material.BARRIER);
            }
        }

        // Create ceiling
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                world.getBlockAt(x + dx, y + 51, z + dz).setType(Material.BARRIER);
            }
        }

        // Add light sources (sea lanterns under floor)
        for (int dx = -radius; dx <= radius; dx += 5) {
            for (int dz = -radius; dz <= radius; dz += 5) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.SEA_LANTERN);
            }
        }

        return new Location(world, x, y, z);
    }

    private void spawnBot(Player player, ArenaSession session, Kit kit, Difficulty difficulty, Location arenaCenter, World world) {
        // Spawn 1 bot on red spawn platform
        int botX = arenaCenter.getBlockX() + 15;
        int botY = arenaCenter.getBlockY() + 1;
        int botZ = arenaCenter.getBlockZ();

        Location botLoc = new Location(world, botX + 0.5, botY, botZ + 0.5);
        Zombie bot = (Zombie) world.spawnEntity(botLoc, EntityType.ZOMBIE);

        setupBot(bot, player, kit, difficulty);
        session.setBotId(bot.getUniqueId());
        BotAI botAI = new BotAI(bot, player, kit, difficulty, world);
        session.setBotAI(botAI);
    }

    private void setupBot(Zombie bot, Player player, Kit kit, Difficulty difficulty) {
        // Custom name with Adventure API
        bot.customName(Component.text(kit.getName() + " Bot [" + difficulty.getName() + "]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD));
        bot.setCustomNameVisible(true);

        // Make zombie not burn in sun and behave like player
        bot.setBaby(false);
        bot.setAI(false); // Disable vanilla AI so BotAI can control
        bot.setRemoveWhenFarAway(false); // Persistent

        // Stats based on difficulty
        if (bot.getAttribute(Attribute.MAX_HEALTH) != null) {
            bot.getAttribute(Attribute.MAX_HEALTH).setBaseValue(difficulty.getHealth());
        }
        if (bot.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            bot.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(difficulty.getMovementSpeed());
        }
        if (bot.getAttribute(Attribute.FOLLOW_RANGE) != null) {
            bot.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(difficulty.getFollowRange());
        }
        if (bot.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            bot.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(getWeaponDamage(kit) * difficulty.getDamageMultiplier());
        }
        if (bot.getAttribute(Attribute.ATTACK_KNOCKBACK) != null) {
            bot.getAttribute(Attribute.ATTACK_KNOCKBACK).setBaseValue(0.5);
        }
        bot.setHealth(difficulty.getHealth());

        // Set target
        bot.setTarget(player);

        // Apply kit equipment
        kit.applyToBot(bot);
    }

    private float getWeaponDamage(Kit kit) {
        return switch (kit) {
            case MACE -> 7.0f;
            case NODEBUFF, GAPPLE, COMBO, DEBUFF -> 7.0f;
            case BUILDUHC -> 7.0f;
            case CRYSTAL -> 7.0f;
            case BOXING -> 7.0f;
            case SUMO -> 1.0f;
            case BRIDGE -> 6.0f;
            case AXE_SHIELD -> 9.0f;
            case ANCHOR -> 7.0f;
            case ARCHER -> 1.0f;
            case CLASSIC -> 6.0f;
            case SOUP -> 7.0f;
        };
    }

    public boolean leaveArena(Player player) {
        UUID playerId = player.getUniqueId();

        ArenaSession session = activeSessions.get(playerId);

        // Try to load from disk if not in memory
        if (session == null && ArenaDataStorage.hasPlayerData(playerId)) {
            session = ArenaDataStorage.loadPlayerData(playerId);
        }

        if (session == null) {
            player.sendMessage(Component.text("Du bist nicht in einer Arena!")
                    .color(NamedTextColor.RED));
            return false;
        }

        // Clean up
        cleanupArena(player, session, true);
        activeSessions.remove(playerId);
        ArenaDataStorage.deletePlayerData(playerId);

        player.sendMessage(Component.text("Arena verlassen!")
                .color(NamedTextColor.YELLOW));

        return true;
    }

    private void cleanupArena(Player player, ArenaSession session, boolean teleportToLobby) {
        // Remove bossbar
        session.removeBossBar();

        // Get the correct world where arena was created
        World arenaWorld = null;
        if (session.getArenaCenter() != null && session.getArenaCenter().getWorld() != null) {
            arenaWorld = session.getArenaCenter().getWorld();
        }
        if (arenaWorld == null) {
            arenaWorld = Bukkit.getWorld(ARENA_WORLD_NAME);
        }
        if (arenaWorld == null) {
            arenaWorld = Bukkit.getWorlds().get(0);
        }

        // Remove bot
        if (session.getBotId() != null) {
            Entity bot = Bukkit.getEntity(session.getBotId());
            if (bot != null) {
                bot.remove();
            }
        }

        // Remove arena blocks
        Location center = session.getArenaCenter();
        if (center != null) {
            int cx = center.getBlockX();
            int cy = center.getBlockY();
            int cz = center.getBlockZ();
            int radius = 20;

            // Remove floor and light layer
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    arenaWorld.getBlockAt(cx + dx, cy, cz + dz).setType(Material.AIR);
                    arenaWorld.getBlockAt(cx + dx, cy - 1, cz + dz).setType(Material.AIR);
                }
            }
            // Remove walls (50 blocks high)
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = 1; dy <= 50; dy++) {
                    arenaWorld.getBlockAt(cx + dx, cy + dy, cz - radius).setType(Material.AIR);
                    arenaWorld.getBlockAt(cx + dx, cy + dy, cz + radius).setType(Material.AIR);
                    arenaWorld.getBlockAt(cx - radius, cy + dy, cz + dx).setType(Material.AIR);
                    arenaWorld.getBlockAt(cx + radius, cy + dy, cz + dx).setType(Material.AIR);
                }
            }
            // Remove ceiling
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    arenaWorld.getBlockAt(cx + dx, cy + 51, cz + dz).setType(Material.AIR);
                }
            }
        }

        // Restore player inventory and stats
        session.restore(player);

        // Teleport to lobby
        if (teleportToLobby) {
            teleportToLobby(player);
        }
    }

    private void teleportToLobby(Player player) {
        // Use LobbyManager from the plugin if available, otherwise use default spawn
        World lobbyWorld = Bukkit.getWorld(ARENA_WORLD_NAME);
        if (lobbyWorld == null) {
            lobbyWorld = Bukkit.getWorlds().get(0);
        }

        // Default lobby spawn at 0, 101, 0
        Location lobbySpawn = new Location(lobbyWorld, 0.5, 102, 0.5, 0, 0);
        player.teleport(lobbySpawn);
        player.setVelocity(player.getVelocity().zero());
    }

    private int endermanCleanupTimer = 0;
    private int tickCounter = 0;

    public void tick(World world) {
        // Only process for the arena world
        if (!ARENA_WORLD_NAME.equals(world.getName())) {
            return;
        }

        tickCounter++;

        // Remove endermen from active arenas (every 2 seconds instead of every tick)
        endermanCleanupTimer++;
        if (endermanCleanupTimer >= 40) {
            endermanCleanupTimer = 0;
            for (ArenaSession session : activeSessions.values()) {
                Location center = session.getArenaCenter();
                if (center != null && center.getWorld() != null && center.getWorld().equals(world)) {
                    int cx = center.getBlockX();
                    int cy = center.getBlockY();
                    int cz = center.getBlockZ();

                    // Get all endermen in the world and filter by distance
                    for (Enderman enderman : world.getEntitiesByClass(Enderman.class)) {
                        double distSq = enderman.getLocation().distanceSquared(center);
                        if (distSq < 625) { // 25^2
                            enderman.remove();
                        }
                    }
                }
            }
        }

        // Collect sessions to remove after iteration (avoids ConcurrentModificationException)
        List<UUID> toRemove = null;

        for (Map.Entry<UUID, ArenaSession> entry : activeSessions.entrySet()) {
            UUID playerId = entry.getKey();
            ArenaSession session = entry.getValue();

            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            // Check if this session belongs to this world
            Location sessionCenter = session.getArenaCenter();
            if (sessionCenter == null || sessionCenter.getWorld() == null || !sessionCenter.getWorld().equals(world)) {
                continue;
            }

            // Handle warmup phase
            if (!session.isWarmupComplete()) {
                session.tickWarmup();
                int warmupTicks = session.getWarmupTicks();
                int seconds = (warmupTicks / 20) + 1;

                if (warmupTicks % 20 == 0 && warmupTicks > 0) {
                    NamedTextColor color = seconds == 3 ? NamedTextColor.GREEN :
                            seconds == 2 ? NamedTextColor.YELLOW :
                                    NamedTextColor.RED;

                    player.sendActionBar(Component.text("\u23F1 Kampf beginnt in " + seconds + "...")
                            .color(color));

                    world.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }

                if (warmupTicks == 0) {
                    // FIGHT! title
                    Title.Times fightTimes = Title.Times.times(
                            Duration.ofMillis(0),
                            Duration.ofMillis(20 * 50),
                            Duration.ofMillis(10 * 50)
                    );
                    player.showTitle(Title.title(
                            Component.text("FIGHT!")
                                    .color(NamedTextColor.RED)
                                    .decorate(TextDecoration.BOLD),
                            Component.empty(),
                            fightTimes));

                    world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                }

                continue;
            }

            // Tick bot AI
            BotAI botAI = session.getBotAI();
            if (botAI != null) {
                botAI.tick();
            }

            // Single entity lookup for bot
            boolean botDead = true;
            if (session.getBotId() != null) {
                Entity botEntity = Bukkit.getEntity(session.getBotId());
                if (botEntity instanceof Zombie bot && !bot.isDead()) {
                    botDead = false;
                    float healthPercent = (float) (bot.getHealth() / bot.getAttribute(Attribute.MAX_HEALTH).getValue());
                    session.updateBossBar(healthPercent, session.getKit().getName() + " Bot");
                    session.trackCombat((float) player.getHealth(), (float) bot.getHealth());

                    // Show combat info on actionbar every second
                    if (tickCounter % 20 == 0) {
                        BotAI ai = session.getBotAI();
                        int combo = ai != null ? ai.getComboCount() : 0;
                        String comboText = combo > 0 ? " Combo: " + combo + "x" : "";

                        player.sendActionBar(
                                Component.text("Treffer: ").color(NamedTextColor.GREEN)
                                        .append(Component.text(String.valueOf(session.getPlayerHits())).color(NamedTextColor.WHITE))
                                        .append(Component.text(" | ").color(NamedTextColor.GRAY))
                                        .append(Component.text("Erhalten: ").color(NamedTextColor.RED))
                                        .append(Component.text(String.valueOf(session.getBotHits())).color(NamedTextColor.WHITE))
                                        .append(Component.text(comboText).color(NamedTextColor.GOLD))
                        );
                    }
                }
            }

            if (botDead) {
                // Player won!
                Title.Times winTimes = Title.Times.times(
                        Duration.ofMillis(5 * 50),
                        Duration.ofMillis(60 * 50),
                        Duration.ofMillis(20 * 50)
                );
                player.showTitle(Title.title(
                        Component.text("\u2694 SIEG! \u2694")
                                .color(NamedTextColor.GOLD)
                                .decorate(TextDecoration.BOLD),
                        Component.text(session.getKit().getName() + " Bot besiegt!")
                                .color(NamedTextColor.GREEN),
                        winTimes));

                // Show combat stats
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("=== KAMPFSTATISTIKEN ===")
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Kampfdauer: " + session.getFightDurationFormatted())
                        .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Deine Treffer: " + session.getPlayerHits())
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("Erhaltene Treffer: " + session.getBotHits())
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text(String.format("Schaden ausgeteilt: %.1f \u2764", session.getPlayerDamageDealt() / 2))
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text(String.format("Schaden erhalten: %.1f \u2764", session.getPlayerDamageTaken() / 2))
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.empty());

                world.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                // Track stats
                PlayerStats.addWin(playerId);

                cleanupArena(player, session, true);
                if (toRemove == null) toRemove = new ArrayList<>();
                toRemove.add(playerId);
                ArenaDataStorage.deletePlayerData(playerId);
            }

            // Check if player died
            if (player.isDead()) {
                // Show defeat screen
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("=== NIEDERLAGE ===")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Kampfdauer: " + session.getFightDurationFormatted())
                        .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Deine Treffer: " + session.getPlayerHits())
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("Erhaltene Treffer: " + session.getBotHits())
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.empty());

                // Track stats
                PlayerStats.addLoss(playerId);
                // Will be cleaned up on respawn
            }
        }

        // Remove finished sessions after iteration
        if (toRemove != null) {
            for (UUID id : toRemove) {
                activeSessions.remove(id);
            }
        }
    }

    public void onPlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if player has saved arena data
        if (ArenaDataStorage.hasPlayerData(playerId)) {
            ArenaSession session = ArenaDataStorage.loadPlayerData(playerId);
            if (session != null) {
                activeSessions.put(playerId, session);

                player.sendMessage(Component.text("Du hast noch eine aktive Arena!")
                        .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Nutze /wd leave um sie zu verlassen und deine Items zur\u00FCckzubekommen.")
                        .color(NamedTextColor.GRAY));

                // Respawn bot if needed
                Location arenaCenter = session.getArenaCenter();
                if (arenaCenter != null && arenaCenter.getWorld() != null && session.getBotId() != null) {
                    Entity bot = Bukkit.getEntity(session.getBotId());
                    if (bot == null) {
                        // Bot was lost, respawn it
                        spawnBot(player, session, session.getKit(), session.getDifficulty(), arenaCenter, arenaCenter.getWorld());
                    }
                }
            }
        }
    }

    public boolean isInArena(Player player) {
        UUID playerId = player.getUniqueId();
        if (activeSessions.containsKey(playerId)) {
            return true;
        }
        // Also check disk
        return ArenaDataStorage.hasPlayerData(playerId);
    }

    public ArenaSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public boolean rematch(Player player) {
        UUID playerId = player.getUniqueId();
        Kit kit = lastPlayedKit.get(playerId);
        Difficulty difficulty = lastPlayedDifficulty.get(playerId);

        if (kit == null || difficulty == null) {
            player.sendMessage(Component.text("Keine vorherige Arena gefunden! Nutze /wd arena <kit>")
                    .color(NamedTextColor.RED));
            return false;
        }

        return startArena(player, kit, difficulty);
    }

    public Kit getLastPlayedKit(UUID playerId) {
        return lastPlayedKit.get(playerId);
    }

    public Difficulty getLastPlayedDifficulty(UUID playerId) {
        return lastPlayedDifficulty.get(playerId);
    }

    public void handlePlayerDeath(Player player) {
        UUID playerId = player.getUniqueId();
        ArenaSession session = activeSessions.get(playerId);
        if (session == null) return;

        player.sendMessage(Component.text("NIEDERLAGE!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        PlayerStats.addLoss(playerId);

        // Cleanup will happen on respawn
    }

    public BotAI getBotAIForEntity(UUID entityId) {
        for (ArenaSession session : activeSessions.values()) {
            if (session.getBotAI() != null && session.getBotId() != null && session.getBotId().equals(entityId)) {
                return session.getBotAI();
            }
        }
        return null;
    }

    public void handleRespawn(Player player) {
        UUID playerId = player.getUniqueId();

        ArenaSession session = activeSessions.get(playerId);
        if (session == null && ArenaDataStorage.hasPlayerData(playerId)) {
            session = ArenaDataStorage.loadPlayerData(playerId);
        }

        if (session != null) {
            cleanupArena(player, session, true);
            activeSessions.remove(playerId);
            ArenaDataStorage.deletePlayerData(playerId);
        }
    }
}
