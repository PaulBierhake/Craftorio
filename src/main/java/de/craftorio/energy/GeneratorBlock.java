package de.craftorio.energy;

import com.mojang.serialization.MapCodec;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class GeneratorBlock extends MachineBaseBlock {
    private final GeneratorType type;
    private final MapCodec<GeneratorBlock> codec;

    public GeneratorBlock(GeneratorType type, Properties properties) {
        super(properties);
        this.type = type;
        this.codec = simpleCodec(p -> new GeneratorBlock(type, p));
    }

    public GeneratorType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.GENERATOR.get(), GeneratorBlockEntity::serverTick);
    }
}
