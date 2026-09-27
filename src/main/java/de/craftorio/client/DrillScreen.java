package de.craftorio.client;

import de.craftorio.menu.DrillMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/** Furnace-like drill GUI: fuel with a burning flame, an arrow to the output stack and the drill's rate. */
public final class DrillScreen extends MachineScreenBase<DrillMenu> {
    private static final int GRAY = 0x404040;

    public DrillScreen(DrillMenu menu, Inventory inventory, Component title) {
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
        return menu.burner() ? 2 : 1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        if (menu.burner()) {
            int x = leftPos + DrillMenu.FUEL_X + 1;
            int y = topPos + DrillMenu.FUEL_Y - 18;
            graphics.blit(TEXTURE, x, y, 200, 0, 14, 14);
            int lit = (int) Math.ceil(14 * menu.burnFraction());
            if (lit > 0) {
                graphics.blit(TEXTURE, x, y + 14 - lit, 214, 14 - lit, 14, lit);
            }
        }
        // Arrow towards the output: filled while the drill is running.
        int ax = leftPos + 72;
        int ay = topPos + DrillMenu.OUTPUT_Y + 5;
        boolean running = menu.fieldBlocks() > 0 && (menu.burner() ? menu.burnFraction() > 0 : menu.energy() > 0);
        int color = running ? 0xFFFFFFFF : 0xFF8B8B8B;
        graphics.fill(ax, ay + 2, ax + 30, ay + 6, color);
        for (int i = 0; i < 5; i++) {
            graphics.fill(ax + 30 + i, ay - 1 + i, ax + 31 + i, ay + 9 - i, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.fieldBlocks() == 0) {
            graphics.drawString(font, Component.translatable("craftorio.drill.gui_no_field"), 8, 17, 0xB02020, false);
            return;
        }
        graphics.drawString(font, Component.translatable("craftorio.drill.gui_fields", menu.fieldBlocks(), menu.area()), 8, 17, GRAY, false);
        graphics.drawString(font, Component.translatable("craftorio.drill.gui_rate",
                String.format(Locale.ROOT, "%.2f", menu.itemsPerSecond())), 8, 26, GRAY, false);
    }
}
