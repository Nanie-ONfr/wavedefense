package com.wavedefense.listener;

import com.wavedefense.WaveDefensePlugin;
import com.wavedefense.arena.ArenaManager;
import com.wavedefense.arena.BotAI;
import com.wavedefense.arena.PvPManager;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

public class WaveDefenseListener implements Listener {
    private final WaveDefensePlugin plugin;

    public WaveDefenseListener(WaveDefensePlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getArenaManager().onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (plugin.getArenaManager().isInArena(player)) {
            plugin.getArenaManager().handlePlayerDeath(player);
        }
        if (plugin.getPvPManager().isInPvP(player.getUniqueId())) {
            plugin.getPvPManager().handlePlayerDeath(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPvPManager().handlePlayerQuit(player);
        // Arena sessions persist via ArenaDataStorage
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof ArmorStand armorStand)) return;

        Player player = event.getPlayer();
        plugin.getLobbyManager().handleArmorStandInteraction(player, armorStand);
        event.setCancelled(true);
    }

    // Track entity damage for BotAI hurt detection
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        // Find the BotAI for this zombie and notify it
        ArenaManager arenaManager = plugin.getArenaManager();
        BotAI botAI = arenaManager.getBotAIForEntity(zombie.getUniqueId());
        if (botAI != null) {
            botAI.notifyHurt();
        }
    }

    // Prevent item drops in arena
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getArenaManager().isInArena(player) || plugin.getPvPManager().isInPvP(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Handle respawn location for arena players
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (plugin.getArenaManager().isInArena(player) || plugin.getPvPManager().isInPvP(player.getUniqueId())) {
            // Respawn at lobby (arena cleanup handles the rest)
            event.setRespawnLocation(plugin.getLobbyManager().getLobbySpawn());
        }
    }
}
