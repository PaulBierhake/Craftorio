package de.craftorio.team;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** A group of players sharing one credit account. Every player belongs to exactly one team. */
public final class Team {
    private final UUID id;
    private final String name;
    private final Set<UUID> members = new LinkedHashSet<>();
    // Pending invites are not persisted; they expire on server restart.
    private final Set<UUID> invites = new HashSet<>();
    private long balance;

    Team(UUID id, String name, long balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public long balance() {
        return balance;
    }

    public Set<UUID> members() {
        return Collections.unmodifiableSet(members);
    }

    public boolean isInvited(UUID player) {
        return invites.contains(player);
    }

    void setBalance(long balance) {
        this.balance = balance;
    }

    Set<UUID> mutableMembers() {
        return members;
    }

    Set<UUID> mutableInvites() {
        return invites;
    }
}
