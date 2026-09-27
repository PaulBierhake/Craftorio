package de.craftorio.defense;

import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TowerRuinBlockEntity extends BlockEntity {
    private TowerType towerType = TowerType.CROSSBOW;
    private int upgradeLevel = 1;

    public TowerRuinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWER_RUIN.get(), pos, state);
    }

    void remember(TowerType towerType, int upgradeLevel) {
        this.towerType = towerType;
        this.upgradeLevel = upgradeLevel;
        setChanged();
    }

    public long rebuildCost() {
        return towerType.rebuildCost();
    }

    /** Puts the tower back with full health and its previous upgrade level. */
    public void rebuild() {
        if (level == null) {
            return;
        }
        BlockPos pos = worldPosition;
        int keptLevel = upgradeLevel;
        level.setBlock(pos, ModBlocks.tower(towerType).get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            tower.restore(keptLevel);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("tower", towerType.name());
        tag.putInt("level", upgradeLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            towerType = TowerType.valueOf(tag.getString("tower"));
        } catch (IllegalArgumentException unknown) {
            towerType = TowerType.CROSSBOW;
        }
        upgradeLevel = Math.max(1, tag.getInt("level"));
    }
}
