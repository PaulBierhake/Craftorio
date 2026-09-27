package de.craftorio.quest;

import de.craftorio.defense.TowerDefense;
import de.craftorio.economy.Credits;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server side of the guide: measuring a team's progress and paying out rewards. */
public final class QuestActions {
    private QuestActions() {
    }

    public static Quest.Progress progress(MinecraftServer server, Team team) {
        Map<String, Long> sold = new HashMap<>();
        team.sales().forEach((item, sales) -> sold.put(item, sales.count()));
        int tdLevels = TowerDefense.get(server).zone(team.id()).map(zone -> zone.level() - 1).orElse(0);
        return new Quest.Progress(team.totalEarned(), sold, team.unlocked(), team.built(), tdLevels);
    }

    /** Progress of every quest in {@link Quests#ALL} order. */
    public static List<Long> progressList(MinecraftServer server, Team team) {
        Quest.Progress progress = progress(server, team);
        return Quests.ALL.stream().map(quest -> quest.progress(progress)).toList();
    }

    public static boolean claim(ServerPlayer player, int index) {
        if (index < 0 || index >= Quests.ALL.size()) {
            return false;
        }
        Quest quest = Quests.ALL.get(index);
        Team team = TeamData.registry(player.server).ensureTeam(player.getUUID(), player.getGameProfile().getName());
        if (!quest.done(progress(player.server, team))) {
            player.displayClientMessage(Component.translatable("craftorio.quest.not_done").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!TeamData.registry(player.server).claimQuest(team.id(), quest.id(), quest.reward())) {
            return false;
        }
        player.displayClientMessage(Component.translatable("craftorio.quest.claimed", Component.translatable("craftorio.quest." + quest.id()),
                Credits.format(quest.reward())).withStyle(ChatFormatting.GREEN), true);
        return true;
    }
}
