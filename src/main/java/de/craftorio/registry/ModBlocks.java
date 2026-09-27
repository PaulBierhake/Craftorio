package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.economy.block.TradingPostBlock;
import de.craftorio.logistics.ConveyorBeltBlock;
import de.craftorio.logistics.InserterBlock;
import de.craftorio.machine.BurnerDrillBlock;
import de.craftorio.world.OreFieldBlock;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

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

    public static final DeferredBlock<TradingPostBlock> TRADING_POST = BLOCKS.registerBlock("trading_post", TradingPostBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<OreFieldBlock> IRON_ORE_FIELD = oreField("iron_ore_field", () -> Items.RAW_IRON, MapColor.RAW_IRON);
    public static final DeferredBlock<OreFieldBlock> COPPER_ORE_FIELD = oreField("copper_ore_field", () -> Items.RAW_COPPER, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<OreFieldBlock> COAL_FIELD = oreField("coal_field", () -> Items.COAL, MapColor.COLOR_BLACK);

    public static final DeferredBlock<BurnerDrillBlock> BURNER_DRILL = BLOCKS.registerBlock("burner_drill", BurnerDrillBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(BurnerDrillBlock.LIT) ? 10 : 0)
                    .sound(SoundType.METAL));

    public static final DeferredBlock<ConveyorBeltBlock> CONVEYOR_BELT = BLOCKS.registerBlock("conveyor_belt", ConveyorBeltBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5F)
                    .noOcclusion()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<InserterBlock> INSERTER = BLOCKS.registerBlock("inserter", InserterBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.5F)
                    .noOcclusion()
                    .sound(SoundType.METAL));

    private static DeferredBlock<OreFieldBlock> oreField(String name, Supplier<? extends ItemLike> resource, MapColor color) {
        return BLOCKS.registerBlock(name, properties -> new OreFieldBlock(resource, properties),
                BlockBehaviour.Properties.of()
                        .mapColor(color)
                        .strength(4.0F, 1200.0F)
                        .requiresCorrectToolForDrops()
                        .noLootTable()
                        .pushReaction(PushReaction.BLOCK)
                        .sound(SoundType.STONE));
    }

    private ModBlocks() {
    }
}
