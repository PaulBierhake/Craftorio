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

/** Unlocking in the terminal, building at workbenches, upgrading workbenches. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only way to get a player in 1.21.1 game tests
public final class BlueprintGameTests {
    private static final String EMPTY = "empty";

    private BlueprintGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void unlockingNeedsPrerequisitesAndCosts(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Team team = team(player, 1000);

        helper.assertFalse(BlueprintActions.unlock(player, index(helper, "press")), "press needs the generator first");
        helper.assertTrue(BlueprintActions.unlock(player, index(helper, "coal_generator")), "generator unlock");
        helper.assertTrue(BlueprintActions.unlock(player, index(helper, "press")), "press unlock");
        helper.assertValueEqual(team.balance(), 1000L - 250 - 400, "balance after two blueprints");
        helper.assertValueEqual(team.totalSpent(), 650L, "spent");
        helper.assertTrue(BlueprintActions.unlock(player, index(helper, "electric_furnace")), "furnace for 300 of the 350 left");
        helper.assertFalse(BlueprintActions.unlock(player, index(helper, "power_pole")), "only 50 credits left for a 100 credit pole");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void keyMaterialIsConsumedOnUnlock(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Team team = team(player, 5000);
        TeamData.registry(player.server).unlock(team.id(), Craftorio.id("press").toString());

        helper.assertFalse(BlueprintActions.unlock(player, index(helper, "workbench_upgrade_2")), "needs a drill core");
        player.getInventory().add(new ItemStack(ModItems.DRILL_CORE.get()));
        helper.assertTrue(BlueprintActions.unlock(player, index(helper, "workbench_upgrade_2")), "unlock with drill core");
        helper.assertValueEqual(count(player, ModItems.DRILL_CORE.get()), 0, "drill cores left");
        helper.succeed();
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
        player.getInventory().add(new ItemStack(Items.FURNACE));
        player.getInventory().add(new ItemStack(Items.REDSTONE, 2));
        helper.assertFalse(BlueprintActions.build(player, index(helper, "coal_generator"), 1, 1), "generator is not unlocked");
        TeamData.registry(player.server).unlock(team.id(), Craftorio.id("coal_generator").toString());
        helper.assertTrue(BlueprintActions.build(player, index(helper, "coal_generator"), 1, 1), "generator after unlock");
        helper.assertValueEqual(count(player, ModItems.COAL_GENERATOR.get()), 1, "generators built");
        helper.assertFalse(BlueprintActions.build(player, index(helper, "coal_generator"), 1, 1), "materials used up");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void higherTierBlueprintsNeedUpgradedWorkbench(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Team team = team(player, 0);
        TeamData.registry(player.server).unlock(team.id(), Craftorio.id("assembler").toString());
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
