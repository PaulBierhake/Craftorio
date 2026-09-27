package de.craftorio.world.cave;

import de.craftorio.energy.EnergyBuffer;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CaveEntranceBlockEntity extends BlockEntity {
    public static final int DRILL_TICKS = 1_200;
    public static final int ENERGY_PER_TICK = 40;

    private final Map<Item, Integer> delivered = new LinkedHashMap<>();
    private final EnergyBuffer energy = new EnergyBuffer(20_000, 500, 0, this::setChanged);
    private final IItemHandler materials = new MaterialHandler();
    private int drillProgress;

    public CaveEntranceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAVE_ENTRANCE.get(), pos, state);
    }

    /** What the construction site needs in total. */
    public static Map<Item, Integer> requirements() {
        Map<Item, Integer> required = new LinkedHashMap<>();
        required.put(Items.COBBLESTONE, 128);
        required.put(ModItems.IRON_PLATE.get(), 32);
        required.put(ModItems.IRON_GEAR.get(), 16);
        required.put(ModItems.MOTOR.get(), 8);
        return required;
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public IItemHandler materials() {
        return materials;
    }

    public int stage() {
        return getBlockState().getValue(CaveEntranceBlock.STAGE);
    }

    public int missing(Item item) {
        return Math.max(0, requirements().getOrDefault(item, 0) - delivered.getOrDefault(item, 0));
    }

    public boolean needs(ItemStack stack) {
        return stage() == CaveEntranceBlock.STAGE_MATERIALS && !stack.isEmpty() && missing(stack.getItem()) > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CaveEntranceBlockEntity site) {
        if (state.getValue(CaveEntranceBlock.STAGE) != CaveEntranceBlock.STAGE_DRILLING) {
            return;
        }
        if (site.energy.consume(ENERGY_PER_TICK)) {
            site.drillProgress++;
            site.setChanged();
            if (site.drillProgress % 40 == 0) {
                level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.8F, 0.6F);
            }
            if (site.drillProgress >= DRILL_TICKS) {
                site.open((ServerLevel) level);
            }
        }
    }

    private void checkComplete() {
        if (level != null && stage() == CaveEntranceBlock.STAGE_MATERIALS
                && requirements().keySet().stream().allMatch(item -> missing(item) == 0)) {
            level.setBlock(worldPosition, getBlockState().setValue(CaveEntranceBlock.STAGE, CaveEntranceBlock.STAGE_DRILLING), Block.UPDATE_ALL);
        }
    }

    /** Opens the shaft down to the cave floor and unlocks the surrounding cave area. */
    void open(ServerLevel level) {
        CaveAreas areas = CaveAreas.get(level.getServer());
        int floor = areas.shape().floorY(worldPosition.getX(), worldPosition.getZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState lining = Blocks.COBBLESTONE.defaultBlockState();
        for (int y = worldPosition.getY() - 1; y > floor; y--) {
            // Seal the walls above the cave layer so water and lava from vanilla caves cannot flood the shaft;
            // inside the cave layer the shaft opens into the hall.
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if ((Math.abs(dx) == 2 || Math.abs(dz) == 2) && y >= CaveLayers.CAP_ONE_BOTTOM) {
                        pos.set(worldPosition.getX() + dx, y, worldPosition.getZ() + dz);
                        BlockState wall = level.getBlockState(pos);
                        if (wall.isAir() || !wall.getFluidState().isEmpty() || wall.canBeReplaced()) {
                            level.setBlock(pos, lining, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    pos.set(worldPosition.getX() + dx, y, worldPosition.getZ() + dz);
                    BlockState target = dx == 0 && dz == 0
                            ? Blocks.SCAFFOLDING.defaultBlockState().setValue(ScaffoldingBlock.DISTANCE, 0).setValue(ScaffoldingBlock.BOTTOM, false)
                            : Blocks.AIR.defaultBlockState();
                    level.setBlock(pos, target, Block.UPDATE_CLIENTS);
                }
            }
        }
        pos.set(worldPosition.getX(), floor, worldPosition.getZ());
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        areas.unlockAround(worldPosition);
        level.setBlock(worldPosition, getBlockState().setValue(CaveEntranceBlock.STAGE, CaveEntranceBlock.STAGE_OPEN), Block.UPDATE_ALL);
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1F, 0.7F);
        level.players().stream()
                .filter(player -> player.distanceToSqr(worldPosition.getCenter()) < 128 * 128)
                .forEach(player -> player.sendSystemMessage(Component.translatable("craftorio.cave.opened",
                        new ChunkPos(worldPosition).x, new ChunkPos(worldPosition).z)));
    }

    public Component status() {
        return switch (stage()) {
            case CaveEntranceBlock.STAGE_MATERIALS -> {
                MutableComponent line = Component.translatable("craftorio.cave.needs");
                requirements().forEach((item, count) -> {
                    int missing = missing(item);
                    if (missing > 0) {
                        line.append(" ").append(Component.literal(missing + "× ")).append(item.getDescription()).append(";");
                    }
                });
                yield line;
            }
            case CaveEntranceBlock.STAGE_DRILLING -> Component.translatable("craftorio.cave.drilling",
                    100 * drillProgress / DRILL_TICKS, ENERGY_PER_TICK);
            default -> Component.translatable("craftorio.cave.open");
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag deliveredTag = new CompoundTag();
        delivered.forEach((item, count) -> deliveredTag.putInt(BuiltInRegistries.ITEM.getKey(item).toString(), count));
        tag.put("delivered", deliveredTag);
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("progress", drillProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        delivered.clear();
        CompoundTag deliveredTag = tag.getCompound("delivered");
        for (String key : deliveredTag.getAllKeys()) {
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(key))
                    .ifPresent(item -> delivered.put(item, deliveredTag.getInt(key)));
        }
        energy.setEnergy(tag.getInt("energy"));
        drillProgress = tag.getInt("progress");
    }

    /** Accepts required materials up to what is still missing; nothing can be taken out. */
    private final class MaterialHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!needs(stack)) {
                return stack;
            }
            int accepted = Math.min(stack.getCount(), missing(stack.getItem()));
            if (!simulate) {
                delivered.merge(stack.getItem(), accepted, Integer::sum);
                setChanged();
                checkComplete();
            }
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return needs(stack);
        }
    }
}
