package de.craftorio.world.terrain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.craftorio.CraftorioConfig;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function of the factory world: solid up to the height {@link FactoryTerrain} gives for the column, air above.
 * It is evaluated per block, so the surface is exact and the edges are vertical. The seed comes from a noise, which the
 * level seeds like any other noise.
 */
public final class FactoryHeight implements DensityFunction.SimpleFunction {
    public static final MapCodec<FactoryHeight> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(function -> function.seedNoise)
    ).apply(instance, FactoryHeight::new));
    public static final KeyDispatchDataCodec<FactoryHeight> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    /** Terrains by their settings: the density function is copied for every chunk, the terrain behind it must not be. */
    private static final java.util.Map<String, FactoryTerrain> TERRAINS = new java.util.LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, FactoryTerrain> eldest) {
            return size() > 6;
        }
    };

    private final DensityFunction.NoiseHolder seedNoise;
    private volatile FactoryTerrain terrain;

    public FactoryHeight(DensityFunction.NoiseHolder seedNoise) {
        this.seedNoise = seedNoise;
    }

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        double height = terrain().height(context.blockX(), context.blockZ());
        return Math.max(-1.0, Math.min(1.0, height - context.blockY() + 0.5));
    }

    private FactoryTerrain terrain() {
        FactoryTerrain built = terrain;
        if (built == null) {
            synchronized (this) {
                built = terrain;
                if (built == null) {
                    // an unseeded noise (the datapack's own copy) has none; the level's copy gives the seed
                    long seed = Double.doubleToLongBits(seedNoise.getValue(0.0, 0.0, 0.0))
                            ^ Double.doubleToLongBits(seedNoise.getValue(1000.0, 0.0, 1000.0)) * 31;
                    built = shared(seed, CraftorioConfig.terrainPlateaus(), CraftorioConfig.spawnRadius());
                    terrain = built;
                }
            }
        }
        return built;
    }

    private static FactoryTerrain shared(long seed, FactoryTerrain.Plateaus plateaus, int spawnRadius) {
        String key = seed + "/" + plateaus + "/" + spawnRadius;
        synchronized (TERRAINS) {
            return TERRAINS.computeIfAbsent(key, k -> new FactoryTerrain(seed, plateaus, spawnRadius));
        }
    }

    @Override
    public DensityFunction mapAll(DensityFunction.Visitor visitor) {
        return visitor.apply(new FactoryHeight(visitor.visitNoise(seedNoise)));
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
