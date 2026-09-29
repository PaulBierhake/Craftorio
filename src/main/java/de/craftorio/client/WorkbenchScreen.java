package de.craftorio.client;

import de.craftorio.blueprint.Blueprint;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.menu.WorkbenchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import java.util.List;
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
        boolean unlocked = Blueprints.isKnown(minecraft.level.registryAccess(), ClientTeamState.researched(), holder);
        Inventory inventory = minecraft.player.getInventory();
        boolean canBuild = unlocked && Blueprints.craftableTimes(inventory, blueprint, 1) > 0;

        renderRowBackground(graphics, x, y, canBuild ? 0xFF22382A : unlocked ? 0xFF2E2E40 : 0xFF28282C);
        graphics.renderItem(blueprint.result(), x + 3, y + 3);
        graphics.renderItemDecorations(font, blueprint.result(), x + 3, y + 3);
        graphics.drawString(font, font.plainSubstrByWidth(blueprint.result().getHoverName().getString(), 76), x + 24, y + 3,
                unlocked ? 0xFFFFFF : GRAY, false);

        Component status;
        int statusColor = GRAY;
        if (!unlocked) {
            status = Component.translatable("craftorio.workbench.locked");
        } else {
            status = canBuild ? Component.translatable("craftorio.workbench.can_build") : Component.translatable("craftorio.workbench.missing");
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
        if (unlocked) {
            // Without enough material the button tells what is missing instead of doing nothing.
            renderButton(graphics, Component.translatable(canBuild ? "craftorio.workbench.build" : "craftorio.workbench.missing_button"),
                    x, y, true, mouseX, mouseY);
        }
    }

    @Override
    protected void clickRow(int index, Holder.Reference<Blueprint> holder, boolean shift) {
        Blueprint blueprint = holder.value();
        Inventory inventory = minecraft.player.getInventory();
        boolean unlocked = Blueprints.isKnown(minecraft.level.registryAccess(), ClientTeamState.researched(), holder);
        if (!unlocked || Blueprints.craftableTimes(inventory, blueprint, 1) > 0) {
            sendButton(WorkbenchMenu.buttonId(index, shift));
            return;
        }
        MutableComponent message = Component.translatable("craftorio.workbench.missing_list", blueprint.result().getHoverName())
                .withStyle(ChatFormatting.GOLD);
        for (SizedIngredient ingredient : blueprint.ingredients()) {
            int missing = ingredient.count() - Blueprints.count(inventory, ingredient);
            if (missing > 0) {
                message.append(Component.literal("\n  " + missing + "× ").withStyle(ChatFormatting.RED))
                        .append(firstItem(ingredient).getHoverName().copy().withStyle(ChatFormatting.WHITE));
            }
        }
        minecraft.gui.getChat().addMessage(message);
    }

    /** Materials with what the player already carries, missing ones in red. */
    @Override
    protected List<Component> ingredientTooltip(Blueprint blueprint) {
        Inventory inventory = minecraft.player.getInventory();
        List<Component> lines = new ArrayList<>();
        lines.add(blueprint.result().getHoverName());
        lines.add(Component.translatable("craftorio.blueprint.materials").withColor(GRAY));
        for (SizedIngredient ingredient : blueprint.ingredients()) {
            int have = Blueprints.count(inventory, ingredient);
            lines.add(Component.literal(ingredient.count() + "× ").append(firstItem(ingredient).getHoverName())
                    .append(Component.translatable("craftorio.workbench.have", have))
                    .withStyle(have >= ingredient.count() ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        return lines;
    }
}
