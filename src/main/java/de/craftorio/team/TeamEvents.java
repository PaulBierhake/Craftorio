package de.craftorio.team;

import de.craftorio.Craftorio;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class TeamEvents {
    private TeamEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Team team = TeamData.registry(player.server).ensureTeam(player.getUUID(), player.getGameProfile().getName());
            TeamData.sync(player, team);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TeamData.get(event.getServer()).flushSync(event.getServer());
    }
}
