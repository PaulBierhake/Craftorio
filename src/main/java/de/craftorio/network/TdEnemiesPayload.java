package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client, a few times a second: every enemy of an arena as (id, kind, modifier bits, distance along the path).
 * Between two snapshots the client lets them walk on at {@code speed} times their own speed, so little has to be sent.
 *
 * @param speed  factor on the enemies' own speed (late game)
 * @param ids    enemy ids, ascending
 * @param kinds  {@code EnemyDef.index()} per enemy
 * @param flags  per enemy: 1 camo, 2 regrow, 4 fortified
 * @param distances blocks along the path
 */
public record TdEnemiesPayload(int slot, float speed, int[] ids, byte[] kinds, byte[] flags, float[] distances) implements CustomPacketPayload {
    public static final Type<TdEnemiesPayload> TYPE = new Type<>(Craftorio.id("td_enemies"));

    public static final StreamCodec<ByteBuf, TdEnemiesPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                FriendlyByteBuf out = new FriendlyByteBuf(buf);
                out.writeVarInt(payload.slot);
                out.writeFloat(payload.speed);
                out.writeVarInt(payload.ids.length);
                int previous = 0;
                for (int i = 0; i < payload.ids.length; i++) {
                    out.writeVarInt(payload.ids[i] - previous); // ids ascend: small steps
                    previous = payload.ids[i];
                    out.writeByte(payload.kinds[i]);
                    out.writeByte(payload.flags[i]);
                    out.writeFloat(payload.distances[i]);
                }
            },
            buf -> {
                FriendlyByteBuf in = new FriendlyByteBuf(buf);
                int slot = in.readVarInt();
                float speed = in.readFloat();
                int size = in.readVarInt();
                int[] ids = new int[size];
                byte[] kinds = new byte[size];
                byte[] flags = new byte[size];
                float[] distances = new float[size];
                int previous = 0;
                for (int i = 0; i < size; i++) {
                    previous += in.readVarInt();
                    ids[i] = previous;
                    kinds[i] = in.readByte();
                    flags[i] = in.readByte();
                    distances[i] = in.readFloat();
                }
                return new TdEnemiesPayload(slot, speed, ids, kinds, flags, distances);
            });

    public static TdEnemiesPayload empty(int slot) {
        return new TdEnemiesPayload(slot, 1, new int[0], new byte[0], new byte[0], new float[0]);
    }

    public int size() {
        return ids.length;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
