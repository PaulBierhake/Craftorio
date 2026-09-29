package de.craftorio.world;

import de.craftorio.Craftorio;
import de.craftorio.CraftorioConfig;
import de.craftorio.defense.TdEnemy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** The factory world is a calm place: no hunger, no hostile mobs and no night (server config {@code world.*}). */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class WorldRules {
    private static final int DAY_CHECK_INTERVAL = 200;
    private static final long NOON = 6_000;

    private WorldRules() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && CraftorioConfig.NO_HUNGER.get() && player.getFoodData().getFoodLevel() < 20) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && CraftorioConfig.NO_HOSTILE_MOBS.get()
                && event.getEntity() instanceof Enemy && !(event.getEntity() instanceof TdEnemy)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        applyDay(event.getServer().overworld());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % DAY_CHECK_INTERVAL == 0) {
            applyDay(event.getServer().overworld());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyDay(player.serverLevel().getServer().overworld());
        }
    }

    /** Stops the day cycle and the weather at noon while {@code world.eternalDay} is on. */
    private static void applyDay(ServerLevel overworld) {
        if (!CraftorioConfig.ETERNAL_DAY.get()) {
            return;
        }
        var server = overworld.getServer();
        if (overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            overworld.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            overworld.setDayTime(NOON);
            overworld.setWeatherParameters(6000, 0, false, false);
        }
    }
}
