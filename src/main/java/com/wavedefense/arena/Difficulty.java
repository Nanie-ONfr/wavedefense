package com.wavedefense.arena;

public enum Difficulty {
    PRACTICE("Practice", 0.20, 0.0, 50.0f, 60, 3.0),  // No damage, high health for training
    EASY("Easy", 0.25, 0.5, 10.0f, 50, 3.0),          // Player range
    MEDIUM("Medium", 0.30, 0.75, 20.0f, 30, 3.0),     // Player range
    HARD("Hard", 0.35, 1.0, 30.0f, 15, 10.0);         // Extended range (max 10)

    private final String name;
    private final double movementSpeed;
    private final double damageMultiplier;
    private final float health;
    private final int reactionDelayTicks; // Lower = faster reactions
    private final double followRange; // Attack/follow range

    Difficulty(String name, double movementSpeed, double damageMultiplier, float health, int reactionDelayTicks, double followRange) {
        this.name = name;
        this.movementSpeed = movementSpeed;
        this.damageMultiplier = damageMultiplier;
        this.health = health;
        this.reactionDelayTicks = reactionDelayTicks;
        this.followRange = followRange;
    }

    public String getName() {
        return name;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public float getHealth() {
        return health;
    }

    public int getReactionDelayTicks() {
        return reactionDelayTicks;
    }

    public double getFollowRange() {
        return followRange;
    }

    public static Difficulty fromString(String name) {
        for (Difficulty d : values()) {
            if (d.name().equalsIgnoreCase(name) || d.getName().equalsIgnoreCase(name)) {
                return d;
            }
        }
        return null;
    }
}
