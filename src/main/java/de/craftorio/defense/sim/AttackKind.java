package de.craftorio.defense.sim;

/** How an attack reaches the enemies. */
public enum AttackKind {
    /** A projectile flies along a line through the target and hits up to {@code pierce} enemies on the way. */
    PROJECTILE,
    /** Hits the target at once, wherever it is. */
    INSTANT,
    /** A blast at the target's place hits enemies around it. */
    AREA,
    /** Many projectiles fly out from the tower in a ring (or a fan). */
    RING,
    /** Hits enemies around the tower itself. */
    AURA
}
