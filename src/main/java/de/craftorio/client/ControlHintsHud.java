package de.craftorio.client;

import de.craftorio.Craftorio;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Map;

/**
 * Right next to the hotbar: what the mouse buttons do with the item in hand, or with the block looked at, for
 * everything with a function that is not obvious. Keys are drawn as icons, the texts are
 * {@code craftorio.hint.<name>.<n>}.
 */
public final class ControlHintsHud {
    private static final int COLOR = 0xE8E8B0;
    private static final int LINE_HEIGHT = 13;
    private static final int GAP = 3;

    private enum Key {
        RIGHT_CLICK, SNEAK, GUIDE
    }

    private record Line(String text, Key... keys) {
    }

    private record Hint(String name, List<Line> lines) {
        Hint(String name, Line... lines) {
            this(name, List.of(lines));
        }
    }

    private static Hint hint(String name, Key[]... keys) {
        Line[] lines = new Line[keys.length];
        for (int i = 0; i < keys.length; i++) {
            lines[i] = new Line(name + "." + (i + 1), keys[i]);
        }
        return new Hint(name, lines);
    }

    private static final Key[] R = {Key.RIGHT_CLICK};
    private static final Key[] S_R = {Key.SNEAK, Key.RIGHT_CLICK};

    private static final Hint BELT = hint("belt", R);
    private static final Hint UNDERGROUND = hint("underground", R);
    private static final Hint SPLITTER = hint("splitter", R, R, S_R);
    private static final Hint FILTER_INSERTER = hint("filter_inserter", R, S_R);
    private static final Hint PATH_WAND = hint("path_wand", R, S_R, S_R);
    private static final Hint GUIDE = hint("guide", new Key[] {Key.GUIDE});

    /** Item id (path in the craftorio namespace) → hint. */
    private static final Map<String, Hint> HINTS = Map.ofEntries(
            Map.entry("conveyor_belt", BELT),
            Map.entry("fast_belt", BELT),
            Map.entry("express_belt", BELT),
            Map.entry("underground_belt", UNDERGROUND),
            Map.entry("fast_underground_belt", UNDERGROUND),
            Map.entry("splitter", SPLITTER),
            Map.entry("fast_splitter", SPLITTER),
            Map.entry("filter_inserter", FILTER_INSERTER),
            Map.entry("path_wand", PATH_WAND),
            Map.entry("guide_book", GUIDE));

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
        Font font = minecraft.font;
        int x = graphics.guiWidth() / 2 + 91 + 36;
        int y = graphics.guiHeight() - 4 - hint.lines().size() * LINE_HEIGHT;
        for (Line line : hint.lines()) {
            int cx = x;
            for (int i = 0; i < line.keys().length; i++) {
                if (i > 0) {
                    graphics.drawString(font, "+", cx, y + 2, COLOR, true);
                    cx += font.width("+") + GAP * 2;
                }
                cx += drawKey(graphics, font, minecraft, line.keys()[i], cx, y) + GAP;
            }
            graphics.drawString(font, Component.translatable("craftorio.hint." + line.text()), cx + 2, y + 2, COLOR, true);
            y += LINE_HEIGHT;
        }
    }

    /** Draws one key icon at (x, y) and returns its width. */
    private static int drawKey(GuiGraphics graphics, Font font, Minecraft minecraft, Key key, int x, int y) {
        if (key == Key.RIGHT_CLICK) {
            // A mouse with the right button lit.
            graphics.fill(x, y, x + 9, y + 12, 0xFFC8C8C8);
            graphics.fill(x + 1, y + 1, x + 8, y + 11, 0xFF303030);
            graphics.fill(x + 5, y + 1, x + 8, y + 6, 0xFFE05030);
            graphics.fill(x + 4, y + 1, x + 5, y + 6, 0xFFC8C8C8);
            graphics.fill(x + 1, y + 6, x + 8, y + 7, 0xFFC8C8C8);
            return 9;
        }
        Component label = key == Key.SNEAK ? minecraft.options.keyShift.getTranslatedKeyMessage()
                : ClientEvents.OPEN_GUIDE.getTranslatedKeyMessage();
        int width = Math.max(11, font.width(label) + 6);
        graphics.fill(x, y, x + width, y + 12, 0xFFC8C8C8);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 10, 0xFF404040);
        graphics.fill(x + 1, y + 10, x + width - 1, y + 11, 0xFF202020);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 2, 0xFFFFFF, false);
        return width;
    }

    private static Hint hintFor(Item item) {
        if (item == null || new ItemStack(item).isEmpty()) {
            return null;
        }
        var id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals(Craftorio.MOD_ID) ? HINTS.get(id.getPath()) : null;
    }
}
