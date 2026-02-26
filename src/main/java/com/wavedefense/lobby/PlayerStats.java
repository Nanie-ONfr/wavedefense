package com.wavedefense.lobby;

import com.wavedefense.WaveDefensePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {
    private static final Map<UUID, Stats> cache = new HashMap<>();

    public static class Stats {
        public int wins = 0;
        public int losses = 0;
        public int kills = 0;
        public int deaths = 0;
        public int gamesPlayed = 0;

        public double getWinRate() {
            if (gamesPlayed == 0) return 0;
            return (double) wins / gamesPlayed * 100;
        }

        public double getKD() {
            if (deaths == 0) return kills;
            return (double) kills / deaths;
        }
    }

    private static File getDataFolder() {
        File folder = new File(WaveDefensePlugin.getInstance().getDataFolder(), "stats");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private static File getStatsFile(UUID playerId) {
        return new File(getDataFolder(), playerId.toString() + ".yml");
    }

    public static Stats getStats(UUID playerId) {
        if (cache.containsKey(playerId)) {
            return cache.get(playerId);
        }

        Stats stats = loadStats(playerId);
        cache.put(playerId, stats);
        return stats;
    }

    public static void addWin(UUID playerId) {
        Stats stats = getStats(playerId);
        stats.wins++;
        stats.gamesPlayed++;
        stats.kills++;
        saveStats(playerId, stats);
    }

    public static void addLoss(UUID playerId) {
        Stats stats = getStats(playerId);
        stats.losses++;
        stats.gamesPlayed++;
        stats.deaths++;
        saveStats(playerId, stats);
    }

    public static void showStats(Player player) {
        Stats stats = getStats(player.getUniqueId());

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== DEINE STATISTIKEN ===")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Spiele: " + stats.gamesPlayed)
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Siege: " + stats.wins)
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Niederlagen: " + stats.losses)
                .color(NamedTextColor.RED));
        player.sendMessage(Component.text("Win-Rate: " + String.format("%.1f", stats.getWinRate()) + "%")
                .color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("K/D: " + String.format("%.2f", stats.getKD()))
                .color(NamedTextColor.LIGHT_PURPLE));
        player.sendMessage(Component.empty());
    }

    private static Stats loadStats(UUID playerId) {
        File file = getStatsFile(playerId);
        if (!file.exists()) {
            return new Stats();
        }

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            Stats stats = new Stats();
            stats.wins = yaml.getInt("wins", 0);
            stats.losses = yaml.getInt("losses", 0);
            stats.kills = yaml.getInt("kills", 0);
            stats.deaths = yaml.getInt("deaths", 0);
            stats.gamesPlayed = yaml.getInt("gamesPlayed", 0);
            return stats;
        } catch (Exception e) {
            return new Stats();
        }
    }

    private static void saveStats(UUID playerId, Stats stats) {
        try {
            File file = getStatsFile(playerId);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("wins", stats.wins);
            yaml.set("losses", stats.losses);
            yaml.set("kills", stats.kills);
            yaml.set("deaths", stats.deaths);
            yaml.set("gamesPlayed", stats.gamesPlayed);

            yaml.save(file);
            cache.put(playerId, stats);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
