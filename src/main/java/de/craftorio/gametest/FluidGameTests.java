package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.energy.BoilerBlockEntity;
import de.craftorio.energy.SteamEngineBlockEntity;
import de.craftorio.fluid.FluidPipeBlockEntity;
import de.craftorio.fluid.FluidPumpBlock;
import de.craftorio.fluid.FluidPumpBlockEntity;
import de.craftorio.fluid.UndergroundPipeBlock;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModFluids;
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
}
