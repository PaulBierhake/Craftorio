package de.craftorio.recipe;

import de.craftorio.registry.ModRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public enum MachineRecipeKind {
    /** Counted ingredients smelted in the stone furnace and the electric furnace (steel, stone bricks). */
    SMELTING,
    /** Crafted in the assembling machine. */
    ASSEMBLING;

    public RecipeType<MachineRecipe> type() {
        return this == SMELTING ? ModRecipes.SMELTING.get() : ModRecipes.ASSEMBLING.get();
    }

    public RecipeSerializer<MachineRecipe> serializer() {
        return this == SMELTING ? ModRecipes.SMELTING_SERIALIZER.get() : ModRecipes.ASSEMBLING_SERIALIZER.get();
    }
}
