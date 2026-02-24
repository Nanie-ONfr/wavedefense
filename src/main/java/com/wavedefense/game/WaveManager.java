package com.wavedefense.game;

import com.wavedefense.WaveDefenseMod;
import com.wavedefense.fighter.FighterSpawner;
import com.wavedefense.spawner.MobSpawner;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WaveManager {
    private final Map<UUID, GameSession> activeSessions = new HashMap<>();
    private final WaveConfig config = new WaveConfig();
    private final MobSpawner mobSpawner = new MobSpawner();
    private final FighterSpawner fighterSpawner = new FighterSpawner();

    public WaveConfig getConfig() {
        return config;
    }

    private ServerWorld getPlayerWorld(ServerPlayerEntity player) {
        return (ServerWorld) player.getCommandSource().getWorld();
    }

    public boolean startGame(ServerPlayerEntity player, int waves) {
        UUID playerId = player.getUuid();

        if (activeSessions.containsKey(playerId)) {
            player.sendMessage(Text.literal("Du hast bereits ein aktives Spiel! Nutze /wavedefense stop zuerst.")
                .formatted(Formatting.RED), false);
            return false;
        }

        GameSession session = new GameSession(player, waves);
        activeSessions.put(playerId, session);

        player.sendMessage(Text.literal("=== WAVE DEFENSE GESTARTET ===")
            .formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Wellen: " + waves)
            .formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("⚔ PvP-Kämpfer erscheinen ab Welle 5! ⚔")
            .formatted(Formatting.LIGHT_PURPLE), false);

        startNextWave(player, session);
        return true;
    }

    public boolean stopGame(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        GameSession session = activeSessions.get(playerId);

        if (session == null) {
            player.sendMessage(Text.literal("Du hast kein aktives Spiel!")
                .formatted(Formatting.RED), false);
            return false;
        }

        cleanupSession(getPlayerWorld(player), session);
        activeSessions.remove(playerId);

        player.sendMessage(Text.literal("=== WAVE DEFENSE GESTOPPT ===")
            .formatted(Formatting.RED, Formatting.BOLD), false);
        return true;
    }

    private void startNextWave(ServerPlayerEntity player, GameSession session) {
        session.incrementWave();
        session.clearMobs();
        session.setState(GameSession.GameState.WAVE_ACTIVE);

        int wave = session.getCurrentWave();
        int totalWaves = session.getTotalWaves();

        player.sendMessage(Text.literal("--- WELLE " + wave + "/" + totalWaves + " ---")
            .formatted(Formatting.AQUA, Formatting.BOLD), false);

        int mobCount = config.getMobCountForWave(wave);
        player.sendMessage(Text.literal("Gegner: " + mobCount)
            .formatted(Formatting.RED), false);

        // Spawn regular mobs
        mobSpawner.spawnWave(player, session, config);

        // Spawn PvP fighters from wave 5 onwards
        if (wave >= 5) {
            fighterSpawner.spawnFighters(player, session, config, wave);
            int fighterCount = wave < 7 ? 1 : (wave < 9 ? 2 : 3 + (wave - 9));
            player.sendMessage(Text.literal("⚔ " + fighterCount + " PvP-Kämpfer! ⚔")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
        }
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
        List<UUID> toRemove = null;

        for (Map.Entry<UUID, GameSession> entry : activeSessions.entrySet()) {
            UUID playerId = entry.getKey();
            GameSession session = entry.getValue();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                if (toRemove == null) toRemove = new java.util.ArrayList<>();
                toRemove.add(playerId);
                continue;
            }

            ServerWorld world = getPlayerWorld(player);
            if (shouldRemoveSession(player, world, session)) {
                if (toRemove == null) toRemove = new java.util.ArrayList<>();
                toRemove.add(playerId);
            }
        }

        if (toRemove != null) {
            for (UUID id : toRemove) {
                activeSessions.remove(id);
            }
        }
    }

    private boolean shouldRemoveSession(ServerPlayerEntity player, ServerWorld world, GameSession session) {
        switch (session.getState()) {
            case WAVE_ACTIVE:
                checkWaveCompletion(player, world, session);
                return false;
            case BETWEEN_WAVES:
                handleBetweenWaves(player, session);
                return false;
            case GAME_WON:
            case GAME_OVER:
                return true;
            default:
                return false;
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

        player.sendMessage(Text.literal("Welle " + wave + " geschafft!")
            .formatted(Formatting.GREEN, Formatting.BOLD), false);

        if (session.isLastWave()) {
            session.setState(GameSession.GameState.GAME_WON);
            player.sendMessage(Text.literal("=== SIEG! ALLE WELLEN BESIEGT! ===")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        } else {
            session.setState(GameSession.GameState.BETWEEN_WAVES);
            session.setTicksUntilNextWave(config.getDelayBetweenWaves() * 20);

            player.sendMessage(Text.literal("Nächste Welle in " + config.getDelayBetweenWaves() + " Sekunden...")
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
