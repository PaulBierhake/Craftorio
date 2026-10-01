package de.craftorio.research;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The data of the research tree hangs together. */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchTreeGameTests {
    private ResearchTreeGameTests() {
    }

    @GameTest(template = "empty")
    public static void researchTreeIsConsistent(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var researches = access.registryOrThrow(ModRegistries.RESEARCH);
        var blueprints = access.registryOrThrow(ModRegistries.BLUEPRINTS);
        List<String> problems = new ArrayList<>();
        Set<ResourceLocation> unlocked = new HashSet<>();
        for (Holder.Reference<Research> holder : Researches.sorted(access)) {
            String id = Researches.id(holder);
            Research research = holder.value();
            for (ResourceLocation required : research.requires()) {
                if (!researches.containsKey(required)) {
                    problems.add(id + " requires unknown " + required);
                }
            }
            for (ResourceLocation target : research.unlocks()) {
                if (!blueprints.containsKey(target) && helper.getLevel().getRecipeManager().byKey(target).isEmpty()
                        && de.craftorio.fluid.FluidRecipes.byId(target).isEmpty()) {
                    problems.add(id + " unlocks unknown blueprint or recipe " + target);
                }
                if (!unlocked.add(target)) {
                    problems.add(target + " is unlocked by more than one research");
                }
            }
            // the last mining productivity level only raises the ore yield of the drills, the last tower technology only opens tier 5, arena knowledge gives bonuses
            if (research.unlocks().isEmpty() && !id.endsWith("mining_productivity_3") && !id.endsWith("tower_tech_3") && !id.contains(":arena_") && researches.holders().noneMatch(other -> other.value().requires().contains(holder.key().location()))) {
                problems.add(id + " unlocks nothing and nothing requires it");
            }
        }
        // Order the tree by prerequisites: a research may only need researches that are finished before it.
        Set<String> done = new HashSet<>();
        List<Holder.Reference<Research>> open = new ArrayList<>(Researches.sorted(access));
        while (!open.isEmpty()) {
            var next = open.stream().filter(holder -> done.containsAll(Researches.requires(holder.value()))).findFirst();
            if (next.isEmpty()) {
                problems.add("cycle or missing prerequisite among " + open.stream().map(Researches::id).toList());
                break;
            }
            done.add(Researches.id(next.get()));
            open.remove(next.get());
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void costLineListsUnitsPacksAndTime(GameTestHelper helper) {
        Research research = new Research(100, List.of(Research.Pack.RED, Research.Pack.GREEN), 30, List.of(), List.of(), List.of(), 0);
        helper.assertValueEqual(research.costLine(1.0), "100 × R+G, 30 s", "cost line");
        helper.assertValueEqual(research.costLine(0.5), "50 × R+G, 30 s", "cost line with the pacing factor");
        helper.assertValueEqual(new Research(1, List.of(Research.Pack.RED), 1, List.of(), List.of(), List.of(), 0).units(0.01), 1L, "at least one unit");
        helper.succeed();
    }
}
