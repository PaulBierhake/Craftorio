package de.craftorio.defense.sim;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class T5TowersTest {
    private static final List<String> TOWERS = List.of("frost_tower", "glue_turret", "tesla_tower", "laser_tower", "command_post");

    private static TowerProfile profile(String id, int a, int b, int c) {
        return TowerDefs.profile(TowerDefs.get(id), new int[]{a, b, c});
    }

    private static TdSimulation lane() {
        TdSimulation sim = new TdSimulation(new TdPath(List.of(new double[]{0, 0, 0}, new double[]{60, 0, 0})));
        sim.setRound(30);
        return sim;
    }

    @Test
    void everyReachableStateOfEveryTowerBuilds() {
        for (String id : TOWERS) {
            int states = 0;
            for (int a = 0; a <= 5; a++) {
                for (int b = 0; b <= 5; b++) {
                    for (int c = 0; c <= 5; c++) {
                        int[] tiers = {a, b, c};
                        boolean ok = true;
                        int[] built = new int[3];
                        Integer[] order = {0, 1, 2};
                        java.util.Arrays.sort(order, (x, y) -> Integer.compare(tiers[y], tiers[x]));
                        for (int path : order) {
                            while (built[path] < tiers[path] && ok) {
                                ok = TowerRules.canUpgrade(built, path);
                                built[path]++;
                            }
                        }
                        if (!ok) {
                            continue;
                        }
                        TowerProfile profile = TowerDefs.profile(TowerDefs.get(id), tiers);
                        states++;
                        for (Attack attack : profile.attacks) {
                            assertTrue(attack.cooldown > 0 && attack.damage >= 0, id + " " + TowerRules.notation(tiers) + " " + attack.id);
                        }
                        if (TowerDefs.get(id).attack() != null) {
                            assertNotNull(profile.main());
                        }
                    }
                }
            }
            assertEquals(64, states, id);
        }
    }

    @Test
    void whoGetsHitByWhatFollowsTheTableOfTheConcept() {
        String[] kinds = {"sharp", "explosion", "cold", "energy", "fire", "normal"};
        Map<String, boolean[]> table = Map.of(
                "soot_crawler", new boolean[]{true, false, true, true, true, true},
                "frost_crawler", new boolean[]{true, true, false, true, true, true},
                "ember_crawler", new boolean[]{true, true, true, false, false, true},
                "ironbreaker", new boolean[]{false, true, false, false, true, true},
                "twilight_crawler", new boolean[]{true, false, false, true, true, true},
                "shadow_hunter", new boolean[]{false, false, false, false, true, true});
        table.forEach((id, hits) -> {
            EnemyDef def = EnemyDefs.get(id);
            for (int i = 0; i < kinds.length; i++) {
                assertEquals(hits[i], !def.isImmune(DamageKind.byId(kinds[i])), id + " vs " + kinds[i]);
            }
        });
    }

    @Test
    void glacierAndPlasmaHurtLeadButNotWhiteOrPurple() {
        assertFalse(EnemyDefs.get("ironbreaker").isImmune(DamageKind.GLACIER));
        assertFalse(EnemyDefs.get("ironbreaker").isImmune(DamageKind.PLASMA));
        assertFalse(EnemyDefs.get("shadow_hunter").isImmune(DamageKind.PLASMA));
        assertTrue(EnemyDefs.get("frost_crawler").isImmune(DamageKind.GLACIER));
        assertTrue(EnemyDefs.get("ember_crawler").isImmune(DamageKind.PLASMA));
        assertFalse(EnemyDefs.get("soot_crawler").isImmune(DamageKind.GLACIER));
    }

    @Test
    void iceMonkeyNumbersFollowTheWikiStatsPages() {
        TowerProfile base = profile("frost_tower", 0, 0, 0);
        assertEquals(2.0, base.range, 1e-9);
        assertEquals(2.4, base.main().cooldown, 1e-9);
        assertEquals(1.5, base.main().freeze, 1e-9);
        assertTrue(base.main().skipBoss && base.main().skipFrozen);
        assertEquals(DamageKind.GLACIER, profile("frost_tower", 2, 0, 0).main().damageKind);
        assertTrue(profile("frost_tower", 2, 0, 0).detectsCamo);
        assertEquals(1.8, profile("frost_tower", 0, 1, 0).main().cooldown, 1e-9);
        assertEquals(2.2, profile("frost_tower", 0, 2, 0).main().freeze, 1e-9);
        assertEquals(1, profile("frost_tower", 0, 3, 0).attacks.stream().filter(a -> a.id.equals("arctic_wind")).count());
        assertEquals(1, profile("frost_tower", 0, 4, 0).abilities.size());
        assertEquals(30, profile("frost_tower", 0, 4, 0).abilities.get(0).cooldown(), 1e-9);
        assertEquals(25, profile("frost_tower", 0, 5, 0).abilities.get(0).cooldown(), 1e-9);
        assertEquals(2.7, profile("frost_tower", 0, 0, 1).range, 1e-9);
        assertFalse(profile("frost_tower", 0, 0, 2).main().skipFrozen, "Re-Freeze");
        assertEquals(1.2, profile("frost_tower", 0, 0, 3).main().cooldown, 1e-9);
        assertEquals(1.2, profile("frost_tower", 5, 0, 0).main().cooldown, 1e-9);
        assertEquals(2.5, profile("frost_tower", 0, 0, 0).main().radius + 0.5, 1e-9);
    }

    @Test
    void frozenEnemiesStandStillAndTheRingFollowsTheRange() {
        TdSimulation sim = lane();
        SimEnemy red = sim.spawn("crystal_golem");
        for (int i = 0; i < 60; i++) {
            sim.tick();
        }
        TowerUnit ice = new TowerUnit(TowerDefs.get("frost_tower"), red.x(), 0, 1.0, 1);
        double before = red.x();
        assertEquals(1, ice.tick(sim, TowerUnit.Supply.FREE, 1, false).size());
        assertTrue(red.frozen());
        for (int i = 0; i < 20; i++) {
            sim.tick();
        }
        assertEquals(before, red.x(), 1e-6, "frozen: it did not move");
        assertTrue(ice.tick(sim, TowerUnit.Supply.FREE, 1, false).isEmpty(), "no target while it is frozen and the cooldown runs");
        TowerProfile wide = profile("frost_tower", 0, 0, 1);
        assertEquals(wide.range, wide.main().radius, 1e-9, "the ring grows with the range");
    }

    @Test
    void strongerGlueReplacesWeakerButNotTheOtherWayAround() {
        TdSimulation sim = lane();
        SimEnemy red = sim.spawn("red_crawler");
        sim.slow(red, 0.5, 11, false, 1);
        assertEquals(1, red.glueLevelNow());
        sim.slow(red, 0.25, 24, false, 3);
        assertEquals(3, red.glueLevelNow());
        sim.slow(red, 0.5, 11, false, 1);
        assertEquals(3, red.glueLevelNow(), "weak glue does not replace strong glue");
        // a glue gunner ignores enemies that already carry glue as strong as its own
        TowerUnit glue = new TowerUnit(TowerDefs.get("glue_turret"), red.x(), 0, 2, 1);
        assertTrue(glue.tick(sim, TowerUnit.Supply.FREE, 1, false).isEmpty());
    }

    @Test
    void glueGunnerNumbersFollowTheWikiStatsPages() {
        TowerProfile base = profile("glue_turret", 0, 0, 0);
        assertEquals(0.5, base.main().glue, 1e-9);
        assertEquals(11, base.main().glueDuration, 1e-9);
        assertTrue(base.main().skipBoss);
        assertEquals(2, profile("glue_turret", 1, 0, 0).main().glueLevel, 0.0 + 1);
        assertEquals(10.0, profile("glue_turret", 4, 0, 0).main().burn, 1e-9);
        assertEquals(24, profile("glue_turret", 0, 0, 1).main().glueDuration, 1e-9);
        assertEquals(0.25, profile("glue_turret", 0, 0, 2).main().glue, 1e-9);
        assertFalse(profile("glue_turret", 0, 0, 3).main().skipBoss, "MOAB Glue");
        assertEquals(9, profile("glue_turret", 0, 0, 3).main().glueBossSeconds, 1e-9);
        assertEquals(0.34, profile("glue_turret", 0, 3, 0).main().cooldown, 1e-9);
        assertEquals(1, profile("glue_turret", 0, 4, 0).abilities.size());
        assertEquals(10, profile("glue_turret", 0, 5, 0).abilities.get(0).duration(), 1e-9);
    }

    @Test
    void wizardMonkeyNumbersFollowTheWikiStatsPages() {
        assertEquals(1.1, profile("tesla_tower", 0, 0, 0).main().cooldown, 1e-9);
        assertTrue(profile("tesla_tower", 1, 0, 0).main().homing);
        assertEquals(6.0, profile("tesla_tower", 3, 0, 0).range, 1e-9);
        assertEquals(0.55, profile("tesla_tower", 3, 0, 0).main().cooldown, 1e-9);
        assertEquals(0.275, profile("tesla_tower", 4, 0, 0).main().cooldown, 1e-9);
        assertEquals(DamageKind.PLASMA, profile("tesla_tower", 4, 0, 0).main().damageKind);
        assertEquals(19, profile("tesla_tower", 5, 0, 0).main().bonus.get("moab"), 1e-9);
        assertEquals(0.1375, profile("tesla_tower", 5, 0, 0).main().cooldown, 1e-9);
        assertEquals(1, profile("tesla_tower", 0, 1, 0).attacks.stream().filter(a -> a.id.equals("fireball")).count());
        assertEquals(1, profile("tesla_tower", 0, 2, 0).attacks.stream().filter(a -> a.id.equals("wall_of_fire")).count());
        assertEquals(1, profile("tesla_tower", 0, 3, 0).attacks.stream().filter(a -> a.id.equals("dragons_breath")).count());
        assertEquals(8, profile("tesla_tower", 0, 0, 1).main().pierce);
        assertTrue(profile("tesla_tower", 0, 0, 2).detectsCamo);
        assertEquals(1, profile("tesla_tower", 0, 0, 3).attacks.stream().filter(a -> a.id.equals("shimmer") && a.camoOnly).count());
    }

    @Test
    void theShimmerStripsCamoFromEnemiesInAWideArea() {
        TdSimulation sim = lane();
        SimEnemy camo = sim.spawn("red_crawler", true, false, false);
        SimEnemy plain = sim.spawn("red_crawler");
        for (int i = 0; i < 40; i++) {
            sim.tick();
        }
        assertTrue(camo.camo());
        TowerUnit wizard = new TowerUnit(TowerDefs.get("tesla_tower"), camo.x(), 0, 2.5, 1);
        wizard.setTiers(new int[]{0, 0, 3});
        for (int i = 0; i < 4; i++) {
            wizard.tick(sim, TowerUnit.Supply.FREE, 1, false);
        }
        assertFalse(camo.camo() && camo.alive(), "the shimmer removed the camouflage (or the shot popped it)");
        assertNotNull(plain);
    }

    @Test
    void superMonkeyNumbersFollowTheWikiStatsPages() {
        TowerProfile base = profile("laser_tower", 0, 0, 0);
        assertEquals(5.0, base.range, 1e-9);
        assertEquals(DamageKind.ENERGY, profile("laser_tower", 1, 0, 0).main().damageKind);
        assertEquals(DamageKind.PLASMA, profile("laser_tower", 2, 0, 0).main().damageKind);
        assertEquals(3, profile("laser_tower", 3, 0, 0).main().projectiles);
        assertEquals(15, profile("laser_tower", 5, 0, 0).main().damage, 1e-9);
        assertEquals(6.5, profile("laser_tower", 4, 0, 0).range, 1e-9);
        assertEquals(6.0, profile("laser_tower", 0, 1, 0).range, 1e-9);
        assertEquals(7.2, profile("laser_tower", 0, 2, 0).range, 1e-9);
        assertEquals(1, profile("laser_tower", 0, 4, 0).abilities.size());
        assertEquals(2600, profile("laser_tower", 0, 4, 0).attack("tech_terror").damage, 1e-9);
        assertEquals(10400, profile("laser_tower", 0, 5, 0).attack("anti_bloon").damage, 1e-9);
        assertEquals(1, profile("laser_tower", 0, 5, 0).abilities.size());
        assertTrue(profile("laser_tower", 0, 0, 2).detectsCamo);
        assertEquals(25, profile("laser_tower", 0, 0, 5).main().bonus.get("moab"), 1e-9);
        assertEquals(4, profile("laser_tower", 0, 0, 5).main().bonus.get("camo"), 1e-9);
    }

    @Test
    void theVillagesAurasAndEffects() {
        TowerProfile base = profile("command_post", 0, 0, 0);
        assertEquals(4.0, base.range, 1e-9);
        assertTrue(base.attacks.isEmpty());
        assertEquals(5.0, profile("command_post", 1, 0, 0).range, 1e-9);
        TowerProfile drums = profile("command_post", 2, 0, 0);
        assertEquals(0.85, drums.auras.stream().filter(a -> a.id().equals("village_drums")).findFirst().orElseThrow().buff().cooldown(), 1e-9);
        TowerProfile training = profile("command_post", 3, 0, 0);
        TowerProfile.Aura primary = training.auras.stream().filter(a -> a.id().equals("primary_training")).findFirst().orElseThrow();
        assertTrue(primary.appliesTo("crossbow_tower") && primary.appliesTo("glue_turret"));
        assertFalse(primary.appliesTo("laser_tower") || primary.appliesTo("gun_turret"));
        assertEquals(3, profile("command_post", 5, 0, 0).auras.stream().filter(a -> a.id().equals("primary_training")).findFirst().orElseThrow().buff().pierce());
        assertEquals(1, profile("command_post", 5, 0, 0).attacks.size(), "the mega ballista");
        assertEquals(1, profile("command_post", 0, 1, 0).attacks.size(), "the grow blocker");
        assertTrue(profile("command_post", 0, 2, 0).auras.stream().anyMatch(a -> a.buff().camo()), "radar");
        assertEquals(DamageKind.NORMAL, profile("command_post", 0, 3, 0).auras.stream().filter(a -> a.id().equals("intelligence")).findFirst().orElseThrow().buff().kind());
        assertEquals("call_to_arms", profile("command_post", 0, 4, 0).abilities.get(0).id());
        assertEquals("homeland_defense", profile("command_post", 0, 5, 0).abilities.get(0).id());
        assertEquals(0.10, profile("command_post", 0, 0, 1).number("discount"), 1e-9);
        assertEquals(0.15, profile("command_post", 0, 0, 2).number("discount"), 1e-9);
        assertEquals(90, TowerRules.discounted(100, 0.10));
        assertEquals(1700, TowerRules.discounted(2000, 0.15));
    }

    @Test
    void theGrowBlockerRemovesCamoAndRegrowInTheVillagesRadius() {
        TdSimulation sim = lane();
        SimEnemy camo = sim.spawn("yellow_crawler", true, true, false);
        for (int i = 0; i < 40; i++) {
            sim.tick();
        }
        TowerUnit village = new TowerUnit(TowerDefs.get("command_post"), camo.x(), 0, 2.0, 1);
        village.setTiers(new int[]{0, 1, 0});
        for (int i = 0; i < 12; i++) {
            village.tick(sim, TowerUnit.Supply.FREE, 1, false);
        }
        assertFalse(camo.regrow(), "no regrowing");
        assertFalse(camo.camo(), "no camouflage");
    }
}
