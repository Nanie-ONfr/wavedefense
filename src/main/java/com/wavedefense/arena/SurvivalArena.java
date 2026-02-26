package com.wavedefense.arena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Survival Arena - Open world with bots spawning every ~100 blocks
 */
public class SurvivalArena {
    private static final int BOT_SPAWN_DISTANCE = 100;
    private static final int BOT_CHECK_RADIUS = 150;
    private static final int MAX_BOTS_PER_PLAYER = 5;
    private static final int SPAWN_COOLDOWN_TICKS = 100; // 5 seconds between spawn attempts

    private final Map<UUID, PlayerData> playerData = new HashMap<>();
    private final Map<UUID, List<UUID>> playerBots = new HashMap<>();
    private final Map<UUID, BotAI> botAIs = new HashMap<>();
    private final Map<UUID, Integer> spawnCooldowns = new HashMap<>();
    private final Random random = new Random();

    public void startSurvival(Player player, Kit kit) {
        // Use overworld for survival
        World survivalWorld = Bukkit.getWorlds().get(0);

        // Save player data including original world
        PlayerData data = new PlayerData(player);
        playerData.put(player.getUniqueId(), data);
        playerBots.put(player.getUniqueId(), new ArrayList<>());

        // Find spawn location - use world spawn
        Location spawnLoc = findSafeSpawn(survivalWorld, 0, 0);

        // Clear inventory and apply kit
        player.getInventory().clear();
        kit.applyToPlayer(player);
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
        player.setFoodLevel(20);

        // Teleport
        player.teleport(new Location(survivalWorld, spawnLoc.getX() + 0.5, spawnLoc.getY() + 1, spawnLoc.getZ() + 0.5, 0, 0));
        player.setVelocity(new Vector(0, 0, 0));

        player.sendMessage(Component.text("=== SURVIVAL ARENA ===")
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Kit: " + kit.getName())
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Bots spawnen alle ~100 Blöcke!")
                .color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("Besiege sie und sammle Erfahrung!")
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Nutze /wd exit um zu verlassen")
                .color(NamedTextColor.GRAY));
    }

    public void leaveSurvival(Player player) {
        UUID playerId = player.getUniqueId();

        // Remove all bots
        List<UUID> bots = playerBots.getOrDefault(playerId, new ArrayList<>());
        for (UUID botId : bots) {
            Entity bot = Bukkit.getEntity(botId);
            if (bot != null) bot.remove();
            botAIs.remove(botId);
        }

        // Restore player and teleport back
        PlayerData data = playerData.get(playerId);
        if (data != null) {
            // Get original world from stored location
            Location originalLoc = data.originalLocation;
            World originalWorld = originalLoc.getWorld();
            if (originalWorld == null) {
                originalWorld = Bukkit.getWorlds().get(0);
                originalLoc = new Location(originalWorld, originalLoc.getX(), originalLoc.getY(), originalLoc.getZ(),
                        originalLoc.getYaw(), originalLoc.getPitch());
            }

            // Teleport back first
            player.teleport(originalLoc);
            player.setVelocity(new Vector(0, 0, 0));

            // Then restore inventory
            data.restore(player);
        } else {
            // Fallback: teleport to overworld spawn
            World overworld = Bukkit.getWorlds().get(0);
            int y = overworld.getHighestBlockYAt(0, 0);
            player.teleport(new Location(overworld, 0.5, y + 1, 0.5, 0, 0));
        }

        playerData.remove(playerId);
        playerBots.remove(playerId);
        spawnCooldowns.remove(playerId);

        player.sendMessage(Component.text("Survival Arena verlassen!")
                .color(NamedTextColor.YELLOW));
    }

    public void tick(World world) {
        // Only tick in overworld (survival uses overworld now)
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        for (UUID playerId : new HashSet<>(playerData.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) continue;
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) continue;

            // Use computeIfAbsent to ensure the list is stored (fixes getOrDefault bug)
            List<UUID> bots = playerBots.computeIfAbsent(playerId, k -> new ArrayList<>());

            // Remove dead bots
            bots.removeIf(botId -> {
                Entity bot = Bukkit.getEntity(botId);
                if (bot == null || bot.isDead()) {
                    botAIs.remove(botId);
                    player.giveExp(50);
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

    private void trySpawnBot(Player player, World world, List<UUID> bots) {
        // Find a position ~100 blocks away
        double angle = random.nextDouble() * Math.PI * 2;
        int distance = BOT_SPAWN_DISTANCE + random.nextInt(50) - 25;

        int spawnX = (int) (player.getLocation().getX() + Math.cos(angle) * distance);
        int spawnZ = (int) (player.getLocation().getZ() + Math.sin(angle) * distance);

        Location spawnLoc = findSafeSpawn(world, spawnX, spawnZ);
        if (spawnLoc == null) return;

        // Check if too close to player (within 50 blocks)
        if (spawnLoc.distance(player.getLocation()) < 50) return;

        // Random kit for bot
        Kit kit = getRandomKit();
        Difficulty difficulty = getRandomDifficulty();

        // Spawn bot
        Zombie bot = (Zombie) world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
        setupBot(bot, player, kit, difficulty);
        bots.add(bot.getUniqueId());

        BotAI ai = new BotAI(bot, player, kit, difficulty, world);
        botAIs.put(bot.getUniqueId(), ai);

        // Notify player via action bar
        player.sendActionBar(Component.text("Ein " + kit.getName() + " Bot ist in der Nähe erschienen!")
                .color(NamedTextColor.RED));
    }

    private void setupBot(Zombie bot, Player player, Kit kit, Difficulty difficulty) {
        bot.customName(Component.text(kit.getName() + " [" + difficulty.getName() + "]")
                .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        bot.setCustomNameVisible(true);
        bot.setBaby(false);
        bot.setAI(false); // Disable vanilla AI, BotAI controls it
        bot.setRemoveWhenFarAway(false);

        Objects.requireNonNull(bot.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(difficulty.getHealth());
        Objects.requireNonNull(bot.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(difficulty.getMovementSpeed());
        Objects.requireNonNull(bot.getAttribute(Attribute.FOLLOW_RANGE)).setBaseValue(difficulty.getFollowRange());
        Objects.requireNonNull(bot.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(7.0 * difficulty.getDamageMultiplier());
        bot.setHealth(difficulty.getHealth());

        bot.setTarget(player);
        kit.applyToBot(bot);
    }

    private Location findSafeSpawn(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        if (y < 1) y = 100;
        return new Location(world, x, y, z);
    }

    private Kit getRandomKit() {
        Kit[] kits = Kit.values();
        return kits[random.nextInt(kits.length)];
    }

    private Difficulty getRandomDifficulty() {
        Difficulty[] diffs = Difficulty.values();
        return diffs[random.nextInt(diffs.length)];
    }

    public boolean isInSurvival(Player player) {
        return playerData.containsKey(player.getUniqueId());
    }

    private static class PlayerData {
        final Location originalLocation;
        final List<ItemStack> inventory = new ArrayList<>();
        final ItemStack[] armor;
        final ItemStack offhand;
        final double health;
        final int food;

        PlayerData(Player player) {
            this.originalLocation = player.getLocation().clone();
            this.health = player.getHealth();
            this.food = player.getFoodLevel();

            for (int i = 0; i < 36; i++) {
                ItemStack item = player.getInventory().getItem(i);
                inventory.add(item != null ? item.clone() : null);
            }
            this.armor = player.getInventory().getArmorContents().clone();
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null) armor[i] = armor[i].clone();
            }
            ItemStack off = player.getInventory().getItemInOffHand();
            this.offhand = off.getType().isAir() ? null : off.clone();
        }

        void restore(Player player) {
            player.getInventory().clear();
            for (int i = 0; i < inventory.size() && i < 36; i++) {
                ItemStack item = inventory.get(i);
                player.getInventory().setItem(i, item != null ? item.clone() : null);
            }
            // Restore armor
            ItemStack[] armorCopy = new ItemStack[armor.length];
            for (int i = 0; i < armor.length; i++) {
                armorCopy[i] = armor[i] != null ? armor[i].clone() : null;
            }
            player.getInventory().setArmorContents(armorCopy);
            // Restore offhand
            player.getInventory().setItemInOffHand(offhand != null ? offhand.clone() : null);
            player.setHealth(health);
            player.setFoodLevel(food);
        }
    }
}
