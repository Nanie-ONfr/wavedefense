package com.wavedefense.arena;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Advanced PvP AI for arena bots - mimics real player behavior
 */
public class BotAI {
    private final MobEntity bot;
    private final ServerPlayerEntity target;
    private final Kit kit;
    private final Difficulty difficulty;
    private final Random random = new Random();

    // Cooldowns
    private int attackCooldown = 0;
    private int jumpCooldown = 0;
    private int specialCooldown = 0;
    private int strafeCooldown = 0;
    private int sprintResetCooldown = 0;
    private int blockCooldown = 0;
    private int retreatCooldown = 0;

    // State
    private int strafeDirection = 1; // 1 = right, -1 = left
    private boolean isBlocking = false;
    private boolean isRetreating = false;
    private int comboCount = 0;

    // Mace specific
    private boolean preparingSmash = false;
    private double fallStartY = 0;
    private int windChargeCooldown = 0;

    // Bow specific
    private int drawTicks = 0;

    public BotAI(MobEntity bot, ServerPlayerEntity target, Kit kit, Difficulty difficulty) {
        this.bot = bot;
        this.target = target;
        this.kit = kit;
        this.difficulty = difficulty;
    }

    public void tick() {
        if (bot == null || bot.isDead() || target == null || target.isDead()) return;

        // Keep target set
        bot.setTarget(target);

        // Decrement cooldowns
        if (attackCooldown > 0) attackCooldown--;
        if (jumpCooldown > 0) jumpCooldown--;
        if (specialCooldown > 0) specialCooldown--;
        if (strafeCooldown > 0) strafeCooldown--;
        if (sprintResetCooldown > 0) sprintResetCooldown--;
        if (blockCooldown > 0) blockCooldown--;
        if (retreatCooldown > 0) retreatCooldown--;
        if (windChargeCooldown > 0) windChargeCooldown--;

        // Reaction delay based on difficulty
        int reactionChance = switch (difficulty) {
            case EASY -> 40;
            case MEDIUM -> 15;
            case HARD -> 5;
        };
        if (random.nextInt(100) < reactionChance) return;

        double distance = bot.distanceTo(target);

        // Low health retreat behavior
        if (bot.getHealth() < 6.0f && retreatCooldown == 0) {
            isRetreating = true;
            retreatCooldown = 60; // 3 seconds
        }
        if (bot.getHealth() > 10.0f) {
            isRetreating = false;
        }

        // Execute kit-specific behavior
        switch (kit) {
            case MACE -> tickMace(distance);
            case SWORD -> tickSword(distance);
            case AXE -> tickAxe(distance);
            case BOW -> tickBow(distance);
            case CRYSTAL -> tickCrystal(distance);
            case UHC -> tickUHC(distance);
            case SHIELD -> tickShield(distance);
        }

        // Common movement behaviors
        tickMovement(distance);
    }

    private void tickMovement(double distance) {
        Vec3d toTarget = new Vec3d(
            target.getX() - bot.getX(),
            0,
            target.getZ() - bot.getZ()
        ).normalize();

        // Strafing - change direction randomly
        if (strafeCooldown == 0 && distance < 6.0) {
            if (random.nextFloat() < 0.1f) {
                strafeDirection *= -1;
            }
            strafeCooldown = 10 + random.nextInt(20);
        }

        // Calculate strafe vector (perpendicular to target direction)
        Vec3d strafeVec = new Vec3d(-toTarget.z, 0, toTarget.x).multiply(strafeDirection);

        // Retreating behavior
        if (isRetreating && distance < 8.0) {
            // Move away from target
            double speed = difficulty.getMovementSpeed() * 0.8;
            bot.setVelocity(
                -toTarget.x * speed + strafeVec.x * speed * 0.3,
                bot.getVelocity().y,
                -toTarget.z * speed + strafeVec.z * speed * 0.3
            );
            bot.velocityDirty = true;
            return;
        }

        // Approach with strafing when in combat range
        if (distance > 2.5 && distance < 10.0) {
            double speed = difficulty.getMovementSpeed();
            double strafeAmount = 0.4;

            bot.setVelocity(
                toTarget.x * speed + strafeVec.x * speed * strafeAmount,
                bot.getVelocity().y,
                toTarget.z * speed + strafeVec.z * speed * strafeAmount
            );
            bot.velocityDirty = true;
        }

        // Circle strafing when very close
        if (distance < 2.5 && distance > 1.5) {
            double speed = difficulty.getMovementSpeed() * 0.6;
            bot.setVelocity(
                strafeVec.x * speed,
                bot.getVelocity().y,
                strafeVec.z * speed
            );
            bot.velocityDirty = true;
        }
    }

    // SWORD - W-tap combos, jump crits
    private void tickSword(double distance) {
        if (distance < 3.5 && attackCooldown == 0) {
            // Jump crit
            if (bot.isOnGround() && jumpCooldown == 0 && random.nextFloat() < 0.4f) {
                bot.setVelocity(bot.getVelocity().x, 0.42, bot.getVelocity().z);
                bot.velocityDirty = true;
                jumpCooldown = 15;
            }

            // Attack
            bot.swingHand(Hand.MAIN_HAND);
            float damage = (float) (7.0 * difficulty.getDamageMultiplier());

            // Crit bonus if falling
            if (bot.getVelocity().y < -0.1) {
                damage *= 1.5f;
            }

            ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();
            target.damage(world, bot.getDamageSources().mobAttack(bot), damage);

            // W-tap / Sprint reset for knockback
            if (sprintResetCooldown == 0 && random.nextFloat() < 0.3f) {
                Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
                target.addVelocity(kb.x * 0.5, 0.35, kb.z * 0.5);
                target.velocityDirty = true;
                sprintResetCooldown = 8;
            }

            comboCount++;
            attackCooldown = 10; // 0.5 seconds

            // Reset combo after a while
            if (comboCount > 3) comboCount = 0;
        }
    }

    // AXE - Shield breaking, heavy hits
    private void tickAxe(double distance) {
        if (distance < 3.5 && attackCooldown == 0) {
            // Jump for crits
            if (bot.isOnGround() && jumpCooldown == 0 && random.nextFloat() < 0.5f) {
                bot.setVelocity(bot.getVelocity().x, 0.42, bot.getVelocity().z);
                bot.velocityDirty = true;
                jumpCooldown = 20;
            }

            bot.swingHand(Hand.MAIN_HAND);
            float damage = (float) (9.0 * difficulty.getDamageMultiplier());

            // Crit bonus
            if (bot.getVelocity().y < -0.1) {
                damage *= 1.5f;
            }

            ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();
            target.damage(world, bot.getDamageSources().mobAttack(bot), damage);

            // Extra knockback if target is blocking (axe shield break)
            if (target.isBlocking()) {
                Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
                target.addVelocity(kb.x * 0.8, 0.4, kb.z * 0.8);
                target.velocityDirty = true;
            }

            attackCooldown = 16; // Slower than sword
        }
    }

    // MACE - Wind charge combos
    private void tickMace(double distance) {
        // Wind charge jump attack
        if (distance < 10.0 && distance > 3.0 && windChargeCooldown == 0 && bot.isOnGround()) {
            Vec3d toTarget = new Vec3d(
                target.getX() - bot.getX(),
                0,
                target.getZ() - bot.getZ()
            ).normalize();

            bot.setVelocity(toTarget.x * 0.5, 1.2, toTarget.z * 0.5);
            bot.velocityDirty = true;
            windChargeCooldown = 100;
            preparingSmash = true;
            fallStartY = bot.getY() + 5;
        }

        // Smash attack on landing
        if (preparingSmash && bot.isOnGround()) {
            preparingSmash = false;
            if (distance < 5.0) {
                double fallDist = Math.max(0, fallStartY - bot.getY());
                float damage = (float) ((8.0 + fallDist * 2.0) * difficulty.getDamageMultiplier());
                damage = Math.min(damage, 25.0f);

                bot.swingHand(Hand.MAIN_HAND);
                ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();
                target.damage(world, bot.getDamageSources().mobAttack(bot), damage);

                // Big knockback
                Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
                target.addVelocity(kb.x * 0.8, 0.5, kb.z * 0.8);
                target.velocityDirty = true;
            }
        }

        // Normal melee if wind charge on cooldown
        if (!preparingSmash && distance < 3.0 && attackCooldown == 0) {
            bot.swingHand(Hand.MAIN_HAND);
            float damage = (float) (6.0 * difficulty.getDamageMultiplier());
            ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();
            target.damage(world, bot.getDamageSources().mobAttack(bot), damage);
            attackCooldown = 12;
        }
    }

    // BOW - Predictive shooting, kiting
    private void tickBow(double distance) {
        // Kite at range
        if (distance < 8.0) {
            isRetreating = true;
        } else if (distance > 15.0) {
            isRetreating = false;
        }

        // Shoot arrows
        if (distance > 6.0 && distance < 30.0 && specialCooldown == 0) {
            drawTicks++;
            int drawTime = switch (difficulty) {
                case EASY -> 30;
                case MEDIUM -> 22;
                case HARD -> 15;
            };

            if (drawTicks >= drawTime) {
                shootArrow();
                drawTicks = 0;
                specialCooldown = switch (difficulty) {
                    case EASY -> 40;
                    case MEDIUM -> 25;
                    case HARD -> 15;
                };
            }
        } else {
            drawTicks = 0;
            // Melee if too close
            if (distance < 3.0 && attackCooldown == 0) {
                bot.swingHand(Hand.MAIN_HAND);
                float damage = (float) (4.0 * difficulty.getDamageMultiplier());
                ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();
                target.damage(world, bot.getDamageSources().mobAttack(bot), damage);
                attackCooldown = 15;
            }
        }
    }

    // CRYSTAL - Place and detonate simulation
    private void tickCrystal(double distance) {
        if (distance > 2.0 && distance < 8.0 && specialCooldown == 0) {
            // Simulate crystal explosion
            ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();
            float damage = (float) (8.0 * difficulty.getDamageMultiplier());

            // Self damage too (but less)
            bot.damage(world, world.getDamageSources().explosion(null, null), damage * 0.3f);
            target.damage(world, world.getDamageSources().explosion(null, bot), damage);

            // Knockback
            Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
            target.addVelocity(kb.x * 0.6, 0.4, kb.z * 0.6);
            target.velocityDirty = true;

            specialCooldown = switch (difficulty) {
                case EASY -> 60;
                case MEDIUM -> 40;
                case HARD -> 25;
            };
        }

        // Melee attacks
        if (distance < 3.0 && attackCooldown == 0) {
            bot.swingHand(Hand.MAIN_HAND);
            float damage = (float) (6.0 * difficulty.getDamageMultiplier());
            ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();
            target.damage(world, bot.getDamageSources().mobAttack(bot), damage);
            attackCooldown = 12;
        }
    }

    // UHC - Rod combos, fishing rod pulling
    private void tickUHC(double distance) {
        // Rod pull at medium range
        if (distance > 4.0 && distance < 12.0 && specialCooldown == 0) {
            // Pull target closer
            Vec3d pullDir = new Vec3d(bot.getX() - target.getX(), 0.2, bot.getZ() - target.getZ()).normalize();
            target.addVelocity(pullDir.x * 0.5, pullDir.y, pullDir.z * 0.5);
            target.velocityDirty = true;
            specialCooldown = 30;
        }

        // Standard sword combat
        if (distance < 3.5 && attackCooldown == 0) {
            if (bot.isOnGround() && jumpCooldown == 0 && random.nextFloat() < 0.35f) {
                bot.setVelocity(bot.getVelocity().x, 0.42, bot.getVelocity().z);
                bot.velocityDirty = true;
                jumpCooldown = 15;
            }

            bot.swingHand(Hand.MAIN_HAND);
            float damage = (float) (7.0 * difficulty.getDamageMultiplier());
            if (bot.getVelocity().y < -0.1) damage *= 1.5f;

            ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();
            target.damage(world, bot.getDamageSources().mobAttack(bot), damage);
            attackCooldown = 10;
        }
    }

    // SHIELD - Block and counter
    private void tickShield(double distance) {
        // Block when target is attacking
        if (distance < 5.0 && !isBlocking && blockCooldown == 0) {
            // Simulate blocking by reducing incoming damage
            if (random.nextFloat() < 0.3f) {
                isBlocking = true;
                blockCooldown = 40;
            }
        }

        if (isBlocking && blockCooldown < 20) {
            isBlocking = false;
        }

        // Counter attack after blocking
        if (!isBlocking && distance < 3.5 && attackCooldown == 0) {
            // Shield bash knockback
            if (random.nextFloat() < 0.2f) {
                Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
                target.addVelocity(kb.x * 0.7, 0.3, kb.z * 0.7);
                target.velocityDirty = true;
            }

            bot.swingHand(Hand.MAIN_HAND);
            float damage = (float) (6.0 * difficulty.getDamageMultiplier());
            ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();
            target.damage(world, bot.getDamageSources().mobAttack(bot), damage);
            attackCooldown = 12;
        }
    }

    private void shootArrow() {
        ServerWorld world = (ServerWorld) target.getCommandSource().getWorld();

        // Predict target movement
        Vec3d targetVel = target.getVelocity();
        double dist = bot.distanceTo(target);

        // Better prediction based on difficulty
        double predictionMultiplier = switch (difficulty) {
            case EASY -> 0.5;
            case MEDIUM -> 0.8;
            case HARD -> 1.0;
        };

        int ticks = (int) (dist / 2.5);
        Vec3d predicted = new Vec3d(
            target.getX() + targetVel.x * ticks * predictionMultiplier,
            target.getY() + target.getHeight() * 0.7,
            target.getZ() + targetVel.z * ticks * predictionMultiplier
        );

        Vec3d dir = predicted.subtract(bot.getX(), bot.getEyeY(), bot.getZ()).normalize();

        // Add some inaccuracy for easier difficulties
        double inaccuracy = switch (difficulty) {
            case EASY -> 8.0;
            case MEDIUM -> 4.0;
            case HARD -> 1.0;
        };

        ArrowEntity arrow = new ArrowEntity(world, bot, null, null);
        arrow.setPosition(bot.getX(), bot.getEyeY(), bot.getZ());
        arrow.setVelocity(dir.x, dir.y + 0.12, dir.z, 2.5f, (float) inaccuracy);
        arrow.setDamage(6.0 * difficulty.getDamageMultiplier());

        world.spawnEntity(arrow);
    }

    public boolean isBlocking() {
        return isBlocking;
    }
}
