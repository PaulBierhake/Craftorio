package de.craftorio.menu;

import de.craftorio.fluid.FluidMachineBlockEntity;
import de.craftorio.fluid.FluidMachineType;
import de.craftorio.registry.ModMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Chemical plant and oil refinery: recipe arrows, two item inputs, the product slot and three fluid bars. */
public final class FluidMachineMenu extends MachineMenuBase {
    public static final int TANK_DATA = 5;
    public static final int TANKS = FluidMachineType.INPUT_TANKS + FluidMachineType.OUTPUT_TANKS;
    public static final int DATA_COUNT = TANK_DATA + 2 * TANKS + 1;
    public static final int BUTTON_PREVIOUS_RECIPE = 0;
    public static final int BUTTON_NEXT_RECIPE = 1;
    public static final int SLOT_Y = 53;
    /** Left edge of the input slots, the output slot, and the fluid bars (two inputs, one output). */
    public static final int[] SLOT_X = {44, 62, 106, 128};
    /** Two input bars, then three thinner output bars in front of the energy bar. */
    public static final int[] BAR_X = {8, 26, 126, 134, 142};
    public static final int[] BAR_WIDTHS = {14, 14, 7, 7, 7};
    public static final int BAR_Y = 40;
    public static final int BAR_HEIGHT = 36;

    private final FluidMachineBlockEntity machine;
    private final ContainerData data;

    public FluidMachineMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (FluidMachineBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public FluidMachineMenu(int containerId, Inventory inventory, FluidMachineBlockEntity machine, ContainerData data) {
        super(ModMenus.FLUID_MACHINE.get(), containerId, machine);
        this.machine = machine;
        this.data = data;
        for (int slot = 0; slot < FluidMachineType.INPUT_SLOTS; slot++) {
            addSlot(new SlotItemHandler(machine.items(), slot, SLOT_X[slot], SLOT_Y));
        }
        for (int slot = 0; slot < machine.type().outputSlots(); slot++) {
            addSlot(new SlotItemHandler(machine.items(), FluidMachineType.OUTPUT_SLOT + slot, SLOT_X[2 + slot], SLOT_Y));
        }
        addModuleSlots(machine.modules().inventory());
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    /** A productivity module sits in a machine whose recipe may not use one. */
    public boolean productivityBlocked() {
        return data.get(DATA_COUNT - 1) != 0;
    }

    public FluidMachineBlockEntity machine() {
        return machine;
    }

    public int energy() {
        return SplitIntData.join(data.get(0), data.get(1));
    }

    public float progress() {
        int time = data.get(3);
        return time <= 0 ? 0 : Math.min(1F, (float) data.get(2) / time);
    }

    /** Index into {@link de.craftorio.fluid.FluidRecipes#forMachine}, or -1. */
    public int selectedRecipeIndex() {
        return (short) data.get(4);
    }

    /** The fluid in a tank as seen by the client, or water-less {@code EMPTY}. */
    public Fluid fluid(int tank) {
        int id = (short) data.get(TANK_DATA + 2 * tank);
        return id < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(id);
    }

    public int amount(int tank) {
        return data.get(TANK_DATA + 2 * tank + 1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_PREVIOUS_RECIPE || id == BUTTON_NEXT_RECIPE) {
            if (!machine.cycle(id == BUTTON_NEXT_RECIPE ? 1 : -1) && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("craftorio.gui.no_known_recipe"), true);
            }
            return true;
        }
        return false;
    }
}
