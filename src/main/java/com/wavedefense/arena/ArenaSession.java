package com.wavedefense.arena;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArenaSession {
    private final UUID playerId;
    private final Kit kit;
    private final Difficulty difficulty;
    private final Vec3d originalPosition;
    private final float originalYaw;
    private final float originalPitch;
    private final List<ItemStack> originalInventory;
    private final List<ItemStack> originalArmor;
    private final ItemStack originalOffhand;
    private final float originalHealth;
    private final int originalFoodLevel;
    private UUID botId;
    private UUID bot2Id;
    private BotAI botAI;
    private BotAI botAI2;
    private BlockPos arenaCenter;
    private net.minecraft.registry.RegistryKey<net.minecraft.world.World> arenaWorld;

    // Combat feedback
    private ServerBossBar bossBar;
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
    public ArenaSession(ServerPlayerEntity player, Kit kit, Difficulty difficulty) {
        this.playerId = player.getUuid();
        this.kit = kit;
        this.difficulty = difficulty;
        this.originalPosition = new Vec3d(player.getX(), player.getY(), player.getZ());
        this.originalYaw = player.getYaw();
        this.originalPitch = player.getPitch();
        this.originalHealth = player.getHealth();
        this.originalFoodLevel = player.getHungerManager().getFoodLevel();
        this.warmupTicks = BotConfig.getInstance().warmupTicks;

        // Save inventory (main inventory has 36 slots)
        this.originalInventory = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            originalInventory.add(player.getInventory().getStack(i).copy());
        }

        // Save armor (slots 36-39 in inventory)
        this.originalArmor = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            originalArmor.add(player.getInventory().getStack(36 + i).copy());
        }

        // Save offhand
        this.originalOffhand = player.getOffHandStack().copy();
    }

    // Constructor for loading from disk
    public ArenaSession(UUID playerId, Kit kit, Difficulty difficulty, double origX, double origY, double origZ,
                        float origYaw, float origPitch, List<ItemStack> inventory,
                        List<ItemStack> armor, ItemStack offhand, float health, int food) {
        this.playerId = playerId;
        this.kit = kit;
        this.difficulty = difficulty;
        this.originalPosition = new Vec3d(origX, origY, origZ);
        this.originalYaw = origYaw;
        this.originalPitch = origPitch;
        this.originalInventory = inventory;
        this.originalArmor = armor;
        this.originalOffhand = offhand;
        this.originalHealth = health;
        this.originalFoodLevel = food;
        this.warmupTicks = BotConfig.getInstance().warmupTicks;
    }

    public void restore(ServerPlayerEntity player) {
        // Clear current inventory
        player.getInventory().clear();

        // Restore main inventory
        for (int i = 0; i < originalInventory.size() && i < 36; i++) {
            player.getInventory().setStack(i, originalInventory.get(i).copy());
        }

        // Restore armor (slots 36-39 in player inventory)
        for (int i = 0; i < originalArmor.size() && i < 4; i++) {
            player.getInventory().setStack(36 + i, originalArmor.get(i).copy());
        }

        // Restore offhand (slot 40)
        player.getInventory().setStack(40, originalOffhand.copy());

        // Restore health and food
        player.setHealth(originalHealth);
        player.getHungerManager().setFoodLevel(originalFoodLevel);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Kit getKit() {
        return kit;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Vec3d getOriginalPosition() {
        return originalPosition;
    }

    public float getOriginalYaw() {
        return originalYaw;
    }

    public float getOriginalPitch() {
        return originalPitch;
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

    public UUID getBot2Id() {
        return bot2Id;
    }

    public void setBot2Id(UUID bot2Id) {
        this.bot2Id = bot2Id;
    }

    public BotAI getBotAI2() {
        return botAI2;
    }

    public void setBotAI2(BotAI botAI2) {
        this.botAI2 = botAI2;
    }

    public BlockPos getArenaCenter() {
        return arenaCenter;
    }

    public void setArenaCenter(BlockPos arenaCenter) {
        this.arenaCenter = arenaCenter;
    }

    public net.minecraft.registry.RegistryKey<net.minecraft.world.World> getArenaWorld() {
        return arenaWorld;
    }

    public void setArenaWorld(net.minecraft.registry.RegistryKey<net.minecraft.world.World> arenaWorld) {
        this.arenaWorld = arenaWorld;
    }

    // Bossbar methods
    public void createBossBar(ServerPlayerEntity player) {
        bossBar = new ServerBossBar(
            Text.literal(kit.getName() + " Bot").formatted(Formatting.RED, Formatting.BOLD),
            BossBar.Color.RED,
            BossBar.Style.PROGRESS
        );
        bossBar.addPlayer(player);
    }

    public void updateBossBar(float healthPercent, String botName) {
        if (bossBar != null) {
            bossBar.setPercent(Math.max(0, Math.min(1, healthPercent)));
            bossBar.setName(Text.literal(botName + " ")
                .formatted(Formatting.RED, Formatting.BOLD)
                .append(Text.literal(String.format("%.0f%%", healthPercent * 100))
                    .formatted(getHealthColor(healthPercent))));
        }
    }

    private Formatting getHealthColor(float percent) {
        if (percent > 0.5f) return Formatting.GREEN;
        if (percent > 0.25f) return Formatting.YELLOW;
        return Formatting.RED;
    }

    public void removeBossBar() {
        if (bossBar != null) {
            bossBar.clearPlayers();
            bossBar = null;
        }
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
