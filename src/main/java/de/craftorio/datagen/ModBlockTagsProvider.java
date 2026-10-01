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
                ModBlocks.CROSSBOW_TOWER.get(), ModBlocks.SUPPLY_DEPOT.get(), ModBlocks.CAVE_ENTRANCE.get());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.PATH_BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModBlocks.IRON_ORE_FIELD.get(), ModBlocks.COPPER_ORE_FIELD.get(), ModBlocks.COAL_FIELD.get(),
                ModBlocks.BURNER_DRILL.get(), ModBlocks.CONVEYOR_BELT.get(), ModBlocks.INSERTER.get(),
                ModBlocks.STONE_FURNACE.get(), ModBlocks.LONG_INSERTER.get(), ModBlocks.FAST_INSERTER.get(), ModBlocks.FILTER_INSERTER.get(),
                ModBlocks.UNDERGROUND_BELT.get(), ModBlocks.FAST_UNDERGROUND_BELT.get(), ModBlocks.SPLITTER.get(), ModBlocks.FAST_SPLITTER.get(), ModBlocks.ELECTRIC_FURNACE.get(), ModBlocks.ASSEMBLER.get(), ModBlocks.LABORATORY.get(),
                ModBlocks.BOILER.get(), ModBlocks.STEAM_ENGINE.get(), ModBlocks.OFFSHORE_PUMP.get(), ModBlocks.STONE_FIELD.get(), ModBlocks.WOOD_FIELD.get(),
                ModBlocks.TERMINAL.get(),
                ModBlocks.ENEMY_PORTAL.get(), ModBlocks.GUN_TURRET.get(), ModBlocks.TESLA_TOWER.get(), ModBlocks.ELEVATOR.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.MEDIUM_POWER_POLE.get(), ModBlocks.SOLAR_PANEL.get(),
                ModBlocks.STEEL_FURNACE.get(), ModBlocks.ASSEMBLER_2.get(), ModBlocks.ASSEMBLER_3.get(), ModBlocks.BEACON.get(), ModBlocks.LAMP.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.PIPE.get(), ModBlocks.UNDERGROUND_PIPE.get(),
                ModBlocks.STORAGE_TANK.get(), ModBlocks.FLUID_PUMP.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.CONCRETE.get(), ModBlocks.GREENHOUSE.get(), ModBlocks.CENTRIFUGE.get(), ModBlocks.PUMPJACK.get(), ModBlocks.CHEMICAL_PLANT.get(), ModBlocks.OIL_REFINERY.get(),
                ModBlocks.ACCUMULATOR.get());
        tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.OIL_WELL.get());
        tag(BlockTags.WALLS).add(ModBlocks.STONE_WALL.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.STONE_WALL.get(), ModBlocks.FLAMETHROWER_TURRET.get(), ModBlocks.MORTAR_TURRET.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.IRON_ORE_FIELD.get(), ModBlocks.COPPER_ORE_FIELD.get());
        tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.CAVE_RUBBLE.get());
        tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.CAVE_RUBBLE.get());
        tag(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.CAVE_RUBBLE.get());
        tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.MINE_RUBBLE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.ARENA_GATE.get(), ModBlocks.ARENA_FEEDER.get());
        tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.MINE_RUBBLE.get());
        tag(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.MINE_RUBBLE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.URANIUM_ORE_FIELD.get(), ModBlocks.MINE_SHAFT.get(),
                ModBlocks.ELECTRIC_DRILL.get(), ModBlocks.DEEP_DRILL.get(), ModBlocks.FAST_BELT.get(), ModBlocks.EXPRESS_BELT.get(),
                ModBlocks.REACTOR.get(), ModBlocks.HEAT_PIPE.get(), ModBlocks.HEAT_EXCHANGER.get(), ModBlocks.STEAM_TURBINE.get(), ModBlocks.LASER_TOWER.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(ModBlocks.URANIUM_ORE_FIELD.get());
    }
}
