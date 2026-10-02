package de.craftorio.registry;

import com.mojang.serialization.MapCodec;
import de.craftorio.Craftorio;
import de.craftorio.world.terrain.FactoryHeight;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDensityFunctions {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Craftorio.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<FactoryHeight>> FACTORY_HEIGHT =
            TYPES.register("factory_height", () -> FactoryHeight.DATA_CODEC);

    private ModDensityFunctions() {
    }
}
