package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.energy.BoilerBlockEntity;
import de.craftorio.energy.SteamEngineBlockEntity;
import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Boiler, steam engine and offshore pump. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteamPowerGameTests {
    private static final String EMPTY = "empty";

    private SteamPowerGameTests() {
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void engineMakesPowerFromCoalAndWater(GameTestHelper helper) {
        BlockPos engine = new BlockPos(3, 1, 2);
        SteamPower.place(helper, engine, Direction.WEST, 2);
        SteamEngineBlockEntity engineEntity = helper.getBlockEntity(engine);
        BoilerBlockEntity boiler = helper.getBlockEntity(engine.west());

        helper.succeedWhen(() -> {
            helper.assertValueEqual(engineEntity.status(), SteamEngineBlockEntity.RUNNING, "engine status");
            helper.assertTrue(engineEntity.energy().getEnergyStored() > 0, "engine made no power");
            helper.assertTrue(boiler.fuel().getStackInSlot(0).getCount() < 2, "the boiler burned no coal");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void noSteamWithoutAnOffshorePumpAtTheWater(GameTestHelper helper) {
        BlockPos engine = new BlockPos(3, 1, 2);
        SteamPower.place(helper, engine, Direction.WEST, 2);
        SteamEngineBlockEntity engineEntity = helper.getBlockEntity(engine);
        // The pump stands in the open: no water next to it, no water for the boiler.
        helper.setBlock(engine.west(2).west(), Blocks.AIR);

        helper.runAtTickTime(50, () -> {
            helper.assertValueEqual(engineEntity.status(), SteamEngineBlockEntity.NO_STEAM, "engine status without water");
            helper.assertValueEqual(engineEntity.energy().getEnergyStored(), 0, "no power without water");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void aBoilerFeedsAtMostTwoEngines(GameTestHelper helper) {
        BlockPos boiler = new BlockPos(3, 1, 3);
        helper.setBlock(boiler, ModBlocks.BOILER.get());
        helper.setBlock(boiler.east(), ModBlocks.OFFSHORE_PUMP.get());
        helper.setBlock(boiler.east(2), Blocks.WATER);
        BoilerBlockEntity boilerEntity = helper.getBlockEntity(boiler);
        boilerEntity.fuel().insertItem(0, new ItemStack(Items.COAL, 8), false);
        helper.runAtTickTime(30, () -> {
            // Steam for one tick: two engines get it, a third and a repeated request do not.
            long tick = helper.getLevel().getGameTime() + 1_000;
            helper.assertTrue(boilerEntity.tryServe(boiler.west(), tick), "first engine");
            helper.assertTrue(boilerEntity.tryServe(boiler.north(), tick), "second engine");
            helper.assertFalse(boilerEntity.tryServe(boiler.south(), tick), "a boiler feeds at most two engines");
            helper.succeed();
        });
    }
}
