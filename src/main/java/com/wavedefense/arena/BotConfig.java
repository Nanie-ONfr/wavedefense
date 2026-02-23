package com.wavedefense.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for bot behavior and difficulty settings.
 * Allows customization of bot stats and behavior.
 */
public class BotConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("wavedefense_bot.json");

    private static BotConfig INSTANCE;

    // Health settings
    public float easyHealthMultiplier = 1.0f;
    public float mediumHealthMultiplier = 1.0f;
    public float hardHealthMultiplier = 1.0f;

    // Damage settings
    public float easyDamageMultiplier = 0.5f;
    public float mediumDamageMultiplier = 0.75f;
    public float hardDamageMultiplier = 1.0f;

    // Speed settings
    public float easySpeedMultiplier = 0.8f;
    public float mediumSpeedMultiplier = 1.0f;
    public float hardSpeedMultiplier = 1.2f;

    // Reaction time (lower = faster reactions)
    public int easyReactionTicks = 50;
    public int mediumReactionTicks = 30;
    public int hardReactionTicks = 15;

    // Combat behavior
    public float dodgeChanceMultiplier = 1.0f;
    public float critChanceMultiplier = 1.0f;
    public float healingMultiplier = 1.0f;

    // Warmup time in ticks (20 ticks = 1 second)
    public int warmupTicks = 60;

    // Bot attack range
    public double easyAttackRange = 3.0;
    public double mediumAttackRange = 3.0;
    public double hardAttackRange = 10.0;

    // Enable/disable features
    public boolean enableBossBar = true;
    public boolean enableCombatStats = true;
    public boolean enableWarmup = true;
    public boolean enableBotHealing = true;
    public boolean enableBotDodging = true;

    public static BotConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static BotConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                BotConfig config = GSON.fromJson(json, BotConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                System.err.println("[WaveDefense] Failed to load bot config: " + e.getMessage());
            }
        }
        BotConfig config = new BotConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[WaveDefense] Failed to save bot config: " + e.getMessage());
        }
    }

    public void reload() {
        INSTANCE = load();
    }

    // Helper methods to get multipliers based on difficulty
    public float getHealthMultiplier(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> easyHealthMultiplier;
            case MEDIUM -> mediumHealthMultiplier;
            case HARD -> hardHealthMultiplier;
        };
    }

    public float getDamageMultiplier(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> easyDamageMultiplier;
            case MEDIUM -> mediumDamageMultiplier;
            case HARD -> hardDamageMultiplier;
        };
    }

    public float getSpeedMultiplier(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> easySpeedMultiplier;
            case MEDIUM -> mediumSpeedMultiplier;
            case HARD -> hardSpeedMultiplier;
        };
    }

    public int getReactionTicks(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> easyReactionTicks;
            case MEDIUM -> mediumReactionTicks;
            case HARD -> hardReactionTicks;
        };
    }

    public double getAttackRange(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> easyAttackRange;
            case MEDIUM -> mediumAttackRange;
            case HARD -> hardAttackRange;
        };
    }
}
