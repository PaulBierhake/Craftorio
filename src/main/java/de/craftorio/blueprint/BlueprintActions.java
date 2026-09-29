package de.craftorio.blueprint;

import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Server-side building at the workbench. Results are shown to the player. */
public final class BlueprintActions {
    private BlueprintActions() {
    }

    /** Builds a blueprint up to {@code times} times from the player's inventory. */
    public static boolean build(ServerPlayer player, int index, int times) {
        List<Holder.Reference<Blueprint>> all = Blueprints.sorted(player.registryAccess());
        if (index < 0 || index >= all.size()) {
            return false;
        }
        Holder.Reference<Blueprint> holder = all.get(index);
        Blueprint blueprint = holder.value();
        Team team = TeamData.registry(player.server).ensureTeam(player.getUUID(), player.getGameProfile().getName());
        if (!Blueprints.isKnown(player.registryAccess(), team, holder)) {
            player.displayClientMessage(Component.translatable("craftorio.blueprint.locked").withStyle(ChatFormatting.RED), true);
            return false;
        }
        int possible = Blueprints.craftableTimes(player.getInventory(), blueprint, times);
        if (possible <= 0) {
            player.displayClientMessage(Component.translatable("craftorio.workbench.missing").withStyle(ChatFormatting.RED), true);
            return false;
        }
        Blueprints.take(player.getInventory(), blueprint.ingredients(), possible);
        ItemStack result = blueprint.result().copyWithCount(blueprint.result().getCount() * possible);
        player.getInventory().placeItemBackInInventory(result);
        TeamData.registry(player.server).recordBuild(team.id(), Blueprints.id(holder), possible);
        return true;
    }
}
