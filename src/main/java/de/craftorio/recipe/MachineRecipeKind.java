package de.craftorio.recipe;

import de.craftorio.registry.ModRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public enum MachineRecipeKind {
    PRESSING,
    ASSEMBLING;

    public RecipeType<MachineRecipe> type() {
        return this == PRESSING ? ModRecipes.PRESSING.get() : ModRecipes.ASSEMBLING.get();
    }

    public RecipeSerializer<MachineRecipe> serializer() {
        return this == PRESSING ? ModRecipes.PRESSING_SERIALIZER.get() : ModRecipes.ASSEMBLING_SERIALIZER.get();
    }
}
