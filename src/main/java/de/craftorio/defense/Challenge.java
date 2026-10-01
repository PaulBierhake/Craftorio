package de.craftorio.defense;

/**
 * A challenge the team chooses for a level (Bloons TD 6: modes and challenges); it makes the level harder and pays more.
 * Mastering a challenge (winning a level with it) earns a master star for that challenge.
 */
public enum Challenge {
    NONE(1.0, 1.0, 1.0),
    /** Double HP MOABs: MOAB-class enemies have twice the hit points. */
    DOUBLE_BOSS_HP(2.0, 1.0, 1.5),
    /** Half Cash: pops, round bonuses and everything else earn half. */
    HALF_CASH(1.0, 0.5, 1.5),
    /** Alternate Bloons Rounds: the harder round list. */
    ALTERNATE(1.0, 1.0, 1.3),
    /** Apopalypse: the rounds follow each other without a break and cannot be called early. */
    APOCALYPSE(1.0, 1.0, 1.4),
    /** CHIMPS: one life, no coins for selling towers, no war chest, no supply depots. */
    CHIMPS(1.0, 1.0, 2.0);

    private final double bossHp;
    private final double coins;
    private final double reward;

    Challenge(double bossHp, double coins, double reward) {
        this.bossHp = bossHp;
        this.coins = coins;
        this.reward = reward;
    }

    public double bossHpFactor() {
        return bossHp;
    }

    public double coinFactor() {
        return coins;
    }

    /** Factor on the level's credit reward. */
    public double rewardFactor() {
        return reward;
    }

    public boolean alternateRounds() {
        return this == ALTERNATE;
    }

    /** No rest between rounds and no calling rounds early. */
    public boolean continuous() {
        return this == APOCALYPSE;
    }

    public boolean oneLife() {
        return this == CHIMPS;
    }

    public boolean noSelling() {
        return this == CHIMPS;
    }

    public boolean noWarChest() {
        return this == CHIMPS;
    }

    public boolean noDepots() {
        return this == CHIMPS;
    }

    public Challenge next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Challenge byOrdinal(int ordinal) {
        return values()[Math.floorMod(ordinal, values().length)];
    }
}
