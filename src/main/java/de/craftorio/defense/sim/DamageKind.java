package de.craftorio.defense.sim;

import java.util.Locale;

/** What kind of damage a tower deals; enemies are immune to some kinds (see {@link EnemyDef#immune()}). */
public enum DamageKind {
    /** Sharp: arrows and bullets; leaden and shadowy enemies shrug it off. */
    SHARP,
    EXPLOSION,
    COLD,
    ENERGY,
    FIRE,
    /** Hits everything. */
    NORMAL;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DamageKind byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
