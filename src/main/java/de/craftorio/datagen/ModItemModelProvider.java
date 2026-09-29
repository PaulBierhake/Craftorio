package de.craftorio.datagen;

import net.minecraft.world.item.Items;
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
        basicItem(ModItems.COPPER_CABLE.get());
        basicItem(ModItems.IRON_GEAR.get());
        basicItem(ModItems.CIRCUIT.get());
        basicItem(ModItems.MOTOR.get());
        basicItem(ModItems.STEEL_PLATE.get());
        basicItem(ModItems.PLASTIC_BAR.get());
        basicItem(ModItems.IRON_STICK.get());
        basicItem(ModItems.STONE_BRICK.get());
        basicItem(ModItems.BOLT.get());
        basicItem(ModItems.MAGAZINE.get());
        basicItem(ModItems.AP_MAGAZINE.get());
        basicItem(ModItems.GRENADE.get());
        basicItem(ModItems.BRONZE_SEAL.get());
        basicItem(ModItems.SILVER_SEAL.get());
        basicItem(ModItems.GOLD_SEAL.get());
        basicItem(ModItems.PLATINUM_SEAL.get());
        withExistingParent("stone_wall", mcLoc("block/wall_inventory")).texture("wall", modLoc("block/stone_wall"));
        basicItem(ModItems.SULFUR.get());
        basicItem(ModItems.BATTERY.get());
        basicItem(ModItems.RED_SCIENCE.get());
        basicItem(ModItems.GREEN_SCIENCE.get());
        basicItem(ModItems.MILITARY_SCIENCE.get());
        basicItem(ModItems.BLUE_SCIENCE.get());
        basicItem(ModItems.ADVANCED_CIRCUIT.get());
        basicItem(ModItems.RAW_URANIUM.get());
        basicItem(ModItems.URANIUM_PELLET.get());
        basicItem(ModItems.FUEL_ROD.get());
        handheldItem(ModItems.PATH_WAND.get());
        handheldItem(ModItems.STARTER_PICKAXE.get());
        basicItem(ModItems.GUIDE_BOOK.get());
    }
}
