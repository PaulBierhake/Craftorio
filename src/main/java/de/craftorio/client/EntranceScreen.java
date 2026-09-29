package de.craftorio.client;

import de.craftorio.menu.EntranceMenu;
import de.craftorio.world.cave.CaveEntranceBlock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** What the construction site needs with a progress bar per material, the drilling progress and an input slot. */
public final class EntranceScreen extends MachineScreenBase<EntranceMenu> {
    private static final int ROW_HEIGHT = 13;

    public EntranceScreen(EntranceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int energy() {
        return menu.energy();
    }

    @Override
    protected int capacity() {
        return 20_000;
    }

    @Override
    protected int machineSlotCount() {
        return 1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int x = leftPos + 8;
        int y = topPos + 20;
        Component layer = Component.translatable(menu.site().target() == de.craftorio.world.cave.Layer.CAVES ? "craftorio.entrance.caves" : "craftorio.entrance.mines");
        graphics.drawString(font, layer, x, y - 10, 0xFFFFFF, false);
        int stage = menu.stage();
        if (stage == CaveEntranceBlock.STAGE_MATERIALS) {
            List<Item> items = menu.items();
            for (int i = 0; i < items.size() && i < EntranceMenu.MAX_ITEMS; i++) {
                Item item = items.get(i);
                int required = menu.required(item);
                int delivered = Math.min(required, menu.delivered(i));
                graphics.renderItem(new ItemStack(item), x, y - 3 + i * ROW_HEIGHT - 1);
                int barX = x + 20;
                int barY = y + i * ROW_HEIGHT;
                graphics.fill(barX, barY, barX + 76, barY + 8, 0xFF373737);
                graphics.fill(barX + 1, barY + 1, barX + 75, barY + 7, 0xFF1E1E1E);
                int width = required <= 0 ? 0 : 74 * delivered / required;
                graphics.fill(barX + 1, barY + 1, barX + 1 + width, barY + 7, delivered >= required ? 0xFF4CAF50 : 0xFFE0A030);
                graphics.drawString(font, delivered + "/" + required, barX + 80, barY, delivered >= required ? 0x7CFC7C : 0xFFFFFF, false);
            }
        } else if (stage == CaveEntranceBlock.STAGE_DRILLING) {
            graphics.drawString(font, Component.translatable("craftorio.entrance.drilling", (int) (100 * menu.drillFraction())), x, y, 0xFFFFFF, false);
            graphics.fill(x, y + 14, x + 110, y + 24, 0xFF373737);
            graphics.fill(x + 1, y + 15, x + 1 + (int) (108 * menu.drillFraction()), y + 23, 0xFFE0A030);
            graphics.drawString(font, Component.translatable("craftorio.entrance.power", de.craftorio.world.cave.CaveEntranceBlockEntity.energyPerTick(menu.site().target())),
                    x, y + 30, 0x9A9AA6, false);
        } else {
            graphics.drawString(font, Component.translatable("craftorio.entrance.open"), x, y, 0x7CFC7C, false);
        }
        if (stage == CaveEntranceBlock.STAGE_MATERIALS) {
            graphics.drawString(font, Component.translatable("craftorio.entrance.drop"), leftPos + EntranceMenu.SLOT_X - 22, topPos + EntranceMenu.SLOT_Y - 12, 0x9A9AA6, false);
        }
    }
}
