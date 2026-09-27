package de.craftorio.client;

import de.craftorio.defense.LevelPlan;
import de.craftorio.economy.Credits;
import de.craftorio.network.TdStatusPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Team balance in the top-left corner. */
public final class CreditsHud {
    private static final int BALANCE_COLOR = 0xFFD54F;
    private static final int TEAM_COLOR = 0xB0B0B0;

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
        TdStatusPayload td = ClientTdState.status();
        if (td.running()) {
            graphics.drawString(minecraft.font, Component.translatable("craftorio.hud.td", td.level(), td.wave(), td.waves(),
                    td.lives(), LevelPlan.LIVES, td.enemiesLeft()), 6, 28, td.lives() <= 5 ? 0xFF6B6B : 0xFFFFFF, true);
        }
    }
}
