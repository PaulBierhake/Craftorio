package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.fluid.FluidPipeBlockEntity;
import de.craftorio.logistics.ElevatorBlock;
import de.craftorio.registry.ModFluids;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
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

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void elevatorCarriesFluidUpIntoThePipesAtTheTop(GameTestHelper helper) {
        BlockPos sender = new BlockPos(2, 1, 2);
        BlockPos receiver = new BlockPos(2, 4, 2);
        helper.setBlock(sender, ModBlocks.ELEVATOR.get().defaultBlockState().setValue(ElevatorBlock.MODE, ElevatorBlock.Mode.SEND_UP));
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.CAP_ROCK.get());
        helper.setBlock(receiver, ModBlocks.ELEVATOR.get());
        BlockPos pipe = new BlockPos(3, 4, 2);
        helper.setBlock(pipe, ModBlocks.PIPE.get().connectedState(helper.getLevel(), helper.absolutePos(pipe)));

        var up = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(sender), null);
        var top = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(receiver), null);
        helper.assertTrue(up != null && top != null, "elevators have fluid handlers");
        helper.assertValueEqual(top.fill(new FluidStack(ModFluids.SULFURIC_ACID.get(), 100), IFluidHandler.FluidAction.SIMULATE), 0, "receiver takes no input");
        helper.assertValueEqual(up.fill(new FluidStack(ModFluids.SULFURIC_ACID.get(), 500), IFluidHandler.FluidAction.EXECUTE), 500, "sender takes 500 units");

        helper.succeedWhen(() -> {
            FluidPipeBlockEntity pipeEntity = helper.getBlockEntity(pipe);
            helper.assertTrue(pipeEntity.buffer().getFluid().getFluid() == ModFluids.SULFURIC_ACID.get() && pipeEntity.buffer().getFluidAmount() > 0,
                    "acid arrived in the pipe at the top");
        });
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos pos) {
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        if (handler == null) {
            throw new IllegalStateException("no item handler at " + pos);
        }
        return handler;
    }
}
