package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player pressed the ability key while looking at a tower. */
public record TowerAbilityPayload(BlockPos pos, int index) implements CustomPacketPayload {
    public static final Type<TowerAbilityPayload> TYPE = new Type<>(Craftorio.id("tower_ability"));
    public static final StreamCodec<ByteBuf, TowerAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TowerAbilityPayload::pos, ByteBufCodecs.VAR_INT, TowerAbilityPayload::index, TowerAbilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
