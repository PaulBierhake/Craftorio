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
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;
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

        // Cave metals smelt in any furnace, including the electric furnace.
        smelt(output, ModItems.RAW_TIN.get(), ModItems.TIN_INGOT.get(), "tin_ingot");
        smelt(output, ModItems.RAW_LEAD.get(), ModItems.LEAD_INGOT.get(), "lead_ingot");
        smelt(output, ModItems.RAW_TITANIUM.get(), ModItems.TITANIUM_INGOT.get(), "titanium_ingot");

        // The only vanilla recipes: everything else is a blueprint built at the workbench.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WORKBENCH.get())
                .pattern("III")
                .pattern("PTP")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('T', Items.CRAFTING_TABLE)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output);

        // A replacement handbook (every player gets one on the first login).
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.GUIDE_BOOK.get())
                .requires(Items.BOOK)
                .requires(Items.IRON_INGOT)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(output);
    }

    private static void smelt(RecipeOutput output, ItemLike raw, ItemLike ingot, String name) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(raw), RecipeCategory.MISC, ingot, 0.7F, 200)
                .unlockedBy("has_" + name, has(raw))
                .save(output, Craftorio.id("smelting/" + name));
    }

    private static void machineRecipes(RecipeOutput output) {
        // Furnace recipes with counted ingredients; times are Factorio seconds × 20 at speed 1.
        smelting(output, "stone_brick", new ItemStack(ModItems.STONE_BRICK.get()), 64, SizedIngredient.of(Items.COBBLESTONE, 2));
        smelting(output, "steel_plate", new ItemStack(ModItems.STEEL_PLATE.get()), 320, SizedIngredient.of(Items.IRON_INGOT, 5));

        // Assembler: Factorio recipes (the assembling machine 1 has speed 0.5, so it takes twice as long).
        assemble(output, "iron_gear", new ItemStack(ModItems.IRON_GEAR.get()), 10, SizedIngredient.of(Items.IRON_INGOT, 2));
        assemble(output, "copper_cable", new ItemStack(ModItems.COPPER_CABLE.get(), 2), 10, SizedIngredient.of(Items.COPPER_INGOT, 1));
        assemble(output, "circuit", new ItemStack(ModItems.CIRCUIT.get()), 10,
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 3), SizedIngredient.of(Items.IRON_INGOT, 1));
        assemble(output, "pipe", new ItemStack(ModItems.PIPE.get()), 10, SizedIngredient.of(Items.IRON_INGOT, 1));
        assemble(output, "iron_stick", new ItemStack(ModItems.IRON_STICK.get(), 2), 10, SizedIngredient.of(Items.IRON_INGOT, 1));
        assemble(output, "underground_pipe", new ItemStack(ModItems.UNDERGROUND_PIPE.get(), 2), 10,
                SizedIngredient.of(ModItems.PIPE.get(), 10), SizedIngredient.of(Items.IRON_INGOT, 5));
        assemble(output, "fluid_pump", new ItemStack(ModItems.FLUID_PUMP.get()), 40,
                SizedIngredient.of(ModItems.MOTOR.get(), 1), SizedIngredient.of(ModItems.STEEL_PLATE.get(), 1), SizedIngredient.of(ModItems.PIPE.get(), 1));
        assemble(output, "storage_tank", new ItemStack(ModItems.STORAGE_TANK.get()), 60,
                SizedIngredient.of(Items.IRON_INGOT, 20), SizedIngredient.of(ModItems.STEEL_PLATE.get(), 5));
        assemble(output, "green_science", new ItemStack(ModItems.GREEN_SCIENCE.get()), 120,
                SizedIngredient.of(ModItems.INSERTER.get(), 1), SizedIngredient.of(ModItems.CONVEYOR_BELT.get(), 1));
        assemble(output, "red_science", new ItemStack(ModItems.RED_SCIENCE.get()), 100,
                SizedIngredient.of(Items.COPPER_INGOT, 1), SizedIngredient.of(ModItems.IRON_GEAR.get(), 1));
        // Engine unit: 1 steel + 1 gear + 2 pipes, 10 s; assemblers only
        assemble(output, "motor", new ItemStack(ModItems.MOTOR.get()), 200,
                SizedIngredient.of(ModItems.STEEL_PLATE.get(), 1), SizedIngredient.of(ModItems.IRON_GEAR.get(), 1),
                SizedIngredient.of(ModItems.PIPE.get(), 2));
        // Cave products
        assemble(output, "battery", new ItemStack(ModItems.BATTERY.get()), 60,
                SizedIngredient.of(ModItems.LEAD_INGOT.get(), 2), SizedIngredient.of(ModItems.SULFUR.get(), 1),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 2));
        assemble(output, "advanced_circuit", new ItemStack(ModItems.ADVANCED_CIRCUIT.get()), 80,
                SizedIngredient.of(ModItems.CIRCUIT.get(), 2), SizedIngredient.of(ModItems.TIN_INGOT.get(), 2),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 2));
        // Mine products
        assemble(output, "titanium_plate", new ItemStack(ModItems.TITANIUM_PLATE.get()), 40, SizedIngredient.of(ModItems.TITANIUM_INGOT.get(), 1));
        assemble(output, "uranium_pellet", new ItemStack(ModItems.URANIUM_PELLET.get()), 40, SizedIngredient.of(ModItems.RAW_URANIUM.get(), 1));
        assemble(output, "energy_crystal", new ItemStack(ModItems.ENERGY_CRYSTAL.get()), 120,
                SizedIngredient.of(ModItems.CRYSTAL_SHARD.get(), 4), SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 1),
                SizedIngredient.of(ModItems.BATTERY.get(), 1));
        assemble(output, "fuel_rod", new ItemStack(ModItems.FUEL_ROD.get()), 100,
                SizedIngredient.of(ModItems.URANIUM_PELLET.get(), 3), SizedIngredient.of(ModItems.TITANIUM_PLATE.get(), 2));
        // Ammunition for automated tower supply
        assemble(output, "bolt", new ItemStack(ModItems.BOLT.get(), 16), 20,
                SizedIngredient.of(Items.IRON_INGOT, 1), SizedIngredient.of(Items.STICK, 2));
        assemble(output, "cartridge", new ItemStack(ModItems.CARTRIDGE.get(), 16), 30,
                SizedIngredient.of(Items.COPPER_INGOT, 1), SizedIngredient.of(Items.IRON_INGOT, 1), SizedIngredient.of(Items.COAL, 1));
    }

    private static void smelting(RecipeOutput output, String name, ItemStack result, int time, SizedIngredient... ingredients) {
        output.accept(Craftorio.id("smelting/" + name),
                new MachineRecipe(MachineRecipeKind.SMELTING, List.of(ingredients), result, time), null);
    }

    private static void assemble(RecipeOutput output, String name, ItemStack result, int time, SizedIngredient... ingredients) {
        output.accept(Craftorio.id("assembling/" + name),
                new MachineRecipe(MachineRecipeKind.ASSEMBLING, List.of(ingredients), result, time), null);
    }
}
