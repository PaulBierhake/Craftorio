# Craftorio – Spielkonzept & Umsetzungsplan

> Status: v0.3 – alle Grundsatzentscheidungen getroffen (Abschnitte 11 und 12). Umsetzung läuft.

## 1. Vision in einem Satz

Craftorio macht Minecraft zu einem **wirtschaftsgetriebenen Factorio**: Man baut Förderanlagen und Produktionsketten, verkauft veredelte Waren für Geld und investiert es in neue Technik und tiefere Erdschichten. In einer eigenen **Tower-Defense-Zone** spielt man parallel Level für Level gegen immer stärkere Wellen – und genau diese Level liefern die Schlüsselmaterialien, ohne die die Fabrik nicht auf die nächste Stufe kommt. Allein oder im Team.

## 2. Der Kern-Loop

```
  Erzfelder erschließen ──► verarbeiten ──► verkaufen ──► Geld
        ▲                        │                          │
        │                        │ Bauteile/Munition        ▼
        │                        ▼                 Maschinen, Türme, Freischaltungen
  neue Fabrik-Stufe ◄── Schlüsselmaterial ◄── TD-Level (alle 10 Level)
                                                    │
                        Level verloren ─────────────┘
                        → Türme reparieren/ausbauen → Fabrik muss mehr leisten
```

Drei Säulen, die sich gegenseitig antreiben:

| Säule | Was der Spieler tut | Was es der anderen Säule bringt |
|---|---|---|
| **Automatisierung** | Erzfelder erschließen, Bänder, Maschinen, Produktionsketten | Waren zum Verkaufen, Bauteile & Munition für Türme |
| **Wirtschaft** | Verkaufen, Kaufen, Freischalten | Zugang zu Maschinen, Schichten, Türmen, Reparaturen |
| **Tower Defense** | Level spielen, Türme bauen, reparieren, aufrüsten | Geld pro Level, **Schlüsselmaterialien** alle 10 Level |

Designprinzipien:

- **Handarbeit ist möglich, aber schlecht bezahlt.** Wert entsteht durch Automatisierung und Veredelung.
- **Wachstum statt Umbau.** Keine Marktsättigung – eine laufende Produktion bleibt immer profitabel. Fortschritt entsteht, weil höherwertige Produkte *mehr und verschiedenere* Rohstoffe brauchen und Erzfelder einen festen Maximaldurchsatz haben. Man muss also ständig **erweitern**, nie umbauen.
- **Die Fabrik ist sicher.** Die Tower-Defense greift nie die Fabrik an. Druck entsteht durch verlorene Level, nicht durch Zerstörung.

## 3. Welt & Schichten (Progression nach unten)

### 3.1 Aufbau

**Eine einzige Dimension (Overworld)** mit fest definierten Höhenschichten, getrennt durch unzerstörbares Gestein. Das hält Logistik (vertikale Förderung), Chunk-Loading und Multiplayer einfach. *(Entschieden)*

| Schicht | Höhe (Y, Vorschlag) | Zugang | Rohstoffe (Beispiele) |
|---|---|---|---|
| **Oberfläche** | 50 … 320 | von Beginn an | Holz, Stein, Kohle, Eisen, Kupfer, Sand, Lehm |
| *Deckgestein I* | 40 … 50 | unzerstörbar | – |
| **Höhlen** | 0 … 40 | über gebauten **Höhleneingang** | Zinn, Gold, Blei, Schwefel, Quarz, Öl |
| *Deckgestein II* | −10 … 0 | unzerstörbar | – |
| **Minen** | −64 … −10 | über gebauten **Minenschacht** (in den Höhlen) | Diamant, Titan, Uran, seltene Erden, Kristalle |

- **Deckgestein**: Härte −1, extrem hohe Explosionsresistenz, nicht von Kolben verschiebbar, Wither-immun. Kein Durchgraben, kein Enderperlen-Trick (Teleport-Check beim Landen unterhalb einer nicht freigeschalteten Schicht).
- **Worldgen:** Eigene Noise-Settings/Density-Functions über ein Welt-Preset „Craftorio“: normale Oberfläche, darunter die Deckschichten und große, begehbare Höhlensysteme bzw. Minengänge.

### 3.2 Höhleneingänge & Minenschächte

- Ein **Höhleneingang** ist eine Multiblock-Baustelle. Voraussetzung: Technologie freigeschaltet (Geld) **und** das passende **Schlüsselmaterial aus der Tower-Defense** (siehe 6.4). Dann Materialien anliefern (z. B. 200 Stein, 100 Eisenplatten, 20 Stützbalken); die Baustelle bohrt über Zeit den Schacht durch Deckgestein I.
- Jeder Eingang **erschließt einen Bereich** (Vorschlag: 7×7 Chunks) in der Höhlenschicht. Außerhalb bleibt die Höhle durch **Geröll-Barrieren** unpassierbar. Weitere Eingänge bzw. „Stollenerweiterungen“ vergrößern den Bereich.
- Der Schacht enthält eine **Leiter/Aufzug** und **zwei Förderschacht-Slots** für Bänder nach oben/unten.
- Der **Minenschacht** funktioniert identisch, eine Stufe tiefer, teurer, mit Bauteilen aus der Höhlenstufe und einem höheren Schlüsselmaterial.

### 3.3 Erzfelder – unendlich, aber mit festem Maximaldurchsatz *(entschieden)*

- Rohstoffe liegen als **Erzfelder** (zusammenhängender Patch aus Erzfeld-Blöcken), chunk-basiert generiert.
- **Unendlich:** Erzfeld-Blöcke erschöpfen nie.
- **Begrenzt durch Größe:** Ein Bohrer baut eine feste Fläche ab (z. B. 3×3 Blöcke, Stufe-3-Bohrer 5×5) und **Abbauflächen dürfen sich nicht überlappen**. Ein Erzfeld-Block kann also nur von genau einem Bohrer genutzt werden.
  → Jedes Feld hat einen **harten Maximaldurchsatz** = (Anzahl Blöcke) × (Abbaurate pro Block der besten Bohrer-Stufe). Ist ein Feld voll bebaut, hilft nur ein **neues Feld**.
- Bohrer-Upgrades erhöhen die Rate pro Block (Stufe 1 Brenner-Bohrer: 0,03/s pro Block = 0,27/s bei 3×3; Stufe 2: ≈0,06/s; Stufe 3: ≈0,12/s – Richtwerte), aber nie über die Feldgröße hinaus.
- **Feldgrößen:** klein nahe Spawn (≈ 20–40 Blöcke), größer mit Entfernung (≈ 60–150 Blöcke). Tiefere Schichten: seltener, aber wertvoller.
- **Fluide** (Öl, Wasser) als Quellen mit fester Förderrate pro Pumpe und begrenzter Anzahl Pumpenplätze – gleiche Logik.
- Anzeige per Jade/Scanner: Feldgröße, belegte Blöcke, aktueller und maximaler Durchsatz.

## 4. Automatisierung

### 4.1 Maschinen (Stufe 1 → 3)

| Kategorie | Stufe 1 (Oberfläche) | Stufe 2 (Höhlen) | Stufe 3 (Minen) |
|---|---|---|---|
| Förderung | Brenner-Bohrer | Elektrischer Bohrer, Pumpe | Tiefenbohrer |
| Transport | Förderband, Greifarm, Kiste | Schnellband, Splitter, Unterflurband, Rohre | Expressband, Filter-Greifarm, Logistikkisten |
| Verarbeitung | Schmelzofen, Säge, Presse | Montagemaschine, Chemieanlage, Raffinerie | Präzisionsfabrik, Zentrifuge, Kristallformer |
| Energie | Kohle-Generator, Strommast | Dampfturbine, Akku | Reaktor, Solarfeld |

- **Energie** als eigenes Stromnetz (Masten mit Reichweite, Factorio-Stil), intern auf **FE (Forge Energy)**-Basis.
- **Förderbänder sind eigene Blöcke** *(entschieden)* mit sichtbaren Items auf zwei Spuren.
- Alle Rezepte sind **Datapack-JSON**.

### 4.2 Produktionsketten (Beispiel Eisen)

```
Eisenerz ─► Schmelzofen ─► Eisenbarren ─► Presse ─► Eisenplatte ─┬─► Zahnrad
                                                                 └─► Stahl (mit Kohle) ─► Stahlträger
Zahnrad + Kupferkabel + Eisenplatte ─► Montagemaschine ─► Motor
```

Höherwertige Produkte brauchen **mehrere Rohstoffarten gleichzeitig** und in steigender Menge. Wer Motoren im großen Stil bauen will, braucht mehr Eisen- *und* Kupferfelder – das erzwingt Expansion.

### 4.3 Vanilla-Crafting *(entschieden)*

Bleibt für Vanilla-Gegenstände **unverändert**; Craftorio-Maschinen werden ausschließlich über Baupläne an Werkbänken gebaut (siehe 5.3). Später denkbar: ein eigenes Crafting-System, in dem die Fabrik spezielle Rezepte (Ausrüstung für Raids) freischaltet – wird separat konzipiert.

## 5. Wirtschaft

### 5.1 Währung

- **Credits (¢)** auf einem **Teamkonto** (Server-seitig, `SavedData`), keine Münz-Items. *(entschieden)*
- Anzeige im HUD, Transaktionslog im Terminal.

### 5.2 Verkaufen

- **Handelsposten** (Block): Items per Band/Greifarm hinein → sofort gutgeschrieben. Später **Frachtbahnhof** für größere Mengen.
- **Feste Preise, keine Marktsättigung** *(entschieden)*. Ein Item hat immer denselben Preis, egal wie viel verkauft wird.
- **Wertschöpfung pro Stufe:** Verkaufspreis ≈ Summe der Inputpreise × **1,3–1,6** + Energieanteil. Jede Verarbeitungsstufe lohnt sich; tiefe Ketten sind am profitabelsten.

| Ware | Stufe | Preis (umgesetzt, ×10 skaliert) |
|---|---|---|
| Rohes Eisen | 0 | 10 ¢ |
| Eisenbarren | 1 | 16 ¢ |
| Eisenplatte | 2 | 22 ¢ |
| Kupferkabel (2 pro Barren) | 2 | 12 ¢ |
| Zahnrad (2 Platten) | 3 | 60 ¢ |
| Schaltkreis (3 Kabel + 1 Platte) | 3 | 85 ¢ |
| Motor (2 Zahnräder + 1 Platte + 2 Kabel) | 4 | 240 ¢ |

- **Aufträge/Kontrakte** (optional, später): zeitlich begrenzte Bestellungen mit Bonus als zusätzliche Ziele – ohne Einfluss auf die Grundpreise.

### 5.3 Freischalten: Baupläne + Werkbänke *(entschieden, umgesetzt in M4)*

Maschinen, Logistik und Türme entstehen in **zwei Schritten**:

1. **Bauplan freischalten** (Geld, ab Stufe 2 zusätzlich Schlüsselmaterial) im **Terminal**. Baupläne sind
   **Team-Wissen**: einmal gekauft, kann jedes Teammitglied sie nutzen; sie gehen nicht verloren.
2. **An der Werkbank bauen** – aus Rohstoffen und Zwischenprodukten aus dem eigenen Inventar. Die Werkbank zeigt
   alle Baupläne ihrer Stufe: freigeschaltete mit Materialliste (fehlende rot), gesperrte ausgegraut mit Preis.

Vanilla-Crafting-Rezepte für Craftorio-Maschinen entfallen dann komplett.

**Werkbank-Stufen** – jede Stufe baut auch alles der Stufen darunter:

| Stufe | Werkbank | Aufrüstung | Typische Baupläne |
|---|---|---|---|
| 1 | **Konstruktionswerkbank** | Vanilla-Rezept (Startpunkt) | Handelsposten, Brenner-Bohrer, Förderband, Greifarm, Kohle-Generator, Strommast, Elektro-Schmelzofen, Presse, erste Türme |
| 2 | **Montagewerkbank** | Geld + Bohrkern (TD-Level 10) | Montagemaschine, Schnellband, Splitter, Unterflurband, Elektro-Bohrer, Höhleneingang-Bausatz, Türme Stufe 2 |
| 3 | **Präzisionswerkbank** | Geld + Tiefenkern (TD-Level 30) | Stufe-3-Maschinen, Expressband, Minenschacht-Bausatz, Endgame-Türme |
| (4) | **Automatische Fertigung** *(später)* | – | Werkbank, die per Band/Greifarm beliefert wird und Baupläne selbst fertigt |

Aufgerüstet wird **am Platz** (Rechtsklick mit dem Aufrüst-Bausatz), damit die Basis nicht umgebaut werden muss.

**Start:** Die Konstruktionswerkbank (Stufe 1) und das Terminal haben normale Vanilla-Rezepte. Handelsposten, Brenner-Bohrer, Förderband und
Greifarm sind als **Start-Baupläne** gratis freigeschaltet – alles andere kostet Credits.

Das **Terminal** (Block + GUI) hat die Tabs:

1. **Baupläne / Tech-Baum** – Freischaltungen gegen Credits (+ Schlüsselmaterial); Voraussetzungen als Baum.
2. **Tower Defense** – Level-Übersicht, nächstes Level starten, Reparaturen (ab M5).
3. **Statistik** – Einnahmen/Ausgaben, Verkäufe pro Ware, Produktionsraten.

Ein Shop für fertige Maschinen entfällt – Geld kauft **Wissen**, die Fabrik liefert **Material**.

## 6. Tower Defense *(überarbeitet)*

### 6.1 Grundprinzip

- Die Tower-Defense findet ausschließlich in einer **Verteidigungszone** statt. Gegner greifen **nur Türme** an (Turm-HP) – **niemals Fabrik, Spieler-Bauten außerhalb der Zone oder die Welt**. Gegner-Entities können keine Blöcke zerstören.
- Die Tower-Defense ist **levelbasiert**: Das Team startet ein Level im Terminal oder am Zonenkern. Ein Level besteht aus mehreren Wellen.
- Die Fabrik läuft währenddessen normal weiter und wird nie beschädigt.

### 6.2 Die Verteidigungszone

- Jedes Team errichtet eine Zone durch Platzieren eines **Zonenkerns** (gekauft). Die Zone hat eine feste Größe (Vorschlag: 5×5 Chunks, später erweiterbar).
- Am Rand liegt ein **Feindportal**, am anderen Ende der **Kern**. Gegner laufen einen **Pfad** vom Portal zum Kern (Pfad wird vom Spieler mit „Pfadblöcken“ gelegt oder ist vorgegeben – siehe offene Punkte).
- Türme dürfen **nur innerhalb der Zone** gebaut werden; Förderbänder dürfen in die Zone hinein, um Munition zu liefern.

### 6.3 Ablauf eines Levels

- Gegner laufen zum Kern und **greifen Türme in Reichweite an**.
- **Turm-HP = 0 → Turm wird zerstört** und hinterlässt eine **Turmruine** (Upgrades und Module bleiben in der Ruine gespeichert).
- **Level verloren**, wenn zu viele Gegner den Kern erreichen (Lebenspunkte des Levels, z. B. 20). Der Kern selbst bleibt unbeschädigt – er ist nur der Zähler.
- **Nach einer Niederlage:** Beschädigte Türme reparieren (Geld + Reparaturmaterial), Ruinen wiederaufbauen (Geld + Bauteile, günstiger als Neukauf), Türme aufrüsten oder neue bauen – dann Level erneut versuchen. Es gibt keine weitere Strafe.
- **Nach einem Sieg:** Türme behalten ihren Schaden; Reparatur ist Teil der Vorbereitung auf das nächste Level.
- Ziel: Niederlagen sollen den Spieler dazu bringen, die **Fabrik auszubauen**, weil bessere Türme, Upgrades, Reparaturen und Munition Produktionsleistung kosten.

### 6.4 Belohnungen

| Level | Belohnung |
|---|---|
| Jedes Level | Credits (steigend mit Level) |
| **Alle 10 Level** | **Schlüsselmaterial** für den nächsten Fabrik-Fortschritt |

Vorschlag für Schlüsselmaterialien (Beispiele, Balancing offen):

| Level | Schlüsselmaterial | Schaltet frei (mit Geld) |
|---|---|---|
| 10 | Bohrkern | Höhleneingang, Stufe-2-Bohrer |
| 20 | Resonanzkristall | Montagemaschine II, Chemieanlage |
| 30 | Tiefenkern | Minenschacht |
| 40 | Sternenerz-Splitter | Stufe-3-Maschinen, Reaktor |
| 50+ | … | Endgame-Technologien |

Schlüsselmaterialien sind **nicht verkäuflich** und **nicht herstellbar** – nur über die Tower-Defense erhältlich. Damit sind beide Spielhälften zwingend miteinander verzahnt.

### 6.5 Gegner (eigene Entities)

| Gegner | ab Level | Besonderheit |
|---|---|---|
| Krabbler | 1 | schnell, schwach, in Schwärmen |
| Brecher | 5 | hohe HP, greift Türme gezielt an |
| Spucker | 10 | Fernangriff auf Türme |
| Höhlenwurm | 20 | taucht unter, nur kurz angreifbar |
| Kristallgolem | 30 | gepanzert, braucht Durchschlags-/Laserschaden |
| Boss (z. B. Brutmutter) | jedes 10. Level | bewacht das Schlüsselmaterial |

### 6.6 Türme

- Türme werden **mit Geld gekauft** und benötigen **Munition oder Strom aus der Fabrik** (Munition per Band/Greifarm).
- **Upgrades** (Stufe I–V) kosten Geld **plus verarbeitete Bauteile** (Motor, Stahlträger, Kristalllinse …).
- **Module** in Slots: Reichweite, Feuerrate, Schadenstyp, **Panzerung (mehr Turm-HP)**.

| Turm | Munition | Rolle |
|---|---|---|
| Armbrustturm | Bolzen (Holz+Eisen) | Einstieg |
| Geschützturm | Patronen (Kupfer+Eisen+Schwefel) | Allrounder |
| Flammenwerfer | Treibstoff (Rohr) | Flächenschaden |
| Tesla-Turm | Strom | kein Nachschub, hoher Verbrauch |
| Mörser | Granaten | Reichweite, gegen Gruppen |
| Laser-Turm | Strom + Kristalle | Endgame, gegen Panzerung |
| Reparaturturm | Reparaturkits | heilt Türme in Reichweite |

## 7. Coop & Solo

- **Teams** *(eigenes System, optionale FTB-Teams-Kompatibilität – entschieden)*: Das Team teilt **Konto, Technologien, erschlossene Bereiche, Verteidigungszone und Level-Fortschritt**.
- **Solo** = Team mit einem Mitglied.
- **Mehrere Teams auf einem Server** möglich; fremde Maschinen/Kisten/Türme sind geschützt.
- **Skalierung:** Gegner-HP/-Anzahl skalieren moderat mit der Anzahl **online** befindlicher Teammitglieder beim Levelstart.
- **Technik:** Server ist autoritativ; Client bekommt nur Sync-Pakete. Singleplayer, LAN und dedizierte Server.

## 8. Minecraft-Version & Mod-Loader

**Minecraft 1.21.1 + NeoForge (Java 21)** *(entschieden)*

- 1.21.1 ist die aktuelle Langzeit-Modding-Version mit den meisten stabilen Mods.
- NeoForge bietet die APIs für Maschinen, Energie (FE), Item-/Fluid-Handler, Worldgen und GUIs.
- Werkzeuge: ModDevGradle, Parchment-Mappings, Data-Generatoren, GameTests.

Unmittelbar vor dem Projekt-Setup werden die aktuellen Versionsstände geprüft.

## 9. Bestehende Mods

### 9.1 Abhängigkeiten

| Mod | Wofür |
|---|---|
| **GeckoLib** | Animierte Gegner und Türme |

Alles, was den Kern-Loop betrifft, bauen wir selbst.

### 9.2 Empfohlen im Modpack / mit Integration

| Mod | Wofür | Integration |
|---|---|---|
| **EMI** (oder JEI) | Rezeptanzeige | Plugin: eigene Rezepttypen + Verkaufspreis im Tooltip |
| **Jade** | Blick-Tooltips | Plugin: Maschinenfortschritt, Erzfeld-Auslastung, Turm-HP/Munition |
| **FTB Teams** *(optional)* | Team-Verwaltung | Teams übernehmen, falls installiert |
| **FTB Quests** *(optional)* | Tutorial/Story | Quest-Buch für den Einstieg |
| **Sodium, ModernFix, FerriteCore** | Performance | – |
| **Create** *(später optional)* | Mechanik-Mod | Addon-Kompat |

**Nicht empfohlen:** Worldgen-Mods (Terralith, Tectonic, BoP) und Tech-Mods mit eigenen Quarries (Mekanism, Thermal) ohne Config-Anpassung.

## 10. Technische Architektur

### 10.1 Module (Java-Pakete)

```
de.craftorio
├── core         Registrierung, Config, Netzwerk, Team-/Weltdaten (SavedData)
├── economy      Konto, Preistabelle (Datapack), Handelsposten, Terminal-GUI
├── progression  Technologie-Baum (Datapack), Freischaltungen, Schlüsselmaterialien
├── world        Schichten-Worldgen, Deckgestein, Erzfelder, Eingänge/Schächte, Bereichs-Freischaltung
├── logistics    Förderbänder, Greifarme, Splitter, Rohre, Kisten
├── machines     Bohrer (mit Abbauflächen-Reservierung), Öfen, Montage, Energie-Netz
├── defense      Verteidigungszone, Level-/Wellen-Director, Gegner, Türme, Ruinen, Reparatur
└── compat       EMI, Jade, FTB Teams/Quests (nur wenn geladen)
```

### 10.2 Datengetrieben

Per `/reload` änderbar:

- `data/<namespace>/data_maps/item/sell_prices.json` – Verkaufspreise (NeoForge Data Map, auch Item-Tags möglich)
- `data/craftorio/tech/*.json` – Technologie-Knoten (Geld, Schlüsselmaterialien, Voraussetzungen)
- `data/craftorio/td_levels/*.json` – Level-Definitionen (Wellen, Gegner, Belohnungen)
- `data/craftorio/ore_fields/*.json` – Erzfeld-Definitionen (Schicht, Größe, Entfernungsskalierung)
- `data/craftorio/recipe/*.json` – Maschinenrezepte

### 10.3 Technische Risiken

| Risiko | Gegenmaßnahme |
|---|---|
| Performance von Bändern | Item-Bewegung als Daten (keine Entity pro Item), Batch-Rendering |
| Erzfeld-Überlappung | Zentrale Reservierung der Feldblöcke pro Chunk; Bohrer prüft beim Platzieren |
| TD in ungeladenen Chunks | Zonenkern hält die Zone während eines Levels geladen; Level pausiert, wenn kein Teammitglied online ist |
| Gegner verlassen die Zone | Pfadfindung auf die Zone beschränkt, Zonengrenze als unsichtbare Barriere für TD-Entities |
| Umgehen des Deckgesteins | Unzerstörbar, nicht verschiebbar, Teleport-Check, Config-Blacklist |

## 11. Entscheidungslog

| # | Frage | Entscheidung |
|---|---|---|
| 1 | Schichten vs. Dimensionen | Eine Dimension mit Schichten |
| 2 | Erzfelder | Unendlich, aber Maximaldurchsatz durch Feldgröße begrenzt → Expansion nötig |
| 3 | Förderbänder | Eigene |
| 4 | Teamsystem | Eigenes, optionale FTB-Teams-Kompat |
| 5 | Marktsättigung | Keine; feste Preise |
| 6 | Vanilla-Crafting | Unverändert; eigenes Crafting (Raid-Ausrüstung) später |
| 7 | Angriffe | Nur auf Türme (HP) in der Verteidigungszone; Fabrik bleibt unberührt; levelbasiert; Geld pro Level, Schlüsselmaterial alle 10 Level |
| 8 | Währung | Credits (¢) |

## 12. Entscheidungen Runde 2

| # | Frage | Entscheidung |
|---|---|---|
| 9 | Ort der Verteidigungszone | Normale Welt; Munition per Band aus der Fabrik |
| 10 | Gegnerpfad | Vom Spieler mit Pfadblöcken gelegt, mit Mindestlänge |
| 11 | Levelstart | Manuell, optionaler Auto-Modus nach Sieg |
| 12 | Anzahl Zonen | Zunächst eine Zone pro Team, später erweiterbar (Höhlen/Minen) |
| 13 | Spieler im Kampf | Reine Tower Defense (Spieler kämpft nicht mit) |
| 14 | Kaufen vs. Bauen | Baupläne mit Geld freischalten (Team-Wissen), an gestuften Werkbänken aus Material bauen; kein Shop für fertige Maschinen |

## 13. Umsetzungs-Roadmap

| Meilenstein | Inhalt | Ergebnis |
|---|---|---|
| **M0 – Setup** | NeoForge-1.21.1-Projekt, ModDevGradle, Datagen, CI (GitHub Actions) | Leere Mod startet in Client & Server |
| **M1 – Wirtschaftskern** | Teams, Teamkonto, Preistabelle, Handelsposten, HUD, Befehle | Items verkaufen → Geld |
| **M2 – Oberfläche** | Erzfelder (unendlich, Flächenreservierung), Brenner-Bohrer, Förderband, Greifarm, Kiste | Erste Automatisierungsschleife |
| **M3 – Verarbeitung & Energie** | Schmelzofen, Presse, Montagemaschine, Stromnetz | Produktionsketten mit Wertschöpfung |
| **M4 – Terminal, Baupläne & Werkbänke** | Terminal-GUI, Bauplan-Baum (Datapack), Werkbank Stufe 1–3, Vanilla-Rezepte entfernen, Statistik | Vollständiger Wirtschafts-Loop |
| **M5 – Tower Defense I** | Zonenkern, Level-Director, 3 Gegner, 3 Türme, HP/Ruinen/Reparatur, Belohnungen Level 1–10 | Erstes Schlüsselmaterial erspielbar |
| **M6 – Höhlenschicht** | Schichten-Worldgen, Deckgestein, Höhleneingang, Bereichs-Freischaltung, Höhlen-Ressourcen | Zweite Stufe spielbar |
| **M7 – Minenschicht** | Minenschacht, Endgame-Ressourcen, Stufe-3-Maschinen/-Türme, Level 20–40 | Vollständige Progression |
| **M8 – Coop & Polish** | Mehrere Teams, Rechte, Skalierung, Balancing, EMI/Jade-Kompat, Quests | Erste spielbare Beta |

Jeder Meilenstein endet mit einem spielbaren Stand. Aktueller Stand: **M0–M4 abgeschlossen**, als Nächstes **M5 – Tower Defense I**.
