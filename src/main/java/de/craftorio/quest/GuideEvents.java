package de.craftorio.quest;

import de.craftorio.Craftorio;
import de.craftorio.CraftorioConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import java.util.List;
import de.craftorio.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Every player gets the handbook (and, if enabled, a starter kit) once, on their first login. */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class GuideEvents {
    private static final String GIVEN = "craftorio_guide_given";

    private GuideEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (!persisted.getBoolean(GIVEN)) {
                persisted.putBoolean(GIVEN, true);
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
                player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.GUIDE_BOOK.get()));
                if (CraftorioConfig.STARTER_KIT.get()) {
                    // Iron, copper and stone come from the starter pickaxe; coal fuels the first furnace and drill.
                    for (ItemStack stack : List.of(new ItemStack(ModItems.STARTER_PICKAXE.get()), new ItemStack(ModItems.WORKBENCH.get()),
                            new ItemStack(Items.COAL, 16))) {
                        player.getInventory().placeItemBackInInventory(stack);
                    }
                    player.sendSystemMessage(Component.translatable("craftorio.starter_kit").withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }
}
