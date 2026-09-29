package de.craftorio.team;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * All teams and their credit accounts. Free of Minecraft types so the rules can be unit tested;
 * {@link TeamData} persists it and syncs changes to clients.
 *
 * <p>Rules: every player is in exactly one team. A team that loses its last member is dissolved and its
 * balance, blueprints and statistics move with that player, so leaving a solo team to join friends never loses
 * anything. A player leaving a shared team keeps a copy of its blueprints but starts with no credits.
 */
public final class TeamRegistry {
    public static final int MAX_NAME_LENGTH = 24;
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}\\p{N} _-]{1," + MAX_NAME_LENGTH + "}");

    private final Map<UUID, Team> teams = new LinkedHashMap<>();
    private final Map<UUID, UUID> teamByPlayer = new HashMap<>();
    /** Dissolved teams and the team that took them over, so things owned by the old team keep an owner. */
    private final Map<UUID, UUID> mergedInto = new HashMap<>();
    private final LongSupplier startingCredits;
    private final LongSupplier gameTime;
    private Consumer<Team> changeListener = team -> { };
    private BiConsumer<UUID, UUID> mergeListener = (dissolved, into) -> { };

    public TeamRegistry(LongSupplier startingCredits) {
        this(startingCredits, () -> 0L);
    }

    /** @param gameTime current game time in ticks, used to bucket earnings per minute */
    public TeamRegistry(LongSupplier startingCredits, LongSupplier gameTime) {
        this.startingCredits = startingCredits;
        this.gameTime = gameTime;
    }

    public static long minuteOf(long gameTime) {
        return gameTime / 1200;
    }

    public long currentMinute() {
        return minuteOf(gameTime.getAsLong());
    }

    /** Called after every change to a team (balance, members, invites) and after a team is dissolved. */
    public void setChangeListener(Consumer<Team> changeListener) {
        this.changeListener = changeListener;
    }

    /** Called with (dissolved team, team that took it over) when a player's solo team merges into another. */
    public void setMergeListener(BiConsumer<UUID, UUID> mergeListener) {
        this.mergeListener = mergeListener;
    }

    public Collection<Team> teams() {
        return Collections.unmodifiableCollection(teams.values());
    }

    public Optional<Team> team(UUID teamId) {
        return Optional.ofNullable(teams.get(teamId));
    }

    public Optional<Team> teamOf(UUID player) {
        UUID teamId = teamByPlayer.get(player);
        return teamId == null ? Optional.empty() : team(teamId);
    }

    public Optional<Team> teamByName(String name) {
        String key = normalize(name);
        return teams.values().stream().filter(team -> normalize(team.name()).equals(key)).findFirst();
    }

    /** Returns the player's team, creating a solo team with the starting credits on first join. */
    public Team ensureTeam(UUID player, String playerName) {
        Optional<Team> existing = teamOf(player);
        if (existing.isPresent()) {
            return existing.get();
        }
        Team team = newTeam(uniqueName(playerName), startingCredits.getAsLong());
        addMember(team, player);
        return team;
    }

    public Team create(UUID player, String name) {
        String trimmed = validateName(name);
        if (teamByName(trimmed).isPresent()) {
            throw new TeamException("craftorio.team.error.name_taken", trimmed);
        }
        Team dissolved = removeMember(player);
        Team team = newTeam(trimmed, 0);
        takeOver(team, dissolved);
        addMember(team, player);
        return team;
    }

    public void invite(UUID inviter, UUID target) {
        Team team = teamOf(inviter).orElseThrow(() -> new TeamException("craftorio.team.error.no_team"));
        if (team.members().contains(target)) {
            throw new TeamException("craftorio.team.error.already_member");
        }
        team.mutableInvites().add(target);
        changeListener.accept(team);
    }

    public Team join(UUID player, String teamName) {
        Team team = teamByName(teamName).orElseThrow(() -> new TeamException("craftorio.team.error.unknown_team", teamName));
        if (team.members().contains(player)) {
            throw new TeamException("craftorio.team.error.already_member");
        }
        if (!team.isInvited(player)) {
            throw new TeamException("craftorio.team.error.not_invited", team.name());
        }
        Team dissolved = removeMember(player);
        team.mutableInvites().remove(player);
        takeOver(team, dissolved);
        addMember(team, player);
        return team;
    }

    /** Leaves the current team for a fresh solo team. The new team starts empty so leaving can't farm starting credits. */
    public Team leave(UUID player, String playerName) {
        Team current = teamOf(player).orElseThrow(() -> new TeamException("craftorio.team.error.no_team"));
        if (current.members().size() == 1) {
            throw new TeamException("craftorio.team.error.alone");
        }
        removeMember(player);
        Team team = newTeam(uniqueName(playerName), 0);
        team.mutableResearched().putAll(current.researchTimes());
        addMember(team, player);
        return team;
    }

    /** Leader only: removes a member, who continues in a fresh solo team with a copy of the blueprints and no credits. */
    public Team kick(UUID leader, UUID target, String targetName) {
        Team team = requireLeader(leader);
        if (leader.equals(target)) {
            throw new TeamException("craftorio.team.error.kick_self");
        }
        if (!team.members().contains(target)) {
            throw new TeamException("craftorio.team.error.not_member");
        }
        return leave(target, targetName);
    }

    /** Leader only: hands leadership to another member. */
    public void transferLeadership(UUID leader, UUID target) {
        Team team = requireLeader(leader);
        if (!team.members().contains(target)) {
            throw new TeamException("craftorio.team.error.not_member");
        }
        team.makeLeader(target);
        changeListener.accept(team);
    }

    /** Leader only: whether members other than the leader may spend the team's credits. */
    public void setMembersCanSpend(UUID leader, boolean allowed) {
        Team team = requireLeader(leader);
        team.setMembersCanSpend(allowed);
        changeListener.accept(team);
    }

    /**
     * Takes {@code amount} credits on behalf of a player: fails with a {@link TeamException} if the player may not
     * spend, returns false if the team cannot afford it.
     */
    public boolean spend(UUID player, long amount) {
        Team team = teamOf(player).orElseThrow(() -> new TeamException("craftorio.team.error.no_team"));
        if (!team.canSpend(player)) {
            throw new TeamException("craftorio.team.error.no_spend_permission");
        }
        return withdraw(team.id(), amount);
    }

    /** Counts blueprints built at a workbench. */
    public void recordBuild(UUID teamId, String blueprint, long times) {
        Team team = requireTeam(teamId);
        team.mutableBuilt().merge(blueprint, times, TeamRegistry::saturatedAdd);
        changeListener.accept(team);
    }

    /** Marks a quest reward as collected and pays it; false if it was already collected. */
    public boolean claimQuest(UUID teamId, String quest, long reward) {
        Team team = requireTeam(teamId);
        if (!team.mutableClaimedQuests().add(quest)) {
            return false;
        }
        team.setBalance(saturatedAdd(team.balance(), reward));
        changeListener.accept(team);
        return true;
    }

    public void deposit(UUID teamId, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        Team team = requireTeam(teamId);
        team.setBalance(saturatedAdd(team.balance(), amount));
        changeListener.accept(team);
    }

    /** Takes {@code amount} credits if the team can afford it. */
    public boolean withdraw(UUID teamId, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        Team team = requireTeam(teamId);
        if (team.balance() < amount) {
            return false;
        }
        team.setBalance(team.balance() - amount);
        team.addSpent(amount);
        changeListener.accept(team);
        return true;
    }

    /** Credits a sale to the team and records it in the statistics. */
    public void recordSale(UUID teamId, String item, long count, long credits) {
        if (credits < 0 || count < 0) {
            throw new IllegalArgumentException("negative sale: " + count + " x " + item + " for " + credits);
        }
        Team team = requireTeam(teamId);
        team.setBalance(saturatedAdd(team.balance(), credits));
        team.addSale(item, count, credits, currentMinute());
        changeListener.accept(team);
    }

    public boolean hasResearched(UUID teamId, String research) {
        return team(teamId).map(team -> team.researched().contains(research)).orElse(false);
    }

    /** Finishes a research at once (tests, migration); also removes it from the queue. */
    public void grantResearch(UUID teamId, String research) {
        Team team = requireTeam(teamId);
        complete(team, research);
        changeListener.accept(team);
    }

    /** Puts a research at the end of the queue; false if it is finished or already queued. */
    public boolean enqueueResearch(UUID teamId, String research) {
        Team team = requireTeam(teamId);
        if (team.researched().contains(research) || team.researchQueue().contains(research)) {
            return false;
        }
        team.mutableResearchQueue().add(research);
        changeListener.accept(team);
        return true;
    }

    public boolean dequeueResearch(UUID teamId, String research) {
        Team team = requireTeam(teamId);
        if (!team.mutableResearchQueue().remove(research)) {
            return false;
        }
        changeListener.accept(team);
        return true;
    }

    /** Remembers that the key items of a research were handed in, so re-queueing it costs nothing more. */
    public void markKeyItemsPaid(UUID teamId, String research) {
        Team team = requireTeam(teamId);
        if (team.mutableResearchKeys().add(research)) {
            changeListener.accept(team);
        }
    }

    /**
     * Adds finished units to a research the team is working on.
     *
     * @param needed units the research costs in total
     * @return true if this finished the research
     */
    public boolean addResearchUnits(UUID teamId, String research, long units, long needed) {
        Team team = requireTeam(teamId);
        if (team.researched().contains(research) || units <= 0) {
            return false;
        }
        long total = saturatedAdd(team.researchProgress(research), units);
        boolean done = total >= needed;
        if (done) {
            complete(team, research);
        } else {
            team.mutableResearchProgress().put(research, total);
        }
        changeListener.accept(team);
        return done;
    }

    private void complete(Team team, String research) {
        team.mutableResearched().putIfAbsent(research, gameTime.getAsLong());
        team.mutableResearchQueue().remove(research);
        team.mutableResearchProgress().remove(research);
    }

    public void setBalance(UUID teamId, long balance) {
        Team team = requireTeam(teamId);
        team.setBalance(Math.max(0, balance));
        changeListener.accept(team);
    }

    /** Re-adds a team loaded from disk without notifying the change listener. */
    public Team restore(UUID id, String name, long balance, Collection<UUID> members) {
        return restore(id, name, balance, members, Map.of(), 0, 0, Map.of());
    }

    public Team restore(UUID id, String name, long balance, Collection<UUID> members, Map<String, Long> researched,
                        long totalEarned, long totalSpent, Map<String, Team.Sales> sales) {
        Team team = new Team(id, name, balance);
        teams.put(id, team);
        for (UUID member : members) {
            team.mutableMembers().add(member);
            teamByPlayer.put(member, id);
        }
        team.mutableResearched().putAll(researched);
        team.restoreStats(totalEarned, totalSpent, sales);
        return team;
    }

    /** Restores the research queue, progress and paid key items of a team loaded from disk. */
    public void restoreResearch(UUID id, Collection<String> queue, Map<String, Long> progress, Collection<String> keysPaid) {
        Team team = requireTeam(id);
        team.mutableResearchQueue().addAll(queue);
        team.mutableResearchProgress().putAll(progress);
        team.mutableResearchKeys().addAll(keysPaid);
    }

    /** Restores the settings and quest progress of a team loaded from disk. */
    public void restoreSettings(UUID id, boolean membersCanSpend, Collection<String> claimedQuests, Map<String, Long> built) {
        Team team = requireTeam(id);
        team.mutableBuilt().putAll(built);
        team.setMembersCanSpend(membersCanSpend);
        team.mutableClaimedQuests().addAll(claimedQuests);
    }

    /**
     * The team that now stands for {@code teamId}: itself while it exists, the team it was merged into after it was
     * dissolved by joining or founding a team, or empty if it is gone for good.
     */
    public Optional<Team> resolve(UUID teamId) {
        UUID current = teamId;
        for (int hops = 0; current != null && hops <= mergedInto.size(); hops++) {
            Team team = teams.get(current);
            if (team != null) {
                return Optional.of(team);
            }
            current = mergedInto.get(current);
        }
        return Optional.empty();
    }

    public Map<UUID, UUID> mergedTeams() {
        return Collections.unmodifiableMap(mergedInto);
    }

    public void restoreMerge(UUID dissolved, UUID into) {
        mergedInto.put(dissolved, into);
    }

    private void takeOver(Team team, Team dissolved) {
        if (dissolved != null) {
            mergedInto.put(dissolved.id(), team.id());
            mergeListener.accept(dissolved.id(), team.id());
            team.setBalance(saturatedAdd(team.balance(), dissolved.balance()));
            team.absorb(dissolved);
            dissolved.setBalance(0);
        }
    }

    private Team newTeam(String name, long balance) {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (teams.containsKey(id));
        Team team = new Team(id, name, balance);
        teams.put(id, team);
        return team;
    }

    private void addMember(Team team, UUID player) {
        team.mutableMembers().add(player);
        teamByPlayer.put(player, team.id());
        changeListener.accept(team);
    }

    /** Removes the player from their team; returns the team if that dissolved it, otherwise null. */
    private Team removeMember(UUID player) {
        UUID teamId = teamByPlayer.remove(player);
        if (teamId == null) {
            return null;
        }
        Team team = teams.get(teamId);
        team.mutableMembers().remove(player);
        changeListener.accept(team);
        if (!team.members().isEmpty()) {
            return null;
        }
        teams.remove(teamId);
        return team;
    }

    private Team requireLeader(UUID player) {
        Team team = teamOf(player).orElseThrow(() -> new TeamException("craftorio.team.error.no_team"));
        if (!team.isLeader(player)) {
            throw new TeamException("craftorio.team.error.not_leader");
        }
        return team;
    }

    private Team requireTeam(UUID teamId) {
        Team team = teams.get(teamId);
        if (team == null) {
            throw new IllegalArgumentException("unknown team " + teamId);
        }
        return team;
    }

    private String uniqueName(String base) {
        String cleaned = base.replaceAll("[^\\p{L}\\p{N} _-]", "");
        if (cleaned.isEmpty()) {
            cleaned = "Team";
        }
        cleaned = cleaned.substring(0, Math.min(cleaned.length(), MAX_NAME_LENGTH - 4));
        String name = cleaned;
        for (int i = 2; teamByName(name).isPresent(); i++) {
            name = cleaned + " " + i;
        }
        return name;
    }

    static String validateName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            throw new TeamException("craftorio.team.error.invalid_name", MAX_NAME_LENGTH);
        }
        return trimmed;
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    static long saturatedAdd(long a, long b) {
        long sum = a + b;
        // Overflow happens only if both operands have the sign opposite to the result.
        return ((a ^ sum) & (b ^ sum)) < 0 ? Long.MAX_VALUE : sum;
    }
}
