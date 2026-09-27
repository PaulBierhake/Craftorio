package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.world.OreFieldConfiguration;
import de.craftorio.world.OreFieldFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Craftorio.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<OreFieldConfiguration>> ORE_FIELD = FEATURES.register("ore_field", OreFieldFeature::new);

    private ModFeatures() {
    }
}
