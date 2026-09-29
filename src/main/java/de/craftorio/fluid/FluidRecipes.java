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
    /**
     * @param itemOut empty if the recipe makes fluids only; @param fluidsOut the fluids it makes, in the order of the output tanks (up to three)
     * @param keepsFirst the first item ingredient is not used up (the seed comes back from the plant)
     */
    public record Recipe(ResourceLocation id, FluidMachineType machine, List<ItemStack> itemsIn, List<FluidStack> fluidsIn,
                         ItemStack itemOut, List<FluidStack> fluidsOut, int ticks, boolean keepsFirst) {
    }

    private static List<Recipe> all;

    private FluidRecipes() {
    }

    /** Built on first use: items and fluids exist only after registration. */
    public static synchronized List<Recipe> all() {
        if (all == null) {
            all = List.of(
                    recipe("oil/basic_oil_processing", FluidMachineType.OIL_REFINERY, List.of(), List.of(new FluidStack(ModFluids.CRUDE_OIL.get(), 100)),
                            ItemStack.EMPTY, List.of(new FluidStack(ModFluids.PETROLEUM_GAS.get(), 45)), 100),
                    recipe("chem/plastic_bar", FluidMachineType.CHEMICAL_PLANT, List.of(new ItemStack(Items.COAL, 1)),
                            List.of(new FluidStack(ModFluids.PETROLEUM_GAS.get(), 20)),
                            new ItemStack(ModItems.PLASTIC_BAR.get(), 2), List.of(), 20),
                    recipe("chem/sulfur", FluidMachineType.CHEMICAL_PLANT, List.of(),
                            List.of(new FluidStack(Fluids.WATER, 30), new FluidStack(ModFluids.PETROLEUM_GAS.get(), 30)),
                            new ItemStack(ModItems.SULFUR.get(), 2), List.of(), 20),
                    recipe("chem/sulfuric_acid", FluidMachineType.CHEMICAL_PLANT,
                            List.of(new ItemStack(ModItems.SULFUR.get(), 5), new ItemStack(Items.IRON_INGOT, 1)),
                            List.of(new FluidStack(Fluids.WATER, 100)),
                            ItemStack.EMPTY, List.of(new FluidStack(ModFluids.SULFURIC_ACID.get(), 50)), 20),
                    recipe("chem/battery", FluidMachineType.CHEMICAL_PLANT,
                            List.of(new ItemStack(Items.IRON_INGOT, 1), new ItemStack(Items.COPPER_INGOT, 1)),
                            List.of(new FluidStack(ModFluids.SULFURIC_ACID.get(), 20)),
                            new ItemStack(ModItems.BATTERY.get()), List.of(), 80),
                    // Wiki 1.1: advanced oil processing, cracking, lubricant and solid fuel
                    recipe("oil/advanced_oil_processing", FluidMachineType.OIL_REFINERY, List.of(),
                            List.of(new FluidStack(ModFluids.CRUDE_OIL.get(), 100), new FluidStack(Fluids.WATER, 50)), ItemStack.EMPTY,
                            List.of(new FluidStack(ModFluids.HEAVY_OIL.get(), 25), new FluidStack(ModFluids.LIGHT_OIL.get(), 45),
                                    new FluidStack(ModFluids.PETROLEUM_GAS.get(), 55)), 100),
                    recipe("chem/heavy_oil_cracking", FluidMachineType.CHEMICAL_PLANT, List.of(),
                            List.of(new FluidStack(ModFluids.HEAVY_OIL.get(), 40), new FluidStack(Fluids.WATER, 30)), ItemStack.EMPTY,
                            List.of(new FluidStack(ModFluids.LIGHT_OIL.get(), 30)), 40),
                    recipe("chem/light_oil_cracking", FluidMachineType.CHEMICAL_PLANT, List.of(),
                            List.of(new FluidStack(ModFluids.LIGHT_OIL.get(), 30), new FluidStack(Fluids.WATER, 30)), ItemStack.EMPTY,
                            List.of(new FluidStack(ModFluids.PETROLEUM_GAS.get(), 20)), 40),
                    recipe("chem/lubricant", FluidMachineType.CHEMICAL_PLANT, List.of(),
                            List.of(new FluidStack(ModFluids.HEAVY_OIL.get(), 10)), ItemStack.EMPTY,
                            List.of(new FluidStack(ModFluids.LUBRICANT.get(), 10)), 20),
                    recipe("chem/solid_fuel_from_light_oil", FluidMachineType.CHEMICAL_PLANT, List.of(),
                            List.of(new FluidStack(ModFluids.LIGHT_OIL.get(), 10)), new ItemStack(ModItems.SOLID_FUEL.get()), List.of(), 40),
                    recipe("chem/solid_fuel_from_heavy_oil", FluidMachineType.CHEMICAL_PLANT, List.of(),
                            List.of(new FluidStack(ModFluids.HEAVY_OIL.get(), 20)), new ItemStack(ModItems.SOLID_FUEL.get()), List.of(), 40),
                    recipe("chem/solid_fuel_from_petroleum_gas", FluidMachineType.CHEMICAL_PLANT, List.of(),
                            List.of(new FluidStack(ModFluids.PETROLEUM_GAS.get(), 20)), new ItemStack(ModItems.SOLID_FUEL.get()), List.of(), 40),
                    // The greenhouse (docs/FACTORIO-UMBAU.md §7): seed + water → plants, in ticks (seconds × 20).
                    crop("farm/wheat", Items.WHEAT_SEEDS, 50, new ItemStack(Items.WHEAT, 3), 400, true),
                    crop("farm/pumpkin", Items.PUMPKIN_SEEDS, 50, new ItemStack(Items.PUMPKIN), 600, false),
                    crop("farm/carrot", Items.CARROT, 50, new ItemStack(Items.CARROT, 3), 400, false),
                    crop("farm/potato", Items.POTATO, 50, new ItemStack(Items.POTATO, 3), 400, false),
                    crop("farm/sugar_cane", Items.SUGAR_CANE, 50, new ItemStack(Items.SUGAR_CANE, 2), 300, false),
                    crop("farm/tree", Items.OAK_SAPLING, 100, new ItemStack(Items.OAK_LOG, 4), 800, true));
        }
        return all;
    }

    private static Recipe recipe(String id, FluidMachineType machine, List<ItemStack> itemsIn, List<FluidStack> fluidsIn,
                                 ItemStack itemOut, List<FluidStack> fluidsOut, int ticks) {
        return new Recipe(Craftorio.id(id), machine, itemsIn, fluidsIn, itemOut, fluidsOut, ticks, false);
    }

    private static Recipe crop(String id, net.minecraft.world.item.Item seed, int water, ItemStack harvest, int ticks, boolean keepsSeed) {
        return new Recipe(Craftorio.id(id), FluidMachineType.GREENHOUSE, List.of(new ItemStack(seed)),
                List.of(new FluidStack(Fluids.WATER, water)), harvest, List.of(), ticks, keepsSeed);
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
