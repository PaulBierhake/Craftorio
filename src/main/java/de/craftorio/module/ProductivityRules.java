package de.craftorio.module;

import de.craftorio.Craftorio;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Productivity modules only work for intermediate products (item tag {@code craftorio:productivity_allowed}): plates,
 * parts, circuits, science packs, uranium and the products of oil recipes, but not for buildings, modules or ammunition.
 * Recipes that make no item (oil and cracking) are always allowed.
 */
public final class ProductivityRules {
    public static final TagKey<Item> ALLOWED = TagKey.create(Registries.ITEM, Craftorio.id("productivity_allowed"));

    private ProductivityRules() {
    }

    public static boolean allowed(ItemStack product) {
        return product.isEmpty() || product.is(ALLOWED);
    }

    public static boolean allowed(List<ItemStack> products) {
        return products.stream().allMatch(ProductivityRules::allowed);
    }
}
