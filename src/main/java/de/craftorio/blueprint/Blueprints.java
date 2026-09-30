package de.craftorio.blueprint;

import de.craftorio.registry.ModRegistries;
import de.craftorio.research.Researches;
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

    /** All blueprints by order, then id – identical on server and client, so an index identifies one. */
    public static List<Holder.Reference<Blueprint>> sorted(RegistryAccess access) {
        return access.registryOrThrow(ModRegistries.BLUEPRINTS).holders()
                .sorted(Comparator.<Holder.Reference<Blueprint>>comparingInt(holder -> holder.value().order())
                        .thenComparing(holder -> holder.key().location().toString()))
                .toList();
    }

    /** Order of the research that unlocks the blueprint; blueprints that need none come first (-1). */
    public static int unlockOrder(RegistryAccess access, Holder.Reference<Blueprint> holder) {
        String id = id(holder);
        return access.registryOrThrow(de.craftorio.registry.ModRegistries.RESEARCH).holders()
                .filter(research -> research.value().unlocks().stream().anyMatch(unlock -> unlock.toString().equals(id)))
                .mapToInt(research -> research.value().order()).min().orElse(-1);
    }

    /** What the workbench lists: the blueprints the team knows, by category and then in the order they were unlocked. */
    public static List<Holder.Reference<Blueprint>> forWorkbench(RegistryAccess access, Set<String> researched,
                                                                 @org.jetbrains.annotations.Nullable BlueprintCategory category) {
        return sorted(access).stream()
                .filter(holder -> isKnown(access, researched, holder))
                .filter(holder -> category == null || BlueprintCategory.of(holder.key().location().getPath()) == category)
                .sorted(Comparator.<Holder.Reference<Blueprint>>comparingInt(holder -> BlueprintCategory.of(holder.key().location().getPath()).ordinal())
                        .thenComparingInt(holder -> unlockOrder(access, holder)))
                .toList();
    }

    public static String id(Holder.Reference<Blueprint> holder) {
        return holder.key().location().toString();
    }

    /** Is the blueprint available to a team with these finished researches? */
    public static boolean isKnown(RegistryAccess access, Set<String> researched, Holder.Reference<Blueprint> holder) {
        return Researches.knows(access, researched, id(holder));
    }

    public static boolean isKnown(RegistryAccess access, Team team, Holder.Reference<Blueprint> holder) {
        return isKnown(access, team.researched(), holder);
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
        inventory.player.inventoryMenu.sendAllDataToRemote();
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
