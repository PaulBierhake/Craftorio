# Craftorio

Factorio-artige Automatisierung, eine Credits-Wirtschaft und Tower Defense für Minecraft.
Das vollständige Spielkonzept steht in [docs/KONZEPT.md](docs/KONZEPT.md).

- Minecraft **1.21.1**, **NeoForge**, Java **21**
- Build-System: Gradle mit [ModDevGradle](https://github.com/neoforged/ModDevGradle)

## Entwicklung

```bash
./gradlew runData      # Datengeneratoren: Modelle, Blockstates, Tags, Übersetzungen -> src/generated/resources
./gradlew runClient    # Minecraft-Client mit der Mod starten
./gradlew runClient -PquickPlay=<Welt>   # direkt in eine Einzelspielerwelt springen
./gradlew runServer    # Dedizierten Server starten
./gradlew build        # Mod-JAR bauen und Unit-Tests ausführen -> build/libs/
./gradlew runGameTestServer   # GameTests (Tests im laufenden Spiel) auf einem Server ohne Grafik
```

`runData` muss einmal nach dem Klonen und nach jeder Änderung an den Datengeneratoren
(`de.craftorio.datagen`) laufen. Die generierten Dateien werden nicht eingecheckt.

IntelliJ IDEA: Projekt als Gradle-Projekt öffnen; die Run-Konfigurationen werden beim Sync erzeugt.

## Aktueller Spielstand

### M1 – Wirtschaftskern

- Jeder Spieler gehört zu genau einem **Team**; beim ersten Betreten entsteht ein Solo-Team.
  Das Team teilt ein **Credits-Konto** (¢), das oben links im HUD angezeigt wird.
- **Handelsposten**: nimmt Items per Rechtsklick, Trichter oder (später) Förderband an und
  schreibt ihren Verkaufswert dem Team gut. Items ohne Preis werden abgelehnt.
- **Preise** kommen aus der Data Map `data/craftorio/data_maps/item/sell_prices.json`
  (per Datapack änderbar, `/reload`) und stehen im Item-Tooltip.
- Befehle:

| Befehl | Wirkung |
|---|---|
| `/craftorio credits` | Kontostand des eigenen Teams |
| `/craftorio credits add\|set <Spieler> <Betrag>` | Konto ändern (Operator) |
| `/craftorio team` / `team info` | Team, Kontostand, Mitglieder |
| `/craftorio team list` | Alle Teams |
| `/craftorio team create <Name>` | Neues Team gründen |
| `/craftorio team invite <Spieler>` | Spieler einladen |
| `/craftorio team join <Name>` | Einladung annehmen |
| `/craftorio team leave` | Team verlassen (neues, leeres Solo-Team) |

Verlässt das letzte Mitglied ein Team, wird es aufgelöst und sein Guthaben wandert mit.

![Handelsposten](docs/screenshots/m1-handelsposten.png)

### M2 – Oberfläche

- **Erzfelder** (Eisen, Kupfer, Kohle) entstehen als Flecken an der Oberfläche, weiter weg vom Ursprung größer.
  Je ein kleines Start-Feld liegt garantiert nahe dem Spawn. Feldblöcke sind **unerschöpflich**; von Hand abgebaut
  geben sie ein Item und bleiben stehen.
- **Brenner-Bohrer**: auf ein Feld setzen, mit Brennstoff (Kohle, Holz …) füttern. Er baut die 3×3 Blöcke darunter
  ab (0,03 Items/s pro Feldblock, voll belegt 0,27/s) und gibt die Ausbeute nach vorne ab. Abbauflächen zweier Bohrer
  dürfen sich **nicht überlappen** – ein voll bebautes Feld liefert also einen festen Maximaldurchsatz.
  Rechtsklick mit leerer Hand zeigt den Status und nimmt gepufferte Ausbeute heraus.
- **Förderband** mit zwei Spuren (1,875 Blöcke/s, 4 Items pro Spur und Block). Bänder laufen durch Kurven,
  laden seitlich auf die nahe Spur, nehmen fallengelassene Items auf, tragen Spieler mit und liefern am Ende in
  alles mit Inventar (Kisten, Handelsposten, Bohrer-Brennstoff …).
- **Greifarm**: bewegt 1 Item/s vom Block dahinter in den Block davor; legt auf Bänder auf die ferne Spur.
- Alle Blöcke funktionieren auch mit Vanilla-Trichtern und -Kisten.

![Anlage](docs/screenshots/m2-anlage.png)

## Projektstruktur

```
src/main/java/de/craftorio/
├── Craftorio.java          Mod-Einstiegspunkt
├── CraftorioConfig.java    Server-Konfiguration
├── registry/               Blöcke, Items, Block-Entities, Creative-Tab
├── team/                   Teams & Konto (TeamRegistry ist reine Logik mit Unit-Tests)
├── economy/                Preise (Data Map), Verkauf, Handelsposten
├── world/                  Erzfelder, Weltgenerierung, Start-Felder
├── machine/                Bohrer (Abbaufläche, Produktionsrate)
├── logistics/              Förderband (BeltLane = reine Spur-Logik mit Unit-Tests), Greifarm
├── command/                /craftorio-Befehle
├── network/                Server→Client-Sync
├── client/                 HUD, Tooltips
├── gametest/               Tests im laufenden Spiel
└── datagen/                Datengeneratoren
src/test/java/              Unit-Tests (JUnit)
src/main/templates/         neoforge.mods.toml (wird aus gradle.properties befüllt)
src/main/resources/         Handgemachte Assets (Texturen)
```
