package de.craftorio.menu;

import de.craftorio.research.ResearchActions;
import de.craftorio.blueprint.TerminalStats;
import de.craftorio.quest.QuestActions;
import de.craftorio.defense.TowerDefense;
import de.craftorio.registry.ModMenus;
import de.craftorio.team.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** No slots; button ids below {@link #TD_START} are indexes into {@link de.craftorio.research.Researches#sorted} (queue or dequeue). */
public final class TerminalMenu extends AbstractContainerMenu {
    public static final int TD_START = 10_000;
    public static final int TD_TOGGLE_AUTO = 10_001;
    public static final int TD_REPAIR_ALL = 10_002;
    public static final int TD_CALL_WAVE = 10_003;
    /** Plus the quest's index in {@link de.craftorio.quest.Quests#ALL}. */
    public static final int QUEST_CLAIM = 20_000;

    private final BlockPos pos;
    private final TerminalStats stats;

    public TerminalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), TerminalStats.STREAM_CODEC.decode(buf));
    }

    public TerminalMenu(int containerId, Inventory inventory, BlockPos pos, TerminalStats stats) {
        super(ModMenus.TERMINAL.get(), containerId);
        this.pos = pos;
        this.stats = stats;
    }

    public TerminalStats stats() {
        return stats;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (id < TD_START) {
            return ResearchActions.toggle(serverPlayer, id);
        }
        if (id >= QUEST_CLAIM) {
            return QuestActions.claim(serverPlayer, id - QUEST_CLAIM);
        }
        TowerDefense defense = TowerDefense.get(serverPlayer.server);
        UUID team = TeamData.registry(serverPlayer.server).ensureTeam(serverPlayer.getUUID(), serverPlayer.getGameProfile().getName()).id();
        switch (id) {
            case TD_START -> serverPlayer.displayClientMessage(defense.start(serverPlayer.server, team), true);
            case TD_TOGGLE_AUTO -> defense.toggleAuto(team);
            case TD_CALL_WAVE -> serverPlayer.displayClientMessage(defense.callWave(serverPlayer.server, team), true);
            case TD_REPAIR_ALL -> {
                if (TeamData.maySpend(serverPlayer)) {
                    serverPlayer.displayClientMessage(defense.repairAll(serverPlayer.server, team), true);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getCenter()) <= 64;
    }
}
