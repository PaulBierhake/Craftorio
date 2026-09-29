package de.craftorio.blueprint;

import de.craftorio.team.Team;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Comparator;
import java.util.List;

/** Snapshot of a team's statistics, sent when the terminal opens. */
public record TerminalStats(long totalEarned, long totalSpent, long earnedLastTenMinutes, List<Row> topSales,
                            List<Finished> researched) {
    public static final int TOP_ROWS = 4;
    public static final int FINISHED_ROWS = 4;

    /** A finished research with the game time in ticks it was finished at. */
    public record Finished(String research, long tick) {
        static final StreamCodec<RegistryFriendlyByteBuf, Finished> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Finished::research,
                ByteBufCodecs.VAR_LONG, Finished::tick,
                Finished::new);
    }

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
            Finished.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalStats::researched,
            TerminalStats::new);

    /** @param questProgress progress of every quest in guide order */
    public static TerminalStats of(Team team, long currentMinute) {
        List<Row> rows = team.sales().entrySet().stream()
                .map(entry -> new Row(entry.getKey(), entry.getValue().count(), entry.getValue().credits()))
                .sorted(Comparator.comparingLong(Row::credits).reversed())
                .limit(TOP_ROWS)
                .toList();
        List<Finished> finished = team.researchTimes().entrySet().stream()
                .map(entry -> new Finished(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(Finished::tick).reversed())
                .limit(FINISHED_ROWS)
                .toList();
        return new TerminalStats(team.totalEarned(), team.totalSpent(), team.earnedInLastMinutes(currentMinute, 10), rows, finished);
    }
}
