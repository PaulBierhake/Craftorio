package de.craftorio.recipe;

import de.craftorio.Craftorio;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.List;
import java.util.Set;

/**
 * Pickaxes, axes, shovels and hoes cannot be crafted any more: the starter pickaxe is enough, everything else
 * comes from the mod's own progression. Smelting cobblestone to stone is gone too: stone bricks take its place. Removed after the recipes are loaded, because NeoForge itself ships some of
 * these recipe files and a data file of this mod could not override them.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class RemovedVanillaRecipes {
    private static final Set<ResourceLocation> REMOVED = Set.copyOf(names());

    private RemovedVanillaRecipes() {
    }

    private static List<ResourceLocation> names() {
        List<ResourceLocation> names = new java.util.ArrayList<>();
        for (String tool : List.of("pickaxe", "axe", "shovel", "hoe")) {
            for (String material : List.of("wooden", "stone", "iron", "golden", "diamond")) {
                names.add(ResourceLocation.withDefaultNamespace(material + "_" + tool));
            }
            names.add(ResourceLocation.withDefaultNamespace("netherite_" + tool + "_smithing"));
        }
        // Stone is made from stone bricks' raw material only by the mod's 2:1 furnace recipe.
        names.add(ResourceLocation.withDefaultNamespace("stone"));
        return names;
    }

    public static boolean isRemoved(ResourceLocation id) {
        return REMOVED.contains(id);
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        var recipes = event.getServerResources().getRecipeManager();
        // Registered after the recipe manager, so its recipes are already loaded when this runs.
        event.addListener((ResourceManagerReloadListener) manager -> recipes.replaceRecipes(
                recipes.getRecipes().stream().filter(holder -> !isRemoved(holder.id())).<RecipeHolder<?>>map(holder -> holder).toList()));
    }
}
