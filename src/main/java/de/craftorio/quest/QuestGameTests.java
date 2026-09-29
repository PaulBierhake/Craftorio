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
        int index = Quests.ALL.indexOf(Quests.byId("sell_plastic_bar").orElseThrow());

        helper.assertFalse(QuestActions.claim(player, index), "not reached yet");
        registry.recordSale(team.id(), "craftorio:plastic_bar", 64, 1_500);
        long before = team.balance();
        helper.assertTrue(QuestActions.claim(player, index), "reward collected");
        helper.assertValueEqual(team.balance(), before + Quests.ALL.get(index).reward(), "reward paid");
        helper.assertFalse(QuestActions.claim(player, index), "only once");
        helper.succeed();
    }

    /** Every step of the guide names something that exists, can be sold, or is unlocked by a research – and has a text in both languages. */
    @GameTest(template = "empty")
    public static void everyGuideStepPointsAtSomethingRealAndHasTexts(GameTestHelper helper) throws java.io.IOException {
        var access = helper.getLevel().registryAccess();
        var blueprints = access.registryOrThrow(de.craftorio.registry.ModRegistries.BLUEPRINTS);
        var researches = access.registryOrThrow(de.craftorio.registry.ModRegistries.RESEARCH);
        java.util.Set<String> unlockable = new java.util.HashSet<>();
        researches.holders().forEach(holder -> holder.value().unlocks().forEach(id -> unlockable.add(id.toString())));
        java.util.List<String> problems = new java.util.ArrayList<>();
        for (Quest quest : Quests.ALL) {
            switch (quest.kind()) {
                case UNLOCK -> {
                    var id = net.minecraft.resources.ResourceLocation.parse(quest.target());
                    if (!blueprints.containsKey(id) && !unlockable.contains(quest.target())) {
                        problems.add(quest.id() + ": nothing unlocks " + quest.target());
                    } else if (!unlockable.contains(quest.target())) {
                        // a blueprint without a research (the early ones) is bought in the terminal: fine
                    }
                }
                case BUILD -> {
                    if (!blueprints.containsKey(net.minecraft.resources.ResourceLocation.parse(quest.target()))) {
                        problems.add(quest.id() + ": no blueprint " + quest.target());
                    }
                }
                case SELL -> {
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(quest.target()));
                    if (de.craftorio.economy.Economy.unitPrice(new net.minecraft.world.item.ItemStack(item)) <= 0) {
                        problems.add(quest.id() + ": " + quest.target() + " can not be sold");
                    }
                }
                default -> {
                }
            }
            if (quest.hasRewardItem() && net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(quest.rewardItem())) == net.minecraft.world.item.Items.AIR) {
                problems.add(quest.id() + ": unknown reward item " + quest.rewardItem());
            }
        }
        for (String language : java.util.List.of("en_us", "de_de")) {
            try (var stream = QuestGameTests.class.getResourceAsStream("/assets/craftorio/lang/" + language + ".json")) {
                var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for (Quest quest : Quests.ALL) {
                    if (!json.has("craftorio.quest." + quest.id()) || !json.has("craftorio.quest." + quest.id() + ".hint")) {
                        problems.add(quest.id() + " has no text in " + language);
                    }
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }
}
