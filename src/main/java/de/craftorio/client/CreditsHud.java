package de.craftorio.client;

import de.craftorio.economy.Credits;
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
    }
}
