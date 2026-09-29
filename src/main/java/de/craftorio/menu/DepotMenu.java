package de.craftorio.menu;

import de.craftorio.registry.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;

/** The tower depot: a chest-like window from which items can only be taken. */
public final class DepotMenu extends AbstractContainerMenu {
    public static final int ROWS = 6;
    public static final int SLOTS = ROWS * 9;
    public static final int TAKE_ALL = 0;

    private final Container depot;

    public DepotMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SLOTS));
    }

    public DepotMenu(int containerId, Inventory inventory, Container depot) {
        super(ModMenus.DEPOT.get(), containerId);
        this.depot = depot;
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(depot, column + row * 9, 8 + column * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        int offset = (ROWS - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18 + offset));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 161 + offset));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return depot.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        depot.stopOpen(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != TAKE_ALL) {
            return false;
        }
        int taken = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = depot.getItem(slot);
            if (!stack.isEmpty() && moveItemStackTo(stack, SLOTS, slots.size(), true)) {
                taken++;
                if (stack.isEmpty()) {
                    depot.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
        depot.setChanged();
        if (taken > 0) {
            player.displayClientMessage(Component.translatable("craftorio.arena.depot.taken", taken).withStyle(ChatFormatting.GREEN), true);
        }
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (index >= SLOTS || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
