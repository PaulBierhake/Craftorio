package de.craftorio.quest;

import de.craftorio.Craftorio;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel
public final class QuestGameTests {
    private QuestGameTests() {
    }

    @GameTest(template = "empty")
    public static void questRewardIsPaidOnceAfterTheGoal(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "QuestTest");
        int index = Quests.ALL.indexOf(Quests.byId("sell_tin_ingot").orElseThrow());

        helper.assertFalse(QuestActions.claim(player, index), "not reached yet");
        registry.recordSale(team.id(), "craftorio:tin_ingot", 64, 1_500);
        long before = team.balance();
        helper.assertTrue(QuestActions.claim(player, index), "reward collected");
        helper.assertValueEqual(team.balance(), before + Quests.ALL.get(index).reward(), "reward paid");
        helper.assertFalse(QuestActions.claim(player, index), "only once");
        helper.succeed();
    }
}
