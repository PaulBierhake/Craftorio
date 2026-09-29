package de.craftorio.fluid;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Blocks that take part in the fluid network: pipes draw a connection to them where {@link #connectsFluid} is true. */
public interface FluidConnector {
    /** Can fluid pass through this face of the block? */
    boolean connectsFluid(BlockState state, Direction face);
}
