# Craftorio – Spielkonzept & Umsetzungsplan

> Status: Entwurf v0.1 – Grundlage für die Umsetzung. Offene Entscheidungen sind in Abschnitt 11 gesammelt.

## 1. Vision in einem Satz

Craftorio macht Minecraft zu einem **wirtschaftsgetriebenen Factorio**: Man baut Förderanlagen und Produktionsketten, verkauft veredelte Waren für Geld, investiert das Geld in neue Technik und tiefere Erdschichten – und verteidigt die wachsende Fabrik mit Türmen gegen immer stärkere Angriffswellen. Allein oder im Team.

## 2. Der Kern-Loop

```
  Rohstoffe fördern ──► verarbeiten ──► verkaufen ──► Geld
        ▲                                               │
        │                                               ▼
  neue Schichten  ◄── Technologie / Maschinen / Türme kaufen
        │
        └──► mehr Produktion = mehr „Aufmerksamkeit“ ──► stärkere Angriffswellen
```

Drei Säulen, die sich gegenseitig antreiben:

| Säule | Was der Spieler tut | Was es der anderen Säule bringt |
|---|---|---|
| **Automatisierung** | Bohrer, Bänder, Maschinen, Produktionsketten | Waren zum Verkaufen, Munition & Bauteile für Türme |
| **Wirtschaft** | Verkaufen, Kaufen, Freischalten | Zugang zu Maschinen, Schichten, Türmen |
| **Tower Defense** | Türme bauen, versorgen, aufrüsten | Schutz der Fabrik; Belohnungen (Beute, Geldboni) |

Designprinzip: **Handarbeit ist möglich, aber schlecht bezahlt.** Mit der Spitzhacke gewonnene Erze geben nur einen Bruchteil; der Wert entsteht durch Automatisierung und Veredelung.

## 3. Welt & Schichten (Progression nach unten)

### 3.1 Aufbau

Wir nutzen **eine einzige Dimension (Overworld)** mit fest definierten Höhenschichten, getrennt durch unzerstörbares Gestein. Das hält Logistik (vertikale Förderung), Chunk-Loading und Multiplayer einfach.

| Schicht | Höhe (Y, Vorschlag) | Zugang | Rohstoffe (Beispiele) |
|---|---|---|---|
| **Oberfläche** | 50 … 320 | von Beginn an | Holz, Stein, Kohle, Eisen, Kupfer, Sand, Lehm |
| *Deckgestein I* | 40 … 50 | unzerstörbar | – |
| **Höhlen** | 0 … 40 | über gebauten **Höhleneingang** | Zinn, Gold, Blei, Schwefel, Quarz, Grundwasser/Öl |
| *Deckgestein II* | −10 … 0 | unzerstörbar | – |
| **Minen** | −64 … −10 | über gebauten **Minenschacht** (in den Höhlen) | Diamant, Titan, Uran, seltene Erden, Kristalle |

- **Deckgestein** („Craftorio-Grundgestein“): Härte −1, extrem hohe Explosionsresistenz, Tag `minecraft:wither_immune`, `#c:relocation_not_supported` / nicht von Kolben verschiebbar. Kein Durchgraben, kein Enderperlen-Trick (Teleport-Check beim Landen unterhalb einer nicht freigeschalteten Schicht).
- **Worldgen:** Eigene `NoiseSettings`/Density-Functions erzeugen die Oberfläche normal, darunter die Deckschichten und große, begehbare Höhlensysteme bzw. Minengänge.

### 3.2 Höhleneingänge & Minenschächte

- Ein **Höhleneingang** ist eine Multiblock-Baustelle. Man kauft den „Bauplan“ (Geld) und liefert Materialien an (z. B. 200 Stein, 100 Eisenplatten, 20 Stützbalken). Danach „bohrt“ die Baustelle über Zeit einen Schacht durch Deckgestein I.
- Jeder Eingang **erschließt einen Bereich** (Vorschlag: Radius 3 Chunks = 7×7 Chunks) in der Höhlenschicht. Außerhalb dieses Radius bleibt die Höhle durch **„Geröll-Barrieren“** unpassierbar. Weitere Eingänge oder das Kaufen von „Stollenerweiterungen“ vergrößern den Bereich → Expansion à la Factorio.
- Der Schacht enthält automatisch eine **Leiter/Aufzug** und **zwei Förderschacht-Slots**, über die Bänder Güter nach oben/unten transportieren.
- Der **Minenschacht** funktioniert identisch, nur eine Stufe tiefer, teurer und mit Bauteilen aus der Höhlenstufe.

### 3.3 Rohstoffvorkommen („Erzfelder“)

- Rohstoffe liegen nicht als verstreute Erzblöcke vor, sondern als **Erzfelder** (Patch aus 20–200 Blöcken), chunk-basiert generiert – wie in Factorio.
- Jeder Feldblock hat einen **Vorrat** (Block-Entity oder Chunk-Daten): z. B. 500–5 000 Einheiten. Leer → wird zu „erschöpftem Gestein“.
- Felder weiter vom Spawn sind **ergiebiger** (Factorio-Prinzip: Expansion lohnt sich).
- In tieferen Schichten optional **unendliche Quellen** mit sinkender Rate (Öl, Grundwasser), um Langzeitfabriken zu ermöglichen.

## 4. Automatisierung

### 4.1 Maschinen (Stufe 1 → 3)

| Kategorie | Stufe 1 (Oberfläche) | Stufe 2 (Höhlen) | Stufe 3 (Minen) |
|---|---|---|---|
| Förderung | Brenner-Bohrer | Elektrischer Bohrer, Pumpe | Tiefenbohrer, Laser-Bohrer |
| Transport | Förderband, Greifarm, Kiste | Schnellband, Splitter, Unterflurband, Rohre | Expressband, Filter-Greifarm, Logistikkisten |
| Verarbeitung | Schmelzofen, Säge, Presse | Montagemaschine, Chemieanlage, Raffinerie | Präzisionsfabrik, Zentrifuge, Kristallformer |
| Energie | Kohle-Generator, Strommast | Dampfturbine, Akku | Reaktor, Solarfeld |

- **Energie** als eigenes Stromnetz (Masten mit Reichweite, Factorio-Stil), intern auf **FE (Forge Energy)**-Basis, damit andere Mods kompatibel bleiben.
- **Förderbänder** sind eigene Blöcke mit sichtbaren Items (2 Spuren wie in Factorio). Das ist technisch der aufwendigste, aber identitätsstiftendste Teil.
- Alle Rezepte sind **Datapack-JSON**, damit Balancing ohne Code-Änderungen möglich ist.

### 4.2 Produktionsketten (Beispiel Eisen)

```
Eisenerz ─► Schmelzofen ─► Eisenbarren ─► Presse ─► Eisenplatte ─┬─► Zahnrad
                                                                 └─► Stahl (mit Kohle) ─► Stahlträger
Zahnrad + Kupferkabel + Eisenplatte ─► Montagemaschine ─► Motor
```

## 5. Wirtschaft

### 5.1 Währung

- **Teamkonto statt Münz-Items**: Geld ist eine Zahl im Speicher des Teams (Server-seitig, `SavedData`). Keine Dupe-Bugs, einfach für Coop.
- Anzeige im HUD, Transaktionslog im Terminal.

### 5.2 Verkaufen

- **Handelsposten** (Block): Items per Band/Greifarm hinein → werden sofort gutgeschrieben. Später **Frachtbahnhof/Frachtdrohne** für größere Mengen mit Bonus.
- **Wertschöpfung pro Stufe:** Verkaufspreis ≈ Summe der Inputpreise × **1,3–1,6** + Energieanteil. Dadurch lohnt sich jede Verarbeitungsstufe, und tiefe Ketten sind am profitabelsten.

| Ware | Stufe | Preis (Beispiel) |
|---|---|---|
| Eisenerz | 0 | 1 ¢ |
| Eisenbarren | 1 | 2 ¢ |
| Eisenplatte | 2 | 3 ¢ |
| Zahnrad (2 Platten) | 3 | 9 ¢ |
| Motor | 4 | 60 ¢ |
| Stahlträger | 4 | 45 ¢ |

- **Marktsättigung (optional, empfohlen):** Wer eine Ware in großen Mengen verkauft, drückt ihren Preis temporär (erholt sich über Zeit). Motiviert diverse Produktion statt „nur Zahnräder“. Per Config abschaltbar.
- **Aufträge/Kontrakte:** Zeitlich begrenzte Bestellungen („Liefere 500 Motoren in 2 Ingame-Tagen“) mit Bonus – gibt Ziele und Abwechslung.

### 5.3 Kaufen & Freischalten

Zentrales **Terminal** (Block + GUI) mit drei Tabs:

1. **Technologie-Baum** – Freischaltungen kosten Geld (+ ab Stufe 2 zusätzlich eingelieferte Bauteile, damit man nicht nur „grindet“, sondern die Produktion ausbauen muss).
2. **Shop** – freigeschaltete Maschinen, Bänder, Türme kaufen (als Item geliefert). Selbst herstellen ist günstiger, aber aufwendig → echte Entscheidung.
3. **Aufträge & Statistik** – Kontrakte, Einnahmen/Ausgaben, Produktionsrate.

## 6. Tower Defense

### 6.1 Bedrohung

- **Aufmerksamkeit** (analog zu Factorios Verschmutzung/Evolution): steigt mit Produktion, Stromverbrauch und Umsatz; sinkt langsam über Zeit.
- **Nester** entstehen in Entfernung zur Basis (Oberfläche) bzw. in nicht erschlossenen Höhlenbereichen. Sie senden **Wellen** aus, deren Stärke von der Aufmerksamkeit abhängt.
- Ziel der Gegner: Maschinen, Bänder, Strommasten und der **Basiskern** (Terminal). Zerstörte Blöcke hinterlassen eine „Ruine“, die man gegen Geld/Material reparieren kann (kein Totalverlust – frustfrei).
- Ankündigung: Wellen-Timer im HUD, Richtung wird angezeigt. **Friedlicher Modus** per Config/Weltoption.

### 6.2 Gegner (eigene Entities)

| Gegner | Schicht | Besonderheit |
|---|---|---|
| Krabbler | Oberfläche | schnell, schwach, in Schwärmen |
| Brecher | Oberfläche | greift gezielt Mauern & Bänder an |
| Höhlenwurm | Höhlen | gräbt sich unter Mauern durch |
| Kristallgolem | Minen | gepanzert, nur mit Spezialmunition effektiv |
| Brutmutter | alle (Boss) | spawnt Nester, droppt seltene Belohnungen |

### 6.3 Türme

- Türme werden **mit Geld gekauft** (Shop) und benötigen **Munition/Energie aus der Fabrik** – hier greift die Automatisierung direkt ein (Munition per Band/Greifarm).
- **Upgrades** (Stufe I–V) kosten Geld **plus verarbeitete Bauteile** (z. B. Motor, Stahlträger, Kristalllinse).
- **Module** in Slots: Reichweite, Feuerrate, Schadenstyp (Feuer, Frost, Durchschlag).

| Turm | Munition | Rolle |
|---|---|---|
| Armbrustturm | Bolzen (Holz+Eisen) | Einstieg |
| Geschützturm | Patronen (Kupfer+Eisen+Schwefel) | Allrounder |
| Flammenwerfer | Öl/Treibstoff (Rohr) | Flächenschaden |
| Tesla-Turm | Strom | kein Nachschub, hoher Verbrauch |
| Mörser | Granaten | Reichweite, gegen Nester |
| Laser-Turm | Strom + Kristalle | Endgame, gegen Panzerung |

Zusätzlich: **Mauern & Tore** (kaufbar, aufrüstbar), **Reparaturdrohnen-Station** (später).

## 7. Coop & Solo

- **Teams**: Spieler gründen/treten einem Team bei (`/craftorio team …` + GUI). Das Team teilt **Konto, Technologien, Erschlossene Bereiche und Aufmerksamkeit**.
- **Solo** = Team mit einem Mitglied, kein Sonderpfad.
- **Mehrere Teams auf einem Server** möglich (kooperativ oder im Wettbewerb um Aufträge). Rechte pro Team: Maschinen/Kisten fremder Teams sind geschützt.
- **Skalierung**: Wellenstärke skaliert moderat mit Anzahl aktiver Teammitglieder (z. B. +35 % pro weiterem Spieler), damit Coop nicht trivial wird.
- **Technik**: Server ist autoritativ (Geld, Freischaltungen, Wellen); Client bekommt nur Sync-Pakete. Funktioniert im Singleplayer (integrierter Server), LAN und dedizierten Servern.

## 8. Minecraft-Version & Mod-Loader

### Empfehlung: **Minecraft 1.21.1 + NeoForge (Java 21)**

Begründung:

- **1.21.1 ist die aktuelle „Langzeit-Modding-Version“**: Die meisten großen Mods (Create 6, Mekanism, AE2, JEI/EMI, FTB-Suite, GeckoLib …) haben dort stabile Releases. Neuere Versionen (1.21.2+) ändern Rendering, Items/Components und Worldgen in jedem Drop – ständiges Hinterherportieren kostet viel Zeit.
- **NeoForge** statt Fabric: Für ein großes Mod mit Maschinen, Energie (FE-Capabilities), Item-/Fluid-Handlern, eigener Worldgen und vielen GUIs bietet NeoForge die passenden APIs „out of the box“. Die Tech-Mod-Szene (mit der wir kompatibel sein wollen) lebt überwiegend dort.
- **Werkzeuge**: ModDevGradle, Parchment-Mappings, Data-Generatoren für Rezepte/Loot/Models/Sprachdateien, GameTests für automatisierte Tests.

Portierung auf eine neuere Version ist später machbar, wenn der Kern steht. Unmittelbar vor dem Projekt-Setup prüfen wir die aktuellen Versionsstände noch einmal kurz.

## 9. Bestehende Mods

### 9.1 Abhängigkeiten (werden benötigt)

| Mod | Wofür |
|---|---|
| **GeckoLib** | Animierte Gegner (Krabbler, Golems) und Türme (drehende Geschütze) |

Bewusst **wenige harte Abhängigkeiten**: Alles, was den Kern-Loop betrifft (Bänder, Wirtschaft, Schichten, Türme), bauen wir selbst, weil Preise, Freischaltungen und Balancing sonst nicht kontrollierbar sind.

### 9.2 Empfohlen im Modpack / mit Integration

| Mod | Wofür | Integration |
|---|---|---|
| **EMI** (oder JEI) | Rezeptanzeige | Plugin: eigene Rezepttypen + Verkaufspreis im Tooltip |
| **Jade** | Blick-Tooltips | Plugin: Maschinenfortschritt, Erzfeld-Vorrat, Turm-Munition |
| **FTB Teams** *(optional)* | Team-Verwaltung | Wenn vorhanden, Teams übernehmen; sonst eigenes Teamsystem |
| **FTB Quests** *(optional)* | Tutorial/Story-Quests | Quest-Buch für den Einstieg, Belohnungen in Geld |
| **Sodium/Embeddium, ModernFix, FerriteCore** | Performance | keine Integration nötig |
| **Create** *(später optional)* | Beliebte Mechanik-Mod | Addon-Kompat: Create-Items verkaufbar, Energie konvertierbar |

**Nicht empfohlen**: Worldgen-Mods (Terralith, Tectonic, Biomes O' Plenty) – sie kollidieren mit unserer Schichten-Worldgen. Große Tech-Mods (Mekanism, Thermal) nur mit Vorsicht: Sie umgehen sonst die Progression (z. B. eigene Quarries). Dafür gibt es eine Config-Blacklist für „Durchbrechen“ des Deckgesteins.

## 10. Technische Architektur

### 10.1 Module (Java-Pakete)

```
de.craftorio
├── core         Registrierung, Config, Netzwerk, Team-/Weltdaten (SavedData)
├── economy      Konto, Preistabelle (Datapack), Markt, Aufträge, Terminal-GUI
├── progression  Technologie-Baum (Datapack), Freischaltungen, Rezept-Sperren
├── world        Schichten-Worldgen, Deckgestein, Erzfelder, Eingänge/Schächte, Bereichs-Freischaltung
├── logistics    Förderbänder, Greifarme, Splitter, Rohre, Kisten
├── machines     Bohrer, Öfen, Montage, Energie-Netz
├── defense      Aufmerksamkeit, Nester, Wellen-Director, Gegner, Türme, Mauern
└── compat       EMI, Jade, FTB Teams/Quests (nur wenn geladen)
```

### 10.2 Datengetrieben

Alles Balancing-relevante liegt als **Datapack-JSON** vor und ist per `/reload` änderbar:

- `data/craftorio/prices/*.json` – Verkaufspreise
- `data/craftorio/tech/*.json` – Technologie-Knoten (Kosten, Voraussetzungen, Freischaltungen)
- `data/craftorio/waves/*.json` – Wellenzusammensetzung pro Aufmerksamkeits-Stufe
- `data/craftorio/ore_fields/*.json` – Erzfeld-Definitionen pro Schicht
- `data/craftorio/recipe/*.json` – Maschinenrezepte

### 10.3 Wichtige technische Risiken

| Risiko | Gegenmaßnahme |
|---|---|
| Performance von Bändern mit vielen Items | Item-Bewegung als Daten (kein Entity pro Item), Batch-Rendering, Tick nur bei Änderungen |
| Wellen in ungeladenen Chunks | Wellen nur, wenn Basis geladen ist; Team-Chunkloader am Terminal |
| Worldgen-Kompatibilität | Eigener Welt-Preset „Craftorio“, damit Vanilla-Welten unberührt bleiben |
| Umgehen des Deckgesteins | Unzerstörbar, nicht verschiebbar, Teleport-/Enderperlen-Check, Config-Blacklist |

## 11. Offene Entscheidungen (bitte bestätigen)

1. **Ein Dimensionsansatz (Schichten in der Overworld)** statt separater Dimensionen für Höhlen/Minen? *(Empfehlung: ja)*
2. **Erzfelder endlich** (mit Vorrat) oder **unendlich** mit Rate? *(Empfehlung: endlich an der Oberfläche, gemischt tiefer)*
3. **Eigene Förderbänder** (Aufwand hoch, volle Kontrolle) oder auf **Create** aufbauen? *(Empfehlung: eigene)*
4. **Eigenes Teamsystem** mit optionaler FTB-Teams-Kompat? *(Empfehlung: ja)*
5. **Marktsättigung** aktiv per Default? *(Empfehlung: ja, moderat)*
6. **Vanilla-Crafting** von Werkzeugen/Rüstung: normal lassen oder einschränken?
7. Wie „hart“ sollen Angriffe sein – Blöcke zerstören (mit Ruinen-Reparatur) oder nur Schaden an Maschinen-HP?
8. Name der Währung (Vorschlag: **Credits, „¢“**).

## 12. Umsetzungs-Roadmap

| Meilenstein | Inhalt | Ergebnis |
|---|---|---|
| **M0 – Setup** | NeoForge 1.21.1 Projekt, ModDevGradle, Datagen, CI (GitHub Actions Build) | Leere Mod startet im Client & Server |
| **M1 – Wirtschaftskern** | Team-Konto, Preistabelle, Handelsposten, HUD, Befehle | Items verkaufen → Geld |
| **M2 – Oberfläche** | Erzfelder, Brenner-Bohrer, Förderband, Greifarm, Kiste | Erste Automatisierungsschleife |
| **M3 – Verarbeitung & Energie** | Schmelzofen, Presse, Montagemaschine, Stromnetz | Produktionsketten mit Wertschöpfung |
| **M4 – Terminal & Tech-Baum** | Terminal-GUI, Shop, Technologie-Freischaltungen | Vollständiger Wirtschafts-Loop |
| **M5 – Tower Defense I** | Aufmerksamkeit, Nester, 2 Gegner, 3 Türme, Mauern | Verteidigung an der Oberfläche |
| **M6 – Höhlenschicht** | Schichten-Worldgen, Deckgestein, Höhleneingang, Bereichs-Freischaltung, Höhlen-Ressourcen & -Gegner | Zweite Stufe spielbar |
| **M7 – Minenschicht** | Minenschacht, Endgame-Ressourcen, Stufe-3-Maschinen & -Türme, Boss | Vollständige Progression |
| **M8 – Coop & Polish** | Mehrere Teams, Rechte, Aufträge, Balancing, Kompat (EMI/Jade), Quests | Erste spielbare Beta |

Jeder Meilenstein endet mit einem spielbaren Stand. Nächster Schritt nach Freigabe dieses Konzepts: **M0 – Projekt-Setup**.
