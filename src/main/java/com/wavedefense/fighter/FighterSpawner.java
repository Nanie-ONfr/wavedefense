package com.wavedefense.fighter;

import com.wavedefense.game.GameSession;
import com.wavedefense.game.WaveConfig;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Random;

public class FighterSpawner {
    private final Random random = new Random();

    public void spawnFighters(ServerPlayerEntity player, GameSession session, WaveConfig config, int wave) {
        ServerWorld world = (ServerWorld) player.getCommandSource().getWorld();

        // Fighters appear from wave 5 onwards
        if (wave < 5) return;

        int fighterCount = getFighterCount(wave);
        FighterType[] types = FighterType.values();

        for (int i = 0; i < fighterCount; i++) {
            FighterType type = types[random.nextInt(types.length)];
            BlockPos spawnPos = findSpawnPosition(player, config);

            if (spawnPos != null) {
                ZombieEntity fighter = EntityType.ZOMBIE.spawn(world, spawnPos, SpawnReason.MOB_SUMMONED);

                if (fighter != null) {
                    configureFighter(fighter, type);
                    fighter.setTarget(player);
                    session.addSpawnedMob(fighter);
                }
            }
        }
    }

    private int getFighterCount(int wave) {
        if (wave < 5) return 0;
        if (wave < 7) return 1;
        if (wave < 9) return 2;
        return 3 + (wave - 9);
    }

    private void configureFighter(ZombieEntity fighter, FighterType type) {
        // Set baby to false
        fighter.setBaby(false);

        // Set custom name
        fighter.setCustomName(Text.literal("⚔ " + type.getDisplayName() + " ⚔")
            .formatted(Formatting.RED, Formatting.BOLD));
        fighter.setCustomNameVisible(true);

        // Set attributes
        fighter.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(type.getHealth());
        fighter.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(type.getSpeed());
        fighter.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(8.0);
        fighter.getAttributeInstance(EntityAttributes.FOLLOW_RANGE).setBaseValue(40.0);
        fighter.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4);
        fighter.setHealth((float) type.getHealth());

        // Clear equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            fighter.equipStack(slot, ItemStack.EMPTY);
        }

        // Set equipment based on type
        switch (type) {
            case MACE -> {
                fighter.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.MACE));
                fighter.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                fighter.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                fighter.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                fighter.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
                fighter.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(12.0);
            }
            case SWORD -> {
                fighter.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
                fighter.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                fighter.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                fighter.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                fighter.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                fighter.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(10.0);
                fighter.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.38);
            }
            case AXE -> {
                fighter.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_AXE));
                fighter.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                fighter.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                fighter.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                fighter.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                fighter.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                fighter.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(11.0);
            }
            case BOW -> {
                fighter.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                fighter.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                fighter.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                fighter.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
                fighter.equipStack(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
                fighter.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(6.0);
            }
            case SHIELD -> {
                fighter.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                fighter.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                fighter.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                fighter.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                fighter.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                fighter.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
                fighter.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.7);
                fighter.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(8.0);
            }
            case CRYSTAL -> {
                fighter.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.END_CRYSTAL));
                fighter.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
                fighter.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                fighter.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                fighter.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                fighter.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                fighter.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.4);
            }
            case SMP_UHC -> {
                fighter.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                fighter.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                fighter.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                fighter.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                fighter.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                fighter.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                fighter.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(40.0);
                fighter.setHealth(40.0f);
                fighter.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(9.0);
            }
        }

        // Prevent equipment drop
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            fighter.setEquipmentDropChance(slot, 0.0f);
        }
    }

    private BlockPos findSpawnPosition(ServerPlayerEntity player, WaveConfig config) {
        ServerWorld world = (ServerWorld) player.getCommandSource().getWorld();
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
}
