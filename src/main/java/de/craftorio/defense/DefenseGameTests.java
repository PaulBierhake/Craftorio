package de.craftorio.defense;

import de.craftorio.Craftorio;
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

    @GameTest(template = LARGE, timeoutTicks = 1600, batch = "defense_win")
    public static void defendedLevelIsWonAndPaysReward(GameTestHelper helper) {
        Setup setup = buildZone(helper, "WinTest");
        for (BlockPos pos : List.of(new BlockPos(6, 1, 5), new BlockPos(9, 1, 5), new BlockPos(12, 1, 5), new BlockPos(9, 1, 8))) {
            helper.setBlock(pos, ModBlocks.CROSSBOW_TOWER.get());
            TowerBlockEntity tower = helper.getBlockEntity(pos);
            tower.restore(TowerStats.MAX_LEVEL);
            tower.ammo().insertItem(0, new ItemStack(ModItems.BOLT.get(), 64), false);
        }
        setup.defense.start(setup.player.server, setup.team.id());
        helper.assertTrue(setup.zone().run().isPresent(), "level did not start");

        helper.succeedWhen(() -> {
            helper.assertValueEqual(setup.zone().level(), 2, "next level");
            helper.assertValueEqual(setup.team.balance(), LevelPlan.of(1, 1).reward(), "reward");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 1600, batch = "defense_lose")
    public static void undefendedLevelIsLost(GameTestHelper helper) {
        Setup setup = buildZone(helper, "LoseTest");
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

    /** Portal at (1,1,4), a U-shaped path of 24 blocks, zone core at (4,1,6). */
    private static Setup buildZone(GameTestHelper helper, String name) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), name);
        registry.setBalance(team.id(), 0);
        TowerDefense defense = TowerDefense.get(player.server);

        BlockPos core = new BlockPos(4, 1, 6);
        BlockPos portal = new BlockPos(1, 1, 4);
        helper.setBlock(core, ModBlocks.ZONE_CORE.get());
        helper.setBlock(portal, ModBlocks.ENEMY_PORTAL.get());
        defense.placeCore(team.id(), helper.absolutePos(core));
        defense.setPortal(team.id(), helper.absolutePos(portal));
        for (int x = 2; x <= 14; x++) {
            helper.setBlock(new BlockPos(x, 1, 4), ModBlocks.PATH_BLOCK.get());
        }
        helper.setBlock(new BlockPos(14, 1, 5), ModBlocks.PATH_BLOCK.get());
        for (int x = 14; x >= 5; x--) {
            helper.setBlock(new BlockPos(x, 1, 6), ModBlocks.PATH_BLOCK.get());
        }
        TowerDefense.PathCheck check = defense.checkPath(helper.getLevel(), defense.zone(team.id()).orElseThrow());
        helper.assertTrue(check.path() != null, "path invalid: " + check.message().getString());
        return new Setup(player, team, defense);
    }

    @GameTest(template = EMPTY)
    public static void zoneMovesWithPlayerIntoNewTeam(GameTestHelper helper) {
        TeamRegistry registry = TeamData.registry(helper.getLevel().getServer());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        Team solo = registry.ensureTeam(bob, "ZoneBob");
        TowerDefense defense = TowerDefense.get(helper.getLevel().getServer());
        defense.placeCore(solo.id(), helper.absolutePos(new BlockPos(2, 1, 2)));
        // The GameTest world persists between runs, so the team name must be unique.
        String name = "Zone " + Integer.toHexString(alice.hashCode());
        registry.create(alice, name);
        registry.invite(alice, bob);
        Team team = registry.join(bob, name);

        helper.assertTrue(defense.zone(team.id()).isPresent(), "zone moved to the joined team");
        helper.assertTrue(defense.zone(solo.id()).isEmpty(), "old team has no zone left");
        defense.removeCore(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)));
        helper.succeed();
    }
}
