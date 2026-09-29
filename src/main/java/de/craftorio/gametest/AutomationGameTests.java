package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.logistics.ConveyorBeltBlock;
import de.craftorio.logistics.InserterBlock;
import de.craftorio.machine.DrillBlock;
import de.craftorio.machine.DrillTier;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import de.craftorio.machine.DrillBlockEntity;
import de.craftorio.menu.DrillMenu;
import de.craftorio.quest.Quest;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import de.craftorio.registry.ModFluids;
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

    @GameTest(template = EMPTY)
    public static void drillMenuTakesFuelAndGivesOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.BURNER_DRILL.get());
        DrillBlockEntity drill = helper.getBlockEntity(pos);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        DrillMenu menu = (DrillMenu) drill.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(Items.COAL)), "coal goes into the fuel slot");
        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "only fuel");
        helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.COAL)), "output slot is take-only");
        menu.getSlot(0).set(new ItemStack(Items.COAL, 5));
        helper.assertTrue(menu.getSlot(0).mayPickup(player), "players may take fuel back");
        helper.assertTrue(drill.handler().extractItem(DrillBlockEntity.FUEL_SLOT, 1, true).isEmpty(), "automation may not");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void handMiningOreFieldCountsForGuide(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.IRON_ORE_FIELD.get());
        // Mock players always count as creative; a fake player is a plain survival player.
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STARTER_PICKAXE.get()));
        Team team = TeamData.registry(player.server).ensureTeam(player.getUUID(), player.getGameProfile().getName());
        long before = team.built().getOrDefault(Quest.MINED_PREFIX + "minecraft:raw_iron", 0L);
        player.gameMode.destroyBlock(helper.absolutePos(pos));
        helper.assertBlockPresent(ModBlocks.IRON_ORE_FIELD.get(), pos);
        long mined = team.built().getOrDefault(Quest.MINED_PREFIX + "minecraft:raw_iron", 0L) - before;
        helper.assertValueEqual(mined, 1L, "hand-mined raw iron");
        helper.succeed();
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
                helper.setBlock(new BlockPos(x, 1, z), ModBlocks.IRON_ORE_FIELD.get());
            }
        }
        BlockPos drill = new BlockPos(2, 2, 2);
        helper.setBlock(drill, ModBlocks.ELECTRIC_DRILL.get().defaultBlockState().setValue(DrillBlock.FACING, Direction.EAST));
        BlockPos chest = drill.east();
        helper.setBlock(chest, Blocks.CHEST);
        helper.assertTrue(handler(helper, drill, Direction.UP).insertItem(0, new ItemStack(Items.COAL), true).getCount() == 1,
                "electric drills take no fuel");
        SteamPower.place(helper, new BlockPos(0, 2, 4), Direction.WEST, 8);
        helper.setBlock(new BlockPos(1, 2, 3), ModBlocks.POWER_POLE.get());

        helper.succeedWhen(() -> helper.assertTrue(count(helper, chest, Items.RAW_IRON) >= 3,
                "electric drill output did not reach the chest"));
    }

    /** A 3×3 uranium field with an electric drill at (2,2,2) facing east, a chest in front and grid power. */
    private static BlockPos uraniumDrill(GameTestHelper helper, net.minecraft.world.level.block.Block drillBlock) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), ModBlocks.URANIUM_ORE_FIELD.get());
            }
        }
        BlockPos drill = new BlockPos(2, 2, 2);
        helper.setBlock(drill, drillBlock.defaultBlockState().setValue(DrillBlock.FACING, Direction.EAST));
        helper.setBlock(drill.east(), Blocks.CHEST);
        if (!((DrillBlock) drillBlock).tier().usesFuel()) {
            DrillBlockEntity entity = helper.getBlockEntity(drill);
            helper.onEachTick(() -> entity.energy().setEnergy(entity.energy().getMaxEnergyStored()));
        }
        return drill;
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void uraniumIsNotMinedWithoutAcid(GameTestHelper helper) {
        BlockPos drill = uraniumDrill(helper, ModBlocks.ELECTRIC_DRILL.get());
        helper.runAtTickTime(150, () -> {
            helper.assertValueEqual(count(helper, drill.east(), ModItems.RAW_URANIUM.get()), 0, "uranium ore without sulfuric acid");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void uraniumIsMinedWithAcidAtHalfSpeedAndUsesUpAcid(GameTestHelper helper) {
        BlockPos drill = uraniumDrill(helper, ModBlocks.ELECTRIC_DRILL.get());
        DrillBlockEntity entity = helper.getBlockEntity(drill);
        helper.assertTrue(entity.fluidHandler().fill(new FluidStack(ModFluids.LUBRICANT.get(), 10), IFluidHandler.FluidAction.SIMULATE) == 0, "only acid goes in");
        entity.fluidHandler().fill(new FluidStack(ModFluids.SULFURIC_ACID.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        // A 3x3 field gives 9 x 0.06 = 0.54 items/s for iron; uranium runs at half of that, 0.27/s: 30 s ≈ 8 items.
        helper.runAtTickTime(600, () -> {
            int mined = count(helper, drill.east(), ModItems.RAW_URANIUM.get());
            helper.assertTrue(mined >= 7 && mined <= 9, "mined " + mined + " uranium ore in 30 s, expected about 8");
            helper.assertValueEqual(entity.acid().getFluidAmount(), 100 - mined, "one acid per ore");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void theBurnerDrillHasNoFluidInput(GameTestHelper helper) {
        BlockPos drill = new BlockPos(2, 2, 2);
        helper.setBlock(drill, ModBlocks.BURNER_DRILL.get());
        DrillBlockEntity entity = helper.getBlockEntity(drill);
        helper.assertTrue(entity.fluidHandler() == null, "the burner drill takes no fluids");
        helper.succeed();
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
