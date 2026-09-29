package de.craftorio.gametest;

import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;

/** A ready-made steam power plant for game tests: engine, boiler, offshore pump and a water block in a row. */
public final class SteamPower {
    private SteamPower() {
    }

    /**
     * Places the engine at {@code engine} with boiler, pump and water one block after the other in {@code direction},
     * and puts {@code coal} coal into the boiler. One engine makes 900 kW.
     */
    public static void place(GameTestHelper helper, BlockPos engine, Direction direction, int coal) {
        BlockPos boiler = engine.relative(direction);
        BlockPos pump = boiler.relative(direction);
        helper.setBlock(engine, ModBlocks.STEAM_ENGINE.get());
        helper.setBlock(boiler, ModBlocks.BOILER.get());
        helper.setBlock(pump, ModBlocks.OFFSHORE_PUMP.get());
        helper.setBlock(pump.relative(direction), Blocks.WATER);
        var fuel = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(boiler), null);
        if (fuel == null) {
            throw new IllegalStateException("boiler has no fuel slot");
        }
        fuel.insertItem(0, new ItemStack(Items.COAL, coal), false);
    }
}
