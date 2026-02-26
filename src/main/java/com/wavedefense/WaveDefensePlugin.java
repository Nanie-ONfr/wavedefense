package com.wavedefense;

import com.wavedefense.arena.ArenaManager;
import com.wavedefense.arena.Difficulty;
import com.wavedefense.arena.PvPManager;
import com.wavedefense.arena.SurvivalArena;
import com.wavedefense.command.WaveDefenseCommand;
import com.wavedefense.listener.WaveDefenseListener;
import com.wavedefense.lobby.LobbyManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class WaveDefensePlugin extends JavaPlugin {

    private static WaveDefensePlugin instance;

    private ArenaManager arenaManager;
    private LobbyManager lobbyManager;
    private SurvivalArena survivalArena;
    private PvPManager pvpManager;

    public static WaveDefensePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        arenaManager = new ArenaManager();
        lobbyManager = new LobbyManager();
        survivalArena = new SurvivalArena();
        pvpManager = new PvPManager();

        // Register command executor and tab completer
        WaveDefenseCommand commandExecutor = new WaveDefenseCommand(this);
        getCommand("wavedefense").setExecutor(commandExecutor);
        getCommand("wavedefense").setTabCompleter(commandExecutor);

        // Register event listener
        getServer().getPluginManager().registerEvents(new WaveDefenseListener(this), this);

        // Start tick scheduler (runs every tick = 50ms)
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::tick, 0L, 1L);

        getLogger().info("WaveDefense enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("WaveDefense disabled");
    }

    private void tick() {
        for (World world : getServer().getWorlds()) {
            arenaManager.tick(world);
            survivalArena.tick(world);
            pvpManager.tick(world);
        }
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public SurvivalArena getSurvivalArena() {
        return survivalArena;
    }

    public PvPManager getPvPManager() {
        return pvpManager;
    }

    /**
     * Returns a color matching the given difficulty level.
     */
    public static NamedTextColor getDifficultyColor(Difficulty d) {
        return switch (d) {
            case PRACTICE -> NamedTextColor.AQUA;
            case EASY -> NamedTextColor.GREEN;
            case MEDIUM -> NamedTextColor.YELLOW;
            case HARD -> NamedTextColor.RED;
        };
    }
}
