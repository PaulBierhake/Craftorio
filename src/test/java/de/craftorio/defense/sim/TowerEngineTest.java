package de.craftorio.defense.sim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TowerEngineTest {
    private static TdSimulation sim(double length) {
        return new TdSimulation(new TdPath(List.of(new double[]{0, 0, 0}, new double[]{length, 0, 0})));
    }

    @Test
    void theCrossPathRuleAllowsOneLongPathAndOneShortOne() {
        assertTrue(TowerRules.canUpgrade(new int[]{0, 0, 0}, 0));
        assertTrue(TowerRules.canUpgrade(new int[]{4, 2, 0}, 0), "5-2-0");
        assertTrue(TowerRules.canUpgrade(new int[]{4, 0, 2}, 0), "5-0-2");
        assertTrue(TowerRules.canUpgrade(new int[]{2, 0, 4}, 2), "2-0-5");
        assertTrue(TowerRules.canUpgrade(new int[]{0, 1, 2}, 1), "0-2-2");
        assertTrue(TowerRules.canUpgrade(new int[]{2, 2, 0}, 0), "3-2-0");
        assertFalse(TowerRules.canUpgrade(new int[]{5, 0, 0}, 0), "tier 5 is the end");
        assertFalse(TowerRules.canUpgrade(new int[]{5, 2, 0}, 1), "5-3-0");
        assertFalse(TowerRules.canUpgrade(new int[]{3, 2, 0}, 1), "3-3-0");
        assertFalse(TowerRules.canUpgrade(new int[]{2, 2, 0}, 2), "2-2-1: three paths started");
        assertTrue(TowerRules.canUpgrade(new int[]{3, 0, 0}, 1), "3-1-0");
        assertFalse(TowerRules.canUpgrade(new int[]{3, 1, 0}, 2), "3-1-1");
        assertEquals("crosspath", TowerRules.lockReason(new int[]{5, 2, 0}, 1));
        assertEquals("maxed", TowerRules.lockReason(new int[]{5, 2, 0}, 0));
    }

    @Test
    void everyReachableStateMatchesTheSetsOfTheWiki() {
        // BTD6: exactly these 1 + 3 * 5 + ... states exist; count them by the rule
        int states = 0;
        for (int a = 0; a <= 5; a++) {
            for (int b = 0; b <= 5; b++) {
                for (int c = 0; c <= 5; c++) {
                    if (reachable(a, b, c)) {
                        states++;
                    }
                }
            }
        }
        assertEquals(64, states, "the wiki lists 64 cost rows per tower");
    }

    /** Can this state be built by upgrading one tier at a time? */
    private static boolean reachable(int a, int b, int c) {
        int[] want = {a, b, c};
        int[] tiers = new int[3];
        // upgrade the path that ends up highest first, then the others
        Integer[] order = {0, 1, 2};
        java.util.Arrays.sort(order, (x, y) -> Integer.compare(want[y], want[x]));
        for (int path : order) {
            while (tiers[path] < want[path]) {
                if (!TowerRules.canUpgrade(tiers, path)) {
                    return false;
                }
                tiers[path]++;
            }
        }
        return true;
    }

    @Test
    void techResearchGatesTheThirdToFifthTier() {
        assertEquals(0, TowerRules.techRequired(1));
        assertEquals(0, TowerRules.techRequired(2));
        assertEquals(1, TowerRules.techRequired(3));
        assertEquals(2, TowerRules.techRequired(4));
        assertEquals(3, TowerRules.techRequired(5));
    }

    @Test
    void pricesAndSellValuesFollowTheDifficultyAndSeventyPercent() {
        assertEquals(170, TowerRules.price(200, 0.85));
        assertEquals(216, TowerRules.price(200, 1.08));
        assertEquals(240, TowerRules.price(200, 1.2));
        assertEquals(140, TowerRules.sellValue(200, RoundRules.SELL_SHARE));
        assertEquals(238, TowerRules.sellValue(340, RoundRules.SELL_SHARE));
        assertEquals(12_362, TowerRules.sellValue(17_660, RoundRules.SELL_SHARE));
        assertEquals("5-2-0", TowerRules.notation(new int[]{5, 2, 0}));
    }

    /** Wiki: "Costs and sell values" of every tower: the total price of each of the 64 states and what selling it pays. */
    @Test
    void towerPricesAndSellValuesMatchTheWikiForEveryState() throws Exception {
        JsonObject wiki = JsonParser.parseReader(new InputStreamReader(getClass().getResourceAsStream("/td/wiki_tower_costs.json"), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("towers");
        assertEquals(TowerDefs.IDS.size(), wiki.size());
        for (String id : TowerDefs.IDS) {
            TowerDef def = TowerDefs.get(id);
            int checked = 0;
            for (Map.Entry<String, com.google.gson.JsonElement> row : wiki.getAsJsonObject(id).entrySet()) {
                String[] parts = row.getKey().split("-");
                long total = def.cost();
                int[] tiers = new int[3];
                for (int path = 0; path < 3; path++) {
                    tiers[path] = Integer.parseInt(parts[path]);
                    for (int tier = 1; tier <= tiers[path]; tier++) {
                        total += def.upgrade(path, tier).cost();
                    }
                }
                JsonObject expected = row.getValue().getAsJsonObject();
                assertEquals(expected.get("total").getAsLong(), total, id + " " + row.getKey());
                assertEquals(expected.get("sell").getAsLong(), TowerRules.sellValue(total, TowerDefs.profile(def, tiers).sellShare()), id + " sell " + row.getKey());
                checked++;
            }
            assertEquals(64, checked, id);
        }
    }

    @Test
    void everyTowerHasFivePathsOfTiersWithNamesAndCostsAndItsBasePriceOfTheDoc() {
        Map<String, Long> prices = Map.of("crossbow_tower", 200L, "gun_turret", 350L, "mortar_turret", 375L, "flamethrower_turret", 260L,
                "frost_tower", 400L, "glue_turret", 225L, "tesla_tower", 250L, "laser_tower", 2_500L, "command_post", 1_200L, "supply_depot", 1_250L);
        for (TowerDef def : TowerDefs.all()) {
            assertEquals(prices.get(def.id()), def.cost(), def.id());
            for (int path = 0; path < 3; path++) {
                long previous = 0;
                for (int tier = 1; tier <= 5; tier++) {
                    TowerDef.Upgrade upgrade = def.upgrade(path, tier);
                    assertFalse(upgrade.btd6().isBlank(), def.id());
                    assertTrue(upgrade.cost() > 0, def.id() + " " + upgrade.btd6());
                    assertTrue(upgrade.cost() >= previous / 20 || tier == 1, "suspicious price " + def.id() + " " + upgrade.btd6());
                    previous = upgrade.cost();
                }
            }
        }
    }

    // --- the attack engine

    private static TowerUnit unit(String id, double x, double z) {
        return new TowerUnit(TowerDefs.get(id), x, 0, z, 1);
    }

    @Test
    void aProjectilePopsAsManyEnemiesAsItsPierceOnTheWayThroughTheTarget() {
        TdSimulation sim = sim(100);
        for (int i = 0; i < 5; i++) {
            SimEnemy enemy = sim.spawn("red_crawler");
            for (int t = 0; t < i * 4; t++) {
                sim.tick();
            }
        }
        // the first crawler is furthest, all in a row along the path; a tower at x=5, z=2 shoots at the first one
        TowerUnit tower = unit("crossbow_tower", 3, 2);
        sim.tick();
        List<TowerUnit.Shot> shots = tower.tick(sim, TowerUnit.Supply.FREE, 1, false);
        assertEquals(1, shots.size());
        assertTrue(shots.get(0).hits().size() <= 2, "pierce 2");
        assertTrue(sim.paidPops() <= 2);
    }

    @Test
    void towersWaitForTheirCooldownAndIgnoreCamoWithoutDetection() {
        TdSimulation sim = sim(100);
        sim.spawn("red_crawler", true, false, false);
        TowerUnit tower = unit("crossbow_tower", 0.3, 1);
        assertTrue(tower.tick(sim, TowerUnit.Supply.FREE, 1, false).isEmpty(), "camo is invisible to it");
        assertEquals(1, tower.tick(sim, TowerUnit.Supply.FREE, 1, true).size(), "with a village's radar it shoots");
        assertEquals(0, sim.count());
        for (int i = 0; i < 40; i++) {
            sim.spawn("red_crawler");
        }
        TowerUnit fresh = unit("crossbow_tower", 0.3, 1);
        int shots = 0;
        for (int tick = 0; tick < 18; tick++) {
            shots += fresh.tick(sim, TowerUnit.Supply.FREE, 1, false).size();
        }
        assertEquals(1, shots, "it shoots at once, then only every 0.95 s (19 ticks)");
        for (int tick = 0; tick < 18; tick++) {
            shots += fresh.tick(sim, TowerUnit.Supply.FREE, 1, false).size();
        }
        assertEquals(2, shots, "the second shot comes on the 19th tick");
        for (int tick = 0; tick < 19; tick++) {
            shots += fresh.tick(sim, TowerUnit.Supply.FREE, 1, false).size();
        }
        assertEquals(3, shots, "and the third 19 ticks later");
    }

    @Test
    void theSnipersReachIsUnlimitedAndAPayingSupplyIsRequired() {
        TdSimulation sim = sim(1_000);
        SimEnemy far = sim.spawn("red_crawler");
        for (int tick = 0; tick < 2_000 && far.distance() < 900; tick++) {
            sim.tick();
        }
        TowerUnit sniper = unit("gun_turret", 0, 3);
        assertTrue(sniper.tick(sim, (def, attack) -> false, 1, false).isEmpty(), "without ammunition it does not shoot");
        List<TowerUnit.Shot> shots = sniper.tick(sim, TowerUnit.Supply.FREE, 1, false);
        assertEquals(1, shots.size(), "a crawler 900 blocks away is in reach");
        assertEquals(0, sim.count());
    }

    @Test
    void theMortarBlastHitsEveryoneAroundTheTargetAndTheFlameRingAllAroundTheTower() {
        TdSimulation sim = sim(100);
        for (int i = 0; i < 6; i++) {
            sim.spawn("red_crawler");
            for (int t = 0; t < 2; t++) {
                sim.tick();
            }
        }
        TowerUnit mortar = unit("mortar_turret", 2, 2);
        int before = sim.count();
        TowerUnit.Shot shot = mortar.tick(sim, TowerUnit.Supply.FREE, 1, false).get(0);
        assertTrue(shot.hits().size() >= 3, "a blast of 1.2 blocks catches several crawlers");
        assertEquals(before - shot.hits().size(), sim.count());

        TdSimulation around = sim(100);
        for (int i = 0; i < 3; i++) {
            around.spawn("red_crawler");
            for (int t = 0; t < 4; t++) {
                around.tick();
            }
        }
        TowerUnit flame = unit("flamethrower_turret", 1.5, 0.5);
        TowerUnit.Shot ring = flame.tick(around, TowerUnit.Supply.FREE, 1, false).get(0);
        assertEquals(8, ring.attack().projectiles);
        assertFalse(ring.hits().isEmpty());
    }

    @Test
    void theHitsOfTheFrostAuraAreLimitedToTheTowersSurroundings() {
        TdSimulation sim = sim(100);
        sim.spawn("red_crawler");
        TowerUnit frost = unit("frost_tower", 0.5, 1);
        TowerUnit.Shot shot = frost.tick(sim, TowerUnit.Supply.FREE, 1, false).get(0);
        assertEquals(AttackKind.AURA, shot.attack().kind);
        assertEquals(1, shot.hits().size());
        assertEquals(0, sim.count(), "cold damage 1 pops a red crawler");
    }

    @Test
    void leadIgnoresSharpAndColdButNotExplosions() {
        TdSimulation sim = sim(100);
        SimEnemy lead = sim.spawn("ironbreaker");
        TowerUnit crossbow = unit("crossbow_tower", 0.3, 1);
        crossbow.tick(sim, TowerUnit.Supply.FREE, 1, false);
        assertEquals("ironbreaker", lead.def().id());
        assertEquals(1, lead.hp(), 0);
        TowerUnit mortar = unit("mortar_turret", 0.3, 1);
        mortar.tick(sim, TowerUnit.Supply.FREE, 1, false);
        assertFalse(lead.alive(), "the mortar's explosion pops it");
    }

    @Test
    void theUpgradedProfileAppliesEffectsPathAfterPath() {
        TowerDef def = TowerDefs.get("crossbow_tower");
        TowerProfile base = TowerProfile.of(def, new int[]{0, 0, 0});
        assertEquals(3.2, base.range, 0);
        assertEquals(2, base.main().pierce);
        assertFalse(base.detectsCamo);
        JsonObject pierce = JsonParser.parseString("{\"type\":\"attack\",\"add\":{\"pierce\":1},\"mul\":{\"cooldown\":0.85},\"bonus\":{\"ceramic\":3}}").getAsJsonObject();
        TowerProfile profile = new TowerProfile();
        profile.range = def.range();
        profile.attacks.add(def.attack().copy());
        profile.apply(pierce);
        profile.apply(pierce);
        assertEquals(4, profile.main().pierce);
        assertEquals(0.95 * 0.85 * 0.85, profile.main().cooldown, 1e-9);
        assertEquals(6, profile.main().bonus.get("ceramic"), 0);
        profile.apply(JsonParser.parseString("{\"type\":\"range\",\"add\":0.8}").getAsJsonObject());
        profile.apply(JsonParser.parseString("{\"type\":\"camo\"}").getAsJsonObject());
        assertEquals(4.0, profile.range, 1e-9);
        assertTrue(profile.detectsCamo);
        profile.apply(JsonParser.parseString("{\"type\":\"ability\",\"id\":\"fan_club\",\"cooldown\":50,\"duration\":15}").getAsJsonObject());
        assertEquals(new TowerProfile.Ability("fan_club", 50, 15), profile.abilities.get(0));
    }

    @Test
    void bonusDamageCountsForTheClassesAnEnemyBelongsTo() {
        Attack attack = new Attack();
        attack.damage = 2;
        attack.bonus.put("ceramic", 3.0);
        attack.bonus.put("fortified", 2.0);
        attack.bonus.put("moab", 10.0);
        TdSimulation sim = sim(100);
        assertEquals(2, attack.damageAgainst(sim.spawn("red_crawler")), 0);
        assertEquals(5, attack.damageAgainst(sim.spawn("crystal_golem")), 0);
        assertEquals(7, attack.damageAgainst(sim.spawn("crystal_golem", false, false, true)), 0);
        assertEquals(12, attack.damageAgainst(sim.spawn("brood_mother")), 0);
        assertEquals(14, attack.damageAgainst(sim.spawn("brood_mother", false, false, true)), 0);
    }

    @Test
    void targetModesPickFirstLastCloseAndStrong() {
        TdSimulation sim = sim(100);
        SimEnemy first = sim.spawn("pink_crawler");
        for (int t = 0; t < 80; t++) {
            sim.tick();
        }
        SimEnemy second = sim.spawn("shimmer_crawler");
        for (int t = 0; t < 20; t++) {
            sim.tick();
        }
        double tx = second.x();
        assertEquals(first.id(), sim.bestTarget(tx, 1, -1, true, TargetMode.FIRST.order(tx, 0, 1), null).id());
        assertEquals(second.id(), sim.bestTarget(tx, 1, -1, true, TargetMode.LAST.order(tx, 0, 1), null).id());
        assertEquals(second.id(), sim.bestTarget(tx, 1, -1, true, TargetMode.CLOSE.order(tx, 0, 1), null).id());
        assertEquals(second.id(), sim.bestTarget(tx, 1, -1, true, TargetMode.STRONG.order(tx, 0, 1), null).id(), "the rainbow has more RBE");
        assertEquals(TargetMode.LAST, TargetMode.next(TargetMode.FIRST, List.of(TargetMode.FIRST, TargetMode.LAST)));
        assertEquals(TargetMode.FIRST, TargetMode.next(TargetMode.LAST, List.of(TargetMode.FIRST, TargetMode.LAST)));
    }

    @Test
    void abilitiesHaveACooldownAndADuration() {
        JsonObject ability = JsonParser.parseString("{\"type\":\"ability\",\"id\":\"fan_club\",\"cooldown\":50,\"duration\":15}").getAsJsonObject();
        TowerDef.Upgrade withAbility = new TowerDef.Upgrade("Fan Club", 1, List.of(ability));
        TowerDef.Upgrade plain = new TowerDef.Upgrade("Plain", 1, List.of());
        List<TowerDef.Upgrade> path = List.of(withAbility, plain, plain, plain, plain);
        List<TowerDef.Upgrade> empty = List.of(plain, plain, plain, plain, plain);
        TowerDef def = new TowerDef("ability_test_tower", "test", 1, 3, "none", 0, List.of(TargetMode.FIRST),
                TowerDefs.get("crossbow_tower").attack(), List.of(path, empty, empty));
        TowerUnit unit = new TowerUnit(def, 0, 0, 0, 1);
        assertEquals(0, unit.abilityCount());
        assertFalse(unit.abilityReady(0));
        assertNull(unit.activate(0, 1));
        unit.setTiers(new int[]{1, 0, 0});
        assertEquals(1, unit.abilityCount());
        assertTrue(unit.abilityReady(0));
        assertEquals("fan_club", unit.activate(0, 1).id());
        assertFalse(unit.abilityReady(0));
        assertNull(unit.activate(0, 1), "not while it is cooling down");
        assertEquals(1_000, unit.abilityCooldown(0));
        assertEquals(300, unit.abilityActive(0));
        assertTrue(unit.abilityIsActive("fan_club"));
        TdSimulation sim = sim(100);
        for (int tick = 0; tick < 299; tick++) {
            unit.tick(sim, TowerUnit.Supply.FREE, 1, false);
        }
        assertTrue(unit.abilityIsActive("fan_club"));
        unit.tick(sim, TowerUnit.Supply.FREE, 1, false);
        assertFalse(unit.abilityIsActive("fan_club"), "15 s are over");
        assertEquals(700, unit.abilityCooldown(0));
        for (int tick = 0; tick < 700; tick++) {
            unit.tick(sim, TowerUnit.Supply.FREE, 1, false);
        }
        assertTrue(unit.abilityReady(0), "50 s are over");
        unit.activate(0, 0.75);
        assertEquals(750, unit.abilityCooldown(0), "a village shortens the cooldown");
    }
}
