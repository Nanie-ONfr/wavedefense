package com.wavedefense.command;

import com.wavedefense.WaveDefensePlugin;
import com.wavedefense.arena.*;
import com.wavedefense.lobby.LobbyManager;
import com.wavedefense.lobby.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.*;

public class WaveDefenseCommand implements CommandExecutor, TabCompleter {
    private final WaveDefensePlugin plugin;
    // Track last kit/difficulty per player for rematch
    private final Map<UUID, Kit> lastKit = new HashMap<>();
    private final Map<UUID, Difficulty> lastDifficulty = new HashMap<>();

    public WaveDefenseCommand(WaveDefensePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können diesen Befehl nutzen!").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) { showHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "arena", "play" -> handleArena(player, args);
            case "pvp" -> handlePvP(player, args);
            case "leave" -> handleLeave(player);
            case "lobby" -> handleLobby(player);
            case "survival" -> handleSurvival(player, args);
            case "exit" -> handleExit(player);
            case "stats" -> PlayerStats.showStats(player);
            case "kit" -> handleKit(player, args);
            case "rematch" -> handleRematch(player);
            case "config" -> handleConfig(player, args);
            case "help" -> showHelp(player);
            default -> showHelp(player);
        }
        return true;
    }

    private void handleArena(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Nutze: /wd arena <kit> [schwierigkeit]").color(NamedTextColor.RED));
            player.sendMessage(Component.text("Kits: " + Arrays.stream(Kit.values()).map(Kit::getName).collect(Collectors.joining(", "))).color(NamedTextColor.GRAY));
            return;
        }
        Kit kit = parseKit(args[1]);
        if (kit == null) {
            player.sendMessage(Component.text("Unbekanntes Kit: " + args[1]).color(NamedTextColor.RED));
            return;
        }
        Difficulty diff = args.length >= 3 ? parseDifficulty(args[2]) : Difficulty.MEDIUM;
        if (diff == null) {
            player.sendMessage(Component.text("Unbekannte Schwierigkeit: " + args[2]).color(NamedTextColor.RED));
            return;
        }
        lastKit.put(player.getUniqueId(), kit);
        lastDifficulty.put(player.getUniqueId(), diff);
        plugin.getArenaManager().startArena(player, kit, diff);
    }

    private void handlePvP(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Nutze: /wd pvp <kit>").color(NamedTextColor.RED));
            return;
        }
        Kit kit = parseKit(args[1]);
        if (kit == null) {
            player.sendMessage(Component.text("Unbekanntes Kit: " + args[1]).color(NamedTextColor.RED));
            return;
        }
        plugin.getPvPManager().joinQueue(player, kit);
    }

    private void handleLeave(Player player) {
        if (plugin.getArenaManager().isInArena(player)) {
            plugin.getArenaManager().leaveArena(player);
        } else if (plugin.getPvPManager().isInPvP(player.getUniqueId())) {
            player.sendMessage(Component.text("Du kannst ein PvP-Match nicht verlassen!").color(NamedTextColor.RED));
        } else if (plugin.getPvPManager().isInQueue(player.getUniqueId())) {
            plugin.getPvPManager().leaveQueue(player);
        } else {
            player.sendMessage(Component.text("Du bist in keiner Arena!").color(NamedTextColor.RED));
        }
    }

    private void handleLobby(Player player) {
        plugin.getLobbyManager().teleportToLobby(player);
    }

    private void handleSurvival(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Nutze: /wd survival <kit>").color(NamedTextColor.RED));
            return;
        }
        Kit kit = parseKit(args[1]);
        if (kit == null) {
            player.sendMessage(Component.text("Unbekanntes Kit: " + args[1]).color(NamedTextColor.RED));
            return;
        }
        plugin.getSurvivalArena().startSurvival(player, kit);
    }

    private void handleExit(Player player) {
        if (plugin.getSurvivalArena().isInSurvival(player)) {
            plugin.getSurvivalArena().leaveSurvival(player);
        } else {
            player.sendMessage(Component.text("Du bist nicht im Survival-Modus!").color(NamedTextColor.RED));
        }
    }

    private void handleKit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Nutze: /wd kit <kit>").color(NamedTextColor.RED));
            return;
        }
        Kit kit = parseKit(args[1]);
        if (kit == null) {
            player.sendMessage(Component.text("Unbekanntes Kit: " + args[1]).color(NamedTextColor.RED));
            return;
        }
        kit.applyToPlayer(player);
        player.sendMessage(Component.text("Kit " + kit.getName() + " erhalten!").color(NamedTextColor.GREEN));
    }

    private void handleRematch(Player player) {
        Kit kit = lastKit.get(player.getUniqueId());
        Difficulty diff = lastDifficulty.get(player.getUniqueId());
        if (kit == null) {
            player.sendMessage(Component.text("Kein letztes Match gefunden!").color(NamedTextColor.RED));
            return;
        }
        if (diff == null) diff = Difficulty.MEDIUM;
        plugin.getArenaManager().startArena(player, kit, diff);
    }

    private void handleConfig(Player player, String[] args) {
        if (!player.hasPermission("wavedefense.admin")) {
            player.sendMessage(Component.text("Keine Berechtigung!").color(NamedTextColor.RED));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
            new BotConfig().reload();
            player.sendMessage(Component.text("Config neu geladen!").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Nutze: /wd config reload").color(NamedTextColor.YELLOW));
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("=== Wave Defense Hilfe ===").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("/wd arena <kit> [schwierigkeit]").color(NamedTextColor.YELLOW).append(Component.text(" - PvE Arena").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wd pvp <kit>").color(NamedTextColor.YELLOW).append(Component.text(" - PvP Queue").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wd survival <kit>").color(NamedTextColor.YELLOW).append(Component.text(" - Survival Modus").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wd leave").color(NamedTextColor.YELLOW).append(Component.text(" - Arena verlassen").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wd exit").color(NamedTextColor.YELLOW).append(Component.text(" - Survival verlassen").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wd lobby").color(NamedTextColor.YELLOW).append(Component.text(" - Zur Lobby").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wd stats").color(NamedTextColor.YELLOW).append(Component.text(" - Statistiken").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wd rematch").color(NamedTextColor.YELLOW).append(Component.text(" - Letztes Match wiederholen").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wd kit <kit>").color(NamedTextColor.YELLOW).append(Component.text(" - Kit erhalten").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text(""));
    }

    private Kit parseKit(String name) {
        try { return Kit.valueOf(name.toUpperCase()); } catch (Exception e) { return null; }
    }

    private Difficulty parseDifficulty(String name) {
        try { return Difficulty.valueOf(name.toUpperCase()); } catch (Exception e) { return null; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(args[0], List.of("arena", "pvp", "survival", "leave", "exit", "lobby", "stats", "rematch", "kit", "config", "help"));
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("arena") || sub.equals("play") || sub.equals("pvp") || sub.equals("survival") || sub.equals("kit")) {
                return filterStartsWith(args[1], Arrays.stream(Kit.values()).map(k -> k.name().toLowerCase()).toList());
            }
            if (sub.equals("config")) return filterStartsWith(args[1], List.of("reload"));
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("play"))) {
            return filterStartsWith(args[2], Arrays.stream(Difficulty.values()).map(d -> d.name().toLowerCase()).toList());
        }
        return List.of();
    }

    private List<String> filterStartsWith(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.startsWith(lower)).collect(Collectors.toList());
    }
}
