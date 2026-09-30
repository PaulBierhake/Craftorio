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

    /** Upgrades (tiers of the three paths) and the coins paid for a tower carried as an item; towers without it are fresh from the factory. */
    public record TowerState(int path1, int path2, int path3, long paid) {
        public static final Codec<TowerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("path1", 0).forGetter(TowerState::path1),
                Codec.INT.optionalFieldOf("path2", 0).forGetter(TowerState::path2),
                Codec.INT.optionalFieldOf("path3", 0).forGetter(TowerState::path3),
                Codec.LONG.optionalFieldOf("paid", 0L).forGetter(TowerState::paid)).apply(instance, TowerState::new));
        public static final StreamCodec<ByteBuf, TowerState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TowerState::path1, ByteBufCodecs.VAR_INT, TowerState::path2, ByteBufCodecs.VAR_INT, TowerState::path3,
                ByteBufCodecs.VAR_LONG, TowerState::paid, TowerState::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TowerState>> TOWER_STATE =
            COMPONENTS.registerComponentType("tower_state", builder -> builder.persistent(TowerState.CODEC).networkSynchronized(TowerState.STREAM_CODEC));

    /** Where the path wand placed its last path block, so the next click can draw a straight line from there. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> PATH_ANCHOR =
            COMPONENTS.registerComponentType("path_anchor", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    private ModDataComponents() {
    }
}
