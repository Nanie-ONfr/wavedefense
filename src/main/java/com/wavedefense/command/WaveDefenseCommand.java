package com.wavedefense.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.wavedefense.WaveDefenseMod;
import com.wavedefense.game.WaveConfig;
import com.wavedefense.game.WaveManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WaveDefenseCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wavedefense")
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
        );
    }

    private static int executeStart(ServerCommandSource source, int waves) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command can only be executed by a player!"));
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
            source.sendError(Text.literal("This command can only be executed by a player!"));
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
        source.sendFeedback(() -> Text.literal("Spawn radius set to " + value + " blocks")
            .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int executeConfigDelay(ServerCommandSource source, int value) {
        WaveManager manager = WaveDefenseMod.getWaveManager();
        WaveConfig config = manager.getConfig();

        config.setDelayBetweenWaves(value);
        source.sendFeedback(() -> Text.literal("Delay between waves set to " + value + " seconds")
            .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int executeStatus(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command can only be executed by a player!"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        WaveManager manager = WaveDefenseMod.getWaveManager();

        if (!manager.hasActiveSession(player)) {
            source.sendFeedback(() -> Text.literal("No active game. Use /wavedefense start to begin!")
                .formatted(Formatting.YELLOW), false);
            return 1;
        }

        var session = manager.getSession(player);
        source.sendFeedback(() -> Text.literal("=== Wave Defense Status ===")
            .formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("Wave: " + session.getCurrentWave() + "/" + session.getTotalWaves())
            .formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("Mobs remaining: " + session.getSpawnedMobs().size())
            .formatted(Formatting.RED), false);
        source.sendFeedback(() -> Text.literal("State: " + session.getState().name())
            .formatted(Formatting.GRAY), false);

        return 1;
    }
}
