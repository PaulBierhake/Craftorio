package de.craftorio.defense.sim;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * One kind of enemy, loaded from {@code data/craftorio/td_enemies/<id>.json}. The enemy has one layer: its own hit
 * points; when they are gone it releases its children. From round 81 on the {@code late} values apply.
 *
 * @param index        position in {@link EnemyDefs#IDS}; used by packets and preview lists
 * @param btd6         the Bloons TD 6 name this enemy stands for
 * @param hp           hit points of the layer
 * @param fortifiedHp  hit points when fortified, 0 if the enemy cannot be fortified
 * @param speed        BTD6 units per second
 * @param children     what pops out of it
 * @param immune       damage kinds that do nothing to it
 * @param boss         MOAB class: hit points grow in the late game, overkill is not passed on, never regrows
 * @param camo         is always camouflaged
 * @param childMods    modifiers ("camo", "regrow") its children get
 * @param late         values from round 81 on, or null if they do not change
 */
public record EnemyDef(String id, int index, String btd6, double hp, double fortifiedHp, double speed, List<String> children,
                       Set<DamageKind> immune, boolean boss, boolean camo, List<String> childMods, Late late) {

    /**
     * Late-game values; a field that is not given keeps the normal one.
     *
     * @param cash          coins the layer pays when popped (a super crystal golem pays 87 instead of 1), before the income factor
     * @param leak          lives a leaked enemy of this kind costs in total (0: calculated from the layers)
     * @param fortifiedLeak as {@code leak} when fortified
     */
    public record Late(double hp, double fortifiedHp, List<String> children, int cash, int leak, int fortifiedLeak) {
    }

    public EnemyDef {
        children = List.copyOf(children);
        childMods = List.copyOf(childMods);
        immune = immune.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(immune));
    }

    public boolean canBeFortified() {
        return fortifiedHp > 0;
    }

    public double hp(boolean lateGame, boolean fortified) {
        double normal = fortified ? fortifiedHp : hp;
        if (lateGame && late != null) {
            return fortified ? late.fortifiedHp : late.hp;
        }
        return normal;
    }

    public List<String> children(boolean lateGame) {
        return lateGame && late != null ? late.children : children;
    }

    /** Coins popping this layer pays (before the income factor). */
    public int popCash(boolean lateGame) {
        return lateGame && late != null ? late.cash : 1;
    }

    public boolean isImmune(DamageKind kind) {
        return kind != DamageKind.NORMAL && immune.contains(kind);
    }

    /** Blocks per tick before any speed factor. */
    public double blocksPerTick() {
        return TdUnits.blocksPerTick(speed);
    }
}
