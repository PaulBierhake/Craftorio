package de.craftorio.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Field block and size range in blocks; fields grow from min_size near the origin to max_size far away. */
public record OreFieldConfiguration(BlockState state, int minSize, int maxSize) implements FeatureConfiguration {
    public static final Codec<OreFieldConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("state").forGetter(OreFieldConfiguration::state),
            Codec.intRange(1, OreFieldFeature.MAX_SIZE).fieldOf("min_size").forGetter(OreFieldConfiguration::minSize),
            Codec.intRange(1, OreFieldFeature.MAX_SIZE).fieldOf("max_size").forGetter(OreFieldConfiguration::maxSize)
    ).apply(instance, OreFieldConfiguration::new));
}
