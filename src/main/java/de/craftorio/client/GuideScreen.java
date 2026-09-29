package de.craftorio.client;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.economy.Credits;
import de.craftorio.research.Researches;
import de.craftorio.economy.Economy;
import de.craftorio.network.ClaimQuestPayload;
import de.craftorio.quest.Quest;
import de.craftorio.quest.Quests;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModRecipes;
import de.craftorio.registry.ModRegistries;
import de.craftorio.world.OreFieldBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The handbook: one page per guide step with the instructions, the progress and – most important for a new player –
 * how to make the thing: blueprint materials and workbench, the crafting grid of workbench or terminal, or where a
 * resource comes from.
 */
public final class GuideScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int HEIGHT = 240;
    private static final int GOLD = 0xFFD54F;
    private static final int GRAY = 0x9A9AA6;
    private static final int GREEN = 0x7CFC7C;

    private int page;
    private int left;
    private int top;
    private final List<Icon> icons = new ArrayList<>();
    private Button middle;
    /** Page whose reward was just collected: once the server confirms, the book turns to the next step. */
    private int claimedPage = -1;

    private record Icon(ItemStack stack, int x, int y) {
    }

    private GuideScreen() {
        super(Component.translatable("item.craftorio.guide_book"));
        page = Quests.current(ClientTeamState.claimedQuests()).orElse(Quests.ALL.size() - 1);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideScreen());
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        addRenderableWidget(Button.builder(Component.literal("◀"), button -> page = Math.max(0, page - 1))
                .bounds(left + 8, top + HEIGHT - 24, 30, 16).build());
        middle = addRenderableWidget(Button.builder(Component.translatable("craftorio.guide.current"), button -> {
                    if (claimable()) {
                        PacketDistributor.sendToServer(new ClaimQuestPayload(page));
                        claimedPage = page;
                    } else {
                        page = Quests.current(ClientTeamState.claimedQuests()).orElse(page);
                    }
                })
                .bounds(left + WIDTH / 2 - 50, top + HEIGHT - 24, 100, 16).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), button -> page = Math.min(Quests.ALL.size() - 1, page + 1))
                .bounds(left + WIDTH - 38, top + HEIGHT - 24, 30, 16).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** The page's goal is reached and its reward not collected yet. */
    private boolean claimable() {
        Quest quest = Quests.ALL.get(page);
        long progress = page < ClientTeamState.questProgress().size() ? ClientTeamState.questProgress().get(page) : 0;
        return progress >= quest.amount() && !ClientTeamState.claimedQuests().contains(quest.id());
    }

    @Override
    public void tick() {
        if (claimedPage >= 0 && ClientTeamState.claimedQuests().contains(Quests.ALL.get(claimedPage).id())) {
            claimedPage = -1;
            page = Quests.current(ClientTeamState.claimedQuests()).orElse(page);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        middle.setMessage(claimable()
                ? Component.translatable("craftorio.guide.claim").withStyle(ChatFormatting.GREEN)
                : Component.translatable("craftorio.guide.current"));
        super.render(graphics, mouseX, mouseY, partialTick);
        icons.clear();
        Quest quest = Quests.ALL.get(page);
        int x = left + 10;
        int y = top + 8;
        boolean claimed = ClientTeamState.claimedQuests().contains(quest.id());
        long progress = page < ClientTeamState.questProgress().size() ? ClientTeamState.questProgress().get(page) : 0;
        boolean done = progress >= quest.amount();
        int current = Quests.current(ClientTeamState.claimedQuests()).orElse(-1);

        graphics.drawString(font, Component.translatable("craftorio.guide.step", page + 1, Quests.ALL.size()), x, y, GRAY, false);
        Component state = Component.translatable(claimed ? "craftorio.quest.done" : page == current ? "craftorio.quest.next"
                : done ? "craftorio.quest.ready" : "craftorio.quest.open");
        graphics.drawString(font, state, left + WIDTH - 10 - font.width(state), y, claimed ? GREEN : page == current ? GOLD : GRAY, false);
        y += 13;
        graphics.drawString(font, Component.translatable("craftorio.quest." + quest.id()).withStyle(ChatFormatting.BOLD), x, y, GOLD, false);
        y += 13;
        for (FormattedCharSequence line : font.split(Component.translatable("craftorio.quest." + quest.id() + ".hint"), WIDTH - 20)) {
            graphics.drawString(font, line, x, y, 0xFFFFFF, false);
            y += 10;
        }
        y += 3;
        String goal = quest.amount() > 1
                ? Credits.formatNumber(Math.min(progress, quest.amount())) + " / " + Credits.formatNumber(quest.amount())
                : Component.translatable(done ? "craftorio.quest.ready" : "craftorio.quest.open").getString();
        graphics.drawString(font, Component.translatable("craftorio.guide.progress", goal), x, y, done ? GREEN : GRAY, false);
        String reward = Component.translatable("craftorio.guide.reward", de.craftorio.quest.QuestActions.rewardText(quest)).getString();
        graphics.drawString(font, reward, left + WIDTH - 10 - font.width(reward), y, GOLD, false);
        y += 14;
        graphics.fill(x, y, left + WIDTH - 10, y + 1, 0xFF4A4A5A);
        y += 6;
        renderHowTo(graphics, quest, x, y);

        for (Icon icon : icons) {
            if (mouseX >= icon.x && mouseX < icon.x + 16 && mouseY >= icon.y && mouseY < icon.y + 16) {
                graphics.renderTooltip(font, icon.stack, mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xFF5A5A6A);
        graphics.fill(left + 1, top + 1, left + WIDTH - 1, top + HEIGHT - 1, 0xFF1E1E24);
    }

    // --- "how to make it"

    private void renderHowTo(GuiGraphics graphics, Quest quest, int x, int y) {
        switch (quest.kind()) {
            case BUILD, UNLOCK -> {
                Blueprint blueprint = blueprint(quest.target());
                if (blueprint == null) {
                    return;
                }
                renderBlueprint(graphics, quest.target(), blueprint, quest.kind() == Quest.Kind.UNLOCK, x, y);
            }
            case MINE, SELL -> renderSource(graphics, BuiltInRegistries.ITEM.get(ResourceLocation.parse(quest.target())), x, y);
            default -> {
                Component note = quest.kind() == Quest.Kind.EARN ? Component.translatable("craftorio.guide.earn")
                        : Component.translatable("craftorio.guide.defense");
                for (FormattedCharSequence line : font.split(note, WIDTH - 20)) {
                    graphics.drawString(font, line, x, y, GRAY, false);
                    y += 10;
                }
            }
        }
    }

    private void renderBlueprint(GuiGraphics graphics, String blueprintId, Blueprint blueprint, boolean unlock, int x, int y) {
        var gate = Researches.gate(minecraft.level.registryAccess(), blueprintId);
        Component where = gate.isPresent()
                ? Component.translatable("craftorio.guide.research_then_build", Component.translatable("craftorio.research." + gate.get().substring(gate.get().indexOf(':') + 1)),
                        ModBlocks.WORKBENCH.get().getName())
                : Component.translatable("craftorio.guide.build_at", ModBlocks.WORKBENCH.get().getName());
        for (FormattedCharSequence line : font.split(where, WIDTH - 20)) {
            graphics.drawString(font, line, x, y, 0xFFFFFF, false);
            y += 10;
        }
        y += 2;
        int ix = x;
        for (SizedIngredient ingredient : blueprint.ingredients()) {
            icon(graphics, first(ingredient.ingredient()), ingredient.count(), ix, y);
            ix += 20;
        }
        graphics.drawString(font, "→", ix + 2, y + 4, 0xFFFFFF, false);
        icon(graphics, blueprint.result(), blueprint.result().getCount(), ix + 14, y);
        y += 24;
        // How to get the place where it is made: the vanilla recipe of the workbench.
        renderCrafting(graphics, Craftorio.id("workbench"), x, y);
    }

    private void renderCrafting(GuiGraphics graphics, ResourceLocation id, int x, int y) {
        RecipeManager recipes = minecraft.level.getRecipeManager();
        RecipeHolder<?> holder = recipes.byKey(id).orElse(null);
        if (holder == null || !(holder.value() instanceof ShapedRecipe shaped)) {
            return;
        }
        ItemStack result = shaped.getResultItem(minecraft.level.registryAccess());
        graphics.drawString(font, Component.translatable("craftorio.guide.crafting", result.getHoverName()), x, y, 0xFFFFFF, false);
        y += 11;
        for (int row = 0; row < shaped.getHeight(); row++) {
            for (int column = 0; column < shaped.getWidth(); column++) {
                int sx = x + column * 18;
                int sy = y + row * 18;
                graphics.fill(sx, sy, sx + 17, sy + 17, 0xFF33333F);
                Ingredient ingredient = shaped.getIngredients().get(row * shaped.getWidth() + column);
                if (!ingredient.isEmpty()) {
                    icon(graphics, first(ingredient), 1, sx, sy);
                }
            }
        }
        graphics.drawString(font, "→", x + 58, y + 22, 0xFFFFFF, false);
        icon(graphics, result, result.getCount(), x + 70, y + 18);
        graphics.drawString(font, Component.translatable("craftorio.guide.vanilla_table"), x + 94, y + 22, GRAY, false);
    }

    /** Where a sold item comes from: an ore field, a furnace or a machine recipe. */
    private void renderSource(GuiGraphics graphics, Item item, int x, int y) {
        ItemStack stack = new ItemStack(item);
        icon(graphics, stack, 1, x, y);
        graphics.drawString(font, Component.translatable("craftorio.guide.value", Credits.format(Economy.unitPrice(stack))), x + 22, y + 4, GOLD, false);
        y += 24;
        Block field = oreField(item);
        if (field != null) {
            icon(graphics, new ItemStack(field), 1, x, y - 4);
            for (FormattedCharSequence line : font.split(Component.translatable("craftorio.guide.mined_from", field.getName()), WIDTH - 44)) {
                graphics.drawString(font, line, x + 22, y, 0xFFFFFF, false);
                y += 10;
            }
            return;
        }
        RecipeManager recipes = minecraft.level.getRecipeManager();
        for (RecipeType<MachineRecipe> type : List.of(ModRecipes.ASSEMBLING.get())) {
            for (RecipeHolder<MachineRecipe> holder : recipes.getAllRecipesFor(type)) {
                if (holder.value().result().is(item)) {
                    Block machine = ModBlocks.ASSEMBLER.get();
                    graphics.drawString(font, Component.translatable("craftorio.guide.made_in", machine.getName()), x, y, 0xFFFFFF, false);
                    int ix = x;
                    for (SizedIngredient ingredient : holder.value().ingredients()) {
                        icon(graphics, first(ingredient.ingredient()), ingredient.count(), ix, y + 12);
                        ix += 20;
                    }
                    graphics.drawString(font, "→", ix + 2, y + 16, 0xFFFFFF, false);
                    icon(graphics, holder.value().result(), holder.value().result().getCount(), ix + 14, y + 12);
                    return;
                }
            }
        }
        int sy = y;
        recipes.getAllRecipesFor(RecipeType.SMELTING).stream()
                .filter(holder -> holder.value().getResultItem(minecraft.level.registryAccess()).is(item))
                .findFirst()
                .ifPresent(holder -> {
                    graphics.drawString(font, Component.translatable("craftorio.guide.smelted"), x, sy, 0xFFFFFF, false);
                    icon(graphics, first(holder.value().getIngredients().get(0)), 1, x, sy + 12);
                    graphics.drawString(font, "→", x + 22, sy + 16, 0xFFFFFF, false);
                    icon(graphics, stack, 1, x + 34, sy + 12);
                });
    }

    private static @Nullable Block oreField(Item item) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof OreFieldBlock field && field.resource().is(item)) {
                return block;
            }
        }
        return null;
    }

    private @Nullable Blueprint blueprint(String id) {
        return minecraft.level.registryAccess().registryOrThrow(ModRegistries.BLUEPRINTS).get(ResourceLocation.parse(id));
    }

    private static ItemStack first(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }

    private void icon(GuiGraphics graphics, ItemStack stack, int count, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y, count > 1 ? String.valueOf(count) : null);
        icons.add(new Icon(stack, x, y));
    }
}
