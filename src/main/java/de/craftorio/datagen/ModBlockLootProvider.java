package de.craftorio.datagen;

import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModDataComponents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public final class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.TRADING_POST.get());
        dropSelf(ModBlocks.BURNER_DRILL.get());
        dropSelf(ModBlocks.CONVEYOR_BELT.get());
        dropSelf(ModBlocks.INSERTER.get());
        dropSelf(ModBlocks.LONG_INSERTER.get());
        dropSelf(ModBlocks.FAST_INSERTER.get());
        dropSelf(ModBlocks.FILTER_INSERTER.get());
        dropSelf(ModBlocks.UNDERGROUND_BELT.get());
        dropSelf(ModBlocks.FAST_UNDERGROUND_BELT.get());
        dropSelf(ModBlocks.SPLITTER.get());
        dropSelf(ModBlocks.FAST_SPLITTER.get());
        dropSelf(ModBlocks.BOILER.get());
        dropSelf(ModBlocks.STEAM_ENGINE.get());
        dropSelf(ModBlocks.OFFSHORE_PUMP.get());
        dropSelf(ModBlocks.POWER_POLE.get());
        dropSelf(ModBlocks.ELECTRIC_FURNACE.get());
        dropSelf(ModBlocks.STONE_FURNACE.get());
        dropSelf(ModBlocks.ASSEMBLER.get());
        dropSelf(ModBlocks.LABORATORY.get());
        dropSelf(ModBlocks.TERMINAL.get());
        dropSelf(ModBlocks.WORKBENCH.get());
        dropSelf(ModBlocks.ARENA_GATE.get());
        dropSelf(ModBlocks.ARENA_FEEDER.get());
        add(ModBlocks.PATH_BLOCK.get(), noDrop()); // laid with the path wand
        add(ModBlocks.ZONE_CORE.get(), noDrop()); // built by the arena
        add(ModBlocks.ENEMY_PORTAL.get(), noDrop());
        tower(ModBlocks.CROSSBOW_TOWER.get());
        tower(ModBlocks.GUN_TURRET.get());
        tower(ModBlocks.TESLA_TOWER.get());
        dropSelf(ModBlocks.CAVE_ENTRANCE.get());
        dropSelf(ModBlocks.ELEVATOR.get());
        dropSelf(ModBlocks.MINE_SHAFT.get());
        dropSelf(ModBlocks.ELECTRIC_DRILL.get());
        dropSelf(ModBlocks.DEEP_DRILL.get());
        dropSelf(ModBlocks.FAST_BELT.get());
        dropSelf(ModBlocks.EXPRESS_BELT.get());
        dropSelf(ModBlocks.REACTOR.get());
        tower(ModBlocks.LASER_TOWER.get());
    }

    /** Towers keep their upgrade level and health when picked up. */
    private void tower(Block block) {
        add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(ModDataComponents.TOWER_STATE.get()))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
