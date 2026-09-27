package de.craftorio.client;

import de.craftorio.blueprint.Blueprint;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.menu.WorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public final class WorkbenchScreen extends BlueprintListScreen<WorkbenchMenu> {
    private static final int INGREDIENTS_X = 104;

    public WorkbenchScreen(WorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("craftorio.workbench.hint"), leftPos + 8, topPos + 20, GRAY, false);
    }

    @Override
    protected void renderRow(GuiGraphics graphics, int index, Holder.Reference<Blueprint> holder, int x, int y, int mouseX, int mouseY) {
        Blueprint blueprint = holder.value();
        boolean unlocked = blueprint.isFree() || ClientTeamState.unlocked().contains(Blueprints.id(holder));
        boolean tierOk = blueprint.tier() <= menu.tier();
        Inventory inventory = minecraft.player.getInventory();
        boolean canBuild = unlocked && tierOk && Blueprints.craftableTimes(inventory, blueprint, 1) > 0;

        renderRowBackground(graphics, x, y, canBuild ? 0xFF22382A : unlocked && tierOk ? 0xFF2E2E40 : 0xFF28282C);
        graphics.renderItem(blueprint.result(), x + 3, y + 3);
        graphics.renderItemDecorations(font, blueprint.result(), x + 3, y + 3);
        graphics.drawString(font, font.plainSubstrByWidth(blueprint.result().getHoverName().getString(), 76), x + 24, y + 3,
                unlocked && tierOk ? 0xFFFFFF : GRAY, false);

        Component status;
        int statusColor = GRAY;
        if (!unlocked) {
            status = Component.translatable("craftorio.workbench.locked");
        } else if (!tierOk) {
            status = Component.translatable("craftorio.workbench.needs_tier", blueprint.tier());
            statusColor = RED;
        } else {
            status = Component.translatable("craftorio.blueprint.tier", blueprint.tier());
        }
        graphics.drawString(font, font.plainSubstrByWidth(status.getString(), 78), x + 24, y + 13, statusColor, false);

        int iconX = x + INGREDIENTS_X;
        for (SizedIngredient ingredient : blueprint.ingredients()) {
            ItemStack icon = firstItem(ingredient);
            graphics.renderItem(icon, iconX, y + 3);
            boolean enough = Blueprints.count(inventory, ingredient) >= ingredient.count();
            String count = String.valueOf(ingredient.count());
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.drawString(font, count, iconX + 17 - font.width(count), y + 12, enough ? 0xFFFFFF : RED, true);
            graphics.pose().popPose();
            iconX += 20;
        }
        if (unlocked && tierOk) {
            renderButton(graphics, Component.translatable("craftorio.workbench.build"), x, y, canBuild, mouseX, mouseY);
        }
    }

    @Override
    protected void clickRow(int index, Holder.Reference<Blueprint> blueprint, boolean shift) {
        sendButton(WorkbenchMenu.buttonId(index, shift));
    }
}
