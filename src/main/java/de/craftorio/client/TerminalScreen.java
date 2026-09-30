package de.craftorio.client;

import de.craftorio.blueprint.Blueprint;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.blueprint.TerminalStats;
import de.craftorio.research.Research;
import de.craftorio.research.ResearchRules;
import de.craftorio.research.Researches;
import de.craftorio.defense.sim.EnemyDefs;
import de.craftorio.defense.Difficulty;
import de.craftorio.defense.LevelPlan;
import de.craftorio.defense.arena.ArenaTheme;
import de.craftorio.defense.arena.Mutator;
import de.craftorio.economy.Credits;
import de.craftorio.network.TdStatusPayload;
import de.craftorio.menu.TerminalMenu;
import de.craftorio.quest.Quest;
import de.craftorio.quest.Quests;
import java.util.ArrayList;
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
    private enum Tab { RESEARCH, DEFENSE, STATS, QUESTS }

    private Tab tab = Tab.RESEARCH;
    private Button startButton;
    private Button autoButton;
    private Button difficultyButton;
    private Button warChestSmallButton;
    private Button warChestLargeButton;
    private Button callButton;
    private Button sealsButton;
    private Button handbookButton;
    private int questScroll;
    private int researchScroll;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // The arena console shares this menu and opens at the Defense tab.
        if (inventory.player.level().dimension() == de.craftorio.defense.arena.Arenas.DIMENSION) {
            tab = Tab.DEFENSE;
        }
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.research"), button -> tab = Tab.RESEARCH)
                .bounds(leftPos + 8, topPos + 17, 58, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.defense"), button -> tab = Tab.DEFENSE)
                .bounds(leftPos + 68, topPos + 17, 60, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.stats"), button -> tab = Tab.STATS)
                .bounds(leftPos + 130, topPos + 17, 58, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.tab.quests"), button -> showQuests())
                .bounds(leftPos + 190, topPos + 17, 58, 14).build());
        warChestSmallButton = addRenderableWidget(Button.builder(Component.translatable("craftorio.td.button.warchest", 100), button -> sendButton(TerminalMenu.TD_WAR_CHEST_SMALL))
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("craftorio.td.warchest.hint")))
                .bounds(leftPos + 12, topPos + 118, 74, 16).build());
        warChestLargeButton = addRenderableWidget(Button.builder(Component.translatable("craftorio.td.button.warchest", "1.000"), button -> sendButton(TerminalMenu.TD_WAR_CHEST_LARGE))
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("craftorio.td.warchest.hint")))
                .bounds(leftPos + 91, topPos + 118, 74, 16).build());
        difficultyButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendButton(TerminalMenu.TD_DIFFICULTY))
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("craftorio.td.difficulty.hint")))
                .bounds(leftPos + 170, topPos + 118, 74, 16).build());
        startButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendButton(TerminalMenu.TD_START))
                .bounds(leftPos + 12, topPos + 138, 110, 18).build());
        autoButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendButton(TerminalMenu.TD_TOGGLE_AUTO))
                .bounds(leftPos + 128, topPos + 138, 116, 18).build());
        callButton = addRenderableWidget(Button.builder(Component.translatable("craftorio.td.button.call"), button -> sendButton(TerminalMenu.TD_CALL_WAVE))
                .bounds(leftPos + 12, topPos + 160, 232, 18).build());
        sealsButton = addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.claim.seals_button"), button -> sendButton(TerminalMenu.CLAIM_SEALS))
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("craftorio.terminal.claim.seals_hint")))
                .bounds(leftPos + 12, topPos + 183, 110, 14).build());
        handbookButton = addRenderableWidget(Button.builder(Component.translatable("craftorio.terminal.claim.handbook_button"), button -> sendButton(TerminalMenu.CLAIM_HANDBOOK))
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("craftorio.terminal.claim.handbook_hint")))
                .bounds(leftPos + 128, topPos + 183, 116, 14).build());
    }

    @Override
    protected boolean showList() {
        return false;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        TdStatusPayload td = ClientTdState.status();
        boolean defense = tab == Tab.DEFENSE;
        startButton.visible = autoButton.visible = callButton.visible = defense;
        difficultyButton.visible = warChestSmallButton.visible = warChestLargeButton.visible = defense;
        sealsButton.visible = handbookButton.visible = defense;
        callButton.active = td.canCallWave();
        startButton.active = td.hasZone() && !td.running();
        startButton.setMessage(Component.translatable("craftorio.td.button.start", td.level()));
        autoButton.active = td.hasZone();
        autoButton.setMessage(Component.translatable(td.auto() ? "craftorio.td.button.auto_on" : "craftorio.td.button.auto_off"));
        Difficulty difficulty = Difficulty.byOrdinal(td.difficulty());
        difficultyButton.active = td.hasZone() && !td.running() && (!td.campaignStarted() || difficulty != Difficulty.EASY);
        difficultyButton.setMessage(Component.translatable("craftorio.td.button.difficulty",
                Component.translatable("craftorio.td.difficulty." + difficulty.name().toLowerCase())));
        warChestSmallButton.active = warChestLargeButton.active = td.hasZone() && td.warChestLeft() > 0;
    }

    /** The terminal has no blueprint list; its research rows are drawn by {@link #renderResearch}. */
    @Override
    protected void renderRow(GuiGraphics graphics, int index, Holder.Reference<Blueprint> holder, int x, int y, int mouseX, int mouseY) {
    }

    @Override
    protected void clickRow(int index, Holder.Reference<Blueprint> blueprint, boolean shift) {
    }

    private List<Holder.Reference<Research>> researches() {
        return minecraft == null || minecraft.level == null ? List.of() : Researches.sorted(minecraft.level.registryAccess());
    }

    private ResearchRules.Status researchStatus(Holder.Reference<Research> holder) {
        return ResearchRules.status(Researches.id(holder), Researches.requires(holder.value()), ClientTeamState.researched(),
                ClientTeamState.researchQueue());
    }

    private static Component researchName(String id) {
        return Component.translatable("craftorio.research." + id.substring(id.indexOf(':') + 1));
    }

    private void renderResearch(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Holder.Reference<Research>> all = researches();
        researchScroll = Math.max(0, Math.min(researchScroll, Math.max(0, all.size() - VISIBLE_ROWS)));
        double costFactor = 1.0;
        Holder.Reference<Research> hovered = null;
        for (int row = 0; row < VISIBLE_ROWS && researchScroll + row < all.size(); row++) {
            Holder.Reference<Research> holder = all.get(researchScroll + row);
            Research research = holder.value();
            String id = Researches.id(holder);
            ResearchRules.Status status = researchStatus(holder);
            int x = leftPos + LIST_LEFT;
            int y = topPos + LIST_TOP + row * ROW_HEIGHT;
            renderRowBackground(graphics, x, y, switch (status) {
                case DONE -> 0xFF22382A;
                case ACTIVE -> 0xFF3A3420;
                case QUEUED -> 0xFF2A2E40;
                case AVAILABLE -> 0xFF2E2E40;
                case LOCKED -> 0xFF28282C;
            });
            if (status == ResearchRules.Status.ACTIVE) {
                graphics.renderOutline(x, y, LIST_WIDTH - 6, ROW_HEIGHT - 2, 0xFFFFD54F);
                long total = Math.max(1, research.units(costFactor));
                int width = (int) ((LIST_WIDTH - 8) * Math.min(1.0, (double) ClientTeamState.activeProgress() / total));
                graphics.fill(x + 1, y + ROW_HEIGHT - 5, x + 1 + width, y + ROW_HEIGHT - 3, 0xFF5AD05A);
            }
            graphics.renderItem(new ItemStack(research.packs().get(research.packs().size() - 1).item()), x + 3, y + 3);
            graphics.drawString(font, font.plainSubstrByWidth(researchName(id).getString(), BUTTON_X - 30), x + 22, y + 3,
                    status == ResearchRules.Status.LOCKED ? GRAY : 0xFFFFFF, false);
            String detail = switch (status) {
                case DONE -> Component.translatable("craftorio.research.status.done").getString();
                case ACTIVE -> Component.translatable("craftorio.research.status.active", ClientTeamState.activeProgress(),
                        research.units(costFactor)).getString();
                case QUEUED -> Component.translatable("craftorio.research.status.queued", ClientTeamState.researchQueue().indexOf(id) + 1).getString();
                case AVAILABLE -> research.costLine(costFactor);
                case LOCKED -> Component.translatable("craftorio.research.status.locked", missingPrerequisite(research)).getString();
            };
            graphics.drawString(font, font.plainSubstrByWidth(detail, BUTTON_X - 30), x + 22, y + 13,
                    status == ResearchRules.Status.DONE ? GREEN : status == ResearchRules.Status.LOCKED ? GRAY : 0xC8C8D8, false);
            boolean queueable = ResearchRules.canQueue(id, Researches.requires(research), ClientTeamState.researched(), ClientTeamState.researchQueue());
            if (status == ResearchRules.Status.ACTIVE || status == ResearchRules.Status.QUEUED) {
                renderButton(graphics, Component.translatable("craftorio.research.remove"), x, y, true, mouseX, mouseY);
            } else if (queueable) {
                renderButton(graphics, Component.translatable("craftorio.research.queue"), x, y, true, mouseX, mouseY);
            }
            if (mouseX >= x && mouseX < x + BUTTON_X - 4 && mouseY >= y && mouseY < y + ROW_HEIGHT - 2) {
                hovered = holder;
            }
        }
        if (all.size() > VISIBLE_ROWS) {
            int trackTop = topPos + LIST_TOP;
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT - 2;
            int thumb = Math.max(10, trackHeight * VISIBLE_ROWS / all.size());
            int thumbY = trackTop + (trackHeight - thumb) * researchScroll / (all.size() - VISIBLE_ROWS);
            graphics.fill(leftPos + imageWidth - 6, thumbY, leftPos + imageWidth - 3, thumbY + thumb, 0xFF8A8A9A);
        }
        if (hovered != null) {
            graphics.renderComponentTooltip(font, researchTooltip(hovered.value(), costFactor), mouseX, mouseY);
        }
    }

    private Component missingPrerequisite(Research research) {
        for (String required : Researches.requires(research)) {
            if (!ClientTeamState.researched().contains(required)) {
                return researchName(required);
            }
        }
        return Component.empty();
    }

    private List<Component> researchTooltip(Research research, double costFactor) {
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("craftorio.research.cost", research.costLine(costFactor)).withColor(GOLD));
        if (!research.requires().isEmpty()) {
            lines.add(Component.translatable("craftorio.research.requires").withColor(GRAY));
            for (ResourceLocation required : research.requires()) {
                lines.add(Component.literal(" ").append(researchName(required.toString())));
            }
        }
        lines.add(Component.translatable("craftorio.research.unlocks").withColor(GRAY));
        var blueprints = minecraft.level.registryAccess().registryOrThrow(ModRegistries.BLUEPRINTS);
        for (ResourceLocation unlocked : research.unlocks()) {
            Blueprint blueprint = blueprints.get(unlocked);
            lines.add(Component.literal(" ").append(blueprint != null ? blueprint.result().getHoverName() : Component.literal(unlocked.getPath())));
        }
        if (!research.unlockItems().isEmpty()) {
            lines.add(Component.translatable("craftorio.research.key_items").withColor(0xD68CFF));
            for (SizedIngredient key : research.unlockItems()) {
                lines.add(Component.literal(" " + key.count() + "× ").append(firstItem(key).getHoverName()));
            }
        }
        return lines;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (tab == Tab.RESEARCH) {
            renderResearch(graphics, mouseX, mouseY);
        } else if (tab == Tab.STATS) {
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
        if (tab == Tab.RESEARCH) {
            researchScroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.RESEARCH && button == 0) {
            int row = (int) (mouseY - topPos - LIST_TOP) / ROW_HEIGHT;
            double localX = mouseX - leftPos - LIST_LEFT;
            double localY = mouseY - topPos - LIST_TOP - row * ROW_HEIGHT;
            int index = researchScroll + row;
            List<Holder.Reference<Research>> all = researches();
            if (mouseY >= topPos + LIST_TOP && row >= 0 && row < VISIBLE_ROWS && index < all.size()
                    && localX >= BUTTON_X && localX < BUTTON_X + BUTTON_WIDTH && localY >= 3 && localY < 19) {
                sendButton(index);
                return true;
            }
        }
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
        Difficulty difficulty = Difficulty.byOrdinal(td.difficulty());
        line(graphics, "craftorio.td.next_level", td.level() + " · " + Component.translatable("craftorio.td.difficulty."
                + difficulty.name().toLowerCase()).getString(), x, y);
        line(graphics, "craftorio.td.map", Component.translatable("craftorio.arena.theme." + theme).getString(), x, y + 11);
        Mutator mutator = Mutator.values()[Math.floorMod(td.mutator(), Mutator.values().length)];
        if (mutator != Mutator.NONE) {
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("craftorio.arena.mutator." + mutator.name().toLowerCase())
                    .getString(), imageWidth - 24), x, y + 22, 0xD68CFF, false);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("craftorio.arena.theme." + theme + ".rule").getString(),
                    imageWidth - 24), x, y + 22, GRAY, false);
        }
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("craftorio.td.coins", Credits.formatNumber(td.coins()),
                Credits.formatNumber(td.warChestLeft())).getString(), imageWidth - 24), x, y + 33, GOLD, false);
        if (td.running()) {
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("craftorio.td.running", td.wave(), td.waves(), td.lives(),
                    td.maxLives(), td.enemiesLeft()).getString(), imageWidth - 24), x, y + 44, 0xFFFFFF, false);
        } else {
            String stars = td.lastStars() > 0 ? "  " + "★".repeat(td.lastStars()) + "☆".repeat(3 - td.lastStars()) : "";
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("craftorio.td.reward_line",
                    Credits.format(Math.round(LevelPlan.of(td.level(), 1).reward() * difficulty.rewardFactor())), td.maxLives()).getString() + stars,
                    imageWidth - 24), x, y + 44, 0xFFFFFF, false);
        }
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i + 1 < td.preview().size(); i += 2) {
            int code = td.preview().get(i);
            var def = EnemyDefs.byIndex(code & 255);
            if (!preview.isEmpty()) {
                preview.append(", ");
            }
            preview.append(td.preview().get(i + 1)).append("× ").append(Component.translatable("craftorio.enemy." + def.id()).getString());
            List<String> modifiers = new ArrayList<>();
            if ((code >> 8 & 1) != 0) {
                modifiers.add(Component.translatable("craftorio.enemy.modifier.camo").getString());
            }
            if ((code >> 8 & 2) != 0) {
                modifiers.add(Component.translatable("craftorio.enemy.modifier.regrow").getString());
            }
            if ((code >> 8 & 4) != 0) {
                modifiers.add(Component.translatable("craftorio.enemy.modifier.fortified").getString());
            }
            if (!modifiers.isEmpty()) {
                preview.append(" (").append(String.join(", ", modifiers)).append(")");
            }
        }
        graphics.drawString(font, Component.translatable("craftorio.td.preview"), x, y + 55, GRAY, false);
        graphics.drawString(font, font.plainSubstrByWidth(preview.toString(), imageWidth - 24), x, y + 66, 0xFFFFFF, false);
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("craftorio.td.reserves", Credits.formatNumber(td.energy()),
                td.bolts(), td.cartridges()).getString(), imageWidth - 24), x, y + 79, GRAY, false);
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
        rowY += 4;
        graphics.drawString(font, Component.translatable("craftorio.terminal.stats.research"), x, rowY - 8, GRAY, false);
        for (TerminalStats.Finished finished : stats.researched()) {
            graphics.drawString(font, font.plainSubstrByWidth(researchName(finished.research()).getString(), 130), x, rowY + 4, 0xFFFFFF, false);
            String time = playTime(finished.tick());
            graphics.drawString(font, time, leftPos + imageWidth - 12 - font.width(time), rowY + 4, GRAY, false);
            rowY += 11;
        }
    }

    /** Game time in ticks as hours and minutes since the world was created, e.g. "2:05 h". */
    static String playTime(long ticks) {
        long minutes = ticks / 1200;
        return String.format(java.util.Locale.ROOT, "%d:%02d h", minutes / 60, minutes % 60);
    }

    private void line(GuiGraphics graphics, String key, String value, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xFFFFFF, false);
        graphics.drawString(font, value, leftPos + imageWidth - 12 - font.width(value), y, GOLD, false);
    }
}
