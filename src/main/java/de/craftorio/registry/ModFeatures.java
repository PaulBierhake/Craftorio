package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.world.OreFieldConfiguration;
import de.craftorio.world.OreFieldFeature;
import de.craftorio.world.cave.CaveLayerFeature;
import net.minecraft.core.registries.Registries;
import de.craftorio.world.rock.RockFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Craftorio.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<OreFieldConfiguration>> ORE_FIELD = FEATURES.register("ore_field", OreFieldFeature::new);

    public static final DeferredHolder<Feature<?>, CaveLayerFeature> CAVE_LAYERS = FEATURES.register("cave_layers", CaveLayerFeature::new);

    public static final DeferredHolder<Feature<?>, RockFeature> ROCKS = FEATURES.register("rocks", () -> new RockFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }
}
