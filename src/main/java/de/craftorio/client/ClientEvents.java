package de.craftorio.client;

import de.craftorio.Craftorio;
import de.craftorio.economy.Credits;
import de.craftorio.economy.Economy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ClientEvents {
    private ClientEvents() {
    }

    @EventBusSubscriber(modid = Craftorio.MOD_ID, value = Dist.CLIENT)
    public static final class Game {
        private Game() {
        }

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            long unit = Economy.unitPrice(stack);
            if (unit == 0) {
                return;
            }
            Component line = stack.getCount() > 1
                    ? Component.translatable("craftorio.tooltip.sell_price_stack", Credits.format(unit), Credits.format(unit * stack.getCount()))
                    : Component.translatable("craftorio.tooltip.sell_price", Credits.format(unit));
            event.getToolTip().add(line.copy().withStyle(ChatFormatting.GOLD));
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientTeamState.clear();
        }
    }

    @EventBusSubscriber(modid = Craftorio.MOD_ID, value = Dist.CLIENT)
    public static final class Mod {
        private Mod() {
        }

        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(Craftorio.id("credits"), CreditsHud::render);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.CONVEYOR_BELT.get(), ConveyorBeltRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.POWER_POLE.get(), PowerPoleRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.GENERATOR.get(), GeneratorScreen::new);
            event.register(ModMenus.PROCESSING_MACHINE.get(), ProcessingMachineScreen::new);
            event.register(ModMenus.TERMINAL.get(), TerminalScreen::new);
            event.register(ModMenus.WORKBENCH.get(), WorkbenchScreen::new);
        }
    }
}
