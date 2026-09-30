package de.craftorio.defense.arena;

import de.craftorio.Craftorio;
import de.craftorio.defense.TowerBlock;
import de.craftorio.defense.TowerDefense;
import de.craftorio.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.Nullable;

/**
 * In the arena dimension players only lay paths and set up towers in their own field; the map itself cannot be
 * changed and no other creatures spawn. Operators in creative mode may do anything.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class ArenaRules {
    private ArenaRules() {
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Arenas.isArena(level)
                || !(event.getEntity() instanceof ServerPlayer player) || bypasses(player)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        String error;
        if (placed.getBlock() instanceof TowerBlock) {
            error = null; // checked when the tower item is used
        } else if (placed.is(ModBlocks.PATH_BLOCK.get())) {
            error = pathError(level, player, event.getPos());
        } else {
            error = "craftorio.arena.error.no_building";
        }
        if (error != null) {
            player.displayClientMessage(Component.translatable(error).withStyle(ChatFormatting.RED), true);
            event.setCanceled(true);
        }
    }

    private static @Nullable String pathError(ServerLevel level, ServerPlayer player, BlockPos pos) {
        String error = TowerDefense.get(level.getServer()).pathEditError(player, pos);
        if (error != null) {
            return error;
        }
        int[] tile = Arenas.tileAt(pos);
        boolean ok = pos.getY() == Arenas.BUILD_Y
                && TowerDefense.get(level.getServer()).layoutAt(Arenas.slotAt(pos)).tile(tile[0], tile[1]).allowsPath();
        if (!ok) {
            return "craftorio.arena.path.blocked";
        }
        return TowerDefense.get(level.getServer()).pathBranches(level, pos) ? "craftorio.arena.path.branch" : null;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Arenas.isArena(level)
                || !(event.getPlayer() instanceof ServerPlayer player) || bypasses(player)) {
            return;
        }
        BlockState state = event.getState();
        boolean removable = state.is(ModBlocks.PATH_BLOCK.get()) || state.getBlock() instanceof TowerBlock;
        String error = removable ? TowerDefense.get(level.getServer()).pathEditError(player, event.getPos()) : "craftorio.arena.error.no_building";
        if (error != null) {
            player.displayClientMessage(Component.translatable(error).withStyle(ChatFormatting.RED), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && Arenas.isArena(level)
                && event.getEntity() instanceof Mob) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level && Arenas.isArena(level)) {
            event.getAffectedBlocks().clear();
        }
    }

    private static boolean bypasses(Player player) {
        return player.isCreative() && player.hasPermissions(2);
    }
}
