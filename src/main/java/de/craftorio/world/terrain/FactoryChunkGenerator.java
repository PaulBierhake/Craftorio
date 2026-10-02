package de.craftorio.world.terrain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.Set;

/**
 * The chunk generator of the Craftorio world type: a noise generator whose surface is the factory height, without the
 * vanilla carvers (the underground belongs to the Craftorio layers) and without the structures that dig into the ground
 * or need the sea.
 */
public final class FactoryChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<FactoryChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings)
    ).apply(instance, instance.stable(FactoryChunkGenerator::new)));

    /** Structure sets (vanilla names) that do not generate in a Craftorio world. */
    private static final Set<String> EXCLUDED_STRUCTURES = Set.of("mineshafts", "strongholds", "ancient_cities",
            "trial_chambers", "buried_treasures", "ocean_monuments", "ocean_ruins", "shipwrecks");

    public FactoryChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        super.applyBiomeDecoration(level, chunk, structures);
        FactoryFinish.apply(level, chunk);
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> sets, RandomState random, long seed) {
        return ChunkGeneratorStructureState.createForFlat(random, seed, biomeSource, sets.listElements()
                .filter(set -> !(set.key().location().getNamespace().equals("minecraft")
                        && EXCLUDED_STRUCTURES.contains(set.key().location().getPath())))
                .map(set -> (Holder<StructureSet>) set));
    }
}
