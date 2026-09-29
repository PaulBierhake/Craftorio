package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.machine.MachineType;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.registry.ModFluids;
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
import net.neoforged.neoforge.fluids.FluidStack;
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
            new Object[]{"greenhouse", 1, List.of(of(Items.IRON_INGOT, 15), of(ModItems.IRON_GEAR.get(), 5), of(ModItems.CIRCUIT.get(), 3), of(ModItems.PIPE.get(), 5))},
            new Object[]{"rail", 2, List.of(of(Items.COBBLESTONE, 1), of(ModItems.IRON_STICK.get(), 1), of(ModItems.STEEL_PLATE.get(), 1))},
            new Object[]{"flying_robot_frame", 1, List.of(of(ModItems.ELECTRIC_ENGINE.get(), 1), of(ModItems.BATTERY.get(), 2), of(ModItems.STEEL_PLATE.get(), 1), of(ModItems.CIRCUIT.get(), 3))},
            new Object[]{"low_density_structure", 1, List.of(of(ModItems.STEEL_PLATE.get(), 2), of(Items.COPPER_INGOT, 20), of(ModItems.PLASTIC_BAR.get(), 5))},
            new Object[]{"centrifuge", 1, List.of(of(ModItems.CONCRETE.get(), 100), of(ModItems.STEEL_PLATE.get(), 50), of(ModItems.ADVANCED_CIRCUIT.get(), 100), of(ModItems.IRON_GEAR.get(), 100))},
            // Wiki 1.1 asks 500 of each for the reactor; a blueprint is built from the player's inventory, so it takes 100 (see docs §11)
            new Object[]{"reactor", 1, List.of(of(ModItems.CONCRETE.get(), 100), of(ModItems.STEEL_PLATE.get(), 100), of(ModItems.ADVANCED_CIRCUIT.get(), 100), of(Items.COPPER_INGOT, 100))},
            new Object[]{"heat_pipe", 1, List.of(of(Items.COPPER_INGOT, 20), of(ModItems.STEEL_PLATE.get(), 10))},
            new Object[]{"heat_exchanger", 1, List.of(of(Items.COPPER_INGOT, 100), of(ModItems.PIPE.get(), 10), of(ModItems.STEEL_PLATE.get(), 10))},
            new Object[]{"steam_turbine", 1, List.of(of(Items.COPPER_INGOT, 50), of(ModItems.IRON_GEAR.get(), 50), of(ModItems.PIPE.get(), 20))},
            new Object[]{"assembler_3", 1, List.of(of(ModItems.ASSEMBLER_2.get(), 2), of(ModItems.module(de.craftorio.module.ModuleKind.SPEED, 1).get(), 4))},
            new Object[]{"beacon", 1, List.of(of(ModItems.ADVANCED_CIRCUIT.get(), 20), of(ModItems.CIRCUIT.get(), 20), of(ModItems.STEEL_PLATE.get(), 10), of(ModItems.COPPER_CABLE.get(), 10))},
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
            new Object[]{"assembling", "rail", 2, 10, List.of(of(Items.COBBLESTONE, 1), of(ModItems.IRON_STICK.get(), 1), of(ModItems.STEEL_PLATE.get(), 1))},
            new Object[]{"assembling", "flying_robot_frame", 1, 400, List.of(of(ModItems.ELECTRIC_ENGINE.get(), 1), of(ModItems.BATTERY.get(), 2), of(ModItems.STEEL_PLATE.get(), 1), of(ModItems.CIRCUIT.get(), 3))},
            new Object[]{"assembling", "low_density_structure", 1, 400, List.of(of(ModItems.STEEL_PLATE.get(), 2), of(Items.COPPER_INGOT, 20), of(ModItems.PLASTIC_BAR.get(), 5))},
            new Object[]{"assembling", "uranium_fuel_cell", 10, 200, List.of(of(Items.IRON_INGOT, 10), of(ModItems.URANIUM_235.get(), 1), of(ModItems.URANIUM_238.get(), 19))},
            new Object[]{"assembling", "uranium_magazine", 1, 200, List.of(of(ModItems.AP_MAGAZINE.get(), 1), of(ModItems.URANIUM_238.get(), 1))},
            new Object[]{"assembling", "speed_module_1", 1, 300, List.of(of(ModItems.ADVANCED_CIRCUIT.get(), 5), of(ModItems.CIRCUIT.get(), 5))},
            new Object[]{"assembling", "efficiency_module_2", 1, 600, List.of(of(ModItems.module(de.craftorio.module.ModuleKind.EFFICIENCY, 1).get(), 4), of(ModItems.ADVANCED_CIRCUIT.get(), 5), of(ModItems.PROCESSING_UNIT.get(), 5))},
            new Object[]{"assembling", "productivity_module_3", 1, 1_200, List.of(of(ModItems.module(de.craftorio.module.ModuleKind.PRODUCTIVITY, 2).get(), 5), of(ModItems.ADVANCED_CIRCUIT.get(), 5), of(ModItems.PROCESSING_UNIT.get(), 5))},
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

    private record FluidIn(net.minecraft.world.level.material.Fluid fluid, int amount) {
    }

    /** Assembling recipes with fluid ingredients (assembling machine 2 and up): id, result count, ticks, fluid, ingredients. */
    private static final List<Object[]> FLUID_ASSEMBLING = List.of(
            new Object[]{"concrete", 10, 200, new FluidIn(net.minecraft.world.level.material.Fluids.WATER, 100), List.of(of(ModItems.STONE_BRICK.get(), 5), of(Items.RAW_IRON, 1))},
            new Object[]{"processing_unit", 1, 200, new FluidIn(ModFluids.SULFURIC_ACID.get(), 5), List.of(of(ModItems.CIRCUIT.get(), 20), of(ModItems.ADVANCED_CIRCUIT.get(), 2))},
            new Object[]{"electric_engine", 1, 200, new FluidIn(ModFluids.LUBRICANT.get(), 15), List.of(of(ModItems.MOTOR.get(), 1), of(ModItems.CIRCUIT.get(), 2))},
            new Object[]{"express_belt", 1, 10, new FluidIn(ModFluids.LUBRICANT.get(), 20), List.of(of(ModItems.FAST_BELT.get(), 1), of(ModItems.IRON_GEAR.get(), 10))}
    );

    @GameTest(template = "empty")
    public static void fluidAssemblingRecipesMatchTheFactorioTable(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        List<String> problems = new ArrayList<>();
        for (Object[] row : FLUID_ASSEMBLING) {
            String id = "assembling/" + row[0];
            RecipeHolder<MachineRecipe> holder = recipes.getAllRecipesFor(ModRecipes.ASSEMBLING.get()).stream()
                    .filter(candidate -> candidate.id().equals(Craftorio.id(id))).findFirst().orElse(null);
            if (holder == null) {
                problems.add(id + " is missing");
                continue;
            }
            @SuppressWarnings("unchecked") List<Ingredient> expected = (List<Ingredient>) row[4];
            check(problems, id, holder.value().result().getCount(), (int) row[1], holder.value().ingredients(), expected);
            if (holder.value().time() != (int) row[2]) {
                problems.add(id + " takes " + holder.value().time() + " ticks instead of " + row[2]);
            }
            FluidIn fluid = (FluidIn) row[3];
            if (holder.value().fluids().size() != 1 || holder.value().fluids().get(0).getFluid() != fluid.fluid()
                    || holder.value().fluids().get(0).getAmount() != fluid.amount()) {
                problems.add(id + " needs " + holder.value().fluids() + " instead of " + fluid.amount() + " " + fluid.fluid());
            }
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    private static FluidStack fluid(net.minecraft.world.level.material.Fluid fluid, int amount) {
        return new FluidStack(fluid, amount);
    }

    /** Oil and chemical recipes of Factorio 1.1 (times in ticks = seconds × 20): id, ticks, item in, fluids in, item out, fluids out. */
    @GameTest(template = "empty")
    public static void oilAndChemistryRecipesMatchTheFactorioTable(GameTestHelper helper) {
        var water = net.minecraft.world.level.material.Fluids.WATER;
        var crude = ModFluids.CRUDE_OIL.get();
        var heavy = ModFluids.HEAVY_OIL.get();
        var light = ModFluids.LIGHT_OIL.get();
        var gas = ModFluids.PETROLEUM_GAS.get();
        var acid = ModFluids.SULFURIC_ACID.get();
        var lubricant = ModFluids.LUBRICANT.get();
        record Row(String id, int ticks, List<net.minecraft.world.item.ItemStack> itemsIn, List<FluidStack> fluidsIn,
                   net.minecraft.world.item.ItemStack itemOut, List<FluidStack> fluidsOut) {
        }
        var none = net.minecraft.world.item.ItemStack.EMPTY;
        List<Row> table = List.of(
                new Row("oil/basic_oil_processing", 100, List.of(), List.of(fluid(crude, 100)), none, List.of(fluid(gas, 45))),
                new Row("oil/advanced_oil_processing", 100, List.of(), List.of(fluid(crude, 100), fluid(water, 50)), none,
                        List.of(fluid(heavy, 25), fluid(light, 45), fluid(gas, 55))),
                new Row("chem/heavy_oil_cracking", 40, List.of(), List.of(fluid(heavy, 40), fluid(water, 30)), none, List.of(fluid(light, 30))),
                new Row("chem/light_oil_cracking", 40, List.of(), List.of(fluid(light, 30), fluid(water, 30)), none, List.of(fluid(gas, 20))),
                new Row("chem/lubricant", 20, List.of(), List.of(fluid(heavy, 10)), none, List.of(fluid(lubricant, 10))),
                new Row("chem/solid_fuel_from_light_oil", 40, List.of(), List.of(fluid(light, 10)), new net.minecraft.world.item.ItemStack(ModItems.SOLID_FUEL.get()), List.of()),
                new Row("chem/solid_fuel_from_heavy_oil", 40, List.of(), List.of(fluid(heavy, 20)), new net.minecraft.world.item.ItemStack(ModItems.SOLID_FUEL.get()), List.of()),
                new Row("chem/solid_fuel_from_petroleum_gas", 40, List.of(), List.of(fluid(gas, 20)), new net.minecraft.world.item.ItemStack(ModItems.SOLID_FUEL.get()), List.of()),
                new Row("chem/plastic_bar", 20, List.of(new net.minecraft.world.item.ItemStack(Items.COAL)), List.of(fluid(gas, 20)),
                        new net.minecraft.world.item.ItemStack(ModItems.PLASTIC_BAR.get(), 2), List.of()),
                new Row("chem/sulfur", 20, List.of(), List.of(fluid(water, 30), fluid(gas, 30)), new net.minecraft.world.item.ItemStack(ModItems.SULFUR.get(), 2), List.of()),
                new Row("chem/sulfuric_acid", 20, List.of(new net.minecraft.world.item.ItemStack(ModItems.SULFUR.get(), 5), new net.minecraft.world.item.ItemStack(Items.IRON_INGOT)),
                        List.of(fluid(water, 100)), none, List.of(fluid(acid, 50))),
                new Row("chem/battery", 80, List.of(new net.minecraft.world.item.ItemStack(Items.IRON_INGOT), new net.minecraft.world.item.ItemStack(Items.COPPER_INGOT)),
                        List.of(fluid(acid, 20)), new net.minecraft.world.item.ItemStack(ModItems.BATTERY.get()), List.of()));
        List<String> problems = new ArrayList<>();
        for (Row row : table) {
            var recipe = de.craftorio.fluid.FluidRecipes.byId(Craftorio.id(row.id())).orElse(null);
            if (recipe == null) {
                problems.add(row.id() + " is missing");
                continue;
            }
            if (recipe.ticks() != row.ticks()) {
                problems.add(row.id() + " takes " + recipe.ticks() + " ticks instead of " + row.ticks());
            }
            if (!sameStacks(recipe.itemsIn(), row.itemsIn()) || !sameFluids(recipe.fluidsIn(), row.fluidsIn())
                    || !sameFluids(recipe.fluidsOut(), row.fluidsOut())
                    || !net.minecraft.world.item.ItemStack.matches(recipe.itemOut(), row.itemOut())) {
                problems.add(row.id() + " has other ingredients or products than the table");
            }
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** Centrifuge recipes of Factorio 1.1: id, ticks, ingredients, products as item × count × chance × output slot. */
    @GameTest(template = "empty")
    public static void centrifugeRecipesMatchTheFactorioTable(GameTestHelper helper) {
        record Out(net.minecraft.world.item.Item item, int count, double chance, int slot) {
        }
        record Row(String id, int ticks, List<Ingredient> in, List<Out> out) {
        }
        List<Row> table = List.of(
                new Row("centrifuge/uranium_processing", 240, List.of(of(ModItems.RAW_URANIUM.get(), 10)),
                        List.of(new Out(ModItems.URANIUM_235.get(), 1, 0.007, 0), new Out(ModItems.URANIUM_238.get(), 1, 0.993, 1))),
                new Row("centrifuge/nuclear_fuel_reprocessing", 1_200, List.of(of(ModItems.USED_UP_FUEL_CELL.get(), 5)),
                        List.of(new Out(ModItems.URANIUM_238.get(), 3, 1, 1))),
                new Row("centrifuge/kovarex_enrichment", 1_200, List.of(of(ModItems.URANIUM_235.get(), 40), of(ModItems.URANIUM_238.get(), 5)),
                        List.of(new Out(ModItems.URANIUM_235.get(), 41, 1, 0), new Out(ModItems.URANIUM_238.get(), 2, 1, 1))));
        List<String> problems = new ArrayList<>();
        for (Row row : table) {
            var recipe = de.craftorio.fluid.FluidRecipes.byId(Craftorio.id(row.id())).orElse(null);
            if (recipe == null) {
                problems.add(row.id() + " is missing");
                continue;
            }
            if (recipe.machine() != de.craftorio.fluid.FluidMachineType.CENTRIFUGE || recipe.ticks() != row.ticks()) {
                problems.add(row.id() + " runs " + recipe.ticks() + " ticks in " + recipe.machine());
            }
            boolean sameIn = recipe.itemsIn().size() == row.in().size();
            for (int i = 0; sameIn && i < row.in().size(); i++) {
                sameIn = recipe.itemsIn().get(i).is(row.in().get(i).item()) && recipe.itemsIn().get(i).getCount() == row.in().get(i).count();
            }
            boolean sameOut = recipe.itemsOut().size() == row.out().size();
            for (int i = 0; sameOut && i < row.out().size(); i++) {
                var found = recipe.itemsOut().get(i);
                var wanted = row.out().get(i);
                sameOut = found.stack().is(wanted.item()) && found.stack().getCount() == wanted.count()
                        && found.chance() == wanted.chance() && found.slot() == wanted.slot();
            }
            if (!sameIn || !sameOut) {
                problems.add(row.id() + " has other ingredients or products than the table");
            }
        }
        // nuclear power (wiki 1.1): 40 MW reactor, 200 s cell, 10 MW / 103 steam per s exchanger, 60 steam per s and 5.82 MW turbine
        helper.assertValueEqual(de.craftorio.heat.HeatLogic.REACTOR_HEAT, 40 * 1000, "reactor 40 MW");
        helper.assertValueEqual(de.craftorio.heat.HeatLogic.CELL_TICKS, 200 * 20, "a cell burns 200 s");
        helper.assertValueEqual(de.craftorio.heat.HeatLogic.EXCHANGER_HEAT, 10 * 1000, "exchanger 10 MW");
        helper.assertValueEqual(de.craftorio.heat.HeatLogic.TURBINE_POWER, 5_820, "turbine 5.82 MW");
        helper.assertValueEqual(de.craftorio.heat.HeatLogic.TURBINE_STEAM_PER_TICK * 20, 60, "turbine 60 steam/s");
        helper.assertValueEqual(de.craftorio.heat.HeatLogic.STEAM_TEMPERATURE, 500.0, "steam is made from 500 °C");
        helper.assertValueEqual(de.craftorio.machine.DrillBlockEntity.ACID_PER_ORE, 1, "10 acid per 10 uranium ore");
        helper.assertValueEqual(de.craftorio.machine.DrillBlockEntity.URANIUM_SLOWDOWN, 2, "uranium ore is mined at half speed");
        helper.assertValueEqual(de.craftorio.fluid.FluidMachineType.CENTRIFUGE.power(), 350, "centrifuge 350 kW");
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    private static boolean sameStacks(List<net.minecraft.world.item.ItemStack> a, List<net.minecraft.world.item.ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!net.minecraft.world.item.ItemStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameFluids(List<FluidStack> a, List<FluidStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!FluidStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Research costs of Factorio 1.1: id, units, seconds per unit, packs. */
    @GameTest(template = "empty")
    public static void researchCostsMatchTheFactorioTable(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(ModRegistries.RESEARCH);
        var rg = List.of(de.craftorio.research.Research.Pack.RED, de.craftorio.research.Research.Pack.GREEN);
        var rgb = List.of(de.craftorio.research.Research.Pack.RED, de.craftorio.research.Research.Pack.GREEN, de.craftorio.research.Research.Pack.BLUE);
        List<Object[]> table = List.of(
                new Object[]{"advanced_material_processing_2", 250L, 30, rgb},
                new Object[]{"advanced_oil_processing", 75L, 30, rgb},
                new Object[]{"lubricant", 50L, 30, rgb},
                new Object[]{"concrete", 250L, 30, rg},
                new Object[]{"railway", 75L, 30, rg},
                new Object[]{"electric_engine", 50L, 30, rgb},
                new Object[]{"robotics", 75L, 30, rgb},
                new Object[]{"advanced_electronics_2", 300L, 30, rgb},
                new Object[]{"low_density_structure", 300L, 45, rgb},
                new Object[]{"modules", 100L, 30, rg},
                new Object[]{"speed_module", 50L, 30, rg},
                new Object[]{"efficiency_module", 50L, 30, rg},
                new Object[]{"productivity_module", 50L, 30, rg},
                new Object[]{"speed_module_2", 75L, 30, rgb},
                new Object[]{"efficiency_module_2", 75L, 30, rgb},
                new Object[]{"productivity_module_2", 75L, 30, rgb},
                new Object[]{"uranium_processing", 200L, 30, rgb},
                new Object[]{"nuclear_power", 800L, 30, rgb});
        List<String> problems = new ArrayList<>();
        for (Object[] row : table) {
            var research = registry.get(Craftorio.id((String) row[0]));
            if (research == null) {
                problems.add(row[0] + " is missing");
            } else if (research.units() != (long) row[1] || research.seconds() != (int) row[2] || !research.packs().equals(row[3])) {
                problems.add(row[0] + " costs " + research.costLine(1.0) + " instead of " + row[1] + "×" + row[2] + " s " + row[3]);
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

    /** Every blueprint has a description in both languages (craftorio.blueprint.&lt;id&gt;.desc). */
    @GameTest(template = "empty")
    public static void everyBlueprintHasADescriptionInBothLanguages(GameTestHelper helper) throws java.io.IOException {
        var registry = helper.getLevel().registryAccess().registryOrThrow(ModRegistries.BLUEPRINTS);
        List<String> problems = new ArrayList<>();
        for (String language : List.of("en_us", "de_de")) {
            try (var stream = RecipeTableGameTests.class.getResourceAsStream("/assets/craftorio/lang/" + language + ".json")) {
                if (stream == null) {
                    problems.add("no lang file " + language);
                    continue;
                }
                var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                registry.keySet().forEach(id -> {
                    var text = json.get("craftorio.blueprint." + id.getPath() + ".desc");
                    if (text == null || text.getAsString().isBlank()) {
                        problems.add(id.getPath() + " has no description in " + language);
                    }
                });
            }
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }
}
