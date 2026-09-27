package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the player's team name and balance, shown in the HUD. */
public record TeamSyncPayload(String teamName, long balance) implements CustomPacketPayload {
    public static final Type<TeamSyncPayload> TYPE = new Type<>(Craftorio.id("team_sync"));

    public static final StreamCodec<ByteBuf, TeamSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TeamSyncPayload::teamName,
            ByteBufCodecs.VAR_LONG, TeamSyncPayload::balance,
            TeamSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
