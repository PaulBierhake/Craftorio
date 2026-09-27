package de.craftorio.blueprint;

import de.craftorio.team.Team;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Comparator;
import java.util.List;

/** Snapshot of a team's statistics, sent when the terminal opens. */
public record TerminalStats(long totalEarned, long totalSpent, long earnedLastTenMinutes, List<Row> topSales) {
    public static final int TOP_ROWS = 7;

    public record Row(String item, long count, long credits) {
        static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Row::item,
                ByteBufCodecs.VAR_LONG, Row::count,
                ByteBufCodecs.VAR_LONG, Row::credits,
                Row::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, TerminalStats::totalEarned,
            ByteBufCodecs.VAR_LONG, TerminalStats::totalSpent,
            ByteBufCodecs.VAR_LONG, TerminalStats::earnedLastTenMinutes,
            Row.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalStats::topSales,
            TerminalStats::new);

    public static TerminalStats of(Team team, long currentMinute) {
        List<Row> rows = team.sales().entrySet().stream()
                .map(entry -> new Row(entry.getKey(), entry.getValue().count(), entry.getValue().credits()))
                .sorted(Comparator.comparingLong(Row::credits).reversed())
                .limit(TOP_ROWS)
                .toList();
        return new TerminalStats(team.totalEarned(), team.totalSpent(), team.earnedInLastMinutes(currentMinute, 10), rows);
    }
}
