package de.craftorio.client;

import de.craftorio.menu.BoilerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Fuel slot with a flame bar and the water status. */
public final class BoilerScreen extends MachineScreenBase<BoilerMenu> {
    public BoilerScreen(BoilerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int energy() {
        return 0;
    }

    @Override
    protected int capacity() {
        return 0;
    }

    @Override
    protected int machineSlotCount() {
        return 1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int x = leftPos + BoilerMenu.FUEL_X + 1;
        int y = topPos + BoilerMenu.FUEL_Y - 16;
        int flame = (int) (13 * menu.heatFraction());
        graphics.fill(x, y, x + 14, y + 13, 0xFF373737);
        if (flame > 0) {
            graphics.fillGradient(x + 1, y + 13 - flame, x + 13, y + 13, 0xFFFFD54F, 0xFFE0801F);
        }
        graphics.drawString(font, Component.translatable(menu.hasWater() ? "craftorio.boiler.water" : "craftorio.boiler.no_water"),
                leftPos + 8, topPos + 20, menu.hasWater() ? 0x2E6FA8 : 0xA83232, false);
    }
}
