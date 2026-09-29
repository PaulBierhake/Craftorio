package de.craftorio.menu;

import de.craftorio.defense.TargetMode;
import de.craftorio.defense.TowerBlockEntity;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class TowerMenu extends MachineMenuBase {
    public static final int DATA_COUNT = 6;
    public static final int BUTTON_UPGRADE = 0;
    public static final int BUTTON_TARGET = 1;
    public static final int AMMO_X = 26;
    public static final int AMMO_Y = 53;

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
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    public TowerBlockEntity tower() {
        return tower;
    }

    public int health() {
        return (short) data.get(0);
    }

    public int maxHealth() {
        return data.get(1);
    }

    public int upgradeLevel() {
        return data.get(2);
    }

    public int energy() {
        return SplitIntData.join(data.get(3), data.get(4));
    }

    public TargetMode targetMode() {
        return TargetMode.values()[Math.floorMod(data.get(5), TargetMode.values().length)];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TARGET) {
            tower.cycleTargetMode();
            return true;
        }
        return id == BUTTON_UPGRADE && player instanceof ServerPlayer serverPlayer && tower.upgrade(serverPlayer);
    }
}
