package de.craftorio.compat.jade;

import de.craftorio.world.OreFieldBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Jade integration; only loaded when Jade is installed. */
@WailaPlugin
public final class CraftorioJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(StatusProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(StatusProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(OreFieldProvider.INSTANCE, OreFieldBlock.class);
    }
}
