package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.machine.ProcessingMachineBlockEntity;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import net.minecraft.core.BlockPos;
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

    /** Generator and machine next to one pole; returns the machine position. */
    private static BlockPos poweredMachine(GameTestHelper helper, Block machine) {
        BlockPos generator = new BlockPos(1, 1, 1);
        helper.setBlock(generator, ModBlocks.COAL_GENERATOR.get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.POWER_POLE.get());
        BlockPos machinePos = new BlockPos(3, 1, 1);
        helper.setBlock(machinePos, machine);
        items(helper, generator).insertItem(0, new ItemStack(Items.COAL, 4), false);
        return machinePos;
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void electricFurnaceSmeltsOnGridPower(GameTestHelper helper) {
        BlockPos furnace = poweredMachine(helper, ModBlocks.ELECTRIC_FURNACE.get());
        items(helper, furnace).insertItem(0, new ItemStack(Items.RAW_IRON, 2), false);

        helper.succeedWhen(() -> helper.assertValueEqual(output(helper, furnace, Items.IRON_INGOT), 2, "smelted ingots"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pressMakesPlatesAndCables(GameTestHelper helper) {
        BlockPos press = poweredMachine(helper, ModBlocks.PRESS.get());
        items(helper, press).insertItem(0, new ItemStack(Items.COPPER_INGOT, 1), false);

        helper.assertTrue(!items(helper, press).insertItem(0, new ItemStack(Items.DIRT), true).isEmpty(), "press must refuse dirt");
        helper.succeedWhen(() -> helper.assertValueEqual(output(helper, press, ModItems.COPPER_CABLE.get()), 2, "cables from one ingot"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void assemblerCraftsSelectedRecipe(GameTestHelper helper) {
        BlockPos assembler = poweredMachine(helper, ModBlocks.ASSEMBLER.get());
        ProcessingMachineBlockEntity machine = helper.getBlockEntity(assembler);
        IItemHandler handler = items(helper, assembler);

        helper.assertTrue(!handler.insertItem(0, new ItemStack(ModItems.IRON_PLATE.get()), true).isEmpty(), "no recipe selected yet");
        machine.setSelectedRecipe(Craftorio.id("assembling/iron_gear"));
        helper.assertTrue(!handler.insertItem(0, new ItemStack(ModItems.COPPER_CABLE.get()), true).isEmpty(), "cable is no gear ingredient");
        handler.insertItem(0, new ItemStack(ModItems.IRON_PLATE.get(), 4), false);

        helper.succeedWhen(() -> helper.assertValueEqual(output(helper, assembler, ModItems.IRON_GEAR.get()), 2, "gears"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void machineWithoutPowerDoesNothing(GameTestHelper helper) {
        BlockPos press = new BlockPos(2, 1, 2);
        helper.setBlock(press, ModBlocks.PRESS.get());
        items(helper, press).insertItem(0, new ItemStack(Items.IRON_INGOT), false);

        helper.runAtTickTime(80, () -> {
            helper.assertValueEqual(output(helper, press, ModItems.IRON_PLATE.get()), 0, "plates without power");
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

    /** Generator with a pole at one end, pole and press 14 blocks away at the other end. */
    private static BlockPos distantMachine(GameTestHelper helper) {
        BlockPos generator = new BlockPos(0, 1, 8);
        helper.setBlock(generator, ModBlocks.COAL_GENERATOR.get());
        items(helper, generator).insertItem(0, new ItemStack(Items.COAL, 2), false);
        helper.setBlock(new BlockPos(1, 1, 8), ModBlocks.POWER_POLE.get());
        helper.setBlock(new BlockPos(15, 1, 8), ModBlocks.POWER_POLE.get());
        BlockPos machine = new BlockPos(15, 1, 9);
        helper.setBlock(machine, ModBlocks.PRESS.get());
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
