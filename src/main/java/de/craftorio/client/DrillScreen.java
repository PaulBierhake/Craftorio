package de.craftorio.client;

import de.craftorio.machine.DrillBlockEntity;
import de.craftorio.menu.DrillMenu;
import de.craftorio.registry.ModFluids;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/** Furnace-like drill GUI: fuel with a burning flame, an arrow to the output stack and the drill's rate. */
public final class DrillScreen extends MachineScreenBase<DrillMenu> {
    private static final int GRAY = 0x404040;
    private static final int ACID_X = 132;
    private static final int ACID_WIDTH = 14;

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
        if (menu.needsAcid()) {
            int x = leftPos + ACID_X;
            int y = topPos + ENERGY_Y;
            graphics.fill(x, y, x + ACID_WIDTH, y + ENERGY_HEIGHT, 0xFF373737);
            graphics.fill(x + 1, y + 1, x + ACID_WIDTH - 1, y + ENERGY_HEIGHT - 1, 0xFF1E1E1E);
            int height = (ENERGY_HEIGHT - 2) * menu.acid() / DrillBlockEntity.ACID_CAPACITY;
            if (menu.acid() > 0) {
                graphics.fill(x + 1, y + ENERGY_HEIGHT - 1 - Math.max(1, height), x + ACID_WIDTH - 1, y + ENERGY_HEIGHT - 1,
                        ModFluids.color(ModFluids.SULFURIC_ACID.get()));
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.needsAcid() && isHovering(ACID_X, ENERGY_Y, ACID_WIDTH, ENERGY_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("craftorio.drill.acid", menu.acid(), DrillBlockEntity.ACID_CAPACITY), mouseX, mouseY);
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
        if (menu.needsAcid() && menu.acid() == 0) {
            graphics.drawString(font, Component.translatable("craftorio.drill.gui_no_acid"), 8, 35, 0xB02020, false);
        }
    }
}
