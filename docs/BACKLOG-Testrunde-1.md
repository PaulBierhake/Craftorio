# Craftorio – Backlog aus Testrunde 1

Stand: Commit `bf2549f` auf `claude/keen-franklin-naz527`. Das Dokument ordnet die Testnotizen, nennt die im Code
gefundene Ursache und beschreibt pro Punkt eine umsetzbare Lösung mit Akzeptanzkriterien. Es ist als Arbeitsauftrag
für eine KI-Coding-Session (z. B. Sonnet) gedacht.

> **Update (Runde 3):** Die Progression wird auf Factorio umgestellt, siehe `docs/FACTORIO-UMBAU.md`. Dadurch
> gehen **A1, A2, A3, B1, B2 und C1** in den Umbaupaketen U2–U7/U9 auf und werden hier nicht separat umgesetzt.
> Stand nach den Umbaupaketen U0–U10: alles erledigt bis auf **B3** (Lager-Depot). B3 ist zurückgestellt, weil es in Factorio kein Zentraldepot gibt; falls es später gewünscht ist, gilt der Entwurf unten weiter.
> Entscheidungen aus Abschnitt 4 sind getroffen: E-1 lokal, E-2 Oberwelt (Variante a), E-3 ja, E-4 gemischt, E-5 wie empfohlen.

---

## 0. Arbeitsweise (bitte zuerst lesen)

**Projekt:** Minecraft 1.21.1, NeoForge 21.1.252, ModDevGradle, Java 21, Paket `de.craftorio`, Mod-ID `craftorio`.
Konzept: `docs/KONZEPT.md`, Feature-Übersicht: `README.md`.

**Befehle**

| Zweck | Befehl |
|---|---|
| Generierte Ressourcen (Sprachen, Modelle, Loot, Rezepte, Baupläne, Preise) | `./gradlew runData` – Ausgabe ist gitignored, nach jeder Datagen-Änderung ausführen |
| Kompilieren + Unit-Tests | `./gradlew build` (Unit-Tests in `src/test`) |
| GameTests | `./gradlew runGameTestServer` → Log endet mit „All N required tests passed" |
| Client (manuell) | `./gradlew runClient` (optional `-Pcompat` für EMI + Jade) |

**Konventionen**
- Texte immer **Englisch und Deutsch** in `datagen/ModLanguageProvider.java` (beide Blöcke).
- Baupläne in `datagen/ModBlueprints.java`, Verkaufspreise in `datagen/ModSellPriceProvider.java`, Leitfaden in
  `quest/Quests.java` (+ Texte `craftorio.quest.<id>` und `.hint`).
- Reine Logik (ohne Minecraft-Klassen) in eigene Klassen mit Unit-Tests auslagern, z. B. `BeltLane`,
  `PowerDistribution`, `ArenaLayout`, `PathTracer`, `Quest`.
- Verhalten im Spiel mit GameTests absichern (`gametest/*GameTests.java` bzw. `*GameTests.java` im Paket).
  Mock-Spieler (`helper.makeMockServerPlayerInLevel()`) gelten immer als Kreativ; für Survival-Verhalten
  `FakePlayerFactory.getMinecraft(level)` nehmen. Die GameTest-Welt ist persistent: Teamnamen eindeutig halten,
  Zähler als Differenz prüfen.
- Texturen 16×16 PNG unter `assets/craftorio/textures/…`; es gibt keine Vorlagen, sie werden per Skript (Python/PIL) gezeichnet.
- GUIs: `menu/MachineMenuBase` + `client/MachineScreenBase` (Hintergrund `textures/gui/machine.png`, Slot-Rahmen,
  Energiebalken). Beispiele: `DrillMenu`/`DrillScreen`, `TradingPostMenu`/`TradingPostScreen`, `GeneratorMenu`.
  Menüs in `registry/ModMenus.java`, Screens in `client/ClientEvents.java` registrieren.
- Das Handbuch (`client/GuideScreen.java`) erklärt jeden Leitfaden-Schritt; bei neuen Maschinen und Schritten mitpflegen.
- README-Abschnitt passend ergänzen, Screenshots unter `docs/screenshots/`.
- Commit-Nachrichten auf Englisch, ein Commit pro Aufgabenpaket (siehe Abschnitt 3).

**Definition of Done je Aufgabe:** kompiliert, `runData` ausgeführt, Unit-Tests und GameTests grün, neue Logik hat
Tests, Texte in EN und DE vorhanden, README/Handbuch aktualisiert, Verhalten wenn möglich im Client geprüft.

---

## 1. Übersicht

Prio: **P0** blockiert das Spiel · **P1** stört deutlich · **P2** Ausbau/Komfort. Aufwand: S < ½ Tag, M ≈ 1 Tag, L > 1 Tag.

| ID | Thema | Bereich | Prio | Aufwand | Hängt ab von | Stand |
|---|---|---|---|---|---|---|
| A1 | Materialkette nach Factorio-Vorbild (nichts Unbeschaffbares) | Progression | **P0** | L | – | ✅ Umbau U2–U5 |
| A2 | Weniger Geld durch Belohnungen, Preise neu ausbalancieren | Progression | P1 | M | A1 | ✅ U9 (Items statt Credits, Test) |
| A3 | Terminal an der Konstruktionswerkbank bauen | Progression | P1 | S | A1 | ✅ U3 |
| A4 | Beschreibungen zu Maschinen/Items in der Werkbank | UX | P1 | M | – | ✅ |
| B1 | Splitter, Merger, Filter | Logistik | P1 | L | – | ✅ U4 |
| B2 | Steigende und fallende Bänder | Logistik | P2 | M | – | ✅ U4 |
| B3 | Lager-Depot mit Einlass/Auslass und großem Inventar | Logistik | P1 | L | Entscheidung E-1 | ⏸ zurückgestellt: Factorio kennt kein Zentraldepot (nur Kisten); der Aufzug verbindet die Ebenen |
| C1 | Akkus als Strompuffer | Energie | P1 | M | – | ✅ U7 (Akku) |
| C2 | Strom und Munition für die Arena verständlich machen | Arena/UX | P1 | S | – | ✅ U0 |
| D1 | Wellen in der Arena starten | Arena | P1 | M | – | ✅ U0 |
| D2 | Pfadstab: keine Abzweigungen setzbar | Arena | P1 | S | – | ✅ U0 |
| D3 | Wasser-Karten: es muss immer ein erkennbarer Weg existieren | Arena | **P0** | M | – | ✅ U0 |
| D4 | Turmdepot als entnehmbares Inventar statt Auswerfen | Arena | P1 | M | – | ✅ U0 |
| D5 | Türme aus dem Depot stapeln nicht | Arena | P1 | S | – | ✅ U0 |
| D6 | Blumen und Gras droppen beim Kartenneubau | Arena | P1 | S | – | ✅ U0 |
| D7 | Arena-Karten kreativer und „voller" | Arena/Inhalt | P2 | L | D6 | ✅ |
| E1 | Höhleneingang in flachen Welten / Y-Höhe | Höhlen | P1 | M–L | Entscheidung E-2 | ✅ (a) Meldung und Warnung; Variante (b) nicht nötig |
| E2 | Höhlen ausgestalten und dekorieren | Höhlen/Inhalt | P2 | M | – | ✅ |
| E3 | GUI für den Höhleneingang (Baustelle) | Höhlen/UX | P1 | M | – | ✅ |

---

## 2. Aufgaben im Detail

### A – Progression und Wirtschaft

#### A1 · Materialkette nach Factorio-Vorbild (P0)

**Notiz:** „Materialien aus der Progression müssen aufeinander aufbauen … vielleicht einfach die Progression aus der
Factorio-Questline kopieren."

**Befund (Code):**
- An der Oberfläche gibt es nur **Eisen-, Kupfer- und Kohlefelder** (`worldgen/configured_feature/*`,
  `world/StarterFields.java`), dazu Holz, Stein, Sand und Lehm aus Vanilla.
- **Gold** gibt es nur als Höhlen-Erzfeld (`world/cave/CaveCarver.java`), und die Höhlenschichten ersetzen den
  Vanilla-Untergrund, also auch Vanilla-Gold.
- **Redstone** gibt es gar nicht als Feld.
- Trotzdem brauchen Tier-1-Baupläne in `datagen/ModBlueprints.java` diese Materialien:

  | Bauplan | Problematisches Material |
  |---|---|
  | Handelsposten | 1 Gold (nur aus dem Starter-Kit) |
  | Greifarm | Redstone |
  | Kohle-Generator | 2 Redstone |
  | Elektro-Schmelzofen | 2 Redstone, Ziegel |
  | Presse | Kolben (braucht Redstone) |
  | Arena-Tor | 2 Gold, 4 Redstone |
  | Armbrustturm | 2 Redstone |
  | Arena-Einspeiser | 4 Redstone |
  | Geschützturm | 4 Redstone |
  | Tesla-Turm | 4 Gold |
  | Terminal (Vanilla-Rezept) | Redstone |
- **Zirkelschluss in Tier 2:**
  - Der **Assembler** braucht 2 Gold, das es nur in Höhlen gibt.
  - Der **Höhleneingang** braucht Motoren (`CaveEntranceBlockEntity.requirements()`), und Motoren entstehen im Assembler.
  - Ohne Vanilla-Gold kommt man also nie in die Höhlen.
  - Auch das **Arena-Tor** braucht Gold und Redstone. Ohne Tower Defense gibt es keinen Drill Core (Freischalt-Item für
    `workbench_upgrade_2`), also keine Werkbank Stufe 2 und keinen Assembler.
  - Motor, Schaltkreis und Zahnrad selbst sind unkritisch: nur Eisen und Kupfer (`ModRecipeProvider.machineRecipes`).

**Ziel:** Jede Stufe braucht nur Materialien, die mit den bis dahin verfügbaren Maschinen und Feldern herstellbar
sind. Das Vorbild ist Factorio: Platten → Zahnrad/Kabel → Elektronischer Schaltkreis. Handarbeit an der Werkbank
geht immer, Maschinen machen es schneller und billiger.

**Lösung:**
1. **Zwischenprodukte an der Werkbank (Tier 1, kostenlos) zusätzlich zu Presse/Assembler:**
   - Eisenzahnrad: 2 Eisenbarren → 1 (Factorio: 2 Platten)
   - Kupferkabel: 1 Kupferbarren → 2
   - Schaltkreis (`CIRCUIT`): 1 Eisenbarren + 3 Kupferkabel → 1 (Factorio „Electronic circuit")

   Die Presse bzw. der Assembler stellt dasselbe billiger her (mehr Ertrag pro Material) und automatisiert es. Die
   Verkaufspreise der Zwischenprodukte nicht ändern, damit die Handarbeit keinen Wertgewinn gegenüber der Maschine
   bringt. Das `BalanceGameTests`-Kriterium gilt nur für Maschinenrezepte.
2. **Redstone und Gold aus allen Tier-1- und Tier-2-Bauplänen entfernen.** Vorschlag, angelehnt an Factorio:

   | Bauplan | Neu (Vorschlag) | Factorio-Vorbild |
   |---|---|---|
   | Handelsposten | 8 Bretter, 1 Truhe, 4 Eisenbarren | – |
   | Brenner-Bohrer | 3 Zahnräder, 1 Ofen, 3 Eisenbarren | Burner mining drill |
   | Förderband ×2 | 1 Zahnrad, 1 Eisenbarren | Transport belt |
   | Greifarm | 1 Schaltkreis, 1 Zahnrad, 1 Eisenbarren | Inserter |
   | Terminal (A3) | 5 Eisenbarren, 1 Glasscheibe, 2 Schaltkreise | Lab (grob) |
   | Kohle-Generator | 5 Zahnräder, 5 Eisenbarren, 1 Ofen, 4 Kupferkabel | Boiler + Steam engine |
   | Strommast ×2 | 2 Kupferkabel, 1 Stock | Small electric pole |
   | Elektro-Schmelzofen | 10 Steinziegel, 5 Schaltkreise, 1 Ofen | Electric furnace (vereinfacht) |
   | Presse | 10 Eisenbarren, 5 Zahnräder, 3 Schaltkreise | Assembling machine 1 |
   | Arena-Tor | 8 Eisenbarren, 8 Steinziegel, 2 Schaltkreise | – |
   | Armbrustturm | 12 Bretter, 6 Eisenbarren, 4 Zahnräder | – |
   | Arena-Einspeiser | 8 Eisenbarren, 4 Kupferkabel, 2 Schaltkreise, 1 Truhe | – |
   | Geschützturm | 10 Zahnräder, 10 Kupferbarren, 20 Eisenbarren | Gun turret |
   | Tesla-Turm | 24 Kupferkabel, 8 Eisenplatten, 4 Schaltkreise | – |
   | Assembler | 3 Schaltkreise, 5 Zahnräder, 9 Eisenplatten | Assembling machine 1 |

   Gold, Blei, Zinn usw. erst ab Höhlen-Bauplänen (Tier 2 nach dem Höhleneingang) verwenden. Den Höhleneingang
   selbst bleibt ohne Gold baubar (Bruchstein, Eisenplatten, Zahnräder, Motoren).
3. **Starter-Kit** (`quest/GuideEvents.java`): Den Goldbarren entfernen, sobald der Handelsposten kein Gold mehr braucht.
4. **Automatischer Progressionstest (wichtigster Teil):** Einen GameTest `ProgressionGameTests.everyBlueprintIsObtainable`
   schreiben, der die Blueprint-Registry, alle Maschinenrezepte, Schmelzrezepte und die Vanilla-Werkbankrezepte
   durchgeht:
   - Startmenge: Holz/Bretter/Stöcke, Bruchstein, Sand, Lehm, Items der Oberflächenfelder und Schmelzprodukte
     (Ofen, Steinziegel, Glas …).
   - Stufen in der Reihenfolge von `Quests.ALL` bzw. der `requires`-Kette „freischalten" und die Menge der
     herstellbaren Items erweitern (Fixpunkt-Iteration über Rezepte, deren Maschine schon verfügbar ist).
   - Prüfen: Jede Zutat und jedes Freischalt-Item (`unlockItems`, z. B. Drill Core aus dem TD) ist herstellbar,
     bevor der Bauplan benötigt wird. Höhlen-Items zählen erst nach dem Bau des Höhleneingangs, Minen-Items erst
     nach dem Minenschacht.
   - Bei einer Lücke nennt die Fehlermeldung Bauplan und fehlendes Item.
5. Handbuch-Quellen (`GuideScreen.renderSource`/`oreField`), Leitfaden-Hinweise und EMI-Kategorien mitziehen, damit
   die neuen Werkbank-Zwischenprodukte erklärt werden.

**Dateien:** `datagen/ModBlueprints.java`, `datagen/ModRecipeProvider.java` (Vanilla-Terminalrezept entfernen),
`datagen/ModSellPriceProvider.java`, `quest/Quests.java`, `quest/GuideEvents.java`,
`world/cave/CaveEntranceBlockEntity.java`, `client/GuideScreen.java`, neuer GameTest.

**Akzeptanz:**
- Der Progressionstest ist grün.
- In einer neuen Welt kommt man ohne Kreativ und ohne Vanilla-Werkzeuge bis zum Höhleneingang.
- Kein Tier-1- oder Tier-2-Bauplan braucht Redstone oder Gold.

#### A2 · Weniger Geld durch Belohnungen (P1)

**Notiz:** „Zu viel Geld durch Rewards, man muss nichts ausbauen, um direkt alles bis zur Presse freizuschalten."

**Befund:**
- Die Leitfaden-Belohnungen bis zum Schritt „Elektro-Schmelzofen" summieren sich auf rund **1.250 ¢**
  (`quest/Quests.java`).
- Kohle-Generator, Strommast, Elektro-Schmelzofen und Presse kosten zusammen **1.050 ¢** (`ModBlueprints`).
- Die Belohnungen bezahlen die Progression also komplett.

**Lösung:**
1. **Regel:** Die Summe der Belohnungen bis zu einem Schritt deckt höchstens ~25 % der bis dahin nötigen
   Freischaltkosten. Den Rest verdient man durch Verkäufe.
2. Belohnungen senken (frühe Schritte 10–50 ¢) und teils **als Items** statt Credits auszahlen, z. B. 16 Kohle,
   8 Zahnräder oder 1 Förderband-Stapel. Dafür `Quest` um ein optionales Belohnungs-Item erweitern
   (`QuestActions.claim`, Handbuch- und Terminal-Anzeige, Sync-Payload unverändert lassen, weil Items serverseitig
   vergeben werden).
3. Freischaltpreise so setzen, dass vor jeder großen Stufe ein Verkaufsziel sinnvoll ist. Beispiel: Presse erst
   nach ca. 30 Minuten automatisiertem Eisenverkauf; Richtwert ein voller Brenner-Bohrer ≈ 0,27 Items/s × 10 ¢.
4. **Unit-Test in `QuestsTest`:** Belohnungen bis Schritt *i* ≤ 0,25 × kumulierte Preise der UNLOCK-Schritte bis *i*
   (Preise als Konstante im Test oder aus einer gemeinsamen Tabelle).

**Akzeptanz:** Den Test gibt es und er ist grün. Die Presse ist nur nach echtem Verkauf erreichbar; Richtwert im
README festhalten.

#### A3 · Terminal an der Konstruktionswerkbank bauen (P1, klein)

**Befund:** Das Terminal ist ein Vanilla-Rezept (`ModRecipeProvider`, Zeilen „The only vanilla recipes").

**Lösung:**
- Vanilla-Rezept entfernen und einen kostenlosen Tier-1-Bauplan `terminal` in `ModBlueprints` anlegen (Zutaten
  siehe A1).
- Handbuch: `GuideScreen.renderCrafting`/`renderBlueprint` zeigt den Bauplan statt des Crafting-Rasters.
- Leitfaden-Hinweis `unlock_coal_generator.hint` anpassen.
- Die Ersatz-Werkbank (`WORKBENCH`) und das Handbuch bleiben Vanilla-Rezepte (Henne-Ei).

**Akzeptanz:** Das Terminal ist nur noch an der Werkbank baubar; ein GameTest baut es mit `BlueprintActions.build`.

#### A4 · Beschreibungen in der Werkbank (P1)

**Lösung:**
- Pro Bauplan einen Text `craftorio.blueprint.<id>.desc` (EN/DE) mit Kurzbeschreibung, Funktion, Eckwerten (Rate,
  Energie, Reichweite) und Einsatzhinweis.
- Anzeige in `client/WorkbenchScreen.java` und `client/TerminalScreen.java` (Tab Baupläne) als Tooltip beim Hover
  über Name/Icon einer Zeile, eventuell zusätzlich ein Info-Bereich.
- Dieselben Texte als Item-Tooltip (`appendHoverText` bzw. ein Tooltip-Event für alle Craftorio-Blockitems) und im
  Handbuch bei BUILD- und UNLOCK-Schritten.
- Eckwerte möglichst aus dem Code einsetzen (`DrillTier`, `TowerStats`, `GeneratorType`), nicht doppelt pflegen.

**Akzeptanz:** Jeder Bauplan hat einen Text. Ein Unit-/GameTest prüft, dass für jede Blueprint-ID ein Sprachschlüssel
in beiden Sprachen existiert (z. B. über die generierten Lang-JSONs).

### B – Logistik

#### B1 · Splitter, Merger, Filter (P1)

**Befund:**
- Förderbänder: `logistics/ConveyorBeltBlock(Entity)`, reine Spurlogik `BeltLane`.
- Seitliches Aufladen auf ein Band gibt es schon; es wirkt als einfacher Merger.
- Greifarm: `InserterBlock(Entity)`.

**Lösung (ein Block breit, Minecraft-tauglich):**
- **Splitter:** Eingang hinten, Ausgänge links, rechts und vorne. Verteilt reihum auf die Ausgänge, die Platz haben;
  jede Spur (links/rechts) wird getrennt behandelt. Optional per Rechtsklick eine Ausgangspriorität setzen.
- **Filter-Splitter** (oder Filter-Slot im Splitter): Ein Ghost-Slot im GUI legt fest, welches Item nach vorne
  geht; alles andere geht seitlich. Ohne Filter verhält er sich wie ein normaler Splitter.
- **Merger:** Eingänge links, rechts und hinten, Ausgang vorne. Nimmt abwechselnd von jedem Eingang (fair), statt wie
  beim Seitenaufladen die Vorfahrt zu vergeben.
- Die reine Verteillogik (Round-Robin, Priorität, Filter) als eigene Klasse mit Unit-Tests, analog `BeltLane`.
- Rendering: Items auf dem Splitter wie auf dem Band (`ConveyorBeltRenderer` wiederverwenden).
- Baupläne in Tier 1 bzw. 2 (Zutaten nach A1), Blockschutz beachten (`protection/`: geschützte Blöcke verschiedener
  Teams dürfen sich nicht berühren; der Splitter gilt als geschützt).

**Akzeptanz:**
- GameTests: Splitter teilt 1:1 auf zwei Truhen; Filter schickt nur Eisen nach vorne; Merger füllt ein Ausgangsband
  aus zwei Bändern gleichmäßig.
- Die Bänder der Stufen normal, schnell und express funktionieren alle.

#### B2 · Steigende und fallende Bänder (P2)

**Befund:** Bänder sind flach (`SHAPE` 3 px hoch, nur `FACING`). Vertikal gibt es bisher nur den Aufzug
(`ElevatorBlock`).

**Lösung:**
- Blockstate `SLOPE` (`FLAT`/`UP`/`DOWN`) wie bei Vanilla-Schienen: Wird ein Band vor einem Block eine Ebene höher
  (oder tiefer) platziert, stellt es sich schräg. Alternativ Sneak-Rechtsklick mit einem Schraubenschlüssel oder mit
  leerer Hand zum Umschalten.
- Übergabe zwischen Bändern auf y±1 in `ConveyorBeltBlockEntity`; die Item-Höhe im Renderer linear interpolieren.
- Blockmodell: Schräge (eigenes Modell pro Stufe oder rotierte Variante), Hitbox als Treppe oder Schräge.
- Spieler und fallengelassene Items, die mitfahren, müssen auf der Schräge funktionieren.

**Akzeptanz:** GameTest: ein Band steigt 3 Blöcke und fällt 3 Blöcke, die Items kommen an. Der Client zeigt die Items
auf der richtigen Höhe.

#### B3 · Lager-Depot mit Einlass und Auslass (P1)

**Notiz:**
- Rohstoffe in einen Lagerblock mit sehr großem Inventar laufen lassen; Stapelgröße über 64, damit er nicht vollläuft.
- Rohstoffe aus tieferen Ebenen sollen oben einsetzbar sein.

**Lösung (Standardvorschlag, siehe Entscheidung E-1):**
- **Lager-Block („Depot")**:
  - Speichert Items **nach Typ mit `long`-Zähler** (eine Map `Item(+Komponenten) → Anzahl`, keine `ItemStack`-Slots),
    z. B. bis zu 32 Item-Typen mit je 1.000.000 Stück.
  - Stufen: Depot I/II/III mit mehr Typen und Kapazität.
  - Die GUI zeigt eine scrollbare Liste mit Icon, Menge und Button „1 / 64 entnehmen".
- **Depot-Einlass:** Neben das Depot gesetzt, nimmt er von Band, Greifarm oder Trichter an (IItemHandler, der ins
  Depot bucht). Verkauft nichts.
- **Depot-Auslass:** Neben das Depot gesetzt, mit Filter-Slot (Ghost-Item). Gibt dieses Item nach vorne auf ein Band
  bzw. in einen Block aus, Rate je nach Stufe.
- Mehrere Einlässe und Auslässe pro Depot sind erlaubt; sie finden ihr Depot über direkte Nachbarschaft oder über eine
  Kette von Depot-Blöcken (mehrere Depots bilden einen gemeinsamen Speicher, wie ein Multiblock).
- **Ebenen verbinden:** Mit dem vorhandenen Aufzug oder mit Variante E-1 (b).
- Speicherung im BlockEntity (NBT als Liste Item-ID/Anzahl). Beim Abbau bleibt der Inhalt im Item erhalten (Data
  Component), wie bei einer Shulker-Kiste, sonst würden Millionen Items droppen.
- Die Kapazitätslogik als reine Klasse mit Unit-Tests.

**Akzeptanz:**
- GameTests: 10.000 Items per Einlass hinein, gefiltert per Auslass heraus.
- Beim Abbauen und Wiedersetzen bleibt der Inhalt erhalten.
- Es geht nichts verloren, wenn das Depot voll ist (der Einlass lehnt ab und das Band staut sich).

### C – Energie

#### C1 · Akkus als Strompuffer (P1)

**Befund:**
- Netzlogik: `energy/PowerGrid` (Netzwerke über Strommasten), Aufteilung `PowerDistribution.distribute(supply, demand)`.
- Quellen sind BlockEntities mit `PowerSource`, alles andere zählt als Verbraucher.
- Das Item `BATTERY` gibt es schon (Höhlenprodukt).

**Lösung:**
- **Akkubank-Block** mit großem Speicher (z. B. 1.000.000 FE, Lade- und Entladerate 2.000 FE/t), später Stufe II.
- Die Verteilung in `PowerGrid` läuft in zwei Phasen:
  1. Generatoren versorgen die Verbraucher.
  2. **Überschuss** lädt die Akkus (anteilig); bei **Defizit** geben die Akkus bis zu ihrer Entladerate ab, dann
     erneut verteilen.
- Die Logik als reine Funktion in `PowerDistribution` (z. B. `distributeWithStorage`) mit Unit-Tests: Überschuss
  lädt, Defizit entlädt, leerer Akku liefert nichts, Raten werden eingehalten.
- GUI: Füllstand, aktuelle Lade- bzw. Entladeleistung. Jade-Anzeige ergänzen (`compat/jade`).
- Bauplan: Tier 2, braucht Batterien (nach A1 prüfen, ob sie dann erreichbar sind; sonst eine Tier-1-Variante
  „Blei-Säure-Akku" aus Kupfer/Eisen und eine größere Variante mit Batterien).

**Akzeptanz:**
- Unit- und GameTests: Ein Generator läuft nur tagsüber (Brennstoff alle), die Maschine läuft über den Akku weiter,
  bis er leer ist.
- Die Netzanzeige des Strommasts zeigt die gespeicherte Energie.

#### C2 · Strom und Munition für die Arena verständlich machen (P1, klein)

**Notiz:** „Es gibt Türme, die Energie brauchen, in der Arena kann aber nichts gebaut werden. Wie soll Energie von
außen an die Arena angelegt werden?"

**Befund:**
- Das ist schon gelöst, aber nicht auffindbar: Der **Arena-Einspeiser** (`defense/arena/ArenaFeederBlock(Entity)`)
  wird in der Fabrikwelt ans Stromnetz gehängt. Er schickt jede Sekunde Energie in den Arena-Pool des Teams
  (`TowerDefense.feedEnergy`) und nimmt Munition (Bolzen, Patronen) per Band oder Greifarm an.
- Die Türme in der Arena ziehen aus diesem Pool.

**Lösung:**
- Leitfaden-Schritt „Baue einen Arena-Einspeiser" vor dem ersten Strom-Turm (Tesla), mit Hinweistext.
- Tooltip bei Strom- und Munitionstürmen: „Versorgung über den Arena-Einspeiser in deiner Fabrik".
- In der Arena im HUD den Pool anzeigen (Energie, Bolzen, Patronen; die Daten sind schon in `TdStatusPayload`) und
  rot warnen, wenn ein Turm keine Versorgung hat.
- Handbuch-Seite bzw. README-Absatz „Wie kommen Strom und Munition in die Arena?".

**Akzeptanz:** Neue Spieler finden über HUD, Tooltip und Leitfaden die Lösung ohne externe Hilfe.

### D – Arena / Tower Defense

#### D1 · Wellen in der Arena starten (P1)

**Befund:** Start, Auto-Modus, Welle früher rufen und Alles reparieren gibt es nur im Terminal (`menu/TerminalMenu`,
Button-IDs `TD_START`, `TD_TOGGLE_AUTO`, `TD_CALL_WAVE`, `TD_REPAIR_ALL`). Das Terminal lässt sich in der Arena nicht
bauen (`ArenaRules`).

**Lösung:**
- **Arena-Pult:** Ein fester Block, den `ArenaBuilder.buildFrame` auf der Tribüne neben Depot und Ausgang setzt
  (`Arenas.depot`/`exit` als Vorlage für eine neue Position).
- Rechtsklick öffnet das `TerminalMenu` direkt im Tab **Abwehr**. Dafür dem Menü/Screen einen Start-Tab mitgeben (per
  Open-Buffer); `TerminalMenu.stillValid` prüft schon die Distanz zur übergebenen Position.
- Alternativ oder zusätzlich die Befehle `/craftorio td start|wave|auto` für Tastenkürzel.
- Bestehende Arenen bekommen das Pult beim nächsten `buildFrame`/Kartenwechsel. Prüfen, ob `buildFrame` für
  bestehende Arenen erneut läuft, sonst beim Betreten nachrüsten.

**Akzeptanz:** Der Level-Start ist ohne Arena-Verlassen möglich. GameTest: Das Pult steht nach `buildFrame` an der
erwarteten Position; die Menü-Buttons lösen `TowerDefense.start` aus.

#### D2 · Pfadstab erlaubt Abzweigungen (P1, klein)

**Befund:**
- `defense/arena/PathWandItem.useOn` setzt jeden Block, der auf einer erlaubten Kachel liegt.
- Erst `PathTracer.trace` meldet beim Level-Start `Error.BRANCH`; der Spieler merkt den Fehler zu spät.

**Lösung:**
- Vor dem Setzen jedes Blocks (auch in der Linie) prüfen: Nach dem Setzen darf **kein** Pfadblock mehr als zwei
  Pfad-Nachbarn haben (4er-Nachbarschaft auf `BUILD_Y`, ±1 Höhe wie im Tracer). Das gilt für den neuen Block und für
  seine Nachbarn. Das verhindert auch 2×2-Flächen.
- Endpunkte am Tor und am Kern berücksichtigen (Portal und Kern zählen als Nachbar).
- Bei einem Verstoß stoppt die Linie am letzten gültigen Block mit der Meldung `craftorio.arena.path.branch` (EN/DE).
- Die Nachbarschaftsregel als statische Methode in `PathTracer` (reine Logik) mit Unit-Test.

**Akzeptanz:** Unit-Test (T-Kreuzung und 2×2 werden abgelehnt, Kurve und Gerade erlaubt) und GameTest mit dem Pfadstab.

#### D3 · Wasser-Karte ohne baubaren Weg (P0)

**Notiz:** „Levelaufbau mit Wasser hat dazu geführt, dass ich keinen Weg zum Ziel bauen konnte."

**Befund:**
- Logisch ist ein Weg garantiert: `ArenaLayout.generate` ruft `carve(...)`, falls `shortestPath < 0`.
- **Aber in `ArenaBuilder.buildTile` sehen Furten (`ROUGH`: Wasser auf Sand) und tiefes Wasser (`BLOCKED`: Wasser auf
  dunklem Prismarin) von oben gleich aus: beide sind Wasserblöcke auf `FLOOR_Y`.**
- Der Spieler kann die begehbaren Furten nicht erkennen; der Pfadstab lehnt tiefes Wasser mit „blockiert" ab.
- Zusätzlich (Hypothese, bitte verifizieren): Der Builder setzt Blöcke ohne `UPDATE_KNOWN_SHAPE` (siehe D6).
  Dadurch kann Wasser beim Neubau fließen oder Nachbarformen ändern und eventuell Kacheln an `BUILD_Y` belegen, sodass
  `current.isAir() || current.canBeReplaced()` im Pfadstab scheitert.

**Lösung:**
1. **Furten sichtbar machen:** Auf `ROUGH`-Wasserkacheln Trittsteine setzen, z. B. `FLOOR_Y` = Kies oder
   moosbewachsener Bruchstein mit einer Wasserschicht als Deko daneben. Alternativ Seerosen/Schilf nur auf tiefem
   Wasser und klar anderes Material für Furten. Die Regel „Furt bremst Gegner" bleibt über den Tile-Typ erhalten.
2. **Weg-Vorschau:** Sneak-Rechtsklick mit dem Pfadstab in die Luft zeigt die Route aus `ArenaLayout.route()` für
   10 s als Partikel. Der Befehl `/craftorio arena route` existiert schon und kann dafür als Grundlage dienen.
3. **Absicherung per GameTest:** Für alle Themen und viele Seeds (z. B. Level 1–40, 3 Arena-Seeds) das Feld mit
   `ArenaBuilder.buildField` bauen, entlang `route()` mit der Pfadstab-Logik Pfadblöcke setzen (die Setz-Logik aus
   `PathWandItem` in eine testbare Methode auslagern) und `PathTracer.trace` ausführen. Erwartung: ok, und kein
   Block wurde abgelehnt.
4. Die Ursache aus Punkt 3 beheben, falls der Test weitere Fehler findet (z. B. geflossenes Wasser, Seerosen).

**Akzeptanz:** Der GameTest über alle Themen und Seeds ist grün. Die Furten sind im Client klar erkennbar.

#### D4 · Turmdepot als Inventar (P1)

**Befund:** `TowerDefense.takeDepot` legt alles per `placeItemBackInInventory` ins Spieler-Inventar; bei vollem
Inventar fällt es auf den Boden.

**Lösung:**
- Rechtsklick auf das Turmdepot öffnet ein **Nur-Entnahme-Inventar** (z. B. 54 Slots, scrollbar bei mehr) auf Basis
  von `Zone.depot` (Liste von ItemStacks).
- Einlegen ist nicht möglich. Shift-Klick nimmt einen Stapel, die Schaltfläche „Alles nehmen" nimmt so viel, wie passt.
- Serverseitig ein eigenes `IItemHandlerModifiable` über die Liste. Beim Schließen leere Einträge entfernen und
  `setDirty()` aufrufen.

**Akzeptanz:** Nichts droppt mehr. GameTest: Das Depot enthält 3 Türme, der Spieler nimmt einen, 2 bleiben.

#### D5 · Türme aus dem Depot stapeln nicht (P1, klein)

**Befund:**
- `TowerDefense.towerItem` setzt immer die Komponente `TOWER_STATE(upgradeLevel, health)`.
- Frisch gebaute Türme haben keine Komponente, verschieden beschädigte Türme haben unterschiedliche Werte. Stacks
  sind nur bei identischen Komponenten möglich.

**Lösung (Empfehlung):**
- Türme beim Einpacken voll reparieren (neue Karte = frischer Start). Zerstörte Türme (Ruinen) kommen als Ruine bzw.
  mit Reparaturbedarf ins Depot oder werden gegen die Reparaturkosten repariert; das ist eine Designfrage, im Zweifel
  wie bisher Health 0 behalten.
- **Komponente nur setzen, wenn sie vom Standard abweicht** (Upgrade-Stufe > 0 oder beschädigt). Damit stapeln
  unbeschädigte Türme der Stufe 0 mit neu gebauten.
- Den Stufen-Tooltip am Item beibehalten. Türme gleicher Stufe stapeln miteinander.

**Akzeptanz:** GameTest: Ein neu gebauter Armbrustturm und einer aus dem Depot (Stufe 0, voll) stapeln.

#### D6 · Blumen und Gras droppen beim Kartenneubau (P1, klein)

**Befund:**
- `ArenaBuilder.set` nutzt `FLAGS = Block.UPDATE_CLIENTS` **ohne** `Block.UPDATE_KNOWN_SHAPE`.
- `Level.setBlock` ruft dadurch `updateNeighbourShapes` auf. Wenn der Grasblock unter einer Blume zu Luft wird, meldet
  die Blume in `updateShape` Luft zurück, und `Block.updateOrDestroy` ruft `destroyBlock(..., drop=true)` auf, sodass
  Items droppen.

**Lösung:**
- `FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE`.
- Beim Leeren zusätzlich **von oben nach unten** abreißen.
- Nach `buildField` alle `ItemEntity`s in der Arena-Bounding-Box entfernen (Sicherheitsnetz).
- Wasser und Lava ggf. danach gezielt ticken lassen oder bewusst ruhend lassen; im D3-Test prüfen.

**Akzeptanz:** GameTest: Wald-Karte bauen, Folgekarte bauen, danach keine ItemEntity im Arena-Bereich.

#### D7 · Arena-Karten kreativer und voller (P2)

**Wunsch:** mehrere Blockarten, Konstruktionen, Häuser, Bäume usw.

**Lösung:**
- **Tile-Semantik bleibt** (`GROUND`/`ROUGH`/`HIGH`/`BLOCKED`); nur die Optik wird reicher.
- **Paletten je Thema:** Mehrere Boden-, Deko- und Blocker-Blöcke, gewichtet und per Rauschen gemischt (die
  Noise-Hilfe `world/cave/Noise2D` gibt es schon).
- **Strukturen auf zusammenhängenden `BLOCKED`-Flächen:** Kleine NBT-Strukturen unter `data/craftorio/structure/arena/<thema>/`
  (Hütte, Ruine, Brunnen, Felsbogen, Lavaschmiede, Steg, Säulenruine) werden per `StructureTemplate` gesetzt, wenn
  ihr Footprint komplett auf `BLOCKED` liegt. Alternativ im Generator als eigene Tile-Gruppen reservieren
  (`ArenaLayout` um „Struktur-Slots" erweitern, damit der Weg- und Bauplatz-Check sie kennt).
- **Bäume:** Mehrere Arten und Formen (Eiche, Birke, Fichte, große Eiche 2×2), Büsche, Laub, Pilze.
- **Rand:** Wände mit Bannern je Thema, Fackeln/Laternen, Zuschauer-Deko (Rüstungsständer) auf der Tribüne.
- Die Deko darf den Pfad (`BUILD_Y` auf `GROUND`/`ROUGH`) und Turmplätze nicht blockieren: Deko nur auf `BLOCKED` bzw.
  unterhalb von `FLOOR_Y`, ausgenommen niedrige, ersetzbare Pflanzen auf `GROUND`, die der Pfadstab und das Turmsetzen
  ersetzen (`canBeReplaced`).

**Akzeptanz:** Unit-Test: `MIN_BUILD_TILES` und der Weg bleiben garantiert. D3-GameTest weiter grün. Screenshots je
Thema im README.

### E – Höhlen

#### E1 · Höhleneingang in flachen Welten / Y-Höhe (P1)

**Notiz:** „Cave entrance y oder höher? Im flachen Grasland gibt es keine Caves und man befindet sich auf y = −60."

**Befund:**
- Die Schichten sind feste Höhenbänder (`world/cave/CaveLayers`):
  - Oberfläche ab y ≥ 50
  - Deckgestein I: 40–49
  - Höhlen: 0–39
  - Deckgestein II: −10…−1
  - Minen: −59…−11
- Erzeugt werden sie durch das Worldgen-Feature `cave_layers`.
- Der Eingang muss auf y ≥ 50 stehen (`CaveEntranceBlock.placementError`) und prüft per `hasLayer`, ob die Schicht
  existiert.
- In **Superflat-Welten** (Oberfläche y = −60) gibt es keine Schichten, der Eingang ist unmöglich. Dasselbe gilt für
  alte Chunks, die vor M6 generiert wurden.

**Lösung (je nach Entscheidung E-2):**
- **(a) Kurzfristig, immer machen:**
  - Klare Meldung beim Platzieren: „Diese Welt hat keine Craftorio-Schichten (Superflat oder alte Chunks). Nutze
    einen normalen Welttyp."
  - Beim Serverstart den Welttyp prüfen und eine Warnung ins Log und an Operatoren senden.
  - Eine **eigene World-Preset** `craftorio:default` (Normalwelt mit den Schichten) anbieten und im README empfehlen.
  - Die Frage „y oder höher?" im Handbuch-Hinweis beantworten: Eingang auf der Oberfläche setzen, y ≥ 50.
- **(b) Robust:** Höhlen und Minen als **eigene Dimensionen** (wie die Arena, `data/craftorio/dimension/…`) mit eigenem
  Generator. Der Eingang wird zum Portal bzw. Schacht in die Höhlen-Dimension, Aufzüge verbinden dimensionsübergreifend.
  - Vorteil: Funktioniert in jeder Welt (auch Superflat und bestehende Welten), unabhängig von anderen Worldgen-Mods.
  - Nachteil: Größerer Umbau von Aufzug, Schacht und Bereichsfreischaltung (`CaveAreas`).

**Akzeptanz:**
- (a) Verständliche Fehlermeldung, Preset auswählbar.
- (b) GameTest: Eingang in einer Superflat-Testwelt öffnet die Höhle.

#### E2 · Höhlen ausgestalten (P2)

**Befund:** `CaveCarver`/`CaveShape` erzeugen Hallen und Tunnel mit Füllgestein; es gibt kaum Deko.

**Lösung:**
- Deterministische Deko pro Chunk (Seed aus der Chunk-Position):
  - Tropfstein (spitzer Tropfstein oben und unten), Leuchtflechten, Moosflecken, Pilze, Spinnweben
  - kleine Wasser- und Lavapfützen (nicht neben Erzfeldern), Amethyst-Cluster
  - verlassene Minen-Deko (Schienenstücke, Holzstützen, Laternen), Gesteinsvarianten (Tuff, Andesit, Tiefenschiefer)
    per Rauschen
- Minen-Schicht: dunkler und technischer (Stützbalken, Kristallgeoden als Deko um Kristallfelder).
- Licht: Etwas natürliches Licht (Leuchtflechten, Pilze), damit die Höhlen nicht komplett dunkel sind, aber Fackeln
  weiter nötig bleiben.
- Deko nie auf Erzfeldern, Pfaden oder im Umkreis des Schachts. Die Performance bei der Freischaltung beachten (Carving
  in `CaveEntranceBlockEntity` ist schrittweise).

**Akzeptanz:** GameTest: Nach der Freischaltung gibt es in einem Chunk mindestens N Deko-Blöcke, und Erzfelder sind
unverändert. Screenshots im README.

#### E3 · GUI für den Höhleneingang (P1)

**Befund:**
- `CaveEntranceBlockEntity` hat `requirements()` (z. B. 128 Bruchstein, 32 Eisenplatten, 16 Zahnräder, 8 Motoren) und
  `delivered`.
- Geliefert wird per Rechtsklick oder Automatik; es fehlt eine Übersicht.

**Lösung:**
- Rechtsklick öffnet eine GUI (`MachineMenuBase`):
  - Liste der Anforderungen mit Icon, geliefert/benötigt und Fortschrittsbalken
  - Einwurf-Slot (Items werden sofort verbucht und überschüssige zurückgegeben)
  - Status (Baustelle, Bohren X %, Offen) und Ziel-Schicht
- Daten über `ContainerData` oder einen Sync beim Öffnen und bei Änderung.
- Greifarm, Band und Trichter liefern weiter automatisch.
- Dieselbe GUI für den Minenschacht (gleiche Klasse, anderes Ziel).

**Akzeptanz:** GameTest: Einwurf über das Menü verbucht richtig; überschüssige Items bleiben beim Spieler.

---

## 3. Empfohlene Reihenfolge (je ein Commit)

1. **Paket 1 – Arena-Fehler:** D6, D5, D2, D3, D4 (kleine, klar umrissene Fehler, bringen sofort Spielbarkeit).
2. **Paket 2 – Arena-Komfort:** D1, C2.
3. **Paket 3 – Progression:** A1 (inkl. Progressionstest), A3, A2, dann A4.
4. **Paket 4 – Logistik:** B1, B3, danach B2.
5. **Paket 5 – Energie:** C1.
6. **Paket 6 – Höhlen:** E3, E1 (nach Entscheidung E-2), E2.
7. **Paket 7 – Inhalte:** D7.

Nach jedem Paket: Tests grün, README aktualisiert, kurzer Changelog-Absatz im Commit.

---

## 4. Offene Entscheidungen (vor dem jeweiligen Paket klären)

| ID | Frage | Empfehlung (gilt, falls nichts anderes entschieden wird) |
|---|---|---|
| E-1 | Lager-Depot lokal (Einlass/Auslass nur am Block) oder teamweit (alle Einlässe/Auslässe eines Teams teilen sich einen Speicher, über Ebenen hinweg)? | **Lokal** + Aufzug für die Ebenen. Teamweit („Team-Lager") ist später als Tier-3-Upgrade möglich. |
| E-2 | Höhlen/Minen als Höhenbänder in der Oberwelt behalten (nur normale Welten) oder als eigene Dimensionen (jede Welt)? | Zuerst **(a)** (klare Meldung + World-Preset); **(b)** eigene Dimensionen, falls Superflat oder bestehende Welten wichtig sind. |
| E-3 | Vanilla-Werkzeugrezepte (Spitzhacken, Äxte …) abschalten, da es die Starter-Spitzhacke gibt? | **Ja**, zusammen mit A1 (Datapack-Rezeptentfernung oder Condition). Schwerter und Rüstung bleiben. |
| E-4 | Leitfaden-Belohnungen als Credits oder als Items? | **Gemischt**: früh Items (Kohle, Zahnräder, Bänder), später kleine Credit-Beträge. |
| E-5 | Zerstörte Türme beim Kartenwechsel: als Ruine mit Reparaturkosten ins Depot oder kostenlos repariert? | Ruinen weiter reparaturpflichtig, intakte Türme werden voll geheilt (siehe D5). |
