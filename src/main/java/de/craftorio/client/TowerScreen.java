package de.craftorio.client;

import de.craftorio.Craftorio;
import de.craftorio.defense.Difficulty;
import de.craftorio.defense.TowerBlockEntity;
import de.craftorio.defense.sim.RoundRules;
import de.craftorio.defense.sim.TowerDef;
import de.craftorio.defense.sim.TowerDefs;
import de.craftorio.defense.sim.TowerProfile;
import de.craftorio.defense.sim.TowerRules;
import de.craftorio.economy.Credits;
import de.craftorio.menu.TowerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import java.util.Set;

/**
 * The tower GUI: three upgrade paths with five tiers each (Bloons TD 6 style), the target mode, selling and the ammunition slot.
 */
public final class TowerScreen extends AbstractContainerScreen<TowerMenu> {
    private static final ResourceLocation SLOT = Craftorio.id("textures/gui/machine.png");
    private static final int COLUMN_WIDTH = 54;
    private static final int COLUMN_LEFT = 7;
    private static final int PATHS_TOP = 17;
    private static final int BUTTON_TOP = 44;
    private static final int BUTTON_HEIGHT = 34;

    private final Button[] upgrade = new Button[3];
    private final Component[] lineOne = {Component.empty(), Component.empty(), Component.empty()};
    private final Component[] lineTwo = {Component.empty(), Component.empty(), Component.empty()};
    private Button target;
    private Button sell;
    private final Button[] ability = new Button[2];

    public TowerScreen(TowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = TowerMenu.IMAGE_HEIGHT;
        inventoryLabelY = TowerMenu.INVENTORY_TOP - 11;
    }

    @Override
    protected void init() {
        super.init();
        for (int path = 0; path < 3; path++) {
            int index = path;
            upgrade[path] = addRenderableWidget(Button.builder(Component.empty(), button -> press(TowerMenu.BUTTON_UPGRADE + index))
                    .bounds(leftPos + COLUMN_LEFT + path * (COLUMN_WIDTH + 2), topPos + BUTTON_TOP + 14, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        }
        target = addRenderableWidget(Button.builder(Component.empty(), button -> press(TowerMenu.BUTTON_TARGET))
                .bounds(leftPos + 32, topPos + 96, 66, 18).build());
        sell = addRenderableWidget(Button.builder(Component.empty(), button -> press(TowerMenu.BUTTON_SELL))
                .bounds(leftPos + 102, topPos + 96, 67, 18).build());
        for (int i = 0; i < 2; i++) {
            int index = i;
            ability[i] = addRenderableWidget(Button.builder(Component.empty(), button -> press(TowerMenu.BUTTON_ABILITY + index))
                    .bounds(leftPos + 8 + i * 82, topPos + 118, 80, 16).build());
        }
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private TowerDef def() {
        return menu.tower().def();
    }

    /** What the tower can do with its current upgrades (from the synced tiers, not from the client's copy of the block entity). */
    private TowerProfile profile() {
        return TowerDefs.profile(def(), menu.tiers());
    }

    private static Component pathName(TowerDef def, int path) {
        return Component.translatableWithFallback("craftorio.tower." + def.id() + ".path." + (path + 1), "Pfad " + (path + 1));
    }

    private static Component upgradeName(TowerDef def, int path, int tier) {
        return Component.translatableWithFallback("craftorio.tower." + def.id() + "." + (path + 1) + "." + tier, def.upgrade(path, tier).btd6());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        TowerDef def = def();
        var status = ClientTdState.status();
        double factor = Difficulty.byOrdinal(status.difficulty()).priceFactor();
        Set<String> researched = ClientTeamState.researched();
        int[] tiers = menu.tiers();
        for (int path = 0; path < 3; path++) {
            int tier = tiers[path];
            Button button = upgrade[path];
            if (tier >= TowerDef.TIERS) {
                lineOne[path] = Component.translatable("craftorio.tower.maxed");
                lineTwo[path] = Component.empty();
                button.active = false;
                button.setTooltip(null);
                continue;
            }
            TowerDef.Upgrade next = def.upgrade(path, tier + 1);
            long price = TowerRules.price(next.cost(), factor);
            String reason = TowerRules.lockReason(tiers, path);
            int tech = TowerRules.techRequired(tier + 1);
            if (reason == null && tech > 0 && !researched.contains(TowerBlockEntity.techResearch(tech))) {
                reason = "research";
            }
            button.active = reason == null;
            String name = font.plainSubstrByWidth(upgradeName(def, path, tier + 1).getString(), COLUMN_WIDTH - 6);
            lineOne[path] = Component.literal(name);
            lineTwo[path] = Component.literal(Credits.formatNumber(price) + " ⛁")
                    .withStyle(status.coins() >= price ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.RED);
            Component tip = upgradeName(def, path, tier + 1).copy().append(Component.literal(" – " + Credits.formatNumber(price) + " ⛁"));
            Component detail = Component.translatableWithFallback("craftorio.tower." + def.id() + "." + (path + 1) + "." + (tier + 1) + ".desc", "");
            if (reason != null) {
                tip = tip.copy().append(Component.literal("\n")).append(Component.translatable("craftorio.tower.locked." + reason)
                        .withStyle(net.minecraft.ChatFormatting.RED));
            } else if (tier + 1 >= 3) {
                tip = tip.copy().append(Component.literal("\n")).append(parts(menu.tower().upgradeMaterials(tier + 1)));
            }
            if (!detail.getString().isEmpty()) {
                tip = tip.copy().append(Component.literal("\n")).append(detail.copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            button.setTooltip(Tooltip.create(tip));
        }
        int abilities = profile().abilities.size();
        for (int i = 0; i < 2; i++) {
            ability[i].visible = i < abilities;
            if (i < abilities) {
                String id = profile().abilities.get(i).id();
                int cooldown = menu.abilityCooldown(i);
                int active = menu.abilityActive(i);
                boolean passive = profile().abilities.get(i).passive();
                ability[i].active = cooldown <= 0 && !passive;
                ability[i].setMessage(passive ? Component.translatable("craftorio.tower.ability.passive",
                        Component.translatableWithFallback("craftorio.ability." + id, id)) : active > 0 ? Component.translatable("craftorio.tower.ability.active", active)
                        : cooldown > 0 ? Component.translatable("craftorio.tower.ability.wait", cooldown)
                        : Component.translatableWithFallback("craftorio.ability." + id, id));
            }
        }
        target.setMessage(Component.translatable("craftorio.tower.target." + menu.targetMode().id()));
        target.active = def.targets().size() > 1;
        target.visible = !def.targets().isEmpty();
        sell.setMessage(Component.translatable("craftorio.tower.sell", Credits.formatNumber(TowerRules.sellValue(menu.paid(), RoundRules.SELL_SHARE))));
    }

    private static Component parts(List<SizedIngredient> materials) {
        net.minecraft.network.chat.MutableComponent line = Component.translatable("craftorio.tower.needs");
        for (SizedIngredient ingredient : materials) {
            ItemStack stack = ingredient.getItems()[0];
            line.append(Component.literal(" " + ingredient.count() + "× ")).append(stack.getHoverName());
        }
        return line.withStyle(net.minecraft.ChatFormatting.AQUA);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF555555);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        TowerDef def = def();
        int[] tiers = menu.tiers();
        for (int path = 0; path < 3; path++) {
            int x = leftPos + COLUMN_LEFT + path * (COLUMN_WIDTH + 2);
            int y = topPos + PATHS_TOP;
            graphics.fill(x, y, x + COLUMN_WIDTH, y + 27, 0xFF8B8B8B);
            graphics.drawString(font, font.plainSubstrByWidth(pathName(def, path).getString(), COLUMN_WIDTH - 4), x + 2, y + 2, 0x202020, false);
            for (int tier = 0; tier < TowerDef.TIERS; tier++) {
                int px = x + 3 + tier * 10;
                graphics.fill(px, y + 12, px + 8, y + 18, tier < tiers[path] ? 0xFF3CB043 : 0xFF404040);
            }
            String current = tiers[path] == 0 ? "–" : upgradeName(def, path, tiers[path]).getString();
            graphics.drawString(font, font.plainSubstrByWidth(current, COLUMN_WIDTH - 4), x + 2, y + 19, 0x303030, false);
        }
        String notation = TowerRules.notation(tiers);
        graphics.drawString(font, notation, leftPos + imageWidth - 8 - font.width(notation), topPos + 6, 0x404040, false);
        if (menu.tower().type().usesItemAmmo()) {
            Slot slot = menu.slots.get(0);
            graphics.blit(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, 176, 0, 18, 18);
        }
        graphics.drawString(font, supplyLine(), leftPos + 8, topPos + 82, 0x404040, false);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                graphics.blit(SLOT, leftPos + 7 + column * 18, topPos + TowerMenu.INVENTORY_TOP - 1 + row * 18, 176, 0, 18, 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            graphics.blit(SLOT, leftPos + 7 + column * 18, topPos + TowerMenu.INVENTORY_TOP + 57, 176, 0, 18, 18);
        }
    }

    /** What the tower shoots with and how much of it is there, in a short line. */
    private Component supplyLine() {
        var type = menu.tower().type();
        double range = profile().range;
        Component reach = range < 0 ? Component.translatable("craftorio.tower.range.unlimited")
                : Component.translatable("craftorio.tower.range", String.format(java.util.Locale.ROOT, "%.1f", range));
        if (type.usesEnergy()) {
            return reach.copy().append(Component.literal(" · " + Credits.formatNumber(menu.energy()) + " FE"));
        }
        if (type == de.craftorio.defense.TowerType.DEPOT) {
            Component line = Component.translatable("craftorio.tower.depot.income", Credits.formatNumber((long) de.craftorio.defense.sim.DepotEconomy.production(profile())));
            if (profile().has("bank")) {
                line = line.copy().append(Component.translatable("craftorio.tower.depot.bank", Credits.formatNumber(menu.bank())));
            }
            if (menu.debt() > 0) {
                line = line.copy().append(Component.translatable("craftorio.tower.depot.debt", Credits.formatNumber(menu.debt())));
            }
            return line.copy().append(Component.translatable(menu.basketDelivered() ? "craftorio.tower.depot.basket_yes" : "craftorio.tower.depot.basket_no"));
        }
        return reach;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int path = 0; path < 3; path++) {
            Button button = upgrade[path];
            int color = button.active ? 0xFFFFFF : 0xA0A0A0;
            graphics.drawCenteredString(font, lineOne[path], button.getX() + COLUMN_WIDTH / 2, button.getY() + 6, color);
            graphics.drawCenteredString(font, lineTwo[path], button.getX() + COLUMN_WIDTH / 2, button.getY() + 19, color);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }
}
