package de.craftorio.world.rock;

/** What rocks give when mined (docs/WELT-UMBAU.md §6). Plain numbers, so the tests need no Minecraft. */
public final class RockLoot {
    public static final int BIG_STONE_MIN = 24;
    public static final int BIG_STONE_MAX = 50;
    public static final int BIG_COAL_MIN = 10;
    public static final int BIG_COAL_MAX = 25;
    public static final int SMALL_STONE_MIN = 5;
    public static final int SMALL_STONE_MAX = 10;

    private RockLoot() {
    }

    public static int stone(boolean big, int roll) {
        return big ? BIG_STONE_MIN + roll % (BIG_STONE_MAX - BIG_STONE_MIN + 1) : SMALL_STONE_MIN + roll % (SMALL_STONE_MAX - SMALL_STONE_MIN + 1);
    }

    public static int coal(boolean big, int roll) {
        return big ? BIG_COAL_MIN + roll % (BIG_COAL_MAX - BIG_COAL_MIN + 1) : 0;
    }
}
