package de.craftorio.defense.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * One round of the standard list (bloons wiki, Rounds (BTD6)): groups of enemies in the order they appear, how long
 * the round lasts and the reference values of the wiki for checking the data.
 *
 * @param duration  seconds from the first to the last enemy
 * @param rbe       RBE of all enemies of the round
 * @param cash      coins popping everything pays (already reduced by the income factor)
 */
public record RoundDef(int round, double duration, int rbe, double cash, List<Group> groups) {

    public record Group(String enemy, int count, boolean camo, boolean regrow, boolean fortified) {
    }

    /** An enemy that enters the path at a tick counted from the start of the round. */
    public record Spawn(int tick, Group group) {
    }

    public int enemyCount() {
        int total = 0;
        for (Group group : groups) {
            total += group.count;
        }
        return total;
    }

    public int durationTicks() {
        return TdUnits.ticks(duration);
    }

    /** Spawn times: the wiki gives only the total duration, so the enemies enter evenly spread over it. */
    public List<Spawn> spawns() {
        int total = enemyCount();
        List<Spawn> list = new ArrayList<>(total);
        int index = 0;
        int ticks = durationTicks();
        for (Group group : groups) {
            for (int i = 0; i < group.count; i++, index++) {
                list.add(new Spawn(total <= 1 ? 0 : (int) Math.round(index * (double) ticks / (total - 1)), group));
            }
        }
        return list;
    }
}
