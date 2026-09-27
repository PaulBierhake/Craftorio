package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.logistics.ConveyorBeltBlock;
import de.craftorio.logistics.InserterBlock;
import de.craftorio.energy.GeneratorBlockEntity;
import de.craftorio.energy.GeneratorType;
import de.craftorio.machine.DrillBlock;
import de.craftorio.machine.DrillTier;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Drill → belt → inserter → chest/trading post chains. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AutomationGameTests {
    private static final String EMPTY = "empty";

    private AutomationGameTests() {
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void drillMinesFieldIntoChest(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), ModBlocks.IRON_ORE_FIELD.get());
            }
        }
        BlockPos drill = new BlockPos(2, 2, 2);
        helper.setBlock(drill, ModBlocks.BURNER_DRILL.get().defaultBlockState().setValue(DrillBlock.FACING, Direction.EAST));
        BlockPos chest = drill.east();
        helper.setBlock(chest, Blocks.CHEST);
        handler(helper, drill, Direction.UP).insertItem(0, new ItemStack(Items.COAL, 1), false);

        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, chest, Items.RAW_IRON) >= 2, "drill output did not reach the chest");
            helper.assertBlockPresent(ModBlocks.IRON_ORE_FIELD.get(), new BlockPos(2, 1, 2));
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void drillWithoutFuelProducesNothing(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), ModBlocks.IRON_ORE_FIELD.get());
            }
        }
        BlockPos drill = new BlockPos(2, 2, 2);
        helper.setBlock(drill, ModBlocks.BURNER_DRILL.get().defaultBlockState().setValue(DrillBlock.FACING, Direction.EAST));
        BlockPos chest = drill.east();
        helper.setBlock(chest, Blocks.CHEST);

        helper.runAtTickTime(150, () -> {
            helper.assertValueEqual(count(helper, chest, Items.RAW_IRON), 0, "output without fuel");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void electricDrillRunsOnGridPower(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), ModBlocks.TITANIUM_ORE_FIELD.get());
            }
        }
        BlockPos drill = new BlockPos(2, 2, 2);
        helper.setBlock(drill, ModBlocks.ELECTRIC_DRILL.get().defaultBlockState().setValue(DrillBlock.FACING, Direction.EAST));
        BlockPos chest = drill.east();
        helper.setBlock(chest, Blocks.CHEST);
        helper.assertTrue(handler(helper, drill, Direction.UP).insertItem(0, new ItemStack(Items.COAL), true).getCount() == 1,
                "electric drills take no fuel");
        helper.setBlock(new BlockPos(0, 2, 4), ModBlocks.COAL_GENERATOR.get());
        helper.setBlock(new BlockPos(1, 2, 3), ModBlocks.POWER_POLE.get());
        handler(helper, new BlockPos(0, 2, 4), Direction.UP).insertItem(0, new ItemStack(Items.COAL, 8), false);

        helper.succeedWhen(() -> helper.assertTrue(count(helper, chest, ModItems.RAW_TITANIUM.get()) >= 3,
                "electric drill output did not reach the chest"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void reactorBurnsFuelRodsOnly(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.REACTOR.get());
        IItemHandler fuel = handler(helper, pos, Direction.UP);
        helper.assertTrue(fuel.insertItem(0, new ItemStack(Items.COAL), false).getCount() == 1, "reactor rejects coal");
        fuel.insertItem(0, new ItemStack(ModItems.FUEL_ROD.get()), false);
        GeneratorBlockEntity reactor = helper.getBlockEntity(pos);

        helper.runAtTickTime(50, () -> {
            helper.assertTrue(reactor.energy().getEnergyStored() >= 40 * GeneratorType.REACTOR.fePerTick(), "reactor output");
            helper.assertValueEqual(reactor.energy().getMaxEnergyStored(), GeneratorType.REACTOR.capacity(), "reactor buffer");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void drillsCannotShareFieldBlocks(GameTestHelper helper) {
        BlockPos drill = new BlockPos(1, 2, 1);
        helper.setBlock(drill, ModBlocks.BURNER_DRILL.get());

        helper.assertTrue(DrillBlock.findOverlappingDrill(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 1)), DrillTier.BURNER) != null,
                "3x3 areas two blocks apart overlap");
        helper.assertTrue(DrillBlock.findOverlappingDrill(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 1)), DrillTier.BURNER) == null,
                "3x3 areas three blocks apart are fine");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void beltCarriesBothLanesIntoTradingPost(GameTestHelper helper) {
        Team team = TradingPostGameTests.newTeam(helper);
        for (int x = 0; x <= 2; x++) {
            belt(helper, new BlockPos(x, 1, 1), Direction.EAST);
        }
        TradingPostGameTests.placeTradingPost(helper, new BlockPos(3, 1, 1), team);
        IItemHandler start = handler(helper, new BlockPos(0, 1, 1), Direction.WEST);
        start.insertItem(0, new ItemStack(Items.IRON_INGOT), false);
        start.insertItem(0, new ItemStack(Items.IRON_INGOT), false);

        helper.succeedWhen(() -> helper.assertValueEqual(team.balance(), 32L, "balance after two ingots"));
    }

    @GameTest(template = EMPTY)
    public static void beltFollowsCurveIntoChest(GameTestHelper helper) {
        belt(helper, new BlockPos(1, 1, 3), Direction.EAST);
        belt(helper, new BlockPos(2, 1, 3), Direction.NORTH);
        belt(helper, new BlockPos(2, 1, 2), Direction.NORTH);
        belt(helper, new BlockPos(2, 1, 1), Direction.NORTH);
        BlockPos chest = new BlockPos(2, 1, 0);
        helper.setBlock(chest, Blocks.CHEST);
        handler(helper, new BlockPos(1, 1, 3), Direction.WEST).insertItem(0, new ItemStack(Items.RAW_COPPER), false);

        helper.succeedWhen(() -> helper.assertValueEqual(count(helper, chest, Items.RAW_COPPER), 1, "copper in chest"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void inserterMovesItemsBetweenChests(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 1);
        BlockPos target = new BlockPos(3, 1, 1);
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.INSERTER.get().defaultBlockState().setValue(InserterBlock.FACING, Direction.EAST));
        helper.setBlock(target, Blocks.CHEST);
        ((Container) helper.getBlockEntity(source)).setItem(0, new ItemStack(Items.RAW_IRON, 3));

        helper.succeedWhen(() -> helper.assertValueEqual(count(helper, target, Items.RAW_IRON), 3, "items moved by inserter"));
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void inserterRefuelsDrillButNeverTakesFuelOut(GameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 2, 1);
        BlockPos drill = new BlockPos(3, 2, 1);
        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(new BlockPos(2, 2, 1), ModBlocks.INSERTER.get().defaultBlockState().setValue(InserterBlock.FACING, Direction.EAST));
        helper.setBlock(drill, ModBlocks.BURNER_DRILL.get());
        ((Container) helper.getBlockEntity(chest)).setItem(0, new ItemStack(Items.COAL, 2));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(handler(helper, drill, Direction.UP).getStackInSlot(0).getCount(), 2, "coal in drill fuel slot");
            helper.assertTrue(handler(helper, drill, Direction.UP).extractItem(0, 1, true).isEmpty(), "fuel must not be extractable");
        });
    }

    private static void belt(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, ModBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, facing));
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos pos, Direction side) {
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side);
        if (handler == null) {
            throw new IllegalStateException("no item handler at " + pos);
        }
        return handler;
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        return ((Container) helper.getBlockEntity(pos)).countItem(item);
    }
}
