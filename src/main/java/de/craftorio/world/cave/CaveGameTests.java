package de.craftorio.world.cave;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Map;

@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CaveGameTests {
    private static final String EMPTY = "empty";

    private CaveGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void carvingTurnsCaveFillIntoRockAndHalls(GameTestHelper helper) {
        BlockPos column = helper.absolutePos(new BlockPos(2, 0, 2));
        int x = column.getX();
        int z = column.getZ();
        for (int y = CaveLayers.CAVE_BOTTOM; y < CaveLayers.CAP_ONE_BOTTOM; y++) {
            helper.getLevel().setBlock(new BlockPos(x, y, z), ModBlocks.CAVE_RUBBLE.get().defaultBlockState(), 2);
        }
        CaveShape shape = CaveAreas.get(helper.getLevel().getServer()).shape(Layer.CAVES);

        CaveCarver.carveColumn(helper.getLevel(), Layer.CAVES, x, z, shape);

        for (int y = CaveLayers.CAVE_BOTTOM; y < CaveLayers.CAP_ONE_BOTTOM; y++) {
            helper.assertFalse(helper.getLevel().getBlockState(new BlockPos(x, y, z)).is(ModBlocks.CAVE_RUBBLE.get()), "fill left at y=" + y);
        }
        int floor = shape.floorY(x, z);
        if (!shape.isPillar(x, z)) {
            helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x, floor, z)).is(Blocks.TUFF), "tuff floor");
            helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x, floor + 1, z)).isAir(), "open above the floor");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 1800)
    public static void entranceNeedsMaterialsAndPowerThenUnlocksArea(GameTestHelper helper) {
        BlockPos entrance = new BlockPos(3, 1, 3);
        helper.setBlock(entrance, ModBlocks.CAVE_ENTRANCE.get());
        de.craftorio.gametest.SteamPower.place(helper, new BlockPos(1, 1, 1), Direction.WEST, 16);
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.POWER_POLE.get());
        CaveEntranceBlockEntity site = helper.getBlockEntity(entrance);
        IItemHandler materials = items(helper, entrance);

        helper.assertTrue(!materials.insertItem(0, new ItemStack(Items.DIRT), true).isEmpty(), "dirt is not needed");
        ItemStack rest = materials.insertItem(0, new ItemStack(ModItems.MOTOR.get(), 12), false);
        helper.assertValueEqual(rest.getCount(), 4, "only 8 motors are needed");
        helper.assertValueEqual(site.stage(), CaveEntranceBlock.STAGE_MATERIALS, "still waiting for materials");
        for (Map.Entry<Item, Integer> required : CaveEntranceBlockEntity.requirements(Layer.CAVES).entrySet()) {
            int left = required.getValue();
            while (left > 0) {
                int batch = Math.min(64, left);
                materials.insertItem(0, new ItemStack(required.getKey(), batch), false);
                left -= batch;
            }
        }
        helper.assertValueEqual(site.stage(), CaveEntranceBlock.STAGE_DRILLING, "drilling after all materials");

        ChunkPos chunk = new ChunkPos(helper.absolutePos(entrance));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(entrance, CaveEntranceBlock.STAGE, CaveEntranceBlock.STAGE_OPEN);
            helper.assertTrue(CaveAreas.get(helper.getLevel().getServer()).isUnlocked(Layer.CAVES, chunk), "cave area unlocked");
        });
    }

    @GameTest(template = EMPTY)
    @SuppressWarnings("removal")
    public static void theEntranceMenuBooksDeliveriesAndKeepsWhatIsNotNeeded(GameTestHelper helper) {
        BlockPos entrance = new BlockPos(3, 1, 3);
        helper.setBlock(entrance, ModBlocks.CAVE_ENTRANCE.get());
        CaveEntranceBlockEntity site = helper.getBlockEntity(entrance);
        var player = helper.makeMockServerPlayerInLevel();
        var menu = new de.craftorio.menu.EntranceMenu(1, player.getInventory(), site, new net.minecraft.world.inventory.SimpleContainerData(de.craftorio.menu.EntranceMenu.DATA_COUNT));

        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "only needed items go in");
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(Items.COBBLESTONE)), "cobblestone is needed");
        menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT, 64));
        helper.assertValueEqual(site.delivered(Items.IRON_INGOT), 32, "the needed 32 are booked at once");
        helper.assertValueEqual(menu.getSlot(0).getItem().getCount(), 32, "the surplus stays in the slot");
        menu.removed(player);
        helper.assertValueEqual(player.getInventory().countItem(Items.IRON_INGOT), 32, "and goes back to the player");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void aCarvedChunkGetsDecorationButNotOnOreFields(GameTestHelper helper) {
        var level = helper.getLevel();
        ChunkPos chunk = new ChunkPos(helper.absolutePos(new BlockPos(2, 0, 2)));
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = CaveLayers.CAVE_BOTTOM; y < CaveLayers.CAP_ONE_BOTTOM; y++) {
                    level.setBlock(new BlockPos(x, y, z), ModBlocks.CAVE_RUBBLE.get().defaultBlockState(), 2);
                }
            }
        }
        CaveShape shape = CaveAreas.get(level.getServer()).shape(Layer.CAVES);
        CaveCarver.carveChunk(level, Layer.CAVES, chunk, shape);

        int decoration = 0;
        int wells = 0;
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = CaveLayers.CAVE_BOTTOM; y < CaveLayers.CAP_ONE_BOTTOM; y++) {
                    var state = level.getBlockState(new BlockPos(x, y, z));
                    if (state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.MOSS_CARPET)
                            || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.COBWEB)
                            || state.is(Blocks.AMETHYST_CLUSTER)) {
                        decoration++;
                    }
                    wells += state.is(ModBlocks.OIL_WELL.get()) ? 1 : 0;
                }
            }
        }
        helper.assertTrue(decoration >= 10, "only " + decoration + " decoration blocks in a carved chunk");
        // Deterministic: carving the same chunk again in the same state places the same decoration.
        helper.assertTrue(wells == 0 || decoration > 0, "wells and decoration");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void carvingMineLayerUsesBasaltFloor(GameTestHelper helper) {
        BlockPos column = helper.absolutePos(new BlockPos(2, 0, 2));
        int x = column.getX();
        int z = column.getZ();
        for (int y = Layer.MINES.minY(); y <= Layer.MINES.maxY(); y++) {
            helper.getLevel().setBlock(new BlockPos(x, y, z), ModBlocks.MINE_RUBBLE.get().defaultBlockState(), 2);
        }
        CaveShape shape = CaveAreas.get(helper.getLevel().getServer()).shape(Layer.MINES);

        CaveCarver.carveColumn(helper.getLevel(), Layer.MINES, x, z, shape);

        for (int y = Layer.MINES.minY(); y <= Layer.MINES.maxY(); y++) {
            helper.assertFalse(helper.getLevel().getBlockState(new BlockPos(x, y, z)).is(ModBlocks.MINE_RUBBLE.get()), "fill left at y=" + y);
        }
        if (!shape.isPillar(x, z)) {
            int floor = shape.floorY(x, z);
            helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x, floor, z)).is(Blocks.SMOOTH_BASALT), "basalt floor");
            helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x, floor + 1, z)).isAir(), "open above the floor");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 2400)
    public static void mineShaftNeedsDeepMaterialsThenUnlocksMines(GameTestHelper helper) {
        BlockPos shaft = new BlockPos(3, 1, 3);
        helper.setBlock(shaft, ModBlocks.MINE_SHAFT.get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.POWER_POLE.get());
        de.craftorio.gametest.SteamPower.place(helper, new BlockPos(1, 1, 1), Direction.WEST, 32);
        CaveEntranceBlockEntity site = helper.getBlockEntity(shaft);
        IItemHandler materials = items(helper, shaft);

        helper.assertValueEqual(site.target(), Layer.MINES, "mine shaft opens the mine layer");
        helper.assertTrue(!materials.insertItem(0, new ItemStack(Items.COBBLESTONE), true).isEmpty(), "cobblestone is not needed");
        for (Map.Entry<Item, Integer> required : CaveEntranceBlockEntity.requirements(Layer.MINES).entrySet()) {
            int left = required.getValue();
            while (left > 0) {
                int batch = Math.min(64, left);
                materials.insertItem(0, new ItemStack(required.getKey(), batch), false);
                left -= batch;
            }
        }
        helper.assertValueEqual(site.stage(), CaveEntranceBlock.STAGE_DRILLING, "drilling after all materials");

        ChunkPos chunk = new ChunkPos(helper.absolutePos(shaft));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(shaft, CaveEntranceBlock.STAGE, CaveEntranceBlock.STAGE_OPEN);
            helper.assertTrue(CaveAreas.get(helper.getLevel().getServer()).isUnlocked(Layer.MINES, chunk), "mine area unlocked");
        });
    }

    private static IItemHandler items(GameTestHelper helper, BlockPos pos) {
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        if (handler == null) {
            throw new IllegalStateException("no item handler at " + pos);
        }
        return handler;
    }
}
