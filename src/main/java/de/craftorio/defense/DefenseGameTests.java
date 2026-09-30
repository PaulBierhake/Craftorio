package de.craftorio.defense;

import net.minecraft.world.item.Items;
import de.craftorio.Craftorio;
import de.craftorio.defense.arena.ArenaBuilder;
import de.craftorio.defense.arena.ArenaLayout;
import de.craftorio.defense.arena.ArenaTheme;
import de.craftorio.defense.arena.Arenas;
import de.craftorio.defense.arena.Tile;
import de.craftorio.defense.sim.EnemyDefs;
import de.craftorio.defense.sim.SimEnemy;
import de.craftorio.defense.sim.TdPath;
import de.craftorio.defense.sim.TdSimulation;
import de.craftorio.network.TdEnemiesPayload;
import de.craftorio.network.TdPathPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import de.craftorio.registry.ModBlocks;
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

    /** A straight track through the test area with a run the towers can see; the test's ticks move the enemies. */
    private static LevelRun track(GameTestHelper helper, int level) {
        return track(helper, level, true);
    }

    /** As {@link #track(GameTestHelper, int)}; without {@code tickSimulation} the test runs the whole level itself. */
    private static LevelRun track(GameTestHelper helper, int level, boolean tickSimulation) {
        List<Vec3> path = new ArrayList<>();
        for (int x = 1; x <= 15; x++) {
            path.add(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 1, 6))));
        }
        LevelRun run = new LevelRun(LevelPlan.of(level, 1), path, helper.absolutePos(new BlockPos(15, 1, 8)));
        run.register(helper.getLevel());
        if (tickSimulation) {
            helper.onEachTick(() -> run.simulation().tick());
        }
        return run;
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void crossbowTowerPopsACrawler(GameTestHelper helper) {
        BlockPos towerPos = new BlockPos(3, 1, 3);
        helper.setBlock(towerPos, ModBlocks.CROSSBOW_TOWER.get());
        TowerBlockEntity tower = helper.getBlockEntity(towerPos);
        tower.ammo().insertItem(0, new ItemStack(ModItems.BOLT.get(), 10), false);
        LevelRun run = track(helper, 1);
        run.simulation().spawn("red_crawler");

        helper.succeedWhen(() -> {
            helper.assertTrue(run.simulation().count() == 0 && run.simulation().paidPops() == 1, "crawler not popped");
            helper.assertTrue(tower.ammo().getStackInSlot(0).getCount() < 10, "tower used no ammunition");
            run.end(helper.getLevel());
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void boltsBounceOffAnIronbreaker(GameTestHelper helper) {
        BlockPos towerPos = new BlockPos(3, 1, 3);
        helper.setBlock(towerPos, ModBlocks.CROSSBOW_TOWER.get());
        TowerBlockEntity tower = helper.getBlockEntity(towerPos);
        tower.ammo().insertItem(0, new ItemStack(ModItems.BOLT.get(), 10), false);
        LevelRun run = track(helper, 1);
        run.simulation().spawn("ironbreaker");

        helper.runAtTickTime(60, () -> {
            helper.assertValueEqual(run.simulation().count(), 1, "the ironbreaker is still walking");
            helper.assertValueEqual(run.simulation().paidPops(), 0, "nothing popped: sharp damage does nothing to lead");
            helper.assertTrue(tower.ammo().getStackInSlot(0).getCount() < 10, "the tower shot anyway");
            run.end(helper.getLevel());
            helper.succeed();
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void armourPiercingMagazinesPopIronbreakers(GameTestHelper helper) {
        BlockPos towerPos = new BlockPos(3, 1, 4);
        helper.setBlock(towerPos, ModBlocks.GUN_TURRET.get());
        TowerBlockEntity tower = helper.getBlockEntity(towerPos);
        tower.ammo().insertItem(0, new ItemStack(ModItems.AP_MAGAZINE.get()), false);
        LevelRun run = track(helper, 1);
        run.simulation().spawn("ironbreaker");

        helper.succeedWhen(() -> {
            helper.assertTrue(run.simulation().paidPops() >= 1, "the normal damage of armour-piercing magazines pops lead");
            run.end(helper.getLevel());
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void laserTowerPopsACrystalGolem(GameTestHelper helper) {
        BlockPos towerPos = new BlockPos(3, 1, 3);
        helper.setBlock(towerPos, ModBlocks.LASER_TOWER.get());
        TowerBlockEntity tower = helper.getBlockEntity(towerPos);
        tower.energy().setEnergy(tower.energy().getMaxEnergyStored());
        LevelRun run = track(helper, 20);
        run.simulation().spawn("crystal_golem");

        helper.succeedWhen(() -> {
            helper.assertTrue(run.simulation().enemies().stream().noneMatch(e -> e.def().id().equals("crystal_golem")), "golem still there");
            helper.assertTrue(run.simulation().paidPops() >= 1, "no layer popped");
            run.end(helper.getLevel());
        });
    }

    @GameTest(template = EMPTY)
    public static void enemySnapshotsAreCompactAndSurviveTheWire(GameTestHelper helper) {
        TdPathPayload path = new TdPathPayload(3, List.of(new float[]{1, 2, 3}, new float[]{4, 5, 6}));
        io.netty.buffer.ByteBuf buffer = io.netty.buffer.Unpooled.buffer();
        TdPathPayload.STREAM_CODEC.encode(buffer, path);
        TdPathPayload decodedPath = TdPathPayload.STREAM_CODEC.decode(buffer);
        helper.assertValueEqual(decodedPath.points().get(1)[2], 6F, "path point");

        TdSimulation sim = new TdSimulation(new TdPath(List.of(new double[]{0, 0, 0}, new double[]{150, 0, 0})));
        for (int i = 0; i < 1_000; i++) {
            sim.spawn(EnemyDefs.IDS.get(i % 12), i % 7 == 0, i % 5 == 0, i % 3 == 0);
        }
        for (int i = 0; i < 100; i++) {
            sim.tick();
        }
        TdEnemiesPayload payload = TowerDefense.enemiesPayload(2, sim);
        buffer = io.netty.buffer.Unpooled.buffer();
        TdEnemiesPayload.STREAM_CODEC.encode(buffer, payload);
        helper.assertTrue(buffer.readableBytes() < 8_000, "1,000 enemies take " + buffer.readableBytes() + " bytes");
        TdEnemiesPayload decoded = TdEnemiesPayload.STREAM_CODEC.decode(buffer);
        helper.assertValueEqual(decoded.size(), 1_000, "enemies");
        helper.assertValueEqual(decoded.ids()[999], payload.ids()[999], "ids survive the delta coding");
        helper.assertValueEqual(decoded.kinds()[11], payload.kinds()[11], "kind");
        helper.assertValueEqual(decoded.flags()[14], payload.flags()[14], "flags");
        helper.assertValueEqual(decoded.distances()[500], payload.distances()[500], "distance");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100, batch = "defense_clear")
    public static void operatorsClearLevelsAndLostSealsCanBeClaimedAgain(GameTestHelper helper) {
        Setup setup = buildArena(helper, "ClearTest");
        ServerPlayer player = setup.player();
        player.getInventory().clearContent();
        helper.assertValueEqual(setup.defense().claimSeals(player), 0, "no level cleared, no seal");
        setup.defense().clearLevels(player.server, setup.team().id(), 30);
        helper.assertValueEqual(setup.zone().level(), 31, "levels up to 30 cleared");
        // bronze (5), silver (10), gold (20) and platinum (30): every research that needs one has not been paid yet
        helper.assertTrue(setup.defense().claimSeals(player) >= 4, "the seals of cleared levels are handed out");
        helper.assertValueEqual(setup.defense().claimSeals(player), 0, "nothing more while the player carries them");
        helper.assertTrue(player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.PLATINUM_SEAL.get())), "platinum seal");
        helper.assertFalse(player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.DIAMOND_SEAL.get())), "level 40 is not cleared");
        setup.defense().clearLevels(player.server, setup.team().id(), -1);
        helper.assertValueEqual(setup.zone().level(), 32, "a single level cleared");
        helper.succeed();
    }

    /** A tower in the team's arena (the upgrade and the sale need the arena's coins). */
    private static TowerBlockEntity arenaTower(Setup setup, TowerType type, int x, int z) {
        ServerLevel arena = TowerDefense.arena(setup.player.server);
        BlockPos pos = Arenas.field(setup.zone().slot(), x, z, Arenas.BUILD_Y);
        arena.setBlock(pos, ModBlocks.tower(type).get().defaultBlockState(), 3);
        return (TowerBlockEntity) arena.getBlockEntity(pos);
    }

    @GameTest(template = EMPTY)
    public static void upgradesCostCoinsPartsAndResearchAndFollowTheCrossPathRule(GameTestHelper helper) {
        Setup setup = buildArena(helper, "UpgradeTest");
        ServerPlayer player = setup.player;
        TeamRegistry registry = TeamData.registry(player.server);
        TowerBlockEntity tower = arenaTower(setup, TowerType.CROSSBOW, 2, 2);
        tower.setPaid(200);
        setup.defense.setCoins(setup.zone(), 10_000);

        helper.assertTrue(tower.upgrade(player, 0), "Sharp Shots (140)");
        helper.assertValueEqual(setup.zone().coins(), 9_860L, "coins left");
        helper.assertValueEqual(tower.paid(), 340L, "paid so far: 200 + 140");
        helper.assertTrue(tower.upgrade(player, 0), "Razor Sharp Shots (200)");
        helper.assertValueEqual(setup.zone().coins(), 9_660L, "coins left");
        helper.assertFalse(tower.upgrade(player, 0), "tier 3 needs the research Tower Technology I");
        helper.assertValueEqual(tower.tier(0), 2, "still tier 2");
        registry.grantResearch(setup.team.id(), TowerBlockEntity.techResearch(1));
        helper.assertFalse(tower.upgrade(player, 0), "and five circuits");
        player.getInventory().add(new ItemStack(ModItems.CIRCUIT.get(), 5));
        helper.assertTrue(tower.upgrade(player, 0), "Spike-o-pult (320)");
        helper.assertValueEqual(setup.zone().coins(), 9_340L, "coins left");
        helper.assertFalse(player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.CIRCUIT.get())), "the circuits are used up");
        helper.assertTrue(tower.upgrade(player, 1), "second path: tier 1");
        helper.assertTrue(tower.upgrade(player, 1), "second path: tier 2");
        helper.assertFalse(tower.upgrade(player, 1), "second path stops at tier 2 (3-3-0 is not allowed)");
        helper.assertFalse(tower.upgrade(player, 2), "third path stays closed (3-2-1 is not allowed)");
        helper.assertValueEqual(tower.tiers()[0] + "-" + tower.tiers()[1] + "-" + tower.tiers()[2], "3-2-0", "state");
        helper.assertValueEqual(tower.paid(), 200L + 140 + 200 + 320 + 100 + 190, "everything paid");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void upgradesNeedEnoughCoinsAndRespectTheDifficulty(GameTestHelper helper) {
        Setup setup = buildArena(helper, "UpgradePriceTest");
        TowerBlockEntity tower = arenaTower(setup, TowerType.CROSSBOW, 2, 2);
        setup.defense.setCoins(setup.zone(), 100);
        helper.assertFalse(tower.upgrade(setup.player, 0), "140 coins needed, 100 there");
        setup.defense.setDifficulty(setup.team.id(), Difficulty.EASY);
        helper.assertFalse(tower.upgrade(setup.player, 0), "on Easy it costs 119, still more than 100");
        setup.defense.setCoins(setup.zone(), 119);
        helper.assertTrue(tower.upgrade(setup.player, 0), "119 coins are enough on Easy");
        helper.assertValueEqual(setup.zone().coins(), 0L, "all spent");
        helper.assertValueEqual(tower.paid(), 119L, "the price paid counts for selling");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void sellingPaysBackSeventyPercentAndReturnsThePlainTower(GameTestHelper helper) {
        Setup setup = buildArena(helper, "SellTest");
        TowerBlockEntity tower = arenaTower(setup, TowerType.GUN, 2, 2);
        tower.setPaid(350);
        tower.setTiers(1, 0, 0);
        tower.setPaid(350 + 350);
        setup.defense.setCoins(setup.zone(), 0);
        BlockPos pos = tower.getBlockPos();
        helper.assertValueEqual(tower.sellValue(), 490L, "70 % of 700");
        setup.player.getInventory().clearContent();
        helper.assertTrue(tower.sell(setup.player), "sold");
        helper.assertValueEqual(setup.zone().coins(), 490L, "coins paid back");
        helper.assertTrue(TowerDefense.arena(setup.player.server).getBlockState(pos).isAir(), "the tower is gone");
        ItemStack back = setup.player.getInventory().getItem(0);
        helper.assertTrue(back.is(ModBlocks.GUN_TURRET.get().asItem()) && !back.has(de.craftorio.registry.ModDataComponents.TOWER_STATE.get()),
                "a plain tower item comes back: " + back);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aFreshTowerCostsItsPriceAndOneFromTheDepotIsFree(GameTestHelper helper) {
        Setup setup = buildArena(helper, "PlaceTest");
        ServerLevel arena = TowerDefense.arena(setup.player.server);
        BlockPos pos = Arenas.field(setup.zone().slot(), 2, 2, Arenas.BUILD_Y);
        ItemStack fresh = new ItemStack(ModBlocks.CROSSBOW_TOWER.get());
        int slot = setup.zone().slot();
        setup.defense.setCoins(setup.zone(), 150);
        helper.assertTrue(setup.defense.placementCoinsError(slot, fresh) != null, "200 coins needed");
        setup.defense.setCoins(setup.zone(), 650);
        helper.assertTrue(setup.defense.placementCoinsError(slot, fresh) == null, "650 coins are enough");
        // placing pays
        arena.setBlock(pos, ModBlocks.CROSSBOW_TOWER.get().defaultBlockState(), 3);
        ModBlocks.CROSSBOW_TOWER.get().setPlacedBy(arena, pos, arena.getBlockState(pos), setup.player, fresh);
        helper.assertValueEqual(setup.zone().coins(), 450L, "200 paid");
        helper.assertValueEqual(((TowerBlockEntity) arena.getBlockEntity(pos)).paid(), 200L, "remembered");
        // a tower from the depot costs nothing
        BlockPos other = Arenas.field(setup.zone().slot(), 4, 2, Arenas.BUILD_Y);
        ItemStack packed = TowerDefense.towerItem(TowerType.CROSSBOW, new int[]{2, 0, 0}, 340);
        helper.assertTrue(setup.defense.placementCoinsError(slot, packed) == null, "no coins needed");
        arena.setBlock(other, ModBlocks.CROSSBOW_TOWER.get().defaultBlockState(), 3);
        TowerBlockEntity placed = (TowerBlockEntity) arena.getBlockEntity(other);
        placed.applyComponentsFromItemStack(packed);
        ModBlocks.CROSSBOW_TOWER.get().setPlacedBy(arena, other, arena.getBlockState(other), setup.player, packed);
        helper.assertValueEqual(setup.zone().coins(), 450L, "nothing paid");
        helper.assertValueEqual(placed.tier(0), 2, "upgrades came along");
        helper.assertValueEqual(placed.paid(), 340L, "and what was paid");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 3600, batch = "defense_win")
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
                if (placed < 12 && tile[0] % 5 == 2 && layout.tile(x, z) == Tile.GROUND && arena.getBlockState(pos).canBeReplaced()) {
                    arena.setBlock(pos, ModBlocks.CROSSBOW_TOWER.get().defaultBlockState(), 3);
                    if (arena.getBlockEntity(pos) instanceof TowerBlockEntity tower) {
                        tower.setTiers(5, 2, 0);
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
            // both rounds paid their bonus (101 + 102) and the pops paid coins
            helper.assertTrue(setup.zone().coins() >= 650 + 101 + 102 + 30, "coins " + setup.zone().coins());
            helper.assertTrue(TowerDefense.findTowers(arena, TowerDefense.arenaMin(setup.zone().slot()),
                    TowerDefense.arenaMax(setup.zone().slot())).isEmpty(), "towers packed up for the new map");
            helper.assertTrue(setup.defense.checkPath(arena, setup.zone()).path() == null, "old path removed");
            helper.assertTrue(setup.zone().depotSize() >= placedTowers, "towers in the depot");
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 2000, batch = "defense_lose")
    public static void undefendedLevelIsLostAndTheSnapshotComesBack(GameTestHelper helper) {
        Setup setup = buildArena(helper, "LoseTest", 25); // the enemies of level 25 cost more than 150 lives
        ServerLevel arena = TowerDefense.arena(setup.player.server);
        BlockPos towerPos = Arenas.field(setup.zone().slot(), 2, 2, Arenas.BUILD_Y);
        arena.setBlock(towerPos, ModBlocks.CROSSBOW_TOWER.get().defaultBlockState(), 3);
        ((TowerBlockEntity) arena.getBlockEntity(towerPos)).setTiers(3, 2, 0);
        setup.defense.setCoins(setup.zone(), 1_234);
        setup.defense.start(setup.player.server, setup.team.id());
        helper.assertTrue(setup.zone().run().isPresent(), "level did not start");
        arena.setBlock(towerPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3); // gone during the level
        setup.defense.setCoins(setup.zone(), 99_999);

        helper.succeedWhen(() -> {
            helper.assertTrue(setup.zone().run().isEmpty(), "level still running");
            helper.assertValueEqual(setup.zone().level(), 25, "level must not advance");
            helper.assertValueEqual(setup.team.balance(), 0L, "no reward");
            helper.assertValueEqual(setup.zone().coins(), 1_234L, "the coins of the start are back");
            helper.assertTrue(arena.getBlockEntity(towerPos) instanceof TowerBlockEntity tower && tower.tier(0) == 3 && tower.tier(1) == 2,
                    "the tower is back with its upgrades");
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 2400, batch = "defense_lose")
    public static void theDifficultyCanOnlyBeMadeEasierAfterTheFirstLevel(GameTestHelper helper) {
        Setup setup = buildArena(helper, "DifficultyTest", 25);
        UUID team = setup.team.id();
        helper.assertValueEqual(setup.zone().difficulty(), Difficulty.MEDIUM, "standard difficulty");
        setup.defense.setDifficulty(team, Difficulty.HARD);
        helper.assertValueEqual(setup.zone().difficulty(), Difficulty.HARD, "any difficulty before the first level");
        setup.defense.cycleDifficulty(team);
        helper.assertValueEqual(setup.zone().difficulty(), Difficulty.IMPOPPABLE, "cycled");
        setup.defense.setDifficulty(team, Difficulty.HARD);
        setup.defense.start(setup.player.server, team);
        helper.assertTrue(setup.zone().run().isPresent(), "level did not start");
        helper.assertValueEqual(setup.zone().run().get().maxLives(), 100, "hard: 100 lives");
        helper.assertValueEqual(setup.zone().run().get().difficulty(), Difficulty.HARD, "the run knows it");
        setup.defense.setDifficulty(team, Difficulty.EASY);
        helper.assertValueEqual(setup.zone().difficulty(), Difficulty.HARD, "not while a level runs");

        helper.succeedWhen(() -> {
            helper.assertTrue(setup.zone().run().isEmpty(), "level still running");
            helper.assertTrue(setup.zone().campaignStarted(), "campaign started");
            setup.defense.setDifficulty(team, Difficulty.IMPOPPABLE);
            helper.assertValueEqual(setup.zone().difficulty(), Difficulty.HARD, "no harder difficulty afterwards");
            setup.defense.cycleDifficulty(team);
            helper.assertValueEqual(setup.zone().difficulty(), Difficulty.MEDIUM, "cycling goes down one step at a time");
            setup.defense.setDifficulty(team, Difficulty.EASY);
            helper.assertValueEqual(setup.zone().difficulty(), Difficulty.EASY, "easier is fine");
            setup.defense.cycleDifficulty(team);
            helper.assertValueEqual(setup.zone().difficulty(), Difficulty.EASY, "the easiest stays");
        });
    }

    @GameTest(template = EMPTY)
    public static void theWarChestExchangesCreditsForCoinsUpToTheLevelLimit(GameTestHelper helper) {
        Setup setup = buildArena(helper, "WarChestTest");
        TeamRegistry registry = TeamData.registry(setup.player.server);
        UUID team = setup.team.id();
        registry.setBalance(team, 5_000);
        long coins = setup.zone().coins();
        helper.assertValueEqual(setup.zone().warChestLeft(), TowerDefense.WAR_CHEST_PER_LEVEL, "level 1 allows 100");
        setup.defense.exchangeCredits(setup.player.server, team, 1_000);
        helper.assertValueEqual(setup.zone().coins(), coins + 100, "only the limit is exchanged");
        helper.assertValueEqual(setup.team.balance(), 4_900L, "100 credits paid");
        helper.assertValueEqual(setup.zone().warChestLeft(), 0L, "used up");
        setup.defense.exchangeCredits(setup.player.server, team, 100);
        helper.assertValueEqual(setup.zone().coins(), coins + 100, "nothing more at level 1");
        helper.assertValueEqual(setup.team.balance(), 4_900L, "and no credits taken");
        setup.defense.clearLevels(setup.player.server, team, 19); // levels 1 to 19 cleared: level 20 next, 2,000 coins per level
        helper.assertValueEqual(setup.zone().level(), 20, "level 20");
        helper.assertValueEqual(setup.zone().warChestLeft(), 2_000L, "level 20: 2,000 coins");
        registry.setBalance(team, 50);
        setup.defense.exchangeCredits(setup.player.server, team, 1_000);
        helper.assertValueEqual(setup.team.balance(), 0L, "not more than the team owns");
        helper.assertValueEqual(setup.zone().coins(), coins + 150, "50 more coins");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 2400)
    public static void roundsPayTheirBonusAndLeaksCostLives(GameTestHelper helper) {
        LevelRun run = track(helper, 1, false);
        double[] coins = new double[1];
        helper.onEachTick(() -> coins[0] += run.takeCoins());
        int[] ticks = new int[1];
        LevelRun.Outcome[] outcome = {LevelRun.Outcome.RUNNING};
        helper.onEachTick(() -> {
            if (outcome[0] == LevelRun.Outcome.RUNNING) {
                outcome[0] = run.tick(helper.getLevel());
                ticks[0]++;
            }
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(outcome[0] == LevelRun.Outcome.WON, "level not won yet: " + outcome[0] + " after " + ticks[0] + " ticks");
            helper.assertValueEqual(run.lives(), 150 - 55, "55 red crawlers leaked, one life each");
            helper.assertTrue(Math.abs(coins[0] - (101 + 102)) < 1e-6, "the two round bonuses are all there is to earn: " + coins[0]);
            run.end(helper.getLevel());
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 2400)
    public static void theNextRoundCanBeCalledAtAnyTime(GameTestHelper helper) {
        LevelRun run = track(helper, 1, false);
        double[] coins = new double[1];
        int[] ticks = new int[1];
        LevelRun.Outcome[] outcome = {LevelRun.Outcome.RUNNING};
        helper.onEachTick(() -> {
            coins[0] += run.takeCoins();
            if (outcome[0] == LevelRun.Outcome.RUNNING) {
                outcome[0] = run.tick(helper.getLevel());
                ticks[0]++;
            }
        });
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(run.canCallWave(), "round 2 can be called while round 1 is still coming");
            helper.assertTrue(run.callNextWave() > 0, "called");
            helper.assertFalse(run.canCallWave(), "there is no round 3 in a level");
            helper.assertValueEqual(run.simulation().round(), 1, "round 2's first enemy has not entered yet");
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(outcome[0] == LevelRun.Outcome.WON, "level not won yet: " + outcome[0] + " after " + ticks[0] + " ticks");
            helper.assertTrue(ticks[0] < 1_000, "the rounds overlap, so the level is over early: " + ticks[0]);
            helper.assertTrue(Math.abs(coins[0] - (101 + 102)) < 1e-6, "no bonus for calling early, both round bonuses as usual: " + coins[0]);
            run.end(helper.getLevel());
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "defense_maps")
    public static void pathsOutsideTheWindowAreRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "WindowTest");
        TowerDefense defense = TowerDefense.get(player.server);
        TowerDefense.Zone zone = defense.ensureArena(player.server, team.id());
        ServerLevel arena = TowerDefense.arena(player.server);
        defense.jumpToLevel(arena, zone, 10); // the colosseum: the gate and the core are in the same row
        for (int x = 0; x < ArenaLayout.SIZE; x++) {
            arena.setBlock(Arenas.field(zone.slot(), x, ArenaLayout.CORE_ROW, Arenas.BUILD_Y), ModBlocks.PATH_BLOCK.get().defaultBlockState(), 3);
        }
        TowerDefense.PathCheck check = defense.checkPath(arena, zone);
        helper.assertTrue(check.path() == null, "a straight path of 41 blocks is accepted");
        helper.assertTrue(check.message().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                && contents.getKey().endsWith("path.too_short"), "the message says the path is too short");
        defense.start(player.server, team.id());
        helper.assertTrue(zone.run().isEmpty(), "no run with a path that is too short");
        helper.succeed();
    }

    /** A run with a swarm queen on it: towers keep shooting at her for a long time without popping her. */
    private static LevelRun target(GameTestHelper helper) {
        LevelRun run = track(helper, 20);
        run.simulation().spawn("swarm_queen");
        return run;
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void aGunTurretFiresTenShotsPerMagazine(GameTestHelper helper) {
        BlockPos towerPos = new BlockPos(3, 1, 4);
        helper.setBlock(towerPos, ModBlocks.GUN_TURRET.get());
        TowerBlockEntity tower = helper.getBlockEntity(towerPos);
        tower.ammo().insertItem(0, new ItemStack(ModItems.MAGAZINE.get(), 2), false);
        helper.assertTrue(tower.ammo().isItemValid(0, new ItemStack(ModItems.AP_MAGAZINE.get())), "AP magazines fit too");
        helper.assertFalse(tower.ammo().isItemValid(0, new ItemStack(ModItems.BOLT.get())), "bolts do not");
        LevelRun run = target(helper);

        helper.runAtTickTime(20, () -> {
            run.end(helper.getLevel());
            // 10 shots per magazine: the first magazine is loaded, the second one still sits in the slot.
            helper.assertValueEqual(tower.ammo().getStackInSlot(0).getCount(), 1, "one magazine used so far");
            helper.assertTrue(tower.shotsLeft() < TowerType.SHOTS_PER_MAGAZINE && tower.shotsLeft() >= 0, "shots left " + tower.shotsLeft());
            helper.assertFalse(tower.armourPiercing(), "a normal magazine");
            helper.succeed();
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void anArmourPiercingMagazineHitsHarder(GameTestHelper helper) {
        BlockPos towerPos = new BlockPos(3, 1, 4);
        helper.setBlock(towerPos, ModBlocks.GUN_TURRET.get());
        TowerBlockEntity tower = helper.getBlockEntity(towerPos);
        tower.ammo().insertItem(0, new ItemStack(ModItems.AP_MAGAZINE.get()), false);
        LevelRun run = target(helper);

        helper.runAtTickTime(20, () -> {
            run.end(helper.getLevel());
            helper.assertTrue(tower.armourPiercing(), "the AP magazine is loaded");
            helper.assertTrue(tower.ammo().getStackInSlot(0).isEmpty(), "and taken from the slot");
            helper.succeed();
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void aUraniumMagazineIsLoadedAndFiredAtFullStrength(GameTestHelper helper) {
        BlockPos towerPos = new BlockPos(3, 1, 4);
        helper.setBlock(towerPos, ModBlocks.GUN_TURRET.get());
        TowerBlockEntity tower = helper.getBlockEntity(towerPos);
        helper.assertTrue(tower.ammo().isItemValid(0, new ItemStack(ModItems.URANIUM_MAGAZINE.get())), "uranium magazines fit");
        helper.assertTrue(de.craftorio.defense.arena.ArenaFeederBlockEntity.isAmmo(new ItemStack(ModItems.URANIUM_MAGAZINE.get())), "the feeder takes them");
        tower.ammo().insertItem(0, new ItemStack(ModItems.URANIUM_MAGAZINE.get()), false);
        LevelRun run = target(helper);
        SimEnemy target = run.simulation().enemies().get(0);
        double before = target.hp();

        helper.runAtTickTime(20, () -> {
            run.end(helper.getLevel());
            helper.assertValueEqual(tower.magazine(), Magazine.URANIUM, "the uranium magazine is loaded");
            helper.assertTrue(tower.ammo().getStackInSlot(0).isEmpty(), "and taken from the slot");
            // 2 damage x 4.8 = 9.6 layers, rounded to 10 (normal damage)
            helper.assertTrue(before - target.hp() >= 8, "damage " + (before - target.hp()));
            helper.succeed();
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 100, batch = "defense_flame")
    public static void theFeederTakesCrudeOilForFlamethrowerTurrets(GameTestHelper helper) {
        Setup setup = buildArena(helper, "FlameTest");
        BlockPos feederPos = new BlockPos(2, 1, 2);
        helper.setBlock(feederPos, ModBlocks.ARENA_FEEDER.get());
        de.craftorio.protection.BlockOwnership.get(helper.getLevel()).claim(helper.absolutePos(feederPos), setup.team.id());
        var oil = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, helper.absolutePos(feederPos), null);
        helper.assertTrue(oil != null, "the feeder has a fluid input");
        helper.assertValueEqual(oil.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 50),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE), 0, "water is refused");
        helper.assertValueEqual(oil.fill(new net.neoforged.neoforge.fluids.FluidStack(de.craftorio.registry.ModFluids.CRUDE_OIL.get(), 50),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE), 50, "crude oil is taken");
        helper.assertValueEqual(setup.zone().fluid(), 50, "into the arena reserve");

        TowerBlockEntity tower = new TowerBlockEntity(BlockPos.ZERO, ModBlocks.FLAMETHROWER_TURRET.get().defaultBlockState());
        helper.assertFalse(tower.lacksSupply(setup.zone()), "50 units are enough for a shot");
        helper.assertTrue(setup.defense.drawFluid(setup.zone().slot(), TowerType.FLAME.fluidPerShot()), "a shot burns oil");
        helper.assertValueEqual(setup.zone().fluid(), 50 - TowerType.FLAME.fluidPerShot(), "reserve after the shot");
        helper.assertFalse(setup.defense.drawFluid(setup.zone().slot(), 1_000), "not more than the reserve holds");
        helper.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 400, batch = "defense_decor")
    public static void decoratedMapsKeepPathsAndTowerSpotsFree(GameTestHelper helper) {
        Setup setup = buildArena(helper, "DecorTest");
        ServerLevel arena = TowerDefense.arena(setup.player.server);
        int slot = setup.zone().slot();
        List<String> problems = new ArrayList<>();
        java.util.Set<ArenaTheme> seen = new java.util.HashSet<>();
        for (int level : new int[]{1, 2, 3, 4, 10, 11, 12}) {
            ArenaLayout layout = ArenaLayout.generate(4_242L + level, level);
            seen.add(layout.theme());
            ArenaBuilder.buildField(arena, slot, layout, 4_242L + level);
            int banners = 0;
            for (int x = 0; x < ArenaLayout.SIZE; x++) {
                for (int z = 0; z < ArenaLayout.SIZE; z++) {
                    Tile tile = layout.tile(x, z);
                    var above = arena.getBlockState(Arenas.field(slot, x, z, Arenas.BUILD_Y));
                    if ((tile == Tile.GROUND || tile == Tile.ROUGH) && !above.isAir() && !above.canBeReplaced()) {
                        problems.add(layout.theme() + " " + x + "," + z + " " + tile + " blocked by " + above.getBlock().getName().getString());
                    }
                    banners += arena.getBlockState(Arenas.field(slot, x, z, Arenas.WALL_TOP - 1)).getBlock() instanceof net.minecraft.world.level.block.WallBannerBlock ? 1 : 0;
                }
            }
            if (banners < 6) {
                problems.add(layout.theme() + " has only " + banners + " banners");
            }
        }
        helper.assertTrue(seen.size() >= 4, "themes tested: " + seen);
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems.subList(0, Math.min(6, problems.size()))));
        helper.succeed();
    }

    private record Setup(ServerPlayer player, Team team, TowerDefense defense) {
        TowerDefense.Zone zone() {
            return defense.zone(team.id()).orElseThrow();
        }
    }

    /** A fresh arena for a new team with the shortest possible path laid from the gate to the core. */
    private static Setup buildArena(GameTestHelper helper, String name) {
        return buildArena(helper, name, 1);
    }

    private static Setup buildArena(GameTestHelper helper, String name, int level) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), name);
        registry.setBalance(team.id(), 0);
        TowerDefense defense = TowerDefense.get(player.server);
        TowerDefense.Zone zone = defense.ensureArena(player.server, team.id());
        ServerLevel arena = TowerDefense.arena(player.server);
        if (level > 1) {
            defense.jumpToLevel(arena, zone, level);
        }
        for (int[] tile : defense.layout(zone).route()) {
            arena.setBlock(Arenas.field(zone.slot(), tile[0], tile[1], Arenas.BUILD_Y), ModBlocks.PATH_BLOCK.get().defaultBlockState(), 3);
        }
        TowerDefense.PathCheck check = defense.checkPath(arena, zone);
        helper.assertTrue(check.path() != null, "path invalid: " + check.message().getString());
        return new Setup(player, team, defense);
    }

    @GameTest(template = EMPTY)
    public static void towerItemsKeepTheirUpgradesAndWhatWasPaid(GameTestHelper helper) {
        ItemStack item = TowerDefense.towerItem(TowerType.GUN, new int[]{3, 2, 0}, 1_200);
        var state = item.get(de.craftorio.registry.ModDataComponents.TOWER_STATE.get());
        helper.assertValueEqual(state.path1() + "-" + state.path2() + "-" + state.path3(), "3-2-0", "upgrades on the item");
        helper.assertValueEqual(state.paid(), 1_200L, "paid on the item");
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.GUN_TURRET.get());
        TowerBlockEntity tower = helper.getBlockEntity(pos);
        tower.applyComponentsFromItemStack(item);
        helper.assertValueEqual(tower.tier(0), 3, "path 1 applied");
        helper.assertValueEqual(tower.tier(1), 2, "path 2 applied");
        helper.assertValueEqual(tower.paid(), 1_200L, "paid applied");
        helper.assertTrue(ItemStack.isSameItemSameComponents(item, TowerDefense.towerItem(TowerType.GUN, new int[]{3, 2, 0}, 1_200)), "equal towers stack");
        helper.assertFalse(ItemStack.isSameItemSameComponents(item, TowerDefense.towerItem(TowerType.GUN, new int[]{3, 2, 0}, 1_201)), "others do not");
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
    public static void towersFromTheDepotDoNotStackWithFreshOnes(GameTestHelper helper) {
        ItemStack fresh = new ItemStack(ModBlocks.CROSSBOW_TOWER.get());
        ItemStack packed = TowerDefense.towerItem(TowerType.CROSSBOW, new int[]{0, 0, 0}, 200);
        helper.assertFalse(ItemStack.isSameItemSameComponents(fresh, packed), "a depot tower is already paid for, a fresh one is not");
        helper.assertTrue(ItemStack.isSameItemSameComponents(packed, TowerDefense.towerItem(TowerType.CROSSBOW, new int[]{0, 0, 0}, 200)), "two equal ones stack");
        helper.assertFalse(ItemStack.isSameItemSameComponents(packed, TowerDefense.towerItem(TowerType.CROSSBOW, new int[]{1, 0, 0}, 340)), "upgraded ones do not");
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
            TowerDefense.addToDepot(zone, TowerDefense.towerItem(TowerType.CROSSBOW, new int[]{0, 0, 0}, 200));
        }
        TowerDefense.addToDepot(zone, TowerDefense.towerItem(TowerType.GUN, new int[]{3, 0, 0}, 1_000));
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
