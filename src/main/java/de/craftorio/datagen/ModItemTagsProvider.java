package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.module.ProductivityRules;
import de.craftorio.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/** The item tag that says which products productivity modules may be used for: intermediate products only. */
public final class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                               CompletableFuture<net.minecraft.data.tags.TagsProvider.TagLookup<Block>> blockTags, ExistingFileHelper fileHelper) {
        super(output, lookup, blockTags, Craftorio.MOD_ID, fileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        Item[] intermediates = {
                Items.IRON_INGOT, Items.COPPER_INGOT, Items.GLASS, Items.BRICK,
                ModItems.STEEL_PLATE.get(), ModItems.STONE_BRICK.get(), ModItems.IRON_GEAR.get(), ModItems.COPPER_CABLE.get(),
                ModItems.IRON_STICK.get(), ModItems.PIPE.get(), ModItems.CIRCUIT.get(), ModItems.ADVANCED_CIRCUIT.get(),
                ModItems.PROCESSING_UNIT.get(), ModItems.MOTOR.get(), ModItems.ELECTRIC_ENGINE.get(), ModItems.FLYING_ROBOT_FRAME.get(),
                ModItems.LOW_DENSITY_STRUCTURE.get(), ModItems.PLASTIC_BAR.get(), ModItems.SULFUR.get(), ModItems.EXPLOSIVES.get(), ModItems.BATTERY.get(),
                ModItems.SOLID_FUEL.get(), ModItems.URANIUM_235.get(), ModItems.URANIUM_238.get(),
                ModItems.RED_SCIENCE.get(), ModItems.GREEN_SCIENCE.get(), ModItems.BLUE_SCIENCE.get(), ModItems.MILITARY_SCIENCE.get()};
        tag(ProductivityRules.ALLOWED).add(intermediates);
    }
}
