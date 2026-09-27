package de.craftorio.blueprint;

import de.craftorio.registry.ModRegistries;
import de.craftorio.team.Team;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Blueprint lookups and inventory checks shared by server logic and client screens. */
public final class Blueprints {
    private Blueprints() {
    }

    /** All blueprints by tier, then order, then id – identical on server and client, so an index identifies one. */
    public static List<Holder.Reference<Blueprint>> sorted(RegistryAccess access) {
        return access.registryOrThrow(ModRegistries.BLUEPRINTS).holders()
                .sorted(Comparator.<Holder.Reference<Blueprint>>comparingInt(holder -> holder.value().tier())
                        .thenComparingInt(holder -> holder.value().order())
                        .thenComparing(holder -> holder.key().location().toString()))
                .toList();
    }

    public static String id(Holder.Reference<Blueprint> holder) {
        return holder.key().location().toString();
    }

    public static UnlockRules.Status status(Holder.Reference<Blueprint> holder, Set<String> unlocked, long balance, Inventory inventory) {
        Blueprint blueprint = holder.value();
        return UnlockRules.check(id(holder), blueprint.isFree(), unlocked,
                blueprint.requires().stream().map(Object::toString).toList(),
                balance, blueprint.cost(), hasAll(inventory, blueprint.unlockItems(), 1));
    }

    public static boolean isUnlocked(Holder.Reference<Blueprint> holder, Team team) {
        return holder.value().isFree() || team.unlocked().contains(id(holder));
    }

    /** Matching items in the player's main inventory and hotbar. */
    public static int count(Inventory inventory, SizedIngredient ingredient) {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (ingredient.ingredient().test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean hasAll(Inventory inventory, List<SizedIngredient> ingredients, int times) {
        return ingredients.stream().allMatch(ingredient -> count(inventory, ingredient) >= ingredient.count() * times);
    }

    /** Removes the ingredients; call only after {@link #hasAll}. */
    public static void take(Inventory inventory, List<SizedIngredient> ingredients, int times) {
        for (SizedIngredient ingredient : ingredients) {
            int remaining = ingredient.count() * times;
            for (ItemStack stack : inventory.items) {
                if (remaining <= 0) {
                    break;
                }
                if (ingredient.ingredient().test(stack)) {
                    int used = Math.min(remaining, stack.getCount());
                    stack.shrink(used);
                    remaining -= used;
                }
            }
        }
        inventory.setChanged();
        // While another menu is open the inventory is not synced on its own; push the change now.
        inventory.player.inventoryMenu.broadcastChanges();
    }

    /** How many times the player could build the blueprint from their inventory, up to {@code limit}. */
    public static int craftableTimes(Inventory inventory, Blueprint blueprint, int limit) {
        int times = limit;
        for (SizedIngredient ingredient : blueprint.ingredients()) {
            times = Math.min(times, count(inventory, ingredient) / ingredient.count());
        }
        return Math.max(0, times);
    }
}
