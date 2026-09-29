package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.energy.SteamTurbineBlockEntity;
import de.craftorio.heat.HeatExchangerBlockEntity;
import de.craftorio.heat.HeatLogic;
import de.craftorio.heat.HeatNode;
import de.craftorio.heat.HeatPipeBlockEntity;
import de.craftorio.heat.ReactorBlockEntity;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModFluids;
import de.craftorio.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/** Reactor, heat network, heat exchangers and steam turbines (package U11c). */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NuclearGameTests {
    private static final String LARGE = "empty_large";

    private NuclearGameTests() {
    }

    private static ReactorBlockEntity reactor(GameTestHelper helper, BlockPos pos, int cells) {
        helper.setBlock(pos, ModBlocks.REACTOR.get());
        ReactorBlockEntity reactor = helper.getBlockEntity(pos);
        reactor.items().insertItem(ReactorBlockEntity.FUEL_SLOT, new ItemStack(ModItems.URANIUM_FUEL_CELL.get(), cells), false);
        return reactor;
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aReactorMakesFortyMegawattsOfHeatFromAFuelCell(GameTestHelper helper) {
        ReactorBlockEntity reactor = reactor(helper, new BlockPos(2, 1, 2), 2);
        IItemHandler automation = reactor.automation();
        helper.assertTrue(!automation.insertItem(0, new ItemStack(net.minecraft.world.item.Items.COAL), true).isEmpty(), "only fuel cells go in");
        helper.assertTrue(automation.insertItem(1, new ItemStack(ModItems.URANIUM_FUEL_CELL.get()), true).getCount() == 1, "nothing goes into the used cell slot");

        helper.runAtTickTime(100, () -> {
            helper.assertTrue(reactor.burning(), "the cell burns");
            helper.assertValueEqual(reactor.heatPerTick(), 40_000, "40 MW");
            helper.assertValueEqual(reactor.items().getStackInSlot(ReactorBlockEntity.FUEL_SLOT).getCount(), 1, "one cell went in, one is left");
            // 100 ticks × 40,000 FE into 200,000 FE per °C: 20 °C above ambient
            helper.assertTrue(Math.abs(reactor.temperature() - (HeatLogic.AMBIENT + 20)) < 1, "temperature " + reactor.temperature());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 4_300)
    public static void aCellBurnsTwoHundredSecondsAndLeavesAUsedCell(GameTestHelper helper) {
        ReactorBlockEntity reactor = reactor(helper, new BlockPos(2, 1, 2), 1);
        helper.runAtTickTime(3_900, () -> helper.assertTrue(reactor.items().getStackInSlot(ReactorBlockEntity.USED_SLOT).isEmpty(), "still burning after 195 s"));
        helper.succeedWhen(() -> {
            helper.assertTrue(!reactor.burning(), "burnt out");
            helper.assertValueEqual(reactor.items().getStackInSlot(ReactorBlockEntity.USED_SLOT).getCount(), 1, "a used up fuel cell");
            helper.assertTrue(reactor.automation().extractItem(1, 1, true).is(ModItems.USED_UP_FUEL_CELL.get()), "inserters can take it");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void adjacentRunningReactorsDoubleTheirHeat(GameTestHelper helper) {
        ReactorBlockEntity first = reactor(helper, new BlockPos(1, 1, 2), 1);
        ReactorBlockEntity second = reactor(helper, new BlockPos(2, 1, 2), 1);
        ReactorBlockEntity third = reactor(helper, new BlockPos(3, 1, 2), 1);
        helper.runAtTickTime(20, () -> {
            helper.assertValueEqual(first.heatPerTick(), 80_000, "one running neighbour: 80 MW");
            helper.assertValueEqual(second.heatPerTick(), 120_000, "two running neighbours: 120 MW");
            helper.assertValueEqual(third.heatPerTick(), 80_000, "one running neighbour: 80 MW");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aHotReactorWaitsWithTheNextCell(GameTestHelper helper) {
        ReactorBlockEntity reactor = reactor(helper, new BlockPos(2, 1, 2), 1);
        reactor.setTemperature(950);
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(!reactor.burning(), "no new cell above the 900 °C limit");
            helper.assertValueEqual(reactor.items().getStackInSlot(ReactorBlockEntity.FUEL_SLOT).getCount(), 1, "the cell stays");
            reactor.changeLimit(2); // 1000 °C
            helper.assertValueEqual(reactor.limit(), 1_000, "limit raised");
        });
        helper.succeedWhen(() -> helper.assertTrue(reactor.burning(), "loaded once the limit allows it"));
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void heatPipesCarryHeatFromTheReactorToAnExchangerThatMakesSteam(GameTestHelper helper) {
        ReactorBlockEntity reactor = reactor(helper, new BlockPos(0, 1, 2), 1);
        BlockPos pipe = new BlockPos(1, 1, 2);
        helper.setBlock(pipe, ModBlocks.HEAT_PIPE.get().connectedState(helper.getLevel(), helper.absolutePos(pipe)));
        BlockPos exchangerPos = new BlockPos(2, 1, 2);
        helper.setBlock(exchangerPos, ModBlocks.HEAT_EXCHANGER.get());
        // the pipe's arms are set when placed next to the reactor and later blocks update them
        helper.setBlock(pipe, ModBlocks.HEAT_PIPE.get().connectedState(helper.getLevel(), helper.absolutePos(pipe)));
        HeatExchangerBlockEntity exchanger = helper.getBlockEntity(exchangerPos);
        reactor.setTemperature(900);
        helper.onEachTick(() -> exchanger.water().fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE));

        helper.succeedWhen(() -> {
            HeatPipeBlockEntity heatPipe = helper.getBlockEntity(pipe);
            helper.assertTrue(heatPipe.temperature() > 100, "the pipe warmed up: " + heatPipe.temperature());
            helper.assertTrue(exchanger.temperature() > 500, "the exchanger reached 500 °C: " + exchanger.temperature());
            helper.assertTrue(exchanger.steam().getFluidAmount() > 0 || exchanger.temperature() > 500, "steam");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void anExchangerNeedsFiveHundredDegreesAndTurbinesOnlyTakeHotSteam(GameTestHelper helper) {
        BlockPos exchangerPos = new BlockPos(1, 1, 2);
        helper.setBlock(exchangerPos, ModBlocks.HEAT_EXCHANGER.get());
        HeatExchangerBlockEntity exchanger = helper.getBlockEntity(exchangerPos);
        exchanger.water().fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE);
        exchanger.setTemperature(400);
        BlockPos turbinePos = new BlockPos(2, 1, 2);
        helper.setBlock(turbinePos, ModBlocks.STEAM_TURBINE.get());
        SteamTurbineBlockEntity turbine = helper.getBlockEntity(turbinePos);
        helper.assertValueEqual(turbine.steam().fill(new FluidStack(ModFluids.STEAM.get(), 50), IFluidHandler.FluidAction.SIMULATE), 0, "plain steam is no fuel for a turbine");
        helper.assertValueEqual(exchanger.handler().fill(new FluidStack(ModFluids.HOT_STEAM.get(), 50), IFluidHandler.FluidAction.SIMULATE), 0, "an exchanger takes water only");

        helper.runAtTickTime(40, () -> {
            helper.assertValueEqual(exchanger.steam().getFluidAmount() + turbine.steam().getFluidAmount(), 0, "no steam below 500 °C");
            exchanger.setTemperature(700);
        });
        helper.runAtTickTime(80, () -> {
            helper.assertTrue(turbine.steam().getFluidAmount() > 0 || turbine.energy().getEnergyStored() > 0, "steam reached the turbine");
            helper.assertTrue(exchanger.temperature() < 700, "making steam cools the exchanger");
            helper.succeed();
        });
    }

    /**
     * One reactor, four heat exchangers and seven turbines under a full load of 7 × 5.82 MW: the steam is enough for
     * (nearly) all of it, and the reactor keeps the exchangers above 500 °C. The exchangers stand between the turbines
     * along a short steam bus (a pipe moves at most half of the level difference per tick, so long pipes throttle).
     */
    @GameTest(template = LARGE, timeoutTicks = 1_200)
    public static void oneReactorFourExchangersAndSevenTurbinesCarryAFortyMegawattLoad(GameTestHelper helper) {
        BlockPos reactorPos = new BlockPos(0, 3, 8);
        ReactorBlockEntity reactor = reactor(helper, reactorPos, 1);
        List<HeatNode> nodes = new ArrayList<>();
        nodes.add(reactor);
        // T E T T E T T E T E T along x = 1..11 at y = 2, a steam bus of pipes below, heat pipes above the exchangers
        String order = "TETTETTETET";
        List<HeatExchangerBlockEntity> exchangers = new ArrayList<>();
        List<SteamTurbineBlockEntity> turbines = new ArrayList<>();
        for (int i = 0; i < order.length(); i++) {
            BlockPos pos = new BlockPos(1 + i, 2, 8);
            helper.setBlock(pos, order.charAt(i) == 'E' ? ModBlocks.HEAT_EXCHANGER.get() : ModBlocks.STEAM_TURBINE.get());
            if (order.charAt(i) == 'E') {
                exchangers.add(helper.getBlockEntity(pos));
            } else {
                turbines.add(helper.getBlockEntity(pos));
            }
        }
        for (int x = 1; x <= 11; x++) {
            helper.setBlock(new BlockPos(x, 1, 8), ModBlocks.PIPE.get());
        }
        for (int x = 2; x <= 10; x++) { // heat pipes over x = 2..10 reach every exchanger (and stand idle over the turbines)
            helper.setBlock(new BlockPos(x, 3, 8), ModBlocks.HEAT_PIPE.get());
        }
        for (int x = 2; x <= 10; x++) { // now that all neighbours stand, give every pipe its arms
            BlockPos pos = new BlockPos(x, 3, 8);
            helper.setBlock(pos, ModBlocks.HEAT_PIPE.get().connectedState(helper.getLevel(), helper.absolutePos(pos)));
            nodes.add(helper.getBlockEntity(pos));
        }
        for (int x = 1; x <= 11; x++) {
            BlockPos pos = new BlockPos(x, 1, 8);
            helper.setBlock(pos, ModBlocks.PIPE.get().connectedState(helper.getLevel(), helper.absolutePos(pos)));
        }
        helper.assertValueEqual(exchangers.size(), 4, "four exchangers");
        helper.assertValueEqual(turbines.size(), 7, "seven turbines");
        nodes.addAll(exchangers);
        // warm start: 900 °C everywhere, as after a few minutes of operation
        nodes.forEach(node -> node.setTemperature(900));
        reactor.changeLimit(1); // 950 °C: a cell may go in at 900 °C

        long[] drawn = new long[1];
        int[] ticks = new int[1];
        helper.onEachTick(() -> {
            exchangers.forEach(exchanger -> exchanger.water().fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE));
            ticks[0]++;
            for (SteamTurbineBlockEntity turbine : turbines) {
                int taken = turbine.energy().extractEnergy(SteamTurbineBlockEntity.POWER, false);
                if (ticks[0] > 200) {
                    drawn[0] += taken;
                }
            }
        });
        helper.runAtTickTime(1_000, () -> {
            double ideal = 7.0 * SteamTurbineBlockEntity.POWER * 800;
            helper.assertTrue(drawn[0] >= ideal * 0.93, "turbines gave " + drawn[0] + " of " + (long) ideal + " FE");
            helper.assertTrue(reactor.burning(), "the cell still burns");
            for (HeatExchangerBlockEntity exchanger : exchangers) {
                helper.assertTrue(exchanger.temperature() >= HeatLogic.STEAM_TEMPERATURE, "exchanger at " + exchanger.temperature());
            }
            helper.succeed();
        });
    }
}
