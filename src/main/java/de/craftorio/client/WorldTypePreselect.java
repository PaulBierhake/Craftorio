package de.craftorio.client;

import de.craftorio.Craftorio;
import de.craftorio.CraftorioClientConfig;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.ref.WeakReference;

/** Selects the Craftorio world type once when the create-world screen opens; the player can still change it. */
@EventBusSubscriber(modid = Craftorio.MOD_ID, value = Dist.CLIENT)
public final class WorldTypePreselect {
    private static WeakReference<CreateWorldScreen> handled = new WeakReference<>(null);

    private WorldTypePreselect() {
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen) || handled.get() == screen
                || !CraftorioClientConfig.preselectWorldType()) {
            return;
        }
        handled = new WeakReference<>(screen);
        WorldCreationUiState state = screen.getUiState();
        for (WorldCreationUiState.WorldTypeEntry entry : state.getNormalPresetList()) {
            if (entry.preset().unwrapKey().map(key -> key.location().equals(Craftorio.id("factory"))).orElse(false)) {
                state.setWorldType(entry);
                return;
            }
        }
    }
}
