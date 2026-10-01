package de.craftorio.defense.sim;

/** The rules of the upgrade system that are plain arithmetic (Bloons TD 6: costs, cross paths, selling). */
public final class TowerRules {
    /** Tiers above this need the research "Turmtechnik" I (3), II (4) and III (5). */
    public static final int FREE_TIERS = 2;

    private TowerRules() {
    }

    /**
     * May this path be upgraded by one tier? One path may reach tier 5, a second one tier 2, the third stays at 0
     * (5-2-0, 2-0-5 and 0-2-5 are fine; 3-3-0, 2-2-2 and 5-3-0 are not).
     */
    public static boolean canUpgrade(int[] tiers, int path) {
        if (path < 0 || path >= TowerDef.PATHS || tiers[path] >= TowerDef.TIERS) {
            return false;
        }
        int[] after = tiers.clone();
        after[path]++;
        int beyondTwo = 0;
        int started = 0;
        for (int tier : after) {
            if (tier > FREE_TIERS) {
                beyondTwo++;
            }
            if (tier > 0) {
                started++;
            }
        }
        return beyondTwo <= 1 && started <= 2;
    }

    /** Why the upgrade is impossible, as a short id, or null if it is allowed apart from money and research. */
    public static String lockReason(int[] tiers, int path) {
        if (tiers[path] >= TowerDef.TIERS) {
            return "maxed";
        }
        return canUpgrade(tiers, path) ? null : "crosspath";
    }

    /** The research step a tier needs: 0 for tiers 1 and 2, then 1, 2 and 3. */
    public static int techRequired(int tier) {
        return Math.max(0, tier - FREE_TIERS);
    }

    /** A price with the difficulty's factor applied, in whole coins. */
    public static long price(long base, double factor) {
        return Math.round(base * factor);
    }

    /** What a sold tower pays back: a share of everything paid for it (Bloons TD 6: 70 %, rounded to whole coins). */
    /** The price after a command post's discount (0.1 = 10 % off). */
    public static long discounted(long price, double discount) {
        return discount <= 0 ? price : Math.round(price * (1 - Math.min(discount, 0.9)));
    }

    public static long sellValue(long paid, double share) {
        return java.math.BigDecimal.valueOf(share).multiply(java.math.BigDecimal.valueOf(paid)).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }

    /** The BTD6 notation of the upgrade state, e.g. "5-2-0". */
    public static String notation(int[] tiers) {
        return tiers[0] + "-" + tiers[1] + "-" + tiers[2];
    }
}
