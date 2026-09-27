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

### M3 – Verarbeitung & Energie

- **Kohle-Generator**: verbrennt Brennstoff zu Strom (60 FE/t, Puffer 20.000 FE) – nur solange Platz im Puffer ist.
- **Strommast**: verbindet sich automatisch mit Masten im Umkreis von 8 Blöcken (sichtbare Kupferkabel) und
  versorgt alle Maschinen und Generatoren im Umkreis von 2 Blöcken. Rechtsklick zeigt die Netzauslastung.
  Bei Strommangel wird die Energie anteilig verteilt – alle Maschinen werden gleich langsamer.
  Das Netz arbeitet mit Forge Energy (FE) und versorgt auch FE-Maschinen anderer Mods.
- **Elektro-Schmelzofen** (Vanilla-Schmelzrezepte, 80 Ticks, 20 FE/t), **Presse** (15 FE/t) und
  **Montagemaschine** (25 FE/t, Rezept per Pfeiltasten in der GUI wählen; jeder Eingangsslot nimmt genau
  seine Zutat an). Alle Maschinen haben eine GUI und arbeiten mit Bändern und Greifarmen zusammen.
- **Zwischenprodukte** mit Wertschöpfung (Preise ×10 skaliert):

| Kette | Preis |
|---|---|
| Rohes Eisen → Eisenbarren → Eisenplatte | 10 → 16 → 22 ¢ |
| Kupferbarren → 2 Kupferkabel | 16 → 2 × 12 ¢ |
| 2 Eisenplatten → Zahnrad | 44 → 60 ¢ |
| 3 Kabel + 1 Platte → Schaltkreis | 58 → 85 ¢ |
| 2 Zahnräder + 1 Platte + 2 Kabel → Motor | 166 → 240 ¢ |

- Maschinenrezepte sind Datapack-JSON (`craftorio:pressing`, `craftorio:assembling`).

![Stromnetz](docs/screenshots/m3-stromnetz.png)
![Montagemaschine](docs/screenshots/m3-montagemaschine.png)

### M4 – Terminal, Baupläne & Werkbänke

Craftorio-Maschinen entstehen in zwei Schritten: **Bauplan im Terminal freischalten** (Credits, ab Stufe 2 plus
Schlüsselmaterial) und **an der Werkbank aus Rohstoffen bauen**. Vanilla-Rezepte gibt es nur noch für
**Konstruktionswerkbank** und **Terminal**.

- **Terminal**: Bauplan-Baum mit Voraussetzungen, Preisen und Materiallisten (Tooltip); Tab **Statistik** mit
  Einnahmen, Ausgaben, Einnahmen der letzten 10 Minuten und den meistverkauften Waren.
- **Werkbänke**: Konstruktionswerkbank (Stufe 1) → Montagewerkbank (2) → Präzisionswerkbank (3). Aufgerüstet wird
  am Platz per Rechtsklick mit dem **Aufrüstsatz**. Jede Werkbank baut alle Baupläne ihrer Stufe und darunter aus dem
  Spielerinventar (Klick = 1, Shift-Klick = 10); fehlendes Material ist rot markiert.
- **Baupläne sind Team-Wissen**: Beim Beitritt zu einem Team wandern sie mit, beim Austritt behält man eine Kopie.
- **Start-Baupläne** (gratis): Handelsposten, Brenner-Bohrer, Förderband, Greifarm.
- **Schlüsselmaterialien** (Bohrkern, Resonanzkristall, Tiefenkern, Sternenerz-Splitter) kommen ab M5 aus der
  Tower Defense; bis dahin per `/give`.
- Baupläne sind Datapack-JSON: `data/<namespace>/craftorio/blueprint/*.json`
  (`result`, `ingredients`, `tier`, `cost`, `unlock_items`, `requires`, `order`).

![Terminal](docs/screenshots/m4-terminal.png)
![Werkbank](docs/screenshots/m4-werkbank.png)

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
├── energy/                 Generator, Strommast, Stromnetz (Verteilung als reine Logik mit Unit-Tests)
├── recipe/                 Maschinenrezepte (Presse, Montage)
├── blueprint/              Baupläne, Freischalt-Regeln, Terminal, Werkbänke
├── menu/                   Container-Menüs der Maschinen
├── command/                /craftorio-Befehle
├── network/                Server→Client-Sync
├── client/                 HUD, Tooltips
├── gametest/               Tests im laufenden Spiel
└── datagen/                Datengeneratoren
src/test/java/              Unit-Tests (JUnit)
src/main/templates/         neoforge.mods.toml (wird aus gradle.properties befüllt)
src/main/resources/         Handgemachte Assets (Texturen)
```
