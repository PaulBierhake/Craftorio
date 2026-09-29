package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.blueprint.KeyMaterialItem;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import de.craftorio.registry.ModRecipes;
import de.craftorio.registry.ModRegistries;
import de.craftorio.world.OreFieldBlock;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nothing in the game may need something that cannot be made: starting with what a player can mine by hand, every
 * blueprint and machine recipe must be reachable through recipes that are available at that point (caves and mines
 * only count after their entrance or shaft can be built).
 */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProgressionGameTests {
    private ProgressionGameTests() {
    }

    /** One recipe as ingredients (any matching item will do) and results. */
    private record Step(String name, List<List<Item>> ingredients, List<Item> results) {
    }

    @GameTest(template = "empty")
    public static void everyBlueprintAndMachineRecipeIsObtainable(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var recipes = helper.getLevel().getRecipeManager();
        List<Step> steps = new ArrayList<>();
        List<String> blueprintNames = new ArrayList<>();
        Set<Item> blueprintResults = new HashSet<>();
        for (Holder.Reference<Blueprint> holder : access.registryOrThrow(ModRegistries.BLUEPRINTS).holders().toList()) {
            Blueprint blueprint = holder.value();
            steps.add(new Step("blueprint " + holder.key().location(), sized(blueprint.ingredients()), List.of(blueprint.result().getItem())));
            blueprintNames.add(holder.key().location().toString());
            blueprintResults.add(blueprint.result().getItem());
        }
        for (RecipeType<MachineRecipe> type : List.of(ModRecipes.SMELTING.get(), ModRecipes.ASSEMBLING.get())) {
            for (RecipeHolder<MachineRecipe> holder : recipes.getAllRecipesFor(type)) {
                steps.add(new Step("machine recipe " + holder.id(), sized(holder.value().ingredients()), List.of(holder.value().result().getItem())));
                blueprintResults.add(holder.value().result().getItem());
            }
        }
        for (RecipeHolder<? extends Recipe<?>> holder : recipes.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.getType() == RecipeType.SMELTING || recipe.getType() == RecipeType.CRAFTING) {
                List<List<Item>> ingredients = new ArrayList<>();
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (!ingredient.isEmpty()) {
                        ingredients.add(items(ingredient));
                    }
                }
                ItemStack result = recipe.getResultItem(access);
                if (!result.isEmpty()) {
                    steps.add(new Step("vanilla " + holder.id(), ingredients, List.of(result.getItem())));
                }
            }
        }

        // Hand-mined and surface materials, and the key materials of the arena (tower defense).
        Set<Item> have = new HashSet<>(List.of(Items.RAW_IRON, Items.RAW_COPPER, Items.COAL, Items.COBBLESTONE, Items.SAND, Items.CLAY_BALL));
        BuiltInRegistries.ITEM.stream().filter(item -> item.getDefaultInstance().is(ItemTags.LOGS)).forEach(have::add);
        BuiltInRegistries.ITEM.stream().filter(item -> item instanceof KeyMaterialItem).forEach(have::add);

        boolean cavesOpen = false;
        boolean minesOpen = false;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Step step : steps) {
                if (step.ingredients().stream().allMatch(options -> options.stream().anyMatch(have::contains))) {
                    for (Item result : step.results()) {
                        changed |= have.add(result);
                    }
                }
            }
            if (!cavesOpen && have.contains(ModItems.CAVE_ENTRANCE.get())) {
                cavesOpen = true;
                changed |= addResources(have, ModBlocks.TIN_ORE_FIELD.get(), ModBlocks.LEAD_ORE_FIELD.get(), ModBlocks.SULFUR_FIELD.get(),
                        ModBlocks.GOLD_ORE_FIELD.get(), ModBlocks.QUARTZ_FIELD.get());
            }
            if (!minesOpen && have.contains(ModItems.MINE_SHAFT.get())) {
                minesOpen = true;
                changed |= addResources(have, ModBlocks.DIAMOND_FIELD.get(), ModBlocks.TITANIUM_ORE_FIELD.get(),
                        ModBlocks.URANIUM_ORE_FIELD.get(), ModBlocks.CRYSTAL_FIELD.get());
            }
        }

        List<String> problems = new ArrayList<>();
        for (Step step : steps) {
            if (!step.name().startsWith("vanilla") && step.results().stream().noneMatch(have::contains)) {
                List<String> missing = new ArrayList<>();
                for (List<Item> options : step.ingredients()) {
                    if (options.stream().noneMatch(have::contains)) {
                        missing.add(options.isEmpty() ? "?" : BuiltInRegistries.ITEM.getKey(options.get(0)).toString());
                    }
                }
                problems.add(step.name() + " needs " + missing);
            }
        }
        helper.assertTrue(cavesOpen, "the cave entrance can not be built");
        helper.assertTrue(minesOpen, "the mine shaft can not be built");
        helper.assertTrue(problems.isEmpty(), "not obtainable: " + String.join("; ", problems));
        helper.succeed();
    }

    private static boolean addResources(Set<Item> have, Block... fields) {
        boolean added = false;
        for (Block field : fields) {
            if (field instanceof OreFieldBlock ore) {
                added |= have.add(ore.resource().getItem());
            }
        }
        return added;
    }

    private static List<List<Item>> sized(List<SizedIngredient> ingredients) {
        return ingredients.stream().map(ingredient -> items(ingredient.ingredient())).toList();
    }

    private static List<Item> items(Ingredient ingredient) {
        List<Item> items = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            items.add(stack.getItem());
        }
        return items;
    }
}
