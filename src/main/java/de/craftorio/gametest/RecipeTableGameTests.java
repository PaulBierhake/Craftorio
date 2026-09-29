package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.machine.MachineType;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.registry.ModItems;
import de.craftorio.registry.ModRecipes;
import de.craftorio.registry.ModRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The recipe table of docs/FACTORIO-UMBAU.md §4 (Factorio 1.1) as test data: the generated recipes must match it
 * in amounts and times. Where the game deliberately differs, the difference is listed under "Deviations" below.
 *
 * <p>Deviations: iron and copper plates are the vanilla ingots; wood is any log; assembler times are given at speed 1
 * (the assembling machine 1 then takes twice as long); electric furnace and steam recipes follow with later packages.
 */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeTableGameTests {
    private RecipeTableGameTests() {
    }

    private record Ingredient(Item item, int count) {
    }

    private static Ingredient of(ItemLike item, int count) {
        return new Ingredient(item.asItem(), count);
    }

    /** Workbench recipes: id, result count, ingredients. */
    private static final List<Object[]> BLUEPRINTS = List.of(
            new Object[]{"iron_gear", 1, List.of(of(Items.IRON_INGOT, 2))},
            new Object[]{"copper_cable", 2, List.of(of(Items.COPPER_INGOT, 1))},
            new Object[]{"circuit", 1, List.of(of(Items.IRON_INGOT, 1), of(ModItems.COPPER_CABLE.get(), 3))},
            new Object[]{"pipe", 1, List.of(of(Items.IRON_INGOT, 1))},
            new Object[]{"stone_furnace", 1, List.of(of(Items.COBBLESTONE, 5))},
            new Object[]{"burner_drill", 1, List.of(of(ModItems.IRON_GEAR.get(), 3), of(ModItems.STONE_FURNACE.get(), 1), of(Items.IRON_INGOT, 3))},
            new Object[]{"conveyor_belt", 2, List.of(of(ModItems.IRON_GEAR.get(), 1), of(Items.IRON_INGOT, 1))},
            new Object[]{"inserter", 1, List.of(of(ModItems.CIRCUIT.get(), 1), of(ModItems.IRON_GEAR.get(), 1), of(Items.IRON_INGOT, 1))},
            new Object[]{"boiler", 1, List.of(of(ModItems.STONE_FURNACE.get(), 1), of(ModItems.PIPE.get(), 4))},
            new Object[]{"steam_engine", 1, List.of(of(ModItems.IRON_GEAR.get(), 8), of(ModItems.PIPE.get(), 5), of(Items.IRON_INGOT, 10))},
            new Object[]{"offshore_pump", 1, List.of(of(ModItems.CIRCUIT.get(), 2), of(ModItems.PIPE.get(), 1), of(ModItems.IRON_GEAR.get(), 1))},
            new Object[]{"laboratory", 1, List.of(of(ModItems.CIRCUIT.get(), 10), of(ModItems.IRON_GEAR.get(), 10), of(ModItems.CONVEYOR_BELT.get(), 4))},
            new Object[]{"electric_drill", 1, List.of(of(ModItems.CIRCUIT.get(), 3), of(ModItems.IRON_GEAR.get(), 5), of(Items.IRON_INGOT, 10))},
            new Object[]{"assembler", 1, List.of(of(ModItems.CIRCUIT.get(), 3), of(ModItems.IRON_GEAR.get(), 5), of(Items.IRON_INGOT, 9))},
            new Object[]{"red_science", 1, List.of(of(Items.COPPER_INGOT, 1), of(ModItems.IRON_GEAR.get(), 1))},
            new Object[]{"fast_belt", 1, List.of(of(ModItems.IRON_GEAR.get(), 5), of(ModItems.CONVEYOR_BELT.get(), 1))},
            new Object[]{"underground_belt", 2, List.of(of(Items.IRON_INGOT, 10), of(ModItems.CONVEYOR_BELT.get(), 5))},
            new Object[]{"splitter", 1, List.of(of(ModItems.CIRCUIT.get(), 5), of(Items.IRON_INGOT, 5), of(ModItems.CONVEYOR_BELT.get(), 4))},
            new Object[]{"long_inserter", 1, List.of(of(ModItems.INSERTER.get(), 1), of(ModItems.IRON_GEAR.get(), 1), of(Items.IRON_INGOT, 1))},
            new Object[]{"fast_inserter", 1, List.of(of(ModItems.INSERTER.get(), 1), of(ModItems.CIRCUIT.get(), 2), of(Items.IRON_INGOT, 2))},
            new Object[]{"filter_inserter", 1, List.of(of(ModItems.FAST_INSERTER.get(), 1), of(ModItems.CIRCUIT.get(), 4))},
            new Object[]{"fast_underground_belt", 2, List.of(of(ModItems.IRON_GEAR.get(), 40), of(ModItems.UNDERGROUND_BELT.get(), 2))},
            new Object[]{"fast_splitter", 1, List.of(of(ModItems.SPLITTER.get(), 1), of(ModItems.IRON_GEAR.get(), 10), of(ModItems.CIRCUIT.get(), 10))},
            new Object[]{"gun_turret", 1, List.of(of(ModItems.IRON_GEAR.get(), 10), of(Items.COPPER_INGOT, 10), of(Items.IRON_INGOT, 20))},
            new Object[]{"iron_stick", 2, List.of(of(Items.IRON_INGOT, 1))},
            new Object[]{"green_science", 1, List.of(of(ModItems.INSERTER.get(), 1), of(ModItems.CONVEYOR_BELT.get(), 1))},
            new Object[]{"medium_power_pole", 1, List.of(of(ModItems.STEEL_PLATE.get(), 2), of(Items.COPPER_INGOT, 2), of(ModItems.IRON_STICK.get(), 4))},
            new Object[]{"assembler_2", 1, List.of(of(ModItems.STEEL_PLATE.get(), 2), of(ModItems.CIRCUIT.get(), 3), of(ModItems.IRON_GEAR.get(), 5), of(ModItems.ASSEMBLER.get(), 1))},
            new Object[]{"steel_furnace", 1, List.of(of(ModItems.STEEL_PLATE.get(), 6), of(ModItems.STONE_BRICK.get(), 10))},
            new Object[]{"solar_panel", 1, List.of(of(ModItems.STEEL_PLATE.get(), 5), of(ModItems.CIRCUIT.get(), 15), of(Items.COPPER_INGOT, 5))},
            new Object[]{"underground_pipe", 2, List.of(of(ModItems.PIPE.get(), 10), of(Items.IRON_INGOT, 5))},
            new Object[]{"fluid_pump", 1, List.of(of(ModItems.MOTOR.get(), 1), of(ModItems.STEEL_PLATE.get(), 1), of(ModItems.PIPE.get(), 1))},
            new Object[]{"storage_tank", 1, List.of(of(Items.IRON_INGOT, 20), of(ModItems.STEEL_PLATE.get(), 5))},
            new Object[]{"pumpjack", 1, List.of(of(ModItems.STEEL_PLATE.get(), 5), of(ModItems.IRON_GEAR.get(), 10), of(ModItems.CIRCUIT.get(), 5), of(ModItems.PIPE.get(), 10))},
            new Object[]{"oil_refinery", 1, List.of(of(ModItems.STEEL_PLATE.get(), 15), of(ModItems.IRON_GEAR.get(), 10), of(ModItems.STONE_BRICK.get(), 10), of(ModItems.CIRCUIT.get(), 10), of(ModItems.PIPE.get(), 10))},
            new Object[]{"chemical_plant", 1, List.of(of(ModItems.STEEL_PLATE.get(), 5), of(ModItems.IRON_GEAR.get(), 5), of(ModItems.CIRCUIT.get(), 5), of(ModItems.PIPE.get(), 5))},
            new Object[]{"accumulator", 1, List.of(of(Items.IRON_INGOT, 2), of(ModItems.BATTERY.get(), 5))},
            new Object[]{"chemical_science", 2, List.of(of(ModItems.MOTOR.get(), 2), of(ModItems.ADVANCED_CIRCUIT.get(), 3), of(ModItems.SULFUR.get(), 1))},
            new Object[]{"laser_tower", 1, List.of(of(ModItems.STEEL_PLATE.get(), 20), of(ModItems.CIRCUIT.get(), 20), of(ModItems.BATTERY.get(), 12))},
            new Object[]{"magazine", 1, List.of(of(Items.IRON_INGOT, 4))},
            new Object[]{"ap_magazine", 1, List.of(of(ModItems.MAGAZINE.get(), 1), of(ModItems.STEEL_PLATE.get(), 1), of(Items.COPPER_INGOT, 5))},
            new Object[]{"grenade", 1, List.of(of(Items.COAL, 5), of(Items.IRON_INGOT, 5))},
            new Object[]{"stone_wall", 1, List.of(of(ModItems.STONE_BRICK.get(), 5))},
            new Object[]{"military_science", 2, List.of(of(ModItems.AP_MAGAZINE.get(), 1), of(ModItems.GRENADE.get(), 1), of(ModItems.STONE_WALL.get(), 2))},
            new Object[]{"flamethrower_turret", 1, List.of(of(ModItems.STEEL_PLATE.get(), 30), of(ModItems.IRON_GEAR.get(), 15), of(ModItems.PIPE.get(), 10), of(ModItems.MOTOR.get(), 5))},
            new Object[]{"tesla_tower", 1, List.of(of(ModItems.COPPER_CABLE.get(), 24), of(Items.IRON_INGOT, 8), of(ModItems.CIRCUIT.get(), 4))}
    );

    /** Machine recipes: kind, id, result count, time in ticks at speed 1, ingredients. */
    private static final List<Object[]> MACHINE_RECIPES = List.of(
            new Object[]{"assembling", "iron_gear", 1, 10, List.of(of(Items.IRON_INGOT, 2))},
            new Object[]{"assembling", "copper_cable", 2, 10, List.of(of(Items.COPPER_INGOT, 1))},
            new Object[]{"assembling", "circuit", 1, 10, List.of(of(ModItems.COPPER_CABLE.get(), 3), of(Items.IRON_INGOT, 1))},
            new Object[]{"assembling", "pipe", 1, 10, List.of(of(Items.IRON_INGOT, 1))},
            new Object[]{"assembling", "red_science", 1, 100, List.of(of(Items.COPPER_INGOT, 1), of(ModItems.IRON_GEAR.get(), 1))},
            new Object[]{"assembling", "iron_stick", 2, 10, List.of(of(Items.IRON_INGOT, 1))},
            new Object[]{"assembling", "green_science", 1, 120, List.of(of(ModItems.INSERTER.get(), 1), of(ModItems.CONVEYOR_BELT.get(), 1))},
            new Object[]{"assembling", "motor", 1, 200, List.of(of(ModItems.STEEL_PLATE.get(), 1), of(ModItems.IRON_GEAR.get(), 1), of(ModItems.PIPE.get(), 2))},
            new Object[]{"assembling", "advanced_circuit", 1, 120, List.of(of(ModItems.CIRCUIT.get(), 2), of(ModItems.PLASTIC_BAR.get(), 2), of(ModItems.COPPER_CABLE.get(), 4))},
            new Object[]{"assembling", "blue_science", 2, 480, List.of(of(ModItems.MOTOR.get(), 2), of(ModItems.ADVANCED_CIRCUIT.get(), 3), of(ModItems.SULFUR.get(), 1))},
            new Object[]{"assembling", "military_science", 2, 200, List.of(of(ModItems.AP_MAGAZINE.get(), 1), of(ModItems.GRENADE.get(), 1), of(ModItems.STONE_WALL.get(), 2))},
            new Object[]{"assembling", "grenade", 1, 160, List.of(of(Items.COAL, 5), of(Items.IRON_INGOT, 5))},
            new Object[]{"assembling", "ap_magazine", 1, 60, List.of(of(ModItems.MAGAZINE.get(), 1), of(ModItems.STEEL_PLATE.get(), 1), of(Items.COPPER_INGOT, 5))},
            new Object[]{"smelting", "steel_plate", 1, 320, List.of(of(Items.IRON_INGOT, 5))},
            new Object[]{"smelting", "stone_brick", 1, 64, List.of(of(Items.COBBLESTONE, 2))}
    );

    @GameTest(template = "empty")
    public static void workbenchRecipesMatchTheFactorioTable(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(ModRegistries.BLUEPRINTS);
        List<String> problems = new ArrayList<>();
        for (Object[] row : BLUEPRINTS) {
            String id = (String) row[0];
            Blueprint blueprint = registry.get(Craftorio.id(id));
            if (blueprint == null) {
                problems.add(id + " is missing");
                continue;
            }
            @SuppressWarnings("unchecked") List<Ingredient> expected = (List<Ingredient>) row[2];
            check(problems, id, blueprint.result().getCount(), (int) row[1], blueprint.ingredients(), expected);
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void machineRecipesMatchTheFactorioTable(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        List<String> problems = new ArrayList<>();
        for (Object[] row : MACHINE_RECIPES) {
            String kind = (String) row[0];
            String id = (String) row[1];
            var type = kind.equals("assembling") ? ModRecipes.ASSEMBLING.get() : ModRecipes.SMELTING.get();
            RecipeHolder<MachineRecipe> holder = recipes.getAllRecipesFor(type).stream()
                    .filter(candidate -> candidate.id().equals(Craftorio.id(kind + "/" + id))).findFirst().orElse(null);
            if (holder == null) {
                problems.add(kind + "/" + id + " is missing");
                continue;
            }
            @SuppressWarnings("unchecked") List<Ingredient> expected = (List<Ingredient>) row[4];
            check(problems, kind + "/" + id, holder.value().result().getCount(), (int) row[2], holder.value().ingredients(), expected);
            if (holder.value().time() != (int) row[3]) {
                problems.add(kind + "/" + id + " takes " + holder.value().time() + " ticks instead of " + row[3]);
            }
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void machineSpeedsAndSmeltingTimesFollowFactorio(GameTestHelper helper) {
        helper.assertValueEqual(MachineType.smeltingTicks(200), 64, "a plate takes 3.2 s in a stone furnace");
        helper.assertValueEqual(MachineType.STONE_FURNACE.ticks(MachineType.smeltingTicks(200)), 64, "stone furnace, speed 1");
        helper.assertValueEqual(MachineType.ELECTRIC_FURNACE.ticks(MachineType.smeltingTicks(200)), 32, "electric furnace, speed 2");
        helper.assertValueEqual(MachineType.ASSEMBLER.ticks(10), 20, "assembling machine 1, speed 0.5");
        helper.assertValueEqual(MachineType.STEEL_FURNACE.ticks(MachineType.smeltingTicks(200)), 32, "steel furnace, speed 2");
        helper.assertValueEqual(MachineType.ASSEMBLER_2.ticks(30), 40, "assembling machine 2, speed 0.75");
        helper.assertValueEqual(MachineType.ASSEMBLER_2.energyPerTick(), 150, "assembling machine 2 150 kW");
        helper.assertValueEqual(MachineType.STONE_FURNACE.energyPerTick(), 90, "stone furnace 90 kW");
        helper.assertValueEqual(MachineType.ASSEMBLER.energyPerTick(), 75, "assembling machine 1 75 kW");
        helper.succeed();
    }

    private static void check(List<String> problems, String id, int resultCount, int expectedCount, List<SizedIngredient> actual,
                              List<Ingredient> expected) {
        if (resultCount != expectedCount) {
            problems.add(id + " makes " + resultCount + " instead of " + expectedCount);
        }
        if (actual.size() != expected.size()) {
            problems.add(id + " has " + actual.size() + " ingredients instead of " + expected.size());
            return;
        }
        for (int i = 0; i < expected.size(); i++) {
            SizedIngredient found = actual.get(i);
            if (found.count() != expected.get(i).count() || !found.ingredient().test(expected.get(i).item().getDefaultInstance())) {
                problems.add(id + " ingredient " + i + " is not " + expected.get(i).count() + "× " + expected.get(i).item());
            }
        }
    }
}
