package de.craftorio.guide;

import de.craftorio.defense.Challenge;
import de.craftorio.defense.Difficulty;
import de.craftorio.defense.Knowledge;
import de.craftorio.defense.sim.DamageKind;
import de.craftorio.defense.sim.DepotEconomy;
import de.craftorio.defense.sim.EnemyDef;
import de.craftorio.defense.sim.EnemyDefs;
import de.craftorio.defense.sim.RoundDef;
import de.craftorio.defense.sim.RoundDefs;
import de.craftorio.defense.sim.RoundRules;
import de.craftorio.defense.sim.TowerDef;
import de.craftorio.defense.sim.TowerDefs;
import de.craftorio.economy.Credits;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The handbook's reference part: pages about the economy, the difficulties, the challenges, arena knowledge, every enemy
 * (with what it is immune to) and every tower (with its three upgrade paths). Built from the game data, so it never
 * disagrees with the rules.
 */
public final class Lexicon {
    /** Lines a page may have (the book's page holds about this many lines of text). */
    public static final int MAX_LINES = 19;
    private static final int ENEMIES_PER_PAGE = 4;

    public record Page(Component title, List<Component> lines) {
    }

    private Lexicon() {
    }

    public static List<Page> pages() {
        List<Page> pages = new ArrayList<>();
        pages.add(economy());
        pages.add(difficulties());
        pages.add(challenges());
        pages.add(knowledge());
        List<EnemyDef> enemies = EnemyDefs.all();
        for (int i = 0; i < enemies.size(); i += ENEMIES_PER_PAGE) {
            pages.add(enemies(enemies.subList(i, Math.min(enemies.size(), i + ENEMIES_PER_PAGE))));
        }
        for (String id : TowerDefs.IDS) {
            pages.add(tower(TowerDefs.get(id), id));
        }
        return pages;
    }

    private static Component t(String key, Object... args) {
        return Component.translatable("craftorio.lexicon." + key, args);
    }

    private static Page economy() {
        List<Component> lines = new ArrayList<>();
        lines.add(t("economy.coins", RoundRules.START_COINS));
        lines.add(t("economy.round_bonus"));
        lines.add(t("economy.income"));
        lines.add(t("economy.sell", Math.round(RoundRules.SELL_SHARE * 100)));
        lines.add(t("economy.war_chest"));
        lines.add(t("economy.depot", Math.round(DepotEconomy.BASE_INCOME), Math.round(DepotEconomy.BASKET_BONUS * 100)));
        lines.add(t("economy.lives"));
        lines.add(t("economy.rounds"));
        return new Page(t("economy.title"), lines);
    }

    private static Page difficulties() {
        List<Component> lines = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            lines.add(t("difficulty", Component.translatable("craftorio.td.difficulty." + difficulty.name().toLowerCase(Locale.ROOT)),
                    difficulty.lives(), percent(difficulty.priceFactor()), percent(difficulty.speedFactor()), percent(difficulty.rewardFactor())));
        }
        lines.add(t("difficulty.note"));
        return new Page(t("difficulty.title"), lines);
    }

    private static Page challenges() {
        List<Component> lines = new ArrayList<>();
        for (Challenge challenge : Challenge.values()) {
            if (challenge != Challenge.NONE) {
                lines.add(Component.translatable("craftorio.td.challenge." + challenge.name().toLowerCase(Locale.ROOT)));
            }
        }
        return new Page(t("challenges.title"), lines);
    }

    private static Page knowledge() {
        List<Component> lines = new ArrayList<>();
        for (String id : Knowledge.ALL) {
            lines.add(Component.translatable("craftorio.research." + id));
        }
        lines.add(t("knowledge.note"));
        return new Page(t("knowledge.title"), lines);
    }

    private static Page enemies(List<EnemyDef> defs) {
        List<Component> lines = new ArrayList<>();
        for (EnemyDef def : defs) {
            lines.add(Component.translatable("craftorio.enemy." + def.id()));
            lines.add(t("enemy.stats", Credits.formatNumber(Math.round(def.hp())), String.format(Locale.ROOT, "%.1f", def.speed() / 10.0),
                    firstRound(def.id()), def.boss() ? Component.translatable("craftorio.lexicon.enemy.boss") : Component.empty()));
            lines.add(t("enemy.immune", def.immune().isEmpty() ? Component.translatable("craftorio.lexicon.none") : kinds(def)));
            if (!def.children().isEmpty()) {
                lines.add(t("enemy.children", def.children().size(), Component.translatable("craftorio.enemy." + def.children().get(0))));
            }
        }
        return new Page(t("enemies.title"), lines);
    }

    private static Component kinds(EnemyDef def) {
        List<String> names = new ArrayList<>();
        for (DamageKind kind : DamageKind.values()) {
            if (def.immune().contains(kind)) {
                names.add(Component.translatable("craftorio.damage." + kind.id()).getString());
            }
        }
        return Component.literal(String.join(", ", names));
    }

    /** The first round of the standard list the enemy shows up in, or 0. */
    static int firstRound(String enemy) {
        for (int number = 1; number <= RoundDefs.count(); number++) {
            RoundDef round = RoundDefs.get(number);
            if (round.groups().stream().anyMatch(group -> group.enemy().equals(enemy))) {
                return number;
            }
        }
        return 0;
    }

    private static Page tower(TowerDef def, String id) {
        List<Component> lines = new ArrayList<>();
        Component range = def.range() < 0 ? Component.translatable("craftorio.tower.range.unlimited")
                : Component.translatable("craftorio.tower.range", String.format(Locale.ROOT, "%.1f", def.range()));
        lines.add(t("tower.stats", Credits.formatNumber(def.cost()), range, Component.translatable("craftorio.lexicon.supply." + def.supply())));
        for (int path = 0; path < TowerDef.PATHS; path++) {
            lines.add(Component.translatableWithFallback("craftorio.tower." + id + ".path." + (path + 1), "Path " + (path + 1))
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            StringBuilder tiers = new StringBuilder();
            for (int tier = 1; tier <= TowerDef.TIERS; tier++) {
                TowerDef.Upgrade upgrade = def.upgrade(path, tier);
                if (tier > 1) {
                    tiers.append(" · ");
                }
                tiers.append(tier).append(' ').append(Component.translatableWithFallback("craftorio.tower." + id + "." + (path + 1) + "." + tier,
                        upgrade.btd6()).getString()).append(' ').append(Credits.formatNumber(upgrade.cost())).append(" ⛁");
            }
            lines.add(Component.literal(tiers.toString()));
        }
        lines.add(t("tower.rule"));
        return new Page(Component.translatable("block.craftorio." + id), lines);
    }

    private static String percent(double factor) {
        return Math.round(factor * 100) + " %";
    }
}
