package de.craftorio.research;

import de.craftorio.CraftorioConfig;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/** Server side of the terminal's research tab: queueing and dequeueing researches. */
public final class ResearchActions {
    private ResearchActions() {
    }

    /** Terminal button: queues the research at this index of {@link Researches#sorted}, or removes it from the queue. */
    public static boolean toggle(ServerPlayer player, int index) {
        List<Holder.Reference<Research>> all = Researches.sorted(player.registryAccess());
        if (index < 0 || index >= all.size()) {
            return false;
        }
        Holder.Reference<Research> holder = all.get(index);
        Research research = holder.value();
        String id = Researches.id(holder);
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), player.getGameProfile().getName());
        if (team.researched().contains(id)) {
            return false;
        }
        if (team.researchQueue().contains(id)) {
            registry.dequeueResearch(team.id(), id);
            pruneQueue(player, registry, team);
            return true;
        }
        if (!ResearchRules.canQueue(id, Researches.requires(research), team.researched(), team.researchQueue())) {
            player.displayClientMessage(Component.translatable("craftorio.research.locked").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!research.unlockItems().isEmpty() && !team.keyItemsPaid(id)) {
            if (!Blueprints.hasAll(player.getInventory(), research.unlockItems(), 1)) {
                player.displayClientMessage(Component.translatable("craftorio.research.missing_key_items").withStyle(ChatFormatting.RED), true);
                return false;
            }
            Blueprints.take(player.getInventory(), research.unlockItems(), 1);
            registry.markKeyItemsPaid(team.id(), id);
        }
        registry.enqueueResearch(team.id(), id);
        return true;
    }

    /** After a dequeue, researches that no longer have their prerequisites in front of them leave the queue as well. */
    private static void pruneQueue(ServerPlayer player, TeamRegistry registry, Team team) {
        var access = player.registryAccess();
        var registryOfResearch = access.registryOrThrow(de.craftorio.registry.ModRegistries.RESEARCH);
        Set<String> researched = team.researched();
        List<String> kept = ResearchRules.pruneQueue(team.researchQueue(), id -> {
            Research research = registryOfResearch.get(net.minecraft.resources.ResourceLocation.parse(id));
            return research == null ? List.of() : Researches.requires(research);
        }, researched);
        for (String id : List.copyOf(team.researchQueue())) {
            if (!kept.contains(id)) {
                registry.dequeueResearch(team.id(), id);
            }
        }
    }

    /** Units the research costs this server, with the pacing factor. */
    public static long units(Research research) {
        return research.units(CraftorioConfig.researchCost());
    }
}
