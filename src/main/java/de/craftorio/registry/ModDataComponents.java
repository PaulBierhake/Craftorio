package de.craftorio.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Craftorio.MOD_ID);

    /** Upgrade level of a tower carried as an item (old items also stored the health of the tower, which is ignored now). */
    public record TowerState(int level) {
        public static final Codec<TowerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("level").forGetter(TowerState::level)).apply(instance, TowerState::new));
        public static final StreamCodec<ByteBuf, TowerState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TowerState::level, TowerState::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TowerState>> TOWER_STATE =
            COMPONENTS.registerComponentType("tower_state", builder -> builder.persistent(TowerState.CODEC).networkSynchronized(TowerState.STREAM_CODEC));

    /** Where the path wand placed its last path block, so the next click can draw a straight line from there. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> PATH_ANCHOR =
            COMPONENTS.registerComponentType("path_anchor", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    private ModDataComponents() {
    }
}
