package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.world.terrain.FactoryChunkGenerator;
import de.craftorio.world.terrain.FactoryTerrain;
import de.craftorio.world.OreFieldFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

/** The Craftorio world type (preset, biome source, terrain). Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldGameTests {
    private static final String EMPTY = "empty";
    private static final long SEED = 20260101L;

    private WorldGameTests() {
    }

    private static FactoryChunkGenerator generator(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        WorldPreset preset = registries.registryOrThrow(Registries.WORLD_PRESET).get(Craftorio.id("factory"));
        if (preset == null) {
            throw new AssertionError("world preset craftorio:factory is missing");
        }
        LevelStem overworld = preset.overworld().orElseThrow();
        if (!(overworld.generator() instanceof FactoryChunkGenerator generator)) {
            throw new AssertionError("the overworld of the preset is not a factory generator: " + overworld.generator());
        }
        return generator;
    }

    private static RandomState randomState(GameTestHelper helper, FactoryChunkGenerator generator) {
        return RandomState.create(generator.generatorSettings().value(),
                helper.getLevel().registryAccess().lookupOrThrow(Registries.NOISE), SEED);
    }

    @GameTest(template = EMPTY)
    public static void presetSurfaceAroundSpawnIsGround(GameTestHelper helper) {
        FactoryChunkGenerator generator = generator(helper);
        RandomState random = randomState(helper, generator);
        LevelHeightAccessor access = LevelHeightAccessor.create(-64, 384);
        for (int x = -176; x <= 176; x += 16) {
            for (int z = -176; z <= 176; z += 16) {
                if (x * x + z * z > 190 * 190) {
                    continue;
                }
                int top = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, access, random) - 1;
                helper.assertTrue(top == FactoryTerrain.GROUND, "the surface at " + x + "," + z + " is y=" + top + ", not "
                        + FactoryTerrain.GROUND);
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void presetHasManyLandBiomes(GameTestHelper helper) {
        FactoryChunkGenerator generator = generator(helper);
        RandomState random = randomState(helper, generator);
        Set<String> biomes = new HashSet<>();
        for (int x = -512; x < 512; x += 16) {
            for (int z = -512; z < 512; z += 16) {
                Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(x >> 2, 16, z >> 2, random.sampler());
                biomes.add(biome.unwrapKey().orElseThrow().location().toString());
            }
        }
        helper.assertTrue(biomes.size() >= 6, "only " + biomes.size() + " biomes in 1024 x 1024 blocks: " + biomes);
        for (String biome : biomes) {
            helper.assertTrue(!biome.contains("ocean") && !biome.contains("beach") && !biome.contains("river"),
                    "water biome " + biome);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void starterFieldIgnoresTreesStandingOnTheGround(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
                helper.setBlock(new BlockPos(x, 6, z), Blocks.AIR); // the barrier ceiling of the test box would count as ground
            }
        }
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(3, 4, 3), Blocks.OAK_LEAVES);
        BlockPos column = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.assertTrue(OreFieldFeature.groundY(helper.getLevel(), column.getX(), column.getZ()) == column.getY(),
                "the ground under the tree is not found");
        int placed = OreFieldFeature.placeField(helper.getLevel(), column.above(), Blocks.IRON_BLOCK.defaultBlockState(), 9,
                RandomSource.create(1));
        helper.assertTrue(placed == 9, "placed " + placed + " blocks of 9");
        helper.assertBlock(new BlockPos(3, 1, 3), block -> block == Blocks.IRON_BLOCK, () -> "field under the tree");
        helper.assertBlock(new BlockPos(3, 2, 3), block -> block == Blocks.AIR, () -> "trunk removed above the field");
        helper.succeed();
    }
}
