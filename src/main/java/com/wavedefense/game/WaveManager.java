package com.wavedefense.game;

import com.wavedefense.WaveDefenseMod;
import com.wavedefense.spawner.MobSpawner;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WaveManager {
    private final Map<UUID, GameSession> activeSessions = new HashMap<>();
    private final WaveConfig config = new WaveConfig();
    private final MobSpawner mobSpawner = new MobSpawner();

    public WaveConfig getConfig() {
        return config;
    }

    private ServerWorld getPlayerWorld(ServerPlayerEntity player) {
        return (ServerWorld) player.getCommandSource().getWorld();
    }

    public boolean startGame(ServerPlayerEntity player, int waves) {
        UUID playerId = player.getUuid();

        if (activeSessions.containsKey(playerId)) {
            player.sendMessage(Text.literal("You already have an active game! Use /wavedefense stop first.")
                .formatted(Formatting.RED), false);
            return false;
        }

        GameSession session = new GameSession(player, waves);
        activeSessions.put(playerId, session);

        player.sendMessage(Text.literal("=== WAVE DEFENSE STARTED ===")
            .formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Total waves: " + waves)
            .formatted(Formatting.YELLOW), false);

        startNextWave(player, session);
        return true;
    }

    public boolean stopGame(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        GameSession session = activeSessions.get(playerId);

        if (session == null) {
            player.sendMessage(Text.literal("You don't have an active game!")
                .formatted(Formatting.RED), false);
            return false;
        }

        cleanupSession(getPlayerWorld(player), session);
        activeSessions.remove(playerId);

        player.sendMessage(Text.literal("=== WAVE DEFENSE STOPPED ===")
            .formatted(Formatting.RED, Formatting.BOLD), false);
        return true;
    }

    private void startNextWave(ServerPlayerEntity player, GameSession session) {
        session.incrementWave();
        session.clearMobs();
        session.setState(GameSession.GameState.WAVE_ACTIVE);

        int wave = session.getCurrentWave();
        int totalWaves = session.getTotalWaves();

        player.sendMessage(Text.literal("--- WAVE " + wave + "/" + totalWaves + " ---")
            .formatted(Formatting.AQUA, Formatting.BOLD), false);

        int mobCount = config.getMobCountForWave(wave);
        player.sendMessage(Text.literal("Enemies incoming: " + mobCount)
            .formatted(Formatting.RED), false);

        mobSpawner.spawnWave(player, session, config);
    }

    private void cleanupSession(ServerWorld world, GameSession session) {
        for (UUID mobId : session.getSpawnedMobs()) {
            Entity entity = world.getEntity(mobId);
            if (entity != null) {
                entity.discard();
            }
        }
        session.clearMobs();
    }

    public void tick(MinecraftServer server) {
        for (Map.Entry<UUID, GameSession> entry : new HashMap<>(activeSessions).entrySet()) {
            UUID playerId = entry.getKey();
            GameSession session = entry.getValue();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                activeSessions.remove(playerId);
                continue;
            }

            ServerWorld world = getPlayerWorld(player);
            updateSession(player, world, session);
        }
    }

    private void updateSession(ServerPlayerEntity player, ServerWorld world, GameSession session) {
        switch (session.getState()) {
            case WAVE_ACTIVE:
                checkWaveCompletion(player, world, session);
                break;
            case BETWEEN_WAVES:
                handleBetweenWaves(player, session);
                break;
            case GAME_WON:
            case GAME_OVER:
                activeSessions.remove(player.getUuid());
                break;
            default:
                break;
        }
    }

    private void checkWaveCompletion(ServerPlayerEntity player, ServerWorld world, GameSession session) {
        session.getSpawnedMobs().removeIf(mobId -> {
            Entity entity = world.getEntity(mobId);
            return entity == null || !entity.isAlive();
        });

        if (!session.hasActiveMobs()) {
            session.setState(GameSession.GameState.WAVE_COMPLETED);
            onWaveCompleted(player, session);
        }
    }

    private void onWaveCompleted(ServerPlayerEntity player, GameSession session) {
        int wave = session.getCurrentWave();

        player.sendMessage(Text.literal("Wave " + wave + " completed!")
            .formatted(Formatting.GREEN, Formatting.BOLD), false);

        if (session.isLastWave()) {
            session.setState(GameSession.GameState.GAME_WON);
            player.sendMessage(Text.literal("=== VICTORY! ALL WAVES DEFEATED! ===")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        } else {
            session.setState(GameSession.GameState.BETWEEN_WAVES);
            session.setTicksUntilNextWave(config.getDelayBetweenWaves() * 20);

            player.sendMessage(Text.literal("Next wave in " + config.getDelayBetweenWaves() + " seconds...")
                .formatted(Formatting.YELLOW), false);
        }
    }

    private void handleBetweenWaves(ServerPlayerEntity player, GameSession session) {
        session.decrementTicksUntilNextWave();

        int ticks = session.getTicksUntilNextWave();
        if (ticks > 0 && ticks % 20 == 0) {
            int seconds = ticks / 20;
            if (seconds <= 3) {
                player.sendMessage(Text.literal(String.valueOf(seconds) + "...")
                    .formatted(Formatting.YELLOW), true);
            }
        }

        if (ticks <= 0) {
            startNextWave(player, session);
        }
    }

    public boolean hasActiveSession(ServerPlayerEntity player) {
        return activeSessions.containsKey(player.getUuid());
    }

    public GameSession getSession(ServerPlayerEntity player) {
        return activeSessions.get(player.getUuid());
    }
}
