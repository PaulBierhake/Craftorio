package de.craftorio.defense.arena;

/** The look and special rule of an arena map. Every tenth level is fought in the colosseum. */
public enum ArenaTheme {
    /** Tree clusters; enemies in the thicket are camouflaged. */
    FOREST,
    /** Rock ridges and plateaus; towers on plateaus reach further. */
    MOUNTAIN,
    /** Lava lakes; vents erupt and burn enemies walking over them. */
    FIRE,
    /** A river with fords and lakes; shallow water slows enemies. */
    WATER,
    /** Boss arena: open sand with pillars. */
    COLOSSEUM;

    public static final ArenaTheme[] ROTATION = {FOREST, MOUNTAIN, FIRE, WATER};
}
