package de.craftorio.client;

import de.craftorio.economy.Credits;
import de.craftorio.economy.block.TradingPostBlockEntity;
import de.craftorio.menu.TradingPostMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Counter GUI: items put in are listed with their value and only sold with the "Sell" button. */
public final class TradingPostScreen extends MachineScreenBase<TradingPostMenu> {
    private static final int TEXT_X = 90;
    private Button sell;

    public TradingPostScreen(TradingPostMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        sell = addRenderableWidget(Button.builder(Component.translatable("craftorio.trading_post.sell"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TradingPostMenu.SELL_BUTTON);
            }
        }).bounds(leftPos + TEXT_X, topPos + 50, 76, 18).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        sell.active = menu.counterValue() > 0;
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
        return TradingPostBlockEntity.COUNTER_SLOTS;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("craftorio.trading_post.value", Credits.format(menu.counterValue())),
                TEXT_X, 20, 0x8A6A00, false);
        String owner = Component.translatable("craftorio.trading_post.owner", menu.owner()).getString();
        graphics.drawString(font, font.plainSubstrByWidth(owner, 76), TEXT_X, 34, 0x404040, false);
        graphics.drawString(font, Component.translatable("craftorio.trading_post.earned", Credits.format(menu.totalEarned())),
                TEXT_X, inventoryLabelY, 0x404040, false);
    }
}
