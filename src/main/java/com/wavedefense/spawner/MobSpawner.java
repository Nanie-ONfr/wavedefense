package com.wavedefense.spawner;

import com.wavedefense.game.GameSession;
import com.wavedefense.game.WaveConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MobSpawner {
    private final Random random = new Random();

    private ServerWorld getPlayerWorld(ServerPlayerEntity player) {
        return (ServerWorld) player.getCommandSource().getWorld();
    }

    public void spawnWave(ServerPlayerEntity player, GameSession session, WaveConfig config) {
        ServerWorld world = getPlayerWorld(player);
        int wave = session.getCurrentWave();

        List<EntityType<?>> mobTypes = config.getMobTypesForWave(wave);
        int mobCount = config.getMobCountForWave(wave);

        for (int i = 0; i < mobCount; i++) {
            EntityType<?> mobType = mobTypes.get(random.nextInt(mobTypes.size()));
            BlockPos spawnPos = findSpawnPosition(player, config);

            if (spawnPos != null) {
                MobEntity mob = (MobEntity) mobType.spawn(
                    world,
                    spawnPos,
                    SpawnReason.MOB_SUMMONED
                );

                if (mob != null) {
                    mob.setTarget(player);
                    session.addSpawnedMob(mob);
                }
            }
        }
    }

    private BlockPos findSpawnPosition(ServerPlayerEntity player, WaveConfig config) {
        ServerWorld world = getPlayerWorld(player);
        double playerX = player.getX();
        double playerZ = player.getZ();

        int minRadius = config.getMinSpawnRadius();
        int maxRadius = config.getSpawnRadius();

        for (int attempts = 0; attempts < 20; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);

            int x = (int) (playerX + Math.cos(angle) * distance);
            int z = (int) (playerZ + Math.sin(angle) * distance);

            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.down();

            if (world.getBlockState(below).isSolidBlock(world, below) &&
                world.getBlockState(pos).isAir() &&
                world.getBlockState(pos.up()).isAir()) {
                return pos;
            }
        }

        int x = (int) (playerX + (random.nextDouble() - 0.5) * maxRadius * 2);
        int z = (int) (playerZ + (random.nextDouble() - 0.5) * maxRadius * 2);
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

        return new BlockPos(x, y, z);
    }

    public void cleanupMobs(ServerWorld world, GameSession session) {
        for (UUID mobId : new HashSet<>(session.getSpawnedMobs())) {
            var entity = world.getEntity(mobId);
            if (entity != null) {
                entity.discard();
            }
        }
        session.clearMobs();
    }
}
