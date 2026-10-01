package de.craftorio.datagen;

import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.List;
import java.util.Map;

/**
 * Names of the upgrade paths, upgrades and abilities of the towers, and short descriptions of the special ones. The English
 * names of the upgrades come from the tower definitions ({@code btd6} fields), so only German texts live here.
 */
final class TowerTexts {
    private TowerTexts() {
    }

    /** Per tower: three path names, then the 15 upgrade names in the order of the paths. */
    private static final Map<String, List<String>> GERMAN = Map.of(
            "crossbow_tower", List.of("Geschosse", "Tempo", "Fernsicht",
                    "Scharfe Bolzen", "Messerscharfe Bolzen", "Stachelschleuder", "Koloss", "Ultra-Koloss",
                    "Schnelle Schüsse", "Sehr schnelle Schüsse", "Dreifachschuss", "Fanclub", "Plasma-Fanclub",
                    "Weitschuss", "Adleraugen", "Armbrust", "Scharfschütze", "Armbrust-Meister"),
            "gun_turret", List.of("Durchschlag", "Spähtrupp", "Schnellfeuer",
                    "Vollmantelgeschoss", "Großkaliber", "Tödliche Präzision", "Behemoth verstümmeln", "Behemoth lähmen",
                    "Nachtsichtgerät", "Splitterschuss", "Querschläger", "Nachschubabwurf", "Elite-Schütze",
                    "Schnelles Feuer", "Noch schnelleres Feuer", "Halbautomatik", "Vollautomatik", "Elite-Verteidiger"),
            "flamethrower_turret", List.of("Hitze", "Reichweite", "Streuung",
                    "Schnelleres Schießen", "Noch schnelleres Schießen", "Heiße Schüsse", "Feuerring", "Inferno-Ring",
                    "Weite Dornen", "Sehr weite Dornen", "Klingenwerfer", "Klingensturm", "Super-Klingensturm",
                    "Mehr Dornen", "Noch mehr Dornen", "Dornensprüher", "Überdrehen", "Die Dornenzone"),
            "mortar_turret", List.of("Sprengkraft", "Feuerrate", "Streuung",
                    "Größere Bomben", "Schwere Bomben", "Heftiger Einschlag", "Erdbeben-Bomben", "Mega-Zerstörer",
                    "Schnelleres Nachladen", "Noch schnelleres Nachladen", "Behemoth-Zerstörer", "Behemoth-Attentäter", "Behemoth-Vernichter",
                    "Größere Reichweite", "Splitterbomben", "Clusterbomben", "Rekursive Cluster", "Bombenhagel"),
            "supply_depot", List.of("Produktion", "Bank", "Automatik",
                    "Mehr Ertrag", "Großer Ertrag", "Plantage", "Forschungsanlage", "Zentrale",
                    "Haltbare Ware", "Wertvolle Ware", "Sparbank", "Kredit", "Wirtschaftswunder",
                    "Selbstabholung", "Bergung", "Markt", "Großmarkt", "Börse"));

    private static final Map<String, String> GERMAN_ABILITIES = Map.ofEntries(
            Map.entry("fan_club", "Fanclub"), Map.entry("supply_drop", "Nachschubabwurf"), Map.entry("elite_defender", "Elite-Verteidiger"),
            Map.entry("blade_maelstrom", "Klingensturm"), Map.entry("moab_assassin", "Behemoth-Attentäter"),
            Map.entry("bank_withdraw", "Bank leeren"), Map.entry("imf_loan", "Kredit"), Map.entry("monkey_nomics", "Wirtschaftswunder"));

    private static final Map<String, String> ENGLISH_ABILITIES = Map.ofEntries(
            Map.entry("fan_club", "Fan Club"), Map.entry("supply_drop", "Supply Drop"), Map.entry("elite_defender", "Elite Defender"),
            Map.entry("blade_maelstrom", "Blade Maelstrom"), Map.entry("moab_assassin", "Behemoth Assassin"),
            Map.entry("bank_withdraw", "Empty bank"), Map.entry("imf_loan", "Loan"), Map.entry("monkey_nomics", "Economic Miracle"));

    static void german(LanguageProvider provider) {
        GERMAN.forEach((id, names) -> {
            for (int path = 0; path < 3; path++) {
                provider.add("craftorio.tower." + id + ".path." + (path + 1), names.get(path));
                for (int tier = 1; tier <= 5; tier++) {
                    provider.add("craftorio.tower." + id + "." + (path + 1) + "." + tier, names.get(3 + path * 5 + tier - 1));
                }
            }
        });
        GERMAN_ABILITIES.forEach((id, name) -> provider.add("craftorio.ability." + id, name));
        provider.add("craftorio.tower.ability.passive", "Passiv: %s");
        provider.add("craftorio.tower.depot.income", "%s ⛁ pro Runde");
        provider.add("craftorio.tower.depot.bank", " · Bank %s ⛁");
        provider.add("craftorio.tower.depot.debt", " · Schuld %s ⛁");
        provider.add("craftorio.tower.depot.basket_yes", " · Korb geliefert");
        provider.add("craftorio.tower.depot.basket_no", " · Korb fehlt");
    }

    static void english(LanguageProvider provider) {
        GERMAN.keySet().forEach(id -> {
            String[] paths = ENGLISH_PATHS.getOrDefault(id, new String[]{"Path 1", "Path 2", "Path 3"});
            for (int path = 0; path < 3; path++) {
                provider.add("craftorio.tower." + id + ".path." + (path + 1), paths[path]);
            }
        });
        ENGLISH_ABILITIES.forEach((id, name) -> provider.add("craftorio.ability." + id, name));
        provider.add("craftorio.tower.ability.passive", "Passive: %s");
        provider.add("craftorio.tower.depot.income", "%s ⛁ per round");
        provider.add("craftorio.tower.depot.bank", " · bank %s ⛁");
        provider.add("craftorio.tower.depot.debt", " · loan %s ⛁");
        provider.add("craftorio.tower.depot.basket_yes", " · basket delivered");
        provider.add("craftorio.tower.depot.basket_no", " · basket missing");
    }

    private static final Map<String, String[]> ENGLISH_PATHS = Map.of(
            "crossbow_tower", new String[]{"Projectiles", "Speed", "Sight"},
            "gun_turret", new String[]{"Penetration", "Recon", "Rate of fire"},
            "flamethrower_turret", new String[]{"Heat", "Reach", "Spread"},
            "mortar_turret", new String[]{"Blast", "Rate of fire", "Dispersal"},
            "supply_depot", new String[]{"Production", "Bank", "Automation"});
}
