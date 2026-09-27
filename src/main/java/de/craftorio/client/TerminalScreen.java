package de.craftorio.client;

import de.craftorio.blueprint.Blueprint;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.blueprint.TerminalStats;
import de.craftorio.blueprint.UnlockRules;
import de.craftorio.economy.Credits;
import de.craftorio.menu.TerminalMenu;
import de.craftorio.registry.ModRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

public final class TerminalScreen extends BlueprintListScreen<TerminalMenu> {
    private boolean statsTab;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.blueprints"), button -> statsTab = false)
                .bounds(leftPos + 8, topPos + 17, 80, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.stats"), button -> statsTab = true)
                .bounds(leftPos + 92, topPos + 17, 80, 14).build());
    }

    @Override
    protected boolean showList() {
        return !statsTab;
    }

    @Override
    protected void renderRow(GuiGraphics graphics, int index, Holder.Reference<Blueprint> holder, int x, int y, int mouseX, int mouseY) {
        Blueprint blueprint = holder.value();
        UnlockRules.Status status = Blueprints.status(holder, ClientTeamState.unlocked(), ClientTeamState.balance(), minecraft.player.getInventory());
        int background = switch (status) {
            case UNLOCKED -> 0xFF22382A;
            case AVAILABLE -> 0xFF2E2E40;
            default -> 0xFF28282C;
        };
        renderRowBackground(graphics, x, y, background);
        graphics.renderItem(blueprint.result(), x + 3, y + 3);
        graphics.renderItemDecorations(font, blueprint.result(), x + 3, y + 3);
        graphics.drawString(font, font.plainSubstrByWidth(blueprint.result().getHoverName().getString(), 120), x + 24, y + 3, 0xFFFFFF, false);
        int textRight = x + BUTTON_X - 4;
        if (status != UnlockRules.Status.UNLOCKED) {
            int iconX = x + BUTTON_X - 4;
            String cost = blueprint.cost() > 0 ? Credits.format(blueprint.cost()) : "";
            iconX -= font.width(cost);
            textRight = iconX - 18 * blueprint.unlockItems().size() - 4;
            graphics.drawString(font, cost, iconX, y + 8, status == UnlockRules.Status.NOT_ENOUGH_CREDITS ? RED : GOLD, false);
            for (SizedIngredient key : blueprint.unlockItems()) {
                iconX -= 18;
                graphics.renderItem(firstItem(key), iconX, y + 3);
            }
            renderButton(graphics, Component.translatable("craftorio.terminal.unlock"), x, y, status == UnlockRules.Status.AVAILABLE, mouseX, mouseY);
        }
        String line = statusLine(holder, status).getString();
        graphics.drawString(font, font.plainSubstrByWidth(line, textRight - x - 24), x + 24, y + 13, statusColor(status), false);
    }

    private Component statusLine(Holder.Reference<Blueprint> holder, UnlockRules.Status status) {
        Component tier = Component.translatable("craftorio.blueprint.tier", holder.value().tier());
        Component detail = switch (status) {
            case UNLOCKED -> Component.translatable("craftorio.blueprint.status.unlocked");
            case MISSING_PREREQUISITE -> Component.translatable("craftorio.blueprint.requires", missingPrerequisite(holder.value()));
            case MISSING_KEY_ITEMS -> Component.translatable("craftorio.blueprint.status.missing_key_items");
            case NOT_ENOUGH_CREDITS -> Component.translatable("craftorio.blueprint.status.not_enough_credits");
            case AVAILABLE -> Component.translatable("craftorio.blueprint.status.available");
        };
        return tier.copy().append(" · ").append(detail);
    }

    private Component missingPrerequisite(Blueprint blueprint) {
        var registry = minecraft.level.registryAccess().registryOrThrow(ModRegistries.BLUEPRINTS);
        for (ResourceLocation required : blueprint.requires()) {
            if (!ClientTeamState.unlocked().contains(required.toString())) {
                Blueprint other = registry.get(ResourceKey.create(ModRegistries.BLUEPRINTS, required));
                return other == null ? Component.literal(required.toString()) : other.result().getHoverName();
            }
        }
        return Component.empty();
    }

    private static int statusColor(UnlockRules.Status status) {
        return switch (status) {
            case UNLOCKED -> GREEN;
            case AVAILABLE -> 0xFFFFFF;
            case NOT_ENOUGH_CREDITS -> RED;
            default -> GRAY;
        };
    }

    @Override
    protected void clickRow(int index, Holder.Reference<Blueprint> blueprint, boolean shift) {
        sendButton(index);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (statsTab) {
            renderStats(graphics);
        }
    }

    private void renderStats(GuiGraphics graphics) {
        TerminalStats stats = menu.stats();
        int x = leftPos + 12;
        int y = topPos + LIST_TOP + 2;
        line(graphics, "craftorio.terminal.stats.earned", Credits.format(stats.totalEarned()), x, y);
        line(graphics, "craftorio.terminal.stats.spent", Credits.format(stats.totalSpent()), x, y + 12);
        line(graphics, "craftorio.terminal.stats.recent", Credits.format(stats.earnedLastTenMinutes()), x, y + 24);
        graphics.drawString(font, Component.translatable("craftorio.terminal.stats.top"), x, y + 42, GRAY, false);
        int rowY = y + 54;
        for (TerminalStats.Row row : stats.topSales()) {
            ItemStack stack = BuiltInRegistries.ITEM.get(ResourceLocation.parse(row.item())).getDefaultInstance();
            graphics.renderItem(stack, x, rowY - 4);
            graphics.drawString(font, font.plainSubstrByWidth(stack.getHoverName().getString(), 110), x + 20, rowY, 0xFFFFFF, false);
            graphics.drawString(font, row.count() + "×", x + 136, rowY, GRAY, false);
            String credits = Credits.format(row.credits());
            graphics.drawString(font, credits, leftPos + imageWidth - 12 - font.width(credits), rowY, GOLD, false);
            rowY += 17;
        }
    }

    private void line(GuiGraphics graphics, String key, String value, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xFFFFFF, false);
        graphics.drawString(font, value, leftPos + imageWidth - 12 - font.width(value), y, GOLD, false);
    }
}
