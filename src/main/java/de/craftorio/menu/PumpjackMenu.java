package de.craftorio.menu;

import de.craftorio.oil.PumpjackBlockEntity;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/** Pumpjack GUI: status text and the module slots (in the module panel). */
public final class PumpjackMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 4;

    private final ContainerData data;

    public PumpjackMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (PumpjackBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public PumpjackMenu(int containerId, Inventory inventory, PumpjackBlockEntity pumpjack, ContainerData data) {
        super(ModMenus.PUMPJACK.get(), containerId, pumpjack);
        this.data = data;
        addModuleSlots(pumpjack.modules().inventory());
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    public int energy() {
        return SplitIntData.join(data.get(0), data.get(1));
    }

    /** Yield of the well in percent, or -1 if the pumpjack does not stand on one. */
    public int yield() {
        return (short) data.get(2);
    }

    public int oil() {
        return data.get(3);
    }
}
