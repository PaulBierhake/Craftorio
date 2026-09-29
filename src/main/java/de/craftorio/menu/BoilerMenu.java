package de.craftorio.menu;

import de.craftorio.energy.BoilerBlockEntity;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class BoilerMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 5;
    public static final int FUEL_X = 80;
    public static final int FUEL_Y = 44;

    private final ContainerData data;

    public BoilerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (BoilerBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public BoilerMenu(int containerId, Inventory inventory, BoilerBlockEntity boiler, ContainerData data) {
        super(ModMenus.BOILER.get(), containerId, boiler);
        this.data = data;
        addSlot(new SlotItemHandler(boiler.fuel(), 0, FUEL_X, FUEL_Y));
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    /** Heat left of the burning fuel item, 0 to 1. */
    public float heatFraction() {
        int total = SplitIntData.join(data.get(2), data.get(3));
        return total <= 0 ? 0 : Math.min(1F, (float) SplitIntData.join(data.get(0), data.get(1)) / total);
    }

    public boolean hasWater() {
        return data.get(4) == 1;
    }
}
