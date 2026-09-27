package de.craftorio.blueprint;

import de.craftorio.economy.Credits;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Server-side unlocking (terminal) and building (workbench). Results are shown to the player. */
public final class BlueprintActions {
    private BlueprintActions() {
    }

    public static boolean unlock(ServerPlayer player, int index) {
        List<Holder.Reference<Blueprint>> all = Blueprints.sorted(player.registryAccess());
        if (index < 0 || index >= all.size()) {
            return false;
        }
        Holder.Reference<Blueprint> holder = all.get(index);
        Blueprint blueprint = holder.value();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), player.getGameProfile().getName());
        UnlockRules.Status status = Blueprints.status(holder, team.unlocked(), team.balance(), player.getInventory());
        if (status != UnlockRules.Status.AVAILABLE) {
            player.displayClientMessage(Component.translatable("craftorio.blueprint.status." + status.name().toLowerCase())
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!registry.withdraw(team.id(), blueprint.cost())) {
            return false;
        }
        Blueprints.take(player.getInventory(), blueprint.unlockItems(), 1);
        registry.unlock(team.id(), Blueprints.id(holder));
        player.displayClientMessage(Component.translatable("craftorio.blueprint.unlocked",
                blueprint.result().getHoverName(), Credits.format(blueprint.cost())).withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    /** Builds a blueprint up to {@code times} times from the player's inventory at a workbench of {@code benchTier}. */
    public static boolean build(ServerPlayer player, int index, int benchTier, int times) {
        List<Holder.Reference<Blueprint>> all = Blueprints.sorted(player.registryAccess());
        if (index < 0 || index >= all.size()) {
            return false;
        }
        Holder.Reference<Blueprint> holder = all.get(index);
        Blueprint blueprint = holder.value();
        Team team = TeamData.registry(player.server).ensureTeam(player.getUUID(), player.getGameProfile().getName());
        if (!Blueprints.isUnlocked(holder, team)) {
            player.displayClientMessage(Component.translatable("craftorio.blueprint.locked").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (blueprint.tier() > benchTier) {
            player.displayClientMessage(Component.translatable("craftorio.workbench.tier_too_low", blueprint.tier())
                    .withStyle(ChatFormatting.RED), true);
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
        return true;
    }
}
