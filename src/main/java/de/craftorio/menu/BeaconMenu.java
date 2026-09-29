package de.craftorio.menu;

import de.craftorio.module.BeaconBlockEntity;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/** Beacon GUI: the two module slots (in the module panel) and the power. */
public final class BeaconMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 3;

    private final ContainerData data;

    public BeaconMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (BeaconBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public BeaconMenu(int containerId, Inventory inventory, BeaconBlockEntity beacon, ContainerData data) {
        super(ModMenus.BEACON.get(), containerId, beacon);
        this.data = data;
        addModuleSlots(beacon.modules());
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    public int energy() {
        return SplitIntData.join(data.get(0), data.get(1));
    }

    public boolean active() {
        return data.get(2) != 0;
    }
}
