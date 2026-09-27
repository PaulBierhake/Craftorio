package de.craftorio.team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamRegistryTest {
    private static final long STARTING_CREDITS = 100;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final List<Team> changes = new ArrayList<>();
    private TeamRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TeamRegistry(() -> STARTING_CREDITS);
        registry.setChangeListener(changes::add);
    }

    @Test
    void firstJoinCreatesSoloTeamWithStartingCredits() {
        Team team = registry.ensureTeam(alice, "Alice");

        assertEquals("Alice", team.name());
        assertEquals(STARTING_CREDITS, team.balance());
        assertEquals(team, registry.ensureTeam(alice, "Alice"));
        assertEquals(1, registry.teams().size());
    }

    @Test
    void soloTeamNamesAreMadeUnique() {
        registry.create(bob, "alice");

        assertEquals("Alice 2", registry.ensureTeam(alice, "Alice").name());
    }

    @Test
    void creatingTeamCarriesBalanceOfDissolvedSoloTeam() {
        registry.ensureTeam(alice, "Alice");

        Team team = registry.create(alice, "  Iron Works ");

        assertEquals("Iron Works", team.name());
        assertEquals(STARTING_CREDITS, team.balance());
        assertEquals(1, registry.teams().size());
    }

    @Test
    void teamNamesAreValidatedAndUniqueIgnoringCase() {
        registry.create(alice, "Iron Works");

        assertEquals("craftorio.team.error.name_taken",
                assertThrows(TeamException.class, () -> registry.create(bob, "iron works")).translationKey());
        assertThrows(TeamException.class, () -> registry.create(bob, ""));
        assertThrows(TeamException.class, () -> registry.create(bob, "bad/name"));
        assertThrows(TeamException.class, () -> registry.create(bob, "x".repeat(TeamRegistry.MAX_NAME_LENGTH + 1)));
    }

    @Test
    void joiningRequiresInvite() {
        Team factory = registry.create(alice, "Factory");
        registry.ensureTeam(bob, "Bob");

        assertEquals("craftorio.team.error.not_invited",
                assertThrows(TeamException.class, () -> registry.join(bob, "Factory")).translationKey());

        registry.invite(alice, bob);
        Team joined = registry.join(bob, "factory");

        assertEquals(factory, joined);
        assertTrue(joined.members().contains(bob));
        assertFalse(joined.isInvited(bob));
        assertEquals(joined, registry.teamOf(bob).orElseThrow());
    }

    @Test
    void joiningMovesCreditsOfDissolvedSoloTeam() {
        Team factory = registry.create(alice, "Factory");
        registry.deposit(factory.id(), 50);
        registry.ensureTeam(bob, "Bob");

        registry.invite(alice, bob);
        registry.join(bob, "Factory");

        assertEquals(50 + STARTING_CREDITS, factory.balance());
        assertEquals(1, registry.teams().size());
    }

    @Test
    void leavingGivesEmptySoloTeamAndKeepsOldTeamBalance() {
        Team factory = registry.create(alice, "Factory");
        registry.deposit(factory.id(), 500);
        registry.invite(alice, bob);
        registry.join(bob, "Factory");

        Team solo = registry.leave(bob, "Bob");

        assertEquals(0, solo.balance());
        assertEquals(500, factory.balance());
        assertFalse(factory.members().contains(bob));
        assertNotEquals(factory, registry.teamOf(bob).orElseThrow());
    }

    @Test
    void cannotLeaveWhenAlone() {
        registry.ensureTeam(alice, "Alice");

        assertEquals("craftorio.team.error.alone",
                assertThrows(TeamException.class, () -> registry.leave(alice, "Alice")).translationKey());
    }

    @Test
    void depositSaturatesAndWithdrawChecksFunds() {
        Team team = registry.ensureTeam(alice, "Alice");

        assertFalse(registry.withdraw(team.id(), STARTING_CREDITS + 1));
        assertTrue(registry.withdraw(team.id(), 40));
        assertEquals(STARTING_CREDITS - 40, team.balance());

        registry.deposit(team.id(), Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, team.balance());
        assertThrows(IllegalArgumentException.class, () -> registry.deposit(team.id(), -1));
    }

    @Test
    void changesNotifyListenerButRestoreDoesNot() {
        registry.restore(UUID.randomUUID(), "Loaded", 10, List.of(bob));
        assertTrue(changes.isEmpty());
        assertEquals("Loaded", registry.teamOf(bob).orElseThrow().name());

        Team team = registry.ensureTeam(alice, "Alice");
        registry.deposit(team.id(), 1);

        assertEquals(List.of(team, team), changes);
    }

    @Test
    void blueprintsAndStatisticsMoveWithDissolvedTeam() {
        Team factory = registry.create(alice, "Factory");
        Team solo = registry.ensureTeam(bob, "Bob");
        registry.unlock(solo.id(), "craftorio:press");
        registry.recordSale(solo.id(), "minecraft:iron_ingot", 5, 80);

        registry.invite(alice, bob);
        registry.join(bob, "Factory");

        assertTrue(factory.unlocked().contains("craftorio:press"));
        assertEquals(80, factory.totalEarned());
        assertEquals(new Team.Sales(5, 80), factory.sales().get("minecraft:iron_ingot"));
    }

    @Test
    void leavingKeepsACopyOfBlueprintsButNoCredits() {
        Team factory = registry.create(alice, "Factory");
        registry.unlock(factory.id(), "craftorio:press");
        registry.deposit(factory.id(), 100);
        registry.invite(alice, bob);
        registry.join(bob, "Factory");

        Team solo = registry.leave(bob, "Bob");

        assertTrue(solo.unlocked().contains("craftorio:press"));
        assertTrue(factory.unlocked().contains("craftorio:press"));
        assertEquals(0, solo.balance());
    }

    @Test
    void salesAreCountedPerMinuteAndSpendingIsTracked() {
        long[] now = {0};
        TeamRegistry timed = new TeamRegistry(() -> 0, () -> now[0]);
        Team team = timed.ensureTeam(alice, "Alice");

        timed.recordSale(team.id(), "a", 1, 10);
        now[0] = 1200 * 3;
        timed.recordSale(team.id(), "a", 2, 20);
        now[0] = 1200 * 20;
        timed.recordSale(team.id(), "b", 1, 5);

        assertEquals(35, team.totalEarned());
        assertEquals(5, team.earnedInLastMinutes(TeamRegistry.minuteOf(now[0]), 10), "older minutes fall out of the window");
        assertEquals(new Team.Sales(3, 30), team.sales().get("a"));
        assertTrue(timed.withdraw(team.id(), 15));
        assertEquals(15, team.totalSpent());
    }

    @Test
    void leaderKicksTransfersAndControlsSpending() {
        registry.create(alice, "Factory");
        registry.invite(alice, bob);
        Team team = registry.join(bob, "Factory");
        registry.deposit(team.id(), 1_000);
        long start = team.balance();

        assertEquals(alice, team.leader());
        assertEquals("craftorio.team.error.not_leader",
                assertThrows(TeamException.class, () -> registry.kick(bob, alice, "Alice")).translationKey());
        assertTrue(registry.spend(bob, 100));

        registry.setMembersCanSpend(alice, false);
        assertEquals("craftorio.team.error.no_spend_permission",
                assertThrows(TeamException.class, () -> registry.spend(bob, 100)).translationKey());
        assertTrue(registry.spend(alice, 100));
        assertEquals(start - 200, team.balance());

        registry.transferLeadership(alice, bob);
        assertEquals(bob, team.leader());
        assertTrue(team.canSpend(bob));
        assertFalse(team.canSpend(alice));

        Team kicked = registry.kick(bob, alice, "Alice");
        assertEquals(0, kicked.balance());
        assertEquals(java.util.Set.of(bob), team.members());
        assertEquals("craftorio.team.error.kick_self",
                assertThrows(TeamException.class, () -> registry.kick(bob, bob, "Bob")).translationKey());
    }

    @Test
    void questRewardsArePaidOnce() {
        Team team = registry.ensureTeam(alice, "Alice");
        long before = team.balance();
        assertTrue(registry.claimQuest(team.id(), "first_sale", 500));
        assertFalse(registry.claimQuest(team.id(), "first_sale", 500));
        assertEquals(before + 500, team.balance());
        assertTrue(team.claimedQuests().contains("first_sale"));
    }

    @Test
    void dissolvedTeamsResolveToTheTeamThatTookThemOver() {
        Team solo = registry.ensureTeam(bob, "Bob");
        registry.create(alice, "Factory");
        registry.invite(alice, bob);
        Team factory = registry.join(bob, "Factory");

        assertEquals(factory, registry.resolve(solo.id()).orElseThrow());
        assertEquals(factory, registry.resolve(factory.id()).orElseThrow());
        assertTrue(registry.resolve(UUID.randomUUID()).isEmpty());
    }
}
