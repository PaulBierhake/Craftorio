package de.craftorio;

import com.mojang.logging.LogUtils;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModCreativeTabs;
import de.craftorio.registry.ModDataComponents;
import de.craftorio.registry.ModEntities;
import de.craftorio.registry.ModFeatures;
import de.craftorio.registry.ModFluids;
import de.craftorio.registry.ModMenus;
import de.craftorio.registry.ModRecipes;
import de.craftorio.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Craftorio.MOD_ID)
public final class Craftorio {
    public static final String MOD_ID = "craftorio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Craftorio(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, CraftorioConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
