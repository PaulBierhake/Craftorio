package de.craftorio.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * A recipe for hand crafting at a workbench of at least {@link #tier}. It is available from the start unless a
 * research lists its id under {@code unlocks}; then the team needs that research.
 * Loaded from {@code data/<namespace>/craftorio/blueprint/*.json} and synced to clients.
 */
public record Blueprint(ItemStack result, List<SizedIngredient> ingredients, int tier, int order) {
    public static final int MAX_TIER = 3;
    public static final int MAX_INGREDIENTS = 4;

    public static final Codec<Blueprint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(Blueprint::result),
            SizedIngredient.FLAT_CODEC.listOf(1, MAX_INGREDIENTS).fieldOf("ingredients").forGetter(Blueprint::ingredients),
            Codec.intRange(1, MAX_TIER).fieldOf("tier").forGetter(Blueprint::tier),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Blueprint::order)
    ).apply(instance, Blueprint::new));
}
