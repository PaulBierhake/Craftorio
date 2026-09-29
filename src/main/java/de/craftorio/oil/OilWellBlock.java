package de.craftorio.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/** A crude oil well in the caves; a pumpjack on top of it draws oil at the well's yield. Unbreakable. */
public final class OilWellBlock extends Block {
    public OilWellBlock(Properties properties) {
        super(properties);
    }

    /** Yield in percent, 100 to 300 in steps of 25, fixed by the position; 100 % is 10 units of crude per second. */
    public static int yieldPercent(BlockPos pos) {
        long hash = pos.asLong() * 0x9E3779B97F4A7C15L;
        return 100 + 25 * (int) Math.floorMod(hash >>> 40, 9L);
    }
}
