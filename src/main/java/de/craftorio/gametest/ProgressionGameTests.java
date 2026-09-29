package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.blueprint.KeyMaterialItem;
import de.craftorio.fluid.FluidMachineType;
import de.craftorio.fluid.FluidRecipes;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.research.Research;
import de.craftorio.research.Researches;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModFluids;
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
import net.minecraft.world.level.material.Fluids;
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

    /** One recipe as ingredients (any matching item will do) and results; {@code unlockId} is what a research names to unlock it. */
    private record Step(String name, String unlockId, List<List<Item>> ingredients, List<Item> results) {
    }

    private static List<Step> steps(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var recipes = helper.getLevel().getRecipeManager();
        List<Step> steps = new ArrayList<>();
        for (Holder.Reference<Blueprint> holder : access.registryOrThrow(ModRegistries.BLUEPRINTS).holders().toList()) {
            Blueprint blueprint = holder.value();
            steps.add(new Step("blueprint " + holder.key().location(), holder.key().location().toString(),
                    sized(blueprint.ingredients()), List.of(blueprint.result().getItem())));
        }
        for (RecipeType<MachineRecipe> type : List.of(ModRecipes.SMELTING.get(), ModRecipes.ASSEMBLING.get())) {
            for (RecipeHolder<MachineRecipe> holder : recipes.getAllRecipesFor(type)) {
                List<List<Item>> ingredients = new ArrayList<>(sized(holder.value().ingredients()));
                // Fluid ingredients need assembling machine 2 (or better) and the fluid itself.
                if (holder.value().needsFluid()) {
                    ingredients.add(List.of(ModItems.ASSEMBLER_2.get()));
                    holder.value().fluids().forEach(fluid -> {
                        if (fluid.getFluid() != Fluids.WATER) {
                            ingredients.add(List.of(fluidStandIn(fluid.getFluid())));
                        }
                    });
                }
                steps.add(new Step("machine recipe " + holder.id(), holder.id().toString(), ingredients,
                        List.of(holder.value().result().getItem())));
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
                    steps.add(new Step("vanilla " + holder.id(), null, ingredients, List.of(result.getItem())));
                }
            }
        }
        // Fluid recipes: items in, items out; the fluids are stand-in items (water is always there).
        for (FluidRecipes.Recipe recipe : FluidRecipes.all()) {
            List<List<Item>> ingredients = new ArrayList<>();
            ingredients.add(List.of(switch (recipe.machine()) {
                case OIL_REFINERY -> ModItems.OIL_REFINERY.get();
                case CHEMICAL_PLANT -> ModItems.CHEMICAL_PLANT.get();
                case GREENHOUSE -> ModItems.GREENHOUSE.get();
            }));
            recipe.itemsIn().forEach(stack -> ingredients.add(List.of(stack.getItem())));
            recipe.fluidsIn().forEach(fluid -> {
                if (fluid.getFluid() != Fluids.WATER) {
                    ingredients.add(List.of(fluidStandIn(fluid.getFluid())));
                }
            });
            List<Item> results = new ArrayList<>();
            if (!recipe.itemOut().isEmpty()) {
                results.add(recipe.itemOut().getItem());
            }
            recipe.fluidsOut().forEach(fluid -> results.add(fluidStandIn(fluid.getFluid())));
            steps.add(new Step("fluid recipe " + recipe.id(), recipe.id().toString(), ingredients, results));
        }
        return steps;
    }

    /** Everything that can be made from hand-mined materials and the arena seals with the steps {@code allowed}. */
    private static Set<Item> closure(List<Step> steps, java.util.function.Predicate<Step> allowed) {
        // Seeds and crops come from breaking grass and from villages in the ordinary world.
        Set<Item> have = new HashSet<>(List.of(Items.RAW_IRON, Items.RAW_COPPER, Items.COAL, Items.COBBLESTONE, Items.SAND, Items.CLAY_BALL,
                Items.WHEAT_SEEDS, Items.PUMPKIN_SEEDS, Items.CARROT, Items.POTATO, Items.SUGAR_CANE, Items.OAK_SAPLING));
        BuiltInRegistries.ITEM.stream().filter(item -> item.getDefaultInstance().is(ItemTags.LOGS)).forEach(have::add);
        BuiltInRegistries.ITEM.stream().filter(item -> item instanceof KeyMaterialItem).forEach(have::add);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Step step : steps) {
                if (allowed.test(step) && step.ingredients().stream().allMatch(options -> options.stream().anyMatch(have::contains))) {
                    for (Item result : step.results()) {
                        changed |= have.add(result);
                    }
                }
            }
            // The caves hold oil wells: crude oil is there once the entrance is built and a pumpjack stands on a well.
            if (have.contains(ModItems.CAVE_ENTRANCE.get()) && have.contains(ModItems.PUMPJACK.get())) {
                changed |= have.add(CRUDE_OIL);
            }
            if (have.contains(ModItems.MINE_SHAFT.get())) {
                changed |= addResources(have, ModBlocks.URANIUM_ORE_FIELD.get());
            }
        }
        return have;
    }

    @GameTest(template = "empty")
    public static void everyBlueprintAndMachineRecipeIsObtainable(GameTestHelper helper) {
        List<Step> steps = steps(helper);
        Set<Item> have = closure(steps, step -> true);
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
        helper.assertTrue(have.contains(ModItems.CAVE_ENTRANCE.get()), "the cave entrance can not be built");
        helper.assertTrue(have.contains(ModItems.MINE_SHAFT.get()), "the mine shaft can not be built");
        helper.assertTrue(problems.isEmpty(), "not obtainable: " + String.join("; ", problems));
        helper.succeed();
    }

    private static Item pack(Research.Pack pack) {
        return switch (pack) {
            case RED -> ModItems.RED_SCIENCE.get();
            case GREEN -> ModItems.GREEN_SCIENCE.get();
            case MILITARY -> ModItems.MILITARY_SCIENCE.get();
            case BLUE -> ModItems.BLUE_SCIENCE.get();
        };
    }

    /**
     * Research in some order: a research can be done once its prerequisites are finished and every science pack it
     * costs can be made with what the finished researches unlocked (the arena seals are taken as given). Every research
     * must get its turn; recipes that a research unlocks are only usable afterwards.
     */
    @GameTest(template = "empty")
    public static void everyResearchCanBePaidWithWhatIsUnlockedBefore(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        List<Step> steps = steps(helper);
        List<Holder.Reference<Research>> open = new ArrayList<>(Researches.sorted(access));
        Set<String> done = new HashSet<>();
        List<String> order = new ArrayList<>();
        boolean progress = true;
        while (progress && !open.isEmpty()) {
            progress = false;
            Set<Item> have = closure(steps, step -> step.unlockId() == null || Researches.knows(access, done, step.unlockId()));
            for (var holder : new ArrayList<>(open)) {
                Research research = holder.value();
                if (done.containsAll(Researches.requires(research)) && research.packs().stream().allMatch(kind -> have.contains(pack(kind)))) {
                    done.add(Researches.id(holder));
                    order.add(Researches.id(holder));
                    open.remove(holder);
                    progress = true;
                }
            }
        }
        List<String> stuck = open.stream().map(holder -> Researches.id(holder) + " (" + holder.value().costLine(1.0) + ")").toList();
        helper.assertTrue(stuck.isEmpty(), "can never be researched: " + stuck + "; done in this order: " + order);
        helper.succeed();
    }

    private static final Item CRUDE_OIL = Items.BLACK_DYE;

    /** Items that stand for fluids while checking what can be made. */
    private static Item fluidStandIn(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == ModFluids.CRUDE_OIL.get()) {
            return CRUDE_OIL;
        }
        if (fluid == ModFluids.PETROLEUM_GAS.get()) {
            return Items.GREEN_DYE;
        }
        if (fluid == ModFluids.HEAVY_OIL.get()) {
            return Items.RED_DYE;
        }
        if (fluid == ModFluids.LIGHT_OIL.get()) {
            return Items.ORANGE_DYE;
        }
        if (fluid == ModFluids.LUBRICANT.get()) {
            return Items.BROWN_DYE;
        }
        return Items.YELLOW_DYE; // sulfuric acid
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
