package com.wavedefense;

import com.wavedefense.arena.ArenaManager;
import com.wavedefense.arena.Difficulty;
import com.wavedefense.arena.Kit;
import com.wavedefense.arena.SurvivalArena;
import com.wavedefense.command.WaveDefenseCommand;
import com.wavedefense.game.WaveManager;
import com.wavedefense.lobby.LobbyManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaveDefenseMod implements ModInitializer {
    public static final String MOD_ID = "wavedefense";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static WaveManager waveManager;
    private static ArenaManager arenaManager;
    private static LobbyManager lobbyManager;
    private static SurvivalArena survivalArena;

    @Override
    public void onInitialize() {
        LOGGER.info("Wave Defense Mod initializing...");

        waveManager = new WaveManager();
        arenaManager = new ArenaManager();
        lobbyManager = new LobbyManager();
        survivalArena = new SurvivalArena();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            WaveDefenseCommand.register(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            waveManager.tick(server);
            // Tick arena for all worlds
            for (ServerWorld world : server.getWorlds()) {
                arenaManager.tick(world);
                survivalArena.tick(world);
            }
        });

        // Handle player join - restore arena sessions
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            arenaManager.onPlayerJoin(handler.getPlayer());
        });

        // Handle right-click on entities (armor stands in lobby)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof ArmorStandEntity armorStand)) return ActionResult.PASS;

            // Check if in lobby
            if (!lobbyManager.isInLobby(serverPlayer)) return ActionResult.PASS;

            // Check if already in arena
            if (arenaManager.isInArena(serverPlayer)) return ActionResult.PASS;

            // Check armor stand name for kit
            Text customName = armorStand.getCustomName();
            if (customName == null) return ActionResult.PASS;

            String name = customName.getString().toUpperCase();
            Kit selectedKit = null;

            for (Kit kit : Kit.values()) {
                if (name.contains(kit.getName().toUpperCase())) {
                    selectedKit = kit;
                    break;
                }
            }

            if (selectedKit == null) return ActionResult.PASS;

            // Check if player is sneaking for difficulty selection
            Difficulty difficulty = Difficulty.MEDIUM;
            if (player.isSneaking()) {
                // Cycle through difficulties based on selected kit stored in player
                difficulty = lobbyManager.cycleDifficulty(serverPlayer.getUuid());
                serverPlayer.sendMessage(Text.literal("Schwierigkeit: " + difficulty.getName())
                        .formatted(getDifficultyColor(difficulty)), true);
                return ActionResult.SUCCESS;
            }

            // Start arena with selected kit and current difficulty
            Difficulty currentDifficulty = lobbyManager.getSelectedDifficulty(serverPlayer.getUuid());
            arenaManager.startArena(serverPlayer, selectedKit, currentDifficulty);
            return ActionResult.SUCCESS;
        });

        LOGGER.info("Wave Defense Mod initialized!");
    }

    public static WaveManager getWaveManager() {
        return waveManager;
    }

    public static ArenaManager getArenaManager() {
        return arenaManager;
    }

    public static LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public static SurvivalArena getSurvivalArena() {
        return survivalArena;
    }

    private static Formatting getDifficultyColor(Difficulty difficulty) {
        return switch (difficulty) {
            case PRACTICE -> Formatting.AQUA;
            case EASY -> Formatting.GREEN;
            case MEDIUM -> Formatting.YELLOW;
            case HARD -> Formatting.RED;
        };
    }
}
