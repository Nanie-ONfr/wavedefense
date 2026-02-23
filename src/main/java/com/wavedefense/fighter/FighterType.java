package com.wavedefense.fighter;

public enum FighterType {
    MACE("Mace Fighter", 30.0, 0.35),
    SWORD("Sword Fighter", 25.0, 0.4),
    AXE("Axe Fighter", 28.0, 0.35),
    BOW("Archer", 20.0, 0.3),
    SHIELD("Shield Fighter", 35.0, 0.3),
    CRYSTAL("Crystal PvPer", 20.0, 0.45),
    SMP_UHC("UHC Fighter", 40.0, 0.35);

    private final String displayName;
    private final double health;
    private final double speed;

    FighterType(String displayName, double health, double speed) {
        this.displayName = displayName;
        this.health = health;
        this.speed = speed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getHealth() {
        return health;
    }

    public double getSpeed() {
        return speed;
    }
}
