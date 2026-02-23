package com.wavedefense.lobby;

import com.wavedefense.arena.Difficulty;
import com.wavedefense.arena.Kit;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LobbyManager {
    private static final BlockPos LOBBY_SPAWN = new BlockPos(0, 101, 0);
    private boolean lobbyCreated = false;
    private final Map<UUID, Kit> selectedKits = new HashMap<>();
    private final Map<UUID, Difficulty> selectedDifficulties = new HashMap<>();

    // Use The End for lobby (same as arena)
    public static final RegistryKey<World> LOBBY_DIMENSION = World.END;

    public void createLobby(MinecraftServer server) {
        if (lobbyCreated) return;

        ServerWorld world = server.getWorld(LOBBY_DIMENSION);
        if (world == null) {
            world = server.getOverworld();
        }

        BlockPos center = LOBBY_SPAWN;

        // Create lobby platform - circular design
        int radius = 15;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    // Main floor
                    world.setBlockState(center.add(dx, 0, dz), Blocks.POLISHED_BLACKSTONE.getDefaultState());
                    // Light under floor
                    world.setBlockState(center.add(dx, -1, dz), Blocks.SEA_LANTERN.getDefaultState());
                }
            }
        }

        // Center platform (spawn)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(center.add(dx, 0, dz), Blocks.GOLD_BLOCK.getDefaultState());
            }
        }

        // Create kit selection areas in a circle
        Kit[] kits = Kit.values();
        double angleStep = 2 * Math.PI / kits.length;

        for (int i = 0; i < kits.length; i++) {
            Kit kit = kits[i];
            double angle = i * angleStep;
            int kitX = (int) (Math.cos(angle) * 10);
            int kitZ = (int) (Math.sin(angle) * 10);
            BlockPos kitPos = center.add(kitX, 0, kitZ);

            // Kit platform
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    world.setBlockState(kitPos.add(dx, 0, dz), getKitBlockColor(kit));
                }
            }

            // Spawn armor stand with kit name
            spawnKitDisplay(world, kitPos.add(0, 1, 0), kit);
        }

        // Barrier walls around lobby
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dy = 1; dy <= 10; dy++) {
                if (dx == -radius - 1 || dx == radius + 1) {
                    for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                        world.setBlockState(center.add(dx, dy, dz), Blocks.BARRIER.getDefaultState());
                    }
                } else {
                    world.setBlockState(center.add(dx, dy, -radius - 1), Blocks.BARRIER.getDefaultState());
                    world.setBlockState(center.add(dx, dy, radius + 1), Blocks.BARRIER.getDefaultState());
                }
            }
        }

        lobbyCreated = true;
    }

    private net.minecraft.block.BlockState getKitBlockColor(Kit kit) {
        return switch (kit) {
            case MACE -> Blocks.PURPLE_CONCRETE.getDefaultState();
            case SWORD -> Blocks.BLUE_CONCRETE.getDefaultState();
            case AXE -> Blocks.BROWN_CONCRETE.getDefaultState();
            case BOW -> Blocks.YELLOW_CONCRETE.getDefaultState();
            case CRYSTAL -> Blocks.MAGENTA_CONCRETE.getDefaultState();
            case UHC -> Blocks.GREEN_CONCRETE.getDefaultState();
            case SHIELD -> Blocks.CYAN_CONCRETE.getDefaultState();
            case POTION -> Blocks.PINK_CONCRETE.getDefaultState();
        };
    }

    private void spawnKitDisplay(ServerWorld world, BlockPos pos, Kit kit) {
        // Create invisible armor stand with name
        ArmorStandEntity stand = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        stand.setPosition(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
        stand.setCustomName(Text.literal(kit.getName()).formatted(Formatting.GOLD, Formatting.BOLD));
        stand.setCustomNameVisible(true);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        world.spawnEntity(stand);

        // Description armor stand - line 1
        ArmorStandEntity desc = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        desc.setPosition(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        desc.setCustomName(Text.literal("Rechtsklick = Spielen").formatted(Formatting.GREEN));
        desc.setCustomNameVisible(true);
        desc.setInvisible(true);
        desc.setInvulnerable(true);
        desc.setNoGravity(true);
        world.spawnEntity(desc);

        // Description armor stand - line 2
        ArmorStandEntity desc2 = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        desc2.setPosition(pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5);
        desc2.setCustomName(Text.literal("Shift+Klick = Schwierigkeit").formatted(Formatting.GRAY));
        desc2.setCustomNameVisible(true);
        desc2.setInvisible(true);
        desc2.setInvulnerable(true);
        desc2.setNoGravity(true);
        world.spawnEntity(desc2);
    }

    public void teleportToLobby(ServerPlayerEntity player) {
        MinecraftServer server = player.getCommandSource().getServer();
        ServerWorld lobbyWorld = server.getWorld(LOBBY_DIMENSION);
        if (lobbyWorld == null) {
            lobbyWorld = server.getOverworld();
        }

        // Create lobby if not exists
        createLobby(server);

        // Clear inventory and give lobby items
        player.getInventory().clear();

        // Teleport
        player.teleport(lobbyWorld, LOBBY_SPAWN.getX() + 0.5, LOBBY_SPAWN.getY() + 1, LOBBY_SPAWN.getZ() + 0.5,
                java.util.Set.of(), 0, 0, true);

        // Send welcome message
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("=== WAVE DEFENSE PVP ===").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Rechtsklick auf Kit-Schild = Spielen").formatted(Formatting.GREEN), false);
        player.sendMessage(Text.literal("Shift + Rechtsklick = Schwierigkeit ändern").formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("Oder nutze: /wd arena <kit> [easy/medium/hard]").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal(""), false);
    }

    public Kit getSelectedKit(UUID playerId) {
        return selectedKits.getOrDefault(playerId, Kit.SWORD);
    }

    public void setSelectedKit(UUID playerId, Kit kit) {
        selectedKits.put(playerId, kit);
    }

    public BlockPos getLobbySpawn() {
        return LOBBY_SPAWN;
    }

    public boolean isInLobby(ServerPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        int dx = Math.abs(playerPos.getX() - LOBBY_SPAWN.getX());
        int dz = Math.abs(playerPos.getZ() - LOBBY_SPAWN.getZ());
        return dx <= 20 && dz <= 20 && playerPos.getY() >= LOBBY_SPAWN.getY() - 5 && playerPos.getY() <= LOBBY_SPAWN.getY() + 15;
    }

    public Difficulty getSelectedDifficulty(UUID playerId) {
        return selectedDifficulties.getOrDefault(playerId, Difficulty.MEDIUM);
    }

    public void setSelectedDifficulty(UUID playerId, Difficulty difficulty) {
        selectedDifficulties.put(playerId, difficulty);
    }

    public Difficulty cycleDifficulty(UUID playerId) {
        Difficulty current = getSelectedDifficulty(playerId);
        Difficulty next = switch (current) {
            case EASY -> Difficulty.MEDIUM;
            case MEDIUM -> Difficulty.HARD;
            case HARD -> Difficulty.EASY;
        };
        selectedDifficulties.put(playerId, next);
        return next;
    }
}
