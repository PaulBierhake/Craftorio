package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.economy.Economy;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.registry.ModRecipes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Economy balance: every processing step must pay – selling the product earns more than selling its inputs. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BalanceGameTests {
    /** Machine steps must add at least this much value on top of their inputs. */
    private static final double MIN_MACHINE_MARGIN = 1.1;

    private BalanceGameTests() {
    }

    @GameTest(template = "empty")
    public static void everyProcessingStepAddsValue(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (RecipeType<MachineRecipe> type : List.of(ModRecipes.PRESSING.get(), ModRecipes.ASSEMBLING.get())) {
            for (RecipeHolder<MachineRecipe> holder : recipes.getAllRecipesFor(type)) {
                MachineRecipe recipe = holder.value();
                if (Economy.unitPrice(recipe.result()) == 0) {
                    continue; // consumables such as tower ammunition are not traded
                }
                long inputs = 0;
                boolean priced = true;
                for (SizedIngredient ingredient : recipe.ingredients()) {
                    long unit = Economy.unitPrice(firstItem(ingredient));
                    priced &= unit > 0;
                    inputs += unit * ingredient.count();
                }
                long output = Economy.unitPrice(recipe.result()) * recipe.result().getCount();
                if (!priced) {
                    problems.add(holder.id() + " has unpriced ingredients");
                } else if (output < inputs * MIN_MACHINE_MARGIN) {
                    problems.add(holder.id() + ": " + output + " < " + inputs + " × " + MIN_MACHINE_MARGIN);
                }
                checked++;
            }
        }
        for (RecipeHolder<SmeltingRecipe> holder : recipes.getAllRecipesFor(RecipeType.SMELTING)) {
            ItemStack[] inputs = holder.value().getIngredients().get(0).getItems();
            ItemStack result = holder.value().getResultItem(helper.getLevel().registryAccess());
            long in = inputs.length == 0 ? 0 : Economy.unitPrice(inputs[0]);
            long out = Economy.unitPrice(result) * result.getCount();
            if (in > 0 && out > 0) {
                checked++;
                if (out < in) {
                    problems.add(holder.id() + ": " + out + " < " + in);
                }
            }
        }
        helper.assertTrue(checked >= 15, "only " + checked + " recipes checked");
        helper.assertTrue(problems.isEmpty(), "unbalanced: " + String.join("; ", problems));
        helper.succeed();
    }

    private static ItemStack firstItem(SizedIngredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }
}
