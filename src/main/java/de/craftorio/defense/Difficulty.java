package de.craftorio.defense;

/**
 * The difficulty of a team's arena (Bloons TD 6: Easy, Medium, Hard, Impoppable): lives per level, prices of towers,
 * how fast enemies walk and the credits a cleared level pays.
 *
 * @param lives       lives at the start of every level (the game is lost when they are gone)
 * @param priceFactor factor on tower prices and upgrades
 * @param rewardFactor factor on the credits for a cleared level
 * @param speedFactor factor on the enemies' speed (Bloons TD 6: Easy 1, Medium +10 %, Hard +25 %)
 */
public enum Difficulty {
    EASY(200, 0.85, 0.75, 1.0),
    MEDIUM(150, 1.0, 1.0, 1.1),
    HARD(100, 1.08, 1.25, 1.25),
    IMPOPPABLE(1, 1.2, 1.5, 1.25);

    private final int lives;
    private final double priceFactor;
    private final double rewardFactor;
    private final double speedFactor;

    Difficulty(int lives, double priceFactor, double rewardFactor, double speedFactor) {
        this.lives = lives;
        this.priceFactor = priceFactor;
        this.rewardFactor = rewardFactor;
        this.speedFactor = speedFactor;
    }

    public int lives() {
        return lives;
    }

    public double priceFactor() {
        return priceFactor;
    }

    public double rewardFactor() {
        return rewardFactor;
    }

    public double speedFactor() {
        return speedFactor;
    }

    /** A price in coins with the factor applied (rounded to whole coins). */
    public long price(long basePrice) {
        return Math.round(basePrice * priceFactor);
    }

    public Difficulty next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Difficulty byOrdinal(int ordinal) {
        return values()[Math.floorMod(ordinal, values().length)];
    }

    /** Before the first level starts any difficulty may be chosen; afterwards only an easier one. */
    public boolean mayChangeTo(Difficulty wanted, boolean campaignStarted) {
        return !campaignStarted || wanted.ordinal() < ordinal();
    }
}
