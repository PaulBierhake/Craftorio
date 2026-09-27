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
    }
}
