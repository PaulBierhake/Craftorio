package de.craftorio.economy.block;

import de.craftorio.economy.Economy;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.team.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.Containers;

import java.util.Optional;
import java.util.UUID;

public final class TradingPostBlockEntity extends BlockEntity {
    private UUID owner;
    private long totalEarned;
    private final IItemHandler input = new SellingHandler();
    /** Items put in by hand through the GUI; they are only sold when a player presses "Sell". */
    private final ItemStackHandler counter = new ItemStackHandler(COUNTER_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return Economy.isSellable(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    public static final int COUNTER_SLOTS = 9;

    public TradingPostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRADING_POST.get(), pos, state);
    }

    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public long totalEarned() {
        return totalEarned;
    }

    /** Exposed as the item handler capability on every side. */
    public IItemHandler input() {
        return input;
    }

    public ItemStackHandler counter() {
        return counter;
    }

    /** Sells everything on the counter; returns the credits earned. */
    public long sellCounter() {
        long total = 0;
        for (int slot = 0; slot < counter.getSlots(); slot++) {
            ItemStack stack = counter.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                long earned = sell(stack);
                if (earned > 0) {
                    total += earned;
                    counter.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        }
        return total;
    }

    public void dropContents() {
        if (level != null) {
            for (int slot = 0; slot < counter.getSlots(); slot++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), counter.getStackInSlot(slot));
                counter.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    /** Sells the stack for the owning team; returns the credits earned (0 if nothing was sold). */
    public long sell(ItemStack stack) {
        if (!hasActiveOwner()) {
            return 0;
        }
        long earned = Economy.sell(level.getServer(), owner, stack);
        if (earned > 0) {
            totalEarned += earned;
            setChanged();
        }
        return earned;
    }

    /** False if unowned or the owning team has been dissolved; such posts refuse items until a player claims them. */
    private boolean hasActiveOwner() {
        MinecraftServer server = level == null ? null : level.getServer();
        return server != null && owner != null && TeamData.registry(server).team(owner).isPresent();
    }

    private boolean canSell(ItemStack stack) {
        return Economy.isSellable(stack) && hasActiveOwner();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putLong("total_earned", totalEarned);
        tag.put("counter", counter.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        totalEarned = tag.getLong("total_earned");
        if (tag.contains("counter")) {
            counter.deserializeNBT(registries, tag.getCompound("counter"));
        }
    }

    /** A bottomless single slot: accepted items are sold on insertion, unsellable items are refused. */
    private final class SellingHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !canSell(stack)) {
                return stack;
            }
            if (!simulate && sell(stack) == 0) {
                return stack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canSell(stack);
        }
    }
}
