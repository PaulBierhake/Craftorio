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

        // Temporary until the shop (M4) sells machines.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRADING_POST.get())
                .pattern("IGI")
                .pattern("PCP")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GOLD_INGOT)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_chest", has(Items.CHEST))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BURNER_DRILL.get())
                .pattern("III")
                .pattern("IFI")
                .pattern("IPI")
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .define('P', Items.IRON_PICKAXE)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CONVEYOR_BELT.get(), 6)
                .pattern("III")
                .pattern("PRP")
                .define('I', Items.IRON_INGOT)
                .define('P', ItemTags.PLANKS)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INSERTER.get(), 2)
                .pattern("II ")
                .pattern(" R ")
                .pattern("CCC")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COAL_GENERATOR.get())
                .pattern("III")
                .pattern("IFI")
                .pattern("CRC")
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.POWER_POLE.get(), 2)
                .pattern("CCC")
                .pattern(" S ")
                .pattern(" S ")
                .define('C', Items.COPPER_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ELECTRIC_FURNACE.get())
                .pattern("BBB")
                .pattern("BFB")
                .pattern("CRC")
                .define('B', Items.BRICKS)
                .define('F', Items.FURNACE)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PRESS.get())
                .pattern("III")
                .pattern("IPI")
                .pattern("CRC")
                .define('I', Items.IRON_INGOT)
                .define('P', Items.PISTON)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ASSEMBLER.get())
                .pattern("IGI")
                .pattern("CTC")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GOLD_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('T', Items.CRAFTING_TABLE)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
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
