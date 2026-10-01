package de.craftorio.defense;

import de.craftorio.defense.sim.TowerProfile;

import java.util.List;
import java.util.Set;

/**
 * Arena knowledge: researches of the military branch that give the team lasting bonuses in the arena (the counterpart of
 * the monkey knowledge of Bloons TD 6).
 */
public final class Knowledge {
    /** War supplies: +200 coins at the start of the campaign. */
    public static final String WAR_SUPPLIES = "arena_war_supplies";
    /** Better gear: towers sell for 75 % instead of 70 %. */
    public static final String BETTER_GEAR = "arena_better_gear";
    /** Thrifty building: upgrades up to tier 2 cost 5 % less. */
    public static final String THRIFTY = "arena_thrifty";
    /** Strong bolts: crossbow towers pierce one more enemy. */
    public static final String STRONG_BOLTS = "arena_strong_bolts";
    /** Field hospital: 25 extra lives in every level. */
    public static final String FIELD_HOSPITAL = "arena_field_hospital";
    /** Compound interest: supply depots earn 10 % more. */
    public static final String COMPOUND_INTEREST = "arena_compound_interest";
    /** Vigilance: command posts reach 10 % further. */
    public static final String VIGILANCE = "arena_vigilance";

    public static final List<String> ALL = List.of(WAR_SUPPLIES, BETTER_GEAR, THRIFTY, STRONG_BOLTS, FIELD_HOSPITAL, COMPOUND_INTEREST, VIGILANCE);

    public static final long START_COINS = 200;
    public static final double SELL_SHARE = 0.75;
    public static final double THRIFTY_DISCOUNT = 0.05;
    public static final int FIELD_HOSPITAL_LIVES = 25;
    public static final double COMPOUND_INTEREST_BONUS = 0.10;
    public static final double VIGILANCE_RANGE = 1.10;

    private Knowledge() {
    }

    public static boolean has(Set<String> researched, String knowledge) {
        return researched.contains("craftorio:" + knowledge);
    }

    /** The share a tower sells for. */
    public static double sellShare(Set<String> researched, double base) {
        return has(researched, BETTER_GEAR) ? Math.max(base, SELL_SHARE) : base;
    }

    /** Discount on an upgrade to this tier. */
    public static double upgradeDiscount(Set<String> researched, int tier) {
        return tier <= 2 && has(researched, THRIFTY) ? THRIFTY_DISCOUNT : 0;
    }

    /** A number that changes whenever the knowledge that matters for towers does (so towers rebuild their profile). */
    public static int key(Set<String> researched) {
        int key = 0;
        for (int i = 0; i < ALL.size(); i++) {
            if (has(researched, ALL.get(i))) {
                key |= 1 << i;
            }
        }
        return key;
    }

    /** Applies the knowledge that changes what a tower of this kind can do to its profile. */
    public static void apply(TowerProfile profile, String towerId, Set<String> researched) {
        if (towerId.equals("crossbow_tower") && has(researched, STRONG_BOLTS) && !profile.attacks.isEmpty()) {
            profile.main().pierce += 1;
        }
        if (towerId.equals("supply_depot") && has(researched, COMPOUND_INTEREST)) {
            double factor = profile.numbers.containsKey("income_factor") ? profile.number("income_factor") : 1;
            profile.numbers.put("income_factor", factor * (1 + COMPOUND_INTEREST_BONUS));
        }
        if (towerId.equals("command_post") && has(researched, VIGILANCE) && profile.range > 0) {
            double before = profile.range;
            profile.range *= VIGILANCE_RANGE;
            for (var attack : profile.attacks) {
                if (attack.followRange) {
                    attack.radius += profile.range - before;
                }
            }
        }
    }
}
