package de.craftorio.fluid;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModFluids;
import de.craftorio.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The recipes of the chemical plant and the oil refinery (Factorio 1.1 amounts). Ticks are Factorio seconds × 20 at
 * speed 1. Items go into the machine's item slots in the order listed (ingredient n in slot n); fluids into the
 * input tanks in the order listed. The research that unlocks a recipe names it by its id, e.g. {@code craftorio:chem/plastic_bar}.
 */
public final class FluidRecipes {
    /** @param itemOut empty if the recipe makes a fluid; @param fluidOut empty if it makes an item */
    public record Recipe(ResourceLocation id, FluidMachineType machine, List<ItemStack> itemsIn, List<FluidStack> fluidsIn,
                         ItemStack itemOut, FluidStack fluidOut, int ticks) {
    }

    private static List<Recipe> all;

    private FluidRecipes() {
    }

    /** Built on first use: items and fluids exist only after registration. */
    public static synchronized List<Recipe> all() {
        if (all == null) {
            all = List.of(
                    recipe("oil/basic_oil_processing", FluidMachineType.OIL_REFINERY, List.of(), List.of(new FluidStack(ModFluids.CRUDE_OIL.get(), 100)),
                            ItemStack.EMPTY, new FluidStack(ModFluids.PETROLEUM_GAS.get(), 45), 100),
                    recipe("chem/plastic_bar", FluidMachineType.CHEMICAL_PLANT, List.of(new ItemStack(Items.COAL, 1)),
                            List.of(new FluidStack(ModFluids.PETROLEUM_GAS.get(), 20)),
                            new ItemStack(ModItems.PLASTIC_BAR.get(), 2), FluidStack.EMPTY, 20),
                    recipe("chem/sulfur", FluidMachineType.CHEMICAL_PLANT, List.of(),
                            List.of(new FluidStack(Fluids.WATER, 30), new FluidStack(ModFluids.PETROLEUM_GAS.get(), 30)),
                            new ItemStack(ModItems.SULFUR.get(), 2), FluidStack.EMPTY, 20),
                    recipe("chem/sulfuric_acid", FluidMachineType.CHEMICAL_PLANT,
                            List.of(new ItemStack(ModItems.SULFUR.get(), 5), new ItemStack(Items.IRON_INGOT, 1)),
                            List.of(new FluidStack(Fluids.WATER, 100)),
                            ItemStack.EMPTY, new FluidStack(ModFluids.SULFURIC_ACID.get(), 50), 20),
                    recipe("chem/battery", FluidMachineType.CHEMICAL_PLANT,
                            List.of(new ItemStack(Items.IRON_INGOT, 1), new ItemStack(Items.COPPER_INGOT, 1)),
                            List.of(new FluidStack(ModFluids.SULFURIC_ACID.get(), 20)),
                            new ItemStack(ModItems.BATTERY.get()), FluidStack.EMPTY, 80));
        }
        return all;
    }

    private static Recipe recipe(String id, FluidMachineType machine, List<ItemStack> itemsIn, List<FluidStack> fluidsIn,
                                 ItemStack itemOut, FluidStack fluidOut, int ticks) {
        return new Recipe(Craftorio.id(id), machine, itemsIn, fluidsIn, itemOut, fluidOut, ticks);
    }

    /** The recipes of one machine type in a fixed order shared by server and client. */
    public static List<Recipe> forMachine(FluidMachineType machine) {
        return all().stream().filter(recipe -> recipe.machine() == machine)
                .sorted(Comparator.comparing(recipe -> recipe.id().toString())).toList();
    }

    public static Optional<Recipe> byId(ResourceLocation id) {
        return all().stream().filter(recipe -> recipe.id().equals(id)).findFirst();
    }
}
