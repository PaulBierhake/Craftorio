package de.craftorio.machine;

import de.craftorio.client.ClientTeamState;
import de.craftorio.protection.BlockOwnership;
import de.craftorio.research.Researches;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** What machines may do depending on research. */
public final class MachineRules {
    private MachineRules() {
    }

    /**
     * May the machine at {@code pos} use the recipe? Recipes that no research unlocks are open to everybody; otherwise
     * the team owning the machine needs the research. Machines without an owner (e.g. in tests) know every recipe.
     */
    public static boolean knows(Level level, BlockPos pos, ResourceLocation recipe) {
        if (level.isClientSide) {
            return Researches.knows(level.registryAccess(), ClientTeamState.researched(), recipe.toString());
        }
        return BlockOwnership.get((ServerLevel) level).owner((ServerLevel) level, pos)
                .map(team -> Researches.knows(level.registryAccess(), team.researched(), recipe.toString())).orElse(true);
    }
}
