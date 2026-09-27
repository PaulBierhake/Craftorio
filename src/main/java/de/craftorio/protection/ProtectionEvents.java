package de.craftorio.protection;

import de.craftorio.Craftorio;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.Optional;

/** Keeps players of other teams from breaking, opening or blowing up protected blocks. Operators in creative may. */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class ProtectionEvents {
    private ProtectionEvents() {
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ServerPlayer player
                && BlockOwnership.isProtectable(event.getPlacedBlock())) {
            BlockOwnership.get(level).claim(event.getPos(), teamOf(player).id());
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (denied(level, event.getPos(), player)) {
            event.setCanceled(true);
            return;
        }
        BlockOwnership.get(level).release(event.getPos());
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ServerPlayer player
                && denied(level, event.getPos(), player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level && BlockOwnership.enabled()) {
            BlockOwnership ownership = BlockOwnership.get(level);
            event.getAffectedBlocks().removeIf(pos -> ownership.owner(level, pos).isPresent());
        }
    }

    /** True (and tells the player) if the block belongs to another team. */
    private static boolean denied(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!BlockOwnership.enabled() || bypasses(player)) {
            return false;
        }
        Team team = teamOf(player);
        Optional<Team> owner = BlockOwnership.get(level).owner(level, pos);
        if (owner.isEmpty() || owner.get().id().equals(team.id())) {
            return false;
        }
        player.displayClientMessage(Component.translatable("craftorio.protection.denied", owner.get().name())
                .withStyle(ChatFormatting.RED), true);
        return true;
    }

    private static boolean bypasses(Player player) {
        return player.isCreative() && player.hasPermissions(2);
    }

    private static Team teamOf(ServerPlayer player) {
        return TeamData.registry(player.server).ensureTeam(player.getUUID(), player.getGameProfile().getName());
    }
}
