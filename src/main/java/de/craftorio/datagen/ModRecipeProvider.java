package de.craftorio.datagen;

import de.craftorio.registry.ModItems;
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
    }
}
