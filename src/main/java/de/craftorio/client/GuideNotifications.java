package de.craftorio.client;

import de.craftorio.quest.Quest;
import de.craftorio.quest.Quests;
import de.craftorio.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

/** Shows a toast when a guide goal is reached; kept apart from ClientTeamState, which common code references. */
final class GuideNotifications {
    private GuideNotifications() {
    }

    static void announce(List<Long> before, List<Long> after, Set<String> claimed) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            for (int i = 0; i < Quests.ALL.size() && i < after.size(); i++) {
                Quest quest = Quests.ALL.get(i);
                if (!claimed.contains(quest.id()) && before.get(i) < quest.amount() && after.get(i) >= quest.amount()) {
                    minecraft.getToasts().addToast(new GuideToast(Component.translatable("craftorio.toast.goal"),
                            Component.translatable("craftorio.quest." + quest.id()), new ItemStack(ModItems.GUIDE_BOOK.get())));
                }
            }
        });
    }
}
