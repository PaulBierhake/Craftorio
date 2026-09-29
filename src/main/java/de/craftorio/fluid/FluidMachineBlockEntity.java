package de.craftorio.fluid;

import de.craftorio.CraftorioConfig;
import de.craftorio.energy.EnergyBuffer;
import de.craftorio.machine.MachineBaseBlock;
import de.craftorio.machine.MachineRules;
import de.craftorio.menu.FluidMachineMenu;
import de.craftorio.menu.SplitIntData;
import de.craftorio.module.ModuleEffects;
import de.craftorio.module.ModuleState;
import de.craftorio.module.ProductivityRules;
import de.craftorio.protection.BlockOwnership;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Crafts the selected {@link FluidRecipes.Recipe}: item ingredients in slot n, fluid ingredients in input tank n,
 * products in the output slot or the output tank (which is pushed into the pipes around the machine).
 */
public final class FluidMachineBlockEntity extends BlockEntity implements MenuProvider, MachineBaseBlock.DropsContents, de.craftorio.module.ModuleHost {
    private static final int FIRST_OUTPUT = FluidMachineType.INPUT_TANKS;
    private static final int PUSH = 60;

    private final FluidMachineType type;
    private final EnergyBuffer energy = new EnergyBuffer(20_000, 2_000, 0, this::setChanged);
    private final ItemStackHandler items = new ItemStackHandler(FluidMachineType.ITEM_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            FluidRecipes.Recipe recipe = selected();
            return slot < FluidMachineType.INPUT_SLOTS && recipe != null && slot < recipe.itemsIn().size()
                    && stack.is(recipe.itemsIn().get(slot).getItem());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidBuffer[] tanks = new FluidBuffer[FluidMachineType.INPUT_TANKS + FluidMachineType.OUTPUT_TANKS];
    private final IItemHandler itemAutomation = new ItemAutomation();
    private final IFluidHandler fluids = new Fluids();
    private final ModuleState modules;
    private boolean productivityBlocked;
    private int progress;
    private int recipeTime;
    private @Nullable ResourceLocation selectedRecipe;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SplitIntData.low(energy.getEnergyStored());
                case 1 -> SplitIntData.high(energy.getEnergyStored());
                case 2 -> progress;
                case 3 -> recipeTime;
                case 4 -> selectedIndex();
                case FluidMachineMenu.DATA_COUNT - 1 -> productivityBlocked ? 1 : 0;
                default -> {
                    int tank = (index - FluidMachineMenu.TANK_DATA) / 2;
                    if (tank < 0 || tank >= tanks.length) {
                        yield 0;
                    }
                    FluidStack stack = tanks[tank].getFluid();
                    yield (index - FluidMachineMenu.TANK_DATA) % 2 == 0
                            ? (stack.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(stack.getFluid())) : stack.getAmount();
                }
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return FluidMachineMenu.DATA_COUNT;
        }
    };

    public FluidMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_MACHINE.get(), pos, state);
        this.type = ((FluidMachineBlock) state.getBlock()).machineType();
        this.modules = new ModuleState(type.moduleSlots(), this::setChanged);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = new FluidBuffer(FluidMachineType.TANK_CAPACITY, this::setChanged);
        }
    }

    public FluidMachineType type() {
        return type;
    }

    public ModuleState modules() {
        return modules;
    }

    public ModuleEffects moduleEffects() {
        return level == null ? ModuleEffects.NONE : modules.effects(level, worldPosition);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidBuffer tank(int index) {
        return tanks[index];
    }

    public IItemHandler itemAutomation() {
        return itemAutomation;
    }

    public IFluidHandler fluids() {
        return fluids;
    }

    public @Nullable FluidRecipes.Recipe selected() {
        if (selectedRecipe == null) {
            return null;
        }
        return FluidRecipes.byId(selectedRecipe).filter(recipe -> recipe.machine() == type).orElse(null);
    }

    private int selectedIndex() {
        FluidRecipes.Recipe recipe = selected();
        return recipe == null ? -1 : FluidRecipes.forMachine(type).indexOf(recipe);
    }

    public void select(@Nullable ResourceLocation id) {
        selectedRecipe = id;
        progress = 0;
        FluidRecipes.Recipe recipe = selected();
        // Fluids the new recipe does not use would block its tanks: they are let go.
        for (int i = 0; i < tanks.length; i++) {
            boolean input = i < FIRST_OUTPUT;
            int index = input ? i : i - FIRST_OUTPUT;
            List<FluidStack> list = recipe == null ? List.of() : input ? recipe.fluidsIn() : recipe.fluidsOut();
            FluidStack wanted = index < list.size() ? list.get(index) : FluidStack.EMPTY;
            if (!tanks[i].isEmpty() && (wanted.isEmpty() || !FluidStack.isSameFluid(wanted, tanks[i].getFluid()))) {
                tanks[i].setFluid(FluidStack.EMPTY);
            }
        }
        setChanged();
    }

    /** Steps through the recipes of this machine that the owning team may use (from the GUI's arrow buttons). */
    public void cycle(int direction) {
        List<FluidRecipes.Recipe> recipes = FluidRecipes.forMachine(type);
        if (level == null || recipes.isEmpty()) {
            return;
        }
        int index = selectedIndex();
        if (index < 0 && direction < 0) {
            index = 0;
        }
        for (int tried = 0; tried < recipes.size(); tried++) {
            index = Math.floorMod(index + direction, recipes.size());
            if (MachineRules.knows(level, worldPosition, recipes.get(index).id())) {
                select(recipes.get(index).id());
                return;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidMachineBlockEntity machine) {
        machine.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        FluidRecipes.Recipe recipe = selected();
        boolean working = false;
        ModuleEffects effects = modules.effects(level, pos);
        productivityBlocked = recipe != null && modules.hasProductivity() && !ProductivityRules.allowed(recipe.itemsOut().stream().map(FluidRecipes.ItemOutput::stack).toList());
        if (recipe != null && !productivityBlocked && MachineRules.knows(level, pos, recipe.id()) && canWork(recipe) && energy.consume(effects.power(type.power()))) {
            working = true;
            recipeTime = CraftorioConfig.craftingTicks(effects.ticks(recipe.ticks()));
            if (++progress >= recipeTime) {
                progress = 0;
                finish(recipe, 1 + modules.bar().add(effects.productivityBonus()));
            }
            setChanged();
        } else if (recipe == null || productivityBlocked || !canWork(recipe)) {
            progress = 0;
        }
        for (int out = FIRST_OUTPUT; out < tanks.length; out++) {
            if (tanks[out].isEmpty()) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                IFluidHandler neighbour = FluidHelper.neighbour(level, pos, direction);
                if (neighbour != null) {
                    FluidHelper.push(tanks[out], neighbour, PUSH);
                }
            }
        }
        pushItems(level, pos, state.getValue(MachineBaseBlock.FACING));
        MachineBaseBlock.setActive(level, pos, state, working);
    }

    private boolean canWork(FluidRecipes.Recipe recipe) {
        for (int i = 0; i < recipe.itemsIn().size(); i++) {
            ItemStack need = recipe.itemsIn().get(i);
            ItemStack have = items.getStackInSlot(i);
            if (!have.is(need.getItem()) || have.getCount() < need.getCount()) {
                return false;
            }
        }
        for (int i = 0; i < recipe.fluidsIn().size(); i++) {
            FluidStack need = recipe.fluidsIn().get(i);
            if (tanks[i].getFluidAmount() < need.getAmount() || !FluidStack.isSameFluid(tanks[i].getFluid(), need)) {
                return false;
            }
        }
        for (FluidRecipes.ItemOutput output : recipe.itemsOut()) {
            ItemStack out = output.stack();
            ItemStack have = items.getStackInSlot(FluidMachineType.OUTPUT_SLOT + output.slot());
            if (!have.isEmpty() && (!ItemStack.isSameItemSameComponents(have, out) || have.getCount() + out.getCount() > have.getMaxStackSize())) {
                return false;
            }
        }
        for (int i = 0; i < recipe.fluidsOut().size(); i++) {
            FluidStack made = recipe.fluidsOut().get(i);
            FluidBuffer tank = tanks[FIRST_OUTPUT + i];
            if (tank.space() < made.getAmount() || (!tank.isEmpty() && !FluidStack.isSameFluid(tank.getFluid(), made))) {
                return false;
            }
        }
        return true;
    }

    /** Finishes a run; {@code batches} is 2 or more when the productivity bar gave extra products. */
    private void finish(FluidRecipes.Recipe recipe, int batches) {
        for (int i = recipe.keepsFirst() ? 1 : 0; i < recipe.itemsIn().size(); i++) {
            items.extractItem(i, recipe.itemsIn().get(i).getCount(), false);
        }
        for (int i = 0; i < recipe.fluidsIn().size(); i++) {
            tanks[i].use(recipe.fluidsIn().get(i).getAmount());
        }
        for (int batch = 0; batch < batches; batch++) {
            List<ItemStack> made = recipe.rollOutputs(level == null ? RandomSource.create() : level.random);
            for (int i = 0; i < made.size(); i++) {
                if (made.get(i).isEmpty()) {
                    continue;
                }
                int slot = FluidMachineType.OUTPUT_SLOT + recipe.itemsOut().get(i).slot();
                ItemStack have = items.getStackInSlot(slot);
                if (have.isEmpty()) {
                    items.setStackInSlot(slot, made.get(i));
                } else {
                    have.grow(Math.min(made.get(i).getCount(), have.getMaxStackSize() - have.getCount()));
                    items.setStackInSlot(slot, have);
                }
            }
            for (int i = 0; i < recipe.fluidsOut().size(); i++) {
                tanks[FIRST_OUTPUT + i].fill(recipe.fluidsOut().get(i).copy(), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /** Hands finished items to whatever stands in front of the machine (belt, chest, next machine). */
    private void pushItems(Level level, BlockPos pos, Direction facing) {
        if (!(level instanceof ServerLevel server) || outputEmpty()) {
            return;
        }
        BlockPos front = pos.relative(facing);
        if (!BlockOwnership.sameOwner(server, pos, front)) {
            return;
        }
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, front, facing.getOpposite());
        if (target == null) {
            return;
        }
        for (int slot = FluidMachineType.OUTPUT_SLOT; slot < FluidMachineType.OUTPUT_SLOT + type.outputSlots(); slot++) {
            while (!items.getStackInSlot(slot).isEmpty()
                    && ItemHandlerHelper.insertItem(target, items.extractItem(slot, 1, true), false).isEmpty()) {
                items.extractItem(slot, 1, false);
            }
        }
    }

    private boolean outputEmpty() {
        for (int slot = FluidMachineType.OUTPUT_SLOT; slot < FluidMachineType.OUTPUT_SLOT + type.outputSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FluidMachineMenu(containerId, inventory, this, data);
    }

    @Override
    public void dropContents() {
        if (level != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
            }
            modules.dropContents(level, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("progress", progress);
        for (int i = 0; i < tanks.length; i++) {
            tag.put("tank" + i, tanks[i].writeToNBT(registries, new CompoundTag()));
        }
        modules.save(tag, registries);
        if (selectedRecipe != null) {
            tag.putString("recipe", selectedRecipe.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        energy.setEnergy(tag.getInt("energy"));
        progress = tag.getInt("progress");
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(registries, tag.getCompound("tank" + i));
        }
        modules.load(tag, registries);
        selectedRecipe = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
    }

    /** Ingredients can be inserted, the product extracted; nothing else. */
    private final class ItemAutomation implements IItemHandler {
        @Override
        public int getSlots() {
            return FluidMachineType.INPUT_SLOTS + type.outputSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < FluidMachineType.INPUT_SLOTS ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= FluidMachineType.OUTPUT_SLOT && slot < FluidMachineType.OUTPUT_SLOT + type.outputSlots()
                    ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    }

    /** Input tank n takes the recipe's fluid n; the output tank can only be emptied. */
    private final class Fluids implements IFluidHandler {
        @Override
        public int getTanks() {
            return tanks.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tanks[tank].getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return FluidMachineType.TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            FluidRecipes.Recipe recipe = selected();
            return tank < FluidMachineType.INPUT_TANKS && recipe != null && tank < recipe.fluidsIn().size()
                    && FluidStack.isSameFluid(recipe.fluidsIn().get(tank), stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            for (int i = 0; i < FluidMachineType.INPUT_TANKS; i++) {
                if (isFluidValid(i, resource)) {
                    return tanks[i].fill(resource, action);
                }
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            for (int i = FIRST_OUTPUT; i < tanks.length; i++) {
                if (FluidStack.isSameFluid(tanks[i].getFluid(), resource)) {
                    return tanks[i].drain(resource, action);
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            for (int i = FIRST_OUTPUT; i < tanks.length; i++) {
                if (!tanks[i].isEmpty()) {
                    return tanks[i].drain(maxDrain, action);
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
