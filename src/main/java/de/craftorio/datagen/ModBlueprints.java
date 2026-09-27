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
 * The blueprint tree. Tier 1 needs only smelted and pressed materials; the tier 2 workbench needs a drill core
 * from tower defense level 10, tier 3 a deep core from level 30.
 */
public final class ModBlueprints {
    private static int order;

    private ModBlueprints() {
    }

    public static void bootstrap(BootstrapContext<Blueprint> context) {
        order = 0;
        // Tier 1 – free starter kit
        add(context, "trading_post", stack(ModItems.TRADING_POST.get(), 1), 1, 0, List.of(), List.of(),
                SizedIngredient.of(ItemTags.PLANKS, 8), SizedIngredient.of(Items.CHEST, 1),
                SizedIngredient.of(Items.IRON_INGOT, 2), SizedIngredient.of(Items.GOLD_INGOT, 1));
        add(context, "burner_drill", stack(ModItems.BURNER_DRILL.get(), 1), 1, 0, List.of(), List.of(),
                SizedIngredient.of(Items.IRON_INGOT, 8), SizedIngredient.of(Items.FURNACE, 1), SizedIngredient.of(Items.COBBLESTONE, 8));
        add(context, "conveyor_belt", stack(ModItems.CONVEYOR_BELT.get(), 4), 1, 0, List.of(), List.of(),
                SizedIngredient.of(Items.IRON_INGOT, 3), SizedIngredient.of(ItemTags.PLANKS, 2));
        add(context, "inserter", stack(ModItems.INSERTER.get(), 1), 1, 0, List.of(), List.of(),
                SizedIngredient.of(Items.IRON_INGOT, 2), SizedIngredient.of(Items.REDSTONE, 1), SizedIngredient.of(Items.COBBLESTONE, 2));

        // Tier 1 – bought with credits
        add(context, "coal_generator", stack(ModItems.COAL_GENERATOR.get(), 1), 1, 250, List.of(), List.of(),
                SizedIngredient.of(Items.IRON_INGOT, 8), SizedIngredient.of(Items.COPPER_INGOT, 4),
                SizedIngredient.of(Items.FURNACE, 1), SizedIngredient.of(Items.REDSTONE, 2));
        add(context, "power_pole", stack(ModItems.POWER_POLE.get(), 2), 1, 100, List.of(), List.of(id("coal_generator")),
                SizedIngredient.of(Items.COPPER_INGOT, 2), SizedIngredient.of(Items.STICK, 2));
        add(context, "electric_furnace", stack(ModItems.ELECTRIC_FURNACE.get(), 1), 1, 300, List.of(), List.of(id("coal_generator")),
                SizedIngredient.of(Items.BRICK, 8), SizedIngredient.of(Items.COPPER_INGOT, 4),
                SizedIngredient.of(Items.FURNACE, 1), SizedIngredient.of(Items.REDSTONE, 2));
        add(context, "press", stack(ModItems.PRESS.get(), 1), 1, 400, List.of(), List.of(id("coal_generator")),
                SizedIngredient.of(Items.IRON_INGOT, 10), SizedIngredient.of(Items.COPPER_INGOT, 4), SizedIngredient.of(Items.PISTON, 1));
        add(context, "workbench_upgrade_2", stack(ModItems.WORKBENCH_UPGRADE_2.get(), 1), 1, 2_000,
                List.of(SizedIngredient.of(ModItems.DRILL_CORE.get(), 1)), List.of(id("press")),
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 20), SizedIngredient.of(ModItems.COPPER_CABLE.get(), 20),
                SizedIngredient.of(Items.IRON_INGOT, 10));

        // Tower defense – zone blocks are free, towers cost credits
        add(context, "zone_core", stack(ModItems.ZONE_CORE.get(), 1), 1, 0, List.of(), List.of(),
                SizedIngredient.of(Items.IRON_INGOT, 8), SizedIngredient.of(Items.GOLD_INGOT, 2), SizedIngredient.of(Items.REDSTONE, 4));
        add(context, "enemy_portal", stack(ModItems.ENEMY_PORTAL.get(), 1), 1, 0, List.of(), List.of(),
                SizedIngredient.of(Items.COBBLESTONE, 8), SizedIngredient.of(Items.IRON_INGOT, 2), SizedIngredient.of(Items.REDSTONE, 2));
        add(context, "path_block", stack(ModItems.PATH_BLOCK.get(), 16), 1, 0, List.of(), List.of(),
                SizedIngredient.of(Items.COBBLESTONE, 8));
        add(context, "crossbow_tower", stack(ModItems.CROSSBOW_TOWER.get(), 1), 1, 300, List.of(), List.of(),
                SizedIngredient.of(ItemTags.PLANKS, 12), SizedIngredient.of(Items.IRON_INGOT, 6), SizedIngredient.of(Items.REDSTONE, 2));
        add(context, "bolt", stack(ModItems.BOLT.get(), 16), 1, 50, List.of(), List.of(id("crossbow_tower")),
                SizedIngredient.of(Items.IRON_INGOT, 1), SizedIngredient.of(Items.STICK, 2));
        add(context, "gun_turret", stack(ModItems.GUN_TURRET.get(), 1), 1, 1_500, List.of(), List.of(id("crossbow_tower"), id("press")),
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 12), SizedIngredient.of(ModItems.COPPER_CABLE.get(), 6),
                SizedIngredient.of(Items.REDSTONE, 4));
        add(context, "cartridge", stack(ModItems.CARTRIDGE.get(), 16), 1, 150, List.of(), List.of(id("gun_turret")),
                SizedIngredient.of(Items.COPPER_INGOT, 1), SizedIngredient.of(ModItems.IRON_PLATE.get(), 1), SizedIngredient.of(Items.COAL, 1));
        add(context, "tesla_tower", stack(ModItems.TESLA_TOWER.get(), 1), 1, 3_000, List.of(), List.of(id("gun_turret"), id("coal_generator")),
                SizedIngredient.of(ModItems.COPPER_CABLE.get(), 24), SizedIngredient.of(ModItems.IRON_PLATE.get(), 8),
                SizedIngredient.of(Items.GOLD_INGOT, 4));

        // Tier 2
        add(context, "assembler", stack(ModItems.ASSEMBLER.get(), 1), 2, 800, List.of(), List.of(id("workbench_upgrade_2")),
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 12), SizedIngredient.of(ModItems.COPPER_CABLE.get(), 8),
                SizedIngredient.of(Items.GOLD_INGOT, 2), SizedIngredient.of(Items.CRAFTING_TABLE, 1));
        add(context, "workbench_upgrade_3", stack(ModItems.WORKBENCH_UPGRADE_3.get(), 1), 2, 10_000,
                List.of(SizedIngredient.of(ModItems.DEEP_CORE.get(), 1)), List.of(id("assembler")),
                SizedIngredient.of(ModItems.MOTOR.get(), 10), SizedIngredient.of(ModItems.CIRCUIT.get(), 20),
                SizedIngredient.of(ModItems.IRON_PLATE.get(), 40));
    }

    private static void add(BootstrapContext<Blueprint> context, String name, ItemStack result, int tier, long cost,
                            List<SizedIngredient> unlockItems, List<ResourceLocation> requires, SizedIngredient... ingredients) {
        context.register(ResourceKey.create(ModRegistries.BLUEPRINTS, id(name)),
                new Blueprint(result, List.of(ingredients), tier, cost, unlockItems, requires, order++));
    }

    private static ResourceLocation id(String name) {
        return Craftorio.id(name);
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }
}
