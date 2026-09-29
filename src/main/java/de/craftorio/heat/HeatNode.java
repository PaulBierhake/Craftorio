package de.craftorio.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A block entity that holds heat and shares it with the heat blocks next to it (see {@link HeatLogic}). */
public interface HeatNode {
    double temperature();

    void setTemperature(double temperature);

    double capacity();

    /** Adds (or, if negative, takes) heat in FE and keeps the temperature within its limits. */
    default void addHeat(double heat) {
        setTemperature(HeatLogic.warm(temperature(), capacity(), heat));
    }

    /** Blocks that connect to heat: reactor, heat pipe, heat exchanger. */
    interface Connector {
        boolean connectsHeat(BlockState state, Direction face);
    }

    /** The heat node next to {@code pos} in {@code side} direction if both blocks connect to each other, else null. */
    static HeatNode neighbour(Level level, BlockPos pos, Direction side) {
        BlockState own = level.getBlockState(pos);
        BlockPos other = pos.relative(side);
        BlockState state = level.getBlockState(other);
        if (own.getBlock() instanceof Connector mine && mine.connectsHeat(own, side)
                && state.getBlock() instanceof Connector theirs && theirs.connectsHeat(state, side.getOpposite())
                && level.getBlockEntity(other) instanceof HeatNode node) {
            return node;
        }
        return null;
    }

    /** Shares heat with the neighbours: every link is handled once, by the node on its negative side. */
    static void conduct(Level level, BlockPos pos, HeatNode self) {
        int degree = 0;
        HeatNode[] neighbours = new HeatNode[6];
        for (Direction direction : Direction.values()) {
            neighbours[direction.ordinal()] = neighbour(level, pos, direction);
            if (neighbours[direction.ordinal()] != null) {
                degree++;
            }
        }
        for (Direction direction : new Direction[]{Direction.UP, Direction.SOUTH, Direction.EAST}) {
            HeatNode other = neighbours[direction.ordinal()];
            if (other == null) {
                continue;
            }
            int otherDegree = 0;
            for (Direction back : Direction.values()) {
                if (neighbour(level, pos.relative(direction), back) != null) {
                    otherDegree++;
                }
            }
            double flow = HeatLogic.flow(self.temperature(), self.capacity(), degree, other.temperature(), other.capacity(), otherDegree);
            if (Math.abs(flow) > 1e-6) {
                self.addHeat(-flow);
                other.addHeat(flow);
            }
        }
    }
}
