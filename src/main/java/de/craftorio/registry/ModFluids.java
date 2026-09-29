package de.craftorio.registry;

import de.craftorio.Craftorio;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The fluids of the factory. They only live in pipes, tanks and machines: there are no fluid blocks or buckets in the
 * world. Water is the vanilla fluid. The oil fluids are used from package U7 on.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Craftorio.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Craftorio.MOD_ID);

    /** Name → tint colour (ARGB) used to draw the fluid in GUIs. */
    public static final Map<String, Integer> COLORS = new LinkedHashMap<>();

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM = fluid("steam", 0xFFDCE6EE, -100, 200);
    /** 500 °C steam from the heat exchangers: only steam turbines take it. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HOT_STEAM = fluid("hot_steam", 0xFFF4D9C6, -100, 200);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CRUDE_OIL = fluid("crude_oil", 0xFF1A1420, 900, 3000);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HEAVY_OIL = fluid("heavy_oil", 0xFF6B2A2A, 950, 3500);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIGHT_OIL = fluid("light_oil", 0xFFD49A2A, 850, 2000);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PETROLEUM_GAS = fluid("petroleum_gas", 0xFF7E9C6A, -50, 300);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SULFURIC_ACID = fluid("sulfuric_acid", 0xFFD8E03A, 1200, 1500);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LUBRICANT = fluid("lubricant", 0xFF2E7D32, 900, 2500);

    private ModFluids() {
    }

    private static DeferredHolder<Fluid, BaseFlowingFluid.Source> fluid(String name, int color, int density, int viscosity) {
        COLORS.put(name, color);
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register(name,
                () -> new FluidType(FluidType.Properties.create().density(density).viscosity(viscosity)));
        // The still and flowing variants are needed by the flowing-fluid class but never appear as blocks.
        DeferredHolder<Fluid, BaseFlowingFluid.Source>[] source = new DeferredHolder[1];
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing>[] flowing = new DeferredHolder[1];
        BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(type, () -> source[0].get(), () -> flowing[0].get());
        source[0] = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties));
        flowing[0] = FLUIDS.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(properties));
        return source[0];
    }

    /** Tint colour of a fluid in GUIs; water is blue. */
    public static int color(Fluid fluid) {
        var key = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
        if (key.getNamespace().equals(Craftorio.MOD_ID)) {
            return COLORS.getOrDefault(key.getPath().replace("_flowing", ""), 0xFFFFFFFF);
        }
        return 0xFF3F76E4;
    }
}
