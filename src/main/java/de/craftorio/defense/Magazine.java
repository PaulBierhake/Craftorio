package de.craftorio.defense;

import de.craftorio.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The magazines a gun turret fires: damage relative to the plain magazine (wiki 1.1: 5 / 8 / 24 damage). */
public enum Magazine {
    NORMAL(1.0),
    ARMOUR_PIERCING(TowerType.AP_FACTOR),
    URANIUM(TowerType.URANIUM_FACTOR);

    private final double factor;

    Magazine(double factor) {
        this.factor = factor;
    }

    /** The damage kind of its hits: only the plain magazine is sharp; the heavy ones hit everything. */
    public de.craftorio.defense.sim.DamageKind damageKind() {
        return this == NORMAL ? null : de.craftorio.defense.sim.DamageKind.NORMAL;
    }

    public double factor() {
        return factor;
    }

    public Item item() {
        return switch (this) {
            case NORMAL -> ModItems.MAGAZINE.get();
            case ARMOUR_PIERCING -> ModItems.AP_MAGAZINE.get();
            case URANIUM -> ModItems.URANIUM_MAGAZINE.get();
        };
    }

    /** The kind of magazine an item is, or null if it is none. */
    public static Magazine of(ItemStack stack) {
        for (Magazine kind : values()) {
            if (stack.is(kind.item())) {
                return kind;
            }
        }
        return null;
    }

    /** Best kind first: what a turret takes from the arena reserve. */
    public static final Magazine[] BEST_FIRST = {URANIUM, ARMOUR_PIERCING, NORMAL};
}
