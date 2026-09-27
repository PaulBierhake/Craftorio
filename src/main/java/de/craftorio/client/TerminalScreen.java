package de.craftorio.client;

import de.craftorio.blueprint.Blueprint;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.blueprint.TerminalStats;
import de.craftorio.blueprint.UnlockRules;
import de.craftorio.defense.EnemyType;
import de.craftorio.defense.LevelPlan;
import de.craftorio.defense.arena.ArenaTheme;
import de.craftorio.defense.arena.Mutator;
import de.craftorio.economy.Credits;
import de.craftorio.network.TdStatusPayload;
import de.craftorio.menu.TerminalMenu;
import de.craftorio.quest.Quest;
import de.craftorio.quest.Quests;
import java.util.List;
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
    private enum Tab { BLUEPRINTS, DEFENSE, STATS, QUESTS }

    private Tab tab = Tab.BLUEPRINTS;
    private Button startButton;
    private Button autoButton;
    private Button repairButton;
    private Button callButton;
    private int questScroll;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.blueprints"), button -> tab = Tab.BLUEPRINTS)
                .bounds(leftPos + 8, topPos + 17, 58, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.defense"), button -> tab = Tab.DEFENSE)
                .bounds(leftPos + 68, topPos + 17, 60, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.stats"), button -> tab = Tab.STATS)
                .bounds(leftPos + 130, topPos + 17, 58, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.quests"), button -> showQuests())
                .bounds(leftPos + 190, topPos + 17, 58, 14).build());
        startButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendButton(TerminalMenu.TD_START))
                .bounds(leftPos + 12, topPos + 150, 110, 18).build());
        autoButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendButton(TerminalMenu.TD_TOGGLE_AUTO))
                .bounds(leftPos + 128, topPos + 150, 116, 18).build());
        repairButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendButton(TerminalMenu.TD_REPAIR_ALL))
                .bounds(leftPos + 12, topPos + 172, 150, 18).build());
        callButton = addRenderableWidget(Button.builder(Component.translatable("craftorio.td.button.call"), button -> sendButton(TerminalMenu.TD_CALL_WAVE))
                .bounds(leftPos + 166, topPos + 172, 78, 18).build());
    }

    @Override
    protected boolean showList() {
        return tab == Tab.BLUEPRINTS;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        TdStatusPayload td = ClientTdState.status();
        boolean defense = tab == Tab.DEFENSE;
        startButton.visible = autoButton.visible = repairButton.visible = callButton.visible = defense;
        callButton.active = td.canCallWave();
        startButton.active = td.hasZone() && !td.running();
        startButton.setMessage(Component.translatable("craftorio.td.button.start", td.level()));
        autoButton.active = td.hasZone();
        autoButton.setMessage(Component.translatable(td.auto() ? "craftorio.td.button.auto_on" : "craftorio.td.button.auto_off"));
        repairButton.active = td.hasZone() && td.repairCost() > 0 && !td.running();
        repairButton.setMessage(Component.translatable("craftorio.td.button.repair", Credits.format(td.repairCost())));
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
        if (tab == Tab.STATS) {
            renderStats(graphics);
        } else if (tab == Tab.DEFENSE) {
            renderDefense(graphics);
        } else if (tab == Tab.QUESTS) {
            renderQuests(graphics, mouseX, mouseY);
        }
    }

    /** Opens the guide scrolled to the step the team should do next. */
    private void showQuests() {
        tab = Tab.QUESTS;
        questScroll = Quests.current(ClientTeamState.claimedQuests()).map(index -> index - 1).orElse(Quests.ALL.size());
    }

    private void renderQuests(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Quest> quests = Quests.ALL;
        questScroll = Math.max(0, Math.min(questScroll, quests.size() - VISIBLE_ROWS));
        int current = Quests.current(ClientTeamState.claimedQuests()).orElse(-1);
        Quest hovered = null;
        for (int row = 0; row < VISIBLE_ROWS && questScroll + row < quests.size(); row++) {
            int index = questScroll + row;
            Quest quest = quests.get(index);
            int x = leftPos + LIST_LEFT;
            int y = topPos + LIST_TOP + row * ROW_HEIGHT;
            long progress = questProgress(index);
            boolean claimed = ClientTeamState.claimedQuests().contains(quest.id());
            boolean done = progress >= quest.amount();
            renderRowBackground(graphics, x, y, claimed ? 0xFF22382A : done ? 0xFF2E2E40 : 0xFF28282C);
            if (index == current) {
                graphics.renderOutline(x, y, LIST_WIDTH - 6, ROW_HEIGHT - 2, 0xFFFFD54F);
            }
            String title = (index == current ? "▶ " : "") + Component.translatable("craftorio.quest." + quest.id()).getString();
            graphics.drawString(font, font.plainSubstrByWidth(title, BUTTON_X - 8), x + 4, y + 3, index == current ? GOLD : 0xFFFFFF, false);
            if (mouseX >= x && mouseX < x + BUTTON_X - 4 && mouseY >= y && mouseY < y + ROW_HEIGHT - 2) {
                hovered = quest;
            }
            String detail = claimed ? Component.translatable("craftorio.quest.done").getString()
                    : quest.amount() == 1 ? Component.translatable(done ? "craftorio.quest.ready" : "craftorio.quest.open").getString()
                    : Credits.formatNumber(progress) + " / " + Credits.formatNumber(quest.amount());
            String reward = Credits.format(quest.reward());
            int detailWidth = BUTTON_X - 16 - font.width(reward);
            graphics.drawString(font, font.plainSubstrByWidth(detail, detailWidth), x + 4, y + 13, claimed ? GREEN : done ? 0xFFFFFF : GRAY, false);
            graphics.drawString(font, reward, x + BUTTON_X - 4 - font.width(reward), y + 13, GOLD, false);
            if (!claimed) {
                renderButton(graphics, Component.translatable("craftorio.quest.claim"), x, y, done, mouseX, mouseY);
            }
        }
        if (quests.size() > VISIBLE_ROWS) {
            int trackTop = topPos + LIST_TOP;
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT - 2;
            int thumb = Math.max(10, trackHeight * VISIBLE_ROWS / quests.size());
            int thumbY = trackTop + (trackHeight - thumb) * questScroll / (quests.size() - VISIBLE_ROWS);
            graphics.fill(leftPos + imageWidth - 6, thumbY, leftPos + imageWidth - 3, thumbY + thumb, 0xFF8A8A9A);
        }
        if (hovered != null) {
            List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
            lines.add(Component.translatable("craftorio.quest." + hovered.id()).withStyle(net.minecraft.ChatFormatting.GOLD).getVisualOrderText());
            lines.addAll(font.split(Component.translatable("craftorio.quest." + hovered.id() + ".hint"), 200));
            graphics.renderTooltip(font, lines, mouseX, mouseY);
        }
    }

    private long questProgress(int index) {
        List<Long> progress = ClientTeamState.questProgress();
        return index < progress.size() ? progress.get(index) : 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.QUESTS) {
            questScroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.QUESTS && button == 0) {
            int row = (int) (mouseY - topPos - LIST_TOP) / ROW_HEIGHT;
            double localX = mouseX - leftPos - LIST_LEFT;
            double localY = mouseY - topPos - LIST_TOP - row * ROW_HEIGHT;
            int index = questScroll + row;
            if (mouseY >= topPos + LIST_TOP && row >= 0 && row < VISIBLE_ROWS && index < Quests.ALL.size()
                    && localX >= BUTTON_X && localX < BUTTON_X + BUTTON_WIDTH && localY >= 3 && localY < 19
                    && questProgress(index) >= Quests.ALL.get(index).amount()
                    && !ClientTeamState.claimedQuests().contains(Quests.ALL.get(index).id())) {
                sendButton(TerminalMenu.QUEST_CLAIM + index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderDefense(GuiGraphics graphics) {
        TdStatusPayload td = ClientTdState.status();
        int x = leftPos + 12;
        int y = topPos + LIST_TOP + 2;
        if (!td.hasZone()) {
            graphics.drawWordWrap(font, Component.translatable("craftorio.td.help"), x, y, imageWidth - 24, 0xFFFFFF);
            return;
        }
        String theme = ArenaTheme.values()[Math.floorMod(td.theme(), ArenaTheme.values().length)].name().toLowerCase();
        line(graphics, "craftorio.td.next_level", String.valueOf(td.level()), x, y);
        line(graphics, "craftorio.td.map", Component.translatable("craftorio.arena.theme." + theme).getString(), x, y + 11);
        Mutator mutator = Mutator.values()[Math.floorMod(td.mutator(), Mutator.values().length)];
        if (mutator != Mutator.NONE) {
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("craftorio.arena.mutator." + mutator.name().toLowerCase())
                    .getString(), imageWidth - 24), x, y + 22, 0xD68CFF, false);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("craftorio.arena.theme." + theme + ".rule").getString(),
                    imageWidth - 24), x, y + 22, GRAY, false);
        }
        if (td.running()) {
            line(graphics, "craftorio.td.wave", td.wave() + " / " + td.waves(), x, y + 34);
            line(graphics, "craftorio.td.lives", td.lives() + " / " + LevelPlan.LIVES, x, y + 45);
            line(graphics, "craftorio.td.enemies", String.valueOf(td.enemiesLeft()), x, y + 56);
        } else {
            line(graphics, "craftorio.td.reward", Credits.format(LevelPlan.of(td.level(), 1).reward()), x, y + 34);
            line(graphics, "craftorio.td.milestone", Component.translatable("craftorio.td.milestone.in",
                    10 - (td.level() - 1) % 10).getString(), x, y + 45);
            if (td.lastStars() > 0) {
                line(graphics, "craftorio.td.last_stars", "★".repeat(td.lastStars()) + "☆".repeat(3 - td.lastStars()), x, y + 56);
            }
        }
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i + 1 < td.preview().size(); i += 2) {
            EnemyType type = EnemyType.values()[Math.floorMod(td.preview().get(i), EnemyType.values().length)];
            if (!preview.isEmpty()) {
                preview.append(", ");
            }
            preview.append(td.preview().get(i + 1)).append("× ")
                    .append(Component.translatable("entity.craftorio." + type.name().toLowerCase()).getString());
        }
        graphics.drawString(font, Component.translatable("craftorio.td.preview"), x, y + 69, GRAY, false);
        graphics.drawString(font, font.plainSubstrByWidth(preview.toString(), imageWidth - 24), x, y + 80, 0xFFFFFF, false);
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("craftorio.td.reserves", Credits.formatNumber(td.energy()),
                td.bolts(), td.cartridges()).getString(), imageWidth - 24), x, y + 94, GRAY, false);
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
