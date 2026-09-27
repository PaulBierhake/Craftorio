package de.craftorio.defense.arena;

/** What a square of the arena field allows. */
public enum Tile {
    /** Open ground: path or towers. */
    GROUND(true, true),
    /** Thicket, scree, lava vent or shallow water: path only, with the theme's effect on enemies. */
    ROUGH(true, false),
    /** Plateau: towers only, with extra range. */
    HIGH(false, true),
    /** Trees, rock, lava or deep water: nothing. */
    BLOCKED(false, false);

    private final boolean path;
    private final boolean build;

    Tile(boolean path, boolean build) {
        this.path = path;
        this.build = build;
    }

    public boolean allowsPath() {
        return path;
    }

    public boolean allowsTower() {
        return build;
    }
}
