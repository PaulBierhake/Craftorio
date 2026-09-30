package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/** Server to client when a level starts (and now and then): the walk points of the arena's path in world coordinates, so the client can draw the enemies along it. */
public record TdPathPayload(int slot, List<float[]> points) implements CustomPacketPayload {
    public static final Type<TdPathPayload> TYPE = new Type<>(Craftorio.id("td_path"));

    public static final StreamCodec<ByteBuf, TdPathPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                FriendlyByteBuf out = new FriendlyByteBuf(buf);
                out.writeVarInt(payload.slot);
                out.writeVarInt(payload.points.size());
                for (float[] point : payload.points) {
                    out.writeFloat(point[0]);
                    out.writeFloat(point[1]);
                    out.writeFloat(point[2]);
                }
            },
            buf -> {
                FriendlyByteBuf in = new FriendlyByteBuf(buf);
                int slot = in.readVarInt();
                int size = in.readVarInt();
                List<float[]> points = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    points.add(new float[]{in.readFloat(), in.readFloat(), in.readFloat()});
                }
                return new TdPathPayload(slot, List.copyOf(points));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
