package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.BlueprintActions;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Building at workbenches, upgrading workbenches. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only way to get a player in 1.21.1 game tests
public final class BlueprintGameTests {
    private static final String EMPTY = "empty";

    private BlueprintGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void firstLoginGivesGuideAndStarterKit(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertValueEqual(count(player, ModItems.GUIDE_BOOK.get()), 1, "guide book");
        helper.assertValueEqual(count(player, ModItems.WORKBENCH.get()), 1, "starter workbench");
        helper.assertValueEqual(count(player, ModItems.STARTER_PICKAXE.get()), 1, "starter pickaxe");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void workbenchBuildsUnlockedBlueprintsFromInventory(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Team team = team(player, 0);
        player.getInventory().clearContent(); // drop the login starter kit
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 20));
        player.getInventory().add(new ItemStack(Items.OAK_PLANKS, 4));

        // Free starter blueprint: 3 iron + 2 planks -> 4 belts, twice
        helper.assertTrue(BlueprintActions.build(player, index(helper, "conveyor_belt"), 1, 2), "build belts");
        helper.assertValueEqual(count(player, ModItems.CONVEYOR_BELT.get()), 8, "belts built");
        helper.assertValueEqual(count(player, Items.IRON_INGOT), 14, "iron left");

        player.getInventory().add(new ItemStack(Items.COPPER_INGOT, 4));
        player.getInventory().add(new ItemStack(Items.PISTON));
        helper.assertFalse(BlueprintActions.build(player, index(helper, "press"), 1, 1), "press needs the research");
        TeamData.registry(player.server).grantResearch(team.id(), Craftorio.id("automation").toString());
        helper.assertTrue(BlueprintActions.build(player, index(helper, "press"), 1, 1), "press after the research");
        helper.assertValueEqual(count(player, ModItems.PRESS.get()), 1, "presses built");
        helper.assertFalse(BlueprintActions.build(player, index(helper, "press"), 1, 1), "materials used up");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void higherTierBlueprintsNeedUpgradedWorkbench(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Team team = team(player, 0);
        TeamData.registry(player.server).grantResearch(team.id(), Craftorio.id("assembling").toString());
        player.getInventory().add(new ItemStack(ModItems.IRON_PLATE.get(), 12));
        player.getInventory().add(new ItemStack(ModItems.COPPER_CABLE.get(), 8));
        player.getInventory().add(new ItemStack(Items.GOLD_INGOT, 2));
        player.getInventory().add(new ItemStack(Items.CRAFTING_TABLE));

        helper.assertFalse(BlueprintActions.build(player, index(helper, "assembler"), 1, 1), "tier 1 workbench");
        helper.assertTrue(BlueprintActions.build(player, index(helper, "assembler"), 2, 1), "tier 2 workbench");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgradeKitUpgradesWorkbenchInPlace(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos bench = new BlockPos(2, 1, 2);
        helper.setBlock(bench, ModBlocks.WORKBENCH.get());

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WORKBENCH_UPGRADE_3.get()));
        useOn(helper, player, bench);
        helper.assertBlockPresent(ModBlocks.WORKBENCH.get(), bench);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WORKBENCH_UPGRADE_2.get()));
        useOn(helper, player, bench);
        helper.assertBlockPresent(ModBlocks.ASSEMBLY_WORKBENCH.get(), bench);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "kit consumed");
        helper.succeed();
    }

    private static void useOn(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(absolute.getCenter(), Direction.UP, absolute, false)));
    }

    private static Team team(ServerPlayer player, long balance) {
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "BlueprintTest");
        registry.setBalance(team.id(), balance);
        return team;
    }

    private static int index(GameTestHelper helper, String name) {
        var all = Blueprints.sorted(helper.getLevel().registryAccess());
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).key().location().equals(Craftorio.id(name))) {
                return i;
            }
        }
        throw new IllegalArgumentException("unknown blueprint " + name);
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }
}
