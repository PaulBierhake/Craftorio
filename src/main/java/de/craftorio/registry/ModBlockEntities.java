package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.economy.block.TradingPostBlockEntity;
import de.craftorio.defense.TowerBlockEntity;
import de.craftorio.defense.TowerRuinBlockEntity;
import de.craftorio.energy.GeneratorBlockEntity;
import de.craftorio.energy.PowerPoleBlockEntity;
import de.craftorio.machine.ProcessingMachineBlockEntity;
import de.craftorio.world.cave.CaveEntranceBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import de.craftorio.logistics.ConveyorBeltBlockEntity;
import de.craftorio.logistics.ElevatorBlockEntity;
import de.craftorio.logistics.InserterBlockEntity;
import de.craftorio.machine.DrillBlockEntity;
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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrillBlockEntity>> DRILL = BLOCK_ENTITIES.register("burner_drill",
            () -> BlockEntityType.Builder.of(DrillBlockEntity::new, ModBlocks.BURNER_DRILL.get(), ModBlocks.ELECTRIC_DRILL.get(),
                    ModBlocks.DEEP_DRILL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBeltBlockEntity>> CONVEYOR_BELT = BLOCK_ENTITIES.register("conveyor_belt",
            () -> BlockEntityType.Builder.of(ConveyorBeltBlockEntity::new, ModBlocks.CONVEYOR_BELT.get(), ModBlocks.FAST_BELT.get(),
                    ModBlocks.EXPRESS_BELT.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InserterBlockEntity>> INSERTER = BLOCK_ENTITIES.register("inserter",
            () -> BlockEntityType.Builder.of(InserterBlockEntity::new, ModBlocks.INSERTER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR = BLOCK_ENTITIES.register("coal_generator",
            () -> BlockEntityType.Builder.of(GeneratorBlockEntity::new, ModBlocks.COAL_GENERATOR.get(), ModBlocks.REACTOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerPoleBlockEntity>> POWER_POLE = BLOCK_ENTITIES.register("power_pole",
            () -> BlockEntityType.Builder.of(PowerPoleBlockEntity::new, ModBlocks.POWER_POLE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> MACHINE = BLOCK_ENTITIES.register("machine",
            () -> BlockEntityType.Builder.of(ProcessingMachineBlockEntity::new,
                    ModBlocks.ELECTRIC_FURNACE.get(), ModBlocks.PRESS.get(), ModBlocks.ASSEMBLER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TowerBlockEntity>> TOWER = BLOCK_ENTITIES.register("tower",
            () -> BlockEntityType.Builder.of(TowerBlockEntity::new,
                    ModBlocks.CROSSBOW_TOWER.get(), ModBlocks.GUN_TURRET.get(), ModBlocks.TESLA_TOWER.get(),
                    ModBlocks.LASER_TOWER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TowerRuinBlockEntity>> TOWER_RUIN = BLOCK_ENTITIES.register("tower_ruin",
            () -> BlockEntityType.Builder.of(TowerRuinBlockEntity::new, ModBlocks.TOWER_RUIN.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CaveEntranceBlockEntity>> CAVE_ENTRANCE = BLOCK_ENTITIES.register("cave_entrance",
            () -> BlockEntityType.Builder.of(CaveEntranceBlockEntity::new, ModBlocks.CAVE_ENTRANCE.get(), ModBlocks.MINE_SHAFT.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElevatorBlockEntity>> ELEVATOR = BLOCK_ENTITIES.register("elevator",
            () -> BlockEntityType.Builder.of(ElevatorBlockEntity::new, ModBlocks.ELEVATOR.get()).build(null));

    private ModBlockEntities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TRADING_POST.get(), (post, side) -> post.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DRILL.get(), (drill, side) -> drill.handler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DRILL.get(), (drill, side) -> drill.energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONVEYOR_BELT.get(), ConveyorBeltBlockEntity::handler);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GENERATOR.get(), (generator, side) -> insertOnly(generator.fuel()));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, GENERATOR.get(), (generator, side) -> generator.energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE.get(), (machine, side) -> machine.automation());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MACHINE.get(), (machine, side) -> machine.energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TOWER.get(), (tower, side) -> tower.type().usesEnergy() ? null : insertOnly(tower.ammo()));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELEVATOR.get(), (elevator, side) -> elevator.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CAVE_ENTRANCE.get(), (site, side) -> site.materials());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CAVE_ENTRANCE.get(), (site, side) -> site.energy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TOWER.get(), (tower, side) -> tower.type().usesEnergy() ? tower.energy() : null);
    }

    /** Lets automation fill a fuel slot without being able to take the fuel back out. */
    private static IItemHandler insertOnly(IItemHandler handler) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return handler.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return handler.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return handler.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return handler.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return handler.isItemValid(slot, stack);
            }
        };
    }
}
