package de.craftorio.client;

import de.craftorio.menu.BeaconMenu;
import de.craftorio.module.BeaconBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Power bar, the state and what the beacon does; the module slots are in the panel on the right. */
public final class BeaconScreen extends MachineScreenBase<BeaconMenu> {
    public BeaconScreen(BeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int energy() {
        return menu.energy();
    }

    @Override
    protected int capacity() {
        return BeaconBlockEntity.ENERGY_CAPACITY;
    }

    @Override
    protected int machineSlotCount() {
        return 0;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("craftorio.beacon.info"), 8, 20, 0x404040, false);
        graphics.drawString(font, Component.translatable("craftorio.beacon.range"), 8, 32, 0x404040, false);
        graphics.drawString(font, Component.translatable(menu.active() ? "craftorio.beacon.active" : "craftorio.beacon.inactive"), 8, 56, menu.active() ? 0x2E7D32 : 0xB02020, false);
    }
}
