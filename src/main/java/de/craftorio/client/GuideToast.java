package de.craftorio.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** "Guide goal reached – collect the reward": shown like an advancement when a guide step is completed. */
public final class GuideToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    private static final long DURATION = 6000;

    private final Component title;
    private final Component description;
    private final ItemStack icon;

    public GuideToast(Component title, Component description, ItemStack icon) {
        this.title = title;
        this.description = description;
        this.icon = icon;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        graphics.blitSprite(BACKGROUND, 0, 0, width(), height());
        graphics.drawString(toasts.getMinecraft().font, title, 30, 7, 0xFFFF00, false);
        Font font = toasts.getMinecraft().font;
        String text = description.getString();
        if (font.width(text) > 125) {
            text = font.plainSubstrByWidth(text, 125 - font.width("…")) + "…";
        }
        graphics.drawString(font, text, 30, 18, 0xFFFFFF, false);
        graphics.renderFakeItem(icon, 8, 8);
        return timeSinceLastVisible >= DURATION * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
