package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.energy.AccumulatorBlockEntity;
import de.craftorio.energy.BoilerBlockEntity;
import de.craftorio.energy.SteamEngineBlockEntity;
import de.craftorio.fluid.FluidMachineBlockEntity;
import de.craftorio.fluid.FluidMachineType;
import de.craftorio.fluid.FluidPipeBlockEntity;
import de.craftorio.fluid.FluidRecipes;
import de.craftorio.fluid.FluidPumpBlock;
import de.craftorio.fluid.FluidPumpBlockEntity;
import de.craftorio.fluid.UndergroundPipeBlock;
import de.craftorio.machine.ProcessingMachineBlockEntity;
import de.craftorio.oil.OilWellBlock;
import de.craftorio.oil.PumpjackBlockEntity;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModFluids;
import de.craftorio.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The fluid network of package U6: pipes, tanks, pumps and steam. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FluidGameTests {
    private static final String LARGE = "empty_large";

    private FluidGameTests() {
    }

    private static int amount(GameTestHelper helper, BlockPos pos) {
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pos);
        return pipe.buffer().getFluidAmount();
    }

    /** Places a pipe the way a player does: with the arms to its neighbours. */
    private static void pipe(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.PIPE.get().connectedState(helper.getLevel(), helper.absolutePos(pos)));
    }

    /** Water at (0,1,3) and an offshore pump at (1,1,3). */
    private static void shore(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(1, 1, 3), ModBlocks.OFFSHORE_PUMP.get());
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void waterReachesABoilerThroughPipesAndItsSteamRunsAnEngine(GameTestHelper helper) {
        shore(helper);
        for (int x = 2; x <= 5; x++) {
            pipe(helper, new BlockPos(x, 1, 3));
        }
        BlockPos boiler = new BlockPos(6, 1, 3);
        BlockPos engine = new BlockPos(7, 1, 3);
        helper.setBlock(boiler, ModBlocks.BOILER.get());
        helper.setBlock(engine, ModBlocks.STEAM_ENGINE.get());
        BoilerBlockEntity boilerEntity = helper.getBlockEntity(boiler);
        boilerEntity.fuel().insertItem(0, new ItemStack(Items.COAL, 4), false);
        SteamEngineBlockEntity engineEntity = helper.getBlockEntity(engine);

        helper.succeedWhen(() -> helper.assertValueEqual(engineEntity.status(), SteamEngineBlockEntity.RUNNING, "engine fed through the pipes"));
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void aPipeCarriesSteamFromABoilerToAnEngineFarAway(GameTestHelper helper) {
        shore(helper);
        BlockPos boiler = new BlockPos(2, 1, 3);
        helper.setBlock(boiler, ModBlocks.BOILER.get());
        ((BoilerBlockEntity) helper.getBlockEntity(boiler)).fuel().insertItem(0, new ItemStack(Items.COAL, 4), false);
        for (int x = 3; x <= 8; x++) {
            pipe(helper, new BlockPos(x, 1, 3));
        }
        BlockPos engine = new BlockPos(9, 1, 3);
        helper.setBlock(engine, ModBlocks.STEAM_ENGINE.get());
        SteamEngineBlockEntity engineEntity = helper.getBlockEntity(engine);

        helper.succeedWhen(() -> helper.assertValueEqual(engineEntity.status(), SteamEngineBlockEntity.RUNNING, "engine fed with piped steam"));
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void aTankFillsAndEvensOutWithThePipeBehindIt(GameTestHelper helper) {
        shore(helper);
        BlockPos tank = new BlockPos(2, 1, 3);
        BlockPos pipe = new BlockPos(3, 1, 3);
        helper.setBlock(tank, ModBlocks.STORAGE_TANK.get());
        pipe(helper, pipe);
        FluidPipeBlockEntity tankEntity = helper.getBlockEntity(tank);
        helper.assertValueEqual(tankEntity.buffer().getCapacity(), 25_000, "tank capacity");

        helper.succeedWhen(() -> {
            helper.assertTrue(amount(helper, tank) > 100, "the tank collects water");
            helper.assertTrue(amount(helper, pipe) > 0, "and the pipe after it too");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void anUndergroundPipeJoinsItsTwoHalvesAcrossNineBlocks(GameTestHelper helper) {
        shore(helper);
        BlockPos entrance = new BlockPos(2, 1, 3);
        BlockPos exit = new BlockPos(12, 1, 3);
        BlockPos pipe = new BlockPos(13, 1, 3);
        helper.setBlock(entrance, ModBlocks.UNDERGROUND_PIPE.get().defaultBlockState().setValue(UndergroundPipeBlock.FACING, Direction.WEST));
        helper.setBlock(exit, ModBlocks.UNDERGROUND_PIPE.get().defaultBlockState().setValue(UndergroundPipeBlock.FACING, Direction.EAST));
        pipe(helper, pipe);
        helper.assertTrue(UndergroundPipeBlock.partnerOf(helper.getLevel(), helper.absolutePos(entrance)) != null, "the halves find each other");

        helper.succeedWhen(() -> helper.assertTrue(amount(helper, pipe) > 0, "water came through the tunnel"));
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void undergroundPipesTooFarApartStayApart(GameTestHelper helper) {
        shore(helper);
        BlockPos entrance = new BlockPos(1, 1, 5);
        BlockPos exit = new BlockPos(12, 1, 5);
        helper.setBlock(entrance, ModBlocks.UNDERGROUND_PIPE.get().defaultBlockState().setValue(UndergroundPipeBlock.FACING, Direction.WEST));
        helper.setBlock(exit, ModBlocks.UNDERGROUND_PIPE.get().defaultBlockState().setValue(UndergroundPipeBlock.FACING, Direction.EAST));
        helper.assertTrue(UndergroundPipeBlock.partnerOf(helper.getLevel(), helper.absolutePos(entrance)) == null, "10 blocks between the halves is too far");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void aPumpMovesFluidForPower(GameTestHelper helper) {
        BlockPos source = new BlockPos(2, 1, 3);
        BlockPos pump = new BlockPos(3, 1, 3);
        BlockPos target = new BlockPos(4, 1, 3);
        helper.setBlock(source, ModBlocks.STORAGE_TANK.get());
        helper.setBlock(target, ModBlocks.STORAGE_TANK.get());
        helper.setBlock(pump, ModBlocks.FLUID_PUMP.get().defaultBlockState().setValue(FluidPumpBlock.FACING, Direction.EAST));
        FluidPipeBlockEntity sourceEntity = helper.getBlockEntity(source);
        sourceEntity.buffer().fill(new FluidStack(Fluids.WATER, 5_000), IFluidHandler.FluidAction.EXECUTE);
        FluidPumpBlockEntity pumpEntity = helper.getBlockEntity(pump);
        pumpEntity.energy().setEnergy(400);

        helper.succeedWhen(() -> {
            helper.assertTrue(amount(helper, target) > 500, "the pump moved water to the second tank");
            helper.assertTrue(pumpEntity.energy().getEnergyStored() < 400, "and used power for it");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void aPipeDoesNotMixFluids(GameTestHelper helper) {
        BlockPos pipe = new BlockPos(2, 1, 3);
        pipe(helper, pipe);
        FluidPipeBlockEntity entity = helper.getBlockEntity(pipe);
        entity.buffer().fill(new FluidStack(Fluids.WATER, 50), IFluidHandler.FluidAction.EXECUTE);
        helper.assertValueEqual(entity.buffer().fill(new FluidStack(ModFluids.STEAM.get(), 50), IFluidHandler.FluidAction.SIMULATE), 0, "steam into a water pipe");
        helper.assertValueEqual(entity.buffer().fill(new FluidStack(Fluids.WATER, 80), IFluidHandler.FluidAction.SIMULATE), 50, "the pipe holds 100");
        helper.succeed();
    }

    private static FluidMachineBlockEntity machine(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.Block block, String recipe) {
        helper.setBlock(pos, block);
        FluidMachineBlockEntity machine = helper.getBlockEntity(pos);
        machine.select(Craftorio.id(recipe));
        machine.energy().setEnergy(20_000);
        return machine;
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void aChemicalPlantMakesPlasticFromCoalAndPetroleum(GameTestHelper helper) {
        FluidMachineBlockEntity plant = machine(helper, new BlockPos(2, 1, 3), ModBlocks.CHEMICAL_PLANT.get(), "chem/plastic_bar");
        helper.assertFalse(plant.items().isItemValid(0, new ItemStack(Items.IRON_INGOT)), "only the recipe's ingredient goes in");
        plant.items().insertItem(0, new ItemStack(Items.COAL, 3), false);
        helper.assertValueEqual(plant.fluids().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE), 0, "water is no ingredient");
        plant.fluids().fill(new FluidStack(ModFluids.PETROLEUM_GAS.get(), 60), IFluidHandler.FluidAction.EXECUTE);

        helper.succeedWhen(() -> helper.assertValueEqual(plant.items().getStackInSlot(FluidMachineType.OUTPUT_SLOT).getCount(), 6, "two plastic bars per coal"));
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void aChemicalPlantNeedsBothFluidsForSulfur(GameTestHelper helper) {
        FluidMachineBlockEntity plant = machine(helper, new BlockPos(2, 1, 3), ModBlocks.CHEMICAL_PLANT.get(), "chem/sulfur");
        plant.fluids().fill(new FluidStack(Fluids.WATER, 30), IFluidHandler.FluidAction.EXECUTE);
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(plant.items().getStackInSlot(FluidMachineType.OUTPUT_SLOT).isEmpty(), "no sulfur without petroleum");
            plant.fluids().fill(new FluidStack(ModFluids.PETROLEUM_GAS.get(), 30), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.succeedWhen(() -> helper.assertValueEqual(plant.items().getStackInSlot(FluidMachineType.OUTPUT_SLOT).getCount(), 2, "two sulfur"));
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void aRefineryTurnsCrudeOilIntoPetroleumForThePipesAroundIt(GameTestHelper helper) {
        FluidMachineBlockEntity refinery = machine(helper, new BlockPos(2, 1, 3), ModBlocks.OIL_REFINERY.get(), "oil/basic_oil_processing");
        refinery.fluids().fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 200), IFluidHandler.FluidAction.EXECUTE);
        helper.onEachTick(() -> refinery.energy().setEnergy(20_000)); // 420 kW for 5 s is more than the buffer holds
        BlockPos pipe = new BlockPos(3, 1, 3);
        pipe(helper, pipe);

        helper.succeedWhen(() -> helper.assertTrue(amount(helper, pipe) > 0 && ((FluidPipeBlockEntity) helper.getBlockEntity(pipe)).buffer()
                .getFluid().getFluid() == ModFluids.PETROLEUM_GAS.get(), "petroleum gas in the pipe next to the refinery"));
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void advancedOilProcessingFillsThreeOutputTanks(GameTestHelper helper) {
        FluidMachineBlockEntity refinery = machine(helper, new BlockPos(2, 1, 3), ModBlocks.OIL_REFINERY.get(), "oil/advanced_oil_processing");
        refinery.fluids().fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        refinery.fluids().fill(new FluidStack(Fluids.WATER, 50), IFluidHandler.FluidAction.EXECUTE);
        helper.onEachTick(() -> refinery.energy().setEnergy(20_000));

        helper.succeedWhen(() -> {
            int first = FluidMachineType.INPUT_TANKS;
            helper.assertValueEqual(refinery.tank(first).getFluidAmount(), 25, "heavy oil");
            helper.assertValueEqual(refinery.tank(first + 1).getFluidAmount(), 45, "light oil");
            helper.assertValueEqual(refinery.tank(first + 2).getFluidAmount(), 55, "petroleum gas");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void crackingTurnsHeavyOilAndWaterIntoLightOil(GameTestHelper helper) {
        FluidMachineBlockEntity plant = machine(helper, new BlockPos(2, 1, 3), ModBlocks.CHEMICAL_PLANT.get(), "chem/heavy_oil_cracking");
        plant.fluids().fill(new FluidStack(ModFluids.HEAVY_OIL.get(), 40), IFluidHandler.FluidAction.EXECUTE);
        plant.fluids().fill(new FluidStack(Fluids.WATER, 30), IFluidHandler.FluidAction.EXECUTE);
        helper.onEachTick(() -> plant.energy().setEnergy(20_000));

        helper.succeedWhen(() -> helper.assertValueEqual(plant.tank(FluidMachineType.INPUT_TANKS).getFluidAmount(), 30, "light oil"));
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void solidFuelComesFromPetroleumGas(GameTestHelper helper) {
        FluidMachineBlockEntity plant = machine(helper, new BlockPos(2, 1, 3), ModBlocks.CHEMICAL_PLANT.get(), "chem/solid_fuel_from_petroleum_gas");
        plant.fluids().fill(new FluidStack(ModFluids.PETROLEUM_GAS.get(), 40), IFluidHandler.FluidAction.EXECUTE);
        helper.onEachTick(() -> plant.energy().setEnergy(20_000));

        helper.succeedWhen(() -> helper.assertValueEqual(plant.items().getStackInSlot(FluidMachineType.OUTPUT_SLOT).getCount(), 2, "two solid fuel"));
    }

    @GameTest(template = LARGE, timeoutTicks = 400)
    public static void assemblingMachine2CraftsWithAFluidIngredient(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 3);
        helper.setBlock(pos, ModBlocks.ASSEMBLER_2.get());
        ProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        machine.setSelectedRecipe(Craftorio.id("assembling/electric_engine"));
        helper.onEachTick(() -> machine.energy().setEnergy(20_000));
        helper.assertTrue(machine.fluidHandler() != null, "assembling machine 2 has a fluid input");
        helper.assertValueEqual(machine.fluidHandler().fill(new FluidStack(ModFluids.SULFURIC_ACID.get(), 100), IFluidHandler.FluidAction.SIMULATE), 0,
                "only the recipe's fluid goes in");
        machine.fluidHandler().fill(new FluidStack(ModFluids.LUBRICANT.get(), 15), IFluidHandler.FluidAction.EXECUTE);
        machine.items().insertItem(0, new ItemStack(ModItems.MOTOR.get(), 1), false);
        machine.items().insertItem(1, new ItemStack(ModItems.CIRCUIT.get(), 2), false);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(machine.items().getStackInSlot(machine.type().outputSlot()).getCount(), 1, "an electric engine");
            helper.assertValueEqual(machine.fluid().getFluidAmount(), 0, "15 lubricant used");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void assemblingMachine1HasNoFluidInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 3);
        helper.setBlock(pos, ModBlocks.ASSEMBLER.get());
        ProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(machine.fluidHandler() == null, "assembling machine 1 takes no fluids");
        helper.assertFalse(ProcessingMachineBlockEntity.assemblerRecipes(helper.getLevel()).stream()
                .filter(holder -> holder.value().needsFluid()).map(holder -> holder.id()).toList().isEmpty(), "fluid recipes exist");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 1400)
    public static void aCentrifugeReprocessesUsedFuelCells(GameTestHelper helper) {
        FluidMachineBlockEntity centrifuge = machine(helper, new BlockPos(2, 1, 3), ModBlocks.CENTRIFUGE.get(), "centrifuge/nuclear_fuel_reprocessing");
        centrifuge.items().insertItem(0, new ItemStack(ModItems.USED_UP_FUEL_CELL.get(), 5), false);
        helper.onEachTick(() -> centrifuge.energy().setEnergy(20_000));
        helper.assertTrue(centrifuge.fluids().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE) == 0, "no fluids in a centrifuge");

        // 60 s at speed 1 = 1200 ticks
        helper.runAtTickTime(1100, () -> helper.assertTrue(centrifuge.items().getStackInSlot(FluidMachineType.SECOND_OUTPUT_SLOT).isEmpty(), "not finished after 55 s"));
        helper.succeedWhen(() -> {
            helper.assertValueEqual(centrifuge.items().getStackInSlot(FluidMachineType.SECOND_OUTPUT_SLOT).getCount(), 3, "3 uranium-238");
            helper.assertTrue(centrifuge.items().getStackInSlot(0).isEmpty(), "the cells are used up");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 1400)
    public static void kovarexTurnsFortyUranium235IntoFortyOne(GameTestHelper helper) {
        FluidMachineBlockEntity centrifuge = machine(helper, new BlockPos(2, 1, 3), ModBlocks.CENTRIFUGE.get(), "centrifuge/kovarex_enrichment");
        centrifuge.items().insertItem(0, new ItemStack(ModItems.URANIUM_235.get(), 40), false);
        centrifuge.items().insertItem(1, new ItemStack(ModItems.URANIUM_238.get(), 5), false);
        helper.onEachTick(() -> centrifuge.energy().setEnergy(20_000));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(centrifuge.items().getStackInSlot(FluidMachineType.OUTPUT_SLOT).getCount(), 41, "41 uranium-235");
            helper.assertValueEqual(centrifuge.items().getStackInSlot(FluidMachineType.SECOND_OUTPUT_SLOT).getCount(), 2, "2 uranium-238");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 500)
    public static void aCentrifugeUsesUpTenOreForEveryRun(GameTestHelper helper) {
        FluidMachineBlockEntity centrifuge = machine(helper, new BlockPos(2, 1, 3), ModBlocks.CENTRIFUGE.get(), "centrifuge/uranium_processing");
        centrifuge.items().insertItem(0, new ItemStack(ModItems.RAW_URANIUM.get(), 25), false);
        helper.onEachTick(() -> centrifuge.energy().setEnergy(20_000));

        helper.succeedWhen(() -> helper.assertValueEqual(centrifuge.items().getStackInSlot(0).getCount(), 5, "two runs of 10 ore each in 24 s"));
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void uraniumProcessingGivesAboutSevenPerMilleUranium235(GameTestHelper helper) {
        var recipe = FluidRecipes.byId(Craftorio.id("centrifuge/uranium_processing")).orElseThrow();
        helper.assertValueEqual(recipe.ticks(), 240, "12 s");
        var random = net.minecraft.util.RandomSource.create(42);
        int runs = 200_000;
        int u235 = 0;
        int u238 = 0;
        for (int i = 0; i < runs; i++) {
            var made = recipe.rollOutputs(random);
            u235 += made.get(0).getCount();
            u238 += made.get(1).getCount();
        }
        double share = (double) u235 / runs;
        helper.assertTrue(share > 0.005 && share < 0.009, "U-235 share " + share);
        helper.assertTrue(Math.abs((double) u238 / runs - 0.993) < 0.003, "U-238 share " + (double) u238 / runs);
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void aRefineryOfATeamThatKnowsOilProcessingCanSelectItsRecipe(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var registry = de.craftorio.team.TeamData.registry(player.server);
        var team = registry.ensureTeam(player.getUUID(), "RefineryTest");
        registry.grantResearch(team.id(), Craftorio.id("oil_processing").toString());
        BlockPos pos = new BlockPos(2, 1, 3);
        helper.setBlock(pos, ModBlocks.OIL_REFINERY.get());
        de.craftorio.protection.BlockOwnership.get(helper.getLevel()).claim(helper.absolutePos(pos), team.id());
        FluidMachineBlockEntity refinery = helper.getBlockEntity(pos);
        refinery.cycle(1);
        helper.assertTrue(refinery.selected() != null, "a recipe is selected");
        helper.assertValueEqual(refinery.selected().id(), Craftorio.id("oil/basic_oil_processing"), "basic oil processing is the one the team knows");
        helper.assertValueEqual(refinery.fluids().fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 50), IFluidHandler.FluidAction.SIMULATE), 50, "crude oil is accepted");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void aPumpjackOnAnOilWellFillsTheRefinery(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 3), ModBlocks.OIL_WELL.get());
        BlockPos pumpjack = new BlockPos(2, 2, 3);
        helper.setBlock(pumpjack, ModBlocks.PUMPJACK.get());
        ((PumpjackBlockEntity) helper.getBlockEntity(pumpjack)).energy().setEnergy(1_800);
        BlockPos refinery = new BlockPos(3, 2, 3);
        FluidMachineBlockEntity machine = machine(helper, refinery, ModBlocks.OIL_REFINERY.get(), "oil/basic_oil_processing");
        int yield = OilWellBlock.yieldPercent(helper.absolutePos(new BlockPos(2, 1, 3)));
        helper.assertTrue(yield >= 100 && yield <= 300 && yield % 25 == 0, "yield " + yield);

        helper.succeedWhen(() -> helper.assertTrue(machine.tank(0).getFluidAmount() > 0, "crude oil arrived in the refinery"));
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void aPumpjackWithoutAWellPumpsNothing(GameTestHelper helper) {
        BlockPos pumpjack = new BlockPos(2, 1, 3);
        helper.setBlock(pumpjack, ModBlocks.PUMPJACK.get());
        PumpjackBlockEntity entity = helper.getBlockEntity(pumpjack);
        entity.energy().setEnergy(1_800);
        helper.runAtTickTime(60, () -> {
            helper.assertValueEqual(entity.tank().getFluidAmount(), 0, "oil without a well");
            helper.succeed();
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void fluidRecipesFollowTheFactorioTable(GameTestHelper helper) {
        var battery = FluidRecipes.byId(Craftorio.id("chem/battery")).orElseThrow();
        helper.assertValueEqual(battery.ticks(), 80, "battery 4 s");
        helper.assertValueEqual(battery.fluidsIn().get(0).getAmount(), 20, "20 sulfuric acid per battery");
        var acid = FluidRecipes.byId(Craftorio.id("chem/sulfuric_acid")).orElseThrow();
        helper.assertValueEqual(acid.fluidsOut().get(0).getAmount(), 50, "50 sulfuric acid");
        helper.assertValueEqual(acid.itemsIn().get(0).getCount(), 5, "5 sulfur");
        var oil = FluidRecipes.byId(Craftorio.id("oil/basic_oil_processing")).orElseThrow();
        helper.assertValueEqual(oil.fluidsIn().get(0).getAmount() + ":" + oil.fluidsOut().get(0).getAmount(), "100:45", "basic oil processing");
        helper.assertValueEqual(oil.ticks(), 100, "5 s");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void anAccumulatorChargesFromSurplusAndGivesItBack(GameTestHelper helper) {
        // Engine → pole → accumulator, nobody uses the power: it charges.
        SteamPower.place(helper, new BlockPos(1, 1, 1), Direction.WEST, 4);
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.POWER_POLE.get());
        BlockPos accumulator = new BlockPos(3, 1, 2);
        helper.setBlock(accumulator, ModBlocks.ACCUMULATOR.get());
        AccumulatorBlockEntity entity = helper.getBlockEntity(accumulator);
        helper.succeedWhen(() -> helper.assertTrue(entity.energy().getEnergyStored() > 500, "the accumulator charged from the surplus"));
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void anAccumulatorPowersMachinesWithoutAGenerator(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.POWER_POLE.get());
        BlockPos accumulator = new BlockPos(3, 1, 2);
        helper.setBlock(accumulator, ModBlocks.ACCUMULATOR.get());
        ((AccumulatorBlockEntity) helper.getBlockEntity(accumulator)).energy().setEnergy(50_000);
        BlockPos furnace = new BlockPos(4, 1, 2);
        helper.setBlock(furnace, ModBlocks.ELECTRIC_FURNACE.get());

        helper.succeedWhen(() -> helper.assertTrue(energy(helper, furnace) > 0, "the furnace draws from the accumulator"));
    }

    private static int energy(GameTestHelper helper, BlockPos pos) {
        var storage = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), null);
        return storage == null ? 0 : storage.getEnergyStored();
    }

    @GameTest(template = LARGE, timeoutTicks = 700)
    public static void aGreenhouseGrowsWheatAndKeepsTheSeed(GameTestHelper helper) {
        FluidMachineBlockEntity greenhouse = machine(helper, new BlockPos(2, 1, 3), ModBlocks.GREENHOUSE.get(), "farm/wheat");
        helper.assertFalse(greenhouse.items().isItemValid(0, new ItemStack(Items.CARROT)), "only wheat seeds for wheat");
        greenhouse.items().insertItem(0, new ItemStack(Items.WHEAT_SEEDS), false);
        greenhouse.fluids().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        helper.onEachTick(() -> greenhouse.energy().setEnergy(20_000));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(greenhouse.items().getStackInSlot(FluidMachineType.OUTPUT_SLOT).getCount(), 3, "three wheat per harvest");
            helper.assertValueEqual(greenhouse.items().getStackInSlot(0).getCount(), 1, "the seed is still there");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void aGreenhouseUsesUpPumpkinSeeds(GameTestHelper helper) {
        FluidMachineBlockEntity greenhouse = machine(helper, new BlockPos(2, 1, 3), ModBlocks.GREENHOUSE.get(), "farm/pumpkin");
        greenhouse.items().insertItem(0, new ItemStack(Items.PUMPKIN_SEEDS, 2), false);
        greenhouse.fluids().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        helper.onEachTick(() -> greenhouse.energy().setEnergy(20_000));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(greenhouse.items().getStackInSlot(FluidMachineType.OUTPUT_SLOT).getCount(), 1, "one pumpkin");
            helper.assertTrue(greenhouse.items().getStackInSlot(0).getCount() <= 1, "a seed was used up");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void bioFuelBurnsLikeSolidFuel(GameTestHelper helper) {
        helper.assertTrue(Math.abs(de.craftorio.energy.Fuel.megajoules(new ItemStack(de.craftorio.registry.ModItems.BIO_FUEL.get())) - 12.0) < 0.01,
                "12 MJ");
        BlockPos boiler = new BlockPos(2, 1, 3);
        helper.setBlock(boiler, ModBlocks.BOILER.get());
        BoilerBlockEntity entity = helper.getBlockEntity(boiler);
        helper.assertTrue(entity.fuel().isItemValid(0, new ItemStack(de.craftorio.registry.ModItems.BIO_FUEL.get())), "boilers take bio fuel");
        helper.succeed();
    }
}
