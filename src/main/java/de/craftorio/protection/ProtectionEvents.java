package de.craftorio.protection;

import de.craftorio.Craftorio;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    /**
     * Claims protected blocks for the placing team. They may not touch another team's protected blocks, so no hopper,
     * belt, drill, elevator or inserter can move items into or out of a foreign base.
     */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)
                || !BlockOwnership.isProtectable(event.getPlacedBlock())) {
            return;
        }
        Team team = teamOf(player);
        // Each arena belongs to one team, so the neighbour rule is not needed there.
        if (BlockOwnership.enabled() && !bypasses(player) && !de.craftorio.defense.arena.Arenas.isArena(level)) {
            Optional<Team> neighbour = foreignNeighbour(level, event.getPos(), team);
            if (neighbour.isPresent()) {
                player.displayClientMessage(Component.translatable("craftorio.protection.too_close", neighbour.get().name())
                        .withStyle(ChatFormatting.RED), true);
                event.setCanceled(true);
                return;
            }
        }
        BlockOwnership.get(level).claim(event.getPos(), team.id());
    }

    static Optional<Team> foreignNeighbour(ServerLevel level, BlockPos pos, Team team) {
        BlockOwnership ownership = BlockOwnership.get(level);
        for (Direction direction : Direction.values()) {
            Optional<Team> owner = ownership.owner(level, pos.relative(direction));
            if (owner.isPresent() && !owner.get().id().equals(team.id())) {
                return owner;
            }
        }
        return Optional.empty();
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
