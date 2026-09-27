package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper fileHelper) {
        super(output, lookup, Craftorio.MOD_ID, fileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Cap rock must survive withers, the dragon and worldgen features, just like bedrock.
        tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.CAP_ROCK.get());
        tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.CAP_ROCK.get());
        tag(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.CAP_ROCK.get());

        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.TRADING_POST.get(), ModBlocks.POWER_POLE.get(), ModBlocks.WORKBENCH.get(),
                ModBlocks.CROSSBOW_TOWER.get(), ModBlocks.CAVE_ENTRANCE.get());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.PATH_BLOCK.get(), ModBlocks.TOWER_RUIN.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModBlocks.IRON_ORE_FIELD.get(), ModBlocks.COPPER_ORE_FIELD.get(), ModBlocks.COAL_FIELD.get(),
                ModBlocks.BURNER_DRILL.get(), ModBlocks.CONVEYOR_BELT.get(), ModBlocks.INSERTER.get(),
                ModBlocks.COAL_GENERATOR.get(), ModBlocks.ELECTRIC_FURNACE.get(), ModBlocks.PRESS.get(), ModBlocks.ASSEMBLER.get(),
                ModBlocks.TERMINAL.get(), ModBlocks.ASSEMBLY_WORKBENCH.get(), ModBlocks.PRECISION_WORKBENCH.get(),
                ModBlocks.ENEMY_PORTAL.get(), ModBlocks.GUN_TURRET.get(), ModBlocks.TESLA_TOWER.get(), ModBlocks.ELEVATOR.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.IRON_ORE_FIELD.get(), ModBlocks.COPPER_ORE_FIELD.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.TIN_ORE_FIELD.get(), ModBlocks.LEAD_ORE_FIELD.get(), ModBlocks.GOLD_ORE_FIELD.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.TIN_ORE_FIELD.get(), ModBlocks.LEAD_ORE_FIELD.get(), ModBlocks.SULFUR_FIELD.get(),
                ModBlocks.GOLD_ORE_FIELD.get(), ModBlocks.QUARTZ_FIELD.get());
        tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.CAVE_RUBBLE.get());
        tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.CAVE_RUBBLE.get());
        tag(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.CAVE_RUBBLE.get());
        tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.MINE_RUBBLE.get());
        tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.MINE_RUBBLE.get());
        tag(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.MINE_RUBBLE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.DIAMOND_FIELD.get(), ModBlocks.TITANIUM_ORE_FIELD.get(),
                ModBlocks.URANIUM_ORE_FIELD.get(), ModBlocks.CRYSTAL_FIELD.get(), ModBlocks.MINE_SHAFT.get(),
                ModBlocks.ELECTRIC_DRILL.get(), ModBlocks.DEEP_DRILL.get(), ModBlocks.FAST_BELT.get(), ModBlocks.EXPRESS_BELT.get(),
                ModBlocks.REACTOR.get(), ModBlocks.LASER_TOWER.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(ModBlocks.DIAMOND_FIELD.get(), ModBlocks.TITANIUM_ORE_FIELD.get(),
                ModBlocks.URANIUM_ORE_FIELD.get(), ModBlocks.CRYSTAL_FIELD.get());
    }
}
