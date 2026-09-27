package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.economy.block.TradingPostBlockEntity;
import de.craftorio.logistics.ConveyorBeltBlockEntity;
import de.craftorio.logistics.InserterBlockEntity;
import de.craftorio.machine.BurnerDrillBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Craftorio.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // null data fixer type is standard for mod block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TradingPostBlockEntity>> TRADING_POST = BLOCK_ENTITIES.register("trading_post",
            () -> BlockEntityType.Builder.of(TradingPostBlockEntity::new, ModBlocks.TRADING_POST.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BurnerDrillBlockEntity>> BURNER_DRILL = BLOCK_ENTITIES.register("burner_drill",
            () -> BlockEntityType.Builder.of(BurnerDrillBlockEntity::new, ModBlocks.BURNER_DRILL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBeltBlockEntity>> CONVEYOR_BELT = BLOCK_ENTITIES.register("conveyor_belt",
            () -> BlockEntityType.Builder.of(ConveyorBeltBlockEntity::new, ModBlocks.CONVEYOR_BELT.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InserterBlockEntity>> INSERTER = BLOCK_ENTITIES.register("inserter",
            () -> BlockEntityType.Builder.of(InserterBlockEntity::new, ModBlocks.INSERTER.get()).build(null));

    private ModBlockEntities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TRADING_POST.get(), (post, side) -> post.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BURNER_DRILL.get(), (drill, side) -> drill.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONVEYOR_BELT.get(), ConveyorBeltBlockEntity::handler);
    }
}
