package de.craftorio.menu;

import de.craftorio.registry.ModMenus;
import de.craftorio.research.LaboratoryBlockEntity;
import de.craftorio.research.Research;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The laboratory: one slot per science pack kind. */
public final class LaboratoryMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 5;
    public static final int SLOT_X = 26;
    public static final int SLOT_Y = 35;
    public static final int SLOT_STEP = 24;

    private final ContainerData data;

    public LaboratoryMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (LaboratoryBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public LaboratoryMenu(int containerId, Inventory inventory, LaboratoryBlockEntity lab, ContainerData data) {
        super(ModMenus.LABORATORY.get(), containerId, lab);
        this.data = data;
        for (Research.Pack pack : Research.Pack.values()) {
            addSlot(new SlotItemHandler(lab.packs(), pack.ordinal(), SLOT_X + pack.ordinal() * SLOT_STEP, SLOT_Y));
        }
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    public int energy() {
        return SplitIntData.join(data.get(0), data.get(1));
    }

    /** Progress of the current unit, 0 to 1. */
    public float progress() {
        int total = data.get(3);
        return total <= 0 ? 0 : Math.min(1F, 1F - (float) data.get(2) / total);
    }

    public int status() {
        return data.get(4);
    }
}
