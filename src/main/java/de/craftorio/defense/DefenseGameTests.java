package de.craftorio.defense;

import de.craftorio.Craftorio;
import de.craftorio.defense.arena.ArenaLayout;
import de.craftorio.defense.arena.Arenas;
import de.craftorio.defense.arena.Tile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModEntities;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Towers against enemies and whole levels. Lives in this package to reach the level internals. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel
public final class DefenseGameTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private DefenseGameTests() {
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void crossbowTowerKillsCrawler(GameTestHelper helper) {
        BlockPos towerPos = new BlockPos(3, 1, 3);
        helper.setBlock(towerPos, ModBlocks.CROSSBOW_TOWER.get());
        TowerBlockEntity tower = helper.getBlockEntity(towerPos);
        tower.ammo().insertItem(0, new ItemStack(ModItems.BOLT.get(), 10), false);

        List<Vec3> path = new ArrayList<>();
        for (int x = 1; x <= 15; x++) {
            path.add(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 1, 6))));
        }
        LevelRun run = new LevelRun(LevelPlan.of(1, 1), path, helper.absolutePos(new BlockPos(15, 1, 8)));
        TdEnemy crawler = ModEntities.CRAWLER.get().create(helper.getLevel());
        crawler.start(run, path, 1.0);
        helper.getLevel().addFreshEntity(crawler);

        helper.succeedWhen(() -> {
            helper.assertTrue(crawler.isRemoved() && crawler.isDeadOrDying(), "crawler still alive");
            helper.assertTrue(tower.ammo().getStackInSlot(0).getCount() < 10, "tower used no ammunition");
        });
    }

    @GameTest(template = LARGE)
    public static void crystalGolemArmourOnlyStopsAmmunition(GameTestHelper helper) {
        List<Vec3> path = new ArrayList<>();
        for (int x = 1; x <= 15; x++) {
            path.add(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 1, 6))));
        }
        LevelRun run = new LevelRun(LevelPlan.of(20, 1), path, helper.absolutePos(new BlockPos(15, 1, 8)));
        TdEnemy golem = ModEntities.CRYSTAL_GOLEM.get().create(helper.getLevel());
        golem.start(run, path, 1.0);
        helper.getLevel().addFreshEntity(golem);
        float full = golem.getHealth();

        golem.hurt(helper.getLevel().damageSources().generic(), 10);
        helper.assertTrue(Math.abs(full - 3.5F - golem.getHealth()) < 0.01F, "ammunition hits are reduced to 35 %");
        golem.invulnerableTime = 0;
        golem.hurt(helper.getLevel().damageSources().magic(), 10);
        helper.assertTrue(Math.abs(full - 13.5F - golem.getHealth()) < 0.01F, "energy hits pass the armour");
        golem.discard();
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void laserTowerBurnsDownCrystalGolem(GameTestHelper helper) {
        BlockPos towerPos = new BlockPos(3, 1, 3);
        helper.setBlock(towerPos, ModBlocks.LASER_TOWER.get());
        TowerBlockEntity tower = helper.getBlockEntity(towerPos);
        tower.energy().setEnergy(tower.energy().getMaxEnergyStored());

        List<Vec3> path = new ArrayList<>();
        for (int x = 1; x <= 15; x++) {
            path.add(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 1, 6))));
        }
        LevelRun run = new LevelRun(LevelPlan.of(20, 1), path, helper.absolutePos(new BlockPos(15, 1, 8)));
        TdEnemy golem = ModEntities.CRYSTAL_GOLEM.get().create(helper.getLevel());
        golem.start(run, path, 1.0);
        helper.getLevel().addFreshEntity(golem);

        helper.succeedWhen(() -> helper.assertTrue(golem.isRemoved() && golem.isDeadOrDying(), "golem still alive"));
    }

    @GameTest(template = EMPTY)
    public static void destroyedTowerBecomesRuinAndRebuildsWithItsLevel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.GUN_TURRET.get());
        TowerBlockEntity tower = helper.getBlockEntity(pos);
        tower.restore(3);

        tower.damage(10_000);
        helper.assertBlockPresent(ModBlocks.TOWER_RUIN.get(), pos);
        TowerRuinBlockEntity ruin = helper.getBlockEntity(pos);
        helper.assertValueEqual(ruin.rebuildCost(), TowerType.GUN.rebuildCost(), "rebuild cost");

        ruin.rebuild();
        helper.assertBlockPresent(ModBlocks.GUN_TURRET.get(), pos);
        TowerBlockEntity rebuilt = helper.getBlockEntity(pos);
        helper.assertValueEqual(rebuilt.upgradeLevel(), 3, "upgrade level kept");
        helper.assertValueEqual(rebuilt.health(), rebuilt.maxHealth(), "full health after rebuild");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgradeCostsCreditsAndMaterials(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "UpgradeTest");
        registry.setBalance(team.id(), 200);
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.CROSSBOW_TOWER.get());
        TowerBlockEntity tower = helper.getBlockEntity(pos);

        helper.assertFalse(tower.upgrade(player), "no iron plates yet");
        player.getInventory().add(new ItemStack(ModItems.IRON_PLATE.get(), 8));
        helper.assertTrue(tower.upgrade(player), "upgrade to level 2");
        helper.assertValueEqual(tower.upgradeLevel(), 2, "level");
        helper.assertValueEqual(team.balance(), 50L, "credits left");
        helper.assertValueEqual(tower.maxHealth(), 125, "health of a level 2 crossbow tower");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 2000, batch = "defense_win")
    public static void defendedLevelIsWonPaysRewardAndChangesTheMap(GameTestHelper helper) {
        Setup setup = buildArena(helper, "WinTest");
        ServerLevel arena = TowerDefense.arena(setup.player.server);
        ArenaLayout layout = setup.defense.layout(setup.zone());
        int placed = 0;
        for (int[] tile : layout.route()) {
            for (int[] side : new int[][]{{0, 1}, {0, -1}}) {
                int x = tile[0] + side[0];
                int z = tile[1] + side[1];
                BlockPos pos = Arenas.field(setup.zone().slot(), x, z, Arenas.BUILD_Y);
                if (placed < 8 && tile[0] % 5 == 2 && layout.tile(x, z) == Tile.GROUND && arena.getBlockState(pos).canBeReplaced()) {
                    arena.setBlock(pos, ModBlocks.CROSSBOW_TOWER.get().defaultBlockState(), 3);
                    if (arena.getBlockEntity(pos) instanceof TowerBlockEntity tower) {
                        tower.restore(TowerStats.MAX_LEVEL);
                        tower.ammo().insertItem(0, new ItemStack(ModItems.BOLT.get(), 64), false);
                        placed++;
                    }
                }
            }
        }
        helper.assertTrue(placed >= 4, "only " + placed + " towers fit");
        Component started = setup.defense.start(setup.player.server, setup.team.id());
        helper.assertTrue(setup.zone().run().isPresent(), "level did not start: " + started.getString());
        int placedTowers = placed;

        helper.succeedWhen(() -> {
            helper.assertValueEqual(setup.zone().level(), 2, "next level");
            helper.assertTrue(setup.team.balance() >= LevelPlan.of(1, 1).reward(), "reward paid");
            helper.assertTrue(TowerDefense.findTowers(arena, TowerDefense.arenaMin(setup.zone().slot()),
                    TowerDefense.arenaMax(setup.zone().slot())).isEmpty(), "towers packed up for the new map");
            helper.assertTrue(setup.defense.checkPath(arena, setup.zone()).path() == null, "old path removed");
            helper.assertTrue(setup.zone().depotSize() >= placedTowers, "towers in the depot");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 2000, batch = "defense_lose")
    public static void undefendedLevelIsLost(GameTestHelper helper) {
        Setup setup = buildArena(helper, "LoseTest");
        setup.defense.start(setup.player.server, setup.team.id());
        helper.assertTrue(setup.zone().run().isPresent(), "level did not start");

        helper.succeedWhen(() -> {
            helper.assertTrue(setup.zone().run().isEmpty(), "level still running");
            helper.assertValueEqual(setup.zone().level(), 1, "level must not advance");
            helper.assertValueEqual(setup.team.balance(), 0L, "no reward");
        });
    }

    private record Setup(ServerPlayer player, Team team, TowerDefense defense) {
        TowerDefense.Zone zone() {
            return defense.zone(team.id()).orElseThrow();
        }
    }

    /** A fresh arena for a new team with the shortest possible path laid from the gate to the core. */
    private static Setup buildArena(GameTestHelper helper, String name) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), name);
        registry.setBalance(team.id(), 0);
        TowerDefense defense = TowerDefense.get(player.server);
        TowerDefense.Zone zone = defense.ensureArena(player.server, team.id());
        ServerLevel arena = TowerDefense.arena(player.server);
        for (int[] tile : defense.layout(zone).route()) {
            arena.setBlock(Arenas.field(zone.slot(), tile[0], tile[1], Arenas.BUILD_Y), ModBlocks.PATH_BLOCK.get().defaultBlockState(), 3);
        }
        TowerDefense.PathCheck check = defense.checkPath(arena, zone);
        helper.assertTrue(check.path() != null, "path invalid: " + check.message().getString());
        return new Setup(player, team, defense);
    }

    @GameTest(template = EMPTY)
    public static void towerItemsKeepLevelAndHealth(GameTestHelper helper) {
        ItemStack item = TowerDefense.towerItem(TowerType.GUN, 4, 0);
        helper.assertValueEqual(item.get(de.craftorio.registry.ModDataComponents.TOWER_STATE.get()).level(), 4, "level on the item");
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.GUN_TURRET.get());
        TowerBlockEntity tower = helper.getBlockEntity(pos);
        tower.applyComponentsFromItemStack(TowerDefense.towerItem(TowerType.GUN, 4, 50));
        helper.assertValueEqual(tower.upgradeLevel(), 4, "level applied");
        helper.assertValueEqual(tower.health(), 50, "health applied");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void zoneMovesWithPlayerIntoNewTeam(GameTestHelper helper) {
        TeamRegistry registry = TeamData.registry(helper.getLevel().getServer());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        Team solo = registry.ensureTeam(bob, "ZoneBob");
        TowerDefense defense = TowerDefense.get(helper.getLevel().getServer());
        int slot = defense.ensureArena(helper.getLevel().getServer(), solo.id()).slot();
        // The GameTest world persists between runs, so the team name must be unique.
        String name = "Zone " + Integer.toHexString(alice.hashCode());
        registry.create(alice, name);
        registry.invite(alice, bob);
        Team team = registry.join(bob, name);

        helper.assertValueEqual(defense.zone(team.id()).orElseThrow().slot(), slot, "arena moved to the joined team");
        helper.assertTrue(defense.zone(solo.id()).isEmpty(), "old team has no arena left");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 1200, batch = "defense_maps")
    public static void everyMapHasAPathThatCanBeLaidWithThePathWand(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "MapTest");
        TowerDefense defense = TowerDefense.get(player.server);
        TowerDefense.Zone zone = defense.ensureArena(player.server, team.id());
        ServerLevel arena = TowerDefense.arena(player.server);
        java.util.Set<de.craftorio.defense.arena.ArenaTheme> themes = java.util.EnumSet.noneOf(de.craftorio.defense.arena.ArenaTheme.class);
        for (int level = 1; level <= 40; level++) {
            defense.jumpToLevel(arena, zone, level);
            ArenaLayout layout = defense.layout(zone);
            themes.add(layout.theme());
            List<BlockPos> route = new ArrayList<>();
            for (int[] tile : layout.route()) {
                route.add(Arenas.field(zone.slot(), tile[0], tile[1], Arenas.BUILD_Y));
            }
            de.craftorio.defense.arena.PathWandItem.Result result = de.craftorio.defense.arena.PathWandItem.lay(arena, defense, layout, route);
            helper.assertTrue(result.error() == null && result.placed() == route.size(),
                    "level " + level + " (" + layout.theme() + "): path stopped after " + result.placed() + "/" + route.size() + " " + result.error());
            TowerDefense.PathCheck check = defense.checkPath(arena, zone);
            helper.assertTrue(check.path() != null, "level " + level + " (" + layout.theme() + "): " + check.message().getString());
        }
        helper.assertTrue(themes.size() == de.craftorio.defense.arena.ArenaTheme.values().length, "not every theme was tested: " + themes);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 600, batch = "defense_maps")
    public static void rebuildingMapsLeavesNoDroppedItems(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "DropTest");
        TowerDefense defense = TowerDefense.get(player.server);
        TowerDefense.Zone zone = defense.ensureArena(player.server, team.id());
        ServerLevel arena = TowerDefense.arena(player.server);
        BlockPos min = TowerDefense.arenaMin(zone.slot());
        BlockPos max = TowerDefense.arenaMax(zone.slot());
        for (int level = 1; level <= 8; level++) {
            defense.jumpToLevel(arena, zone, level);
            var items = arena.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, net.minecraft.world.phys.AABB.encapsulatingFullBlocks(min, max.offset(1, 1, 1)));
            helper.assertTrue(items.isEmpty(), "dropped items after building level " + level + ": " + items.size());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void pathWandRefusesBranches(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "BranchTest");
        TowerDefense defense = TowerDefense.get(player.server);
        TowerDefense.Zone zone = defense.ensureArena(player.server, team.id());
        ServerLevel arena = TowerDefense.arena(player.server);
        ArenaLayout layout = defense.layout(zone);
        List<BlockPos> route = new ArrayList<>();
        for (int[] tile : layout.route().subList(0, 8)) {
            route.add(Arenas.field(zone.slot(), tile[0], tile[1], Arenas.BUILD_Y));
        }
        helper.assertTrue(de.craftorio.defense.arena.PathWandItem.lay(arena, defense, layout, route).error() == null, "straight path");
        // Next to the first block (which already touches the portal) a side block would make a junction.
        BlockPos first = route.get(0);
        for (BlockPos side : new BlockPos[]{first.north(), first.south()}) {
            if (!route.contains(side)) {
                helper.assertTrue(defense.pathBranches(arena, side), "junction next to the first block at " + side);
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void towersFromTheDepotStackWithNewOnes(GameTestHelper helper) {
        ItemStack fresh = new ItemStack(ModBlocks.CROSSBOW_TOWER.get());
        ItemStack packed = TowerDefense.towerItem(TowerType.CROSSBOW, 1, TowerStats.maxHealth(TowerType.CROSSBOW, 1));
        helper.assertTrue(ItemStack.isSameItemSameComponents(fresh, packed), "unupgraded intact towers stack");
        helper.assertFalse(ItemStack.isSameItemSameComponents(fresh, TowerDefense.towerItem(TowerType.CROSSBOW, 2, 200)), "upgraded towers do not");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void depotIsATakeOnlyInventory(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "DepotTest");
        TowerDefense defense = TowerDefense.get(player.server);
        TowerDefense.Zone zone = defense.ensureArena(player.server, team.id());
        int before = zone.depotSize();
        for (int i = 0; i < 3; i++) {
            TowerDefense.addToDepot(zone, TowerDefense.towerItem(TowerType.CROSSBOW, 1, TowerStats.maxHealth(TowerType.CROSSBOW, 1)));
        }
        TowerDefense.addToDepot(zone, TowerDefense.towerItem(TowerType.GUN, 3, 50));
        helper.assertValueEqual(zone.depotSize(), before + 2, "three equal towers share a stack");

        var view = defense.depotView(zone);
        var menu = new de.craftorio.menu.DepotMenu(1, player.getInventory(), view);
        int crossbowSlot = -1;
        for (int i = 0; i < view.getContainerSize(); i++) {
            if (view.getItem(i).is(ModBlocks.CROSSBOW_TOWER.get().asItem()) && view.getItem(i).getCount() == 3) {
                crossbowSlot = i;
            }
        }
        helper.assertTrue(crossbowSlot >= 0, "stack of three towers in the depot");
        menu.clicked(crossbowSlot, 1, net.minecraft.world.inventory.ClickType.PICKUP, player); // right-click: half the stack
        helper.assertValueEqual(menu.getCarried().getCount(), 2, "picked up");
        helper.assertValueEqual(view.getItem(crossbowSlot).getCount(), 1, "one stays");
        int emptySlot = view.getContainerSize() - 1;
        menu.clicked(emptySlot, 0, net.minecraft.world.inventory.ClickType.PICKUP, player); // nothing can be put in
        helper.assertTrue(view.getItem(emptySlot).isEmpty(), "nothing put in");
        helper.assertValueEqual(menu.getCarried().getCount(), 2, "still carried");
        player.getInventory().add(menu.getCarried());
        menu.setCarried(ItemStack.EMPTY);

        int inventoryBefore = countAll(player);
        menu.clickMenuButton(player, de.craftorio.menu.DepotMenu.TAKE_ALL);
        helper.assertTrue(countAll(player) > inventoryBefore, "take all gives the towers");
        menu.removed(player);
        helper.assertValueEqual(zone.depotSize(), before, "empty entries are dropped when closed");
        helper.succeed();
    }

    private static int countAll(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            count += stack.getCount();
        }
        return count;
    }

    @GameTest(template = EMPTY)
    public static void arenaConsoleControlsTheLevel(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "ConsoleTest");
        TowerDefense defense = TowerDefense.get(player.server);
        TowerDefense.Zone zone = defense.ensureArena(player.server, team.id());
        ServerLevel arena = TowerDefense.arena(player.server);
        BlockPos console = Arenas.console(zone.slot());
        helper.assertTrue(arena.getBlockState(console).is(ModBlocks.ARENA_CONSOLE.get()), "console stands on the stands");
        helper.assertTrue(!console.equals(Arenas.exit(zone.slot())) && !console.equals(Arenas.depot(zone.slot())), "own position");

        arena.setBlock(console, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        defense.ensureArena(player.server, team.id());
        helper.assertTrue(arena.getBlockState(console).is(ModBlocks.ARENA_CONSOLE.get()), "existing arenas get the console back");

        var menu = new de.craftorio.menu.TerminalMenu(1, player.getInventory(), console,
                de.craftorio.blueprint.TerminalStats.of(team, registry.currentMinute()));
        boolean auto = zone.auto();
        helper.assertTrue(menu.clickMenuButton(player, de.craftorio.menu.TerminalMenu.TD_TOGGLE_AUTO), "button handled");
        helper.assertTrue(zone.auto() != auto, "auto mode toggled from the console menu");
        menu.clickMenuButton(player, de.craftorio.menu.TerminalMenu.TD_TOGGLE_AUTO);
        helper.succeed();
    }
}
