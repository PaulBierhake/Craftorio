package de.craftorio.menu;

import de.craftorio.blueprint.BlueprintActions;
import de.craftorio.blueprint.TerminalStats;
import de.craftorio.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** No slots; button ids are indexes into {@link de.craftorio.blueprint.Blueprints#sorted}. */
public final class TerminalMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final TerminalStats stats;

    public TerminalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), TerminalStats.STREAM_CODEC.decode(buf));
    }

    public TerminalMenu(int containerId, Inventory inventory, BlockPos pos, TerminalStats stats) {
        super(ModMenus.TERMINAL.get(), containerId);
        this.pos = pos;
        this.stats = stats;
    }

    public TerminalStats stats() {
        return stats;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return player instanceof ServerPlayer serverPlayer && BlueprintActions.unlock(serverPlayer, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getCenter()) <= 64;
    }
}
