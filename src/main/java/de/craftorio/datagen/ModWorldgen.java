package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModDensityFunctions;
import de.craftorio.world.terrain.FactoryHeight;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/** Noise and noise settings of the Craftorio world type (data/craftorio/worldgen). */
final class ModWorldgen {
    static final ResourceKey<NormalNoise.NoiseParameters> TERRAIN_SEED = ResourceKey.create(Registries.NOISE, Craftorio.id("terrain_seed"));
    static final ResourceKey<NoiseGeneratorSettings> FACTORY = ResourceKey.create(Registries.NOISE_SETTINGS, Craftorio.id("factory"));

    private ModWorldgen() {
    }

    static void bootstrapNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        context.register(TERRAIN_SEED, new NormalNoise.NoiseParameters(-3, 1.0));
    }

    /**
     * The vanilla overworld settings (climate for the biomes, surface rules, spawn), with a surface that is only the
     * factory height: no caves, no aquifers, no ore veins, no noise terrain.
     */
    static void bootstrapNoiseSettings(BootstrapContext<NoiseGeneratorSettings> context) {
        NoiseGeneratorSettings vanilla = NoiseGeneratorSettings.overworld(context, false, false);
        Holder<NormalNoise.NoiseParameters> seed = context.lookup(Registries.NOISE).getOrThrow(TERRAIN_SEED);
        DensityFunction height = new FactoryHeight(new DensityFunction.NoiseHolder(seed));
        DensityFunction zero = DensityFunctions.zero();
        NoiseRouter v = vanilla.noiseRouter();
        NoiseRouter router = new NoiseRouter(zero, zero, zero, zero,
                v.temperature(), v.vegetation(), v.continents(), v.erosion(), v.depth(), v.ridges(),
                height, height, zero, zero, zero);
        context.register(FACTORY, new NoiseGeneratorSettings(vanilla.noiseSettings(), vanilla.defaultBlock(), vanilla.defaultFluid(),
                router, vanilla.surfaceRule(), List.of(), 64, false, false, false, false));
    }
}
