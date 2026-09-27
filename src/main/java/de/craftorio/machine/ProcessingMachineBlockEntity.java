package de.craftorio.machine;

import de.craftorio.energy.EnergyBuffer;
import de.craftorio.menu.ProcessingMachineMenu;
import de.craftorio.menu.SplitIntData;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.recipe.MachineRecipeKind;
import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public final class ProcessingMachineBlockEntity extends BlockEntity implements MenuProvider, MachineBaseBlock.DropsContents {
    private final MachineType type;
    private final EnergyBuffer energy = new EnergyBuffer(MachineType.ENERGY_CAPACITY, MachineType.MAX_INPUT, 0, this::setChanged);
    private final ItemStackHandler items;
    private final IItemHandler automation;
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
                case 4 -> selectedRecipeIndex();
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
        this.items = new ItemStackHandler(type.inputSlots() + 1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
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
        if (job == null) {
            progress = 0;
        } else if (canOutput(job.result()) && energy.consume(type.energyPerTick())) {
            working = true;
            recipeTime = job.time();
            if (++progress >= job.time()) {
                progress = 0;
                consume(job.ingredients());
                ItemStack output = items.getStackInSlot(type.outputSlot());
                if (output.isEmpty()) {
                    items.setStackInSlot(type.outputSlot(), job.result().copy());
                } else {
                    output.grow(job.result().getCount());
                    items.setStackInSlot(type.outputSlot(), output);
                }
            }
            setChanged();
        }
        MachineBaseBlock.setActive(level, pos, state, working);
    }

    private record Job(List<SizedIngredient> ingredients, ItemStack result, int time) {
    }

    private @Nullable Job findJob(Level level) {
        RecipeInput input = inputs();
        return switch (type) {
            case ELECTRIC_FURNACE -> {
                ItemStack stack = items.getStackInSlot(0);
                if (stack.isEmpty()) {
                    yield null;
                }
                SingleRecipeInput single = new SingleRecipeInput(stack);
                yield level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, single, level)
                        .map(recipe -> new Job(List.of(new SizedIngredient(Ingredient.of(stack.getItem()), 1)),
                                recipe.value().assemble(single, level.registryAccess()), MachineType.SMELTING_TIME))
                        .orElse(null);
            }
            case PRESS -> level.getRecipeManager().getRecipeFor(MachineRecipeKind.PRESSING.type(), input, level)
                    .map(recipe -> new Job(recipe.value().ingredients(), recipe.value().result(), recipe.value().time()))
                    .orElse(null);
            case ASSEMBLER -> {
                MachineRecipe recipe = selectedAssemblerRecipe(level);
                yield recipe != null && recipe.matches(input, level) ? new Job(recipe.ingredients(), recipe.result(), recipe.time()) : null;
            }
        };
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
            case ELECTRIC_FURNACE -> level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent();
            case PRESS -> level.getRecipeManager().getAllRecipesFor(MachineRecipeKind.PRESSING.type()).stream()
                    .anyMatch(recipe -> recipe.value().ingredients().stream().anyMatch(ingredient -> ingredient.ingredient().test(stack)));
            case ASSEMBLER -> {
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
        if (level == null || type != MachineType.ASSEMBLER) {
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
        setSelectedRecipe(recipes.get(index).id());
    }

    public void setSelectedRecipe(@Nullable ResourceLocation id) {
        selectedRecipe = id;
        progress = 0;
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
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("progress", progress);
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
        selectedRecipe = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
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
            return slot < type.inputSlots() ? items.insertItem(slot, stack, simulate) : stack;
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
