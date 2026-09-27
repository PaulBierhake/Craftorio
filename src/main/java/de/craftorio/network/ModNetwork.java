package de.craftorio.network;

import de.craftorio.Craftorio;
import de.craftorio.client.ClientTeamState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        // ClientTeamState holds plain fields only, so referencing it is safe on a dedicated server.
        registrar.playToClient(TeamSyncPayload.TYPE, TeamSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientTeamState.update(payload.teamName(), payload.balance(), payload.unlocked()));
    }
}
