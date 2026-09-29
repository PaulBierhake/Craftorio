package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server to client: the player's team name, balance (HUD) finished researches and the research queue (terminal, workbench) and the guide (progress, collected rewards). */
public record TeamSyncPayload(String teamName, long balance, List<String> researched, List<String> claimedQuests,
                              List<Long> questProgress, List<String> researchQueue, long activeProgress) implements CustomPacketPayload {
    public static final Type<TeamSyncPayload> TYPE = new Type<>(Craftorio.id("team_sync"));

    private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final StreamCodec<ByteBuf, TeamSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.teamName);
                ByteBufCodecs.VAR_LONG.encode(buf, payload.balance);
                STRINGS.encode(buf, payload.researched);
                STRINGS.encode(buf, payload.claimedQuests);
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).encode(buf, payload.questProgress);
                STRINGS.encode(buf, payload.researchQueue);
                ByteBufCodecs.VAR_LONG.encode(buf, payload.activeProgress);
            },
            buf -> new TeamSyncPayload(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), STRINGS.decode(buf),
                    STRINGS.decode(buf), ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).decode(buf), STRINGS.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
