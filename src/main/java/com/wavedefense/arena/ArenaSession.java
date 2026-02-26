package com.wavedefense.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArenaSession {
    private final UUID playerId;
    private final Kit kit;
    private final Difficulty difficulty;
    private final Location originalLocation;
    private final List<ItemStack> originalInventory;
    private final List<ItemStack> originalArmor;
    private final ItemStack originalOffhand;
    private final float originalHealth;
    private final int originalFoodLevel;
    private UUID botId;
    private BotAI botAI;
    private Location arenaCenter;

    // Combat feedback
    private BossBar bossBar;
    private int warmupTicks;
    private boolean warmupComplete = false;
    private long fightStartTime = 0;
    private int playerHits = 0;
    private int botHits = 0;
    private float playerDamageDealt = 0;
    private float playerDamageTaken = 0;

    // Health tracking for hit detection
    private float lastPlayerHealth = 20.0f;
    private float lastBotHealth = 20.0f;

    // Constructor for new session from player
    public ArenaSession(Player player, Kit kit, Difficulty difficulty) {
        this.playerId = player.getUniqueId();
        this.kit = kit;
        this.difficulty = difficulty;
        this.originalLocation = player.getLocation().clone();
        this.originalHealth = (float) player.getHealth();
        this.originalFoodLevel = player.getFoodLevel();
        this.warmupTicks = BotConfig.getInstance().warmupTicks;

        // Save inventory (main inventory has 36 slots: 0-35)
        this.originalInventory = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            originalInventory.add(stack != null ? stack.clone() : null);
        }

        // Save armor (4 slots)
        this.originalArmor = new ArrayList<>();
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (int i = 0; i < 4; i++) {
            originalArmor.add(armorContents[i] != null ? armorContents[i].clone() : null);
        }

        // Save offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        this.originalOffhand = offhand.getType().isAir() ? null : offhand.clone();
    }

    // Constructor for loading from disk
    public ArenaSession(UUID playerId, Kit kit, Difficulty difficulty, Location originalLocation,
                        List<ItemStack> inventory, List<ItemStack> armor, ItemStack offhand,
                        float health, int food) {
        this.playerId = playerId;
        this.kit = kit;
        this.difficulty = difficulty;
        this.originalLocation = originalLocation;
        this.originalInventory = inventory;
        this.originalArmor = armor;
        this.originalOffhand = offhand;
        this.originalHealth = health;
        this.originalFoodLevel = food;
        this.warmupTicks = BotConfig.getInstance().warmupTicks;
    }

    public void restore(Player player) {
        // Clear current inventory
        player.getInventory().clear();

        // Restore main inventory
        for (int i = 0; i < originalInventory.size() && i < 36; i++) {
            ItemStack stack = originalInventory.get(i);
            player.getInventory().setItem(i, stack != null ? stack.clone() : null);
        }

        // Restore armor
        ItemStack[] armorContents = new ItemStack[4];
        for (int i = 0; i < originalArmor.size() && i < 4; i++) {
            ItemStack stack = originalArmor.get(i);
            armorContents[i] = stack != null ? stack.clone() : null;
        }
        player.getInventory().setArmorContents(armorContents);

        // Restore offhand
        player.getInventory().setItemInOffHand(originalOffhand != null ? originalOffhand.clone() : null);

        // Restore health and food
        player.setHealth(originalHealth);
        player.setFoodLevel(originalFoodLevel);

        // Teleport back to original location
        player.teleport(originalLocation);
    }

    // Getters
    public UUID getPlayerId() {
        return playerId;
    }

    public Kit getKit() {
        return kit;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public List<ItemStack> getOriginalInventory() {
        return originalInventory;
    }

    public List<ItemStack> getOriginalArmor() {
        return originalArmor;
    }

    public ItemStack getOriginalOffhand() {
        return originalOffhand;
    }

    public float getOriginalHealth() {
        return originalHealth;
    }

    public int getOriginalFoodLevel() {
        return originalFoodLevel;
    }

    public UUID getBotId() {
        return botId;
    }

    public void setBotId(UUID botId) {
        this.botId = botId;
    }

    public BotAI getBotAI() {
        return botAI;
    }

    public void setBotAI(BotAI botAI) {
        this.botAI = botAI;
    }

    public Location getArenaCenter() {
        return arenaCenter;
    }

    public void setArenaCenter(Location arenaCenter) {
        this.arenaCenter = arenaCenter;
    }

    // Bossbar methods
    public void createBossBar(Player player) {
        bossBar = Bukkit.createBossBar(
            kit.getName() + " Bot",
            BarColor.RED,
            BarStyle.SOLID
        );
        bossBar.addPlayer(player);
    }

    public void updateBossBar(float healthPercent, String botName) {
        if (bossBar != null) {
            bossBar.setProgress(Math.max(0, Math.min(1, healthPercent)));
            bossBar.setTitle(botName + " " + String.format("%.0f%%", healthPercent * 100));

            // Update color based on health
            if (healthPercent > 0.5f) {
                bossBar.setColor(BarColor.GREEN);
            } else if (healthPercent > 0.25f) {
                bossBar.setColor(BarColor.YELLOW);
            } else {
                bossBar.setColor(BarColor.RED);
            }
        }
    }

    public void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    // Warmup methods
    public boolean isWarmupComplete() {
        return warmupComplete;
    }

    public int getWarmupTicks() {
        return warmupTicks;
    }

    public void tickWarmup() {
        if (warmupTicks > 0) {
            warmupTicks--;
        }
        if (warmupTicks <= 0 && !warmupComplete) {
            warmupComplete = true;
            fightStartTime = System.currentTimeMillis();
        }
    }

    // Combat stats
    public void addPlayerHit() { playerHits++; }
    public void addBotHit() { botHits++; }
    public void addPlayerDamageDealt(float damage) { playerDamageDealt += damage; }
    public void addPlayerDamageTaken(float damage) { playerDamageTaken += damage; }

    public int getPlayerHits() { return playerHits; }
    public int getBotHits() { return botHits; }
    public float getPlayerDamageDealt() { return playerDamageDealt; }
    public float getPlayerDamageTaken() { return playerDamageTaken; }

    // Track combat by comparing health changes
    public void trackCombat(float currentPlayerHealth, float currentBotHealth) {
        // Player took damage (bot hit player)
        if (currentPlayerHealth < lastPlayerHealth) {
            float damage = lastPlayerHealth - currentPlayerHealth;
            addBotHit();
            addPlayerDamageTaken(damage);
        }

        // Bot took damage (player hit bot)
        if (currentBotHealth < lastBotHealth) {
            float damage = lastBotHealth - currentBotHealth;
            addPlayerHit();
            addPlayerDamageDealt(damage);
        }

        lastPlayerHealth = currentPlayerHealth;
        lastBotHealth = currentBotHealth;
    }

    public void initHealthTracking(float playerHealth, float botHealth) {
        this.lastPlayerHealth = playerHealth;
        this.lastBotHealth = botHealth;
    }

    public long getFightDuration() {
        if (fightStartTime == 0) return 0;
        return System.currentTimeMillis() - fightStartTime;
    }

    public String getFightDurationFormatted() {
        long ms = getFightDuration();
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
