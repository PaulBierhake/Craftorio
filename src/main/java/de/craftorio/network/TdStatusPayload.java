package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client, once a second: the team's tower defense status for the HUD and the terminal. */
public record TdStatusPayload(boolean hasZone, int level, boolean running, int wave, int waves, int lives,
                              int enemiesLeft, boolean auto, long repairCost) implements CustomPacketPayload {
    public static final TdStatusPayload NONE = new TdStatusPayload(false, 1, false, 0, 0, 0, 0, false, 0);
    public static final Type<TdStatusPayload> TYPE = new Type<>(Craftorio.id("td_status"));

    public static final StreamCodec<ByteBuf, TdStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                FriendlyByteBuf out = new FriendlyByteBuf(buf);
                out.writeBoolean(payload.hasZone);
                out.writeVarInt(payload.level);
                out.writeBoolean(payload.running);
                out.writeVarInt(payload.wave);
                out.writeVarInt(payload.waves);
                out.writeVarInt(payload.lives);
                out.writeVarInt(payload.enemiesLeft);
                out.writeBoolean(payload.auto);
                out.writeVarLong(payload.repairCost);
            },
            buf -> {
                FriendlyByteBuf in = new FriendlyByteBuf(buf);
                return new TdStatusPayload(in.readBoolean(), in.readVarInt(), in.readBoolean(), in.readVarInt(), in.readVarInt(),
                        in.readVarInt(), in.readVarInt(), in.readBoolean(), in.readVarLong());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
