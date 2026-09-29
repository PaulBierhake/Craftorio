package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.BlueprintActions;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Hand crafting at the workbench. */
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
    public static void workbenchBuildsBlueprintsFromInventory(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Team team = team(player, 0);
        player.getInventory().clearContent(); // drop the login starter kit
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 20));
        player.getInventory().add(new ItemStack(ModItems.IRON_GEAR.get(), 2));

        // Start blueprint: 1 gear + 1 iron plate -> 2 belts, twice
        helper.assertTrue(BlueprintActions.build(player, index(helper, "conveyor_belt"), 2), "build belts");
        helper.assertValueEqual(count(player, ModItems.CONVEYOR_BELT.get()), 4, "belts built");
        helper.assertValueEqual(count(player, Items.IRON_INGOT), 18, "iron left");
        helper.assertFalse(BlueprintActions.build(player, index(helper, "conveyor_belt"), 1), "gears used up");

        // Hand crafting of the basics starts from plates alone.
        helper.assertTrue(BlueprintActions.build(player, index(helper, "iron_gear"), 3), "gears from iron");
        helper.assertValueEqual(count(player, ModItems.IRON_GEAR.get()), 3, "gears");
        helper.assertTrue(BlueprintActions.build(player, index(helper, "pipe"), 2), "pipes");
        helper.assertValueEqual(count(player, ModItems.PIPE.get()), 2, "pipes built");

        // Locked until the research is done.
        player.getInventory().add(new ItemStack(ModItems.CIRCUIT.get(), 3));
        player.getInventory().add(new ItemStack(ModItems.IRON_GEAR.get(), 2));
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 5));
        helper.assertFalse(BlueprintActions.build(player, index(helper, "assembler"), 1), "assembler needs the research");
        TeamData.registry(player.server).grantResearch(team.id(), Craftorio.id("automation").toString());
        helper.assertTrue(BlueprintActions.build(player, index(helper, "assembler"), 1), "assembler after the research");
        helper.assertValueEqual(count(player, ModItems.ASSEMBLER.get()), 1, "assemblers built");
        helper.assertFalse(BlueprintActions.build(player, index(helper, "assembler"), 1), "materials used up");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void terminalIsBuiltAtTheWorkbench(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        team(player, 0);
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 5));
        player.getInventory().add(new ItemStack(ModItems.CIRCUIT.get(), 2));

        helper.assertTrue(BlueprintActions.build(player, index(helper, "terminal"), 1), "terminal blueprint");
        helper.assertValueEqual(count(player, ModItems.TERMINAL.get()), 1, "terminal built");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(Craftorio.id("terminal")).isEmpty(), "no vanilla recipe for the terminal");
        helper.succeed();
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
