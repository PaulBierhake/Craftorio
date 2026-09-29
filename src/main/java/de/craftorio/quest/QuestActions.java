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
        // "Unlocked" goals mean the blueprint is available: free from the start or researched.
        java.util.Set<String> known = de.craftorio.research.Researches.knownOf(server.registryAccess(), team.researched(),
                Quests.ALL.stream().filter(quest -> quest.kind() == Quest.Kind.UNLOCK).map(Quest::target).toList());
        return new Quest.Progress(team.totalEarned(), sold, known, team.built(), tdLevels);
    }

    /** Progress of every quest in {@link Quests#ALL} order. */
    public static List<Long> progressList(MinecraftServer server, Team team) {
        Quest.Progress progress = progress(server, team);
        return Quests.ALL.stream().map(quest -> quest.progress(progress)).toList();
    }

    /** "8× Coal", "50 ¢" or both, for the guide and the chat message. */
    public static Component rewardText(Quest quest) {
        var parts = new java.util.ArrayList<Component>();
        if (quest.reward() > 0) {
            parts.add(Component.literal(de.craftorio.economy.Credits.format(quest.reward())));
        }
        if (quest.hasRewardItem()) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(quest.rewardItem()));
            parts.add(Component.literal(quest.rewardCount() + "× ").append(item.getDescription()));
        }
        var text = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            text.append(i == 0 ? parts.get(i) : Component.literal(" + ").append(parts.get(i)));
        }
        return text;
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
        if (quest.hasRewardItem()) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(quest.rewardItem()));
            player.getInventory().placeItemBackInInventory(new net.minecraft.world.item.ItemStack(item, quest.rewardCount()));
        }
        player.displayClientMessage(Component.translatable("craftorio.quest.claimed", Component.translatable("craftorio.quest." + quest.id()),
                rewardText(quest)).withStyle(ChatFormatting.GREEN), true);
        return true;
    }
}
