package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModItems;
import de.craftorio.registry.ModRegistries;
import de.craftorio.research.Research;
import de.craftorio.research.Research.Pack;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * The research tree. Every blueprint that no research unlocks is available from the start.
 *
 * <p>Unlocked ids are blueprints (workbench recipes) or machine recipes such as {@code assembling/motor}.
 * This is the tree for the content that exists today; it grows with each package of the Factorio rework
 * (docs/FACTORIO-UMBAU.md §5), and the science pack costs follow that table.
 */
public final class ModResearch {
    private static int order;

    private ModResearch() {
    }

    public static void bootstrap(BootstrapContext<Research> context) {
        order = 0;
        add(context, "automation", 10, 10, List.of(), List.of("press", "electric_furnace"));
        add(context, "turrets", 10, 10, List.of(), List.of("crossbow_tower", "bolt", "arena_feeder"));
        add(context, "gun_turrets", 20, 15, List.of("turrets", "automation"), List.of("gun_turret", "cartridge"));
        add(context, "workbench_2", 30, 15, List.of("automation"), List.of("workbench_upgrade_2"), key(ModItems.DRILL_CORE.get()));
        add(context, "assembling", 50, 15, List.of("workbench_2"), List.of("assembler"));
        add(context, "energy_turrets", 100, 30, List.of("gun_turrets", "assembling"), List.of("tesla_tower"));
        add(context, "engines", 30, 15, List.of("assembling"), List.of("assembling/motor"));
        add(context, "caves", 100, 30, List.of("assembling", "engines"), List.of("cave_entrance"));
        add(context, "elevators", 50, 15, List.of("caves"), List.of("elevator"));
        add(context, "electric_mining", 75, 30, List.of("assembling"), List.of("electric_drill"), key(ModItems.RESONANCE_CRYSTAL.get()));
        add(context, "fast_belts", 75, 30, List.of("assembling"), List.of("fast_belt"));
        add(context, "workbench_3", 200, 30, List.of("assembling"), List.of("workbench_upgrade_3"), key(ModItems.DEEP_CORE.get()));
        add(context, "mine_shaft", 300, 30, List.of("elevators", "workbench_3"), List.of("mine_shaft"));
        add(context, "deep_mining", 200, 30, List.of("electric_mining", "mine_shaft"), List.of("deep_drill"));
        add(context, "express_belts", 150, 30, List.of("fast_belts", "mine_shaft"), List.of("express_belt"));
        add(context, "nuclear_power", 300, 30, List.of("mine_shaft"), List.of("reactor"));
        add(context, "laser_turrets", 300, 30, List.of("energy_turrets", "mine_shaft"), List.of("laser_tower"));
    }

    private static SizedIngredient key(ItemLike item) {
        return SizedIngredient.of(item, 1);
    }

    private static void add(BootstrapContext<Research> context, String name, long units, int seconds, List<String> requires,
                            List<String> unlocks, SizedIngredient... unlockItems) {
        context.register(ResourceKey.create(ModRegistries.RESEARCH, id(name)),
                new Research(units, List.of(Pack.RED), seconds, requires.stream().map(ModResearch::id).toList(),
                        unlocks.stream().map(ModResearch::id).toList(), List.of(unlockItems), order++));
    }

    private static ResourceLocation id(String name) {
        return Craftorio.id(name);
    }
}
