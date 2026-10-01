package de.craftorio.defense.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class T4TowersTest {
    private static final List<String> TOWERS = List.of("crossbow_tower", "gun_turret", "flamethrower_turret", "mortar_turret", "supply_depot");

    private static TowerProfile profile(String id, int a, int b, int c) {
        return TowerDefs.profile(TowerDefs.get(id), new int[]{a, b, c});
    }

    @Test
    void everyReachableStateOfEveryTowerBuilds() {
        for (String id : TOWERS) {
            int states = 0;
            for (int a = 0; a <= 5; a++) {
                for (int b = 0; b <= 5; b++) {
                    for (int c = 0; c <= 5; c++) {
                        int[] tiers = {a, b, c};
                        if (!reachable(tiers)) {
                            continue;
                        }
                        TowerProfile profile = TowerDefs.profile(TowerDefs.get(id), tiers);
                        states++;
                        if (TowerDefs.get(id).attack() != null) {
                            assertNotNull(profile.main(), id + " " + TowerRules.notation(tiers));
                            assertTrue(profile.main().cooldown > 0, id + " " + TowerRules.notation(tiers));
                            for (Attack attack : profile.attacks) {
                                assertTrue(attack.damage >= 0 && attack.pierce >= 0, id + " " + attack.id);
                            }
                        }
                    }
                }
            }
            assertEquals(64, states, id);
        }
    }

    private static boolean reachable(int[] want) {
        int[] tiers = new int[3];
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
    void dartMonkeyNumbersFollowTheWikiStatsPages() {
        TowerProfile base = profile("crossbow_tower", 0, 0, 0);
        assertEquals(3.2, base.range, 1e-9);
        assertEquals(0.95, base.main().cooldown, 1e-9);
        assertEquals(2, base.main().pierce);
        TowerProfile razor = profile("crossbow_tower", 2, 0, 0);
        assertEquals(5, razor.main().pierce, "Razor Sharp Shots");
        TowerProfile spike = profile("crossbow_tower", 3, 0, 0);
        assertEquals(18, spike.main().pierce);
        assertEquals(2, spike.main().damage, 1e-9);
        assertEquals(3.68, spike.range, 1e-9);
        assertEquals(1.15, spike.main().cooldown, 1e-9);
        TowerProfile juggernaut = profile("crossbow_tower", 4, 0, 0);
        assertEquals(1.0, juggernaut.main().cooldown, 1e-9);
        assertEquals(60, juggernaut.main().pierce);
        assertEquals(3, juggernaut.main().bonus.get("ceramic"), 1e-9);
        TowerProfile ultra = profile("crossbow_tower", 5, 0, 0);
        assertEquals(210, ultra.main().pierce);
        assertEquals(5, ultra.main().damage, 1e-9);
        assertEquals(1, ultra.main().children.size(), "the mini juggernauts");
        assertEquals(0.6365, profile("crossbow_tower", 0, 2, 0).main().cooldown, 1e-4);
        assertEquals(0.4774, profile("crossbow_tower", 0, 3, 0).main().cooldown, 1e-4);
        assertEquals(3, profile("crossbow_tower", 0, 3, 0).main().projectiles);
        assertEquals(0.2387, profile("crossbow_tower", 0, 4, 0).main().cooldown, 1e-4);
        assertEquals(0.2375, profile("crossbow_tower", 0, 0, 5).main().cooldown, 1e-4);
        assertEquals(8.0, profile("crossbow_tower", 0, 0, 5).range, 1e-9);
        assertTrue(profile("crossbow_tower", 0, 0, 2).detectsCamo, "Enhanced Eyesight");
        assertFalse(profile("crossbow_tower", 0, 0, 1).detectsCamo);
        TowerProfile fanClub = profile("crossbow_tower", 0, 4, 0);
        assertEquals(1, fanClub.abilities.size());
        assertEquals(50, fanClub.abilities.get(0).cooldown(), 1e-9);
        assertEquals(15, fanClub.abilities.get(0).duration(), 1e-9);
        assertEquals(0.0316, profile("crossbow_tower", 0, 5, 0).abilities.get(0).buff().cooldown(), 1e-9);
    }

    @Test
    void sniperNumbersFollowTheWikiStatsPages() {
        assertEquals(1.59, profile("gun_turret", 0, 0, 0).main().cooldown, 1e-9);
        assertEquals(140, profile("gun_turret", 5, 0, 0).main().damage, 1e-9);
        assertEquals(50, profile("gun_turret", 3, 0, 0).main().bonus.get("ceramic"), 1e-9);
        assertEquals(5, profile("gun_turret", 5, 0, 0).main().maim);
        assertEquals(1, profile("gun_turret", 5, 0, 0).main().children.size(), "the explosion");
        assertTrue(profile("gun_turret", 0, 1, 0).main().hitsCamo);
        assertTrue(profile("gun_turret", 0, 1, 0).detectsCamo);
        assertEquals(1, profile("gun_turret", 0, 2, 0).main().children.size(), "shrapnel");
        assertEquals(2, profile("gun_turret", 0, 3, 0).main().bounces);
        assertEquals(1, profile("gun_turret", 0, 4, 0).abilities.size());
        assertEquals(1100, profile("gun_turret", 0, 4, 0).number("supply_drop"), 1e-9);
        assertEquals(3000, profile("gun_turret", 0, 5, 0).number("supply_drop"), 1e-9);
        assertEquals(1.59 * 0.4, profile("gun_turret", 0, 5, 0).main().cooldown, 1e-9);
        assertEquals(1, profile("gun_turret", 0, 5, 0).auras.size(), "the elite sniper's global buff");
        assertEquals(0.0649, profile("gun_turret", 0, 0, 5).main().cooldown, 1e-4);
        assertEquals(1.113, profile("gun_turret", 0, 0, 1).main().cooldown, 1e-3);
        assertTrue(profile("gun_turret", 0, 0, 5).abilities.get(0).passive());
    }

    @Test
    void tackShooterTurnsIntoARingOfFireAndKeepsItsRangeUpgrades() {
        TowerProfile base = profile("flamethrower_turret", 0, 0, 0);
        assertEquals(8, base.main().projectiles);
        assertEquals(2.3, base.range, 1e-9);
        TowerProfile ring = profile("flamethrower_turret", 4, 0, 0);
        assertEquals(AttackKind.AURA, ring.main().kind);
        assertEquals(2.5, ring.main().radius, 1e-9);
        assertEquals(DamageKind.FIRE, ring.main().damageKind);
        TowerProfile wide = profile("flamethrower_turret", 4, 2, 0);
        assertEquals(wide.range + 0.2, wide.main().radius, 1e-9, "range upgrades widen the ring");
        TowerProfile inferno = profile("flamethrower_turret", 5, 0, 0);
        assertEquals(2, inferno.attacks.size(), "ring and meteor");
        assertEquals(700, inferno.attack("meteor").damage, 1e-9);
        assertEquals(3.45, inferno.range, 1e-9);
        assertEquals(3.45 + 0.2, inferno.main().radius, 1e-9);
        TowerProfile maelstrom = profile("flamethrower_turret", 0, 4, 0);
        assertEquals("blade_maelstrom", maelstrom.attack("maelstrom").abilityId);
        assertEquals(3, maelstrom.abilities.get(0).duration(), 1e-9);
        assertEquals(9, profile("flamethrower_turret", 0, 5, 0).abilities.get(0).duration(), 1e-9);
        assertEquals(32, profile("flamethrower_turret", 0, 0, 5).main().projectiles);
        assertEquals(16, profile("flamethrower_turret", 0, 0, 3).main().projectiles);
    }

    @Test
    void mortarUpgradesFollowTheStatsPages() {
        TowerProfile base = profile("mortar_turret", 0, 0, 0);
        assertEquals(22, base.main().pierce);
        assertEquals(1.5, base.main().cooldown, 1e-9);
        TowerProfile heavy = profile("mortar_turret", 3, 0, 0);
        assertEquals(80, heavy.main().pierce);
        assertEquals(4, heavy.main().damage, 1e-9);
        assertEquals(2.7, heavy.main().radius, 1e-9);
        assertEquals(0, heavy.main().pushbackBoss, 1e-9);
        assertEquals(24, profile("mortar_turret", 5, 0, 0).main().damage, 1e-9);
        assertEquals(1.125, profile("mortar_turret", 0, 1, 0).main().cooldown, 1e-9);
        assertEquals(0.825, profile("mortar_turret", 0, 2, 0).main().cooldown, 1e-9);
        TowerProfile assassin = profile("mortar_turret", 0, 4, 0);
        assertEquals(30, assassin.main().bonus.get("moab"), 1e-9);
        assertEquals(750, assassin.attack("assassin").damage, 1e-9);
        assertEquals(4500, profile("mortar_turret", 0, 5, 0).attack("assassin").damage, 1e-9);
        assertEquals(10, profile("mortar_turret", 0, 5, 0).abilities.get(0).cooldown(), 1e-9);
        assertEquals(1, profile("mortar_turret", 0, 0, 3).main().children.size(), "cluster bombs replace the frags");
        assertEquals("secondary", profile("mortar_turret", 0, 0, 4).attack("main.cluster").children.get(0).id);
    }

    @Test
    void theAssassinKillsAMoabClassEnemyOnTheSpot() {
        TdSimulation sim = new TdSimulation(new TdPath(List.of(new double[]{0, 0, 0}, new double[]{60, 0, 0})));
        sim.setRound(40);
        SimEnemy boss = sim.spawn("brood_mother");
        for (int i = 0; i < 100; i++) {
            sim.tick();
        }
        TowerUnit mortar = new TowerUnit(TowerDefs.get("mortar_turret"), 90, 0, 0, 1);
        mortar.setTiers(new int[]{0, 5, 0});
        TowerProfile.Ability used = mortar.activate(0, 1);
        assertNotNull(used);
        for (int i = 0; i < 3; i++) {
            mortar.tick(sim, TowerUnit.Supply.FREE, 1, false);
        }
        assertFalse(boss.alive(), "the boss is gone");
        assertTrue(sim.enemies().stream().noneMatch(e -> e.def().boss()), "the golems it released are no MOAB class: they stay");
    }

    @Test
    void theDepotsIncomeMatchesTheWikiTable() {
        assertEquals(80, DepotEconomy.production(profile("supply_depot", 0, 0, 0)), 1e-9);
        assertEquals(120, DepotEconomy.production(profile("supply_depot", 1, 0, 0)), 1e-9);
        assertEquals(150, DepotEconomy.production(profile("supply_depot", 1, 2, 0)), 1e-9);
        assertEquals(160, DepotEconomy.production(profile("supply_depot", 2, 0, 0)), 1e-9);
        assertEquals(200, DepotEconomy.production(profile("supply_depot", 2, 2, 0)), 1e-9);
        assertEquals(100, DepotEconomy.production(profile("supply_depot", 0, 2, 0)), 1e-9);
        assertEquals(400, DepotEconomy.production(profile("supply_depot", 3, 2, 0)), 1e-9);
        assertEquals(1500, DepotEconomy.production(profile("supply_depot", 4, 0, 0)), 1e-9);
        assertEquals(6000, DepotEconomy.production(profile("supply_depot", 5, 0, 0)), 1e-9);
        assertEquals(7500, DepotEconomy.production(profile("supply_depot", 5, 2, 0)), 1e-9);
        assertEquals(0.8, profile("supply_depot", 0, 0, 2).sellShare(), 1e-9);
    }

    @Test
    void theBankEarnsInterestAndPaysBackLoans() {
        TowerProfile bank = profile("supply_depot", 0, 3, 0);
        DepotEconomy depot = new DepotEconomy();
        assertEquals(100.0, DepotEconomy.production(bank), 1e-9, "valuable bananas");
        depot.roundEnd(bank, false);
        assertEquals(400, depot.bank(), 1e-9);
        depot.roundEnd(bank, false);
        assertEquals(400 * 1.15 + 400, depot.bank(), 1e-9);
        for (int i = 0; i < 40; i++) {
            depot.roundEnd(bank, false);
        }
        assertEquals(7000, depot.bank(), 1e-9, "capped");
        assertEquals(7000, depot.withdraw(), 1e-9);
        assertEquals(0, depot.bank(), 1e-9);

        TowerProfile loan = profile("supply_depot", 0, 4, 0);
        DepotEconomy borrower = new DepotEconomy();
        assertEquals(9000, borrower.loan(loan), 1e-9);
        assertEquals(0, borrower.loan(loan), 1e-9, "only one loan at a time");
        assertEquals(0, borrower.roundEnd(loan, false), 1e-9, "the income repays the loan first");
        assertEquals(9000 - DepotEconomy.production(loan), borrower.debt(), 1e-9);
    }

    @Test
    void theBasketAddsHalfToTheRoundsIncome() {
        TowerProfile profile = profile("supply_depot", 2, 0, 0);
        assertEquals(160, new DepotEconomy().roundEnd(profile, false), 1e-9);
        assertEquals(240, new DepotEconomy().roundEnd(profile, true), 1e-9);
    }
}
