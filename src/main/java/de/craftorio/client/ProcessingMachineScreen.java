package de.craftorio.client;

import de.craftorio.machine.MachineType;
import de.craftorio.machine.ProcessingMachineBlockEntity;
import de.craftorio.menu.ProcessingMachineMenu;
import de.craftorio.recipe.MachineRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ProcessingMachineScreen extends MachineScreenBase<ProcessingMachineMenu> {
    private static final int FLUID_X = 134;
    private static final int FLUID_Y = 40;
    private static final int FLUID_HEIGHT = 36;

    private final MachineType type;

    public ProcessingMachineScreen(ProcessingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.type = menu.machine().type();
    }

    @Override
    protected void init() {
        super.init();
        if (type.assembling()) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> click(ProcessingMachineMenu.BUTTON_PREVIOUS_RECIPE))
                    .bounds(leftPos + 8, topPos + 18, 14, 18).build());
            addRenderableWidget(Button.builder(Component.literal(">"), button -> click(ProcessingMachineMenu.BUTTON_NEXT_RECIPE))
                    .bounds(leftPos + 132, topPos + 18, 14, 18).build());
        }
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
        return type.usesFuel() ? 0 : MachineType.ENERGY_CAPACITY;
    }

    @Override
    protected int machineSlotCount() {
        return type.slotCount();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int arrowX = leftPos + (type.assembling() ? 87 : 79);
        int arrowY = topPos + ProcessingMachineMenu.inputY(type);
        graphics.blit(TEXTURE, arrowX, arrowY, 176, 18, 24, 17);
        int done = (int) (24 * menu.progress());
        if (done > 0) {
            graphics.blit(TEXTURE, arrowX, arrowY, 176, 35, done, 17);
        }
        if (type.assembling()) {
            renderRecipe(graphics);
        }
        if (type.fluidInput()) {
            int x = leftPos + FLUID_X;
            int y = topPos + FLUID_Y;
            graphics.fill(x, y, x + 14, y + FLUID_HEIGHT, 0xFF373737);
            graphics.fill(x + 1, y + 1, x + 13, y + FLUID_HEIGHT - 1, 0xFF1E1E1E);
            if (menu.fluid() != net.minecraft.world.level.material.Fluids.EMPTY && menu.fluidAmount() > 0) {
                int height = Math.max(1, (FLUID_HEIGHT - 2) * menu.fluidAmount() / ProcessingMachineBlockEntity.FLUID_CAPACITY);
                graphics.fill(x + 1, y + FLUID_HEIGHT - 1 - height, x + 13, y + FLUID_HEIGHT - 1, de.craftorio.registry.ModFluids.color(menu.fluid()));
            }
        }
        if (type.usesFuel()) {
            int flame = (int) (13 * menu.burnFraction());
            int x = leftPos + ProcessingMachineMenu.FUEL_X + 1;
            int y = topPos + ProcessingMachineMenu.FUEL_Y - 16;
            graphics.fill(x, y, x + 14, y + 13, 0xFF373737);
            if (flame > 0) {
                graphics.fillGradient(x + 1, y + 13 - flame, x + 13, y + 13, 0xFFFFD54F, 0xFFE0801F);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (type.fluidInput() && isHovering(FLUID_X, FLUID_Y, 14, FLUID_HEIGHT, mouseX, mouseY)) {
            MachineRecipe recipe = selectedRecipe();
            Component text;
            if (menu.fluid() != net.minecraft.world.level.material.Fluids.EMPTY && menu.fluidAmount() > 0) {
                text = Component.translatable("craftorio.fluid.contents", new net.neoforged.neoforge.fluids.FluidStack(menu.fluid(), 1).getHoverName(),
                        menu.fluidAmount(), ProcessingMachineBlockEntity.FLUID_CAPACITY);
            } else if (recipe != null && recipe.needsFluid()) {
                text = Component.translatable("craftorio.fluid.wanted", recipe.fluids().get(0).getHoverName(), recipe.fluids().get(0).getAmount());
            } else {
                text = Component.translatable("craftorio.fluid.empty", ProcessingMachineBlockEntity.FLUID_CAPACITY);
            }
            graphics.renderTooltip(font, text, mouseX, mouseY);
        }
    }

    private void renderRecipe(GuiGraphics graphics) {
        MachineRecipe recipe = selectedRecipe();
        graphics.fill(leftPos + 24, topPos + 18, leftPos + 130, topPos + 36, 0xFF8B8B8B);
        if (recipe == null) {
            graphics.drawString(font, Component.translatable("craftorio.gui.no_recipe"), leftPos + 30, topPos + 23, 0xFFFFFF, false);
            return;
        }
        graphics.renderItem(recipe.result(), leftPos + 26, topPos + 19);
        graphics.drawString(font, recipe.result().getHoverName(), leftPos + 46, topPos + 23, 0xFFFFFF, false);
        // Ghost icons show which ingredient each input slot expects.
        List<SizedIngredient> ingredients = recipe.ingredients();
        for (int i = 0; i < ingredients.size() && i < type.inputSlots(); i++) {
            if (!menu.slots.get(i).hasItem()) {
                ItemStack[] options = ingredients.get(i).getItems();
                if (options.length > 0) {
                    int x = leftPos + ProcessingMachineMenu.inputX(type, i);
                    int y = topPos + ProcessingMachineMenu.inputY(type);
                    graphics.renderItem(options[0], x, y);
                    graphics.renderItemDecorations(font, options[0], x, y, String.valueOf(ingredients.get(i).count()));
                    graphics.fill(x, y, x + 16, y + 16, 300, 0x998B8B8B);
                }
            }
        }
    }

    private @Nullable MachineRecipe selectedRecipe() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        int index = menu.selectedRecipeIndex();
        List<RecipeHolder<MachineRecipe>> recipes = ProcessingMachineBlockEntity.assemblerRecipes(minecraft.level);
        return index >= 0 && index < recipes.size() ? recipes.get(index).value() : null;
    }
}
