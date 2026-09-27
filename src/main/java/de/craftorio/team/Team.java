package de.craftorio.team;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A group of players sharing one credit account and one set of unlocked blueprints. Every player belongs to exactly one team. */
public final class Team {
    /** Earnings are bucketed per in-game minute for the "last minutes" statistic. */
    static final int EARNING_BUCKETS = 10;

    private final UUID id;
    private final String name;
    private final Set<UUID> members = new LinkedHashSet<>();
    // Pending invites are not persisted; they expire on server restart.
    private final Set<UUID> invites = new HashSet<>();
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final Set<String> claimedQuests = new LinkedHashSet<>();
    private final Map<String, Long> built = new HashMap<>();
    private final Map<String, Sales> sales = new HashMap<>();
    private final long[] bucketMinute = new long[EARNING_BUCKETS];
    private final long[] bucketEarned = new long[EARNING_BUCKETS];
    private long balance;
    private long totalEarned;
    private long totalSpent;
    private boolean membersCanSpend = true;

    /** Items and credits sold of one item type. */
    public record Sales(long count, long credits) {
        Sales plus(long moreCount, long moreCredits) {
            return new Sales(TeamRegistry.saturatedAdd(count, moreCount), TeamRegistry.saturatedAdd(credits, moreCredits));
        }
    }

    Team(UUID id, String name, long balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        Arrays.fill(bucketMinute, -1);
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

    /** Members in join order; the first one is the leader. */
    public Set<UUID> members() {
        return Collections.unmodifiableSet(members);
    }

    /** The leader may kick members, hand over leadership and decide whether members may spend credits. */
    public UUID leader() {
        return members.iterator().next();
    }

    public boolean isLeader(UUID player) {
        return !members.isEmpty() && leader().equals(player);
    }

    /** If false, only the leader can buy blueprints, upgrades and repairs. */
    public boolean membersCanSpend() {
        return membersCanSpend;
    }

    public boolean canSpend(UUID player) {
        return members.contains(player) && (membersCanSpend || isLeader(player));
    }

    /** How often each blueprint was built at a workbench (for the guide). */
    public Map<String, Long> built() {
        return Collections.unmodifiableMap(built);
    }

    /** Ids of quests whose reward was already collected. */
    public Set<String> claimedQuests() {
        return Collections.unmodifiableSet(claimedQuests);
    }

    public boolean isInvited(UUID player) {
        return invites.contains(player);
    }

    /** Ids of blueprints bought by this team (free starter blueprints are not listed). */
    public Set<String> unlocked() {
        return Collections.unmodifiableSet(unlocked);
    }

    public long totalEarned() {
        return totalEarned;
    }

    public long totalSpent() {
        return totalSpent;
    }

    public Map<String, Sales> sales() {
        return Collections.unmodifiableMap(sales);
    }

    /** Credits earned in the given number of minutes up to and including {@code currentMinute}. */
    public long earnedInLastMinutes(long currentMinute, int minutes) {
        long total = 0;
        for (int i = 0; i < EARNING_BUCKETS; i++) {
            if (bucketMinute[i] >= 0 && bucketMinute[i] > currentMinute - minutes && bucketMinute[i] <= currentMinute) {
                total += bucketEarned[i];
            }
        }
        return total;
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

    Set<String> mutableUnlocked() {
        return unlocked;
    }

    Map<String, Long> mutableBuilt() {
        return built;
    }

    Set<String> mutableClaimedQuests() {
        return claimedQuests;
    }

    void setMembersCanSpend(boolean membersCanSpend) {
        this.membersCanSpend = membersCanSpend;
    }

    /** Moves {@code player} to the front of the member list, making them leader. */
    void makeLeader(UUID player) {
        Set<UUID> rest = new LinkedHashSet<>(members);
        rest.remove(player);
        members.clear();
        members.add(player);
        members.addAll(rest);
    }

    void addSale(String item, long count, long credits, long minute) {
        sales.merge(item, new Sales(count, credits), (old, added) -> old.plus(added.count(), added.credits()));
        totalEarned = TeamRegistry.saturatedAdd(totalEarned, credits);
        int bucket = (int) Math.floorMod(minute, EARNING_BUCKETS);
        if (bucketMinute[bucket] != minute) {
            bucketMinute[bucket] = minute;
            bucketEarned[bucket] = 0;
        }
        bucketEarned[bucket] = TeamRegistry.saturatedAdd(bucketEarned[bucket], credits);
    }

    void addSpent(long amount) {
        totalSpent = TeamRegistry.saturatedAdd(totalSpent, amount);
    }

    void restoreStats(long totalEarned, long totalSpent, Map<String, Sales> sales) {
        this.totalEarned = totalEarned;
        this.totalSpent = totalSpent;
        this.sales.putAll(sales);
    }

    /** Takes over the knowledge and history of a team that was dissolved into this one. */
    void absorb(Team other) {
        unlocked.addAll(other.unlocked);
        claimedQuests.addAll(other.claimedQuests);
        other.built.forEach((id, count) -> built.merge(id, count, TeamRegistry::saturatedAdd));
        other.sales.forEach((item, stat) -> sales.merge(item, stat, (a, b) -> a.plus(b.count(), b.credits())));
        totalEarned = TeamRegistry.saturatedAdd(totalEarned, other.totalEarned);
        totalSpent = TeamRegistry.saturatedAdd(totalSpent, other.totalSpent);
    }
}
