package de.craftorio.client;

/** Last team state received from the server. Plain Java so it can be referenced from common code. */
public final class ClientTeamState {
    private static volatile String teamName;
    private static volatile long balance;

    private ClientTeamState() {
    }

    public static void update(String teamName, long balance) {
        ClientTeamState.teamName = teamName;
        ClientTeamState.balance = balance;
    }

    public static void clear() {
        teamName = null;
        balance = 0;
    }

    /** Null until the server has sent the first update. */
    public static String teamName() {
        return teamName;
    }

    public static long balance() {
        return balance;
    }
}
