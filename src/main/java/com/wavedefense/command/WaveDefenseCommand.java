package com.wavedefense.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.wavedefense.WaveDefenseMod;
import com.wavedefense.arena.ArenaManager;
import com.wavedefense.arena.BotConfig;
import com.wavedefense.arena.Difficulty;
import com.wavedefense.arena.Kit;
import com.wavedefense.arena.SurvivalArena;
import com.wavedefense.game.WaveConfig;
import com.wavedefense.game.WaveManager;
import com.wavedefense.lobby.LobbyManager;
import com.wavedefense.lobby.PlayerStats;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WaveDefenseCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wavedefense")
            .executes(context -> executeHelp(context.getSource()))
            .then(CommandManager.literal("start")
                .executes(context -> executeStart(context.getSource(), 10))
                .then(CommandManager.argument("waves", IntegerArgumentType.integer(1, 100))
                    .executes(context -> executeStart(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "waves")
                    ))
                )
            )
            .then(CommandManager.literal("stop")
                .executes(context -> executeStop(context.getSource()))
            )
            .then(CommandManager.literal("config")
                .then(CommandManager.literal("radius")
                    .then(CommandManager.argument("value", IntegerArgumentType.integer(10, 50))
                        .executes(context -> executeConfigRadius(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "value")
                        ))
                    )
                )
                .then(CommandManager.literal("delay")
                    .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 60))
                        .executes(context -> executeConfigDelay(
                            context.getSource(),
                            IntegerArgumentType.getInteger(context, "value")
                        ))
                    )
                )
            )
            .then(CommandManager.literal("status")
                .executes(context -> executeStatus(context.getSource()))
            )
            // Arena commands
            .then(CommandManager.literal("arena")
                .executes(context -> executeArenaHelp(context.getSource()))
                .then(CommandManager.literal("mace")
                    .executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.HARD))))
                .then(CommandManager.literal("sword")
                    .executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.HARD))))
                .then(CommandManager.literal("axe")
                    .executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.HARD))))
                .then(CommandManager.literal("bow")
                    .executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.HARD))))
                .then(CommandManager.literal("crystal")
                    .executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.HARD))))
                .then(CommandManager.literal("uhc")
                    .executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.HARD))))
                .then(CommandManager.literal("shield")
                    .executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.HARD))))
                .then(CommandManager.literal("potion")
                    .executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.HARD))))
            )
            .then(CommandManager.literal("leave")
                .executes(context -> executeLeave(context.getSource()))
            )
            // New lobby commands
            .then(CommandManager.literal("lobby")
                .executes(context -> executeLobby(context.getSource()))
            )
            .then(CommandManager.literal("play")
                .then(CommandManager.literal("mace")
                    .executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.HARD))))
                .then(CommandManager.literal("sword")
                    .executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.HARD))))
                .then(CommandManager.literal("axe")
                    .executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.HARD))))
                .then(CommandManager.literal("bow")
                    .executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.HARD))))
                .then(CommandManager.literal("crystal")
                    .executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.HARD))))
                .then(CommandManager.literal("uhc")
                    .executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.HARD))))
                .then(CommandManager.literal("shield")
                    .executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.HARD))))
                .then(CommandManager.literal("potion")
                    .executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.HARD))))
            )
            .then(CommandManager.literal("stats")
                .executes(context -> executeStats(context.getSource()))
            )
            .then(CommandManager.literal("kit")
                .then(CommandManager.literal("mace")
                    .executes(context -> executeKit(context.getSource(), Kit.MACE)))
                .then(CommandManager.literal("sword")
                    .executes(context -> executeKit(context.getSource(), Kit.SWORD)))
                .then(CommandManager.literal("axe")
                    .executes(context -> executeKit(context.getSource(), Kit.AXE)))
                .then(CommandManager.literal("bow")
                    .executes(context -> executeKit(context.getSource(), Kit.BOW)))
                .then(CommandManager.literal("crystal")
                    .executes(context -> executeKit(context.getSource(), Kit.CRYSTAL)))
                .then(CommandManager.literal("uhc")
                    .executes(context -> executeKit(context.getSource(), Kit.UHC)))
                .then(CommandManager.literal("shield")
                    .executes(context -> executeKit(context.getSource(), Kit.SHIELD)))
                .then(CommandManager.literal("potion")
                    .executes(context -> executeKit(context.getSource(), Kit.POTION)))
            )
            .then(CommandManager.literal("survival")
                .executes(context -> executeSurvivalHelp(context.getSource()))
                .then(CommandManager.literal("mace").executes(context -> executeSurvival(context.getSource(), Kit.MACE)))
                .then(CommandManager.literal("sword").executes(context -> executeSurvival(context.getSource(), Kit.SWORD)))
                .then(CommandManager.literal("axe").executes(context -> executeSurvival(context.getSource(), Kit.AXE)))
                .then(CommandManager.literal("bow").executes(context -> executeSurvival(context.getSource(), Kit.BOW)))
                .then(CommandManager.literal("crystal").executes(context -> executeSurvival(context.getSource(), Kit.CRYSTAL)))
                .then(CommandManager.literal("uhc").executes(context -> executeSurvival(context.getSource(), Kit.UHC)))
                .then(CommandManager.literal("shield").executes(context -> executeSurvival(context.getSource(), Kit.SHIELD)))
                .then(CommandManager.literal("potion").executes(context -> executeSurvival(context.getSource(), Kit.POTION)))
            )
            .then(CommandManager.literal("exit")
                .executes(context -> executeExitSurvival(context.getSource()))
            )
            .then(CommandManager.literal("rematch")
                .executes(context -> executeRematch(context.getSource()))
            )
            .then(CommandManager.literal("config")
                .then(CommandManager.literal("reload")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(context -> executeConfigReload(context.getSource()))
                )
            )
        );

        // Short alias
        dispatcher.register(CommandManager.literal("wd")
            .executes(context -> executeHelp(context.getSource()))
            .then(CommandManager.literal("start")
                .executes(context -> executeStart(context.getSource(), 10))
                .then(CommandManager.argument("waves", IntegerArgumentType.integer(1, 100))
                    .executes(context -> executeStart(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "waves")
                    ))
                )
            )
            .then(CommandManager.literal("stop")
                .executes(context -> executeStop(context.getSource()))
            )
            .then(CommandManager.literal("status")
                .executes(context -> executeStatus(context.getSource()))
            )
            .then(CommandManager.literal("arena")
                .executes(context -> executeArenaHelp(context.getSource()))
                .then(CommandManager.literal("mace")
                    .executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.MACE, Difficulty.HARD))))
                .then(CommandManager.literal("sword")
                    .executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.SWORD, Difficulty.HARD))))
                .then(CommandManager.literal("axe")
                    .executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.AXE, Difficulty.HARD))))
                .then(CommandManager.literal("bow")
                    .executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.BOW, Difficulty.HARD))))
                .then(CommandManager.literal("crystal")
                    .executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.CRYSTAL, Difficulty.HARD))))
                .then(CommandManager.literal("uhc")
                    .executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.UHC, Difficulty.HARD))))
                .then(CommandManager.literal("shield")
                    .executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.SHIELD, Difficulty.HARD))))
                .then(CommandManager.literal("potion")
                    .executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executeArena(context.getSource(), Kit.POTION, Difficulty.HARD))))
            )
            .then(CommandManager.literal("leave")
                .executes(context -> executeLeave(context.getSource()))
            )
            .then(CommandManager.literal("lobby")
                .executes(context -> executeLobby(context.getSource()))
            )
            .then(CommandManager.literal("play")
                .then(CommandManager.literal("mace")
                    .executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.MACE, Difficulty.HARD))))
                .then(CommandManager.literal("sword")
                    .executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.SWORD, Difficulty.HARD))))
                .then(CommandManager.literal("axe")
                    .executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.AXE, Difficulty.HARD))))
                .then(CommandManager.literal("bow")
                    .executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.BOW, Difficulty.HARD))))
                .then(CommandManager.literal("crystal")
                    .executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.CRYSTAL, Difficulty.HARD))))
                .then(CommandManager.literal("uhc")
                    .executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.UHC, Difficulty.HARD))))
                .then(CommandManager.literal("shield")
                    .executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.SHIELD, Difficulty.HARD))))
                .then(CommandManager.literal("potion")
                    .executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.MEDIUM))
                    .then(CommandManager.literal("practice").executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.PRACTICE)))
                    .then(CommandManager.literal("easy").executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.EASY)))
                    .then(CommandManager.literal("medium").executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.MEDIUM)))
                    .then(CommandManager.literal("hard").executes(context -> executePlay(context.getSource(), Kit.POTION, Difficulty.HARD))))
            )
            .then(CommandManager.literal("stats")
                .executes(context -> executeStats(context.getSource()))
            )
            .then(CommandManager.literal("kit")
                .then(CommandManager.literal("mace")
                    .executes(context -> executeKit(context.getSource(), Kit.MACE)))
                .then(CommandManager.literal("sword")
                    .executes(context -> executeKit(context.getSource(), Kit.SWORD)))
                .then(CommandManager.literal("axe")
                    .executes(context -> executeKit(context.getSource(), Kit.AXE)))
                .then(CommandManager.literal("bow")
                    .executes(context -> executeKit(context.getSource(), Kit.BOW)))
                .then(CommandManager.literal("crystal")
                    .executes(context -> executeKit(context.getSource(), Kit.CRYSTAL)))
                .then(CommandManager.literal("uhc")
                    .executes(context -> executeKit(context.getSource(), Kit.UHC)))
                .then(CommandManager.literal("shield")
                    .executes(context -> executeKit(context.getSource(), Kit.SHIELD)))
                .then(CommandManager.literal("potion")
                    .executes(context -> executeKit(context.getSource(), Kit.POTION)))
            )
            .then(CommandManager.literal("survival")
                .executes(context -> executeSurvivalHelp(context.getSource()))
                .then(CommandManager.literal("mace").executes(context -> executeSurvival(context.getSource(), Kit.MACE)))
                .then(CommandManager.literal("sword").executes(context -> executeSurvival(context.getSource(), Kit.SWORD)))
                .then(CommandManager.literal("axe").executes(context -> executeSurvival(context.getSource(), Kit.AXE)))
                .then(CommandManager.literal("bow").executes(context -> executeSurvival(context.getSource(), Kit.BOW)))
                .then(CommandManager.literal("crystal").executes(context -> executeSurvival(context.getSource(), Kit.CRYSTAL)))
                .then(CommandManager.literal("uhc").executes(context -> executeSurvival(context.getSource(), Kit.UHC)))
                .then(CommandManager.literal("shield").executes(context -> executeSurvival(context.getSource(), Kit.SHIELD)))
                .then(CommandManager.literal("potion").executes(context -> executeSurvival(context.getSource(), Kit.POTION)))
            )
            .then(CommandManager.literal("exit")
                .executes(context -> executeExitSurvival(context.getSource()))
            )
            .then(CommandManager.literal("rematch")
                .executes(context -> executeRematch(context.getSource()))
            )
            .then(CommandManager.literal("config")
                .then(CommandManager.literal("reload")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(context -> executeConfigReload(context.getSource()))
                )
            )
        );
    }

    private static int executeRematch(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        ArenaManager arenaManager = WaveDefenseMod.getArenaManager();

        if (arenaManager.isInArena(player)) {
            source.sendError(Text.literal("Du bist bereits in einer Arena! Nutze /wd leave zuerst."));
            return 0;
        }

        if (arenaManager.rematch(player)) {
            return 1;
        }
        return 0;
    }

    private static int executeConfigReload(ServerCommandSource source) {
        BotConfig.getInstance().reload();
        source.sendFeedback(() -> Text.literal("Bot-Konfiguration neu geladen!")
            .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int executeHelp(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("===============================")
            .formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("      WAVE DEFENSE HILFE")
            .formatted(Formatting.GOLD, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("===============================")
            .formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal(""), false);

        // Arena Commands
        source.sendFeedback(() -> Text.literal("PvP Arena:").formatted(Formatting.AQUA, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("  /wd arena <kit> [easy/medium/hard]")
            .formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("  /wd play <kit> [difficulty]")
            .formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("  /wd leave").append(Text.literal(" - Arena verlassen")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("  /wd rematch").append(Text.literal(" - Letztes Match wiederholen")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal(""), false);

        // Survival Commands
        source.sendFeedback(() -> Text.literal("Survival Arena:").formatted(Formatting.AQUA, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("  /wd survival <kit>").append(Text.literal(" - Survival starten")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("  /wd exit").append(Text.literal(" - Survival verlassen")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal(""), false);

        // Wave Defense Commands
        source.sendFeedback(() -> Text.literal("Wave Defense:").formatted(Formatting.AQUA, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("  /wd start [wellen]").append(Text.literal(" - Spiel starten")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("  /wd stop").append(Text.literal(" - Spiel stoppen")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal(""), false);

        // Other Commands
        source.sendFeedback(() -> Text.literal("Andere:").formatted(Formatting.AQUA, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("  /wd lobby").append(Text.literal(" - Zur Lobby teleportieren")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("  /wd stats").append(Text.literal(" - Statistiken anzeigen")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("  /wd kit <kit>").append(Text.literal(" - Kit erhalten")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("  /wd status").append(Text.literal(" - Aktueller Status")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("  /wd config reload").append(Text.literal(" - Config neu laden")
            .formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal(""), false);

        // Kits
        source.sendFeedback(() -> Text.literal("Verfugbare Kits:").formatted(Formatting.AQUA, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("  mace, sword, axe, bow, crystal, uhc, shield, potion")
            .formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal(""), false);

        return 1;
    }

    private static int executeSurvivalHelp(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("=== Survival Arena ===")
            .formatted(Formatting.GOLD, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("/wd survival <kit>").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("Kits: mace, sword, axe, bow, crystal, uhc, shield, potion")
            .formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("Beispiel: /wd survival sword")
            .formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int executeSurvival(ServerCommandSource source, Kit kit) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        SurvivalArena survivalArena = WaveDefenseMod.getSurvivalArena();

        if (survivalArena.isInSurvival(player)) {
            source.sendError(Text.literal("Du bist bereits in der Survival Arena!"));
            return 0;
        }

        survivalArena.startSurvival(player, kit);
        return 1;
    }

    private static int executeExitSurvival(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        SurvivalArena survivalArena = WaveDefenseMod.getSurvivalArena();

        if (!survivalArena.isInSurvival(player)) {
            source.sendError(Text.literal("Du bist nicht in der Survival Arena!"));
            return 0;
        }

        survivalArena.leaveSurvival(player);
        return 1;
    }

    private static int executeKit(ServerCommandSource source, Kit kit) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        kit.applyToPlayer(player);
        source.sendFeedback(() -> Text.literal("Kit " + kit.getName() + " erhalten!")
            .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int executeStart(ServerCommandSource source, int waves) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        WaveManager manager = WaveDefenseMod.getWaveManager();

        if (manager.startGame(player, waves)) {
            return 1;
        }
        return 0;
    }

    private static int executeStop(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        WaveManager manager = WaveDefenseMod.getWaveManager();

        if (manager.stopGame(player)) {
            return 1;
        }
        return 0;
    }

    private static int executeConfigRadius(ServerCommandSource source, int value) {
        WaveManager manager = WaveDefenseMod.getWaveManager();
        WaveConfig config = manager.getConfig();

        config.setSpawnRadius(value);
        source.sendFeedback(() -> Text.literal("Spawn-Radius auf " + value + " Blöcke gesetzt")
            .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int executeConfigDelay(ServerCommandSource source, int value) {
        WaveManager manager = WaveDefenseMod.getWaveManager();
        WaveConfig config = manager.getConfig();

        config.setDelayBetweenWaves(value);
        source.sendFeedback(() -> Text.literal("Pause zwischen Wellen auf " + value + " Sekunden gesetzt")
            .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int executeStatus(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        WaveManager manager = WaveDefenseMod.getWaveManager();
        ArenaManager arenaManager = WaveDefenseMod.getArenaManager();

        // Check arena status
        if (arenaManager.isInArena(player)) {
            var session = arenaManager.getSession(player);
            source.sendFeedback(() -> Text.literal("=== Arena Status ===")
                .formatted(Formatting.GOLD), false);
            source.sendFeedback(() -> Text.literal("Kit: " + session.getKit().getName())
                .formatted(Formatting.AQUA), false);
            source.sendFeedback(() -> Text.literal("Nutze /wd leave um zu verlassen")
                .formatted(Formatting.GRAY), false);
            return 1;
        }

        if (!manager.hasActiveSession(player)) {
            source.sendFeedback(() -> Text.literal("Kein aktives Spiel. Nutze /wd start oder /wd arena <kit>")
                .formatted(Formatting.YELLOW), false);
            return 1;
        }

        var session = manager.getSession(player);
        source.sendFeedback(() -> Text.literal("=== Wave Defense Status ===")
            .formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("Welle: " + session.getCurrentWave() + "/" + session.getTotalWaves())
            .formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("Verbleibende Mobs: " + session.getSpawnedMobs().size())
            .formatted(Formatting.RED), false);
        source.sendFeedback(() -> Text.literal("Status: " + session.getState().name())
            .formatted(Formatting.GRAY), false);

        return 1;
    }

    private static int executeArenaHelp(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("=== Arena Kits ===")
            .formatted(Formatting.GOLD, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("/wd arena <kit> [easy/medium/hard]").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("mace").formatted(Formatting.AQUA)
            .append(Text.literal(" - Mace + Wind Charges").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("sword").formatted(Formatting.AQUA)
            .append(Text.literal(" - Diamond Sword + Shield").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("axe").formatted(Formatting.AQUA)
            .append(Text.literal(" - Diamond Axe + Shield").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("bow").formatted(Formatting.AQUA)
            .append(Text.literal(" - Power Bow + Infinity").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("crystal").formatted(Formatting.AQUA)
            .append(Text.literal(" - Crystals + Obsidian + Totems").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("uhc").formatted(Formatting.AQUA)
            .append(Text.literal(" - UHC Loadout").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("shield").formatted(Formatting.AQUA)
            .append(Text.literal(" - Sword + Shield Tank").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("potion").formatted(Formatting.AQUA)
            .append(Text.literal(" - Splash Potions + Speed + Strength").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("=== Schwierigkeiten ===")
            .formatted(Formatting.GOLD, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal("easy").formatted(Formatting.GREEN)
            .append(Text.literal(" - 10 HP, langsam, wenig Schaden").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("medium").formatted(Formatting.YELLOW)
            .append(Text.literal(" - 20 HP, normal (Standard)").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("hard").formatted(Formatting.RED)
            .append(Text.literal(" - 30 HP, schnell, viel Schaden").formatted(Formatting.GRAY)), false);
        source.sendFeedback(() -> Text.literal("practice").formatted(Formatting.LIGHT_PURPLE)
            .append(Text.literal(" - Kein Schaden, Training").formatted(Formatting.GRAY)), false);
        return 1;
    }

    private static int executeArena(ServerCommandSource source, Kit kit, Difficulty difficulty) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        ArenaManager arenaManager = WaveDefenseMod.getArenaManager();

        if (arenaManager.startArena(player, kit, difficulty)) {
            return 1;
        }
        return 0;
    }

    private static int executeLeave(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        ArenaManager arenaManager = WaveDefenseMod.getArenaManager();

        if (arenaManager.leaveArena(player)) {
            // Teleport back to lobby
            LobbyManager lobbyManager = WaveDefenseMod.getLobbyManager();
            lobbyManager.teleportToLobby(player);
            return 1;
        }
        return 0;
    }

    private static int executeLobby(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        ArenaManager arenaManager = WaveDefenseMod.getArenaManager();

        // Check if in arena
        if (arenaManager.isInArena(player)) {
            source.sendError(Text.literal("Du musst erst die Arena verlassen! Nutze /wd leave"));
            return 0;
        }

        LobbyManager lobbyManager = WaveDefenseMod.getLobbyManager();
        lobbyManager.teleportToLobby(player);
        return 1;
    }

    private static int executePlay(ServerCommandSource source, Kit kit, Difficulty difficulty) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        ArenaManager arenaManager = WaveDefenseMod.getArenaManager();

        // Start arena with selected kit and difficulty
        if (arenaManager.startArena(player, kit, difficulty)) {
            return 1;
        }
        return 0;
    }

    private static int executeStats(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Nur Spieler können diesen Befehl nutzen!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        PlayerStats.showStats(player);
        return 1;
    }
}
