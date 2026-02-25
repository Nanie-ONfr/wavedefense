package com.wavedefense.lobby;

import com.wavedefense.util.NbtCompat;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {
    private static final String STATS_FOLDER = "wavedefense_stats";
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

    public static Stats getStats(MinecraftServer server, UUID playerId) {
        if (cache.containsKey(playerId)) {
            return cache.get(playerId);
        }

        Stats stats = loadStats(server, playerId);
        cache.put(playerId, stats);
        return stats;
    }

    public static void addWin(MinecraftServer server, UUID playerId) {
        Stats stats = getStats(server, playerId);
        stats.wins++;
        stats.gamesPlayed++;
        stats.kills++;
        saveStats(server, playerId, stats);
    }

    public static void addLoss(MinecraftServer server, UUID playerId) {
        Stats stats = getStats(server, playerId);
        stats.losses++;
        stats.gamesPlayed++;
        stats.deaths++;
        saveStats(server, playerId, stats);
    }

    public static void showStats(ServerPlayerEntity player) {
        MinecraftServer server = player.getCommandSource().getServer();
        Stats stats = getStats(server, player.getUuid());

        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("=== DEINE STATISTIKEN ===").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Spiele: " + stats.gamesPlayed).formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("Siege: " + stats.wins).formatted(Formatting.GREEN), false);
        player.sendMessage(Text.literal("Niederlagen: " + stats.losses).formatted(Formatting.RED), false);
        player.sendMessage(Text.literal("Win-Rate: " + String.format("%.1f", stats.getWinRate()) + "%").formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal("K/D: " + String.format("%.2f", stats.getKD())).formatted(Formatting.LIGHT_PURPLE), false);
        player.sendMessage(Text.literal(""), false);
    }

    private static Stats loadStats(MinecraftServer server, UUID playerId) {
        try {
            Path worldPath = server.getSavePath(WorldSavePath.ROOT);
            File statsFile = worldPath.resolve(STATS_FOLDER).resolve(playerId.toString() + ".dat").toFile();

            if (!statsFile.exists()) {
                return new Stats();
            }

            NbtCompound nbt = NbtIo.readCompressed(statsFile.toPath(), NbtSizeTracker.ofUnlimitedBytes());
            Stats stats = new Stats();
            stats.wins = NbtCompat.getInt(nbt, "wins", 0);
            stats.losses = NbtCompat.getInt(nbt, "losses", 0);
            stats.kills = NbtCompat.getInt(nbt, "kills", 0);
            stats.deaths = NbtCompat.getInt(nbt, "deaths", 0);
            stats.gamesPlayed = NbtCompat.getInt(nbt, "gamesPlayed", 0);
            return stats;
        } catch (IOException e) {
            return new Stats();
        }
    }

    private static void saveStats(MinecraftServer server, UUID playerId, Stats stats) {
        try {
            Path worldPath = server.getSavePath(WorldSavePath.ROOT);
            File statsFolder = worldPath.resolve(STATS_FOLDER).toFile();
            if (!statsFolder.exists()) {
                statsFolder.mkdirs();
            }

            File statsFile = new File(statsFolder, playerId.toString() + ".dat");
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("wins", stats.wins);
            nbt.putInt("losses", stats.losses);
            nbt.putInt("kills", stats.kills);
            nbt.putInt("deaths", stats.deaths);
            nbt.putInt("gamesPlayed", stats.gamesPlayed);

            NbtIo.writeCompressed(nbt, statsFile.toPath());
            cache.put(playerId, stats);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
