package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.registry.ModItems;
import de.craftorio.registry.ModRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * The workbench recipes. Which of them a team may use is decided by the researches in {@link ModResearch}: a blueprint
 * that no research unlocks is available from the start.
 */
public final class ModBlueprints {
    private static int order;

    private ModBlueprints() {
    }

    public static void bootstrap(BootstrapContext<Blueprint> context) {
        order = 0;
        // Tier 1 – available from the start
        add(context, "laboratory", stack(ModItems.LABORATORY.get(), 1), 1,
                SizedIngredient.of(Items.IRON_INGOT, 10), SizedIngredient.of(Items.COPPER_INGOT, 10));
        add(context, "red_science", stack(ModItems.RED_SCIENCE.get(), 1), 1,
                SizedIngredient.of(Items.COPPER_INGOT, 1), SizedIngredient.of(Items.IRON_INGOT, 2));
        add(context, "trading_post", stack(ModItems.TRADING_POST.get(), 1), 1,
                SizedIngredient.of(ItemTags.PLANKS, 8), SizedIngredient.of(Items.CHEST, 1),
                SizedIngredient.of(Items.IRON_INGOT, 2), SizedIngredient.of(Items.GOLD_INGOT, 1));
        add(context, "burner_drill", stack(ModItems.BURNER_DRILL.get(), 1), 1,
                SizedIngredient.of(Items.IRON_INGOT, 8), SizedIngredient.of(Items.FURNACE, 1),
                SizedIngredient.of(Items.COBBLESTONE, 8));
        add(context, "conveyor_belt", stack(ModItems.CONVEYOR_BELT.get(), 4), 1,
                SizedIngredient.of(Items.IRON_INGOT, 3), SizedIngredient.of(ItemTags.PLANKS, 2));
        add(context, "inserter", stack(ModItems.INSERTER.get(), 1), 1,
                SizedIngredient.of(Items.IRON_INGOT, 2), SizedIngredient.of(Items.REDSTONE, 1),
                SizedIngredient.of(Items.COBBLESTONE, 2));

        // Tier 1 – power, automation and towers
        add(context, "coal_generator", stack(ModItems.COAL_GENERATOR.get(), 1), 1,
                SizedIngredient.of(Items.IRON_INGOT, 8), SizedIngredient.of(Items.COPPER_INGOT, 4),
                SizedIngredient.of(Items.FURNACE, 1), SizedIngredient.of(Items.REDSTONE, 2));
        add(context, "power_pole", stack(ModItems.POWER_POLE.get(), 2), 1,
                SizedIngredient.of(Items.COPPER_INGOT, 2), SizedIngredient.of(Items.STICK, 2));
        add(context, "electric_furnace", stack(ModItems.ELECTRIC_FURNACE.get(), 1), 1,
                SizedIngredient.of(Items.BRICK, 8), SizedIngredient.of(Items.COPPER_INGOT, 4),
                SizedIngredient.of(Items.FURNACE, 1), SizedIngredient.of(Items.REDSTONE, 2));
        add(context, "press", stack(ModItems.PRESS.get(), 1), 1,
                SizedIngredient.of(Items.IRON_INGOT, 10), SizedIngredient.of(Items.COPPER_INGOT, 4),
                SizedIngredient.of(Items.PISTON, 1));
        add(context, "workbench_upgrade_2", stack(ModItems.WORKBENCH_UPGRADE_2.get(), 1), 1,
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 20),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 20), SizedIngredient.of(Items.IRON_INGOT, 10));

        // Tower defense – the arena gate is free, towers and the feeder come from research
        add(context, "arena_gate", stack(ModItems.ARENA_GATE.get(), 1), 1,
                SizedIngredient.of(Items.IRON_INGOT, 8), SizedIngredient.of(Items.GOLD_INGOT, 2),
                SizedIngredient.of(Items.REDSTONE, 4));
        add(context, "crossbow_tower", stack(ModItems.CROSSBOW_TOWER.get(), 1), 1,
                SizedIngredient.of(ItemTags.PLANKS, 12), SizedIngredient.of(Items.IRON_INGOT, 6),
                SizedIngredient.of(Items.REDSTONE, 2));
        add(context, "bolt", stack(ModItems.BOLT.get(), 16), 1,
                SizedIngredient.of(Items.IRON_INGOT, 1), SizedIngredient.of(Items.STICK, 2));
        add(context, "arena_feeder", stack(ModItems.ARENA_FEEDER.get(), 1), 1,
                SizedIngredient.of(Items.IRON_INGOT, 8), SizedIngredient.of(Items.COPPER_INGOT, 8),
                SizedIngredient.of(Items.CHEST, 1), SizedIngredient.of(Items.REDSTONE, 4));
        add(context, "gun_turret", stack(ModItems.GUN_TURRET.get(), 1), 1,
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 12),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 6), SizedIngredient.of(Items.REDSTONE, 4));
        add(context, "cartridge", stack(ModItems.CARTRIDGE.get(), 16), 1,
                SizedIngredient.of(Items.COPPER_INGOT, 1), SizedIngredient.of(ModItems.IRON_PLATE.get(), 1),
                SizedIngredient.of(Items.COAL, 1));
        add(context, "tesla_tower", stack(ModItems.TESLA_TOWER.get(), 1), 1,
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 24),
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 8), SizedIngredient.of(Items.GOLD_INGOT, 4));

        // Tier 2
        add(context, "assembler", stack(ModItems.ASSEMBLER.get(), 1), 2,
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 12),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 8), SizedIngredient.of(Items.GOLD_INGOT, 2),
                SizedIngredient.of(Items.CRAFTING_TABLE, 1));
        add(context, "cave_entrance", stack(ModItems.CAVE_ENTRANCE.get(), 1), 2,
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 16), SizedIngredient.of(ModItems.IRON_GEAR.get(), 8),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 8));
        add(context, "elevator", stack(ModItems.ELEVATOR.get(), 2), 2,
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 8), SizedIngredient.of(ModItems.MOTOR.get(), 2),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 4));
        add(context, "electric_drill", stack(ModItems.ELECTRIC_DRILL.get(), 1), 2,
                SizedIngredient.of(ModItems.BURNER_DRILL.get(), 1), SizedIngredient.of(ModItems.MOTOR.get(), 2),
                SizedIngredient.of(ModItems.CIRCUIT.get(), 2), SizedIngredient.of(ModItems.IRON_PLATE.get(), 8));
        add(context, "fast_belt", stack(ModItems.FAST_BELT.get(), 4), 2,
                SizedIngredient.of(ModItems.CONVEYOR_BELT.get(), 4), SizedIngredient.of(ModItems.IRON_GEAR.get(), 4),
                SizedIngredient.of(ModItems.CIRCUIT.get(), 1));
        add(context, "workbench_upgrade_3", stack(ModItems.WORKBENCH_UPGRADE_3.get(), 1), 2,
                SizedIngredient.of(ModItems.MOTOR.get(), 10), SizedIngredient.of(ModItems.CIRCUIT.get(), 20),
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 40));

        // Tier 3 – mine layer
        add(context, "mine_shaft", stack(ModItems.MINE_SHAFT.get(), 1), 3,
                SizedIngredient.of(ModItems.LEAD_INGOT.get(), 16), SizedIngredient.of(ModItems.MOTOR.get(), 8),
                SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 4),
                SizedIngredient.of(ModItems.BATTERY.get(), 4));
        add(context, "deep_drill", stack(ModItems.DEEP_DRILL.get(), 1), 3,
                SizedIngredient.of(ModItems.ELECTRIC_DRILL.get(), 1),
                SizedIngredient.of(ModItems.TITANIUM_PLATE.get(), 12), SizedIngredient.of(ModItems.MOTOR.get(), 4),
                SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 2));
        add(context, "express_belt", stack(ModItems.EXPRESS_BELT.get(), 4), 3,
                SizedIngredient.of(ModItems.FAST_BELT.get(), 4),
                SizedIngredient.of(ModItems.TITANIUM_PLATE.get(), 2),
                SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 1));
        add(context, "reactor", stack(ModItems.REACTOR.get(), 1), 3,
                SizedIngredient.of(ModItems.TITANIUM_PLATE.get(), 20),
                SizedIngredient.of(ModItems.LEAD_INGOT.get(), 32),
                SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 8),
                SizedIngredient.of(ModItems.BATTERY.get(), 8));
        add(context, "laser_tower", stack(ModItems.LASER_TOWER.get(), 1), 3,
                SizedIngredient.of(ModItems.TITANIUM_PLATE.get(), 12),
                SizedIngredient.of(ModItems.ENERGY_CRYSTAL.get(), 2),
                SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 4),
                SizedIngredient.of(ModItems.BATTERY.get(), 4));
    }

    private static void add(BootstrapContext<Blueprint> context, String name, ItemStack result, int tier, SizedIngredient... ingredients) {
        context.register(ResourceKey.create(ModRegistries.BLUEPRINTS, id(name)),
                new Blueprint(result, List.of(ingredients), tier, order++));
    }

    private static ResourceLocation id(String name) {
        return Craftorio.id(name);
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }
}
