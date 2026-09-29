package de.craftorio.research;

import de.craftorio.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Research lookups shared by server logic and client screens. */
public final class Researches {
    private static Registry<Research> cachedRegistry;
    private static int cachedSize = -1;
    private static Map<String, String> cachedGates = Map.of();

    private Researches() {
    }

    /** All researches by order, then id – identical on server and client, so an index identifies one. */
    public static List<Holder.Reference<Research>> sorted(RegistryAccess access) {
        return access.registryOrThrow(ModRegistries.RESEARCH).holders()
                .sorted(Comparator.<Holder.Reference<Research>>comparingInt(holder -> holder.value().order())
                        .thenComparing(holder -> holder.key().location().toString()))
                .toList();
    }

    public static String id(Holder.Reference<Research> holder) {
        return holder.key().location().toString();
    }

    public static List<String> requires(Research research) {
        return research.requires().stream().map(ResourceLocation::toString).toList();
    }

    /** Which research unlocks the blueprint or machine recipe with this id (first one wins); empty if it is available from the start. */
    public static Optional<String> gate(RegistryAccess access, String unlockableId) {
        return Optional.ofNullable(gates(access).get(unlockableId));
    }

    /** Does a team with these finished researches know the blueprint or machine recipe? Not gated means known. */
    public static boolean knows(RegistryAccess access, Set<String> researched, String unlockableId) {
        String gate = gates(access).get(unlockableId);
        return gate == null || researched.contains(gate);
    }

    /** Ids of all blueprints and recipes a team with these researches knows, given the candidate ids. */
    public static Set<String> knownOf(RegistryAccess access, Set<String> researched, Iterable<String> candidates) {
        Set<String> known = new HashSet<>();
        for (String candidate : candidates) {
            if (knows(access, researched, candidate)) {
                known.add(candidate);
            }
        }
        return known;
    }

    private static synchronized Map<String, String> gates(RegistryAccess access) {
        Registry<Research> registry = access.registryOrThrow(ModRegistries.RESEARCH);
        if (registry != cachedRegistry || registry.size() != cachedSize) {
            Map<String, String> gates = new HashMap<>();
            for (Holder.Reference<Research> holder : sorted(access)) {
                for (ResourceLocation unlocked : holder.value().unlocks()) {
                    gates.putIfAbsent(unlocked.toString(), id(holder));
                }
            }
            cachedGates = gates;
            cachedRegistry = registry;
            cachedSize = registry.size();
        }
        return cachedGates;
    }
}
