package de.craftorio.client;

import java.util.List;
import java.util.Set;

/** Last team state received from the server. Plain Java so it can be referenced from common code. */
public final class ClientTeamState {
    private static volatile String teamName;
    private static volatile long balance;
    private static volatile Set<String> unlocked = Set.of();
    private static volatile Set<String> claimedQuests = Set.of();
    private static volatile List<Long> questProgress = List.of();

    private ClientTeamState() {
    }

    public static void update(String teamName, long balance, List<String> unlocked, List<String> claimedQuests, List<Long> questProgress) {
        List<Long> previous = ClientTeamState.questProgress;
        ClientTeamState.questProgress = List.copyOf(questProgress);
        // Only goals reached while playing are announced, not the ones already done at login.
        if (ClientTeamState.teamName != null && previous.size() == questProgress.size()) {
            GuideNotifications.announce(previous, questProgress, Set.copyOf(claimedQuests));
        }
        ClientTeamState.teamName = teamName;
        ClientTeamState.balance = balance;
        ClientTeamState.unlocked = Set.copyOf(unlocked);
        ClientTeamState.claimedQuests = Set.copyOf(claimedQuests);
    }

    public static void clear() {
        teamName = null;
        balance = 0;
        unlocked = Set.of();
        claimedQuests = Set.of();
        questProgress = List.of();
    }

    /** Progress of every quest in guide order. */
    public static List<Long> questProgress() {
        return questProgress;
    }

    public static Set<String> claimedQuests() {
        return claimedQuests;
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
