package de.craftorio.client;

import de.craftorio.Craftorio;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bottom left: what the right mouse button (and friends) do with the item in hand, or with the block looked at, for
 * everything with a function that is not obvious. The texts are {@code craftorio.hint.<name>.<n>}.
 */
public final class ControlHintsHud {
    private static final int COLOR = 0xE8E8B0;
    private static final int LINE_HEIGHT = 10;
    private static final int MARGIN = 6;
    private static final int MAX_WIDTH = 220;

    /** Item id (path in the craftorio namespace) → hint name and number of lines. */
    private static final Map<String, Hint> HINTS = Map.ofEntries(
            Map.entry("conveyor_belt", new Hint("belt", 2)),
            Map.entry("fast_belt", new Hint("belt", 2)),
            Map.entry("express_belt", new Hint("belt", 2)),
            Map.entry("underground_belt", new Hint("underground", 1)),
            Map.entry("fast_underground_belt", new Hint("underground", 1)),
            Map.entry("splitter", new Hint("splitter", 2)),
            Map.entry("fast_splitter", new Hint("splitter", 2)),
            Map.entry("filter_inserter", new Hint("filter_inserter", 1)),
            Map.entry("path_wand", new Hint("path_wand", 2)),
            Map.entry("guide_book", new Hint("guide", 1)));

    private record Hint(String name, int lines) {
    }

    private ControlHintsHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        Hint hint = hintFor(minecraft.player.getMainHandItem().getItem());
        if (hint == null && minecraft.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            hint = hintFor(minecraft.level.getBlockState(block.getBlockPos()).getBlock().asItem());
        }
        if (hint == null) {
            return;
        }
        List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
        for (int i = 1; i <= hint.lines(); i++) {
            lines.addAll(minecraft.font.split(Component.translatable("craftorio.hint." + hint.name() + "." + i), MAX_WIDTH));
        }
        int y = minecraft.getWindow().getGuiScaledHeight() - MARGIN - lines.size() * LINE_HEIGHT;
        for (var line : lines) {
            graphics.drawString(minecraft.font, line, MARGIN, y, COLOR, true);
            y += LINE_HEIGHT;
        }
    }

    private static Hint hintFor(Item item) {
        if (item == null || new ItemStack(item).isEmpty()) {
            return null;
        }
        var id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals(Craftorio.MOD_ID) ? HINTS.get(id.getPath()) : null;
    }
}
