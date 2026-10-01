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
    /** Cold that also hurts leaden enemies (glacier ice); whites still shrug it off. */
    GLACIER,
    /** Energy that also hurts leaden enemies (plasma); purples still shrug it off. */
    PLASMA,
    /** Hits everything. */
    NORMAL;

    /** The kind whose immunities count for this one. */
    public DamageKind proxy() {
        return switch (this) {
            case GLACIER -> COLD;
            case PLASMA -> ENERGY;
            default -> this;
        };
    }

    /** Hits leaden enemies even if the proxy kind would not. */
    public boolean hitsLead() {
        return this == GLACIER || this == PLASMA;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DamageKind byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
