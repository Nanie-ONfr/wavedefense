package com.wavedefense.arena;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

/**
 * Survival Arena - Open world with bots spawning every ~100 blocks
 */
public class SurvivalArena {
    private static final int BOT_SPAWN_DISTANCE = 100;
    private static final int BOT_CHECK_RADIUS = 150;
    private static final int MAX_BOTS_PER_PLAYER = 5;
    private static final int SPAWN_COOLDOWN_TICKS = 100; // 5 seconds between spawn attempts

    public static final RegistryKey<World> SURVIVAL_DIMENSION = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("wavedefense", "pvp_arena")
    );

    private final Map<UUID, PlayerData> playerData = new HashMap<>();
    private final Map<UUID, List<UUID>> playerBots = new HashMap<>();
    private final Map<UUID, BotAI> botAIs = new HashMap<>();
    private final Map<UUID, Integer> spawnCooldowns = new HashMap<>();
    private final Random random = new Random();

    public void startSurvival(ServerPlayerEntity player, Kit kit) {
        MinecraftServer server = player.getCommandSource().getServer();
        // Use overworld for survival - custom dimension has terrain issues
        ServerWorld survivalWorld = server.getOverworld();

        // Save player data including original world
        PlayerData data = new PlayerData(player, server);
        playerData.put(player.getUuid(), data);
        playerBots.put(player.getUuid(), new ArrayList<>());

        // Find spawn location - use world spawn
        BlockPos spawnPos = findSafeSpawn(survivalWorld, 0, 0);

        // Clear inventory and apply kit
        player.getInventory().clear();
        kit.applyToPlayer(player);
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);

        // Teleport
        player.teleport(survivalWorld, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5,
                java.util.Set.of(), 0, 0, true);
        player.setVelocity(0, 0, 0);
        player.velocityDirty = true;

        player.sendMessage(Text.literal("=== SURVIVAL ARENA ===")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Kit: " + kit.getName())
                .formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("Bots spawnen alle ~100 Blöcke!")
                .formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal("Besiege sie und sammle Erfahrung!")
                .formatted(Formatting.GREEN), false);
        player.sendMessage(Text.literal("Nutze /wd exit um zu verlassen")
                .formatted(Formatting.GRAY), false);
    }

    public void leaveSurvival(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        MinecraftServer server = player.getCommandSource().getServer();

        // Remove all bots
        List<UUID> bots = playerBots.getOrDefault(playerId, new ArrayList<>());
        ServerWorld survivalWorld = server.getOverworld();
        if (survivalWorld != null) {
            for (UUID botId : bots) {
                var bot = survivalWorld.getEntity(botId);
                if (bot != null) bot.discard();
                botAIs.remove(botId);
            }
        }

        // Restore player and teleport back
        PlayerData data = playerData.get(playerId);
        if (data != null) {
            // Get original world
            ServerWorld originalWorld = server.getWorld(data.originalWorld);
            if (originalWorld == null) {
                originalWorld = server.getOverworld();
            }

            // Teleport back first
            player.teleport(originalWorld, data.originalPos.x, data.originalPos.y, data.originalPos.z,
                    java.util.Set.of(), data.yaw, data.pitch, true);
            player.setVelocity(0, 0, 0);
            player.velocityDirty = true;

            // Then restore inventory
            data.restore(player);
        } else {
            // Fallback: teleport to overworld spawn
            ServerWorld overworld = server.getOverworld();
            int y = overworld.getTopY(Heightmap.Type.MOTION_BLOCKING, 0, 0);
            player.teleport(overworld, 0.5, y + 1, 0.5,
                    java.util.Set.of(), 0, 0, true);
        }

        playerData.remove(playerId);
        playerBots.remove(playerId);
        spawnCooldowns.remove(playerId);

        player.sendMessage(Text.literal("Survival Arena verlassen!")
                .formatted(Formatting.YELLOW), false);
    }

    public void tick(ServerWorld world) {
        // Only tick in overworld (survival uses overworld now)
        if (!world.getRegistryKey().equals(World.OVERWORLD)) return;

        MinecraftServer server = world.getServer();

        for (UUID playerId : new HashSet<>(playerData.keySet())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) continue;
            if (!player.getCommandSource().getWorld().getRegistryKey().equals(World.OVERWORLD)) continue;

            // Use computeIfAbsent to ensure the list is stored (fixes getOrDefault bug)
            List<UUID> bots = playerBots.computeIfAbsent(playerId, k -> new ArrayList<>());

            // Remove dead bots
            bots.removeIf(botId -> {
                var bot = world.getEntity(botId);
                if (bot == null || !bot.isAlive()) {
                    botAIs.remove(botId);
                    player.addExperience(50);
                    return true;
                }
                return false;
            });

            // Decrement spawn cooldown
            int cooldown = spawnCooldowns.getOrDefault(playerId, 0);
            if (cooldown > 0) {
                spawnCooldowns.put(playerId, cooldown - 1);
            } else if (bots.size() < MAX_BOTS_PER_PLAYER) {
                trySpawnBot(player, world, bots);
                spawnCooldowns.put(playerId, SPAWN_COOLDOWN_TICKS);
            }

            // Tick bot AIs
            for (UUID botId : bots) {
                BotAI ai = botAIs.get(botId);
                if (ai != null) ai.tick();
            }
        }
    }

    private void trySpawnBot(ServerPlayerEntity player, ServerWorld world, List<UUID> bots) {
        // Find a position ~100 blocks away
        double angle = random.nextDouble() * Math.PI * 2;
        int distance = BOT_SPAWN_DISTANCE + random.nextInt(50) - 25;

        int spawnX = (int) (player.getX() + Math.cos(angle) * distance);
        int spawnZ = (int) (player.getZ() + Math.sin(angle) * distance);

        BlockPos spawnPos = findSafeSpawn(world, spawnX, spawnZ);
        if (spawnPos == null) return;

        // Check if too close to player
        if (spawnPos.isWithinDistance(player.getBlockPos(), 50)) return;

        // Random kit for bot
        Kit kit = getRandomKit();
        Difficulty difficulty = getRandomDifficulty();

        // Spawn bot
        ZombieEntity bot = EntityType.ZOMBIE.spawn(world, spawnPos, SpawnReason.MOB_SUMMONED);
        if (bot != null) {
            setupBot(bot, player, kit, difficulty);
            bots.add(bot.getUuid());

            BotAI ai = new BotAI(bot, player, kit, difficulty);
            botAIs.put(bot.getUuid(), ai);

            // Give player same kit
            player.sendMessage(Text.literal("Ein " + kit.getName() + " Bot ist in der Nähe erschienen!")
                    .formatted(Formatting.RED), true);
        }
    }

    private void setupBot(ZombieEntity bot, ServerPlayerEntity player, Kit kit, Difficulty difficulty) {
        bot.setCustomName(Text.literal(kit.getName() + " [" + difficulty.getName() + "]")
                .formatted(Formatting.RED, Formatting.BOLD));
        bot.setCustomNameVisible(true);
        bot.setBaby(false);
        bot.setAiDisabled(false);
        bot.setPersistent();

        bot.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(difficulty.getHealth());
        bot.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(difficulty.getMovementSpeed());
        bot.getAttributeInstance(EntityAttributes.FOLLOW_RANGE).setBaseValue(difficulty.getFollowRange());
        bot.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(7.0 * difficulty.getDamageMultiplier());
        bot.setHealth(difficulty.getHealth());

        bot.setTarget(player);
        kit.applyToBot(bot);
    }

    private BlockPos findSafeSpawn(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y < 1) y = 100;
        return new BlockPos(x, y, z);
    }

    private Kit getRandomKit() {
        Kit[] kits = Kit.values();
        return kits[random.nextInt(kits.length)];
    }

    private Difficulty getRandomDifficulty() {
        Difficulty[] diffs = Difficulty.values();
        return diffs[random.nextInt(diffs.length)];
    }

    public boolean isInSurvival(ServerPlayerEntity player) {
        return playerData.containsKey(player.getUuid());
    }

    private static class PlayerData {
        final net.minecraft.util.math.Vec3d originalPos;
        final RegistryKey<World> originalWorld;
        final float yaw;
        final float pitch;
        final List<net.minecraft.item.ItemStack> inventory = new ArrayList<>();
        final List<net.minecraft.item.ItemStack> armor = new ArrayList<>();
        final net.minecraft.item.ItemStack offhand;
        final float health;
        final int food;

        PlayerData(ServerPlayerEntity player, MinecraftServer server) {
            this.originalPos = new net.minecraft.util.math.Vec3d(player.getX(), player.getY(), player.getZ());
            this.originalWorld = player.getCommandSource().getWorld().getRegistryKey();
            this.yaw = player.getYaw();
            this.pitch = player.getPitch();
            this.health = player.getHealth();
            this.food = player.getHungerManager().getFoodLevel();

            for (int i = 0; i < 36; i++) {
                inventory.add(player.getInventory().getStack(i).copy());
            }
            for (int i = 0; i < 4; i++) {
                armor.add(player.getInventory().getStack(36 + i).copy());
            }
            this.offhand = player.getOffHandStack().copy();
        }

        void restore(ServerPlayerEntity player) {
            player.getInventory().clear();
            for (int i = 0; i < inventory.size() && i < 36; i++) {
                player.getInventory().setStack(i, inventory.get(i).copy());
            }
            for (int i = 0; i < armor.size() && i < 4; i++) {
                player.getInventory().setStack(36 + i, armor.get(i).copy());
            }
            player.getInventory().setStack(40, offhand.copy());
            player.setHealth(health);
            player.getHungerManager().setFoodLevel(food);
        }
    }
}
