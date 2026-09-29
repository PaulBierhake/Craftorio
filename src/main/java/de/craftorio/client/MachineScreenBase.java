package de.craftorio.client;

import de.craftorio.Craftorio;
import de.craftorio.menu.MachineMenuBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.Locale;

/** Shared panel, slot frames and energy bar for machine GUIs (sprites in textures/gui/machine.png). */
public abstract class MachineScreenBase<M extends MachineMenuBase> extends AbstractContainerScreen<M> {
    protected static final ResourceLocation TEXTURE = Craftorio.id("textures/gui/machine.png");
    protected static final int ENERGY_X = 152;
    protected static final int ENERGY_Y = 17;
    protected static final int ENERGY_WIDTH = 14;
    protected static final int ENERGY_HEIGHT = 54;

    protected MachineScreenBase(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected abstract int energy();

    protected abstract int capacity();

    protected abstract int machineSlotCount();

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int i = 0; i < machineSlotCount(); i++) {
            Slot slot = menu.slots.get(i);
            graphics.blit(TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 176, 0, 18, 18);
        }
        renderEnergyBar(graphics);
        renderModulePanel(graphics);
    }

    /** The module slots hang off the right edge of the GUI in a small panel of their own. */
    private void renderModulePanel(GuiGraphics graphics) {
        int count = menu.moduleSlotCount();
        if (count == 0) {
            return;
        }
        int x = leftPos + imageWidth - 1;
        int y = topPos + MachineMenuBase.MODULE_Y - 6;
        int height = 18 * count + 12;
        graphics.fill(x, y, x + 28, y + height, 0xFF555555);
        graphics.fill(x + 1, y + 1, x + 27, y + height - 1, 0xFFC6C6C6);
        int first = menu.slots.size() - 36 - count;
        for (int i = 0; i < count; i++) {
            Slot slot = menu.slots.get(first + i);
            graphics.blit(TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 176, 0, 18, 18);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        int count = menu.moduleSlotCount();
        if (count > 0 && mouseX >= guiLeft + imageWidth - 1 && mouseX < guiLeft + imageWidth + 27
                && mouseY >= guiTop + MachineMenuBase.MODULE_Y - 6 && mouseY < guiTop + MachineMenuBase.MODULE_Y + 18 * count + 6) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    /** The sum of the effects of the modules in the panel, for the tooltip over it. */
    private void renderModuleTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int count = menu.moduleSlotCount();
        if (count == 0) {
            return;
        }
        int x = leftPos + imageWidth - 1;
        int y = topPos + MachineMenuBase.MODULE_Y - 6;
        if (mouseX < x || mouseX >= x + 28 || mouseY < y || mouseY >= y + 18 * count + 12 || getSlotUnderMouse() != null) {
            return;
        }
        int first = menu.slots.size() - 36 - count;
        de.craftorio.module.ModuleEffects total = de.craftorio.module.ModuleEffects.NONE;
        for (int i = 0; i < count; i++) {
            if (menu.slots.get(first + i).getItem().getItem() instanceof de.craftorio.module.ModuleItem module) {
                total = total.plus(module.effects());
            }
        }
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("craftorio.module.panel"));
        lines.add(Component.translatable("craftorio.module.effect.speed", String.format(Locale.ROOT, "%+.0f", total.speed())));
        lines.add(Component.translatable("craftorio.module.effect.energy", String.format(Locale.ROOT, "%+.0f", total.energy())));
        lines.add(Component.translatable("craftorio.module.effect.productivity", String.format(Locale.ROOT, "%+.0f", total.productivity())));
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private void renderEnergyBar(GuiGraphics graphics) {
        if (capacity() <= 0) {
            return; // e.g. towers that use ammunition
        }
        int x = leftPos + ENERGY_X;
        int y = topPos + ENERGY_Y;
        graphics.fill(x, y, x + ENERGY_WIDTH, y + ENERGY_HEIGHT, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + ENERGY_WIDTH - 1, y + ENERGY_HEIGHT - 1, 0xFF1E1E1E);
        int filled = capacity() <= 0 ? 0 : (int) ((long) (ENERGY_HEIGHT - 2) * energy() / capacity());
        graphics.fillGradient(x + 1, y + ENERGY_HEIGHT - 1 - filled, x + ENERGY_WIDTH - 1, y + ENERGY_HEIGHT - 1, 0xFFFFD54F, 0xFFE0801F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (capacity() > 0 && isHovering(ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("craftorio.gui.energy",
                    String.format(Locale.ROOT, "%,d", energy()), String.format(Locale.ROOT, "%,d", capacity())), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
        renderModuleTooltip(graphics, mouseX, mouseY);
    }
}
