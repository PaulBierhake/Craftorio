package de.craftorio.menu;

import de.craftorio.energy.GeneratorBlockEntity;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class GeneratorMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 6;
    public static final int FUEL_X = 80;
    public static final int FUEL_Y = 44;

    private final ContainerData data;

    public GeneratorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (GeneratorBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public GeneratorMenu(int containerId, Inventory inventory, GeneratorBlockEntity generator, ContainerData data) {
        super(ModMenus.GENERATOR.get(), containerId, generator);
        this.data = data;
        addSlot(new SlotItemHandler(generator.fuel(), 0, FUEL_X, FUEL_Y));
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    public int energy() {
        return SplitIntData.join(data.get(0), data.get(1));
    }

    public int capacity() {
        return SplitIntData.join(data.get(4), data.get(5));
    }

    public float burnFraction() {
        int total = data.get(3);
        return total <= 0 ? 0 : (float) data.get(2) / total;
    }
}
