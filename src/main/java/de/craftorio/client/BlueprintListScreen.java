package de.craftorio.client;

import de.craftorio.blueprint.Blueprint;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.economy.Credits;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

/** Dark panel with a scrollable list of blueprint rows; shared by terminal and workbench. */
public abstract class BlueprintListScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    protected static final int ROW_HEIGHT = 24;
    protected static final int LIST_TOP = 34;
    protected static final int LIST_LEFT = 8;
    protected static final int LIST_WIDTH = 240;
    protected static final int VISIBLE_ROWS = 7;
    protected static final int BUTTON_X = 188;
    protected static final int BUTTON_WIDTH = 48;
    protected static final int GREEN = 0x7CFC7C;
    protected static final int RED = 0xFF6B6B;
    protected static final int GRAY = 0x9A9AA6;
    protected static final int GOLD = 0xFFD54F;

    protected int scroll;

    protected BlueprintListScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT + 8;
    }

    protected List<Holder.Reference<Blueprint>> blueprints() {
        return minecraft == null || minecraft.level == null ? List.of() : Blueprints.sorted(minecraft.level.registryAccess());
    }

    /** Number of rows in the current list (may be 0 when another tab is shown). */
    protected int rowCount() {
        return blueprints().size();
    }

    protected abstract void renderRow(GuiGraphics graphics, int index, Holder.Reference<Blueprint> blueprint, int x, int y, int mouseX, int mouseY);

    /** Called for clicks on a row's button area. */
    protected abstract void clickRow(int index, Holder.Reference<Blueprint> blueprint, boolean shift);

    protected boolean showList() {
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF5A5A6A);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF1E1E24);
        graphics.drawString(font, title, leftPos + 8, topPos + 7, 0xFFFFFF, false);
        String balance = Credits.format(ClientTeamState.balance());
        graphics.drawString(font, balance, leftPos + imageWidth - 8 - font.width(balance), topPos + 7, GOLD, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!showList()) {
            return;
        }
        List<Holder.Reference<Blueprint>> all = blueprints();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, all.size() - VISIBLE_ROWS)));
        Holder.Reference<Blueprint> hovered = null;
        for (int row = 0; row < VISIBLE_ROWS && scroll + row < all.size(); row++) {
            int index = scroll + row;
            int x = leftPos + LIST_LEFT;
            int y = topPos + LIST_TOP + row * ROW_HEIGHT;
            renderRow(graphics, index, all.get(index), x, y, mouseX, mouseY);
            if (mouseX >= x && mouseX < x + BUTTON_X - 4 && mouseY >= y && mouseY < y + ROW_HEIGHT - 2) {
                hovered = all.get(index);
            }
        }
        if (all.size() > VISIBLE_ROWS) {
            int trackTop = topPos + LIST_TOP;
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT - 2;
            int thumb = Math.max(10, trackHeight * VISIBLE_ROWS / all.size());
            int thumbY = trackTop + (trackHeight - thumb) * scroll / (all.size() - VISIBLE_ROWS);
            graphics.fill(leftPos + imageWidth - 6, thumbY, leftPos + imageWidth - 3, thumbY + thumb, 0xFF8A8A9A);
        }
        if (hovered != null) {
            graphics.renderComponentTooltip(font, ingredientTooltip(hovered.value()), mouseX, mouseY);
        }
    }

    protected List<Component> ingredientTooltip(Blueprint blueprint) {
        List<Component> lines = new ArrayList<>();
        lines.add(blueprint.result().getHoverName());
        lines.add(Component.translatable("craftorio.blueprint.materials").withColor(GRAY));
        for (SizedIngredient ingredient : blueprint.ingredients()) {
            lines.add(Component.literal(ingredient.count() + "× ").append(firstItem(ingredient).getHoverName()));
        }
        return lines;
    }

    protected static ItemStack firstItem(SizedIngredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }

    protected void renderRowBackground(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + LIST_WIDTH - 6, y + ROW_HEIGHT - 2, color);
    }

    /** Draws the row's button; returns whether the mouse is over it. */
    protected boolean renderButton(GuiGraphics graphics, Component label, int x, int y, boolean enabled, int mouseX, int mouseY) {
        int bx = x + BUTTON_X;
        int by = y + 3;
        boolean hover = enabled && mouseX >= bx && mouseX < bx + BUTTON_WIDTH && mouseY >= by && mouseY < by + 16;
        graphics.fill(bx, by, bx + BUTTON_WIDTH, by + 16, enabled ? (hover ? 0xFF6FA86F : 0xFF4E7E4E) : 0xFF3A3A3A);
        graphics.drawCenteredString(font, label, bx + BUTTON_WIDTH / 2, by + 4, enabled ? 0xFFFFFF : 0x777777);
        return hover;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showList() && button == 0) {
            int row = (int) (mouseY - topPos - LIST_TOP) / ROW_HEIGHT;
            double localX = mouseX - leftPos - LIST_LEFT;
            double localY = mouseY - topPos - LIST_TOP - row * ROW_HEIGHT;
            List<Holder.Reference<Blueprint>> all = blueprints();
            if (mouseY >= topPos + LIST_TOP && row >= 0 && row < VISIBLE_ROWS && scroll + row < all.size()
                    && localX >= BUTTON_X && localX < BUTTON_X + BUTTON_WIDTH && localY >= 3 && localY < 19) {
                clickRow(scroll + row, all.get(scroll + row), hasShiftDown());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
