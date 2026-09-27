package de.craftorio.client;

import de.craftorio.defense.LevelPlan;
import de.craftorio.economy.Credits;
import de.craftorio.network.TdStatusPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import de.craftorio.quest.Quest;
import de.craftorio.quest.Quests;
import java.util.List;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Team balance, running tower defense level and the guide's current step in the top-left corner. */
public final class CreditsHud {
    private static final int BALANCE_COLOR = 0xFFD54F;
    private static final int TEAM_COLOR = 0xB0B0B0;
    private static final int GUIDE_COLOR = 0xD8D8E8;
    private static final int GUIDE_DONE_COLOR = 0x7CFC7C;
    private static final int GUIDE_WIDTH = 130;

    private CreditsHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        String teamName = ClientTeamState.teamName();
        if (teamName == null || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        graphics.drawString(minecraft.font, Credits.format(ClientTeamState.balance()), 6, 6, BALANCE_COLOR, true);
        graphics.drawString(minecraft.font, teamName, 6, 17, TEAM_COLOR, true);
        int y = 28;
        TdStatusPayload td = ClientTdState.status();
        if (td.running()) {
            graphics.drawString(minecraft.font, Component.translatable("craftorio.hud.td", td.level(), td.wave(), td.waves(),
                    td.lives(), LevelPlan.LIVES, td.enemiesLeft()), 6, y, td.lives() <= 5 ? 0xFF6B6B : 0xFFFFFF, true);
            y += 11;
        }
        renderGuide(graphics, minecraft, y);
    }

    /** The guide's current step, so the player always knows what to do next. */
    private static void renderGuide(GuiGraphics graphics, Minecraft minecraft, int y) {
        Quests.current(ClientTeamState.claimedQuests()).ifPresent(index -> {
            Quest quest = Quests.ALL.get(index);
            List<Long> progress = ClientTeamState.questProgress();
            long value = index < progress.size() ? progress.get(index) : 0;
            Component title = Component.translatable("craftorio.quest." + quest.id());
            boolean done = value >= quest.amount();
            MutableComponent line = Component.translatable(done ? "craftorio.hud.guide_done" : "craftorio.hud.guide", title);
            if (!done && quest.amount() > 1) {
                line.append(" (" + Credits.formatNumber(value) + " / " + Credits.formatNumber(quest.amount()) + ")");
            }
            // Wrapped narrowly so it stays clear of tooltips centred at the top (e.g. Jade).
            int lineY = y;
            for (FormattedCharSequence part : minecraft.font.split(line, GUIDE_WIDTH)) {
                graphics.drawString(minecraft.font, part, 6, lineY, done ? GUIDE_DONE_COLOR : GUIDE_COLOR, true);
                lineY += 10;
            }
            graphics.drawString(minecraft.font, Component.translatable("craftorio.hud.guide_key",
                    ClientEvents.OPEN_GUIDE.getTranslatedKeyMessage()), 6, lineY, TEAM_COLOR, true);
        });
    }
}
