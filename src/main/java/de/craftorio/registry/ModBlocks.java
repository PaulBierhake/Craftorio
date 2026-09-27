package de.craftorio.registry;

import de.craftorio.Craftorio;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Craftorio.MOD_ID);

    /** Unbreakable layer separating surface, caves and mines. Only passable through built entrances. */
    public static final DeferredBlock<Block> CAP_ROCK = BLOCKS.registerSimpleBlock("cap_rock",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(-1.0F, 3_600_000.0F)
                    .noLootTable()
                    .isValidSpawn(Blocks::never)
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.DEEPSLATE));

    private ModBlocks() {
    }
}
