package de.craftorio.menu;

import de.craftorio.registry.ModMenus;
import de.craftorio.world.cave.CaveEntranceBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Construction site of a cave entrance or mine shaft: what is needed and delivered, the status, and a slot that
 * books every needed item the moment it is put in (what is not needed stays in the slot and goes back to the player).
 */
public final class EntranceMenu extends MachineMenuBase {
    public static final int MAX_ITEMS = 6;
    /** Data: stage, drill progress, energy (two halves), then delivered counts. */
    public static final int DELIVERED_DATA = 4;
    public static final int DATA_COUNT = DELIVERED_DATA + MAX_ITEMS;
    public static final int SLOT_X = 132;
    public static final int SLOT_Y = 44;

    private final CaveEntranceBlockEntity site;
    private final ContainerData data;
    private final SimpleContainer input = new SimpleContainer(1);
    private boolean booking;

    public EntranceMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (CaveEntranceBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public EntranceMenu(int containerId, Inventory inventory, CaveEntranceBlockEntity site, ContainerData data) {
        super(ModMenus.ENTRANCE.get(), containerId, site);
        this.site = site;
        this.data = data;
        input.addListener(this::bookDeliveries);
        addSlot(new Slot(input, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return site.needs(stack);
            }
        });
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    private void bookDeliveries(Container container) {
        if (booking || site.getLevel() == null || site.getLevel().isClientSide) {
            return;
        }
        ItemStack stack = input.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        booking = true;
        input.setItem(0, site.materials().insertItem(0, stack, false));
        booking = false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, input);
    }

    public CaveEntranceBlockEntity site() {
        return site;
    }

    public int stage() {
        return data.get(0);
    }

    public float drillFraction() {
        return Math.min(1F, (float) data.get(1) / CaveEntranceBlockEntity.DRILL_TICKS);
    }

    public int energy() {
        return SplitIntData.join(data.get(2), data.get(3));
    }

    /** What the site needs, in the order of the delivered counts. */
    public List<Item> items() {
        return new ArrayList<>(CaveEntranceBlockEntity.requirements(site.target()).keySet());
    }

    public int required(Item item) {
        return CaveEntranceBlockEntity.requirements(site.target()).getOrDefault(item, 0);
    }

    public int delivered(int index) {
        return data.get(DELIVERED_DATA + index);
    }
}
