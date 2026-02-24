package com.wavedefense.arena;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Advanced PvP AI for arena bots - mimics real player behavior
 * Optimized for realistic 1.21 combat mechanics
 */
public class BotAI {
    private final MobEntity bot;
    private ServerPlayerEntity target;
    private final Kit kit;
    private final Difficulty difficulty;

    // Cached difficulty values (computed once, used every tick)
    private final double movementSpeed;
    private final double damageMultiplier;
    private final int reactionChance;

    // Cooldowns (in ticks)
    private int attackCooldown = 0;
    private int jumpCooldown = 0;
    private int specialCooldown = 0;
    private int strafeCooldown = 0;
    private int sprintResetCooldown = 0;
    private int blockCooldown = 0;
    private int retreatCooldown = 0;
    private int healCooldown = 0;
    private int dodgeCooldown = 0;
    private int reactionDelay = 0;

    // State
    private int strafeDirection = 1;
    private boolean isBlocking = false;
    private boolean isRetreating = false;
    private int comboCount = 0;
    private int ticksSinceLastHit = 0;
    private double lastTargetHealth = 20.0;
    private Vec3d lastTargetPos = Vec3d.ZERO;
    private Vec3d lastTargetVelocity = Vec3d.ZERO;

    // Movement patterns
    private int currentPattern = 0;
    private int patternTicks = 0;
    private static final int[][] STRAFE_PATTERNS = {
        {1, 1, 1, -1, -1, -1},           // Simple alternating
        {1, 1, -1, 1, -1, -1},           // Unpredictable
        {1, -1, 1, -1, 1, -1},           // Fast switches
        {1, 1, 1, 1, -1, -1, -1, -1},    // Long holds
        {1, -1, -1, 1, 1, -1}            // Mixed
    };

    // Mace specific
    private boolean preparingSmash = false;
    private double fallStartY = 0;
    private int windChargeCooldown = 0;

    // Bow specific
    private int drawTicks = 0;
    private boolean isDrawing = false;

    // Combat tracking
    private int hitsTaken = 0;
    private int hitsLanded = 0;
    private long lastDamageTime = 0;

    public BotAI(MobEntity bot, ServerPlayerEntity target, Kit kit, Difficulty difficulty) {
        this.bot = bot;
        this.target = target;
        this.kit = kit;
        this.difficulty = difficulty;

        // Cache difficulty values to avoid recomputing every tick
        this.movementSpeed = difficulty.getMovementSpeed();
        this.damageMultiplier = difficulty.getDamageMultiplier();
        this.reactionChance = switch (difficulty) {
            case PRACTICE -> 70;
            case EASY -> 35;
            case MEDIUM -> 12;
            case HARD -> 3;
        };

        this.currentPattern = ThreadLocalRandom.current().nextInt(STRAFE_PATTERNS.length);
    }

    public void setTarget(ServerPlayerEntity newTarget) {
        this.target = newTarget;
    }

    public void tick() {
        if (bot == null || bot.isDead()) return;
        if (target == null || target.isDead()) return;

        // Keep target set
        bot.setTarget(target);

        // Make bot look at target (important for Minecraft melee hit detection)
        bot.getLookControl().lookAt(target, 30.0f, 30.0f);

        // Track target movement for prediction
        Vec3d currentPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        lastTargetVelocity = currentPos.subtract(lastTargetPos);
        lastTargetPos = currentPos;

        // Decrement all cooldowns
        decrementCooldowns();

        // Reaction delay - makes bot feel more human
        if (reactionDelay > 0) {
            reactionDelay--;
            return;
        }

        // Random reaction delays based on difficulty
        if (ThreadLocalRandom.current().nextInt(100) < getReactionChance()) {
            reactionDelay = ThreadLocalRandom.current().nextInt(difficulty.getReactionDelayTicks() / 2);
            return;
        }

        double distance = bot.distanceTo(target);
        ticksSinceLastHit++;

        // Check if we took damage - dodge/react
        if (bot.hurtTime > 0 && dodgeCooldown == 0) {
            performDodge(distance);
        }

        // Healing behavior - eat gapple when low
        if (shouldHeal() && healCooldown == 0) {
            performHeal();
        }

        // Low health retreat behavior
        updateRetreatState();

        // Execute kit-specific behavior
        switch (kit) {
            case MACE -> tickMace(distance);
            case SWORD -> tickSword(distance);
            case AXE -> tickAxe(distance);
            case BOW -> tickBow(distance);
            case CRYSTAL -> tickCrystal(distance);
            case UHC -> tickUHC(distance);
            case SHIELD -> tickShield(distance);
            case POTION -> tickPotion(distance);
        }

        // Common movement behaviors
        tickMovement(distance);

        // Track if target took damage
        if (target.getHealth() < lastTargetHealth) {
            hitsLanded++;
            ticksSinceLastHit = 0;
        }
        lastTargetHealth = target.getHealth();
    }

    private void decrementCooldowns() {
        if (attackCooldown > 0) attackCooldown--;
        if (jumpCooldown > 0) jumpCooldown--;
        if (specialCooldown > 0) specialCooldown--;
        if (strafeCooldown > 0) strafeCooldown--;
        if (sprintResetCooldown > 0) sprintResetCooldown--;
        if (blockCooldown > 0) blockCooldown--;
        if (retreatCooldown > 0) retreatCooldown--;
        if (healCooldown > 0) healCooldown--;
        if (dodgeCooldown > 0) dodgeCooldown--;
        if (windChargeCooldown > 0) windChargeCooldown--;
    }

    private int getReactionChance() {
        return reactionChance;
    }

    private void performDodge(double distance) {
        if (distance > 5.0) return;

        // Jump back or strafe quickly
        Vec3d awayFromTarget = new Vec3d(
            bot.getX() - target.getX(),
            0,
            bot.getZ() - target.getZ()
        ).normalize();

        double dodgeChance = switch (difficulty) {
            case PRACTICE -> 0.0;
            case EASY -> 0.1;
            case MEDIUM -> 0.25;
            case HARD -> 0.45;
        };

        if (ThreadLocalRandom.current().nextDouble() < dodgeChance) {
            // Strafe dodge
            Vec3d strafeVec = new Vec3d(-awayFromTarget.z, 0, awayFromTarget.x);
            int dir = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
            bot.setVelocity(
                strafeVec.x * 0.4 * dir + awayFromTarget.x * 0.2,
                0.1,
                strafeVec.z * 0.4 * dir + awayFromTarget.z * 0.2
            );
            bot.velocityDirty = true;
            strafeDirection *= -1;
        }

        dodgeCooldown = 15;
        hitsTaken++;
    }

    private boolean shouldHeal() {
        float healthPercent = bot.getHealth() / bot.getMaxHealth();
        return switch (difficulty) {
            case PRACTICE -> false;
            case EASY -> healthPercent < 0.25;
            case MEDIUM -> healthPercent < 0.35;
            case HARD -> healthPercent < 0.5;
        };
    }

    private void performHeal() {
        // Simulate eating a gapple
        float healAmount = switch (difficulty) {
            case PRACTICE -> 2.0f;
            case EASY -> 4.0f;
            case MEDIUM -> 6.0f;
            case HARD -> 8.0f;
        };

        bot.heal(healAmount);

        // Add absorption effect
        bot.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 120 * 20, 0));

        // Visual/audio feedback
        ServerWorld world = (ServerWorld) bot.getEntityWorld();
        world.playSound(null, bot.getX(), bot.getY(), bot.getZ(),
            SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.HOSTILE, 1.0f, 1.0f);

        healCooldown = switch (difficulty) {
            case PRACTICE -> 400; // 20 seconds
            case EASY -> 200;     // 10 seconds
            case MEDIUM -> 140;   // 7 seconds
            case HARD -> 80;      // 4 seconds
        };
    }

    private void updateRetreatState() {
        float healthPercent = bot.getHealth() / bot.getMaxHealth();

        if (healthPercent < 0.3f && retreatCooldown == 0) {
            isRetreating = true;
            retreatCooldown = 80;
        }
        if (healthPercent > 0.5f || retreatCooldown == 0) {
            isRetreating = false;
        }
    }

    private void tickMovement(double distance) {
        Vec3d toTarget = new Vec3d(
            target.getX() - bot.getX(),
            0,
            target.getZ() - bot.getZ()
        ).normalize();

        // Update strafe pattern
        patternTicks++;
        int[] pattern = STRAFE_PATTERNS[currentPattern];
        if (patternTicks >= 8) {
            patternTicks = 0;
            int patternIndex = (int) ((System.currentTimeMillis() / 150) % pattern.length);
            strafeDirection = pattern[patternIndex];

            // Occasionally switch patterns
            if (ThreadLocalRandom.current().nextFloat() < 0.05f) {
                currentPattern = ThreadLocalRandom.current().nextInt(STRAFE_PATTERNS.length);
            }
        }

        // Calculate strafe vector
        Vec3d strafeVec = new Vec3d(-toTarget.z, 0, toTarget.x).multiply(strafeDirection);

        // Retreating behavior
        if (isRetreating && distance < 10.0) {
            double speed = movementSpeed * 0.85;
            bot.setVelocity(
                -toTarget.x * speed + strafeVec.x * speed * 0.4,
                bot.getVelocity().y,
                -toTarget.z * speed + strafeVec.z * speed * 0.4
            );
            bot.velocityDirty = true;
            return;
        }

        // Approach with strafing
        if (distance > 3.0 && distance < 12.0) {
            double speed = movementSpeed;
            double strafeAmount = switch (difficulty) {
                case PRACTICE -> 0.1;
                case EASY -> 0.25;
                case MEDIUM -> 0.4;
                case HARD -> 0.55;
            };

            // Add some randomness to movement
            double noise = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1;

            bot.setVelocity(
                toTarget.x * speed + strafeVec.x * speed * strafeAmount + noise,
                bot.getVelocity().y,
                toTarget.z * speed + strafeVec.z * speed * strafeAmount + noise
            );
            bot.velocityDirty = true;
        }

        // Circle strafing when close
        if (distance < 3.0 && distance > 1.5) {
            double speed = movementSpeed * 0.65;
            bot.setVelocity(
                strafeVec.x * speed + toTarget.x * speed * 0.2,
                bot.getVelocity().y,
                strafeVec.z * speed + toTarget.z * speed * 0.2
            );
            bot.velocityDirty = true;
        }
    }

    // SWORD - W-tap combos, jump crits, sprint resets
    private void tickSword(double distance) {
        if (distance < 3.5 && attackCooldown == 0) {
            // Crit jump
            boolean shouldJump = bot.isOnGround() && jumpCooldown == 0;
            double critChance = switch (difficulty) {
                case PRACTICE -> 0.05;
                case EASY -> 0.2;
                case MEDIUM -> 0.4;
                case HARD -> 0.6;
            };

            if (shouldJump && ThreadLocalRandom.current().nextDouble() < critChance) {
                bot.setVelocity(bot.getVelocity().x, 0.42, bot.getVelocity().z);
                bot.velocityDirty = true;
                jumpCooldown = 12;
            }

            // Attack
            performMeleeAttack(7.0f, distance);

            // W-tap / Sprint reset for extra knockback
            double wtapChance = switch (difficulty) {
                case PRACTICE -> 0.0;
                case EASY -> 0.15;
                case MEDIUM -> 0.35;
                case HARD -> 0.55;
            };

            if (sprintResetCooldown == 0 && ThreadLocalRandom.current().nextDouble() < wtapChance && comboCount > 0) {
                Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
                double kbStrength = switch (difficulty) {
                    case PRACTICE -> 0.2;
                    case EASY -> 0.35;
                    case MEDIUM -> 0.45;
                    case HARD -> 0.55;
                };
                target.addVelocity(kb.x * kbStrength, 0.38, kb.z * kbStrength);
                target.velocityDirty = true;
                sprintResetCooldown = 6;
            }

            comboCount++;
            attackCooldown = 10;

            // Reset combo if too long between hits
            if (ticksSinceLastHit > 30) comboCount = 0;
        }
    }

    // AXE - Shield breaking, heavy crits
    private void tickAxe(double distance) {
        if (distance < 3.5 && attackCooldown == 0) {
            // Always try to crit with axe
            if (bot.isOnGround() && jumpCooldown == 0) {
                double critChance = switch (difficulty) {
                    case PRACTICE -> 0.1;
                    case EASY -> 0.35;
                    case MEDIUM -> 0.55;
                    case HARD -> 0.75;
                };
                if (ThreadLocalRandom.current().nextDouble() < critChance) {
                    bot.setVelocity(bot.getVelocity().x, 0.42, bot.getVelocity().z);
                    bot.velocityDirty = true;
                    jumpCooldown = 18;
                }
            }

            performMeleeAttack(9.0f, distance);

            // Shield break knockback
            if (target.isBlocking()) {
                Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
                target.addVelocity(kb.x * 0.7, 0.45, kb.z * 0.7);
                target.velocityDirty = true;

                // Play shield disable sound
                ServerWorld world = (ServerWorld) bot.getEntityWorld();
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }

            attackCooldown = 16;
        }
    }

    // MACE - Wind charge jumps, smash attacks
    private void tickMace(double distance) {
        // Wind charge launch
        if (distance < 12.0 && distance > 4.0 && windChargeCooldown == 0 && bot.isOnGround()) {
            Vec3d toTarget = new Vec3d(
                target.getX() - bot.getX(),
                0,
                target.getZ() - bot.getZ()
            ).normalize();

            double launchPower = switch (difficulty) {
                case PRACTICE -> 0.5;
                case EASY -> 0.8;
                case MEDIUM -> 1.0;
                case HARD -> 1.3;
            };

            bot.setVelocity(toTarget.x * 0.5, launchPower, toTarget.z * 0.5);
            bot.velocityDirty = true;
            windChargeCooldown = switch (difficulty) {
                case PRACTICE -> 200;
                case EASY -> 120;
                case MEDIUM -> 90;
                case HARD -> 60;
            };
            preparingSmash = true;
            fallStartY = bot.getY() + 5;

            // Wind charge sound
            ServerWorld world = (ServerWorld) bot.getEntityWorld();
            world.playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }

        // Smash attack on landing
        if (preparingSmash && bot.isOnGround()) {
            preparingSmash = false;
            if (distance < 6.0) {
                double fallDist = Math.max(0, fallStartY - bot.getY());
                float damage = (float) ((8.0 + fallDist * 2.5) * damageMultiplier);
                damage = Math.min(damage, 28.0f);

                bot.swingHand(Hand.MAIN_HAND);
                ServerWorld world = (ServerWorld) bot.getEntityWorld();
                target.damage(world, bot.getDamageSources().mobAttack(bot), damage);

                // Big knockback
                Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
                target.addVelocity(kb.x * 0.9, 0.55, kb.z * 0.9);
                target.velocityDirty = true;

                // Ground impact particles
                world.spawnParticles(ParticleTypes.EXPLOSION, bot.getX(), bot.getY(), bot.getZ(),
                    5, 1.0, 0.5, 1.0, 0.1);
            }
        }

        // Normal melee if wind charge on cooldown
        if (!preparingSmash && distance < 3.0 && attackCooldown == 0) {
            performMeleeAttack(6.0f, distance);
            attackCooldown = 12;
        }
    }

    // BOW - Predictive shooting with kiting
    private void tickBow(double distance) {
        // Kite behavior
        if (distance < 8.0) {
            isRetreating = true;
        } else if (distance > 18.0) {
            isRetreating = false;
        }

        // Shoot arrows at range
        if (distance > 6.0 && distance < 35.0 && specialCooldown == 0) {
            isDrawing = true;
            drawTicks++;

            int drawTime = switch (difficulty) {
                case PRACTICE -> 40;
                case EASY -> 28;
                case MEDIUM -> 20;
                case HARD -> 14;
            };

            if (drawTicks >= drawTime) {
                shootArrow();
                drawTicks = 0;
                isDrawing = false;
                specialCooldown = switch (difficulty) {
                    case PRACTICE -> 60;
                    case EASY -> 35;
                    case MEDIUM -> 22;
                    case HARD -> 12;
                };
            }
        } else {
            drawTicks = Math.max(0, drawTicks - 2);
            isDrawing = false;

            // Panic melee if too close
            if (distance < 3.0 && attackCooldown == 0) {
                performMeleeAttack(3.0f, distance);
                attackCooldown = 15;
            }
        }
    }

    // CRYSTAL - Explosion damage simulation
    private void tickCrystal(double distance) {
        if (distance > 2.0 && distance < 8.0 && specialCooldown == 0) {
            ServerWorld world = (ServerWorld) bot.getEntityWorld();

            float damage = (float) (9.0 * damageMultiplier);

            // Self damage (but less)
            float selfDamage = damage * 0.25f;
            bot.damage(world, world.getDamageSources().explosion(null, null), selfDamage);
            target.damage(world, world.getDamageSources().explosion(null, bot), damage);

            // Knockback from "explosion"
            Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
            target.addVelocity(kb.x * 0.65, 0.45, kb.z * 0.65);
            target.velocityDirty = true;

            // Crystal explosion particles and sound
            Vec3d midPoint = new Vec3d(
                (bot.getX() + target.getX()) / 2,
                (bot.getY() + target.getY()) / 2,
                (bot.getZ() + target.getZ()) / 2
            );
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, midPoint.x, midPoint.y, midPoint.z,
                1, 0, 0, 0, 0);
            world.playSound(null, midPoint.x, midPoint.y, midPoint.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0f, 1.0f);

            specialCooldown = switch (difficulty) {
                case PRACTICE -> 80;
                case EASY -> 55;
                case MEDIUM -> 35;
                case HARD -> 20;
            };
        }

        // Melee attacks
        if (distance < 3.0 && attackCooldown == 0) {
            performMeleeAttack(6.0f, distance);
            attackCooldown = 11;
        }
    }

    // UHC - Rod combos, fishing rod mechanics
    private void tickUHC(double distance) {
        // Rod pull at medium range
        if (distance > 5.0 && distance < 14.0 && specialCooldown == 0) {
            Vec3d pullDir = new Vec3d(bot.getX() - target.getX(), 0.25, bot.getZ() - target.getZ()).normalize();
            double pullStrength = switch (difficulty) {
                case PRACTICE -> 0.2;
                case EASY -> 0.35;
                case MEDIUM -> 0.5;
                case HARD -> 0.65;
            };
            target.addVelocity(pullDir.x * pullStrength, pullDir.y, pullDir.z * pullStrength);
            target.velocityDirty = true;

            // Rod sound
            ServerWorld world = (ServerWorld) bot.getEntityWorld();
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.HOSTILE, 1.0f, 1.0f);

            specialCooldown = 25;
        }

        // Standard sword combat with combos
        if (distance < 3.5 && attackCooldown == 0) {
            if (bot.isOnGround() && jumpCooldown == 0 && ThreadLocalRandom.current().nextFloat() < 0.35f) {
                bot.setVelocity(bot.getVelocity().x, 0.42, bot.getVelocity().z);
                bot.velocityDirty = true;
                jumpCooldown = 14;
            }

            performMeleeAttack(7.0f, distance);
            attackCooldown = 10;
        }
    }

    // POTION - Splash potion throwing and buff management
    private void tickPotion(double distance) {
        ServerWorld world = (ServerWorld) bot.getEntityWorld();

        // Self buff when low on effects or at start
        if (specialCooldown == 0 && bot.getActiveStatusEffects().isEmpty()) {
            // Give self speed and strength
            bot.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 1));
            bot.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 0));

            // Drinking sound
            world.playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.HOSTILE, 1.0f, 1.0f);

            specialCooldown = 400; // 20 seconds
        }

        // Throw harming potions at range
        if (distance > 4.0 && distance < 12.0 && attackCooldown == 0) {
            // Predict target position
            double predictionMultiplier = switch (difficulty) {
                case PRACTICE -> 0.1;
                case EASY -> 0.3;
                case MEDIUM -> 0.6;
                case HARD -> 0.85;
            };

            int ticks = (int) (distance / 1.5);
            Vec3d predicted = new Vec3d(
                target.getX() + lastTargetVelocity.x * ticks * predictionMultiplier * 20,
                target.getY(),
                target.getZ() + lastTargetVelocity.z * ticks * predictionMultiplier * 20
            );

            // Simulate harming potion damage
            float damage = (float) (6.0 * damageMultiplier);

            // Apply damage and slowness effect
            target.damage(world, world.getDamageSources().magic(), damage);

            // Random debuff
            double debuffChance = switch (difficulty) {
                case PRACTICE -> 0.0;
                case EASY -> 0.2;
                case MEDIUM -> 0.35;
                case HARD -> 0.5;
            };

            if (ThreadLocalRandom.current().nextDouble() < debuffChance) {
                int duration = switch (difficulty) {
                    case PRACTICE -> 20;
                    case EASY -> 60;
                    case MEDIUM -> 100;
                    case HARD -> 160;
                };
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1));
            }

            // Potion particles and sound
            world.spawnParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + 1, target.getZ(),
                15, 0.5, 0.5, 0.5, 0.1);
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.HOSTILE, 1.0f, 1.0f);

            attackCooldown = switch (difficulty) {
                case PRACTICE -> 80;
                case EASY -> 50;
                case MEDIUM -> 35;
                case HARD -> 22;
            };
        }

        // Melee when close
        if (distance < 3.5 && attackCooldown == 0) {
            if (bot.isOnGround() && jumpCooldown == 0 && ThreadLocalRandom.current().nextFloat() < 0.3f) {
                bot.setVelocity(bot.getVelocity().x, 0.42, bot.getVelocity().z);
                bot.velocityDirty = true;
                jumpCooldown = 12;
            }

            performMeleeAttack(7.0f, distance);
            attackCooldown = 10;
        }
    }

    // SHIELD - Block timing and counter attacks
    private void tickShield(double distance) {
        // Predictive blocking
        if (distance < 5.0 && !isBlocking && blockCooldown == 0) {
            // Block when target is likely to attack
            double blockChance = switch (difficulty) {
                case PRACTICE -> 0.05;
                case EASY -> 0.15;
                case MEDIUM -> 0.3;
                case HARD -> 0.5;
            };

            // Higher chance if target is swinging
            if (target.handSwinging) {
                blockChance *= 2;
            }

            if (ThreadLocalRandom.current().nextDouble() < blockChance) {
                isBlocking = true;
                blockCooldown = switch (difficulty) {
                    case PRACTICE -> 80;
                    case EASY -> 50;
                    case MEDIUM -> 35;
                    case HARD -> 25;
                };
            }
        }

        // Release block after some time
        if (isBlocking && blockCooldown < 15) {
            isBlocking = false;
        }

        // Counter attack after blocking
        if (!isBlocking && distance < 3.5 && attackCooldown == 0) {
            // Shield bash
            if (ThreadLocalRandom.current().nextFloat() < 0.25f) {
                Vec3d kb = new Vec3d(target.getX() - bot.getX(), 0, target.getZ() - bot.getZ()).normalize();
                target.addVelocity(kb.x * 0.6, 0.35, kb.z * 0.6);
                target.velocityDirty = true;
            }

            performMeleeAttack(6.0f, distance);
            attackCooldown = 11;
        }
    }

    private void performMeleeAttack(float baseDamage, double distance) {
        bot.swingHand(Hand.MAIN_HAND);

        float damage = (float) (baseDamage * damageMultiplier);

        // Crit bonus if falling
        if (bot.getVelocity().y < -0.08) {
            damage *= 1.5f;

            // Crit particles
            ServerWorld world = (ServerWorld) bot.getEntityWorld();
            world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(),
                8, 0.3, 0.5, 0.3, 0.1);
        }

        ServerWorld world = (ServerWorld) bot.getEntityWorld();
        target.damage(world, bot.getDamageSources().mobAttack(bot), damage);

        // Hit sound
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.5f, 1.0f);
    }

    private void shootArrow() {
        ServerWorld world = (ServerWorld) bot.getEntityWorld();

        // Predict target movement
        double dist = bot.distanceTo(target);

        // Prediction based on difficulty
        double predictionMultiplier = switch (difficulty) {
            case PRACTICE -> 0.1;
            case EASY -> 0.4;
            case MEDIUM -> 0.75;
            case HARD -> 1.0;
        };

        int ticks = (int) (dist / 2.8);
        Vec3d predicted = new Vec3d(
            target.getX() + lastTargetVelocity.x * ticks * predictionMultiplier * 20,
            target.getY() + target.getHeight() * 0.65,
            target.getZ() + lastTargetVelocity.z * ticks * predictionMultiplier * 20
        );

        Vec3d dir = predicted.subtract(bot.getX(), bot.getEyeY(), bot.getZ()).normalize();

        // Inaccuracy based on difficulty
        double inaccuracy = switch (difficulty) {
            case PRACTICE -> 12.0;
            case EASY -> 7.0;
            case MEDIUM -> 3.5;
            case HARD -> 1.0;
        };

        ItemStack arrowStack = new ItemStack(Items.ARROW);
        ArrowEntity arrow = new ArrowEntity(world, bot, arrowStack, null);
        arrow.setPosition(bot.getX(), bot.getEyeY(), bot.getZ());

        // Calculate proper arc
        double yOffset = 0.1 + (dist * 0.008);
        arrow.setVelocity(dir.x, dir.y + yOffset, dir.z, 2.8f, (float) inaccuracy);
        arrow.setDamage(6.0 * damageMultiplier);

        world.spawnEntity(arrow);

        // Bow release sound
        world.playSound(null, bot.getX(), bot.getY(), bot.getZ(),
            SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    public boolean isBlocking() {
        return isBlocking;
    }

    public int getComboCount() {
        return comboCount;
    }

    public int getHitsLanded() {
        return hitsLanded;
    }

    public int getHitsTaken() {
        return hitsTaken;
    }
}
