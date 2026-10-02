package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.world.terrain.FactoryChunkGenerator;
import de.craftorio.world.terrain.FactoryTerrain;
import de.craftorio.registry.ModBlocks;
import de.craftorio.world.OreFieldFeature;
import de.craftorio.world.tool.WorldTools;
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

    @GameTest(template = EMPTY)
    public static void landfillFillsThreeByThreeOfWater(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(BlockPos.ZERO).offset(96, 0, 0).atY(FactoryTerrain.SEA_LEVEL - 1);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlock(center.offset(dx, -3, dz), Blocks.DIRT.defaultBlockState(), 2);
                for (int y = -2; y <= 0; y++) {
                    level.setBlock(center.offset(dx, y, dz), Blocks.WATER.defaultBlockState(), 2);
                }
                level.setBlock(center.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        int filled = WorldTools.landfill(level, center);
        helper.assertTrue(filled == 9, "filled " + filled + " columns instead of 9");
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.assertTrue(level.getBlockState(center.offset(dx, 0, dz)).is(Blocks.GRASS_BLOCK), "grass on top at " + dx + "," + dz);
                helper.assertTrue(level.getBlockState(center.offset(dx, -1, dz)).is(Blocks.DIRT), "earth below at " + dx + "," + dz);
                helper.assertTrue(level.getBlockState(center.offset(dx, -2, dz)).is(Blocks.DIRT), "earth at the bottom at " + dx + "," + dz);
            }
        }
        helper.assertTrue(level.getBlockState(center.offset(2, 0, 0)).is(Blocks.WATER), "the water around stays");
        helper.assertTrue(level.getBlockState(center.offset(0, 0, 2)).is(Blocks.WATER), "the water around stays");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void cliffExplosivesPlacedAndBlownLowerFiveByFiveOfAPlateau(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(BlockPos.ZERO).offset(160, 0, 0).atY(FactoryTerrain.PLATEAU_ONE);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int top = dx <= 0 ? FactoryTerrain.PLATEAU_ONE : FactoryTerrain.GROUND;
                for (int y = FactoryTerrain.GROUND - 4; y <= FactoryTerrain.PLATEAU_TWO + 2; y++) {
                    var state = y > top ? Blocks.AIR.defaultBlockState() : y == top ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : y > top - 4 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    level.setBlock(center.offset(dx, y - FactoryTerrain.PLATEAU_ONE, dz), state, 2);
                }
            }
        }
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        BlockPos charge = center.above();
        level.setBlock(charge, ModBlocks.CLIFF_EXPLOSIVES.get().defaultBlockState(), 3);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(charge), net.minecraft.core.Direction.UP, charge, false);
        helper.assertTrue(level.getBlockState(charge).useWithoutItem(level, player, hit).consumesAction(), "the charge blows");
        helper.assertTrue(level.getBlockState(charge).isAir(), "the charge is used up");
        for (int dx = -2; dx <= 0; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos column = center.offset(dx, 0, dz);
                for (int y = FactoryTerrain.GROUND + 1; y <= FactoryTerrain.PLATEAU_ONE; y++) {
                    helper.assertTrue(level.getBlockState(column.atY(y)).isAir(), "air at " + dx + "," + y + "," + dz);
                }
                helper.assertTrue(level.getBlockState(column.atY(FactoryTerrain.GROUND)).is(Blocks.GRASS_BLOCK), "grass on the new level");
            }
        }
        helper.assertTrue(!level.getBlockState(center.offset(-3, 0, 0)).isAir(), "outside the 5 x 5 area the plateau stays");
        helper.assertTrue(!level.getBlockState(center.offset(0, 0, 3)).isAir(), "outside the 5 x 5 area the plateau stays");
        BlockPos flat = center.offset(5, 0, 0).atY(FactoryTerrain.GROUND + 1);
        level.setBlock(flat, ModBlocks.CLIFF_EXPLOSIVES.get().defaultBlockState(), 3);
        var flatHit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(flat), net.minecraft.core.Direction.UP, flat, false);
        level.getBlockState(flat).useWithoutItem(level, player, flatHit);
        helper.assertTrue(level.getBlockState(flat).is(ModBlocks.CLIFF_EXPLOSIVES.get()), "without a cliff the charge stays");
        helper.succeed();
    }

    private static int drops(GameTestHelper helper, net.minecraft.world.item.Item item) {
        return helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(8))
                .stream().filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    @GameTest(template = EMPTY)
    public static void bigBoulderMinedWithAPickaxeGivesStoneAndCoal(GameTestHelper helper) {
        for (int dx = 1; dx <= 3; dx++) {
            for (int dz = 1; dz <= 3; dz++) {
                helper.setBlock(new BlockPos(dx, 1, dz), ModBlocks.ROCK.get());
            }
        }
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.ROCK.get());
        // Mock players always count as creative; a fake player is a plain survival player.
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(1, 1, 1))), "the rock breaks");
        for (int dx = 1; dx <= 3; dx++) {
            for (int dz = 1; dz <= 3; dz++) {
                helper.assertBlock(new BlockPos(dx, 1, dz), block -> block != ModBlocks.ROCK.get(), () -> "the whole boulder is gone");
            }
        }
        helper.assertBlock(new BlockPos(2, 2, 2), block -> block != ModBlocks.ROCK.get(), () -> "the top is gone too");
        int stone = drops(helper, net.minecraft.world.item.Items.COBBLESTONE);
        int coal = drops(helper, net.minecraft.world.item.Items.COAL);
        helper.assertTrue(stone >= 24 && stone <= 50, "stone " + stone);
        helper.assertTrue(coal >= 10 && coal <= 25, "coal " + coal);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rockMinedByHandBreaksOnlyOneBlockAndGivesNothing(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.SMALL_ROCK.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SMALL_ROCK.get());
        // Mock players always count as creative; a fake player is a plain survival player.
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(1, 1, 1))), "the rock breaks");
        helper.assertBlock(new BlockPos(2, 1, 1), block -> block == ModBlocks.SMALL_ROCK.get(), () -> "the neighbour stays");
        helper.assertTrue(drops(helper, net.minecraft.world.item.Items.COBBLESTONE) == 0, "no stone without a pickaxe");
        helper.succeed();
    }
}
