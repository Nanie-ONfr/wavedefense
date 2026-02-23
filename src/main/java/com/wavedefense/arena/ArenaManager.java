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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaManager {
    private final Map<UUID, ArenaSession> activeSessions = new HashMap<>();

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

        player.sendMessage(Text.literal("=== ARENA GESTARTET ===")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Kit: " + kit.getName() + " | Schwierigkeit: " + difficulty.getName())
                .formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("Besiege den Bot um zu gewinnen!")
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
            BotAI botAI = new BotAI(bot, player, kit, difficulty);
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

    public void tick(ServerWorld world) {
        MinecraftServer server = world.getServer();

        // Remove endermen from active arenas
        for (ArenaSession session : activeSessions.values()) {
            if (session.getArenaWorld() != null && session.getArenaWorld().equals(world.getRegistryKey())) {
                BlockPos center = session.getArenaCenter();
                if (center != null) {
                    // Remove any endermen within arena bounds (radius 25)
                    world.getEntitiesByType(EntityType.ENDERMAN,
                            entity -> entity.squaredDistanceTo(center.getX(), center.getY(), center.getZ()) < 625)
                            .forEach(EndermanEntity::discard);
                }
            }
        }

        for (Map.Entry<UUID, ArenaSession> entry : new HashMap<>(activeSessions).entrySet()) {
            UUID playerId = entry.getKey();
            ArenaSession session = entry.getValue();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                // Player disconnected - session stays in memory and on disk
                continue;
            }

            // Only tick in the arena world
            ServerWorld sessionWorld = server.getWorld(session.getArenaWorld());
            if (sessionWorld == null || sessionWorld != world) {
                continue;
            }

            // Tick bot AI
            BotAI botAI = session.getBotAI();
            if (botAI != null) {
                botAI.tick();
            }

            // Check if bot is dead
            boolean botDead = true;
            if (session.getBotId() != null) {
                var bot = sessionWorld.getEntity(session.getBotId());
                botDead = (bot == null || !bot.isAlive());
            }

            if (botDead) {
                // Player won!
                player.sendMessage(Text.literal("=== SIEG! ===")
                        .formatted(Formatting.GOLD, Formatting.BOLD), false);
                player.sendMessage(Text.literal("Du hast den " + session.getKit().getName() + " Bot besiegt!")
                        .formatted(Formatting.GREEN), false);

                // Track stats
                PlayerStats.addWin(server, playerId);

                cleanupArena(player, session, server, true);
                activeSessions.remove(playerId);
                ArenaDataStorage.deletePlayerData(server, playerId);
            }

            // Check if player died
            if (player.isDead()) {
                player.sendMessage(Text.literal("=== NIEDERLAGE ===")
                        .formatted(Formatting.RED, Formatting.BOLD), false);

                // Track stats
                PlayerStats.addLoss(server, playerId);
                // Will be cleaned up on respawn
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
