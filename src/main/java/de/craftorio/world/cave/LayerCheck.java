package de.craftorio.world.cave;

import de.craftorio.Craftorio;
import de.craftorio.world.terrain.FactoryChunkGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * The caves and mines are height bands made by the world generator. A superflat world has none (its surface is at
 * y = -60), so the server logs a warning and tells operators when they join.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class LayerCheck {
    private LayerCheck() {
    }

    /** True if the overworld cannot have cave and mine layers. */
    public static boolean noLayers(MinecraftServer server) {
        return server.overworld().getChunkSource().getGenerator() instanceof FlatLevelSource;
    }

    /** True if the overworld is not a Craftorio world type (flat building ground is recommended). */
    public static boolean notFactoryWorld(MinecraftServer server) {
        return !(server.overworld().getChunkSource().getGenerator() instanceof FactoryChunkGenerator);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (noLayers(event.getServer())) {
            Craftorio.LOGGER.warn("This is a superflat world: it has no Craftorio cave and mine layers, so cave entrances cannot be built. Use a normal world type.");
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasPermissions(2)) {
            if (noLayers(player.server)) {
                player.sendSystemMessage(Component.translatable("craftorio.cave.error.flat_world"));
            } else if (notFactoryWorld(player.server)) {
                player.sendSystemMessage(Component.translatable("craftorio.world.hint.not_factory"));
            }
        }
    }
}
