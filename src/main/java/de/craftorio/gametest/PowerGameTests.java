package de.craftorio.gametest;

import de.craftorio.Craftorio;
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
import net.neoforged.neoforge.items.IItemHandler;

/** Generator → poles → machines. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PowerGameTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private PowerGameTests() {
    }

    /** Steam engine and machine next to one pole; returns the machine position. */
    private static BlockPos poweredMachine(GameTestHelper helper, Block machine) {
        SteamPower.place(helper, new BlockPos(1, 1, 1), Direction.WEST, 4);
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.POWER_POLE.get());
        BlockPos machinePos = new BlockPos(3, 1, 1);
        helper.setBlock(machinePos, machine);
        return machinePos;
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void electricFurnaceSmeltsOnGridPower(GameTestHelper helper) {
        BlockPos furnace = poweredMachine(helper, ModBlocks.ELECTRIC_FURNACE.get());
        items(helper, furnace).insertItem(0, new ItemStack(Items.RAW_IRON, 2), false);

        helper.succeedWhen(() -> helper.assertValueEqual(output(helper, furnace, Items.IRON_INGOT), 2, "smelted ingots"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stoneFurnaceBurnsFuelAndSmeltsAtFactorioSpeed(GameTestHelper helper) {
        BlockPos furnace = new BlockPos(2, 1, 2);
        helper.setBlock(furnace, ModBlocks.STONE_FURNACE.get());
        IItemHandler handler = items(helper, furnace);
        helper.assertTrue(!handler.insertItem(0, new ItemStack(Items.DIRT), true).isEmpty(), "furnace must refuse dirt");
        handler.insertItem(0, new ItemStack(Items.RAW_IRON, 2), false);
        ProcessingMachineBlockEntity machine = helper.getBlockEntity(furnace);
        helper.assertTrue(machine.items().insertItem(machine.type().fuelSlot(), new ItemStack(Items.COAL), false).isEmpty(), "coal goes into the fuel slot");

        // 3.2 s per plate: one after 64 ticks, both after 128.
        helper.runAtTickTime(50, () -> helper.assertValueEqual(output(helper, furnace, Items.IRON_INGOT), 0, "no plate before 3.2 s"));
        helper.runAtTickTime(75, () -> helper.assertValueEqual(output(helper, furnace, Items.IRON_INGOT), 1, "first plate"));
        helper.runAtTickTime(140, () -> {
            helper.assertValueEqual(output(helper, furnace, Items.IRON_INGOT), 2, "second plate");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void furnaceMakesStoneBricksFromTwoStones(GameTestHelper helper) {
        BlockPos furnace = new BlockPos(2, 1, 2);
        helper.setBlock(furnace, ModBlocks.STONE_FURNACE.get());
        ProcessingMachineBlockEntity machine = helper.getBlockEntity(furnace);
        machine.items().insertItem(machine.type().fuelSlot(), new ItemStack(Items.COAL), false);
        items(helper, furnace).insertItem(0, new ItemStack(Items.COBBLESTONE, 5), false);

        helper.succeedWhen(() -> helper.assertValueEqual(output(helper, furnace, ModItems.STONE_BRICK.get()), 2, "two bricks from four stones"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void assemblerCraftsSelectedRecipe(GameTestHelper helper) {
        BlockPos assembler = poweredMachine(helper, ModBlocks.ASSEMBLER.get());
        ProcessingMachineBlockEntity machine = helper.getBlockEntity(assembler);
        IItemHandler handler = items(helper, assembler);

        helper.assertTrue(!handler.insertItem(0, new ItemStack(Items.IRON_INGOT), true).isEmpty(), "no recipe selected yet");
        machine.setSelectedRecipe(Craftorio.id("assembling/iron_gear"));
        helper.assertTrue(!handler.insertItem(0, new ItemStack(ModItems.COPPER_CABLE.get()), true).isEmpty(), "cable is no gear ingredient");
        handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 4), false);

        helper.succeedWhen(() -> helper.assertValueEqual(output(helper, assembler, ModItems.IRON_GEAR.get()), 2, "gears"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void machineWithoutPowerDoesNothing(GameTestHelper helper) {
        BlockPos furnace = new BlockPos(2, 1, 2);
        helper.setBlock(furnace, ModBlocks.ELECTRIC_FURNACE.get());
        items(helper, furnace).insertItem(0, new ItemStack(Items.RAW_IRON), false);

        helper.runAtTickTime(80, () -> {
            helper.assertValueEqual(output(helper, furnace, Items.IRON_INGOT), 0, "plates without power");
            helper.succeed();
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void polesChainPowerAcrossTheirRange(GameTestHelper helper) {
        BlockPos machine = distantMachine(helper);
        helper.setBlock(new BlockPos(8, 1, 8), ModBlocks.POWER_POLE.get()); // bridges the 14-block gap

        helper.succeedWhen(() -> helper.assertTrue(energy(helper, machine) > 0, "machine got no energy through the pole chain"));
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void polesOutOfRangeStayApart(GameTestHelper helper) {
        BlockPos machine = distantMachine(helper);

        helper.runAtTickTime(60, () -> {
            helper.assertValueEqual(energy(helper, machine), 0, "energy across a 14-block gap");
            helper.succeed();
        });
    }

    /** Steam engine with a pole at one end, pole and furnace 14 blocks away at the other end. */
    private static BlockPos distantMachine(GameTestHelper helper) {
        SteamPower.place(helper, new BlockPos(0, 1, 8), Direction.NORTH, 2);
        helper.setBlock(new BlockPos(1, 1, 8), ModBlocks.POWER_POLE.get());
        helper.setBlock(new BlockPos(15, 1, 8), ModBlocks.POWER_POLE.get());
        BlockPos machine = new BlockPos(15, 1, 9);
        helper.setBlock(machine, ModBlocks.ELECTRIC_FURNACE.get());
        return machine;
    }

    private static IItemHandler items(GameTestHelper helper, BlockPos pos) {
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        if (handler == null) {
            throw new IllegalStateException("no item handler at " + pos);
        }
        return handler;
    }

    private static int energy(GameTestHelper helper, BlockPos pos) {
        var storage = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), null);
        return storage == null ? 0 : storage.getEnergyStored();
    }

    private static int output(GameTestHelper helper, BlockPos pos, Item item) {
        ProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        ItemStack output = machine.items().getStackInSlot(machine.type().outputSlot());
        return output.is(item) ? output.getCount() : 0;
    }
}
