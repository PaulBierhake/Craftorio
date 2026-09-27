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
}
