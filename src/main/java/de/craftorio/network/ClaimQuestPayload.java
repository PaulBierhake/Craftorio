package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: collect the reward of guide step {@code index} from the handbook. */
public record ClaimQuestPayload(int index) implements CustomPacketPayload {
    public static final Type<ClaimQuestPayload> TYPE = new Type<>(Craftorio.id("claim_quest"));
    public static final StreamCodec<ByteBuf, ClaimQuestPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ClaimQuestPayload::new, ClaimQuestPayload::index);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
