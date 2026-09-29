package de.craftorio.menu;

import de.craftorio.machine.DrillBlockEntity;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Drill GUI: fuel slot (burner tier only) and one output stack, like a furnace. */
public final class DrillMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 11;
    public static final int FUEL_X = 44;
    public static final int FUEL_Y = 53;
    public static final int OUTPUT_X = 116;
    public static final int OUTPUT_Y = 35;

    private final ContainerData data;
    private final boolean burner;

    public DrillMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (DrillBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public DrillMenu(int containerId, Inventory inventory, DrillBlockEntity drill, ContainerData data) {
        super(ModMenus.DRILL.get(), containerId, drill);
        this.data = data;
        this.burner = drill.tier().usesFuel();
        if (burner) {
            addSlot(new SlotItemHandler(drill.menuSlots(), DrillBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y));
        }
        addSlot(new SlotItemHandler(drill.menuSlots(), DrillBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addModuleSlots(drill.modules().inventory());
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    public boolean burner() {
        return burner;
    }

    public float burnFraction() {
        int total = data.get(1);
        return total <= 0 ? 0 : (float) data.get(0) / total;
    }

    public int fieldBlocks() {
        return data.get(2);
    }

    public int area() {
        return data.get(3);
    }

    public double itemsPerSecond() {
        return data.get(4) / 100.0;
    }

    public int acid() {
        return data.get(9);
    }

    /** Is there uranium ore below? Then the drill needs sulfuric acid. */
    public boolean needsAcid() {
        return data.get(10) != 0;
    }

    public int energy() {
        return SplitIntData.join(data.get(5), data.get(6));
    }

    public int capacity() {
        return SplitIntData.join(data.get(7), data.get(8));
    }
}
