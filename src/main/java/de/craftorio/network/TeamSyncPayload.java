package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server to client: the player's team name, balance (HUD) and unlocked blueprints (terminal, workbench). */
public record TeamSyncPayload(String teamName, long balance, List<String> unlocked) implements CustomPacketPayload {
    public static final Type<TeamSyncPayload> TYPE = new Type<>(Craftorio.id("team_sync"));

    public static final StreamCodec<ByteBuf, TeamSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TeamSyncPayload::teamName,
            ByteBufCodecs.VAR_LONG, TeamSyncPayload::balance,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TeamSyncPayload::unlocked,
            TeamSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
