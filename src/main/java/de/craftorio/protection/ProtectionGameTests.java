package de.craftorio.protection;

import de.craftorio.Craftorio;
import de.craftorio.logistics.InserterBlock;
import de.craftorio.registry.ModBlocks;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel
public final class ProtectionGameTests {
    private static final String EMPTY = "empty";

    private ProtectionGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void otherTeamsCannotBreakProtectedBlocks(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(owner.server);
        UUID ownerTeam = registry.ensureTeam(owner.getUUID(), "Owner").id();
        registry.ensureTeam(stranger.getUUID(), "Stranger");
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.PRESS.get());
        BlockPos absolute = helper.absolutePos(pos);
        BlockOwnership.get(helper.getLevel()).claim(absolute, ownerTeam);

        stranger.gameMode.destroyBlock(absolute);
        helper.assertBlockPresent(ModBlocks.PRESS.get(), pos);
        helper.assertFalse(BlockOwnership.get(helper.getLevel()).mayAccess(helper.getLevel(), absolute,
                registry.teamOf(stranger.getUUID()).orElseThrow().id()), "stranger may not access");

        owner.gameMode.destroyBlock(absolute);
        helper.assertBlockNotPresent(ModBlocks.PRESS.get(), pos);
        helper.assertTrue(BlockOwnership.get(helper.getLevel()).owner(helper.getLevel(), absolute).isEmpty(), "claim released");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void insertersCannotStealFromOtherTeams(GameTestHelper helper) {
        TeamRegistry registry = TeamData.registry(helper.getLevel().getServer());
        UUID owner = registry.ensureTeam(UUID.randomUUID(), "ChestOwner").id();
        UUID thief = registry.ensureTeam(UUID.randomUUID(), "Thief").id();
        BlockPos chest = new BlockPos(1, 1, 2);
        BlockPos inserter = new BlockPos(2, 1, 2);
        BlockPos target = new BlockPos(3, 1, 2);
        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(inserter, ModBlocks.INSERTER.get().defaultBlockState().setValue(InserterBlock.FACING, Direction.EAST));
        helper.setBlock(target, Blocks.CHEST);
        ChestBlockEntity source = helper.getBlockEntity(chest);
        source.setItem(0, new ItemStack(Items.DIAMOND, 4));
        BlockOwnership ownership = BlockOwnership.get(helper.getLevel());
        ownership.claim(helper.absolutePos(chest), owner);
        ownership.claim(helper.absolutePos(inserter), thief);
        ownership.claim(helper.absolutePos(target), thief);

        helper.runAtTickTime(50, () -> {
            helper.assertValueEqual(source.getItem(0).getCount(), 4, "diamonds left in the owner's chest");
            helper.succeed();
        });
    }
}
