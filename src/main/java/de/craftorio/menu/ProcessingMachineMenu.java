package de.craftorio.menu;

import de.craftorio.machine.MachineType;
import de.craftorio.machine.ProcessingMachineBlockEntity;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class ProcessingMachineMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 10;
    public static final int FUEL_X = 56;
    public static final int FUEL_Y = 59;
    public static final int BUTTON_PREVIOUS_RECIPE = 0;
    public static final int BUTTON_NEXT_RECIPE = 1;

    private final ProcessingMachineBlockEntity machine;
    private final ContainerData data;

    public ProcessingMachineMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (ProcessingMachineBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public ProcessingMachineMenu(int containerId, Inventory inventory, ProcessingMachineBlockEntity machine, ContainerData data) {
        super(ModMenus.PROCESSING_MACHINE.get(), containerId, machine);
        this.machine = machine;
        this.data = data;
        MachineType type = machine.type();
        for (int slot = 0; slot < type.inputSlots(); slot++) {
            addSlot(new SlotItemHandler(machine.items(), slot, inputX(type, slot), inputY(type)));
        }
        addSlot(new SlotItemHandler(machine.items(), type.outputSlot(), outputX(type), inputY(type)));
        if (type.usesFuel()) {
            addSlot(new SlotItemHandler(machine.items(), type.fuelSlot(), FUEL_X, FUEL_Y));
        }
        addModuleSlots(machine.modules().inventory());
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    /** A productivity module sits in a machine whose recipe may not use one. */
    public boolean productivityBlocked() {
        return data.get(9) != 0;
    }

    public static int inputX(MachineType type, int slot) {
        return type.assembling() ? 8 + slot * 18 : 56;
    }

    public static int inputY(MachineType type) {
        return type.assembling() ? 53 : 35;
    }

    public static int outputX(MachineType type) {
        return 116;
    }

    public ProcessingMachineBlockEntity machine() {
        return machine;
    }

    public int energy() {
        return SplitIntData.join(data.get(0), data.get(1));
    }

    public float progress() {
        int time = data.get(3);
        return time <= 0 ? 0 : Math.min(1F, (float) data.get(2) / time);
    }

    /** Remaining burn time of the current fuel item, 0 to 1 (fuel machines only). */
    public float burnFraction() {
        int total = data.get(6);
        return total <= 0 ? 0 : Math.min(1F, (float) data.get(5) / total);
    }

    public net.minecraft.world.level.material.Fluid fluid() {
        int id = (short) data.get(7);
        return id < 0 ? net.minecraft.world.level.material.Fluids.EMPTY : net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(id);
    }

    public int fluidAmount() {
        return data.get(8);
    }

    /** Index into {@link ProcessingMachineBlockEntity#assemblerRecipes}, or -1. */
    public int selectedRecipeIndex() {
        return (short) data.get(4);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_PREVIOUS_RECIPE || id == BUTTON_NEXT_RECIPE) {
            machine.cycleRecipe(id == BUTTON_NEXT_RECIPE ? 1 : -1);
            return true;
        }
        return false;
    }
}
