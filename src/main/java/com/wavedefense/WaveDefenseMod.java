package com.wavedefense;

import com.wavedefense.command.WaveDefenseCommand;
import com.wavedefense.game.WaveManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaveDefenseMod implements ModInitializer {
    public static final String MOD_ID = "wavedefense";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static WaveManager waveManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Wave Defense Mod initializing...");

        waveManager = new WaveManager();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            WaveDefenseCommand.register(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            waveManager.tick(server);
        });

        LOGGER.info("Wave Defense Mod initialized!");
    }

    public static WaveManager getWaveManager() {
        return waveManager;
    }
}
