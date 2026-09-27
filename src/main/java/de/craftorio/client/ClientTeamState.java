package de.craftorio.client;

import java.util.List;
import java.util.Set;

/** Last team state received from the server. Plain Java so it can be referenced from common code. */
public final class ClientTeamState {
    private static volatile String teamName;
    private static volatile long balance;
    private static volatile Set<String> unlocked = Set.of();

    private ClientTeamState() {
    }

    public static void update(String teamName, long balance, List<String> unlocked) {
        ClientTeamState.teamName = teamName;
        ClientTeamState.balance = balance;
        ClientTeamState.unlocked = Set.copyOf(unlocked);
    }

    public static void clear() {
        teamName = null;
        balance = 0;
        unlocked = Set.of();
    }

    public static Set<String> unlocked() {
        return unlocked;
    }

    /** Null until the server has sent the first update. */
    public static String teamName() {
        return teamName;
    }

    public static long balance() {
        return balance;
    }
}
