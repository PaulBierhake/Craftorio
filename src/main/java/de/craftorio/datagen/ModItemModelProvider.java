package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, Craftorio.MOD_ID, fileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.IRON_PLATE.get());
        basicItem(ModItems.COPPER_CABLE.get());
        basicItem(ModItems.IRON_GEAR.get());
        basicItem(ModItems.CIRCUIT.get());
        basicItem(ModItems.MOTOR.get());
        basicItem(ModItems.WORKBENCH_UPGRADE_2.get());
        basicItem(ModItems.WORKBENCH_UPGRADE_3.get());
        basicItem(ModItems.DRILL_CORE.get());
        basicItem(ModItems.RESONANCE_CRYSTAL.get());
        basicItem(ModItems.DEEP_CORE.get());
        basicItem(ModItems.STAR_SHARD.get());
        basicItem(ModItems.BOLT.get());
        basicItem(ModItems.CARTRIDGE.get());
        basicItem(ModItems.RAW_TIN.get());
        basicItem(ModItems.TIN_INGOT.get());
        basicItem(ModItems.RAW_LEAD.get());
        basicItem(ModItems.LEAD_INGOT.get());
        basicItem(ModItems.SULFUR.get());
        basicItem(ModItems.BATTERY.get());
        basicItem(ModItems.RED_SCIENCE.get());
        basicItem(ModItems.GREEN_SCIENCE.get());
        basicItem(ModItems.MILITARY_SCIENCE.get());
        basicItem(ModItems.BLUE_SCIENCE.get());
        basicItem(ModItems.ADVANCED_CIRCUIT.get());
        basicItem(ModItems.RAW_TITANIUM.get());
        basicItem(ModItems.TITANIUM_INGOT.get());
        basicItem(ModItems.TITANIUM_PLATE.get());
        basicItem(ModItems.RAW_URANIUM.get());
        basicItem(ModItems.URANIUM_PELLET.get());
        basicItem(ModItems.CRYSTAL_SHARD.get());
        basicItem(ModItems.ENERGY_CRYSTAL.get());
        basicItem(ModItems.FUEL_ROD.get());
        handheldItem(ModItems.PATH_WAND.get());
        handheldItem(ModItems.STARTER_PICKAXE.get());
        basicItem(ModItems.GUIDE_BOOK.get());
    }
}
