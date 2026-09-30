package de.craftorio.menu;

import de.craftorio.defense.TowerBlockEntity;
import de.craftorio.defense.sim.TargetMode;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class TowerMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 12;
    /** Button ids 0 to 2 buy the next tier of the path with that index. */
    public static final int BUTTON_UPGRADE = 0;
    public static final int BUTTON_TARGET = 3;
    public static final int BUTTON_SELL = 4;
    /** Plus the ability's index. */
    public static final int BUTTON_ABILITY = 10;
    public static final int AMMO_X = 8;
    public static final int AMMO_Y = 98;
    /** Top of the player inventory: the GUI is taller than the standard machine GUI. */
    public static final int INVENTORY_TOP = 146;
    public static final int IMAGE_HEIGHT = 230;

    private final TowerBlockEntity tower;
    private final ContainerData data;

    public TowerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (TowerBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public TowerMenu(int containerId, Inventory inventory, TowerBlockEntity tower, ContainerData data) {
        super(ModMenus.TOWER.get(), containerId, tower);
        this.tower = tower;
        this.data = data;
        if (tower.type().usesItemAmmo()) {
            addSlot(new SlotItemHandler(tower.ammo(), 0, AMMO_X, AMMO_Y));
        }
        addPlayerInventory(inventory, INVENTORY_TOP);
        addDataSlots(data);
    }

    public TowerBlockEntity tower() {
        return tower;
    }

    /** The bought tier of a path. */
    public int tier(int path) {
        return data.get(path);
    }

    public int[] tiers() {
        return new int[]{data.get(0), data.get(1), data.get(2)};
    }

    public TargetMode targetMode() {
        return TargetMode.values()[Math.floorMod(data.get(3), TargetMode.values().length)];
    }

    public int energy() {
        return SplitIntData.join(data.get(4), data.get(5));
    }

    /** Coins paid for the tower; selling pays 70 % of it. */
    public long paid() {
        return SplitIntData.join(data.get(6), data.get(7)) & 0xFFFFFFFFL;
    }

    /** Seconds until ability {@code index} (0 or 1) can be used again. */
    public int abilityCooldown(int index) {
        return data.get(8 + 2 * index);
    }

    /** Seconds ability {@code index} is still active. */
    public int abilityActive(int index) {
        return data.get(9 + 2 * index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (id >= BUTTON_UPGRADE && id < BUTTON_UPGRADE + 3) {
            return tower.upgrade(serverPlayer, id - BUTTON_UPGRADE);
        }
        if (id >= BUTTON_ABILITY && id < BUTTON_ABILITY + 2) {
            return tower.activateAbility(serverPlayer, id - BUTTON_ABILITY);
        }
        if (id == BUTTON_TARGET) {
            tower.cycleTargetMode();
            return true;
        }
        if (id == BUTTON_SELL) {
            boolean sold = tower.sell(serverPlayer);
            if (sold) {
                serverPlayer.closeContainer();
            }
            return sold;
        }
        return false;
    }
}
