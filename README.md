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
./gradlew runClient -Pcompat           # zusätzlich mit EMI und Jade
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
- **Handelsposten**: Rechtsklick öffnet die **Theke** (9 Slots): Items hineinlegen, der Wert wird angezeigt, erst
  der Button **Verkaufen** verkauft sie – so geht nichts aus Versehen weg. Trichter und Förderbänder verkaufen
  direkt. Der Erlös geht aufs Team-Konto; Items ohne Preis werden abgelehnt.
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
  Rechtsklick öffnet ihn wie einen Ofen: Brennstoff-Slot (eine Kohle brennt, dann die nächste), Flamme,
  Feldblöcke und Rate sowie ein Ausgabe-Slot (ein Stapel), aus dem man die Ausbeute nimmt. Automatisch (Greifarm)
  kann nur Brennstoff hinein und Ausbeute heraus.

![Brenner-Bohrer](docs/screenshots/brenner-bohrer-gui.png)
![Handelsposten](docs/screenshots/handelsposten-gui.png)
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

### M5 – Tower Defense (Arena)

- **Arena**: Das **Arena-Tor** (Gratis-Bauplan) in der Oberwelt aufstellen und rechtsklicken – es führt in die
  eigene Arena (eigene Dimension, ein Stadion pro Team). Auf der Tribüne: **Ausgang** und **Turmdepot**.
- **Jedes Level eine neue Karte** (41×41) mit Thema und Regel: **Wald** (Tarnung im Dickicht), **Berge**
  (Plateaus +30 % Reichweite, Geröll bremst), **Feuer** (Lavaschlote verbrennen Gegner), **Wasser** (Fluss mit
  Furten, Flachwasser bremst) und jedes 10. Level das **Kolosseum** (Boss). Eines von drei Toren in der Westmauer ist
  offen, der Kern sitzt in der Ostmauer.
- **Weg legen**: mit dem **Pfadstab** vom offenen Tor zum Kern (Klick = Wegstück, nächster Klick in derselben
  Reihe/Spalte = gerade Linie, Schleichen + Klick entfernt). Rechtsklick auf den Kern prüft den Weg.
- **Türme** frei auf freiem Boden oder Plateaus: **Armbrustturm** (Bolzen), **Geschützturm** (Patronen),
  **Tesla-Turm** und **Laserturm** (Strom). GUI mit Lebenspunkten, **Aufrüstung Stufe I–V** und **Zielmodus**
  (Erster/Letzter/Stärkster/Schwächster). Versorgung per Hand oder über den **Arena-Einspeiser** in der Fabrik
  (Strom und Munition per Band/Greifarm → Arena-Vorrat).
- **Level** im Terminal-Tab *Abwehr* starten (optional automatisch weiter): Karte, Regel, Mutator,
  **Wellenvorschau**, Arena-Vorrat; **Welle rufen** schickt die nächste Welle früher (Bonus-Credits).
  10 Leben; **Sterne** je nach verbliebenen Leben (bis +50 % Belohnung); ab Level 6 zufällige **Mutatoren**
  (Nebel, Eilmarsch, Gehärtet) mit mehr Belohnung.
- **Gegner**: Krabbler, Brecher (ab 5), Spucker (ab 10), Kristallgolem (ab 20), Brutmutter (Boss). Sie greifen
  **nur Türme** an – nie die Fabrik oder Spieler.
- Nach einem Sieg kommen alle Türme mit Stufe, HP und Munition ins **Turmdepot** und die nächste Karte entsteht;
  Schlüsselmaterialien (alle 10 Level) liegen ebenfalls im Depot. Zerstörte Türme werden zu **Ruinen**
  (Wiederaufbau für Credits, *Alle Türme reparieren* im Terminal).
- Admin-Befehl: `/craftorio arena route` legt den kürzesten Weg (für Tests).

![Arena von oben](docs/screenshots/arena-draufsicht.png)
![Kampf in der Wasser-Arena](docs/screenshots/arena-kampf.png)
![Neue Karte: Feuer](docs/screenshots/arena-feuer.png)
![Abwehr-Tab](docs/screenshots/arena-terminal.png)
![Turmdepot](docs/screenshots/arena-depot.png)

### M6 – Höhlenschicht

- **Schichten** (nur in neu erzeugten Chunks): Oberfläche ab Y 50, darunter **Deckgestein** (Y 40–49),
  die **Höhlenschicht** (Y 0–39) und eine zweite Deckgesteinsschicht (Y −10 bis −1). Die Höhlenschicht besteht
  zunächst komplett aus unzerstörbarem **Höhlengeröll** – man kann nicht hineingraben.
- **Höhleneingang** (Bauplan Stufe 2, braucht die Montagemaschine): an der Oberfläche aufstellen, dann Material
  anliefern (128 Bruchstein, 32 Eisenplatten, 16 Zahnräder, 8 Motoren – per Hand, Band oder Greifarm) und mit
  Strom (40 FE/t über einen Strommast) eine Minute bohren lassen.
- Danach öffnet sich ein **Schacht mit Gerüst** (Schleichen zum Absteigen; oberhalb der Höhlen mit Stein
  verkleidet) und der Höhlenbereich von **7×7 Chunks** um den Eingang wird ausgehöhlt: eine große Halle mit
  Säulen, Tuffboden und Platz zum Bauen. Außerhalb bleibt eine Wand aus Höhlengeröll – weitere Eingänge
  erweitern das Gebiet nahtlos.
- **Höhlen-Rohstoffe** als unerschöpfliche Felder auf dem Hallenboden: **Zinn, Blei, Schwefel, Gold, Quarz**.
  Neue Produkte: Zinn-/Bleibarren (Schmelzofen), **Batterie** und **Fortgeschrittener Schaltkreis** (Montage).

- **Warenaufzug** (Bauplan nach dem Höhleneingang, 2 Stück pro Bau): Zwei Aufzüge in derselben Spalte
  (gleiches X/Z) verbinden sich automatisch – auch durch Deckgestein und massiven Fels, bis 256 Blöcke weit.
  Rechtsklick wechselt den Modus: *nach oben senden*, *nach unten senden* oder *empfangen*. Sender nehmen
  Items per Band/Greifarm/Trichter an (bis 40 Items/s), Empfänger geben sie nach vorne aus.

![Höhle](docs/screenshots/m6-hoehle.png)
![Schacht](docs/screenshots/m6-schacht.png)

### M7 – Minenschicht

- **Minenschicht** (Y −59 bis −11, nur in neu erzeugten Chunks): unter dem zweiten Deckgestein, zunächst
  komplett aus unzerstörbarem **Minengeröll**. Niedrigere Gänge mit vielen Säulen, Basaltboden und Tiefenschiefer.
- **Minenschacht** (Bauplan Stufe 3 – Präzisionswerkbank, braucht den Warenaufzug): wird in einem
  freigeschalteten Bereich der **Höhlenschicht** gebaut (sonst Fehlermeldung), braucht 64 Bleibarren,
  16 Motoren, 8 Batterien und 8 fortgeschrittene Schaltkreise und bohrt mit **80 FE/t** (ein Kohlegenerator
  reicht nicht). Danach führt ein Gerüstschacht durch das Deckgestein in die Minen und 7×7 Chunks werden ausgehöhlt.
- **Minen-Rohstoffe**: **Diamant, Titan, Uran, Kristall**. Titan → Titanbarren (Ofen) → Titanplatte (Presse);
  Uran → Uranpellet (Presse) → **Brennstab** (Montage, mit Titanplatten); Kristallsplitter → **Energiekristall**.
- **Maschinen-Stufen**
  - **Elektrischer Bohrer** (Stufe 2, Freischaltung mit dem Resonanzkristall aus TD-Level 20): 3×3, doppelt so
    schnell wie der Brenner-Bohrer, 30 FE/t statt Brennstoff. **Tiefenbohrer** (Stufe 3): **5×5**, 4× schneller
    pro Block (bis 3 Items/s), 80 FE/t. Abbauflächen verschiedener Bohrer dürfen sich weiterhin nicht überlappen.
  - **Schnelles Förderband** (3,75 Blöcke/s, Stufe 2) und **Express-Förderband** (5,625 Blöcke/s, Stufe 3);
    alle Bänder lassen sich beliebig verbinden.
  - **Reaktor** (Stufe 3): 400 FE/t aus Brennstäben (5 Minuten pro Stab), 200.000 FE Puffer.
- **Tower Defense**: Ab Level 20 kommen **Kristallgolems** – ihr Panzer lässt nur 35 % von Munitionsschaden
  durch; Energietürme (Tesla, Laser) machen vollen Schaden. Der **Laserturm** (Stufe 3) schießt 14 Blöcke weit
  mit 30 Schaden (800 FE pro Schuss).
- Admin-Befehl: `/craftorio layer unlock caves|mines` schaltet den Bereich um die eigene Position ohne Eingang
  frei (für Tests oder alte Welten).

![Minenhalle](docs/screenshots/m7-mine-hall.png)
![Neue Maschinen](docs/screenshots/m7-machines.png)
![Minenschacht](docs/screenshots/m7-mine-shaft.png)
![Platzierungsregel](docs/screenshots/m7-shaft-rule.png)

### M8 – Coop & Polish

- **Craftorio-Handbuch**: Jeder Spieler bekommt es beim ersten Betreten (Ersatz: Buch + Eisenbarren, oder
  `/craftorio guide`); öffnen per Rechtsklick oder jederzeit mit **G**. Es schlägt den aktuellen Leitfaden-Schritt
  auf und zeigt, **wie man ihn schafft**: Bauplan-Material mit Symbolen und Werkbank-Stufe, das Crafting-Raster
  von Konstruktionswerkbank bzw. Terminal, Kaufpreis, oder bei Verkaufszielen die Quelle (Erzfeld, Ofen, Presse,
  Montage). Mit den Pfeilen blättert man durch alle Schritte. Ist ein Ziel erreicht, wird der mittlere Button zu
  **„Belohnung abholen"** – ein Terminal braucht man dafür nicht.

- **Starter-Kit** (Server-Config `economy.starterKit`, Standard an): Beim ersten Betreten gibt es zusätzlich die
  unzerstörbare **Starter-Spitzhacke**, eine Konstruktionswerkbank, 1 Goldbarren, 1 Truhe und 16 Kohle.
- **Einstieg wie in Factorio – ohne Werkzeuge bauen zu müssen**: Mit der Starter-Spitzhacke baut man Erzfelder
  (das Feld bleibt stehen) und Stein von Hand ab, stellt einen Ofen her und schmilzt das erste Eisen. Der Leitfaden
  beginnt mit *16 Eisenerz per Hand abbauen* → *Brenner-Bohrer* → *Handelsposten* → *erster Verkauf*.
- **„Fehlt?"** an der Werkbank: Fehlendes Material ist rot markiert, der Tooltip zeigt „du hast X". Ein Klick auf
  *Fehlt?* listet im Chat genau, was noch fehlt.
- **Hinweis-Popups**: Sobald ein Leitfaden-Ziel erreicht ist, erscheint oben rechts „Erreicht! Mit G abholen" –
  die Belohnung holt man im Handbuch (oder im Terminal) ab.

![Handbuch](docs/screenshots/handbuch.png)
![Fehlt?](docs/screenshots/werkbank-fehlt.png)

- **Leitfaden** – die Questline führt Schritt für Schritt durchs Spiel: 33 Ziele vom ersten Handabbau über
  Strom, Tower Defense, Höhlen bis zu Minen und 1 Mio. ¢. Jedes Ziel hat eine **Anleitung** (Tooltip im
  Terminal-Tab *Leitfaden*), der **nächste Schritt** ist markiert und steht mit Fortschritt dauerhaft im **HUD**
  unter dem Kontostand. Gemessen werden von Hand abgebaute Erze, Verkäufe, gekaufte und an der Werkbank gebaute
  Baupläne, Gesamtverdienst und TD-Level; Belohnungen (Credits) holt man einmal pro Team im Handbuch oder Terminal ab.
- **Team-Rechte**: Das erste Mitglied ist die **Teamleitung**. Sie kann Mitglieder entfernen
  (`/craftorio team kick <Spieler>`), die Leitung übergeben (`/craftorio team leader <Spieler>`) und festlegen,
  wer Credits ausgeben darf (`/craftorio team spending all|leader`) – gilt für Baupläne, Turm-Upgrades,
  Reparaturen und Wiederaufbau. `/craftorio team info` zeigt Leitung und Ausgaberecht.
- **Blockschutz zwischen Teams** (Server-Config `protection.enabled`, Standard an): Maschinen, Türme und alle
  Blöcke mit Inventar gehören dem Team, das sie platziert hat. Andere Teams können sie nicht abbauen oder
  öffnen, Explosionen zerstören sie nicht. Geschützte Blöcke verschiedener Teams dürfen sich **nicht berühren**
  (ein Block Abstand) – so kann kein Trichter, Band, Bohrer, Aufzug oder Greifarm Items in eine fremde Basis
  hinein- oder herausbewegen; Greifarme prüfen das zusätzlich selbst. Tritt ein Solo-Spieler einem Team bei,
  gehen seine Blöcke **und seine TD-Zone** an das neue Team über. Operatoren im Kreativmodus dürfen alles.
- **Skalierung**: Pro zusätzlichem Teammitglied online kommen je Welle 2 Krabbler mehr (zusätzlich zu +35 %
  Gegner-HP); die Belohnung bleibt gleich.
- **Balancing-Test**: Ein GameTest prüft, dass jede Presse-/Montagestufe mindestens 10 % Wert schafft und kein
  Schmelzrezept Wert vernichtet.
- **EMI** (optional): Kategorien *Pressen*, *Montage* und *Bauplan (Werkbank)* mit Stufe, Preis und
  Schlüsselmaterialien. **Jade** (optional): Besitzer-Team, Bohrer-Rate, Maschinenfortschritt, Turm-HP/Munition,
  Ruinen-Kosten, Baustellen- und Aufzugsstatus sowie Ertrag und Wert von Erzfeldern.
  Im Dev-Client mit `./gradlew runClient -Pcompat` laden.
  **Empfehlung für Spieler:** EMI (für 1.21.1/NeoForge, z. B. von Modrinth) einfach zusätzlich in den
  `mods`-Ordner des **Clients** legen – dann zeigt ein Klick auf ein Item (R = Rezept, U = Verwendung), wie man
  es herstellt. Auf dem Server ist EMI nicht nötig.

![Leitfaden](docs/screenshots/m8-leitfaden.png)
![HUD und Jade](docs/screenshots/m8-jade-hud.png)
![EMI Bauplan](docs/screenshots/m8-emi-bauplan.png)
![EMI Montage](docs/screenshots/m8-emi-montage.png)

## Projektstruktur

```
src/main/java/de/craftorio/
├── Craftorio.java          Mod-Einstiegspunkt
├── CraftorioConfig.java    Server-Konfiguration
├── registry/               Blöcke, Items, Block-Entities, Creative-Tab
├── team/                   Teams, Konto, Rechte (TeamRegistry ist reine Logik mit Unit-Tests)
├── protection/             Blockschutz zwischen Teams
├── quest/                  Leitfaden (Ziele als reine Logik mit Unit-Tests)
├── compat/                 Optionale EMI- und Jade-Plugins
├── economy/                Preise (Data Map), Verkauf, Handelsposten
├── world/                  Erzfelder, Weltgenerierung, Start-Felder
│   └── cave/               Höhlen-/Minenschicht, Form (reine Logik mit Unit-Tests), Eingänge/Schächte, Aushöhlen
├── machine/                Bohrer-Stufen (Abbaufläche, Produktionsrate), Verarbeitungsmaschinen
├── logistics/              Förderband-Stufen (BeltLane = reine Spur-Logik mit Unit-Tests), Greifarm, Warenaufzug
├── energy/                 Kohlegenerator/Reaktor, Strommast, Stromnetz (Verteilung als reine Logik mit Unit-Tests)
├── recipe/                 Maschinenrezepte (Presse, Montage)
├── blueprint/              Baupläne, Freischalt-Regeln, Terminal, Werkbänke
├── defense/                Tower Defense: Level-Ablauf, Gegner, Türme (LevelPlan/PathTracer/TowerStats = reine Logik)
│   └── arena/              Arena-Dimension, Kartengenerator mit Themen (reine Logik), Bau, Regeln, Pfadstab, Einspeiser
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
