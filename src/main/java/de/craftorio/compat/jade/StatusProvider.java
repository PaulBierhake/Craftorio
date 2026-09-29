package de.craftorio.compat.jade;

import de.craftorio.Craftorio;
import de.craftorio.defense.TowerBlockEntity;
import de.craftorio.defense.TowerRuinBlockEntity;
import de.craftorio.economy.Credits;
import de.craftorio.logistics.ElevatorBlock;
import de.craftorio.logistics.ElevatorBlockEntity;
import de.craftorio.machine.DrillBlockEntity;
import de.craftorio.machine.ProcessingMachineBlockEntity;
import de.craftorio.protection.BlockOwnership;
import de.craftorio.world.cave.CaveEntranceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

/** Status lines computed on the server (owner, drill rate, machine progress, tower health, ...) shown in Jade. */
enum StatusProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Craftorio.id("status");
    private static final String KEY = "craftorio_lines";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        BlockOwnership.get(level).owner(level, accessor.getPosition()).ifPresent(team ->
                lines.add(Component.translatable("craftorio.jade.owner", team.name()).withStyle(ChatFormatting.GRAY)));
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof DrillBlockEntity drill) {
            lines.add(drill.status());
        } else if (blockEntity instanceof ProcessingMachineBlockEntity machine && machine.progressPercent() >= 0) {
            lines.add(Component.translatable("craftorio.jade.progress", machine.progressPercent()));
        } else if (blockEntity instanceof TowerBlockEntity tower) {
            lines.add(Component.translatable("craftorio.jade.tower", tower.health(), tower.maxHealth(), tower.upgradeLevel()));
            if (tower.type().usesItemAmmo()) {
                lines.add(Component.translatable("craftorio.jade.ammo", tower.ammo().getStackInSlot(0).getCount()));
            }
        } else if (blockEntity instanceof TowerRuinBlockEntity ruin) {
            lines.add(Component.translatable("craftorio.jade.ruin", Credits.format(ruin.rebuildCost())).withStyle(ChatFormatting.RED));
        } else if (blockEntity instanceof CaveEntranceBlockEntity site) {
            lines.add(site.status());
        } else if (blockEntity instanceof ElevatorBlockEntity) {
            ElevatorBlock.Mode mode = accessor.getBlockState().getValue(ElevatorBlock.MODE);
            lines.add(Component.translatable("craftorio.elevator.mode." + mode.getSerializedName()));
            if (mode != ElevatorBlock.Mode.RECEIVE) {
                lines.add(ElevatorBlockEntity.findPartner(level, accessor.getPosition(), mode)
                        .map(partner -> Component.translatable("craftorio.elevator.target", Math.abs(partner.getY() - accessor.getPosition().getY())))
                        .orElse(Component.translatable("craftorio.elevator.no_target")));
            }
        }
        if (!lines.isEmpty()) {
            ListTag list = new ListTag();
            lines.forEach(line -> list.add(StringTag.valueOf(Component.Serializer.toJson(line, level.registryAccess()))));
            data.put(KEY, list);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        for (Tag line : accessor.getServerData().getList(KEY, Tag.TAG_STRING)) {
            Component component = Component.Serializer.fromJson(line.getAsString(), accessor.getLevel().registryAccess());
            if (component != null) {
                tooltip.add(component);
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
