package com.wavedefense.arena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.UUID;

public class PvPSession {
    private final UUID player1Id, player2Id;
    private final Kit kit;
    private final Location arenaCenter;

    // Saved state for both players
    private final Location originalLocation1, originalLocation2;
    private final ItemStack[] savedInventory1, savedInventory2;
    private final ItemStack[] savedArmor1, savedArmor2;
    private final ItemStack savedOffhand1, savedOffhand2;
    private final float savedHealth1, savedHealth2;
    private final int savedFood1, savedFood2;

    private int warmupTicks = 60; // 3 second warmup
    private boolean finished = false;

    public PvPSession(Player p1, Player p2, Kit kit, Location arenaCenter) {
        this.player1Id = p1.getUniqueId();
        this.player2Id = p2.getUniqueId();
        this.kit = kit;
        this.arenaCenter = arenaCenter;

        // Save both players' state
        this.originalLocation1 = p1.getLocation().clone();
        this.originalLocation2 = p2.getLocation().clone();
        this.savedInventory1 = cloneContents(p1.getInventory().getContents());
        this.savedInventory2 = cloneContents(p2.getInventory().getContents());
        this.savedArmor1 = cloneContents(p1.getInventory().getArmorContents());
        this.savedArmor2 = cloneContents(p2.getInventory().getArmorContents());
        this.savedOffhand1 = p1.getInventory().getItemInOffHand().clone();
        this.savedOffhand2 = p2.getInventory().getItemInOffHand().clone();
        this.savedHealth1 = (float) p1.getHealth();
        this.savedHealth2 = (float) p2.getHealth();
        this.savedFood1 = p1.getFoodLevel();
        this.savedFood2 = p2.getFoodLevel();
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clone[i] = contents[i] != null ? contents[i].clone() : null;
        }
        return clone;
    }

    public void tickWarmup() {
        if (warmupTicks <= 0 || finished) return;
        warmupTicks--;
        Player p1 = Bukkit.getPlayer(player1Id);
        Player p2 = Bukkit.getPlayer(player2Id);

        if (warmupTicks == 40 || warmupTicks == 20) {
            int seconds = warmupTicks / 20;
            Component msg = Component.text(String.valueOf(seconds))
                    .color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD);
            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(200));
            if (p1 != null) p1.showTitle(Title.title(msg, Component.empty(), times));
            if (p2 != null) p2.showTitle(Title.title(msg, Component.empty(), times));
        }
        if (warmupTicks == 0) {
            Component fight = Component.text("FIGHT!")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD);
            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500));
            if (p1 != null) p1.showTitle(Title.title(fight, Component.empty(), times));
            if (p2 != null) p2.showTitle(Title.title(fight, Component.empty(), times));
        }
    }

    public void restore(Player player) {
        UUID id = player.getUniqueId();
        player.getInventory().clear();

        if (id.equals(player1Id)) {
            player.getInventory().setContents(savedInventory1);
            player.getInventory().setArmorContents(savedArmor1);
            player.getInventory().setItemInOffHand(savedOffhand1);
            player.setHealth(savedHealth1);
            player.setFoodLevel(savedFood1);
            player.teleport(originalLocation1);
        } else {
            player.getInventory().setContents(savedInventory2);
            player.getInventory().setArmorContents(savedArmor2);
            player.getInventory().setItemInOffHand(savedOffhand2);
            player.setHealth(savedHealth2);
            player.setFoodLevel(savedFood2);
            player.teleport(originalLocation2);
        }
    }

    public Player getOpponent(Player player) {
        if (player.getUniqueId().equals(player1Id)) return Bukkit.getPlayer(player2Id);
        return Bukkit.getPlayer(player1Id);
    }

    // Getters
    public UUID getPlayer1Id() { return player1Id; }
    public UUID getPlayer2Id() { return player2Id; }
    public Kit getKit() { return kit; }
    public Location getArenaCenter() { return arenaCenter; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean f) { this.finished = f; }
    public boolean isWarmup() { return warmupTicks > 0; }
}
