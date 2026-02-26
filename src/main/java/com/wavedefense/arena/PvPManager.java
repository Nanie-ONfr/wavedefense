package com.wavedefense.arena;

import com.wavedefense.WaveDefensePlugin;
import com.wavedefense.lobby.PlayerStats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class PvPManager {
    private final WaveDefensePlugin plugin;
    private final Map<Kit, Queue<UUID>> queues = new EnumMap<>(Kit.class);
    private final Map<UUID, PvPSession> activeSessions = new HashMap<>();

    public PvPManager(WaveDefensePlugin plugin) {
        this.plugin = plugin;
        for (Kit kit : Kit.values()) {
            queues.put(kit, new LinkedList<>());
        }
    }

    /**
     * No-arg constructor using the singleton plugin instance.
     */
    public PvPManager() {
        this(WaveDefensePlugin.getInstance());
    }

    // Player joins PvP queue for a kit
    public void joinQueue(Player player, Kit kit) {
        UUID id = player.getUniqueId();
        // Check not already in queue or match
        if (isInPvP(id) || isInQueue(id)) {
            player.sendMessage(Component.text("Du bist bereits in einer Warteschlange oder einem Match!")
                    .color(NamedTextColor.RED));
            return;
        }
        queues.get(kit).add(id);
        player.sendMessage(Component.text("Warteschlange beigetreten für Kit: " + kit.getName())
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Warte auf Gegner...")
                .color(NamedTextColor.YELLOW));

        // Try to match
        tryMatch(kit);
    }

    public void leaveQueue(Player player) {
        UUID id = player.getUniqueId();
        for (Queue<UUID> q : queues.values()) {
            q.remove(id);
        }
        player.sendMessage(Component.text("Warteschlange verlassen.")
                .color(NamedTextColor.YELLOW));
    }

    private void tryMatch(Kit kit) {
        Queue<UUID> queue = queues.get(kit);
        if (queue.size() < 2) return;

        UUID id1 = queue.poll();
        UUID id2 = queue.poll();
        Player p1 = Bukkit.getPlayer(id1);
        Player p2 = Bukkit.getPlayer(id2);
        if (p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline()) return;

        startMatch(p1, p2, kit);
    }

    private void startMatch(Player p1, Player p2, Kit kit) {
        World arenaWorld = ArenaManager.getOrCreateArenaWorld();

        // Hash-based arena position (avoid overlaps)
        int hash = (p1.getUniqueId().hashCode() ^ p2.getUniqueId().hashCode()) & 0x7FFFFFFF;
        int arenaX = (hash % 1000) * 100;
        int arenaZ = ((hash / 1000) % 1000) * 100;
        int arenaY = 100;
        Location center = new Location(arenaWorld, arenaX, arenaY, arenaZ);

        // Build arena (same 41x41 checkerboard as bot arena)
        buildArena(arenaWorld, arenaX, arenaY, arenaZ);

        // Create session
        PvPSession session = new PvPSession(p1, p2, kit, center);
        activeSessions.put(p1.getUniqueId(), session);
        activeSessions.put(p2.getUniqueId(), session);

        // Apply kit to both
        p1.getInventory().clear();
        p2.getInventory().clear();
        kit.applyToPlayer(p1);
        kit.applyToPlayer(p2);

        // Teleport
        Location spawn1 = new Location(arenaWorld, arenaX - 15 + 0.5, arenaY + 1, arenaZ + 0.5, -90, 0);
        Location spawn2 = new Location(arenaWorld, arenaX + 15 + 0.5, arenaY + 1, arenaZ + 0.5, 90, 0);
        p1.teleport(spawn1);
        p2.teleport(spawn2);

        // Messages
        Component msg = Component.text("=== PVP ARENA ===")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD);
        p1.sendMessage(msg);
        p2.sendMessage(msg);
        p1.sendMessage(Component.text("Gegner: " + p2.getName() + " | Kit: " + kit.getName())
                .color(NamedTextColor.YELLOW));
        p2.sendMessage(Component.text("Gegner: " + p1.getName() + " | Kit: " + kit.getName())
                .color(NamedTextColor.YELLOW));
    }

    private void buildArena(World world, int cx, int cy, int cz) {
        // Same checkerboard pattern as ArenaManager
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                Material mat = (Math.abs(x) + Math.abs(z)) % 2 == 0
                        ? Material.WHITE_CONCRETE : Material.LIGHT_GRAY_CONCRETE;
                world.getBlockAt(cx + x, cy, cz + z).setType(mat);
            }
        }
        // Blue platform (player 1)
        for (int x = -17; x <= -13; x++) {
            for (int z = -2; z <= 2; z++) {
                world.getBlockAt(cx + x, cy, cz + z).setType(Material.BLUE_CONCRETE);
            }
        }
        // Red platform (player 2)
        for (int x = 13; x <= 17; x++) {
            for (int z = -2; z <= 2; z++) {
                world.getBlockAt(cx + x, cy, cz + z).setType(Material.RED_CONCRETE);
            }
        }
        // Barrier walls
        for (int x = -21; x <= 21; x++) {
            for (int y = cy + 1; y <= cy + 50; y++) {
                world.getBlockAt(cx + x, y, cz - 21).setType(Material.BARRIER);
                world.getBlockAt(cx + x, y, cz + 21).setType(Material.BARRIER);
            }
        }
        for (int z = -21; z <= 21; z++) {
            for (int y = cy + 1; y <= cy + 50; y++) {
                world.getBlockAt(cx - 21, y, cz + z).setType(Material.BARRIER);
                world.getBlockAt(cx + 21, y, cz + z).setType(Material.BARRIER);
            }
        }
        // Barrier ceiling
        for (int x = -21; x <= 21; x++) {
            for (int z = -21; z <= 21; z++) {
                world.getBlockAt(cx + x, cy + 51, cz + z).setType(Material.BARRIER);
            }
        }
    }

    public void tick(World world) {
        // Only process for the arena world
        if (!ArenaManager.ARENA_WORLD_NAME.equals(world.getName())) {
            return;
        }

        // Tick warmup and check for disconnected players
        for (var entry : new HashMap<>(activeSessions).entrySet()) {
            PvPSession session = entry.getValue();
            if (session.isFinished()) continue;

            session.tickWarmup();
        }
    }

    public void handlePlayerDeath(Player dead) {
        PvPSession session = activeSessions.get(dead.getUniqueId());
        if (session == null) return;

        Player winner = session.getOpponent(dead);
        session.setFinished(true);

        if (winner != null && winner.isOnline()) {
            winner.sendMessage(Component.text("SIEG!")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD));
            winner.showTitle(Title.title(
                    Component.text("SIEG!")
                            .color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            PlayerStats.addWin(winner.getUniqueId());

            // Restore winner
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                session.restore(winner);
                plugin.getLobbyManager().teleportToLobby(winner);
            }, 60L); // 3 seconds
        }

        dead.sendMessage(Component.text("NIEDERLAGE!")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD));
        PlayerStats.addLoss(dead.getUniqueId());

        // Cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cleanupArena(session);
            activeSessions.remove(session.getPlayer1Id());
            activeSessions.remove(session.getPlayer2Id());
        }, 60L);
    }

    public void handlePlayerQuit(Player player) {
        // Remove from queues
        UUID id = player.getUniqueId();
        for (Queue<UUID> q : queues.values()) {
            q.remove(id);
        }

        // If in active match, opponent wins
        PvPSession session = activeSessions.get(id);
        if (session != null && !session.isFinished()) {
            Player opponent = session.getOpponent(player);
            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage(Component.text("Dein Gegner hat das Spiel verlassen. Du gewinnst!")
                        .color(NamedTextColor.GREEN));
                PlayerStats.addWin(opponent.getUniqueId());
                session.restore(opponent);
                plugin.getLobbyManager().teleportToLobby(opponent);
            }
            PlayerStats.addLoss(id);
            session.setFinished(true);
            cleanupArena(session);
            activeSessions.remove(session.getPlayer1Id());
            activeSessions.remove(session.getPlayer2Id());
        }
    }

    private void cleanupArena(PvPSession session) {
        Location center = session.getArenaCenter();
        if (center == null || center.getWorld() == null) return;
        World world = center.getWorld();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        for (int x = -22; x <= 22; x++) {
            for (int y = cy; y <= cy + 52; y++) {
                for (int z = -22; z <= 22; z++) {
                    world.getBlockAt(cx + x, y, cz + z).setType(Material.AIR);
                }
            }
        }
    }

    public boolean isInPvP(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    public boolean isInQueue(UUID playerId) {
        return queues.values().stream().anyMatch(q -> q.contains(playerId));
    }

    public PvPSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }
}
