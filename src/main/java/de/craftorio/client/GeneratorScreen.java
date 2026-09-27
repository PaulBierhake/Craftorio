package de.craftorio.client;

import de.craftorio.menu.GeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GeneratorScreen extends MachineScreenBase<GeneratorMenu> {
    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int energy() {
        return menu.energy();
    }

    @Override
    protected int capacity() {
        return menu.capacity();
    }

    @Override
    protected int machineSlotCount() {
        return 1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int x = leftPos + GeneratorMenu.FUEL_X + 1;
        int y = topPos + GeneratorMenu.FUEL_Y - 18;
        graphics.blit(TEXTURE, x, y, 200, 0, 14, 14);
        int lit = (int) Math.ceil(14 * menu.burnFraction());
        if (lit > 0) {
            graphics.blit(TEXTURE, x, y + 14 - lit, 214, 14 - lit, 14, lit);
        }
    }
}
