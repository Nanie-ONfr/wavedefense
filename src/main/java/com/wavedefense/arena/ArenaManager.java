package com.wavedefense.arena;

import com.wavedefense.lobby.LobbyManager;
import com.wavedefense.lobby.PlayerStats;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.particle.ParticleTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArenaManager {
    private final Map<UUID, ArenaSession> activeSessions = new HashMap<>();
    private final Map<UUID, Kit> lastPlayedKit = new HashMap<>();
    private final Map<UUID, Difficulty> lastPlayedDifficulty = new HashMap<>();

    // Custom PvP Arena dimension
    public static final RegistryKey<World> ARENA_DIMENSION = RegistryKey.of(
            net.minecraft.registry.RegistryKeys.WORLD,
            net.minecraft.util.Identifier.of("wavedefense", "pvp_arena")
    );

    public boolean startArena(ServerPlayerEntity player, Kit kit, Difficulty difficulty) {
        UUID playerId = player.getUuid();
        MinecraftServer server = player.getCommandSource().getServer();

        if (activeSessions.containsKey(playerId)) {
            player.sendMessage(Text.literal("Du bist bereits in einer Arena!")
                    .formatted(Formatting.RED), false);
            return false;
        }

        // Check if player has saved arena data (disconnected during arena)
        if (ArenaDataStorage.hasPlayerData(server, playerId)) {
            player.sendMessage(Text.literal("Du hast noch eine aktive Arena! Nutze /wd leave um sie zu verlassen.")
                    .formatted(Formatting.YELLOW), false);
            return false;
        }

        // Save original state
        ArenaSession session = new ArenaSession(player, kit, difficulty);
        activeSessions.put(playerId, session);

        // Save last played for rematch
        lastPlayedKit.put(playerId, kit);
        lastPlayedDifficulty.put(playerId, difficulty);

        // Get arena dimension
        ServerWorld arenaWorld = server.getWorld(ARENA_DIMENSION);
        if (arenaWorld == null) {
            // Fallback to overworld if dimension doesn't exist
            arenaWorld = server.getOverworld();
        }

        // Create arena
        BlockPos arenaPos = createArena(arenaWorld, playerId);
        session.setArenaCenter(arenaPos);
        session.setArenaWorld(arenaWorld.getRegistryKey());

        // Save to disk immediately
        ArenaDataStorage.savePlayerData(server, playerId, session, server.getRegistryManager());

        // Teleport player to arena dimension (blue spawn)
        player.teleport(arenaWorld, arenaPos.getX() - 15 + 0.5, arenaPos.getY() + 1, arenaPos.getZ() + 0.5,
                java.util.Set.of(), 90, 0, true);

        // Apply kit to player
        kit.applyToPlayer(player);

        // Spawn bot
        spawnBot(player, session, kit, difficulty, arenaPos, arenaWorld);

        // Initialize combat tracking
        session.initHealthTracking(player.getHealth(), difficulty.getHealth());

        // Create bossbar for bot health
        session.createBossBar(player);

        // Show title
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 40, 10));
        player.networkHandler.sendPacket(new TitleS2CPacket(
            Text.literal("⚔ " + kit.getName().toUpperCase() + " ⚔").formatted(Formatting.GOLD, Formatting.BOLD)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(
            Text.literal(difficulty.getName() + " Schwierigkeit").formatted(Formatting.YELLOW)));

        player.sendMessage(Text.literal("=== ARENA GESTARTET ===")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Kit: " + kit.getName() + " | Schwierigkeit: " + difficulty.getName())
                .formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("Warmup: 3 Sekunden...")
                .formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal("Nutze /wd leave um die Arena zu verlassen")
                .formatted(Formatting.GRAY), false);

        return true;
    }

    private BlockPos createArena(ServerWorld world, UUID playerId) {
        // Create unique arena position based on player UUID hash
        int hash = playerId.hashCode();
        int x = (hash & 0xFFFF) * 200;
        int z = ((hash >> 16) & 0xFFFF) * 200;
        int y = 100;

        BlockPos center = new BlockPos(x, y, z);
        int radius = 20; // 41x41 arena

        // Create main floor - checkerboard pattern
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = center.add(dx, 0, dz);
                // Checkerboard pattern with quartz variants
                if ((dx + dz) % 2 == 0) {
                    world.setBlockState(pos, Blocks.WHITE_CONCRETE.getDefaultState());
                } else {
                    world.setBlockState(pos, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
                }
            }
        }

        // Create border ring
        for (int dx = -radius; dx <= radius; dx++) {
            world.setBlockState(center.add(dx, 0, -radius), Blocks.RED_CONCRETE.getDefaultState());
            world.setBlockState(center.add(dx, 0, radius), Blocks.RED_CONCRETE.getDefaultState());
            world.setBlockState(center.add(-radius, 0, dx), Blocks.RED_CONCRETE.getDefaultState());
            world.setBlockState(center.add(radius, 0, dx), Blocks.RED_CONCRETE.getDefaultState());
        }

        // Create spawn platforms
        // Player spawn (blue)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(center.add(dx - 15, 0, dz), Blocks.BLUE_CONCRETE.getDefaultState());
            }
        }
        // Bot spawn (red)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(center.add(dx + 15, 0, dz), Blocks.RED_CONCRETE.getDefaultState());
            }
        }

        // Create invisible walls - very high
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = 1; dy <= 50; dy++) {
                world.setBlockState(center.add(dx, dy, -radius), Blocks.BARRIER.getDefaultState());
                world.setBlockState(center.add(dx, dy, radius), Blocks.BARRIER.getDefaultState());
                world.setBlockState(center.add(-radius, dy, dx), Blocks.BARRIER.getDefaultState());
                world.setBlockState(center.add(radius, dy, dx), Blocks.BARRIER.getDefaultState());
            }
        }

        // Create ceiling
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                world.setBlockState(center.add(dx, 51, dz), Blocks.BARRIER.getDefaultState());
            }
        }

        // Add light sources (invisible light blocks or glowstone under floor)
        for (int dx = -radius; dx <= radius; dx += 5) {
            for (int dz = -radius; dz <= radius; dz += 5) {
                world.setBlockState(center.add(dx, -1, dz), Blocks.SEA_LANTERN.getDefaultState());
            }
        }

        return center;
    }

    private void spawnBot(ServerPlayerEntity player, ArenaSession session, Kit kit, Difficulty difficulty, BlockPos arenaPos, ServerWorld world) {
        // Spawn 1 bot on red spawn platform
        BlockPos botPos = arenaPos.add(15, 1, 0);

        ZombieEntity bot = EntityType.ZOMBIE.spawn(world, botPos, SpawnReason.MOB_SUMMONED);
        if (bot != null) {
            setupBot(bot, player, kit, difficulty);
            session.setBotId(bot.getUuid());
            BotAI botAI = new BotAI(bot, player, kit, difficulty, world);
            session.setBotAI(botAI);
        }
    }

    private void setupBot(ZombieEntity bot, ServerPlayerEntity player, Kit kit, Difficulty difficulty) {
        bot.setCustomName(Text.literal(kit.getName() + " Bot [" + difficulty.getName() + "]")
                .formatted(Formatting.RED, Formatting.BOLD));
        bot.setCustomNameVisible(true);

        // Make zombie not burn in sun and behave like player
        bot.setBaby(false);
        bot.setAiDisabled(false);
        bot.setPersistent();

        // Stats based on difficulty
        bot.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(difficulty.getHealth());
        bot.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(difficulty.getMovementSpeed());
        bot.getAttributeInstance(EntityAttributes.FOLLOW_RANGE).setBaseValue(difficulty.getFollowRange());
        bot.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(getWeaponDamage(kit) * difficulty.getDamageMultiplier());
        bot.getAttributeInstance(EntityAttributes.ATTACK_KNOCKBACK).setBaseValue(0.5);
        bot.setHealth(difficulty.getHealth());

        // Set target
        bot.setTarget(player);

        // Apply kit equipment
        kit.applyToBot(bot);
    }

    private float getWeaponDamage(Kit kit) {
        return switch (kit) {
            case MACE -> 7.0f;
            case SWORD -> 7.0f;
            case AXE -> 9.0f;
            case BOW -> 1.0f;
            case CRYSTAL -> 7.0f;
            case UHC -> 7.0f;
            case SHIELD -> 7.0f;
            case POTION -> 7.0f;
        };
    }

    public boolean leaveArena(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        MinecraftServer server = player.getCommandSource().getServer();

        ArenaSession session = activeSessions.get(playerId);

        // Try to load from disk if not in memory
        if (session == null && ArenaDataStorage.hasPlayerData(server, playerId)) {
            session = ArenaDataStorage.loadPlayerData(server, playerId, server.getRegistryManager());
        }

        if (session == null) {
            player.sendMessage(Text.literal("Du bist nicht in einer Arena!")
                    .formatted(Formatting.RED), false);
            return false;
        }

        // Clean up
        cleanupArena(player, session, server, true);
        activeSessions.remove(playerId);
        ArenaDataStorage.deletePlayerData(server, playerId);

        player.sendMessage(Text.literal("Arena verlassen!")
                .formatted(Formatting.YELLOW), false);

        return true;
    }

    private void cleanupArena(ServerPlayerEntity player, ArenaSession session, MinecraftServer server, boolean teleportToLobby) {
        // Remove bossbar
        session.removeBossBar();

        // Get the correct world where arena was created
        ServerWorld arenaWorld = null;
        if (session.getArenaWorld() != null) {
            arenaWorld = server.getWorld(session.getArenaWorld());
        }
        if (arenaWorld == null) {
            arenaWorld = server.getWorld(ARENA_DIMENSION);
        }
        if (arenaWorld == null) {
            arenaWorld = server.getOverworld();
        }

        // Remove bot
        if (session.getBotId() != null) {
            var bot = arenaWorld.getEntity(session.getBotId());
            if (bot != null) {
                bot.discard();
            }
        }

        // Remove arena blocks
        BlockPos center = session.getArenaCenter();
        if (center != null) {
            int radius = 20;
            // Remove floor and light layer
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    arenaWorld.setBlockState(center.add(dx, 0, dz), Blocks.AIR.getDefaultState());
                    arenaWorld.setBlockState(center.add(dx, -1, dz), Blocks.AIR.getDefaultState());
                }
            }
            // Remove walls (50 blocks high)
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = 1; dy <= 50; dy++) {
                    arenaWorld.setBlockState(center.add(dx, dy, -radius), Blocks.AIR.getDefaultState());
                    arenaWorld.setBlockState(center.add(dx, dy, radius), Blocks.AIR.getDefaultState());
                    arenaWorld.setBlockState(center.add(-radius, dy, dx), Blocks.AIR.getDefaultState());
                    arenaWorld.setBlockState(center.add(radius, dy, dx), Blocks.AIR.getDefaultState());
                }
            }
            // Remove ceiling
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    arenaWorld.setBlockState(center.add(dx, 51, dz), Blocks.AIR.getDefaultState());
                }
            }
        }

        // Restore player inventory and stats
        session.restore(player);

        // Teleport to lobby
        teleportToLobby(player, server);
    }

    private void teleportToLobby(ServerPlayerEntity player, MinecraftServer server) {
        ServerWorld lobbyWorld = server.getWorld(LobbyManager.LOBBY_DIMENSION);
        if (lobbyWorld == null) {
            lobbyWorld = server.getOverworld();
        }
        BlockPos lobbySpawn = new BlockPos(0, 101, 0);
        player.teleport(lobbyWorld, lobbySpawn.getX() + 0.5, lobbySpawn.getY() + 1, lobbySpawn.getZ() + 0.5,
                java.util.Set.of(), 0, 0, true);
        player.setVelocity(0, 0, 0);
        player.velocityDirty = true;
    }

    private int endermanCleanupTimer = 0;

    public void tick(ServerWorld world) {
        MinecraftServer server = world.getServer();

        // Remove endermen from active arenas (every 2 seconds instead of every tick)
        endermanCleanupTimer++;
        if (endermanCleanupTimer >= 40) {
            endermanCleanupTimer = 0;
            for (ArenaSession session : activeSessions.values()) {
                if (session.getArenaWorld() != null && session.getArenaWorld().equals(world.getRegistryKey())) {
                    BlockPos center = session.getArenaCenter();
                    if (center != null) {
                        world.getEntitiesByType(EntityType.ENDERMAN,
                                entity -> entity.squaredDistanceTo(center.getX(), center.getY(), center.getZ()) < 625)
                                .forEach(EndermanEntity::discard);
                    }
                }
            }
        }

        // Collect sessions to remove after iteration (avoids ConcurrentModificationException without copying)
        List<UUID> toRemove = null;

        for (Map.Entry<UUID, ArenaSession> entry : activeSessions.entrySet()) {
            UUID playerId = entry.getKey();
            ArenaSession session = entry.getValue();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                continue;
            }

            ServerWorld sessionWorld = server.getWorld(session.getArenaWorld());
            if (sessionWorld == null || sessionWorld != world) {
                continue;
            }

            // Handle warmup phase
            if (!session.isWarmupComplete()) {
                session.tickWarmup();
                int warmupTicks = session.getWarmupTicks();
                int seconds = (warmupTicks / 20) + 1;

                if (warmupTicks % 20 == 0 && warmupTicks > 0) {
                    player.sendMessage(Text.literal("⏱ Kampf beginnt in " + seconds + "...").formatted(
                        seconds == 3 ? Formatting.GREEN :
                        seconds == 2 ? Formatting.YELLOW :
                        Formatting.RED), true);

                    sessionWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                }

                if (warmupTicks == 0) {
                    player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 20, 10));
                    player.networkHandler.sendPacket(new TitleS2CPacket(
                        Text.literal("FIGHT!").formatted(Formatting.RED, Formatting.BOLD)));
                    sessionWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.5f, 1.5f);
                }

                continue;
            }

            // Tick bot AI
            BotAI botAI = session.getBotAI();
            if (botAI != null) {
                botAI.tick();
            }

            // Single entity lookup for bot (was doing 2 lookups before)
            boolean botDead = true;
            if (session.getBotId() != null) {
                var botEntity = sessionWorld.getEntity(session.getBotId());
                if (botEntity instanceof ZombieEntity bot && bot.isAlive()) {
                    botDead = false;
                    float healthPercent = bot.getHealth() / bot.getMaxHealth();
                    session.updateBossBar(healthPercent, session.getKit().getName() + " Bot");
                    session.trackCombat(player.getHealth(), bot.getHealth());

                    // Show combat info on actionbar every second
                    if (server.getTicks() % 20 == 0) {
                        BotAI ai = session.getBotAI();
                        int combo = ai != null ? ai.getComboCount() : 0;
                        String comboText = combo > 0 ? " Combo: " + combo + "x" : "";

                        player.sendMessage(Text.literal("")
                            .append(Text.literal("Treffer: ").formatted(Formatting.GREEN))
                            .append(Text.literal(String.valueOf(session.getPlayerHits())).formatted(Formatting.WHITE))
                            .append(Text.literal(" | ").formatted(Formatting.GRAY))
                            .append(Text.literal("Erhalten: ").formatted(Formatting.RED))
                            .append(Text.literal(String.valueOf(session.getBotHits())).formatted(Formatting.WHITE))
                            .append(Text.literal(comboText).formatted(Formatting.GOLD)), true);
                    }
                }
            }

            if (botDead) {
                // Player won!
                player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 60, 20));
                player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal("⚔ SIEG! ⚔").formatted(Formatting.GOLD, Formatting.BOLD)));
                player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal(session.getKit().getName() + " Bot besiegt!").formatted(Formatting.GREEN)));

                // Show combat stats
                player.sendMessage(Text.literal(""), false);
                player.sendMessage(Text.literal("=== KAMPFSTATISTIKEN ===")
                        .formatted(Formatting.GOLD, Formatting.BOLD), false);
                player.sendMessage(Text.literal("Kampfdauer: " + session.getFightDurationFormatted())
                        .formatted(Formatting.YELLOW), false);
                player.sendMessage(Text.literal("Deine Treffer: " + session.getPlayerHits())
                        .formatted(Formatting.GREEN), false);
                player.sendMessage(Text.literal("Erhaltene Treffer: " + session.getBotHits())
                        .formatted(Formatting.RED), false);
                player.sendMessage(Text.literal(String.format("Schaden ausgeteilt: %.1f ❤", session.getPlayerDamageDealt() / 2))
                        .formatted(Formatting.GREEN), false);
                player.sendMessage(Text.literal(String.format("Schaden erhalten: %.1f ❤", session.getPlayerDamageTaken() / 2))
                        .formatted(Formatting.RED), false);
                player.sendMessage(Text.literal(""), false);

                sessionWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);

                // Track stats
                PlayerStats.addWin(server, playerId);

                cleanupArena(player, session, server, true);
                if (toRemove == null) toRemove = new java.util.ArrayList<>();
                toRemove.add(playerId);
                ArenaDataStorage.deletePlayerData(server, playerId);
            }

            // Check if player died
            if (player.isDead()) {
                // Show defeat screen
                player.sendMessage(Text.literal(""), false);
                player.sendMessage(Text.literal("=== NIEDERLAGE ===")
                        .formatted(Formatting.RED, Formatting.BOLD), false);
                player.sendMessage(Text.literal("Kampfdauer: " + session.getFightDurationFormatted())
                        .formatted(Formatting.YELLOW), false);
                player.sendMessage(Text.literal("Deine Treffer: " + session.getPlayerHits())
                        .formatted(Formatting.GREEN), false);
                player.sendMessage(Text.literal("Erhaltene Treffer: " + session.getBotHits())
                        .formatted(Formatting.RED), false);
                player.sendMessage(Text.literal(""), false);

                // Track stats
                PlayerStats.addLoss(server, playerId);
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

    public void onPlayerJoin(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        MinecraftServer server = player.getCommandSource().getServer();

        // Check if player has saved arena data
        if (ArenaDataStorage.hasPlayerData(server, playerId)) {
            ArenaSession session = ArenaDataStorage.loadPlayerData(server, playerId, server.getRegistryManager());
            if (session != null) {
                activeSessions.put(playerId, session);

                player.sendMessage(Text.literal("Du hast noch eine aktive Arena!")
                        .formatted(Formatting.YELLOW), false);
                player.sendMessage(Text.literal("Nutze /wd leave um sie zu verlassen und deine Items zurückzubekommen.")
                        .formatted(Formatting.GRAY), false);

                // Respawn bot if needed
                ServerWorld arenaWorld = server.getWorld(session.getArenaWorld());
                if (arenaWorld != null && session.getBotId() != null) {
                    var bot = arenaWorld.getEntity(session.getBotId());
                    if (bot == null) {
                        // Bot was lost, respawn it
                        BlockPos arenaPos = session.getArenaCenter();
                        if (arenaPos != null) {
                            spawnBot(player, session, session.getKit(), session.getDifficulty(), arenaPos, arenaWorld);
                        }
                    }
                }
            }
        }
    }

    public boolean isInArena(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        if (activeSessions.containsKey(playerId)) {
            return true;
        }
        // Also check disk
        MinecraftServer server = player.getCommandSource().getServer();
        return ArenaDataStorage.hasPlayerData(server, playerId);
    }

    public ArenaSession getSession(ServerPlayerEntity player) {
        return activeSessions.get(player.getUuid());
    }

    public boolean rematch(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        Kit kit = lastPlayedKit.get(playerId);
        Difficulty difficulty = lastPlayedDifficulty.get(playerId);

        if (kit == null || difficulty == null) {
            player.sendMessage(Text.literal("Keine vorherige Arena gefunden! Nutze /wd arena <kit>")
                    .formatted(Formatting.RED), false);
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

    public void handleRespawn(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        MinecraftServer server = player.getCommandSource().getServer();

        ArenaSession session = activeSessions.get(playerId);
        if (session == null && ArenaDataStorage.hasPlayerData(server, playerId)) {
            session = ArenaDataStorage.loadPlayerData(server, playerId, server.getRegistryManager());
        }

        if (session != null) {
            cleanupArena(player, session, server, true);
            activeSessions.remove(playerId);
            ArenaDataStorage.deletePlayerData(server, playerId);
        }
    }
}
