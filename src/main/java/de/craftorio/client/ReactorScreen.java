package de.craftorio.client;

import de.craftorio.heat.HeatLogic;
import de.craftorio.menu.ReactorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Fuel cell in, used cell out, the temperature bar and the limit for loading new cells. */
public final class ReactorScreen extends MachineScreenBase<ReactorMenu> {
    private static final int GRAY = 0x404040;

    public ReactorScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("-"), button -> click(ReactorMenu.BUTTON_LOWER_LIMIT))
                .bounds(leftPos + 8, topPos + 62, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> click(ReactorMenu.BUTTON_RAISE_LIMIT))
                .bounds(leftPos + 130, topPos + 62, 14, 14).build());
    }

    private void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected int energy() {
        return 0;
    }

    @Override
    protected int capacity() {
        return 0; // the heat bar is drawn here instead of the energy bar
    }

    @Override
    protected int machineSlotCount() {
        return 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int x = leftPos + ENERGY_X;
        int y = topPos + ENERGY_Y;
        graphics.fill(x, y, x + ENERGY_WIDTH, y + ENERGY_HEIGHT, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + ENERGY_WIDTH - 1, y + ENERGY_HEIGHT - 1, 0xFF1E1E1E);
        double share = (menu.temperature() - HeatLogic.AMBIENT) / (HeatLogic.MAX_TEMPERATURE - HeatLogic.AMBIENT);
        int filled = (int) ((ENERGY_HEIGHT - 2) * Math.max(0, Math.min(1, share)));
        graphics.fillGradient(x + 1, y + ENERGY_HEIGHT - 1 - filled, x + ENERGY_WIDTH - 1, y + ENERGY_HEIGHT - 1, 0xFFFF8A3A, 0xFFB01818);
        // the limit as a tick mark
        double limitShare = (menu.limit() - HeatLogic.AMBIENT) / (HeatLogic.MAX_TEMPERATURE - HeatLogic.AMBIENT);
        int mark = y + ENERGY_HEIGHT - 1 - (int) ((ENERGY_HEIGHT - 2) * limitShare);
        graphics.fill(x - 2, mark, x + ENERGY_WIDTH + 2, mark + 1, 0xFFFFFFFF);
        // flame above the fuel slot while a cell burns
        int fx = leftPos + ReactorMenu.FUEL_X + 1;
        int fy = topPos + ReactorMenu.SLOT_Y - 18;
        graphics.blit(TEXTURE, fx, fy, 200, 0, 14, 14);
        int lit = (int) Math.ceil(14 * menu.burnFraction());
        if (lit > 0) {
            graphics.blit(TEXTURE, fx, fy + 14 - lit, 214, 14 - lit, 14, lit);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("craftorio.reactor.temperature", menu.temperature()), 8, 17, GRAY, false);
        graphics.drawString(font, Component.translatable("craftorio.reactor.heat", String.format(java.util.Locale.ROOT, "%.0f", menu.heat() / 1000.0)), 8, 26, GRAY, false);
        graphics.drawString(font, Component.translatable("craftorio.reactor.limit", menu.limit()), 26, 65, GRAY, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("craftorio.reactor.temperature", menu.temperature()), mouseX, mouseY);
        }
    }
}
