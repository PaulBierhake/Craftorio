package de.craftorio.menu;

import de.craftorio.blueprint.BlueprintActions;
import de.craftorio.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** No slots; button id = blueprint index * 2 (+1 to build ten at once). */
public final class WorkbenchMenu extends AbstractContainerMenu {
    public static final int BULK_AMOUNT = 10;

    private final BlockPos pos;
    private final int tier;

    public WorkbenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), buf.readVarInt());
    }

    public WorkbenchMenu(int containerId, Inventory inventory, BlockPos pos, int tier) {
        super(ModMenus.WORKBENCH.get(), containerId);
        this.pos = pos;
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    public static int buttonId(int blueprintIndex, boolean bulk) {
        return blueprintIndex * 2 + (bulk ? 1 : 0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return player instanceof ServerPlayer serverPlayer
                && BlueprintActions.build(serverPlayer, id / 2, tier, id % 2 == 1 ? BULK_AMOUNT : 1);
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
