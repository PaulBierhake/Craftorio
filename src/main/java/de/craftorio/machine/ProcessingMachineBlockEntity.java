package de.craftorio.machine;

import de.craftorio.CraftorioConfig;
import de.craftorio.client.ClientTeamState;
import de.craftorio.energy.EnergyBuffer;
import de.craftorio.energy.Fuel;
import de.craftorio.fluid.FluidBuffer;
import de.craftorio.menu.ProcessingMachineMenu;
import de.craftorio.menu.SplitIntData;
import de.craftorio.module.ModuleEffects;
import de.craftorio.module.ModuleState;
import de.craftorio.module.ProductivityRules;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.protection.BlockOwnership;
import de.craftorio.recipe.MachineRecipeKind;
import de.craftorio.research.Researches;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public final class ProcessingMachineBlockEntity extends BlockEntity implements MenuProvider, MachineBaseBlock.DropsContents, de.craftorio.module.ModuleHost {
    public static final int FLUID_CAPACITY = 1_000;
    private final MachineType type;
    private final EnergyBuffer energy = new EnergyBuffer(MachineType.ENERGY_CAPACITY, MachineType.MAX_INPUT, 0, this::setChanged);
    private final ItemStackHandler items;
    private final IItemHandler automation;
    private int progress;
    private int recipeTime;
    private int burnTicks;
    private int burnTotal;
    private @Nullable ResourceLocation selectedRecipe;
    /** Fluid ingredient tank of the machines that have a fluid input (see {@link MachineType#fluidInput()}). */
    private final FluidBuffer fluid = new FluidBuffer(FLUID_CAPACITY, this::setChanged);
    private final IFluidHandler fluidHandler = new FluidInput();
    /** Module slots of the machines that have them (see {@link MachineType#moduleSlots()}). */
    private final ModuleState modules;
    /** A productivity module is in a recipe that may not use one: the machine stops (shown in the GUI). */
    private boolean productivityBlocked;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> SplitIntData.low(energy.getEnergyStored());
                case 1 -> SplitIntData.high(energy.getEnergyStored());
                case 2 -> progress;
                case 3 -> recipeTime;
                case 4 -> selectedRecipeIndex();
                case 5 -> burnTicks;
                case 6 -> burnTotal;
                case 7 -> fluid.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluid.getFluid().getFluid());
                case 8 -> fluid.getFluidAmount();
                case 9 -> productivityBlocked ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return ProcessingMachineMenu.DATA_COUNT;
        }
    };

    public ProcessingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE.get(), pos, state);
        this.type = ((ProcessingMachineBlock) state.getBlock()).machineType();
        this.modules = new ModuleState(type.moduleSlots(), this::setChanged);
        this.items = new ItemStackHandler(type.slotCount()) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (type.usesFuel() && slot == type.fuelSlot()) {
                    return Fuel.isFuel(stack);
                }
                return slot < type.inputSlots() && accepts(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.automation = new AutomationHandler();
    }

    public MachineType type() {
        return type;
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidBuffer fluid() {
        return fluid;
    }

    public ModuleState modules() {
        return modules;
    }

    /** Module and beacon effects on this machine right now. */
    public ModuleEffects moduleEffects() {
        return level == null ? ModuleEffects.NONE : modules.effects(level, worldPosition);
    }

    /** The fluid input for the pipes; null for machines without one. */
    public @Nullable IFluidHandler fluidHandler() {
        return type.fluidInput() ? fluidHandler : null;
    }

    /** Ticks the current recipe takes with the modules in place (0 before the first run). */
    public int recipeTime() {
        return recipeTime;
    }

    public boolean productivityBlocked() {
        return productivityBlocked;
    }

    /** Progress of the current job in percent, or -1 if the machine is idle. */
    public int progressPercent() {
        return recipeTime <= 0 || progress == 0 ? -1 : 100 * progress / recipeTime;
    }

    /** Inputs can be inserted, the output extracted; nothing else. */
    public IItemHandler automation() {
        return automation;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ProcessingMachineBlockEntity machine) {
        machine.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        Job job = findJob(level);
        boolean working = false;
        ModuleEffects effects = modules.effects(level, pos);
        productivityBlocked = job != null && modules.hasProductivity() && !ProductivityRules.allowed(job.result());
        if (job == null || productivityBlocked) {
            progress = 0;
        } else if (canOutput(job.result()) && drawPower(effects)) {
            working = true;
            recipeTime = CraftorioConfig.craftingTicks(Math.max(1, (int) Math.round(job.time() / (type.speed() * effects.speedFactor()))));
            if (++progress >= recipeTime) {
                progress = 0;
                consume(job.ingredients());
                job.fluids().forEach(stack -> fluid.use(stack.getAmount()));
                int extra = modules.bar().add(effects.productivityBonus());
                ItemStack output = items.getStackInSlot(type.outputSlot());
                int made = job.result().getCount() * (1 + extra);
                if (output.isEmpty()) {
                    items.setStackInSlot(type.outputSlot(), job.result().copyWithCount(Math.min(made, job.result().getMaxStackSize())));
                } else {
                    output.grow(Math.min(made, output.getMaxStackSize() - output.getCount()));
                    items.setStackInSlot(type.outputSlot(), output);
                }
            }
            setChanged();
        }
        pushOutput(level, pos, state.getValue(MachineBaseBlock.FACING));
        MachineBaseBlock.setActive(level, pos, state, working);
    }

    /** Hands finished products to whatever stands in front of the machine (belt, chest, next machine). */
    private void pushOutput(Level level, BlockPos pos, net.minecraft.core.Direction facing) {
        ItemStack output = items.getStackInSlot(type.outputSlot());
        if (output.isEmpty() || !(level instanceof ServerLevel server)) {
            return;
        }
        BlockPos front = pos.relative(facing);
        if (!BlockOwnership.sameOwner(server, pos, front)) {
            return;
        }
        IItemHandler target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, front, facing.getOpposite());
        if (target == null) {
            return;
        }
        while (!items.getStackInSlot(type.outputSlot()).isEmpty()
                && net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(target, items.extractItem(type.outputSlot(), 1, true), false).isEmpty()) {
            items.extractItem(type.outputSlot(), 1, false);
        }
    }

    /** Pays for one tick of work with fuel or grid power. */
    private boolean drawPower(ModuleEffects effects) {
        if (!type.usesFuel()) {
            return energy.consume(effects.power(type.energyPerTick()));
        }
        if (burnTicks <= 0) {
            ItemStack fuel = items.getStackInSlot(type.fuelSlot());
            int ticks = Fuel.burnTicks(fuel, type.energyPerTick());
            if (ticks <= 0) {
                return false;
            }
            burnTicks = burnTotal = ticks;
            ItemStack remainder = fuel.getCraftingRemainingItem();
            fuel.shrink(1);
            items.setStackInSlot(type.fuelSlot(), fuel.isEmpty() ? remainder : fuel);
        }
        burnTicks--;
        return true;
    }

    private record Job(List<SizedIngredient> ingredients, List<FluidStack> fluids, ItemStack result, int time) {
    }

    private @Nullable Job findJob(Level level) {
        RecipeInput input = inputs();
        return switch (type) {
            case ELECTRIC_FURNACE, STONE_FURNACE, STEEL_FURNACE -> {
                ItemStack stack = items.getStackInSlot(0);
                if (stack.isEmpty()) {
                    yield null;
                }
                SingleRecipeInput single = new SingleRecipeInput(stack);
                Job smelting = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, single, level)
                        .map(recipe -> new Job(List.of(new SizedIngredient(Ingredient.of(stack.getItem()), 1)), List.of(),
                                recipe.value().assemble(single, level.registryAccess()), MachineType.smeltingTicks(recipe.value().getCookingTime())))
                        .orElse(null);
                if (smelting != null) {
                    yield smelting;
                }
                yield level.getRecipeManager().getAllRecipesFor(MachineRecipeKind.SMELTING.type()).stream()
                        .filter(recipe -> knows(level, recipe.id()) && recipe.value().matches(input, level))
                        .findFirst()
                        .map(recipe -> new Job(recipe.value().ingredients(), List.of(), recipe.value().result(), recipe.value().time()))
                        .orElse(null);
            }
            case ASSEMBLER, ASSEMBLER_2, ASSEMBLER_3 -> {
                MachineRecipe recipe = selectedAssemblerRecipe(level);
                if (recipe == null || !recipe.matches(input, level) || (recipe.needsFluid() && !fluidAvailable(recipe.fluids()))) {
                    yield null;
                }
                yield new Job(recipe.ingredients(), recipe.fluids(), recipe.result(), recipe.time());
            }
        };
    }

    private boolean fluidAvailable(List<FluidStack> needed) {
        if (!type.fluidInput()) {
            return false;
        }
        for (FluidStack stack : needed) {
            if (!FluidStack.isSameFluid(fluid.getFluid(), stack) || fluid.getFluidAmount() < stack.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private RecipeInput inputs() {
        return new RecipeInput() {
            @Override
            public ItemStack getItem(int index) {
                return items.getStackInSlot(index);
            }

            @Override
            public int size() {
                return type.inputSlots();
            }
        };
    }

    private boolean canOutput(ItemStack result) {
        ItemStack output = items.getStackInSlot(type.outputSlot());
        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    private void consume(List<SizedIngredient> ingredients) {
        for (SizedIngredient ingredient : ingredients) {
            int remaining = ingredient.count();
            for (int slot = 0; slot < type.inputSlots() && remaining > 0; slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                if (ingredient.ingredient().test(stack)) {
                    int used = Math.min(remaining, stack.getCount());
                    items.extractItem(slot, used, false);
                    remaining -= used;
                }
            }
        }
    }

    /** Which items an input slot takes; the assembler dedicates slot n to ingredient n of its recipe. */
    private boolean accepts(int slot, ItemStack stack) {
        if (level == null) {
            return true;
        }
        return switch (type) {
            case ELECTRIC_FURNACE, STONE_FURNACE, STEEL_FURNACE -> level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent()
                    || level.getRecipeManager().getAllRecipesFor(MachineRecipeKind.SMELTING.type()).stream()
                    .filter(recipe -> knows(level, recipe.id()))
                    .anyMatch(recipe -> recipe.value().ingredients().stream().anyMatch(ingredient -> ingredient.ingredient().test(stack)));
            case ASSEMBLER, ASSEMBLER_2, ASSEMBLER_3 -> {
                MachineRecipe recipe = selectedAssemblerRecipe(level);
                yield recipe != null && slot < recipe.ingredients().size() && recipe.ingredients().get(slot).ingredient().test(stack);
            }
        };
    }

    /** All assembler recipes in a stable order shared by server and client. */
    public static List<RecipeHolder<MachineRecipe>> assemblerRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(MachineRecipeKind.ASSEMBLING.type()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    private @Nullable MachineRecipe selectedAssemblerRecipe(Level level) {
        if (selectedRecipe == null) {
            return null;
        }
        return level.getRecipeManager().byKey(selectedRecipe)
                .filter(holder -> knows(level, holder.id()))
                .map(RecipeHolder::value)
                .filter(recipe -> recipe instanceof MachineRecipe machine && machine.kind() == MachineRecipeKind.ASSEMBLING)
                .map(MachineRecipe.class::cast)
                .orElse(null);
    }

    private int selectedRecipeIndex() {
        if (level == null || selectedRecipe == null) {
            return -1;
        }
        List<RecipeHolder<MachineRecipe>> recipes = assemblerRecipes(level);
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).id().equals(selectedRecipe)) {
                return i;
            }
        }
        return -1;
    }

    /** Steps through the assembler recipes (from the GUI's arrow buttons). */
    public void cycleRecipe(int direction) {
        if (level == null || !type.assembling()) {
            return;
        }
        List<RecipeHolder<MachineRecipe>> recipes = assemblerRecipes(level);
        if (recipes.isEmpty()) {
            return;
        }
        int index = Math.floorMod(selectedRecipeIndex() + direction, recipes.size());
        if (selectedRecipeIndex() < 0 && direction < 0) {
            index = recipes.size() - 1;
        }
        // Skip recipes the owning team has not researched yet and fluid recipes this machine cannot do.
        for (int tried = 0; tried < recipes.size() && !usable(recipes.get(index)); tried++) {
            index = Math.floorMod(index + direction, recipes.size());
        }
        if (usable(recipes.get(index))) {
            setSelectedRecipe(recipes.get(index).id());
        }
    }

    private boolean usable(RecipeHolder<MachineRecipe> recipe) {
        return knows(level, recipe.id()) && (!recipe.value().needsFluid() || type.fluidInput());
    }

    /**
     * May this machine use the recipe? Recipes that no research unlocks are open to everybody; otherwise the team
     * owning the machine needs the research. Machines without an owner (e.g. in tests) know every recipe.
     */
    private boolean knows(Level level, ResourceLocation recipe) {
        if (level.isClientSide) {
            return Researches.knows(level.registryAccess(), ClientTeamState.researched(), recipe.toString());
        }
        return BlockOwnership.get((ServerLevel) level).owner((ServerLevel) level, worldPosition)
                .map(team -> Researches.knows(level.registryAccess(), team.researched(), recipe.toString())).orElse(true);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation id) {
        selectedRecipe = id;
        progress = 0;
        // A fluid that the new recipe does not use would block the tank.
        MachineRecipe recipe = level == null ? null : selectedAssemblerRecipe(level);
        if (!fluid.isEmpty() && (recipe == null || !recipe.needsFluid() || !FluidStack.isSameFluid(recipe.fluids().get(0), fluid.getFluid()))) {
            fluid.setFluid(FluidStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ProcessingMachineMenu(containerId, inventory, this, data);
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
        tag.putInt("burn_ticks", burnTicks);
        tag.putInt("burn_total", burnTotal);
        tag.putInt("progress", progress);
        fluid.writeToNBT(registries, tag);
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
        burnTicks = tag.getInt("burn_ticks");
        burnTotal = tag.getInt("burn_total");
        fluid.readFromNBT(registries, tag);
        modules.load(tag, registries);
        selectedRecipe = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
    }

    /** Takes the fluid the selected recipe needs, up to the tank's capacity; nothing can be drained. */
    private final class FluidInput implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluid.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return FLUID_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            MachineRecipe recipe = level == null ? null : selectedAssemblerRecipe(level);
            return recipe != null && recipe.needsFluid() && FluidStack.isSameFluid(recipe.fluids().get(0), stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return isFluidValid(0, resource) ? fluid.fill(resource, action) : 0;
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

    private final class AutomationHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            boolean fuelSlot = type.usesFuel() && slot == type.fuelSlot();
            return slot < type.inputSlots() || fuelSlot ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == type.outputSlot() ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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
}
