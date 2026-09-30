package de.craftorio.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Player inventory layout and shift-click handling shared by all Craftorio machine menus. */
public abstract class MachineMenuBase extends AbstractContainerMenu {
    protected final BlockEntity blockEntity;
    private int machineSlots;
    private int moduleSlots;
    /** Left edge and top of the module panel, right of the main GUI (see {@code MachineScreenBase}). */
    public static final int MODULE_X = 181;
    public static final int MODULE_Y = 18;

    protected MachineMenuBase(@Nullable MenuType<?> type, int containerId, BlockEntity blockEntity) {
        super(type, containerId);
        this.blockEntity = blockEntity;
    }

    /** Adds the module slots in a column right of the GUI; call after the machine's own slots and before {@link #addPlayerInventory}. */
    protected void addModuleSlots(net.neoforged.neoforge.items.IItemHandler modules) {
        moduleSlots = modules.getSlots();
        for (int i = 0; i < moduleSlots; i++) {
            addSlot(new net.neoforged.neoforge.items.SlotItemHandler(modules, i, MODULE_X, MODULE_Y + 18 * i));
        }
    }

    /** How many module slots this menu has: the last slots before the player inventory. */
    public int moduleSlotCount() {
        return moduleSlots;
    }

    /** Call after adding the machine's own slots. */
    protected void addPlayerInventory(Inventory inventory) {
        addPlayerInventory(inventory, 84);
    }

    /** The player inventory with its top row at {@code top} (84 in the standard GUI). */
    protected void addPlayerInventory(Inventory inventory, int top) {
        machineSlots = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, top + 58));
        }
    }

    public BlockEntity blockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved() && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }
}
