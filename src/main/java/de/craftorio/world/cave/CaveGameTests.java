package de.craftorio.world.cave;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import net.minecraft.core.BlockPos;
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
        CaveShape shape = CaveAreas.get(helper.getLevel().getServer()).shape();

        CaveCarver.carveColumn(helper.getLevel(), x, z, shape);

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
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.COAL_GENERATOR.get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.POWER_POLE.get());
        items(helper, new BlockPos(1, 1, 1)).insertItem(0, new ItemStack(Items.COAL, 16), false);
        CaveEntranceBlockEntity site = helper.getBlockEntity(entrance);
        IItemHandler materials = items(helper, entrance);

        helper.assertTrue(!materials.insertItem(0, new ItemStack(Items.DIRT), true).isEmpty(), "dirt is not needed");
        ItemStack rest = materials.insertItem(0, new ItemStack(ModItems.MOTOR.get(), 12), false);
        helper.assertValueEqual(rest.getCount(), 4, "only 8 motors are needed");
        helper.assertValueEqual(site.stage(), CaveEntranceBlock.STAGE_MATERIALS, "still waiting for materials");
        for (Map.Entry<Item, Integer> required : CaveEntranceBlockEntity.requirements().entrySet()) {
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
            helper.assertTrue(CaveAreas.get(helper.getLevel().getServer()).isUnlocked(chunk), "cave area unlocked");
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
