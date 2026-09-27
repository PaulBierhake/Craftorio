package de.craftorio.defense;

/** Upgrade levels I–V: each level adds 30 % damage and 25 % health; repairs cost one credit per missing hit point. */
public final class TowerStats {
    public static final int MAX_LEVEL = 5;

    /** Credits and material count per upgrade to level 2..5 (materials see {@link #upgradeMaterial}). */
    private static final long[] UPGRADE_CREDITS = {0, 0, 150, 400, 1_000, 2_500};
    private static final int[] UPGRADE_AMOUNT = {0, 0, 8, 16, 4, 4};

    public enum Material {
        IRON_PLATE, COPPER_CABLE, IRON_GEAR, MOTOR
    }

    private TowerStats() {
    }

    public static double damage(TowerType type, int level) {
        return type.damage() * (1 + 0.3 * (level - 1));
    }

    public static int maxHealth(TowerType type, int level) {
        return (int) Math.round(type.health() * (1 + 0.25 * (level - 1)));
    }

    public static long upgradeCredits(int toLevel) {
        return UPGRADE_CREDITS[toLevel];
    }

    public static int upgradeAmount(int toLevel) {
        return UPGRADE_AMOUNT[toLevel];
    }

    public static Material upgradeMaterial(int toLevel) {
        return switch (toLevel) {
            case 2 -> Material.IRON_PLATE;
            case 3 -> Material.COPPER_CABLE;
            case 4 -> Material.IRON_GEAR;
            default -> Material.MOTOR;
        };
    }

    public static long repairCost(int health, int maxHealth) {
        return Math.max(0, maxHealth - health);
    }
}
