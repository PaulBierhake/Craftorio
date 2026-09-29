package de.craftorio.menu;

import de.craftorio.heat.ReactorBlockEntity;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Reactor GUI: one fuel cell slot, the used cell output, the temperature limit for loading new cells. */
public final class ReactorMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 4;
    public static final int FUEL_X = 44;
    public static final int USED_X = 116;
    public static final int SLOT_Y = 44;
    public static final int BUTTON_LOWER_LIMIT = 0;
    public static final int BUTTON_RAISE_LIMIT = 1;

    private final ReactorBlockEntity reactor;
    private final ContainerData data;

    public ReactorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (ReactorBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public ReactorMenu(int containerId, Inventory inventory, ReactorBlockEntity reactor, ContainerData data) {
        super(ModMenus.REACTOR.get(), containerId, reactor);
        this.reactor = reactor;
        this.data = data;
        addSlot(new SlotItemHandler(reactor.items(), ReactorBlockEntity.FUEL_SLOT, FUEL_X, SLOT_Y));
        addSlot(new SlotItemHandler(reactor.items(), ReactorBlockEntity.USED_SLOT, USED_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    public int temperature() {
        return data.get(0);
    }

    public float burnFraction() {
        return (float) data.get(1) / de.craftorio.heat.HeatLogic.CELL_TICKS;
    }

    public int limit() {
        return data.get(2);
    }

    /** Heat output in kW (= FE per tick). */
    public int heat() {
        return data.get(3);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_LOWER_LIMIT || id == BUTTON_RAISE_LIMIT) {
            reactor.changeLimit(id == BUTTON_RAISE_LIMIT ? 1 : -1);
            return true;
        }
        return false;
    }
}
