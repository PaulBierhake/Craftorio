package de.craftorio.client;

import de.craftorio.menu.PumpjackMenu;
import de.craftorio.oil.PumpjackBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The well, the crude oil in the tank and the power; the module slots are in the panel on the right. */
public final class PumpjackScreen extends MachineScreenBase<PumpjackMenu> {
    public PumpjackScreen(PumpjackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int energy() {
        return menu.energy();
    }

    @Override
    protected int capacity() {
        return 20 * PumpjackBlockEntity.POWER;
    }

    @Override
    protected int machineSlotCount() {
        return 0;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.yield() < 0) {
            graphics.drawString(font, Component.translatable("craftorio.pumpjack.no_well_gui"), 8, 24, 0xB02020, false);
        } else {
            graphics.drawString(font, Component.translatable("craftorio.pumpjack.yield", menu.yield()), 8, 24, 0x404040, false);
        }
        graphics.drawString(font, Component.translatable("craftorio.pumpjack.oil", menu.oil(), PumpjackBlockEntity.TANK), 8, 36, 0x404040, false);
    }
}
