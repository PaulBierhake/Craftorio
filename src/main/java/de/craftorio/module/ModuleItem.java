package de.craftorio.module;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/** A module: goes into the module slots of machines and beacons. */
public final class ModuleItem extends Item {
    private final ModuleKind kind;
    private final int tier;

    public ModuleItem(ModuleKind kind, int tier, Properties properties) {
        super(properties);
        this.kind = kind;
        this.tier = tier;
    }

    public ModuleKind kind() {
        return kind;
    }

    public int tier() {
        return tier;
    }

    public ModuleEffects effects() {
        return kind.effects(tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModuleEffects effects = effects();
        addLine(tooltip, "speed", effects.speed(), effects.speed() >= 0);
        addLine(tooltip, "energy", effects.energy(), effects.energy() <= 0);
        addLine(tooltip, "productivity", effects.productivity(), true);
        if (!kind.beaconAllowed()) {
            tooltip.add(Component.translatable("craftorio.module.no_beacon").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void addLine(List<Component> tooltip, String effect, double value, boolean good) {
        if (value != 0) {
            tooltip.add(Component.translatable("craftorio.module.effect." + effect, String.format(Locale.ROOT, "%+.0f", value))
                    .withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }
}
