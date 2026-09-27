package de.craftorio.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * Something a team unlocks in the terminal and then builds at a workbench of at least {@link #tier}.
 * Loaded from {@code data/<namespace>/craftorio/blueprint/*.json} and synced to clients.
 *
 * @param cost         credits to unlock; 0 together with no requirements makes a free starter blueprint
 * @param unlockItems  key materials consumed from the buyer's inventory when unlocking
 * @param requires     blueprints that must be unlocked first
 */
public record Blueprint(ItemStack result, List<SizedIngredient> ingredients, int tier, long cost,
                        List<SizedIngredient> unlockItems, List<ResourceLocation> requires, int order) {
    public static final int MAX_TIER = 3;
    public static final int MAX_INGREDIENTS = 4;

    public static final Codec<Blueprint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(Blueprint::result),
            SizedIngredient.FLAT_CODEC.listOf(1, MAX_INGREDIENTS).fieldOf("ingredients").forGetter(Blueprint::ingredients),
            Codec.intRange(1, MAX_TIER).fieldOf("tier").forGetter(Blueprint::tier),
            Codec.LONG.optionalFieldOf("cost", 0L).forGetter(Blueprint::cost),
            SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("unlock_items", List.of()).forGetter(Blueprint::unlockItems),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Blueprint::requires),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Blueprint::order)
    ).apply(instance, Blueprint::new));

    public boolean isFree() {
        return cost == 0 && unlockItems.isEmpty() && requires.isEmpty();
    }
}
