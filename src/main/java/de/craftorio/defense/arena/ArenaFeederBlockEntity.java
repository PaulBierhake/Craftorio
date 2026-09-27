package de.craftorio.defense.arena;

import de.craftorio.defense.TowerDefense;
import de.craftorio.energy.EnergyBuffer;
import de.craftorio.protection.BlockOwnership;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Optional;

public final class ArenaFeederBlockEntity extends BlockEntity {
    private static final int TRANSFER_INTERVAL = 20;

    private final EnergyBuffer energy = new EnergyBuffer(40_000, 2_000, 0, this::setChanged);
    private final IItemHandler ammoInput = new AmmoInput();
    private int transferIn;

    public ArenaFeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARENA_FEEDER.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public IItemHandler ammoInput() {
        return ammoInput;
    }

    public static boolean isAmmo(ItemStack stack) {
        return stack.is(ModItems.BOLT.get()) || stack.is(ModItems.CARTRIDGE.get());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArenaFeederBlockEntity feeder) {
        if (--feeder.transferIn > 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        feeder.transferIn = TRANSFER_INTERVAL;
        feeder.team(serverLevel).ifPresent(team -> {
            int sent = TowerDefense.get(serverLevel.getServer()).feedEnergy(team.id(), feeder.energy.getEnergyStored());
            if (sent > 0) {
                feeder.energy.consume(sent);
            }
        });
    }

    private Optional<Team> team(ServerLevel level) {
        return BlockOwnership.get(level).owner(level, worldPosition);
    }

    public Component status() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Component.empty();
        }
        return team(serverLevel)
                .map(team -> TowerDefense.get(serverLevel.getServer()).reserves(team.id()))
                .orElse(Component.translatable("craftorio.arena.feeder.no_owner"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("energy"));
    }

    /** Ammunition goes straight into the team's arena reserve. */
    private final class AmmoInput implements IItemHandler {
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
            if (!isAmmo(stack) || !(level instanceof ServerLevel serverLevel)) {
                return stack;
            }
            Optional<Team> team = team(serverLevel);
            if (team.isEmpty()) {
                return stack;
            }
            int accepted = TowerDefense.get(serverLevel.getServer()).feedAmmo(team.get().id(), stack, simulate);
            return stack.copyWithCount(stack.getCount() - accepted);
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
            return isAmmo(stack);
        }
    }
}
