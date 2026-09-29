package de.craftorio.energy;

import de.craftorio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PowerPoleBlockEntity extends BlockEntity {
    /** Poles this one is wired to; synced to clients only for drawing wires. */
    private List<BlockPos> wires = List.of();

    public PowerPoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_POLE.get(), pos, state);
    }

    public List<BlockPos> wires() {
        return wires;
    }

    void setWires(List<BlockPos> wires) {
        if (!wires.equals(this.wires)) {
            this.wires = List.copyOf(wires);
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            PowerGrid.of(serverLevel).addPole(worldPosition,
                    getBlockState().getBlock() instanceof PowerPoleBlock pole ? pole.tier() : PoleTier.SMALL);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) {
            PowerGrid.of(serverLevel).removePole(worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel serverLevel) {
            PowerGrid.of(serverLevel).removePole(worldPosition);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ListTag list = new ListTag();
        wires.forEach(pos -> list.add(LongTag.valueOf(pos.asLong())));
        tag.put("wires", list);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readWires(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readWires(packet.getTag());
    }

    private void readWires(CompoundTag tag) {
        wires = tag.getList("wires", Tag.TAG_LONG).stream().map(element -> BlockPos.of(((LongTag) element).getAsLong())).toList();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
