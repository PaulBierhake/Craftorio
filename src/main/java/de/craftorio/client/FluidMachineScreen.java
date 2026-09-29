package de.craftorio.client;

import de.craftorio.fluid.FluidMachineType;
import de.craftorio.fluid.FluidRecipes;
import de.craftorio.menu.FluidMachineMenu;
import de.craftorio.registry.ModFluids;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Recipe selector, ghost icons for the ingredients and three fluid bars. */
public final class FluidMachineScreen extends MachineScreenBase<FluidMachineMenu> {
    private final FluidMachineType type;

    public FluidMachineScreen(FluidMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.type = menu.machine().type();
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> click(FluidMachineMenu.BUTTON_PREVIOUS_RECIPE))
                .bounds(leftPos + 8, topPos + 18, 14, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> click(FluidMachineMenu.BUTTON_NEXT_RECIPE))
                .bounds(leftPos + 132, topPos + 18, 14, 18).build());
    }

    private void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected int energy() {
        return menu.energy();
    }

    @Override
    protected int capacity() {
        return 20_000;
    }

    @Override
    protected int machineSlotCount() {
        return FluidMachineType.INPUT_SLOTS + type.outputSlots();
    }

    private @Nullable FluidRecipes.Recipe selectedRecipe() {
        List<FluidRecipes.Recipe> recipes = FluidRecipes.forMachine(type);
        int index = menu.selectedRecipeIndex();
        return index >= 0 && index < recipes.size() ? recipes.get(index) : null;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int arrowX = leftPos + 80;
        int arrowY = topPos + FluidMachineMenu.SLOT_Y;
        graphics.blit(TEXTURE, arrowX, arrowY, 176, 18, 24, 17);
        int done = (int) (24 * menu.progress());
        if (done > 0) {
            graphics.blit(TEXTURE, arrowX, arrowY, 176, 35, done, 17);
        }
        FluidRecipes.Recipe recipe = selectedRecipe();
        graphics.fill(leftPos + 24, topPos + 18, leftPos + 130, topPos + 36, 0xFF8B8B8B);
        if (recipe == null) {
            graphics.drawString(font, Component.translatable("craftorio.gui.no_recipe"), leftPos + 30, topPos + 23, 0xFFFFFF, false);
        } else {
            graphics.drawString(font, Component.translatable("craftorio.recipe." + recipe.id().getPath().replace('/', '.')), leftPos + 28, topPos + 23, 0xFFFFFF, false);
            if (menu.productivityBlocked()) {
                graphics.drawString(font, Component.translatable("craftorio.module.productivity_blocked"), leftPos + 44, topPos + 38, 0xFF5555, false);
            }
            for (int i = 0; i < recipe.itemsIn().size(); i++) {
                if (!menu.slots.get(i).hasItem()) {
                    ItemStack ghost = recipe.itemsIn().get(i);
                    int x = leftPos + FluidMachineMenu.SLOT_X[i];
                    int y = topPos + FluidMachineMenu.SLOT_Y;
                    graphics.renderItem(ghost, x, y);
                    graphics.renderItemDecorations(font, ghost, x, y, String.valueOf(ghost.getCount()));
                    graphics.fill(x, y, x + 16, y + 16, 300, 0x998B8B8B);
                }
            }
        }
        for (int tank = 0; tank < FluidMachineMenu.TANKS && type.hasFluids(); tank++) {
            renderBar(graphics, tank, recipe);
        }
    }

    private void renderBar(GuiGraphics graphics, int tank, @Nullable FluidRecipes.Recipe recipe) {
        int x = leftPos + FluidMachineMenu.BAR_X[tank];
        int width = FluidMachineMenu.BAR_WIDTHS[tank];
        int y = topPos + FluidMachineMenu.BAR_Y;
        graphics.fill(x, y, x + width, y + FluidMachineMenu.BAR_HEIGHT, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + width - 1, y + FluidMachineMenu.BAR_HEIGHT - 1, 0xFF1E1E1E);
        Fluid fluid = menu.fluid(tank);
        int amount = menu.amount(tank);
        if (fluid != Fluids.EMPTY && amount > 0) {
            int height = (FluidMachineMenu.BAR_HEIGHT - 2) * amount / FluidMachineType.TANK_CAPACITY;
            graphics.fill(x + 1, y + FluidMachineMenu.BAR_HEIGHT - 1 - Math.max(1, height), x + width - 1,
                    y + FluidMachineMenu.BAR_HEIGHT - 1, ModFluids.color(fluid));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        FluidRecipes.Recipe recipe = selectedRecipe();
        for (int tank = 0; tank < FluidMachineMenu.TANKS && type.hasFluids(); tank++) {
            if (!isHovering(FluidMachineMenu.BAR_X[tank], FluidMachineMenu.BAR_Y, FluidMachineMenu.BAR_WIDTHS[tank], FluidMachineMenu.BAR_HEIGHT, mouseX, mouseY)) {
                continue;
            }
            Fluid fluid = menu.fluid(tank);
            Component text;
            if (fluid != Fluids.EMPTY && menu.amount(tank) > 0) {
                text = Component.translatable("craftorio.fluid.contents", new FluidStack(fluid, 1).getHoverName(),
                        String.format(Locale.ROOT, "%,d", menu.amount(tank)), String.format(Locale.ROOT, "%,d", FluidMachineType.TANK_CAPACITY));
            } else if (recipe != null && tank < recipe.fluidsIn().size()) {
                text = Component.translatable("craftorio.fluid.wanted", recipe.fluidsIn().get(tank).getHoverName(), recipe.fluidsIn().get(tank).getAmount());
            } else {
                text = Component.translatable("craftorio.fluid.empty", FluidMachineType.TANK_CAPACITY);
            }
            graphics.renderTooltip(font, text, mouseX, mouseY);
        }
    }
}
