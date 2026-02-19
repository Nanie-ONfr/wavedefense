package com.wavedefense.game;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GameSession {
    private final UUID playerId;
    private final int totalWaves;
    private int currentWave;
    private final Set<UUID> spawnedMobs;
    private GameState state;
    private int ticksUntilNextWave;

    public enum GameState {
        WAITING_FOR_START,
        WAVE_ACTIVE,
        WAVE_COMPLETED,
        BETWEEN_WAVES,
        GAME_OVER,
        GAME_WON
    }

    public GameSession(ServerPlayerEntity player, int totalWaves) {
        this.playerId = player.getUuid();
        this.totalWaves = totalWaves;
        this.currentWave = 0;
        this.spawnedMobs = new HashSet<>();
        this.state = GameState.WAITING_FOR_START;
        this.ticksUntilNextWave = 0;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getTotalWaves() {
        return totalWaves;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public void setCurrentWave(int wave) {
        this.currentWave = wave;
    }

    public void incrementWave() {
        this.currentWave++;
    }

    public Set<UUID> getSpawnedMobs() {
        return spawnedMobs;
    }

    public void addSpawnedMob(MobEntity mob) {
        spawnedMobs.add(mob.getUuid());
    }

    public void removeMob(UUID mobId) {
        spawnedMobs.remove(mobId);
    }

    public void clearMobs() {
        spawnedMobs.clear();
    }

    public boolean hasActiveMobs() {
        return !spawnedMobs.isEmpty();
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public int getTicksUntilNextWave() {
        return ticksUntilNextWave;
    }

    public void setTicksUntilNextWave(int ticks) {
        this.ticksUntilNextWave = ticks;
    }

    public void decrementTicksUntilNextWave() {
        if (ticksUntilNextWave > 0) {
            ticksUntilNextWave--;
        }
    }

    public boolean isLastWave() {
        return currentWave >= totalWaves;
    }

    public boolean isActive() {
        return state == GameState.WAVE_ACTIVE ||
               state == GameState.WAVE_COMPLETED ||
               state == GameState.BETWEEN_WAVES ||
               state == GameState.WAITING_FOR_START;
    }
}
