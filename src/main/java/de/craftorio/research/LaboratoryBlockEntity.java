package de.craftorio.research;

import de.craftorio.CraftorioConfig;
import de.craftorio.energy.EnergyBuffer;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.menu.LaboratoryMenu;
import de.craftorio.menu.SplitIntData;
import de.craftorio.protection.BlockOwnership;
import de.craftorio.module.ModuleEffects;
import de.craftorio.module.ModuleState;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModRegistries;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * One slot per science pack kind. To start a unit the lab takes one pack of every kind the team's active research
 * needs, then works for the research's time per unit at {@link #POWER} kW. Finished units count for the team, so any
 * number of labs speed a research up.
 */
public final class LaboratoryBlockEntity extends BlockEntity implements MenuProvider, MachineBaseBlock.DropsContents, de.craftorio.module.ModuleHost {
    /** 60 kW like Factorio's lab. */
    public static final int POWER = 60;
    public static final int ENERGY_CAPACITY = 6_000;
    public static final int MAX_INPUT = 500;

    /** What the lab is doing, for the GUI. */
    public static final int IDLE = 0;
    public static final int NO_TEAM = 1;
    public static final int NO_RESEARCH = 2;
    public static final int MISSING_PACKS = 3;
    public static final int WORKING = 4;
    public static final int NO_POWER = 5;

    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_CAPACITY, MAX_INPUT, 0, this::setChanged);
    private final ItemStackHandler packs = new ItemStackHandler(Research.Pack.values().length) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Research.Pack.values()[slot].item());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return packs.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return packs.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return packs.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return packs.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return packs.isItemValid(slot, stack);
        }
    };

    public static final int MODULE_SLOTS = 2;
    private final ModuleState modules = new ModuleState(MODULE_SLOTS, this::setChanged);
    private ModuleEffects effects = ModuleEffects.NONE;
    private int ticksLeft;
    private int ticksTotal;
    private @Nullable String unitResearch;
    private int status = IDLE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SplitIntData.low(energy.getEnergyStored());
                case 1 -> SplitIntData.high(energy.getEnergyStored());
                case 2 -> ticksLeft;
                case 3 -> ticksTotal;
                case 4 -> status;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return LaboratoryMenu.DATA_COUNT;
        }
    };

    public LaboratoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LABORATORY.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public ModuleState modules() {
        return modules;
    }

    public ItemStackHandler packs() {
        return packs;
    }

    public IItemHandler automation() {
        return automation;
    }

    public int status() {
        return status;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LaboratoryBlockEntity lab) {
        lab.tick((ServerLevel) level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        Optional<Team> owner = BlockOwnership.get(level).owner(level, pos);
        effects = modules.effects(level, pos);
        int before = status;
        boolean working = false;
        if (owner.isEmpty()) {
            status = NO_TEAM;
        } else if (ticksLeft > 0) {
            if (energy.consume(effects.power(POWER))) {
                working = true;
                status = WORKING;
                if (--ticksLeft == 0) {
                    finishUnit(level, owner.get());
                }
                setChanged();
            } else {
                status = NO_POWER;
            }
        } else {
            status = startUnit(level, owner.get());
        }
        if (working != state.getValue(MachineBaseBlock.ACTIVE)) {
            MachineBaseBlock.setActive(level, pos, state, working);
        }
        if (before != status) {
            setChanged();
        }
    }

    /** Takes one pack of every needed kind and begins a unit; returns the new status. */
    private int startUnit(ServerLevel level, Team team) {
        String active = team.activeResearch();
        if (active == null) {
            return NO_RESEARCH;
        }
        Research research = level.registryAccess().registryOrThrow(ModRegistries.RESEARCH).get(ResourceLocation.parse(active));
        if (research == null) {
            return NO_RESEARCH;
        }
        for (Research.Pack pack : research.packs()) {
            if (packs.getStackInSlot(pack.ordinal()).isEmpty()) {
                return MISSING_PACKS;
            }
        }
        for (Research.Pack pack : research.packs()) {
            packs.extractItem(pack.ordinal(), 1, false);
        }
        unitResearch = active;
        ticksTotal = ticksLeft = CraftorioConfig.craftingTicks(effects.ticks(research.seconds() * 20));
        return WORKING;
    }

    private void finishUnit(ServerLevel level, Team team) {
        String research = unitResearch;
        unitResearch = null;
        ticksTotal = 0;
        if (research == null) {
            return;
        }
        Research data = level.registryAccess().registryOrThrow(ModRegistries.RESEARCH).get(ResourceLocation.parse(research));
        if (data == null) {
            return;
        }
        // Productivity modules make a unit count for more (Factorio: research productivity).
        int units = 1 + modules.bar().add(effects.productivityBonus());
        boolean done = TeamData.registry(level.getServer()).addResearchUnits(team.id(), research, units, ResearchActions.units(data));
        if (done) {
            Component name = Component.translatable("craftorio.research." + ResourceLocation.parse(research).getPath());
            for (java.util.UUID member : team.members()) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(member);
                if (player != null) {
                    player.displayClientMessage(Component.translatable("craftorio.research.finished", name).withStyle(ChatFormatting.GREEN), false);
                }
            }
        }
    }

    @Override
    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < packs.getSlots(); slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), packs.getStackInSlot(slot));
            packs.setStackInSlot(slot, ItemStack.EMPTY);
        }
        modules.dropContents(level, worldPosition);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LaboratoryMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("packs", packs.serializeNBT(registries));
        modules.save(tag, registries);
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("ticks_left", ticksLeft);
        tag.putInt("ticks_total", ticksTotal);
        if (unitResearch != null) {
            tag.putString("unit_research", unitResearch);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        packs.deserializeNBT(registries, tag.getCompound("packs"));
        if (packs.getSlots() < Research.Pack.values().length) { // saved before the production and utility packs existed
            java.util.List<ItemStack> saved = new java.util.ArrayList<>();
            for (int slot = 0; slot < packs.getSlots(); slot++) {
                saved.add(packs.getStackInSlot(slot));
            }
            packs.setSize(Research.Pack.values().length);
            for (int slot = 0; slot < saved.size(); slot++) {
                packs.setStackInSlot(slot, saved.get(slot));
            }
        }
        modules.load(tag, registries);
        energy.setEnergy(tag.getInt("energy"));
        ticksLeft = tag.getInt("ticks_left");
        ticksTotal = tag.getInt("ticks_total");
        unitResearch = tag.contains("unit_research") ? tag.getString("unit_research") : null;
    }
}
