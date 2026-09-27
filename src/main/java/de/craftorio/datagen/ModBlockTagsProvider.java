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
    }
}
