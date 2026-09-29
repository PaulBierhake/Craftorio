package de.craftorio.client;

import de.craftorio.menu.LaboratoryMenu;
import de.craftorio.research.LaboratoryBlockEntity;
import de.craftorio.research.Research;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class LaboratoryScreen extends MachineScreenBase<LaboratoryMenu> {
    public LaboratoryScreen(LaboratoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int energy() {
        return menu.energy();
    }

    @Override
    protected int capacity() {
        return LaboratoryBlockEntity.ENERGY_CAPACITY;
    }

    @Override
    protected int machineSlotCount() {
        return Research.Pack.values().length;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        // Ghost icons show which pack goes into which slot.
        for (Research.Pack pack : Research.Pack.values()) {
            if (!menu.slots.get(pack.ordinal()).hasItem()) {
                int x = leftPos + LaboratoryMenu.SLOT_X + pack.ordinal() * LaboratoryMenu.SLOT_STEP;
                int y = topPos + LaboratoryMenu.SLOT_Y;
                graphics.renderItem(new ItemStack(pack.item()), x, y);
                graphics.fill(x, y, x + 16, y + 16, 300, 0x998B8B8B);
            }
        }
        int barX = leftPos + LaboratoryMenu.SLOT_X;
        int barY = topPos + 57;
        graphics.fill(barX, barY, barX + 96, barY + 5, 0xFF373737);
        graphics.fill(barX + 1, barY + 1, barX + 1 + (int) (94 * menu.progress()), barY + 4, 0xFF5AD05A);
        graphics.drawString(font, Component.translatable("craftorio.lab.status." + menu.status()), leftPos + 8, topPos + 18, 0x404040, false);
    }
}
