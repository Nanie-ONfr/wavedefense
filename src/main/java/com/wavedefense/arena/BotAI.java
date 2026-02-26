package com.wavedefense.arena;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Advanced PvP AI for arena bots - mimics real player behavior
 * Optimized for realistic 1.21 combat mechanics (Paper API)
 *
 * The bot entity is a Zombie with AI disabled (setAI(false) at creation).
 * All movement is controlled manually via setVelocity().
 */
public class BotAI {
    private final Zombie bot;
    private final World world;
    private Player target;
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
    private Vector lastTargetPos = new Vector(0, 0, 0);
    private Vector lastTargetVelocity = new Vector(0, 0, 0);

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

    // Hurt tracking - set externally via damage event listener since Paper has no hurtTime field
    private boolean wasRecentlyHurt = false;

    public BotAI(Zombie bot, Player target, Kit kit, Difficulty difficulty, World world) {
        this.bot = bot;
        this.world = world;
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

    public void setTarget(Player newTarget) {
        this.target = newTarget;
    }

    /**
     * Called externally (e.g. from EntityDamageEvent listener) to signal the bot was just hurt.
     */
    public void notifyHurt() {
        this.wasRecentlyHurt = true;
    }

    public void tick() {
        if (bot == null || bot.isDead()) return;
        if (target == null || target.isDead()) return;

        // Make bot look at target
        lookAtTarget();

        // Track target movement for prediction
        Vector currentPos = target.getLocation().toVector();
        lastTargetVelocity = currentPos.clone().subtract(lastTargetPos);
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
            reactionDelay = ThreadLocalRandom.current().nextInt(Math.max(1, difficulty.getReactionDelayTicks() / 2));
            return;
        }

        double distance = bot.getLocation().distance(target.getLocation());
        ticksSinceLastHit++;

        // Check if we took damage - dodge/react
        if (wasRecentlyHurt && dodgeCooldown == 0) {
            performDodge(distance);
        }
        wasRecentlyHurt = false;

        // Healing behavior - eat gapple when low
        if (shouldHeal() && healCooldown == 0) {
            performHeal();
        }

        // Low health retreat behavior
        updateRetreatState();

        // Execute kit-specific behavior
        switch (kit) {
            case MACE -> tickMace(distance);
            case NODEBUFF, GAPPLE, COMBO, BOXING, SUMO, SOUP -> tickSword(distance);
            case AXE_SHIELD -> tickAxe(distance);
            case ARCHER, BRIDGE -> tickBow(distance);
            case CRYSTAL, ANCHOR -> tickCrystal(distance);
            case BUILDUHC, CLASSIC -> tickUHC(distance);
            case DEBUFF -> tickPotion(distance);
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

    /**
     * Make bot face the target by computing yaw/pitch and setting rotation.
     * Uses setRotation() to avoid the disruptive teleport approach.
     */
    private void lookAtTarget() {
        Location botLoc = bot.getLocation();
        Vector dir = target.getEyeLocation().toVector().subtract(botLoc.toVector());
        if (dir.lengthSquared() > 0) {
            botLoc.setDirection(dir.normalize());
            bot.setRotation(botLoc.getYaw(), botLoc.getPitch());
        }
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

        Location botLoc = bot.getLocation();
        Location targetLoc = target.getLocation();

        // Jump back or strafe quickly
        Vector awayFromTarget = new Vector(
            botLoc.getX() - targetLoc.getX(),
            0,
            botLoc.getZ() - targetLoc.getZ()
        ).normalize();

        double dodgeChance = switch (difficulty) {
            case PRACTICE -> 0.0;
            case EASY -> 0.1;
            case MEDIUM -> 0.25;
            case HARD -> 0.45;
        };

        if (ThreadLocalRandom.current().nextDouble() < dodgeChance) {
            // Strafe dodge
            Vector strafeVec = new Vector(-awayFromTarget.getZ(), 0, awayFromTarget.getX());
            int dir = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
            bot.setVelocity(new Vector(
                strafeVec.getX() * 0.4 * dir + awayFromTarget.getX() * 0.2,
                0.1,
                strafeVec.getZ() * 0.4 * dir + awayFromTarget.getZ() * 0.2
            ));
            strafeDirection *= -1;
        }

        dodgeCooldown = 15;
        hitsTaken++;
    }

    private boolean shouldHeal() {
        double healthPercent = bot.getHealth() / getMaxHealth();
        return switch (difficulty) {
            case PRACTICE -> false;
            case EASY -> healthPercent < 0.25;
            case MEDIUM -> healthPercent < 0.35;
            case HARD -> healthPercent < 0.5;
        };
    }

    /**
     * Gets the bot's max health via attribute.
     */
    private double getMaxHealth() {
        var attr = bot.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    private void performHeal() {
        // Simulate eating a gapple
        float healAmount = switch (difficulty) {
            case PRACTICE -> 2.0f;
            case EASY -> 4.0f;
            case MEDIUM -> 6.0f;
            case HARD -> 8.0f;
        };

        double newHealth = Math.min(bot.getHealth() + healAmount, getMaxHealth());
        bot.setHealth(newHealth);

        // Add absorption effect
        bot.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 0));

        // Visual/audio feedback
        Location botLoc = bot.getLocation();
        world.playSound(botLoc, Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);

        healCooldown = switch (difficulty) {
            case PRACTICE -> 400; // 20 seconds
            case EASY -> 200;     // 10 seconds
            case MEDIUM -> 140;   // 7 seconds
            case HARD -> 80;      // 4 seconds
        };
    }

    private void updateRetreatState() {
        double healthPercent = bot.getHealth() / getMaxHealth();

        if (healthPercent < 0.3 && retreatCooldown == 0) {
            isRetreating = true;
            retreatCooldown = 80;
        }
        if (healthPercent > 0.5 || retreatCooldown == 0) {
            isRetreating = false;
        }
    }

    private void tickMovement(double distance) {
        Location botLoc = bot.getLocation();
        Location targetLoc = target.getLocation();

        Vector toTarget = new Vector(
            targetLoc.getX() - botLoc.getX(),
            0,
            targetLoc.getZ() - botLoc.getZ()
        );
        if (toTarget.lengthSquared() > 0) {
            toTarget.normalize();
        }

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

        // Calculate strafe vector (perpendicular to toTarget)
        Vector strafeVec = new Vector(-toTarget.getZ(), 0, toTarget.getX()).multiply(strafeDirection);

        // Retreating behavior
        if (isRetreating && distance < 10.0) {
            double speed = movementSpeed * 0.85;
            double currentY = bot.getVelocity().getY();
            bot.setVelocity(new Vector(
                -toTarget.getX() * speed + strafeVec.getX() * speed * 0.4,
                currentY,
                -toTarget.getZ() * speed + strafeVec.getZ() * speed * 0.4
            ));
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
            double currentY = bot.getVelocity().getY();

            bot.setVelocity(new Vector(
                toTarget.getX() * speed + strafeVec.getX() * speed * strafeAmount + noise,
                currentY,
                toTarget.getZ() * speed + strafeVec.getZ() * speed * strafeAmount + noise
            ));
        }

        // Circle strafing when close
        if (distance < 3.0 && distance > 1.5) {
            double speed = movementSpeed * 0.65;
            double currentY = bot.getVelocity().getY();
            bot.setVelocity(new Vector(
                strafeVec.getX() * speed + toTarget.getX() * speed * 0.2,
                currentY,
                strafeVec.getZ() * speed + toTarget.getZ() * speed * 0.2
            ));
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
                Vector vel = bot.getVelocity();
                bot.setVelocity(new Vector(vel.getX(), 0.42, vel.getZ()));
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
                Location bLoc = bot.getLocation();
                Location tLoc = target.getLocation();
                Vector kb = new Vector(
                    tLoc.getX() - bLoc.getX(),
                    0,
                    tLoc.getZ() - bLoc.getZ()
                ).normalize();

                double kbStrength = switch (difficulty) {
                    case PRACTICE -> 0.2;
                    case EASY -> 0.35;
                    case MEDIUM -> 0.45;
                    case HARD -> 0.55;
                };
                target.setVelocity(target.getVelocity().add(
                    new Vector(kb.getX() * kbStrength, 0.38, kb.getZ() * kbStrength)));
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
                    Vector vel = bot.getVelocity();
                    bot.setVelocity(new Vector(vel.getX(), 0.42, vel.getZ()));
                    jumpCooldown = 18;
                }
            }

            performMeleeAttack(9.0f, distance);

            // Shield break knockback
            if (target.isBlocking()) {
                Location bLoc = bot.getLocation();
                Location tLoc = target.getLocation();
                Vector kb = new Vector(
                    tLoc.getX() - bLoc.getX(),
                    0,
                    tLoc.getZ() - bLoc.getZ()
                ).normalize();
                target.setVelocity(target.getVelocity().add(
                    new Vector(kb.getX() * 0.7, 0.45, kb.getZ() * 0.7)));

                // Play shield disable sound
                world.playSound(tLoc, Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
            }

            attackCooldown = 16;
        }
    }

    // MACE - Wind charge jumps, smash attacks
    private void tickMace(double distance) {
        // Wind charge launch
        if (distance < 12.0 && distance > 4.0 && windChargeCooldown == 0 && bot.isOnGround()) {
            Location bLoc = bot.getLocation();
            Location tLoc = target.getLocation();
            Vector toTarget = new Vector(
                tLoc.getX() - bLoc.getX(),
                0,
                tLoc.getZ() - bLoc.getZ()
            ).normalize();

            double launchPower = switch (difficulty) {
                case PRACTICE -> 0.5;
                case EASY -> 0.8;
                case MEDIUM -> 1.0;
                case HARD -> 1.3;
            };

            bot.setVelocity(new Vector(toTarget.getX() * 0.5, launchPower, toTarget.getZ() * 0.5));
            windChargeCooldown = switch (difficulty) {
                case PRACTICE -> 200;
                case EASY -> 120;
                case MEDIUM -> 90;
                case HARD -> 60;
            };
            preparingSmash = true;
            fallStartY = bLoc.getY() + 5;

            // Wind charge sound
            world.playSound(bLoc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.0f);
        }

        // Smash attack on landing
        if (preparingSmash && bot.isOnGround()) {
            preparingSmash = false;
            if (distance < 6.0) {
                Location bLoc = bot.getLocation();
                Location tLoc = target.getLocation();
                double fallDist = Math.max(0, fallStartY - bLoc.getY());
                float damage = (float) ((8.0 + fallDist * 2.5) * damageMultiplier);
                damage = Math.min(damage, 28.0f);

                bot.swingMainHand();
                target.damage(damage, bot);

                // Big knockback
                Vector kb = new Vector(
                    tLoc.getX() - bLoc.getX(),
                    0,
                    tLoc.getZ() - bLoc.getZ()
                ).normalize();
                target.setVelocity(target.getVelocity().add(
                    new Vector(kb.getX() * 0.9, 0.55, kb.getZ() * 0.9)));

                // Ground impact particles
                world.spawnParticle(Particle.EXPLOSION, bLoc.getX(), bLoc.getY(), bLoc.getZ(),
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
            float damage = (float) (9.0 * damageMultiplier);

            // Self damage (but less)
            float selfDamage = damage * 0.25f;
            bot.damage(selfDamage);
            target.damage(damage, bot);

            // Knockback from "explosion"
            Location bLoc = bot.getLocation();
            Location tLoc = target.getLocation();
            Vector kb = new Vector(
                tLoc.getX() - bLoc.getX(),
                0,
                tLoc.getZ() - bLoc.getZ()
            ).normalize();
            target.setVelocity(target.getVelocity().add(
                new Vector(kb.getX() * 0.65, 0.45, kb.getZ() * 0.65)));

            // Crystal explosion particles and sound
            Location midPoint = new Location(world,
                (bLoc.getX() + tLoc.getX()) / 2,
                (bLoc.getY() + tLoc.getY()) / 2,
                (bLoc.getZ() + tLoc.getZ()) / 2
            );
            world.spawnParticle(Particle.EXPLOSION_EMITTER, midPoint, 1, 0, 0, 0, 0);
            world.playSound(midPoint, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

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
            Location bLoc = bot.getLocation();
            Location tLoc = target.getLocation();
            Vector pullDir = new Vector(
                bLoc.getX() - tLoc.getX(),
                0.25,
                bLoc.getZ() - tLoc.getZ()
            ).normalize();

            double pullStrength = switch (difficulty) {
                case PRACTICE -> 0.2;
                case EASY -> 0.35;
                case MEDIUM -> 0.5;
                case HARD -> 0.65;
            };
            target.setVelocity(target.getVelocity().add(
                new Vector(pullDir.getX() * pullStrength, pullDir.getY(), pullDir.getZ() * pullStrength)));

            // Rod sound
            world.playSound(tLoc, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);

            specialCooldown = 25;
        }

        // Standard sword combat with combos
        if (distance < 3.5 && attackCooldown == 0) {
            if (bot.isOnGround() && jumpCooldown == 0 && ThreadLocalRandom.current().nextFloat() < 0.35f) {
                Vector vel = bot.getVelocity();
                bot.setVelocity(new Vector(vel.getX(), 0.42, vel.getZ()));
                jumpCooldown = 14;
            }

            performMeleeAttack(7.0f, distance);
            attackCooldown = 10;
        }
    }

    // POTION - Splash potion throwing and buff management
    private void tickPotion(double distance) {
        // Self buff when low on effects or at start
        if (specialCooldown == 0 && bot.getActivePotionEffects().isEmpty()) {
            // Give self speed and strength
            bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1));
            bot.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 0));

            // Drinking sound
            world.playSound(bot.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);

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
            // Prediction vector computed for future accuracy tuning / hit detection
            @SuppressWarnings("unused")
            Vector predicted = new Vector(
                target.getLocation().getX() + lastTargetVelocity.getX() * ticks * predictionMultiplier * 20,
                target.getLocation().getY(),
                target.getLocation().getZ() + lastTargetVelocity.getZ() * ticks * predictionMultiplier * 20
            );

            // Simulate harming potion damage (magic damage, no attacker source)
            float damage = (float) (6.0 * damageMultiplier);
            target.damage(damage);

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
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 1));
            }

            // Potion particles and sound
            Location tLoc = target.getLocation();
            world.spawnParticle(Particle.SPLASH, tLoc.getX(), tLoc.getY() + 1, tLoc.getZ(),
                15, 0.5, 0.5, 0.5, 0.1);
            world.playSound(tLoc, Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 1.0f);

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
                Vector vel = bot.getVelocity();
                bot.setVelocity(new Vector(vel.getX(), 0.42, vel.getZ()));
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

            // Higher chance if target is swinging (approximation via isHandRaised)
            if (target.isHandRaised()) {
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
                Location bLoc = bot.getLocation();
                Location tLoc = target.getLocation();
                Vector kb = new Vector(
                    tLoc.getX() - bLoc.getX(),
                    0,
                    tLoc.getZ() - bLoc.getZ()
                ).normalize();
                target.setVelocity(target.getVelocity().add(
                    new Vector(kb.getX() * 0.6, 0.35, kb.getZ() * 0.6)));
            }

            performMeleeAttack(6.0f, distance);
            attackCooldown = 11;
        }
    }

    private void performMeleeAttack(float baseDamage, double distance) {
        bot.swingMainHand();

        float damage = (float) (baseDamage * damageMultiplier);

        // Crit bonus if falling
        if (bot.getVelocity().getY() < -0.08) {
            damage *= 1.5f;

            // Crit particles
            Location tLoc = target.getLocation();
            world.spawnParticle(Particle.CRIT, tLoc.getX(), tLoc.getY() + 1, tLoc.getZ(),
                8, 0.3, 0.5, 0.3, 0.1);
        }

        target.damage(damage, bot);

        // Hit sound
        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 1.0f);
    }

    private void shootArrow() {
        Location botLoc = bot.getLocation();
        Location eyeLoc = bot.getEyeLocation();

        // Predict target movement
        double dist = botLoc.distance(target.getLocation());

        // Prediction based on difficulty
        double predictionMultiplier = switch (difficulty) {
            case PRACTICE -> 0.1;
            case EASY -> 0.4;
            case MEDIUM -> 0.75;
            case HARD -> 1.0;
        };

        int ticks = (int) (dist / 2.8);
        Vector predicted = new Vector(
            target.getLocation().getX() + lastTargetVelocity.getX() * ticks * predictionMultiplier * 20,
            target.getLocation().getY() + target.getHeight() * 0.65,
            target.getLocation().getZ() + lastTargetVelocity.getZ() * ticks * predictionMultiplier * 20
        );

        Vector dir = predicted.subtract(eyeLoc.toVector()).normalize();

        // Inaccuracy based on difficulty
        double inaccuracy = switch (difficulty) {
            case PRACTICE -> 12.0;
            case EASY -> 7.0;
            case MEDIUM -> 3.5;
            case HARD -> 1.0;
        };

        // Add inaccuracy
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        dir.add(new Vector(
            (rand.nextDouble() - 0.5) * inaccuracy * 0.01,
            (rand.nextDouble() - 0.5) * inaccuracy * 0.01,
            (rand.nextDouble() - 0.5) * inaccuracy * 0.01
        ));
        if (dir.lengthSquared() > 0) {
            dir.normalize();
        }

        // Calculate proper arc
        double yOffset = 0.1 + (dist * 0.008);
        dir.setY(dir.getY() + yOffset);
        if (dir.lengthSquared() > 0) {
            dir.normalize();
        }

        float speed = 2.8f;
        Arrow arrow = world.spawnArrow(eyeLoc, dir, speed, 0f);
        arrow.setShooter(bot);
        arrow.setDamage(6.0 * damageMultiplier);

        // Bow release sound
        world.playSound(botLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
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
