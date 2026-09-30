package de.craftorio.client;

import de.craftorio.defense.TowerBlockEntity;
import de.craftorio.defense.TowerStats;
import de.craftorio.economy.Credits;
import de.craftorio.menu.TowerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Locale;

public final class TowerScreen extends MachineScreenBase<TowerMenu> {
    private Button upgrade;
    private Button target;

    public TowerScreen(TowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        upgrade = addRenderableWidget(Button.builder(Component.translatable("craftorio.tower.upgrade"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TowerMenu.BUTTON_UPGRADE);
            }
        }).bounds(leftPos + 48, topPos + 52, 60, 18).build());
        target = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TowerMenu.BUTTON_TARGET);
            }
        }).bounds(leftPos + 48, topPos + 71, 60, 12).build());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        target.setMessage(Component.translatable("craftorio.tower.target." + menu.targetMode().name().toLowerCase()));
    }

    @Override
    protected int energy() {
        return menu.energy();
    }

    @Override
    protected int capacity() {
        return menu.tower().type().energyCapacity();
    }

    @Override
    protected int machineSlotCount() {
        return menu.tower().type().usesItemAmmo() ? 1 : 0;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int level = Math.max(1, menu.upgradeLevel());
        int x = leftPos + 8;
        int y = topPos + 18;
        String stats = String.format(Locale.ROOT, "%s · %.1f dmg · %.0f m", Component.translatable("craftorio.tower.level", level).getString(),
                TowerStats.damage(menu.tower().type(), level), menu.tower().type().range());
        graphics.drawString(font, stats, x + 2, y + 21, 0x404040, false);

        boolean maxed = level >= TowerStats.MAX_LEVEL;
        upgrade.active = !maxed;
        if (!maxed) {
            int next = level + 1;
            int cx = leftPos + 112;
            graphics.drawString(font, Credits.format(TowerStats.upgradeCredits(next)), cx, topPos + 52, 0x806000, false);
            for (SizedIngredient ingredient : menu.tower().upgradeMaterials(next)) {
                ItemStack icon = ingredient.getItems()[0];
                graphics.renderItem(icon, cx, topPos + 61);
                graphics.renderItemDecorations(font, icon, cx, topPos + 61, String.valueOf(ingredient.count()));
            }
        }
    }
}
