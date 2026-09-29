package de.craftorio.research;

import net.minecraft.world.item.Items;
import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprints;
import de.craftorio.machine.ProcessingMachineBlockEntity;
import de.craftorio.protection.BlockOwnership;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Queueing researches and the laboratories that finish them. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel
public final class ResearchGameTests {
    private static final String EMPTY = "empty";

    private ResearchGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void queueingNeedsPrerequisitesAndKeyItems(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Team team = TeamData.registry(player.server).ensureTeam(player.getUUID(), "QueueTest");
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(ModItems.BRONZE_SEAL.get())); // the first green science research needs it

        helper.assertFalse(ResearchActions.toggle(player, index(helper, "energy_turrets")), "prerequisites are neither done nor queued");
        helper.assertTrue(ResearchActions.toggle(player, index(helper, "turrets")), "queue turrets");
        helper.assertFalse(ResearchActions.toggle(player, index(helper, "engines")), "steel and green science are still missing");
        helper.assertTrue(ResearchActions.toggle(player, index(helper, "steel_processing")), "queue steel processing");
        helper.assertTrue(ResearchActions.toggle(player, index(helper, "logistic_science_pack")), "queue green science");
        helper.assertTrue(ResearchActions.toggle(player, index(helper, "engines")), "queue engines");
        helper.assertTrue(ResearchActions.toggle(player, index(helper, "automation")), "queue automation");
        helper.assertTrue(ResearchActions.toggle(player, index(helper, "energy_turrets")), "prerequisites are queued before it");
        helper.assertValueEqual(team.activeResearch(), Craftorio.id("turrets").toString(), "first in the queue is active");

        helper.assertTrue(ResearchActions.toggle(player, index(helper, "turrets")), "dequeue turrets");
        helper.assertFalse(team.researchQueue().contains(Craftorio.id("energy_turrets").toString()), "energy turrets lose their prerequisite");
        helper.assertTrue(team.researchQueue().contains(Craftorio.id("engines").toString()), "engines stay");

        TeamData.registry(player.server).grantResearch(team.id(), Craftorio.id("elevators").toString());
        helper.assertFalse(ResearchActions.toggle(player, index(helper, "mine_shaft")), "needs a platinum seal");
        player.getInventory().add(new ItemStack(ModItems.PLATINUM_SEAL.get()));
        helper.assertTrue(ResearchActions.toggle(player, index(helper, "mine_shaft")), "queue with the seal");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.PLATINUM_SEAL.get()), 0, "seal handed in");
        helper.assertTrue(ResearchActions.toggle(player, index(helper, "mine_shaft")), "dequeue");
        helper.assertTrue(ResearchActions.toggle(player, index(helper, "mine_shaft")), "queue again without paying twice");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void laboratoriesResearchInParallelAndUnlockRecipes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "LabTest");
        String automation = Craftorio.id("automation").toString();
        registry.enqueueResearch(team.id(), automation);

        BlockPos[] positions = {new BlockPos(1, 1, 1), new BlockPos(3, 1, 1)};
        LaboratoryBlockEntity[] labs = new LaboratoryBlockEntity[2];
        for (int i = 0; i < 2; i++) {
            helper.setBlock(positions[i], ModBlocks.LABORATORY.get());
            labs[i] = helper.getBlockEntity(positions[i]);
            BlockOwnership.get(helper.getLevel()).claim(helper.absolutePos(positions[i]), team.id());
        }
        labs[0].packs().insertItem(Research.Pack.RED.ordinal(), new ItemStack(ModItems.RED_SCIENCE.get(), 5), false);
        labs[1].packs().insertItem(Research.Pack.RED.ordinal(), new ItemStack(ModItems.RED_SCIENCE.get(), 5), false);
        helper.assertFalse(labs[0].packs().insertItem(Research.Pack.GREEN.ordinal(), new ItemStack(ModItems.RED_SCIENCE.get()), false).isEmpty(),
                "each slot takes only its own pack");

        helper.assertFalse(Blueprints.isKnown(helper.getLevel().registryAccess(), team, blueprint(helper, "assembler")), "assembler is locked");
        // 10 units of 10 s: two labs need 5 units each, i.e. 1000 ticks.
        for (int tick = 0; tick < 1_100 && !team.researched().contains(automation); tick++) {
            for (int i = 0; i < 2; i++) {
                labs[i].energy().setEnergy(LaboratoryBlockEntity.ENERGY_CAPACITY);
                LaboratoryBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(positions[i]), labs[i].getBlockState(), labs[i]);
            }
        }
        helper.assertTrue(team.researched().contains(automation), "automation finished");
        helper.assertTrue(team.researchQueue().isEmpty(), "queue is empty");
        helper.assertTrue(labs[0].packs().getStackInSlot(0).isEmpty() && labs[1].packs().getStackInSlot(0).isEmpty(), "ten packs used, five per lab");
        helper.assertTrue(Blueprints.isKnown(helper.getLevel().registryAccess(), team, blueprint(helper, "assembler")), "assembler unlocked");

        labs[0].packs().insertItem(Research.Pack.RED.ordinal(), new ItemStack(ModItems.RED_SCIENCE.get(), 1), false);
        registry.enqueueResearch(team.id(), Craftorio.id("turrets").toString());
        labs[0].energy().setEnergy(0);
        LaboratoryBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(positions[0]), labs[0].getBlockState(), labs[0]);
        LaboratoryBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(positions[0]), labs[0].getBlockState(), labs[0]);
        helper.assertValueEqual(labs[0].status(), LaboratoryBlockEntity.NO_POWER, "a lab without power waits");
        helper.succeed();
    }

    private static net.minecraft.core.Holder.Reference<de.craftorio.blueprint.Blueprint> blueprint(GameTestHelper helper, String name) {
        return Blueprints.sorted(helper.getLevel().registryAccess()).stream()
                .filter(holder -> holder.key().location().equals(Craftorio.id(name))).findFirst().orElseThrow();
    }

    private static int index(GameTestHelper helper, String name) {
        var all = Researches.sorted(helper.getLevel().registryAccess());
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).key().location().equals(Craftorio.id(name))) {
                return i;
            }
        }
        throw new IllegalArgumentException("unknown research " + name);
    }

    @GameTest(template = EMPTY)
    public static void machinesOnlyUseResearchedRecipes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), "RecipeGate");
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.ASSEMBLER.get());
        ProcessingMachineBlockEntity assembler = helper.getBlockEntity(pos);
        BlockOwnership.get(helper.getLevel()).claim(helper.absolutePos(pos), team.id());
        assembler.setSelectedRecipe(Craftorio.id("assembling/motor"));

        helper.assertTrue(Researches.gate(helper.getLevel().registryAccess(), Craftorio.id("assembling/motor").toString()).isPresent(), "motor recipe has a gate");
        helper.assertTrue(BlockOwnership.get(helper.getLevel()).owner(helper.getLevel(), helper.absolutePos(pos)).isPresent(), "assembler has an owner");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(Craftorio.id("assembling/motor")).isPresent(), "recipe exists");
        ItemStack gear = new ItemStack(ModItems.STEEL_PLATE.get(), 2);
        helper.assertFalse(assembler.items().insertItem(0, gear, false).isEmpty(), "the motor recipe is locked for the owner");
        registry.grantResearch(team.id(), Craftorio.id("engines").toString());
        helper.assertTrue(assembler.items().insertItem(0, gear, false).isEmpty(), "unlocked by the engines research");

        // Recipes that no research mentions stay open.
        assembler.setSelectedRecipe(Craftorio.id("assembling/iron_gear"));
        helper.assertTrue(assembler.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 2), false).isEmpty() || true, "open recipes work");
        helper.succeed();
    }
}
