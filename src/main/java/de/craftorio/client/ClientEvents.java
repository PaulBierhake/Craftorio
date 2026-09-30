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
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ClientEvents {
    /** Opens the handbook at the current guide step. */
    public static final KeyMapping OPEN_GUIDE = new KeyMapping("key.craftorio.guide", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.craftorio");

    /** Uses the first ability of the tower the player is looking at. */
    public static final KeyMapping TOWER_ABILITY = new KeyMapping("key.craftorio.tower_ability", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, "key.categories.craftorio");

    private ClientEvents() {
    }

    @EventBusSubscriber(modid = Craftorio.MOD_ID, value = Dist.CLIENT)
    public static final class Game {
        private Game() {
        }

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack.getItem() instanceof net.minecraft.world.item.BlockItem block) {
                if (block.getBlock() instanceof de.craftorio.defense.TowerBlock tower) {
                    event.getToolTip().add(Component.translatable(tower.towerType().usesEnergy() ? "craftorio.tooltip.tower.energy"
                            : tower.towerType().usesFluid() ? "craftorio.tooltip.tower.fluid" : "craftorio.tooltip.tower.ammo").withStyle(ChatFormatting.AQUA));
                } else if (block.getBlock() == de.craftorio.registry.ModBlocks.ARENA_FEEDER.get()) {
                    event.getToolTip().add(Component.translatable("craftorio.tooltip.arena_feeder").withStyle(ChatFormatting.AQUA));
                }
            }
            addBlueprintDescription(event, stack);
            long unit = Economy.unitPrice(stack);
            if (unit == 0) {
                return;
            }
            Component line = stack.getCount() > 1
                    ? Component.translatable("craftorio.tooltip.sell_price_stack", Credits.format(unit), Credits.format(unit * stack.getCount()))
                    : Component.translatable("craftorio.tooltip.sell_price", Credits.format(unit));
            event.getToolTip().add(line.copy().withStyle(ChatFormatting.GOLD));
        }

        /** Items that a blueprint builds carry its short description. */
        private static void addBlueprintDescription(ItemTooltipEvent event, ItemStack stack) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Craftorio.MOD_ID)) {
                return;
            }
            for (var holder : minecraft.level.registryAccess().registryOrThrow(de.craftorio.registry.ModRegistries.BLUEPRINTS).holders().toList()) {
                if (holder.value().result().is(stack.getItem())) {
                    String key = "craftorio.blueprint." + holder.key().location().getPath() + ".desc";
                    if (net.minecraft.client.resources.language.I18n.exists(key)) {
                        event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
                    }
                    return;
                }
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            while (TOWER_ABILITY.consumeClick()) {
                if (minecraft.screen == null && minecraft.player != null && minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                        && minecraft.level != null && minecraft.level.getBlockState(hit.getBlockPos()).getBlock() instanceof de.craftorio.defense.TowerBlock) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new de.craftorio.network.TowerAbilityPayload(hit.getBlockPos(), 0));
                }
            }
            while (OPEN_GUIDE.consumeClick()) {
                if (minecraft.screen == null && minecraft.player != null) {
                    GuideScreen.open();
                }
            }
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientTeamState.clear();
            ClientTdState.clear();
            ClientTdEnemies.clear();
        }
    }

    @EventBusSubscriber(modid = Craftorio.MOD_ID, value = Dist.CLIENT)
    public static final class Mod {
        private Mod() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_GUIDE);
            event.register(TOWER_ABILITY);
        }

        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(Craftorio.id("credits"), CreditsHud::render);
            event.registerAboveAll(Craftorio.id("control_hints"), ControlHintsHud::render);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.CONVEYOR_BELT.get(), ConveyorBeltRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.POWER_POLE.get(), PowerPoleRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.REACTOR.get(), ReactorScreen::new);
            event.register(ModMenus.BEACON.get(), BeaconScreen::new);
            event.register(ModMenus.PUMPJACK.get(), PumpjackScreen::new);
            event.register(ModMenus.DRILL.get(), DrillScreen::new);
            event.register(ModMenus.TRADING_POST.get(), TradingPostScreen::new);
            event.register(ModMenus.PROCESSING_MACHINE.get(), ProcessingMachineScreen::new);
            event.register(ModMenus.TERMINAL.get(), TerminalScreen::new);
            event.register(ModMenus.WORKBENCH.get(), WorkbenchScreen::new);
            event.register(ModMenus.TOWER.get(), TowerScreen::new);
            event.register(ModMenus.DEPOT.get(), DepotScreen::new);
            event.register(ModMenus.LABORATORY.get(), LaboratoryScreen::new);
            event.register(ModMenus.BOILER.get(), BoilerScreen::new);
            event.register(ModMenus.FLUID_MACHINE.get(), FluidMachineScreen::new);
            event.register(ModMenus.ENTRANCE.get(), EntranceScreen::new);
        }
    }
}
