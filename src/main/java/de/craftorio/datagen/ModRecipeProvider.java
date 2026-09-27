package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.recipe.MachineRecipeKind;
import de.craftorio.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        machineRecipes(output);

        // The only vanilla recipes: everything else is unlocked as a blueprint and built at the workbench.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WORKBENCH.get())
                .pattern("III")
                .pattern("PTP")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('T', Items.CRAFTING_TABLE)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TERMINAL.get())
                .pattern("IGI")
                .pattern("IRI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS_PANE)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output);
    }

    private static void machineRecipes(RecipeOutput output) {
        press(output, "iron_plate", Items.IRON_INGOT, new ItemStack(ModItems.IRON_PLATE.get()));
        press(output, "copper_cable", Items.COPPER_INGOT, new ItemStack(ModItems.COPPER_CABLE.get(), 2));

        assemble(output, "iron_gear", new ItemStack(ModItems.IRON_GEAR.get()), 20,
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 2));
        assemble(output, "circuit", new ItemStack(ModItems.CIRCUIT.get()), 40,
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 3), SizedIngredient.of(ModItems.IRON_PLATE.get(), 1));
        assemble(output, "motor", new ItemStack(ModItems.MOTOR.get()), 80,
                SizedIngredient.of(ModItems.IRON_GEAR.get(), 2), SizedIngredient.of(ModItems.IRON_PLATE.get(), 1),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 2));
    }

    private static void press(RecipeOutput output, String name, ItemLike input, ItemStack result) {
        output.accept(Craftorio.id("pressing/" + name),
                new MachineRecipe(MachineRecipeKind.PRESSING, List.of(SizedIngredient.of(input, 1)), result, 40), null);
    }

    private static void assemble(RecipeOutput output, String name, ItemStack result, int time, SizedIngredient... ingredients) {
        output.accept(Craftorio.id("assembling/" + name),
                new MachineRecipe(MachineRecipeKind.ASSEMBLING, List.of(ingredients), result, time), null);
    }
}
