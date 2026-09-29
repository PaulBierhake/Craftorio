package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.protection.BlockOwnership;
import de.craftorio.team.Team;
import de.craftorio.team.TeamChunkLoader;
import de.craftorio.team.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Basics of the Factorio rework: tool recipes, chunk loader. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel
public final class FoundationGameTests {
    private FoundationGameTests() {
    }

    @GameTest(template = "empty")
    public static void vanillaToolRecipesAreGone(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String tool : List.of("wooden_pickaxe", "stone_axe", "iron_shovel", "golden_hoe", "diamond_pickaxe", "netherite_axe_smithing")) {
            helper.assertTrue(recipes.byKey(ResourceLocation.withDefaultNamespace(tool)).isEmpty(), tool + " can still be crafted");
        }
        helper.assertTrue(recipes.byKey(ResourceLocation.withDefaultNamespace("iron_sword")).isPresent(), "swords stay");
        helper.assertTrue(recipes.byKey(ResourceLocation.withDefaultNamespace("shears")).isPresent(), "shears stay");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkLoaderKeepsTeamChunksWhileTheTeamIsOnline(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Team team = TeamData.registry(player.server).ensureTeam(player.getUUID(), "ChunkTeam");
        BlockOwnership ownership = BlockOwnership.get(helper.getLevel());
        BlockPos base = helper.absolutePos(new BlockPos(0, 1, 0));
        List<BlockPos> claimed = List.of(base, base.offset(16, 0, 0), base.offset(32, 0, 0), base.offset(33, 0, 1));
        claimed.forEach(pos -> ownership.claim(pos, team.id()));

        TeamChunkLoader.update(player.server, id -> true);
        helper.assertValueEqual(TeamChunkLoader.loadedChunks(helper.getLevel(), team.id()).size(), 3, "chunks kept loaded");

        TeamChunkLoader.update(player.server, id -> false);
        helper.assertTrue(TeamChunkLoader.loadedChunks(helper.getLevel(), team.id()).isEmpty(), "released when nobody is online");

        claimed.forEach(ownership::release);
        TeamChunkLoader.update(player.server, id -> true);
        helper.assertTrue(TeamChunkLoader.loadedChunks(helper.getLevel(), team.id()).isEmpty(), "nothing left to load");
        helper.succeed();
    }
}
