# Craftorio – Ebene Factorio-Welt mit Biomen

Status: **entschieden** (alle Entscheidungen in §9 wie empfohlen). Arbeitsweise, Befehle und Definition of Done wie in
`docs/BACKLOG-Testrunde-1.md` Abschnitt 0.

---

## 1. Ziel

Factorio-Fabriken brauchen **ebene Flächen**: lange Bänder, Reihen von Maschinen, Bahnen von Strommasten. Die
Vanilla-Oberwelt ist hügelig; auf ihr wird eine Fabrik in Factorio-Größe unübersichtlich und mühsam.

Gleichzeitig soll es **kein Flachland wie „Superflat"** werden, sondern eine lebendige Welt wie in Factorio:
- große ebene Flächen als Bauland
- Biome mit eigenen Farben, Böden und Bäumen
- Wälder, Seen, einzelne Flüsse und Felsbrocken
- seltene **Hochebenen mit klaren Kanten**, das Gegenstück zu den Klippen in Factorio

## 2. Befund

| Thema | Stand | Folge |
|---|---|---|
| Gelände | Vanilla-Generator (Hügel, Berge, Ozeane) | kaum ebene Flächen, Bänder müssen ständig steigen und fallen |
| Höhlen- und Minenschicht | Feste Höhenbänder (`CaveLayers`): Oberfläche ab y = 50, Deckgestein 40–49, Höhlen 0–39, Deckgestein −10…−1, Minen −59…−11 | Ein neues Gelände muss oberhalb von y = 50 bleiben |
| Erzfelder | Feature auf der Oberfläche (`OreFieldFeature`, Heightmap), per Biome-Modifier für `#minecraft:is_overworld` | Funktionieren auf jedem Gelände, auf ebenem besonders gut |
| Welt-Preset | keins (README: „jeder normale Welttyp") | Neuer Welttyp nötig |
| Superflat-Warnung | `LayerCheck` warnt bei Superflat | bleibt, wird um eine Info zum neuen Welttyp ergänzt |

## 3. Lösung: eigener Welttyp „Craftorio"

Verworfene Alternativen:

| Alternative | Warum nicht |
|---|---|
| Superflat mit Biomen | eintönig, keine Seen, Kanten oder Landschaft; Schichten fehlen |
| Vanilla-Welt + Planier-Werkzeug | Fabrik-Bau würde zur Erdarbeit; große Flächen wären stundenlange Arbeit |
| Nur die Vanilla-Noise anpassen (JSON) | Flache Ebenen mit **exakten** Kanten und eine garantierte Spawn-Ebene sind mit den Vanilla-Dichtefunktionen kaum machbar |

**Gewählt:** Ein Welt-Preset `craftorio:factory` mit eigener Noise-Einstellung. Die Geländehöhe berechnet eine
**eigene Dichtefunktion in Java**: Sie gibt pro Spalte eine exakte Höhe aus wenigen festen Stufen zurück. Die
Biome kommen weiter von Vanilla (nur Land-Biome), damit Bäume, Gras, Blumen, Böden und Farben stimmen.

## 4. Gelände

### 4.1 Höhenstufen

Jede Spalte hat genau eine der folgenden Oberflächen. Dazwischen gibt es **keine** Hänge, nur senkrechte Kanten.

| Stufe | Höhe (Oberkante) | Anteil (Richtwert) | Aussehen |
|---|---|---|---|
| Seeboden | y = 59–61, Wasser bis y = 63 | ca. 6 % | Seen, Ufer 1–2 Blöcke Sand/Kies |
| Flussbett | y = 60–61, Wasser bis y = 63 | ca. 2 % | 5–9 Blöcke breit, schlängelnd |
| **Grundebene** | **y = 64** | **≥ 75 %** | Bauland, Böden laut Biom |
| Hochebene I | y = 68 | ca. 12 % | Kante 4 Blöcke, Flächen meist ≥ 48 × 48 |
| Hochebene II | y = 72 | ca. 3 % | nur auf Hochebene I, Kante wieder 4 Blöcke |

- **Kanten:** senkrecht, in der Regel genau 4 Blöcke. Sie wirken wie Factorio-Klippen und lassen sich mit
  steigenden Bändern, Aufzügen oder Klippensprengstoff (§6) überwinden.
- **Keine** Ozeane, Berge, Schluchten oder Vanilla-Höhlen an der Oberfläche (Vanilla-Carver aus, Aquifere aus).
- **Spawn-Ebene:** Im Radius von 192 Blöcken um den Weltspawn nur Grundebene, keine Seen, Flüsse oder Hochebenen.
  Starter-Erzfelder (`StarterFields`) liegen dort.

### 4.2 Erzeugung

- Neue Klasse `FactoryTerrain` (reine Logik):
  - Eingabe x, z und drei 2D-Rauschfelder: `craftorio:terrain_plateau`, `craftorio:terrain_lake`, `craftorio:terrain_river`.
    Die Rauschparameter liegen als JSON in `data/craftorio/worldgen/noise/`.
  - Ausgabe: Oberkante als ganze Zahl.
  - Schritte:
    1. Hochebene I, wenn `plateau > 0,55`; Hochebene II, wenn `plateau > 0,80`.
    2. See, wenn `lake < −0,62`; Seeboden-Tiefe aus `lake` gestuft (61/60/59).
    3. Fluss, wenn `|river| < 0,035` (schmale Rinne entlang der Null-Linie, wie Vanilla-Flüsse).
    4. Seen und Flüsse **nur** auf der Grundebene (an Hochebenen wird die Rinne zur Grundebene aufgefüllt).
    5. Spawn-Maske: Innerhalb von 192 Blöcken um (0, 0) immer Grundebene; zwischen 192 und 256 Blöcken nur Grundebene
       oder Hochebene, keine Seen.
  - Kleine Inseln vermeiden: Flächen unter 6 × 6 Blöcken einer Stufe werden der umgebenden Stufe zugeschlagen.
    Das geht per Glättung auf dem Rauschen; ein pixelgenauer Flood-Fill ist im Generator zu teuer.
- Neue Dichtefunktion `craftorio:factory_height` (`DensityFunction`, Codec in `Registries.DENSITY_FUNCTION_TYPE`
  registrieren):
  - Wert = `clamp(H(x, z) − y + 0,5, −1, 1)` mit `H` aus `FactoryTerrain`; positiv heißt fest.
  - Die Rauschfelder kommen über `DensityFunction.NoiseHolder`, damit `RandomState` sie mit dem Welt-Seed belegt.
  - Ergebnis pro Spalte (x, z) cachen, weil der Generator jede Spalte für viele y abfragt.
- Noise-Einstellung `craftorio:factory` per Datagen in Java (`DatapackBuiltinEntriesProvider`, Registry
  `NOISE_SETTINGS`):
  - Basis: Werte von Vanilla-Overworld (`NoiseSettings.OVERWORLD_NOISE_SETTINGS`, Stein, Wasser, Meeresspiegel 63).
  - Router: Vanilla-Overworld-Router (`NoiseRouterData`) für Temperatur, Feuchte usw. (die Biomwahl braucht sie),
    aber `initialDensityWithoutJaggedness` und `finalDensity` = `craftorio:factory_height`.
  - Aquifere aus, Erzadern aus.
  - Oberflächenregeln: `SurfaceRuleData.overworld()` aus Vanilla, damit Wüste Sand, Taiga Podsol, verschneite Ebene
    Schnee usw. bekommt.
- Der Höhlen-Generator (`cave_layers`) bleibt unverändert und füllt wie bisher alles unter y = 50.

## 5. Biome

Eigene Biomquelle (`multi_noise` mit fester Parameterliste) **nur mit Land-Biomen**:
- Die Auswahl hängt nur von Temperatur und Feuchte ab.
- Kontinentalität, Erosion, Tiefe und „Weirdness" decken jeweils den ganzen Bereich ab. So entstehen nie
  Ozean- oder Gebirgsbiome.

| Temperatur → / Feuchte ↓ | kalt | kühl | mild | warm | heiß |
|---|---|---|---|---|---|
| trocken | verschneite Ebene | Ebene | Ebene | Savanne | Wüste |
| mittel | verschneite Taiga | Taiga | Wald | Savanne | Wüste |
| feucht | verschneite Taiga | Taiga mit Riesenfichten (selten) | Birkenwald / Blumenwald | Dschungelrand (selten) | Tafelberge-Boden ohne Tafelberge* |
| sehr feucht | Taiga | dunkler Wald | Sumpf | Dschungelrand | Savanne |

\* Für „heiß + feucht" ein mildes Biom mit rotem Sand ohne Erhebungen. Gibt es kein passendes Vanilla-Biom ohne
eigene Erhebungen, die Wüste nehmen.

- **Biomgröße:** Vanilla-Temperatur- und Feuchte-Rauschen; Biome sind einige hundert Blöcke groß.
- **Bäume:**
  - Vanilla-Features je Biom (Wälder dicht, Ebenen spärlich), wie die Wälder in Factorio.
  - Bäume wachsen auf allen Stufen, aber nicht auf Kanten: Kantenblöcke bekommen kein Feature.
  - Am Spawn (Radius 64) keine dichten Wälder: Waldbiome werden dort durch Ebene ersetzt.
- **Tiere** wie Vanilla (feindliche Mobs sind ohnehin aus).
- **Strukturen:** Dörfer, Ruinen und Wegemarken dürfen erscheinen (Dekoration). Ozean-, Unterwasser-, Stronghold-,
  Mineshaft- und Ancient-City-Strukturen sind aus (keine Ozeane; der Untergrund gehört den Schichten).

## 6. Werkzeuge für den Rest

Factorio-Vorbild 1:1. Die Werte sind gegen wiki.factorio.com geprüft (Stand 1.1; die Wiki-Historie nennt die Änderungen der Version 2.0):

| Item | Rezept | Wirkung | Forschung |
|---|---|---|---|
| **Landfüller** | 0,5 s · 20 Stein → 1 *(1.1; seit Factorio 2.0.7 sind es 50 Stein, hier gilt 1.1)* | Rechtsklick auf Wasser füllt ein **3×3**-Feld bis zur Wasseroberfläche (Oberkante y = 63) mit Erde, oben Gras bzw. Biom-Boden | „Landfüller" 50 × R+G, 30 s |
| **Sprengstoff** | 4 s · 1 Kohle + 1 Schwefel + 10 Wasser → 2 | Zwischenprodukt | „Explosivstoffe" 100 × R+G, 15 s |
| **Klippensprengstoff** | 8 s · 10 Sprengstoff + 1 Granate + 1 Fass *(Fass gibt es nicht: durch 1 Stahl ersetzen)* | Wurf oder Rechtsklick auf eine Kante: senkt ein **5×5**-Feld der Hochebene auf die angrenzende tiefere Stufe ab; Abraum fällt als Stein | „Klippensprengstoff" 200 × R+G (ohne blaue Packs), 15 s; Voraussetzungen „Explosivstoffe" und „Militär 2" |

**Felsbrocken** (Factorio-Felsen) als Dekoration:
- groß: 3×3×2 aus Stein, Andesit und Moosbruchstein; abgebaut 24–50 Stein + 10–25 Kohle
- klein: 1–2 Blöcke, 5–10 Stein

Häufigkeit 1 pro 2–4 Chunks, nicht im Spawn-Radius. Abbau mit der Starter-Spitzhacke wie Erzfelder (Block verschwindet).

## 7. Welttyp auswählen

- **Einzelspieler:**
  - Preset `craftorio:factory` mit Namen „Craftorio" (EN/DE), in den Tag `minecraft:normal` aufnehmen, damit er unter
    „Welttyp" erscheint.
  - Damit er **vorausgewählt** ist: Client-Event `ScreenEvent.Init.Post` für `CreateWorldScreen` → Welttyp einmalig
    auf `craftorio:factory` setzen (`WorldCreationUiState`). Abschaltbar per Client-Config.
- **Dedizierter Server:** In `server.properties` `level-type=craftorio\:factory` setzen. README und die
  Server-Anleitung ergänzen.
- `LayerCheck` erweitern: Ist die Welt kein Craftorio-Welttyp, gibt es beim Beitritt eines Operators einen Hinweis
  (keine Warnung), dass der Craftorio-Welttyp für ebene Flächen empfohlen ist.
- **Bestehende Welten** bleiben, wie sie sind; nur neu erzeugte Chunks einer Craftorio-Welt bekommen das neue Gelände.

## 8. Pakete (Reihenfolge, je ein Commit)

| Paket | Inhalt | Akzeptanz |
|---|---|---|
| **W1 – Geländegenerator** ✅ | `FactoryTerrain`, Rauschfelder, Dichtefunktion `factory_height`, Noise-Einstellung `craftorio:factory` (Datagen), Carver/Aquifere/Erzadern aus, Spalten-Cache | Unit-Test `FactoryTerrain` über 2.048 × 2.048 Spalten (fester Seed): ≥ 75 % Grundebene; nur die Höhen aus §4.1; Spawn-Radius 192 nur Grundebene; keine Fläche einer Stufe < 6 × 6 (Stichprobe). Laufzeit < 50 ns pro Spalte |
| **W2 – Biome und Welttyp** ✅ | Biomquelle nur Land (§5), World-Preset `craftorio:factory`, Vorauswahl im Erstellen-Bildschirm, Server-Doku, `LayerCheck`-Hinweis, Strukturen-Auswahl | GameTest bzw. Server-Start mit `level-type=craftorio:factory`: Chunks um den Spawn haben Oberfläche y = 64; in 1.024 × 1.024 Blöcken mindestens 6 verschiedene Biome, kein Ozean |
| **W3 – Kompatibilität** | Höhlen- und Minenschicht unverändert (Test: unter y = 50 Deckgestein und Füllung), Erzfelder liegen eben, Starter-Felder in der Spawn-Ebene, Höhleneingang auf y = 64 setzbar, Arena-Dimension unberührt | GameTests für Schichten, Starter-Felder und Höhleneingang in einer Craftorio-Welt |
| **W4 – Werkzeuge** | Landfüller, Sprengstoff, Klippensprengstoff, Forschungen, Rezepte (Rezepttabellen-Test erweitern) | GameTests: Landfüller füllt 3×3 Wasser; Klippensprengstoff senkt 5×5 einer Hochebene ab |
| **W5 – Dekoration und Feinschliff** | Felsbrocken, keine Bäume auf Kanten, ausgedünnte Wälder am Spawn, Ufer, Handbuch-Seite „Die Welt", README mit Screenshots (Grundebene, Hochebene mit Kante, See, drei Biome) | Client-Prüfung mit Screenshots, alle Tests grün |

## 9. Entscheidungen (getroffen: alle wie empfohlen)

| ID | Frage | Entscheidung |
|---|---|---|
| W-E1 | Flüsse ja/nein? | **Ja, selten** (ca. 2 % der Fläche): geben der Welt Struktur; Landfüller und Unterflurbänder überbrücken sie |
| W-E2 | Hochebenen mit 4-Block-Kanten? | **Ja**, selten (I ca. 12 %, II ca. 3 %); Server-Config `terrain.plateaus = none/rare/normal` (Standard `rare`) |
| W-E3 | Craftorio-Welttyp vorausgewählt? | **Ja** (abschaltbar per Client-Config); auf Servern per `level-type` |
| W-E4 | Vanilla-Strukturen? | Nur Land-Dekoration (Dörfer, Ruinen); Untergrund- und Ozeanstrukturen aus |
| W-E5 | Bestehende Welten umwandeln? | **Nein**; der Testdurchlauf startet in einer neuen Craftorio-Welt |
| W-E6 | Größe der Spawn-Ebene? | **192 Blöcke Radius** (garantiert eben, ohne Wasser); Config `terrain.spawnRadius` |

### Umsetzungsstand

- **W1 ✅:** `FactoryTerrain` (reine Logik, 4×4-Zellen, Mehrheitsglättung) mit `FactoryNoise`; Dichtefunktion
  `craftorio:factory_height` (`FactoryHeight`, Seed aus der Noise `craftorio:terrain_seed`), Noise-Einstellung
  `craftorio:factory` per Datagen (Vanilla-Klima, Aquifere, Erzadern und Rauschhöhlen aus, Meeresspiegel 64, Wasser
  bis y = 63). Config `terrain.plateaus` und `terrain.spawnRadius`. Die Vanilla-Carver hängen an den Biomen und werden
  in W2 mit der Biomquelle abgeschaltet.
- **W2 ✅:** Chunk-Generator `craftorio:factory` (`FactoryChunkGenerator`, ein `NoiseBasedChunkGenerator` ohne Carver; die
  Struktursätze Mineshafts, Festungen, Ancient Cities, Trial Chambers, Ozean-Ruinen/-Monumente, Wracks und vergrabene
  Schätze fehlen); Biomquelle `multi_noise` mit fester Liste aus dem Biomtabellen-Raster (nur Land, Temperatur × Feuchte);
  Welt-Preset `craftorio:factory` (Netherwelt und End wie Vanilla) im Tag `minecraft:normal`; Vorauswahl im
  Erstellen-Bildschirm (Client-Config `world.preselectWorldType`); `LayerCheck`-Hinweis für Operatoren. Bewusst auf W5
  verschoben: Ebene statt dichter Wälder im Spawn-Radius 64 (gehört zur Ausdünnung am Spawn).
