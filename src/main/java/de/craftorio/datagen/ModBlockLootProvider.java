package de.craftorio.datagen;

import de.craftorio.registry.ModBlocks;
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
        dropSelf(ModBlocks.COAL_GENERATOR.get());
        dropSelf(ModBlocks.POWER_POLE.get());
        dropSelf(ModBlocks.ELECTRIC_FURNACE.get());
        dropSelf(ModBlocks.PRESS.get());
        dropSelf(ModBlocks.ASSEMBLER.get());
        dropSelf(ModBlocks.TERMINAL.get());
        dropSelf(ModBlocks.WORKBENCH.get());
        dropSelf(ModBlocks.ASSEMBLY_WORKBENCH.get());
        dropSelf(ModBlocks.PRECISION_WORKBENCH.get());
        dropSelf(ModBlocks.ZONE_CORE.get());
        dropSelf(ModBlocks.ENEMY_PORTAL.get());
        dropSelf(ModBlocks.PATH_BLOCK.get());
        dropSelf(ModBlocks.CROSSBOW_TOWER.get());
        dropSelf(ModBlocks.GUN_TURRET.get());
        dropSelf(ModBlocks.TESLA_TOWER.get());
        dropSelf(ModBlocks.CAVE_ENTRANCE.get());
        dropSelf(ModBlocks.ELEVATOR.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
