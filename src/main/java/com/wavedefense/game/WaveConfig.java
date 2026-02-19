package com.wavedefense.game;

import net.minecraft.entity.EntityType;

import java.util.List;

public class WaveConfig {
    private int spawnRadius = 20;
    private int minSpawnRadius = 15;
    private int delayBetweenWaves = 5;

    public int getSpawnRadius() {
        return spawnRadius;
    }

    public void setSpawnRadius(int radius) {
        this.spawnRadius = Math.max(10, Math.min(50, radius));
    }

    public int getMinSpawnRadius() {
        return minSpawnRadius;
    }

    public int getDelayBetweenWaves() {
        return delayBetweenWaves;
    }

    public void setDelayBetweenWaves(int delay) {
        this.delayBetweenWaves = Math.max(1, Math.min(60, delay));
    }

    public List<EntityType<?>> getMobTypesForWave(int wave) {
        if (wave <= 3) {
            return List.of(EntityType.ZOMBIE, EntityType.SKELETON);
        } else if (wave <= 6) {
            return List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER);
        } else if (wave <= 9) {
            return List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
                          EntityType.CREEPER, EntityType.WITCH, EntityType.ENDERMAN);
        } else {
            return List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
                          EntityType.CREEPER, EntityType.WITCH, EntityType.ENDERMAN,
                          EntityType.WITHER_SKELETON);
        }
    }

    public int getMobCountForWave(int wave) {
        if (wave <= 3) {
            return 5 + wave;
        } else if (wave <= 6) {
            return 10 + (wave - 3);
        } else if (wave <= 9) {
            return 15 + (wave - 6);
        } else {
            return 20 + (wave - 9) * 2;
        }
    }
}
