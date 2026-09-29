package de.craftorio.machine;

import de.craftorio.CraftorioConfig;
import de.craftorio.energy.EnergyBuffer;
import de.craftorio.energy.Fuel;
import de.craftorio.fluid.FluidBuffer;
import de.craftorio.module.ModuleEffects;
import de.craftorio.module.ModuleState;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModFluids;
import de.craftorio.registry.ModItems;
import de.craftorio.world.OreFieldBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import de.craftorio.menu.DrillMenu;
import de.craftorio.menu.SplitIntData;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DrillBlockEntity extends BlockEntity implements MenuProvider, de.craftorio.module.ModuleHost {
    public static final int FUEL_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int RESCAN_INTERVAL = 40;
    public static final int ACID_CAPACITY = 1_000;
    /** Wiki 1.1: 10 sulfuric acid per 10 uranium ore, and uranium ore is mined at half the rate of other ores. */
    public static final int ACID_PER_ORE = 1;
    public static final int URANIUM_SLOWDOWN = 2;

    private final DrillTier tier;
    private final DrillProduction production;
    private final EnergyBuffer energy;
    private final IItemHandler handler = new Handler();
    /** Sulfuric acid for uranium ore (electric and deep drills only; the burner drill has no fluid input). */
    private final FluidBuffer acid = new FluidBuffer(ACID_CAPACITY, FluidBuffer.only(ModFluids.SULFURIC_ACID.get()), this::setChanged);
    private final IFluidHandler acidInput = new AcidInput();
    private boolean uraniumBelow;
    private final ModuleState modules;
    private ModuleEffects effects = ModuleEffects.NONE;
    private final IItemHandlerModifiable menuSlots = new MenuSlots();
    private ItemStack fuel = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int burnTicks;
    /** Burn time of the fuel item currently burning, for the flame in the GUI. */
    private int burnDuration;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTicks;
                case 1 -> burnDuration;
                case 2 -> fieldBlocks.size();
                case 3 -> tier.area();
                case 4 -> (int) Math.round(production.itemsPerSecond(fieldBlocks.size()) * 100 * effects.speedFactor() / (uraniumBelow ? URANIUM_SLOWDOWN : 1));
                case 5 -> SplitIntData.low(energy.getEnergyStored());
                case 6 -> SplitIntData.high(energy.getEnergyStored());
                case 7 -> SplitIntData.low(tier.usesFuel() ? 0 : energy.getMaxEnergyStored());
                case 8 -> SplitIntData.high(tier.usesFuel() ? 0 : energy.getMaxEnergyStored());
                case 9 -> acid.getFluidAmount();
                case 10 -> uraniumBelow ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DrillMenu.DATA_COUNT;
        }
    };
    private List<BlockPos> fieldBlocks = List.of();
    private int nextField;
    private int rescanIn;

    public DrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRILL.get(), pos, state);
        this.tier = state.getBlock() instanceof DrillBlock drill ? drill.tier() : DrillTier.BURNER;
        this.production = new DrillProduction(tier.itemsPerBlockPerSecond() * CraftorioConfig.miningSpeed());
        this.energy = new EnergyBuffer(Math.max(1, tier.energyPerTick() * 200), tier.energyPerTick() * 5, 0, this::setChanged);
        this.modules = new ModuleState(tier.moduleSlots(), this::setChanged);
    }

    public DrillTier tier() {
        return tier;
    }

    /** Grid buffer of electric tiers; burner drills have none. */
    public @Nullable EnergyBuffer energy() {
        return tier.usesFuel() ? null : energy;
    }

    public ModuleState modules() {
        return modules;
    }

    public FluidBuffer acid() {
        return acid;
    }

    /** Fluid input for the pipes; the burner drill has none. */
    public @Nullable IFluidHandler fluidHandler() {
        return tier.usesFuel() ? null : acidInput;
    }

    private static boolean needsAcid(ItemStack resource) {
        return resource.is(ModItems.RAW_URANIUM.get());
    }

    public static boolean isFuel(ItemStack stack) {
        return Fuel.isFuel(stack);
    }

    public IItemHandler handler() {
        return handler;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DrillBlockEntity drill) {
        drill.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        if (--rescanIn <= 0) {
            rescanIn = RESCAN_INTERVAL;
            fieldBlocks = scanFieldBlocks(level, pos, tier.radius());
            uraniumBelow = fieldBlocks.stream().anyMatch(field -> needsAcid(resourceAt(level, field)));
        }
        pushOutput(level, pos, state.getValue(DrillBlock.FACING));
        effects = modules.effects(level, pos);

        boolean working = false;
        ItemStack next = nextResource(level);
        boolean acidOre = needsAcid(next);
        if (!next.isEmpty() && hasRoomFor(next) && (!acidOre || acid.getFluidAmount() >= ACID_PER_ORE) && powered()) {
            working = true;
            int finished = production.tick(fieldBlocks.size(), effects.speedFactor() / (acidOre ? URANIUM_SLOWDOWN : 1));
            for (int i = 0; i < finished && hasRoomFor(nextResource(level)); i++) {
                ItemStack mined = nextResource(level);
                if (needsAcid(mined)) {
                    if (acid.getFluidAmount() < ACID_PER_ORE) {
                        break;
                    }
                    acid.use(ACID_PER_ORE);
                }
                // Productivity gives extra ore without extra acid.
                int extra = modules.bar().add(effects.productivityBonus());
                addOutput(mined.copyWithCount(Math.min(1 + extra, mined.getMaxStackSize() - (output.isEmpty() ? 0 : output.getCount()))));
                nextField++;
            }
            setChanged();
        }
        if (state.getValue(DrillBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(DrillBlock.LIT, working), 3);
        }
    }

    /** Pays for one tick of work with fuel or grid energy. */
    private boolean powered() {
        if (!tier.usesFuel()) {
            return energy.consume(effects.power(tier.energyPerTick()));
        }
        if (burnTicks > 0 || consumeFuel()) {
            burnTicks--;
            return true;
        }
        return false;
    }

    private static List<BlockPos> scanFieldBlocks(Level level, BlockPos pos, int radius) {
        List<BlockPos> found = new ArrayList<>((2 * radius + 1) * (2 * radius + 1));
        BlockPos center = pos.below();
        for (BlockPos candidate : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
            if (level.getBlockState(candidate).getBlock() instanceof OreFieldBlock) {
                found.add(candidate.immutable());
            }
        }
        return found;
    }

    /** Field blocks are mined round-robin, so mixed areas yield a mix of resources. */
    private ItemStack nextResource(Level level) {
        if (fieldBlocks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return resourceAt(level, fieldBlocks.get(Math.floorMod(nextField, fieldBlocks.size())));
    }

    private static ItemStack resourceAt(Level level, BlockPos fieldPos) {
        return level.getBlockState(fieldPos).getBlock() instanceof OreFieldBlock field ? field.resource() : ItemStack.EMPTY;
    }

    private boolean hasRoomFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, stack) && output.getCount() < output.getMaxStackSize());
    }

    private void addOutput(ItemStack stack) {
        if (output.isEmpty()) {
            output = stack.copy();
        } else {
            output.grow(stack.getCount());
        }
    }

    private boolean consumeFuel() {
        if (!isFuel(fuel)) {
            return false;
        }
        burnTicks = Fuel.burnTicks(fuel, DrillTier.BURNER_KW);
        burnDuration = burnTicks;
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            fuel = remainder;
        }
        return true;
    }

    private void pushOutput(Level level, BlockPos pos, Direction facing) {
        if (output.isEmpty()) {
            return;
        }
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(facing), facing.getOpposite());
        if (target == null) {
            return;
        }
        while (!output.isEmpty() && ItemHandlerHelper.insertItem(target, output.copyWithCount(1), false).isEmpty()) {
            output.shrink(1);
            setChanged();
        }
    }

    public Component status() {
        int blocks = fieldBlocks.size();
        String rate = String.format(Locale.ROOT, "%.2f", production.itemsPerSecond(blocks) / (uraniumBelow ? URANIUM_SLOWDOWN : 1));
        if (!tier.usesFuel()) {
            return Component.translatable("craftorio.drill.status_electric", blocks, tier.area(), rate,
                    energy.getEnergyStored(), tier.energyPerTick());
        }
        int fuelSeconds = (burnTicks + (isFuel(fuel) ? fuel.getCount() * Fuel.burnTicks(fuel, DrillTier.BURNER_KW) : 0)) / 20;
        return Component.translatable("craftorio.drill.status", blocks, tier.area(), rate, fuelSeconds);
    }

    /** Slots of the GUI: unlike the automation handler, players may also take fuel back out. */
    public IItemHandlerModifiable menuSlots() {
        return menuSlots;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DrillMenu(containerId, inventory, this, data);
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), fuel);
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), output);
            modules.dropContents(level, worldPosition);
            fuel = ItemStack.EMPTY;
            output = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fuel", fuel.saveOptional(registries));
        tag.put("output", output.saveOptional(registries));
        tag.putInt("burn_ticks", burnTicks);
        tag.putInt("burn_duration", burnDuration);
        tag.putDouble("progress", production.progress());
        tag.putInt("next_field", nextField);
        tag.putInt("energy", energy.getEnergyStored());
        acid.writeToNBT(registries, tag);
        modules.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = ItemStack.parseOptional(registries, tag.getCompound("fuel"));
        output = ItemStack.parseOptional(registries, tag.getCompound("output"));
        burnTicks = tag.getInt("burn_ticks");
        burnDuration = tag.getInt("burn_duration");
        production.setProgress(tag.getDouble("progress"));
        nextField = tag.getInt("next_field");
        energy.setEnergy(tag.getInt("energy"));
        acid.readFromNBT(registries, tag);
        modules.load(tag, registries);
    }

    /** Takes sulfuric acid, nothing else, and gives nothing back. */
    private final class AcidInput implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return acid.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return ACID_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return acid.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return acid.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    /** Slot 0 accepts fuel only; slot 1 holds mined output and can only be extracted. */
    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == FUEL_SLOT ? fuel : output;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != FUEL_SLOT || !isFuel(stack) || !tier.usesFuel()) {
                return stack;
            }
            if (!fuel.isEmpty() && !ItemStack.isSameItemSameComponents(fuel, stack)) {
                return stack;
            }
            int room = stack.getMaxStackSize() - fuel.getCount();
            int moved = Math.min(room, stack.getCount());
            if (moved <= 0) {
                return stack;
            }
            if (!simulate) {
                if (fuel.isEmpty()) {
                    fuel = stack.copyWithCount(moved);
                } else {
                    fuel.grow(moved);
                }
                setChanged();
            }
            return stack.copyWithCount(stack.getCount() - moved);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != OUTPUT_SLOT || output.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            int moved = Math.min(amount, output.getCount());
            ItemStack extracted = output.copyWithCount(moved);
            if (!simulate) {
                output.shrink(moved);
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FUEL_SLOT && isFuel(stack) && tier.usesFuel();
        }
    }

    private final class MenuSlots implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == FUEL_SLOT ? fuel : output;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot == FUEL_SLOT) {
                fuel = stack;
            } else {
                output = stack;
            }
            setChanged();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != FUEL_SLOT) {
                return handler.extractItem(slot, amount, simulate);
            }
            if (fuel.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack extracted = fuel.copyWithCount(Math.min(amount, fuel.getCount()));
            if (!simulate) {
                fuel.shrink(extracted.getCount());
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return handler.isItemValid(slot, stack);
        }
    }
}
