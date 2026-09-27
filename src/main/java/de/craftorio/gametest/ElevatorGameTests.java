package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.logistics.ElevatorBlock;
import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ElevatorGameTests {
    private ElevatorGameTests() {
    }

    @GameTest(template = "empty")
    public static void elevatorCarriesItemsUpThroughBlocks(GameTestHelper helper) {
        BlockPos sender = new BlockPos(2, 1, 2);
        BlockPos receiver = new BlockPos(2, 4, 2);
        helper.setBlock(sender, ModBlocks.ELEVATOR.get().defaultBlockState().setValue(ElevatorBlock.MODE, ElevatorBlock.Mode.SEND_UP));
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.CAP_ROCK.get()); // travels through solid blocks
        helper.setBlock(receiver, ModBlocks.ELEVATOR.get().defaultBlockState().setValue(ElevatorBlock.FACING, Direction.EAST));
        BlockPos chest = receiver.east();
        helper.setBlock(chest, Blocks.CHEST);

        IItemHandler in = handler(helper, sender);
        helper.assertTrue(!handler(helper, receiver).insertItem(0, new ItemStack(Items.IRON_INGOT), true).isEmpty(), "receiver takes no input");
        in.insertItem(0, new ItemStack(Items.IRON_INGOT, 20), false);

        helper.succeedWhen(() -> helper.assertValueEqual(((Container) helper.getBlockEntity(chest)).countItem(Items.IRON_INGOT), 20, "ingots arrived"));
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos pos) {
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        if (handler == null) {
            throw new IllegalStateException("no item handler at " + pos);
        }
        return handler;
    }
}
