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

import com.mojang.datafixers.util.Pair;
import de.craftorio.world.terrain.FactoryChunkGenerator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Noise and noise settings of the Craftorio world type (data/craftorio/worldgen). */
final class ModWorldgen {
    static final ResourceKey<NormalNoise.NoiseParameters> TERRAIN_SEED = ResourceKey.create(Registries.NOISE, Craftorio.id("terrain_seed"));
    static final ResourceKey<NoiseGeneratorSettings> FACTORY = ResourceKey.create(Registries.NOISE_SETTINGS, Craftorio.id("factory"));

    static final ResourceKey<WorldPreset> FACTORY_PRESET = ResourceKey.create(Registries.WORLD_PRESET, Craftorio.id("factory"));

    // temperature bands (cold … hot) and humidity bands (dry … very wet) of the biome table in docs/WELT-UMBAU.md §5
    private static final float[] TEMPERATURE = {-1.0f, -0.45f, -0.15f, 0.2f, 0.55f, 1.0f};
    private static final float[] HUMIDITY = {-1.0f, -0.35f, 0.1f, 0.3f, 1.0f};

    @SuppressWarnings("unchecked")
    private static final ResourceKey<Biome>[][] BIOMES = new ResourceKey[][]{
            // dry
            {Biomes.SNOWY_PLAINS, Biomes.PLAINS, Biomes.PLAINS, Biomes.SAVANNA, Biomes.DESERT},
            // medium
            {Biomes.SNOWY_TAIGA, Biomes.TAIGA, Biomes.FOREST, Biomes.SAVANNA, Biomes.DESERT},
            // wet
            {Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.BIRCH_FOREST, Biomes.SPARSE_JUNGLE, Biomes.DESERT},
            // very wet
            {Biomes.TAIGA, Biomes.DARK_FOREST, Biomes.SWAMP, Biomes.SPARSE_JUNGLE, Biomes.SAVANNA},
    };

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

    /** Land biomes only: the choice depends on temperature and humidity, all other climate values cover everything. */
    static MultiNoiseBiomeSource biomeSource(BootstrapContext<?> context) {
        var biomes = context.lookup(Registries.BIOME);
        Climate.Parameter everything = Climate.Parameter.span(-2.0f, 2.0f);
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries = new ArrayList<>();
        for (int humidity = 0; humidity < BIOMES.length; humidity++) {
            for (int temperature = 0; temperature < BIOMES[humidity].length; temperature++) {
                Climate.ParameterPoint point = Climate.parameters(
                        Climate.Parameter.span(TEMPERATURE[temperature], TEMPERATURE[temperature + 1]),
                        Climate.Parameter.span(HUMIDITY[humidity], HUMIDITY[humidity + 1]),
                        everything, everything, everything, everything, 0.0f);
                entries.add(Pair.of(point, biomes.getOrThrow(BIOMES[humidity][temperature])));
            }
        }
        return MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(entries));
    }

    /** The "normal" world type with the overworld replaced by the factory world (nether and end as in vanilla). */
    static void bootstrapPresets(BootstrapContext<WorldPreset> context) {
        var dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        var noiseSettings = context.lookup(Registries.NOISE_SETTINGS);
        var netherBiomes = context.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        Map<ResourceKey<LevelStem>, LevelStem> stems = new LinkedHashMap<>();
        stems.put(LevelStem.OVERWORLD, new LevelStem(dimensionTypes.getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                new FactoryChunkGenerator(biomeSource(context), noiseSettings.getOrThrow(FACTORY))));
        stems.put(LevelStem.NETHER, new LevelStem(dimensionTypes.getOrThrow(BuiltinDimensionTypes.NETHER),
                new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(
                        netherBiomes.getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER)),
                        noiseSettings.getOrThrow(NoiseGeneratorSettings.NETHER))));
        stems.put(LevelStem.END, new LevelStem(dimensionTypes.getOrThrow(BuiltinDimensionTypes.END),
                new NoiseBasedChunkGenerator(TheEndBiomeSource.create(context.lookup(Registries.BIOME)),
                        noiseSettings.getOrThrow(NoiseGeneratorSettings.END))));
        context.register(FACTORY_PRESET, new WorldPreset(stems));
    }
}
