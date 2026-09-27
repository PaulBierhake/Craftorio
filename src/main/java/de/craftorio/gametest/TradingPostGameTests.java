package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.economy.block.TradingPostBlockEntity;
import de.craftorio.registry.ModBlocks;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.UUID;

/** Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TradingPostGameTests {
    private static final String EMPTY = "empty";
    private static final long IRON_INGOT_PRICE = 2;

    private TradingPostGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void sellsInsertedItemsAndRefusesUnpriced(GameTestHelper helper) {
        Team team = newTeam(helper);
        BlockPos pos = placeTradingPost(helper, new BlockPos(1, 1, 1), team);
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "trading post exposes no item handler");

        ItemStack rest = handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 10), false);
        helper.assertTrue(rest.isEmpty(), "iron ingots were not accepted");
        helper.assertValueEqual(team.balance(), 10 * IRON_INGOT_PRICE, "balance after selling iron");

        ItemStack dirt = new ItemStack(Items.DIRT, 5);
        helper.assertTrue(handler.insertItem(0, dirt, false).getCount() == 5, "dirt has no price and must be refused");
        helper.assertValueEqual(team.balance(), 10 * IRON_INGOT_PRICE, "balance after offering dirt");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void refusesItemsWhenOwnerTeamIsGone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.TRADING_POST.get());
        TradingPostBlockEntity post = helper.getBlockEntity(pos);
        post.setOwner(UUID.randomUUID());
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);

        ItemStack iron = new ItemStack(Items.IRON_INGOT, 4);
        helper.assertTrue(handler.insertItem(0, iron, false).getCount() == 4, "post without a team must refuse items");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void hopperFeedsTradingPost(GameTestHelper helper) {
        Team team = newTeam(helper);
        BlockPos post = placeTradingPost(helper, new BlockPos(1, 1, 1), team);
        BlockPos hopper = post.above();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopperEntity = helper.getBlockEntity(hopper);
        hopperEntity.setItem(0, new ItemStack(Items.IRON_INGOT, 3));

        helper.succeedWhen(() -> helper.assertValueEqual(team.balance(), 3 * IRON_INGOT_PRICE, "balance after hopper transfer"));
    }

    @GameTest(template = EMPTY)
    public static void capRockSurvivesExplosions(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.CAP_ROCK.get());
        BlockPos absolute = helper.absolutePos(pos);
        helper.getLevel().explode(null, absolute.getX() + 0.5, absolute.getY() + 1.5, absolute.getZ() + 0.5, 6.0F,
                Level.ExplosionInteraction.TNT);
        helper.assertBlockPresent(ModBlocks.CAP_ROCK.get(), pos);
        helper.succeed();
    }

    private static Team newTeam(GameTestHelper helper) {
        TeamRegistry registry = TeamData.registry(helper.getLevel().getServer());
        Team team = registry.ensureTeam(UUID.randomUUID(), "GameTest");
        registry.setBalance(team.id(), 0);
        return team;
    }

    private static BlockPos placeTradingPost(GameTestHelper helper, BlockPos pos, Team owner) {
        helper.setBlock(pos, ModBlocks.TRADING_POST.get());
        TradingPostBlockEntity post = helper.getBlockEntity(pos);
        post.setOwner(owner.id());
        return pos;
    }
}
