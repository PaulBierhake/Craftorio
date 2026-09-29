package de.craftorio.compat.emi;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.research.Researches;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModRecipes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/** EMI integration: assembler recipes and the workbench blueprints. Only loaded when EMI is installed. */
@EmiEntrypoint
public final class CraftorioEmiPlugin implements EmiPlugin {
    static final EmiRecipeCategory ASSEMBLING = new EmiRecipeCategory(Craftorio.id("assembling"), EmiStack.of(ModBlocks.ASSEMBLER.get()));
    static final EmiRecipeCategory BLUEPRINT = new EmiRecipeCategory(Craftorio.id("blueprint"), EmiStack.of(ModBlocks.WORKBENCH.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ASSEMBLING);
        registry.addCategory(BLUEPRINT);
        registry.addWorkstation(ASSEMBLING, EmiStack.of(ModBlocks.ASSEMBLER.get()));
        registry.addWorkstation(BLUEPRINT, EmiStack.of(ModBlocks.WORKBENCH.get()));

        for (RecipeHolder<MachineRecipe> holder : registry.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLING.get())) {
            registry.addRecipe(new MachineEmiRecipe(ASSEMBLING, holder.id(), holder.value(), gateOf(holder.id())));
        }
        if (Minecraft.getInstance().level != null) {
            for (Holder.Reference<Blueprint> holder : Blueprints.sorted(Minecraft.getInstance().level.registryAccess())) {
                Component research = Researches.gate(Minecraft.getInstance().level.registryAccess(), Blueprints.id(holder))
                        .map(id -> (Component) Component.translatable("craftorio.research." + id.substring(id.indexOf(':') + 1))).orElse(null);
                registry.addRecipe(new BlueprintEmiRecipe(holder.key().location(), holder.value(), research));
            }
        }
    }

    /** The research that unlocks a recipe, or null if it is available from the start. */
    private static Component gateOf(ResourceLocation id) {
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        return Researches.gate(Minecraft.getInstance().level.registryAccess(), id.toString())
                .map(research -> (Component) Component.translatable("craftorio.research." + research.substring(research.indexOf(':') + 1))).orElse(null);
    }

    static List<EmiIngredient> ingredients(List<SizedIngredient> ingredients) {
        return ingredients.stream().map(ingredient -> EmiIngredient.of(ingredient.ingredient(), ingredient.count())).toList();
    }

    /** Inputs in a row, an arrow with the processing time, the result. */
    private static final class MachineEmiRecipe extends BasicEmiRecipe {
        private final int time;

        private final Component research;

        MachineEmiRecipe(EmiRecipeCategory category, ResourceLocation id, MachineRecipe recipe, Component research) {
            super(category, id, 18 * MachineRecipe.MAX_INGREDIENTS + 60, research == null ? 26 : 38);
            this.research = research;
            inputs = ingredients(recipe.ingredients());
            outputs = List.of(EmiStack.of(recipe.result()));
            time = recipe.time();
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            for (int i = 0; i < inputs.size(); i++) {
                widgets.addSlot(inputs.get(i), i * 18, 4);
            }
            int arrowX = 18 * MachineRecipe.MAX_INGREDIENTS + 4;
            widgets.addFillingArrow(arrowX, 5, time * 50).tooltipText(List.of(
                    Component.translatable("craftorio.emi.time", String.format(java.util.Locale.ROOT, "%.1f", time / 20.0))));
            widgets.addSlot(outputs.get(0), arrowX + 30, 0).large(true).recipeContext(this);
            if (research != null) {
                widgets.addText(Component.translatable("craftorio.emi.research", research), 0, 28, 0xFF404040, false);
            }
        }
    }

    /** Materials built at a workbench; tier, price and key items are shown as text. */
    private static final class BlueprintEmiRecipe extends BasicEmiRecipe {
        private final Blueprint blueprint;
        private final Component researchName;

        BlueprintEmiRecipe(ResourceLocation id, Blueprint blueprint, Component researchName) {
            super(BLUEPRINT, ResourceLocation.fromNamespaceAndPath(Craftorio.MOD_ID, "/blueprint/" + id.getPath()), 150, 40);
            this.blueprint = blueprint;
            this.researchName = researchName;
            inputs = ingredients(blueprint.ingredients());
            outputs = List.of(EmiStack.of(blueprint.result()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            for (int i = 0; i < inputs.size(); i++) {
                widgets.addSlot(inputs.get(i), i * 18, 0);
            }
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 76, 1);
            widgets.addSlot(outputs.get(0), 104, 0).recipeContext(this);
            Component gate = researchName == null ? Component.translatable("craftorio.emi.free") : Component.translatable("craftorio.emi.research", researchName);
            widgets.addText(gate, 0, 24, 0xFF404040, false);
        }
    }
}
