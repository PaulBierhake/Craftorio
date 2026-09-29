package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.blueprint.Blueprint;
import de.craftorio.research.Research;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class ModRegistries {
    /** Datapack registry: data/&lt;namespace&gt;/craftorio/blueprint/*.json */
    public static final ResourceKey<Registry<Blueprint>> BLUEPRINTS = ResourceKey.createRegistryKey(Craftorio.id("blueprint"));

    /** Datapack registry: data/&lt;namespace&gt;/craftorio/research/*.json */
    public static final ResourceKey<Registry<Research>> RESEARCH = ResourceKey.createRegistryKey(Craftorio.id("research"));

    private ModRegistries() {
    }

    @SubscribeEvent
    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(BLUEPRINTS, Blueprint.CODEC, Blueprint.CODEC);
        event.dataPackRegistry(RESEARCH, Research.CODEC, Research.CODEC);
    }
}
