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

        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.TRADING_POST.get(), ModBlocks.POWER_POLE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModBlocks.IRON_ORE_FIELD.get(), ModBlocks.COPPER_ORE_FIELD.get(), ModBlocks.COAL_FIELD.get(),
                ModBlocks.BURNER_DRILL.get(), ModBlocks.CONVEYOR_BELT.get(), ModBlocks.INSERTER.get(),
                ModBlocks.COAL_GENERATOR.get(), ModBlocks.ELECTRIC_FURNACE.get(), ModBlocks.PRESS.get(), ModBlocks.ASSEMBLER.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.IRON_ORE_FIELD.get(), ModBlocks.COPPER_ORE_FIELD.get());
    }
}
