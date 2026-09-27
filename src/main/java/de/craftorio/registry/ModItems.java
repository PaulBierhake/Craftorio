package de.craftorio.registry;

import de.craftorio.Craftorio;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Craftorio.MOD_ID);

    public static final DeferredItem<BlockItem> CAP_ROCK = ITEMS.registerSimpleBlockItem("cap_rock", ModBlocks.CAP_ROCK);

    private ModItems() {
    }
}
