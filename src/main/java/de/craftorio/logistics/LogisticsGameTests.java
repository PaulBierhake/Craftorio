package de.craftorio.logistics;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Underground belts, splitters, slopes and the inserter variants. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LogisticsGameTests {
    private static final String LARGE = "empty_large";

    private LogisticsGameTests() {
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void undergroundBeltCarriesItemsUnderAGap(GameTestHelper helper) {
        BlockPos entrance = new BlockPos(1, 1, 3);
        BlockPos exit = new BlockPos(6, 1, 3); // four blocks in between
        put(helper, entrance, ModBlocks.UNDERGROUND_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        put(helper, exit, ModBlocks.UNDERGROUND_BELT.get(), Direction.EAST, BeltSlope.FLAT, true);
        for (int x = 2; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
        }
        BlockPos chest = new BlockPos(7, 1, 3);
        helper.setBlock(chest, Blocks.CHEST);
        feed(helper, entrance, Direction.WEST, new ItemStack(Items.IRON_INGOT, 3));

        helper.succeedWhen(() -> helper.assertValueEqual(count(helper, chest, Items.IRON_INGOT), 3, "items through the tunnel"));
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void undergroundBeltDoesNotReachTooFar(GameTestHelper helper) {
        put(helper, new BlockPos(1, 1, 3), ModBlocks.UNDERGROUND_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        put(helper, new BlockPos(8, 1, 3), ModBlocks.UNDERGROUND_BELT.get(), Direction.EAST, BeltSlope.FLAT, true); // five blocks in between
        BlockPos chest = new BlockPos(9, 1, 3);
        helper.setBlock(chest, Blocks.CHEST);
        feed(helper, new BlockPos(1, 1, 3), Direction.WEST, new ItemStack(Items.IRON_INGOT, 2));

        helper.runAtTickTime(150, () -> {
            helper.assertValueEqual(count(helper, chest, Items.IRON_INGOT), 0, "items across a gap of five");
            helper.succeed();
        });
    }

    @GameTest(template = LARGE)
    public static void undergroundPiecesPairUpWhenPlaced(GameTestHelper helper) {
        BlockPos entrance = helper.absolutePos(new BlockPos(1, 1, 3));
        put(helper, new BlockPos(1, 1, 3), ModBlocks.UNDERGROUND_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        var level = helper.getLevel();
        helper.assertTrue(entrance.equals(ConveyorBeltBlock.findEntrance(level, entrance.east(4), Direction.EAST, BeltTier.BASIC)),
                "a piece four blocks after the entrance becomes its exit");
        helper.assertTrue(ConveyorBeltBlock.findEntrance(level, entrance.east(7), Direction.EAST, BeltTier.BASIC) == null,
                "too far away: it starts a new tunnel");
        put(helper, new BlockPos(4, 1, 3), ModBlocks.UNDERGROUND_BELT.get(), Direction.EAST, BeltSlope.FLAT, true);
        helper.assertTrue(ConveyorBeltBlock.findEntrance(level, entrance.east(5), Direction.EAST, BeltTier.BASIC) == null,
                "the entrance already has its exit");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void splitterSharesItemsBetweenItsOutputs(GameTestHelper helper) {
        BlockPos splitter = new BlockPos(3, 1, 3);
        put(helper, new BlockPos(2, 1, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        put(helper, splitter, ModBlocks.SPLITTER.get(), Direction.EAST, BeltSlope.FLAT, false);
        BlockPos front = new BlockPos(4, 1, 3);
        BlockPos left = new BlockPos(3, 1, 2);
        BlockPos right = new BlockPos(3, 1, 4);
        for (BlockPos chest : new BlockPos[]{front, left, right}) {
            helper.setBlock(chest, Blocks.CHEST);
        }
        feed(helper, new BlockPos(2, 1, 3), Direction.WEST, new ItemStack(Items.IRON_INGOT, 4));
        feed(helper, new BlockPos(2, 1, 3), Direction.WEST, new ItemStack(Items.IRON_INGOT, 4));

        helper.succeedWhen(() -> {
            int total = count(helper, front, Items.IRON_INGOT) + count(helper, left, Items.IRON_INGOT) + count(helper, right, Items.IRON_INGOT);
            helper.assertTrue(total >= 3, "items still on their way");
            helper.assertTrue(count(helper, front, Items.IRON_INGOT) > 0, "front got nothing");
            helper.assertTrue(count(helper, left, Items.IRON_INGOT) > 0, "left got nothing");
            helper.assertTrue(count(helper, right, Items.IRON_INGOT) > 0, "right got nothing");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 400)
    public static void splitterOnlyUsesConnectedOutputs(GameTestHelper helper) {
        put(helper, new BlockPos(2, 1, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        put(helper, new BlockPos(3, 1, 3), ModBlocks.SPLITTER.get(), Direction.EAST, BeltSlope.FLAT, false);
        BlockPos left = new BlockPos(3, 1, 2);
        BlockPos right = new BlockPos(3, 1, 4);
        helper.setBlock(left, Blocks.CHEST);
        helper.setBlock(right, Blocks.CHEST);
        feed(helper, new BlockPos(2, 1, 3), Direction.WEST, new ItemStack(Items.IRON_INGOT, 6));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(count(helper, left, Items.IRON_INGOT), 3, "left");
            helper.assertValueEqual(count(helper, right, Items.IRON_INGOT), 3, "right");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 400)
    public static void splitterFilterDirectionIsChosen(GameTestHelper helper) {
        put(helper, new BlockPos(2, 1, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        BlockPos splitter = new BlockPos(3, 1, 3);
        put(helper, splitter, ModBlocks.SPLITTER.get(), Direction.EAST, BeltSlope.FLAT, false);
        ConveyorBeltBlockEntity entity = helper.getBlockEntity(splitter);
        entity.setFilter(new ItemStack(Items.IRON_INGOT));
        entity.cycleFilterOutput(); // front -> left
        BlockPos front = new BlockPos(4, 1, 3);
        BlockPos left = new BlockPos(3, 1, 2);
        BlockPos right = new BlockPos(3, 1, 4);
        for (BlockPos chest : new BlockPos[]{front, left, right}) {
            helper.setBlock(chest, Blocks.CHEST);
        }
        feed(helper, new BlockPos(2, 1, 3), Direction.WEST, new ItemStack(Items.IRON_INGOT, 3));
        feed(helper, new BlockPos(2, 1, 3), Direction.WEST, new ItemStack(Items.COPPER_INGOT, 4));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(count(helper, left, Items.IRON_INGOT), 3, "filtered iron goes left");
            helper.assertValueEqual(count(helper, front, Items.COPPER_INGOT), 2, "copper alternates: front");
            helper.assertValueEqual(count(helper, right, Items.COPPER_INGOT), 2, "copper alternates: right");
            helper.assertValueEqual(count(helper, left, Items.COPPER_INGOT), 0, "no copper on the filter side");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 400)
    public static void splitterFilterSendsOnlyMatchesForward(GameTestHelper helper) {
        BlockPos splitter = new BlockPos(3, 1, 3);
        put(helper, new BlockPos(2, 1, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        put(helper, splitter, ModBlocks.SPLITTER.get(), Direction.EAST, BeltSlope.FLAT, false);
        ConveyorBeltBlockEntity entity = helper.getBlockEntity(splitter);
        entity.setFilter(new ItemStack(Items.IRON_INGOT));
        BlockPos front = new BlockPos(4, 1, 3);
        BlockPos left = new BlockPos(3, 1, 2);
        BlockPos right = new BlockPos(3, 1, 4);
        for (BlockPos chest : new BlockPos[]{front, left, right}) {
            helper.setBlock(chest, Blocks.CHEST);
        }
        feed(helper, new BlockPos(2, 1, 3), Direction.WEST, new ItemStack(Items.IRON_INGOT, 3));
        feed(helper, new BlockPos(2, 1, 3), Direction.WEST, new ItemStack(Items.COPPER_INGOT, 3));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(count(helper, front, Items.IRON_INGOT), 3, "iron goes straight on");
            helper.assertValueEqual(count(helper, left, Items.COPPER_INGOT) + count(helper, right, Items.COPPER_INGOT), 3, "copper goes to the sides");
            helper.assertValueEqual(count(helper, front, Items.COPPER_INGOT), 0, "no copper in front");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 600)
    public static void beltsClimbThreeBlocksAndComeDownAgain(GameTestHelper helper) {
        put(helper, new BlockPos(1, 1, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        put(helper, new BlockPos(2, 1, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.UP, false);
        put(helper, new BlockPos(3, 2, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.UP, false);
        put(helper, new BlockPos(4, 3, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.UP, false);
        put(helper, new BlockPos(5, 4, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        put(helper, new BlockPos(6, 4, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        put(helper, new BlockPos(7, 3, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.DOWN, false);
        put(helper, new BlockPos(8, 2, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.DOWN, false);
        put(helper, new BlockPos(9, 1, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.DOWN, false);
        BlockPos chest = new BlockPos(10, 1, 3);
        helper.setBlock(chest, Blocks.CHEST);
        feed(helper, new BlockPos(1, 1, 3), Direction.WEST, new ItemStack(Items.IRON_INGOT, 3));

        helper.succeedWhen(() -> helper.assertValueEqual(count(helper, chest, Items.IRON_INGOT), 3, "items over the hill"));
    }

    @GameTest(template = LARGE)
    public static void beltPlacedBesideAHigherBeltBecomesASlope(GameTestHelper helper) {
        put(helper, new BlockPos(3, 2, 3), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST, BeltSlope.FLAT, false);
        var state = ModBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST);
        helper.setBlock(new BlockPos(2, 1, 3), state);
        // The placement rule is a pure function of the neighbours.
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(3, 2, 3))).getBlock() instanceof ConveyorBeltBlock,
                "the higher belt stands");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void longInserterReachesTwoBlocks(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 2);
        BlockPos target = new BlockPos(5, 1, 2);
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(target, Blocks.CHEST);
        put(helper, new BlockPos(3, 1, 2), ModBlocks.LONG_INSERTER.get().defaultBlockState().setValue(InserterBlock.FACING, Direction.EAST));
        BlockPos near = new BlockPos(2, 1, 4);
        insert(helper, source, new ItemStack(Items.IRON_INGOT, 2));
        // A basic inserter next to a chest two blocks away does nothing.
        helper.setBlock(new BlockPos(2, 1, 5), Blocks.CHEST);
        helper.setBlock(near.south(), Blocks.CHEST);

        helper.succeedWhen(() -> helper.assertValueEqual(count(helper, target, Items.IRON_INGOT), 2, "long inserter moved the items"));
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void filterInserterOnlyMovesItsItem(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 2);
        BlockPos target = new BlockPos(3, 1, 2);
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(target, Blocks.CHEST);
        BlockPos inserter = new BlockPos(2, 1, 2);
        put(helper, inserter, ModBlocks.FILTER_INSERTER.get().defaultBlockState().setValue(InserterBlock.FACING, Direction.EAST));
        InserterBlockEntity entity = helper.getBlockEntity(inserter);
        entity.setFilter(new ItemStack(Items.COPPER_INGOT));
        insert(helper, source, new ItemStack(Items.IRON_INGOT, 2));
        insert(helper, source, new ItemStack(Items.COPPER_INGOT, 2));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(count(helper, target, Items.COPPER_INGOT), 2, "copper moved");
            helper.assertValueEqual(count(helper, target, Items.IRON_INGOT), 0, "iron stays");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void fastInserterIsFasterThanTheBasicOne(GameTestHelper helper) {
        BlockPos fastSource = new BlockPos(1, 1, 2);
        BlockPos fastTarget = new BlockPos(3, 1, 2);
        BlockPos slowSource = new BlockPos(1, 1, 6);
        BlockPos slowTarget = new BlockPos(3, 1, 6);
        for (BlockPos chest : new BlockPos[]{fastSource, fastTarget, slowSource, slowTarget}) {
            helper.setBlock(chest, Blocks.CHEST);
        }
        put(helper, new BlockPos(2, 1, 2), ModBlocks.FAST_INSERTER.get().defaultBlockState().setValue(InserterBlock.FACING, Direction.EAST));
        put(helper, new BlockPos(2, 1, 6), ModBlocks.INSERTER.get().defaultBlockState().setValue(InserterBlock.FACING, Direction.EAST));
        insert(helper, fastSource, new ItemStack(Items.IRON_INGOT, 30));
        insert(helper, slowSource, new ItemStack(Items.IRON_INGOT, 30));

        helper.runAtTickTime(90, () -> {
            int fast = count(helper, fastTarget, Items.IRON_INGOT);
            int slow = count(helper, slowTarget, Items.IRON_INGOT);
            helper.assertTrue(fast > slow + 2, "fast " + fast + " vs basic " + slow);
            helper.succeed();
        });
    }

    private static void put(GameTestHelper helper, BlockPos pos, Block block, Direction facing, BeltSlope slope, boolean exit) {
        helper.setBlock(pos, block.defaultBlockState().setValue(ConveyorBeltBlock.FACING, facing)
                .setValue(ConveyorBeltBlock.SLOPE, slope).setValue(ConveyorBeltBlock.EXIT, exit));
    }

    private static void put(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        helper.setBlock(pos, state);
    }

    /** Puts items onto a belt through its item handler, one per tick (a belt takes one item at a time). */
    private static void feed(GameTestHelper helper, BlockPos belt, Direction side, ItemStack stack) {
        int delay = 0;
        for (int i = 0; i < stack.getCount(); i++) {
            helper.runAfterDelay(delay += 8, () -> {
                IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(belt), side);
                if (handler != null) {
                    handler.insertItem(0, stack.copyWithCount(1), false);
                }
            });
        }
    }

    private static void insert(GameTestHelper helper, BlockPos chest, ItemStack stack) {
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(chest), null);
        net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(handler, stack, false);
        if (!(helper.getBlockEntity(chest) instanceof ChestBlockEntity)) {
            throw new IllegalStateException("no chest at " + chest);
        }
    }

    private static int count(GameTestHelper helper, BlockPos chest, Item item) {
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(chest), null);
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
