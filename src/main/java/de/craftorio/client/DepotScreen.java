package de.craftorio.client;

import de.craftorio.menu.DepotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The chest window of the tower depot with a "Take all" button. */
public final class DepotScreen extends AbstractContainerScreen<DepotMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public DepotScreen(DepotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 114 + DepotMenu.ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("craftorio.arena.depot.take_all"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DepotMenu.TAKE_ALL);
            }
        }).bounds(leftPos + imageWidth - 68, topPos + 4, 62, 12).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, DepotMenu.ROWS * 18 + 17);
        graphics.blit(BACKGROUND, leftPos, topPos + DepotMenu.ROWS * 18 + 17, 0, 126, imageWidth, 96);
    }
}
