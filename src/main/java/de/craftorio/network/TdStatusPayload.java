package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client, once a second: the team's tower defense status for the HUD and the terminal.
 *
 * @param preview     the next wave as pairs (enemy type ordinal, count)
 * @param canCallWave the next wave can be called early right now
 */
public record TdStatusPayload(boolean hasZone, int level, boolean running, int wave, int waves, int lives,
                              int enemiesLeft, boolean auto, long repairCost, int theme, int mutator, int lastStars,
                              List<Integer> preview, long energy, int bolts, int cartridges, boolean canCallWave)
        implements CustomPacketPayload {
    public static final TdStatusPayload NONE = new TdStatusPayload(false, 1, false, 0, 0, 0, 0, false, 0, 0, 0, 0, List.of(), 0, 0, 0, false);
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
                out.writeVarInt(payload.theme);
                out.writeVarInt(payload.mutator);
                out.writeVarInt(payload.lastStars);
                out.writeVarInt(payload.preview.size());
                payload.preview.forEach(out::writeVarInt);
                out.writeVarLong(payload.energy);
                out.writeVarInt(payload.bolts);
                out.writeVarInt(payload.cartridges);
                out.writeBoolean(payload.canCallWave);
            },
            buf -> {
                FriendlyByteBuf in = new FriendlyByteBuf(buf);
                boolean hasZone = in.readBoolean();
                int level = in.readVarInt();
                boolean running = in.readBoolean();
                int wave = in.readVarInt();
                int waves = in.readVarInt();
                int lives = in.readVarInt();
                int enemiesLeft = in.readVarInt();
                boolean auto = in.readBoolean();
                long repairCost = in.readVarLong();
                int theme = in.readVarInt();
                int mutator = in.readVarInt();
                int lastStars = in.readVarInt();
                int size = in.readVarInt();
                List<Integer> preview = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    preview.add(in.readVarInt());
                }
                return new TdStatusPayload(hasZone, level, running, wave, waves, lives, enemiesLeft, auto, repairCost, theme, mutator,
                        lastStars, List.copyOf(preview), in.readVarLong(), in.readVarInt(), in.readVarInt(), in.readBoolean());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
