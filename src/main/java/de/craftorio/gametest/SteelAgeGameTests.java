package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.energy.SolarPanelBlockEntity;
import de.craftorio.machine.ProcessingMachineBlockEntity;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Steel, medium power poles, solar panels and the second assembler (package U5). */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteelAgeGameTests {
    private static final String LARGE = "empty_large";

    private SteelAgeGameTests() {
    }

    /** Steam engine at (1,1,1), a pole of the given kind at (2,1,2) and an electric furnace {@code distance} blocks east of the pole. */
    private static BlockPos furnaceBehindPole(GameTestHelper helper, Block pole, int distance) {
        SteamPower.place(helper, new BlockPos(1, 1, 1), Direction.WEST, 4);
        helper.setBlock(new BlockPos(2, 1, 2), pole);
        BlockPos furnace = new BlockPos(2 + distance, 1, 2);
        helper.setBlock(furnace, ModBlocks.ELECTRIC_FURNACE.get());
        return furnace;
    }

    private static int energy(GameTestHelper helper, BlockPos pos) {
        var storage = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), null);
        return storage == null ? 0 : storage.getEnergyStored();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void mediumPoleSuppliesSevenBySeven(GameTestHelper helper) {
        BlockPos furnace = furnaceBehindPole(helper, ModBlocks.MEDIUM_POWER_POLE.get(), 3);
        helper.succeedWhen(() -> helper.assertTrue(energy(helper, furnace) > 0, "a machine 3 blocks from a medium pole gets no energy"));
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void smallPoleSuppliesOnlyFiveByFive(GameTestHelper helper) {
        BlockPos furnace = furnaceBehindPole(helper, ModBlocks.POWER_POLE.get(), 3);
        helper.runAtTickTime(60, () -> {
            helper.assertValueEqual(energy(helper, furnace), 0, "energy 3 blocks from a small pole");
            helper.succeed();
        });
    }

    /** Engine and medium pole at one end; a second medium pole {@code gap} blocks away with a furnace next to it. */
    private static BlockPos furnaceBehindSecondMediumPole(GameTestHelper helper, int gap) {
        SteamPower.place(helper, new BlockPos(1, 1, 1), Direction.WEST, 4);
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.MEDIUM_POWER_POLE.get());
        helper.setBlock(new BlockPos(2 + gap, 1, 2), ModBlocks.MEDIUM_POWER_POLE.get());
        BlockPos furnace = new BlockPos(2 + gap, 1, 3);
        helper.setBlock(furnace, ModBlocks.ELECTRIC_FURNACE.get());
        return furnace;
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void mediumPolesWireNineBlocks(GameTestHelper helper) {
        BlockPos furnace = furnaceBehindSecondMediumPole(helper, 9);
        helper.succeedWhen(() -> helper.assertTrue(energy(helper, furnace) > 0, "medium poles 9 blocks apart are not wired"));
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void mediumPolesTenBlocksApartStayApart(GameTestHelper helper) {
        BlockPos furnace = furnaceBehindSecondMediumPole(helper, 10);
        helper.runAtTickTime(60, () -> {
            helper.assertValueEqual(energy(helper, furnace), 0, "energy across a 10-block gap");
            helper.succeed();
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 500)
    public static void steelFurnaceMakesSteelFromFiveIronPlatesTwiceAsFastAsAStoneFurnace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.STEEL_FURNACE.get());
        ProcessingMachineBlockEntity furnace = helper.getBlockEntity(pos);
        furnace.items().insertItem(furnace.type().fuelSlot(), new ItemStack(Items.COAL), false);
        furnace.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 5), false);

        // 16 s of a stone furnace, 8 s = 160 ticks here.
        helper.runAtTickTime(120, () -> helper.assertValueEqual(steel(furnace), 0, "no steel after 6 s"));
        helper.runAtTickTime(200, () -> {
            helper.assertValueEqual(steel(furnace), 1, "steel after 8 s");
            helper.succeed();
        });
    }

    private static int steel(ProcessingMachineBlockEntity machine) {
        ItemStack output = machine.items().getStackInSlot(machine.type().outputSlot());
        return output.is((Item) ModItems.STEEL_PLATE.get()) ? output.getCount() : 0;
    }

    // The panels read the time of the shared level, so the day and the night test run in batches of their own.
    @GameTest(template = LARGE, timeoutTicks = 100, batch = "solar_day")
    public static void solarPanelProducesByDayOnly(GameTestHelper helper) {
        BlockPos day = new BlockPos(2, 1, 2);
        helper.setBlock(day, ModBlocks.SOLAR_PANEL.get());
        helper.getLevel().setDayTime(6_000);
        helper.succeedWhen(() -> {
            SolarPanelBlockEntity panel = helper.getBlockEntity(day);
            helper.assertTrue(panel.energy().getEnergyStored() > 0, "solar panel makes no power at noon (sky " + panel.seesSky() + ")");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100, batch = "solar_night")
    public static void solarPanelStopsAtNight(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.SOLAR_PANEL.get());
        helper.getLevel().setDayTime(18_000);
        helper.runAtTickTime(20, () -> {
            SolarPanelBlockEntity panel = helper.getBlockEntity(pos);
            helper.getLevel().setDayTime(6_000);
            helper.assertValueEqual(panel.energy().getEnergyStored(), 0, "solar power at midnight");
            helper.succeed();
        });
    }
}
