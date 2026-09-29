# Craftorio – Umbau auf die Factorio-Progression

Status: **entschieden** (Runde 3, siehe `docs/KONZEPT.md` §14). Dieses Dokument ersetzt für Rohstoffe, Rezepte,
Maschinen, Logistik, Freischaltung und Leitfaden die bisherigen Abschnitte 4 und 5.3 des Konzepts. Tower Defense,
Arena, Teams, Ebenen und Handbuch bleiben und werden angebunden.

Es ist als Arbeitsauftrag für eine KI-Coding-Session gedacht. Arbeitsweise, Befehle und Definition of Done wie in
`docs/BACKLOG-Testrunde-1.md` Abschnitt 0.

---

## 1. Entscheidungen

| # | Frage | Entscheidung |
|---|---|---|
| F-1 | Wie weit Factorio folgen? | Rohstoffe, Rezepte (Mengen und Zeiten), Maschinen, Logistik und Forschungsbaum **1:1 nach Factorio 1.1**, erweitert um Tower Defense und Pflanzen |
| F-2 | Freischalten | **Forschung** mit Wissenschaftspaketen im **Labor** (statt Baupläne per Credits) |
| F-3 | Credits | **Hybrid:** Verkaufen am Handelsposten bringt Credits; Credits bezahlen die Tower Defense (Turm-Upgrades, Reparatur, Arena-Extras) und Komfort, **nicht** die Forschung |
| F-4 | Flüssigkeiten und Öl | **Ja:** Rohre, Pumpen, Tanks, Pumpjack, Raffinerie, Chemiefabrik |
| F-5 | Umfang Stufe 1 | Rot, Grün, **Militär (Grau)**, Blau. Lila und Gelb später als Endgame |
| F-6 | Ebenen | Bleiben in der Oberwelt: Oberfläche → Höhlen → Minen |
| F-7 | Pflanzen | **Gewächshaus** als Maschine (eigene Erweiterung, nicht aus Factorio) |
| F-8 | Spieltempo | **Zeiten 1:1 wie Factorio**; Anpassung nur über Server-Config-Faktoren nach Spieltests (siehe §2) |
| F-9 | Leitfaden | Aus Forschungs- und Bau-Meilensteinen abgeleitet; Belohnungen klein bzw. als Items |
| F-10 | Vanilla-Werkzeuge | Rezepte für Spitzhacken, Äxte, Schaufeln, Hacken entfernen (Starter-Spitzhacke reicht) |

---

## 2. Spieltempo (wichtig)

**Grundsatz:** Maschinen, Bänder, Bohrer und Forschung laufen mit **denselben Zeiten wie in Factorio**. Minecraft
setzt dem keine Grenze: Maschinen sind Zähler pro Tick. Die Bänder laufen schon heute exakt mit Factorio-Werten
(`logistics/BeltTier`):

| Band | Blöcke/s | Items/s (beide Spuren) |
|---|---|---|
| Gelb | 1,875 | 15 |
| Rot | 3,75 | 30 |
| Blau | 5,625 | 45 |

Zeitangaben unten in Sekunden, im Code `Sekunden × 20 = Ticks`.

**Was sich trotzdem anders anfühlt, und wie wir es ausgleichen:**

1. **Bauen dauert länger** (3D, Block für Block, anfangs keine Blaupausen oder Roboter). Ausgleich: Die Handarbeit an
   der Werkbank ist **sofort** (Factorio hat eine Handcraft-Warteschlange mit Zeit). Das beschleunigt den Anfang.
2. **Fabriken stehen still, wenn ihre Chunks entladen sind.** Das passiert z. B. während der Spieler in der
   Arena-Dimension ist oder beim Ausloggen im Coop. In Factorio läuft immer die ganze Karte. Ausgleich: **Team-Chunkloader**
   (§8, Paket U1). Chunks mit Team-Maschinen werden per `ForcedChunks`/Ticket geladen gehalten, begrenzt pro Team
   (Config, z. B. 64 Chunks), nur solange mindestens ein Teammitglied online ist (Config).
3. **Feinjustierung ohne Code:** Server-Config-Faktoren, Standard 1,0:
   - `pacing.craftingSpeed` – Maschinen- und Schmelzzeiten
   - `pacing.miningSpeed` – Bohrer
   - `pacing.researchCost` – Anzahl Pakete pro Forschung

   Bänder bleiben fest, damit Bandverhältnisse stimmen.
4. **Messen statt raten:** Das Terminal speichert pro Team den Zeitpunkt jedes Forschungsabschlusses (Spielzeit in
   Ticks) und zeigt ihn in der Statistik. Zielwerte für Spieltests, grob wie ein erster Factorio-Durchlauf:

   | Meilenstein | Zielzeit |
   |---|---|
   | Rot automatisiert | 30–60 min |
   | Grün automatisiert | 2–3 h |
   | Öl verarbeitet | 5–7 h |
   | Blau automatisiert | 8–12 h |
   | Endgame-Ziele (Uran, Kernkraft, Lila, Gelb) | siehe §11.9 |

   Weichen Tests deutlich ab, zuerst `pacing.researchCost` anpassen, nicht einzelne Rezepte.

---

## 3. Rohstoffe und Ebenen

| Rohstoff | Minecraft-Item | Wo | Gewinnung |
|---|---|---|---|
| Eisenerz | `minecraft:raw_iron` | Oberfläche (Erzfeld) | Hand/Starter-Spitzhacke, Bohrer |
| Kupfererz | `minecraft:raw_copper` | Oberfläche | dito |
| Kohle | `minecraft:coal` | Oberfläche | dito |
| Stein | `minecraft:cobblestone` | Oberfläche (neues **Steinfeld**) + jeder Stein per Hand | dito |
| Holz | Stämme/Bretter | Bäume | Hand |
| Wasser | Flüssigkeit | Wasserquelle | Offshore-Pumpe |
| Rohöl | Flüssigkeit | **Höhlen** (Ölquellen-Block) | Pumpjack |
| Uranerz | `craftorio:raw_uranium` | **Minen** (Erzfeld) | Elektro-Bohrer + Schwefelsäure |

**Zuordnungen:**
- Eisen- und Kupferbarren sind die **Platten** von Factorio (Vanilla-Items weiterverwenden, Anzeige-Name bleibt Vanilla).
- `craftorio:iron_plate` entfällt (Migration §9).
- Stahl ist neu: `craftorio:steel_plate`.

**Entfallen:** Zinn, Blei, Titan, Schwefel-Erzfeld, Quarz-, Diamant-, Gold- und Kristallfelder.
Schwefel entsteht wie in Factorio chemisch. Die Ebenen bekommen damit klare Rollen:
- **Oberfläche:** Grundstoffe, Rot, Grün, Militär
- **Höhlen:** Öl, also Kunststoff, Schwefel, Batterien, Blau
- **Minen:** Uran (Endgame, später Lila und Gelb)

Der Höhleneingang wird durch Forschung „Ölverarbeitung" freigeschaltet, der Minenschacht durch „Uranverarbeitung"
(später). Die Baukosten beider werden auf Factorio-Materialien umgestellt.

---

## 4. Rezepte (Referenz Factorio 1.1)

Format: Zeit (s) · Zutaten → Ergebnis. **Vor der Umsetzung jede Zeile gegen wiki.factorio.com (Version 1.1) prüfen**;
die Werte hier sind aus dem Gedächtnis zusammengestellt. Abweichungen im Code dokumentieren.

### 4.1 Grundstoffe und Zwischenprodukte

| Item | Rezept | Wo |
|---|---|---|
| Eisenplatte (= Eisenbarren) | 3,2 · 1 Eisenerz → 1 | Ofen |
| Kupferplatte (= Kupferbarren) | 3,2 · 1 Kupfererz → 1 | Ofen |
| Stahl | 16 · 5 Eisenplatten → 1 | Ofen |
| Steinziegel | 3,2 · 2 Stein → 1 | Ofen |
| Zahnrad | 0,5 · 2 Eisenplatten → 1 | Hand/Assembler |
| Kupferkabel | 0,5 · 1 Kupferplatte → 2 | Hand/Assembler |
| Elektronischer Schaltkreis (grün) | 0,5 · 1 Eisenplatte + 3 Kupferkabel → 1 | Hand/Assembler |
| Rohr | 0,5 · 1 Eisenplatte → 1 | Hand/Assembler |
| Eisenstange | 0,5 · 1 Eisenplatte → 2 | Hand/Assembler |
| Motor (Engine unit) | 10 · 1 Stahl + 1 Zahnrad + 2 Rohre → 1 | nur Assembler |
| Kunststoff | 1 · 20 Petroleum + 1 Kohle → 2 | Chemiefabrik |
| Schwefel | 1 · 30 Wasser + 30 Petroleum → 2 | Chemiefabrik |
| Schwefelsäure | 1 · 100 Wasser + 5 Schwefel + 1 Eisenplatte → 50 | Chemiefabrik |
| Batterie | 4 · 20 Schwefelsäure + 1 Eisenplatte + 1 Kupferplatte → 1 | Chemiefabrik |
| Fortschrittlicher Schaltkreis (rot) | 6 · 2 Schaltkreise + 2 Kunststoff + 4 Kupferkabel → 1 | Assembler |
| Schmiermittel | 1 · 10 Schweröl → 10 | Chemiefabrik |
| Einfache Ölverarbeitung | 5 · 100 Rohöl → 45 Petroleum | Raffinerie |
| Fortgeschrittene Ölverarbeitung | 5 · 100 Rohöl + 50 Wasser → 25 Schweröl + 45 Leichtöl + 55 Petroleum | Raffinerie |
| Schweröl-/Leichtöl-Cracking | 2 · 40 Schweröl + 30 Wasser → 30 Leichtöl · 2 · 30 Leichtöl + 30 Wasser → 20 Petroleum | Chemiefabrik |

### 4.2 Wissenschaftspakete

| Paket | Rezept |
|---|---|
| Automatisierung (rot) | 5 · 1 Kupferplatte + 1 Zahnrad → 1 |
| Logistik (grün) | 6 · 1 Greifarm + 1 Förderband → 1 |
| Militär (grau) | 10 · 1 panzerbrechendes Magazin + 1 Granate + 2 Steinmauern → 2 |
| Chemie (blau) | 24 · 2 Motoren + 3 fortschrittliche Schaltkreise + 1 Schwefel → 2 |

### 4.3 Maschinen und Logistik

| Maschine | Rezept | Kennwerte (Factorio) |
|---|---|---|
| Steinofen | 0,5 · 5 Stein | Brennstoff, Geschwindigkeit 1, 90 kW |
| Brenner-Bohrer | 2 · 3 Zahnräder + 1 Steinofen + 3 Eisenplatten | 0,25/s, 150 kW Brennstoff, 2×2 → bei uns 3×3 Feld (bisherige Flächenregel) |
| Elektro-Bohrer | 2 · 3 Schaltkreise + 5 Zahnräder + 10 Eisenplatten | 0,5/s, 90 kW, 5×5 |
| Brenner-Greifarm | 0,5 · 1 Zahnrad + 1 Eisenplatte | Brennstoff |
| Greifarm | 0,5 · 1 Schaltkreis + 1 Zahnrad + 1 Eisenplatte | Strom |
| Langer Greifarm | 0,5 · 1 Greifarm + 1 Zahnrad + 1 Eisenplatte | 2 Blöcke Reichweite |
| Schneller Greifarm | 0,5 · 1 Greifarm + 2 Schaltkreise + 2 Eisenplatten | |
| Filter-Greifarm | 0,5 · 1 schneller Greifarm + 4 Schaltkreise | Filter-Slots |
| Förderband ×2 | 0,5 · 1 Zahnrad + 1 Eisenplatte | 15 Items/s |
| Unterflurband ×2 | 1 · 10 Eisenplatten + 5 Förderbänder | Reichweite 4 Blöcke dazwischen |
| Splitter | 1 · 5 Schaltkreise + 5 Eisenplatten + 4 Förderbänder | Priorität, Filter |
| Schnelles Band | 0,5 · 5 Zahnräder + 1 Förderband | 30 Items/s |
| Schnelles Unterflurband ×2 | 2 · 40 Zahnräder + 2 Unterflurbänder | Reichweite 6 |
| Schneller Splitter | 2 · 1 Splitter + 10 Zahnräder + 10 Schaltkreise | |
| Express-Band | 0,5 · 10 Zahnräder + 1 schnelles Band + 20 Schmiermittel | 45 Items/s |
| Holzkiste / Eisenkiste / Stahlkiste | 2 Holz / 8 Eisenplatten / 8 Stahl | 16 / 32 / 48 Slots |
| Rohr-Unterführung ×2 | 0,5 · 10 Rohre + 5 Eisenplatten | Reichweite 10 |
| Offshore-Pumpe | 0,5 · 2 Schaltkreise + 1 Rohr + 1 Zahnrad | 1200 Wasser/s |
| Pumpe | 2 · 1 Motor + 1 Stahl + 1 Rohr | |
| Tank | 3 · 20 Eisenplatten + 5 Stahl | 25.000 Einheiten |
| Kessel | 0,5 · 1 Steinofen + 4 Rohre | 1,8 MW, versorgt 2 Dampfmaschinen |
| Dampfmaschine | 0,5 · 8 Zahnräder + 5 Rohre + 10 Eisenplatten | 900 kW |
| Kleiner Strommast ×2 | 0,5 · 1 Holz + 2 Kupferkabel | Versorgung 5×5, Reichweite 7,5 |
| Mittlerer Strommast | 0,5 · 2 Stahl + 2 Kupferplatten + 4 Eisenstangen | Versorgung 7×7, Reichweite 9 |
| Solarpanel | 10 · 5 Stahl + 15 Schaltkreise + 5 Kupferplatten | 60 kW Spitze, Tag/Nacht aus Minecraft |
| Akku | 10 · 2 Eisenplatten + 5 Batterien | 5 MJ, 300 kW laden/entladen |
| Labor | 2 · 10 Schaltkreise + 10 Zahnräder + 4 Förderbänder | 60 kW |
| Montagemaschine 1 | 0,5 · 3 Schaltkreise + 5 Zahnräder + 9 Eisenplatten | Geschw. 0,5, 75 kW |
| Montagemaschine 2 | 0,5 · 2 Stahl + 3 Schaltkreise + 5 Zahnräder + 1 Montagemaschine 1 | Geschw. 0,75, 150 kW, Flüssigkeiten |
| Stahlofen | 3 · 6 Stahl + 10 Steinziegel | Geschw. 2, Brennstoff |
| Elektroofen | 5 · 10 Stahl + 5 fortschrittliche Schaltkreise + 10 Steinziegel | Geschw. 2, 180 kW |
| Pumpjack | 5 · 5 Stahl + 10 Zahnräder + 5 Schaltkreise + 10 Rohre | auf Ölquelle |
| Ölraffinerie | 8 · 15 Stahl + 10 Zahnräder + 10 Steinziegel + 10 Schaltkreise + 10 Rohre | Multiblock 5×5 → bei uns 3×3 |
| Chemiefabrik | 5 · 5 Stahl + 5 Zahnräder + 5 Schaltkreise + 5 Rohre | Multiblock 3×3 |

**Energie-Einheiten:** Wir rechnen intern weiter in FE/t. Eine feste Umrechnung einführen und überall verwenden,
z. B. **1 kW = 1 FE/t** (900 kW Dampfmaschine = 900 FE/t). Die bisherigen FE-Werte der Maschinen daran anpassen.
Brennwerte wie in Factorio: Kohle 4 MJ, Holz 2 MJ, fester Brennstoff 12 MJ; Vanilla-Brennzeiten entsprechend umrechnen.

### 4.4 Militär = Tower Defense

| Item | Rezept | Verwendung |
|---|---|---|
| Magazin | 1 · 4 Eisenplatten | Geschützturm |
| Panzerbrechendes Magazin | 3 · 1 Magazin + 1 Stahl + 5 Kupferplatten | Geschützturm, Militärpaket |
| Granate | 8 · 5 Kohle + 5 Eisenplatten | Militärpaket (in der Arena nicht werfbar) |
| Steinmauer | 0,5 · 5 Steinziegel | Arena-Deko/Hindernis, Militärpaket |
| Geschützturm | 8 · 10 Zahnräder + 10 Kupferplatten + 20 Eisenplatten | Munition über den Arena-Einspeiser |
| Laser-Turm | 20 · 20 Stahl + 20 Schaltkreise + 12 Batterien | Strom über den Arena-Einspeiser |
| Flammenwerfer-Turm | 20 · 30 Stahl + 15 Zahnräder + 10 Rohre + 5 Motoren | Rohöl/Schweröl über den Einspeiser |

Der bisherige Armbrustturm bleibt als billiger Einstiegsturm vor der Forschung „Geschütztürme" (Holz + Eisen, Bolzen).
Der Tesla-Turm entfällt oder wird zum Endgame-Turm für die Minen; das ist eine spätere Entscheidung.

---

## 5. Forschung

**Mechanik:**
- **Labor** (Block + Menü) nimmt Wissenschaftspakete an (auch per Greifarm), verbraucht sie und erzeugt
  Forschungsfortschritt für das **Team**.
- Mehrere Labore forschen parallel an derselben Forschung.
- Die aktive Forschung wählt man im Terminal (neuer Tab **Forschung**, ersetzt den Tab **Baupläne**); dort gibt es auch
  eine Warteschlange.
- Eine Forschung hat Kosten *Anzahl × Paketsatz* und eine Zeit pro Einheit. Ein Labor mit Geschwindigkeit 1 verbraucht
  pro Einheit genau einen Paketsatz in dieser Zeit.
- Abgeschlossene Forschungen schalten **Rezepte** frei: für die Werkbank (Handarbeit) und für Assembler, Öfen und
  Chemiefabrik.
- Das bestehende Team-Wissen (`TeamRegistry.unlocked`) wird zur Liste abgeschlossener Forschungen.
- Datengetrieben wie heute die Baupläne: Registry `craftorio:research` mit Kosten, Voraussetzungen, freigeschalteten
  Rezepten und optionalem Schlüssel-Item (§6).

**Forschungsbaum, erster Umfang** (Kosten Factorio 1.1: Einheiten × Pakete, Zeit pro Einheit; **gegen das Wiki prüfen**):

| Forschung | Kosten | Voraussetzung | Schaltet frei |
|---|---|---|---|
| *(Start, ohne Forschung)* | – | – | Steinofen, Brenner-Bohrer, Brenner-Greifarm, Förderband, Zahnrad, Kupferkabel, Schaltkreis, Rohr, Kisten, Kessel, Dampfmaschine, Offshore-Pumpe, kleiner Strommast, Labor, Greifarm, Elektro-Bohrer, Rotes Paket, Handelsposten, Terminal, Arena-Tor, Armbrustturm, Bolzen |
| Automatisierung | 10 × R, 10 s | – | Montagemaschine 1, langer Greifarm |
| Logistik | 75 × R, 15 s | – | Unterflurband, Splitter |
| Elektronik | 30 × R, 15 s | Automatisierung | (Voraussetzung für weitere) |
| Geschütztürme | 10 × R, 10 s | – | Geschützturm, Magazin, **Arena-Einspeiser** |
| Steinmauern | 10 × R, 10 s | – | Steinmauer |
| Stahlverarbeitung | 50 × R, 5 s | – | Stahl, Stahlkiste |
| Logistik-Wissenschaftspaket | 75 × R, 5 s | – | Grünes Paket |
| Schnelle Greifarme | 30 × R, 15 s | Elektronik | schneller Greifarm, Filter-Greifarm |
| Automatisierung 2 | 40 × R+G, 15 s | Elektronik, Stahl, Grün | Montagemaschine 2 |
| Logistik 2 | 200 × R+G, 30 s | Logistik, Grün | schnelle Bänder, schnelles Unterflurband, schneller Splitter |
| Elektrische Energieverteilung 1 | 120 × R+G, 30 s | Elektronik, Stahl, Grün | mittlerer Strommast |
| Fortgeschrittene Materialverarbeitung | 75 × R+G, 30 s | Stahl, Grün | Stahlofen |
| Solarenergie | 100 × R+G, 30 s | Optik (entfällt), Elektronik, Stahl, Grün | Solarpanel |
| Motor | 100 × R+G, 15 s | Stahl, Grün | Motor |
| Flüssigkeitsverarbeitung | 50 × R+G, 15 s | Automatisierung 2, Motor | Pumpe, Tank |
| Ölverarbeitung | 100 × R+G, 30 s | Flüssigkeitsverarbeitung | Pumpjack, Raffinerie, Chemiefabrik, Einfache Ölverarbeitung, fester Brennstoff, **Höhleneingang** |
| Kunststoffe | 200 × R+G, 30 s | Ölverarbeitung | Kunststoff |
| Schwefelverarbeitung | 150 × R+G, 30 s | Ölverarbeitung | Schwefel, Schwefelsäure |
| Fortschrittliche Elektronik | 200 × R+G, 30 s | Kunststoffe | fortschrittlicher Schaltkreis |
| Batterie | 150 × R+G, 30 s | Schwefelverarbeitung | Batterie |
| Elektrische Energiespeicher | 150 × R+G, 30 s | Batterie, Energieverteilung 1 | Akku |
| Militär 2 | 20 × R+G, 15 s | Geschütztürme, Grün | panzerbrechendes Magazin, Granate |
| Militär-Wissenschaftspaket | 30 × R+G, 15 s | Militär 2, Steinmauern | Graues Paket |
| Chemie-Wissenschaftspaket | 75 × R+G, 10 s | Fortschrittliche Elektronik, Schwefelverarbeitung | Blaues Paket |
| Fortgeschrittene Ölverarbeitung | 75 × R+G+B, 30 s | Chemie-Paket | Fortgeschrittene Ölverarbeitung, Cracking |
| Schmiermittel | 50 × R+G+B, 30 s | Fortgeschrittene Ölverarbeitung | Schmiermittel |
| Logistik 3 | 300 × R+G+B, 15 s | Logistik 2, Schmiermittel | Express-Band usw. |
| Laser | 100 × R+G+B, 30 s | Chemie-Paket, Batterie | – |
| Laser-Türme | 150 × R+G+Militär, 30 s | Laser, Militär-Paket | Laser-Turm |
| Flammenwerfer | 50 × R+G+Militär, 30 s | Flüssigkeitsverarbeitung, Militär-Paket | Flammenwerfer-Turm |
| Elektroofen | 300 × R+G+B, 30 s | Fortgeschrittene Materialverarbeitung, Chemie-Paket | Elektroofen |

Nicht übernommen (kein Gegenstück in unserem Spiel): Optik, Fahrzeuge, Waffen und Rüstung des Spielers, Roboter,
Radar, Schaltungsnetz. Diese Forschungen fehlen; ihre Nachfolger verlieren die Voraussetzung.

---

## 6. Tower Defense in der Progression

- Die **Arena bleibt** wie sie ist (Karten, Pfadstab, Türme, Wellen). Die Türme werden die Factorio-Türme (§4.4);
  versorgt wird über den Arena-Einspeiser: Strom, Munition und neu Flüssigkeit für Flammenwerfer.
- **Schlüssel aus der Arena:** Die erste Forschung jeder neuen Paketstufe braucht zusätzlich einmalig ein
  **Arena-Siegel** (`unlockItems` wie heute Drill Core/Deep Core, beim Start der Forschung ins Terminal oder Labor
  eingelegt):

  | Forschung | Arena-Siegel aus |
  |---|---|
  | Logistik-Wissenschaftspaket | Bronze-Siegel (TD-Level 5) |
  | Militär-Wissenschaftspaket | Silber-Siegel (TD-Level 10) |
  | Chemie-Wissenschaftspaket | Gold-Siegel (TD-Level 20) |
  | Minenschacht | Platin-Siegel (TD-Level 30) |
  | Produktions-Wissenschaftspaket | Diamant-Siegel (TD-Level 40), siehe §11 |
  | Nutzlast-Wissenschaftspaket | Sternen-Siegel (TD-Level 50), siehe §11 |

- **Credits:** Pro geschafftem Level wie heute. Ausgegeben werden sie für Turm-Upgrades, Reparatur und Wiederaufbau,
  Arena-Extras (z. B. Kartenwechsel neu würfeln, Zusatzleben) und Komfort (Handbuch-Ersatz usw.). Forschung kostet
  **keine** Credits.
- Die Werkbank-Stufen (Aufrüstsätze) **entfallen**. Was man von Hand bauen darf, bestimmt allein die Forschung.
  Bestimmte Rezepte (Motor, Kunststoff, Batterie, fortschrittlicher Schaltkreis) sind wie in Factorio nicht von Hand
  herstellbar, nur in Maschinen.

---

## 7. Pflanzen (eigene Erweiterung)

- **Gewächshaus** (Multiblock 3×3 oder einzelner Block mit Menü): Saatgut + Wasser (Flüssigkeit) + Strom ergibt
  Pflanzen. Automatisierbar über Band und Greifarm.

  | Pflanze | Rezept (Richtwerte, im Spieltest justieren) |
  |---|---|
  | Weizen | 20 s · 1 Weizensaat + 50 Wasser → 3 Weizen + 1 Saat |
  | Kürbis | 30 s · 1 Kürbiskern + 50 Wasser → 1 Kürbis |
  | Karotte | 20 s · 1 Karotte + 50 Wasser → 3 Karotten |
  | Kartoffel | 20 s · 1 Kartoffel + 50 Wasser → 3 Kartoffeln |
  | Zuckerrohr | 15 s · 1 Zuckerrohr + 50 Wasser → 2 Zuckerrohr |
  | Setzling → Holz | 40 s · 1 Setzling + 100 Wasser → 4 Stämme + 1 Setzling (Holz für Strommasten ohne Abholzen) |

- **Verwendung:**
  - Verkauf am Handelsposten, also Credits für die TD
  - **Bio-Brennstoff** (Assembler: 10 Pflanzenmasse → 1, Brennwert wie fester Brennstoff 12 MJ) für Kessel, Öfen und
    Brenner-Maschinen
  - Nahrung
- Forschung „Landwirtschaft" (30 × R) schaltet das Gewächshaus frei, „Bio-Brennstoff" (50 × R+G) den Brennstoff.
- Später optional Dünger aus der Chemie-Kette (+50 % Ertrag).

---

## 8. Umbaupakete

Jedes Paket ist ein eigener Commit und für sich spielbar. Tests und README wie in der Definition of Done.

| Paket | Inhalt | Ersetzt Backlog-Punkt |
|---|---|---|
| **U0 – Arena-Fehler** ✅ | Backlog-Paket 1 und 2 (D1–D6, C2); unabhängig vom Umbau, zuerst | – |
| **U1 – Grundlagen** ✅ | Tempo-Faktoren (§2), Energie-Einheit 1 kW = 1 FE/t, Team-Chunkloader, Brennwerte nach Factorio, Vanilla-Werkzeugrezepte entfernen | – |
| **U2 – Forschungssystem** ✅ | Registry `craftorio:research`, Labor (Block, Menü, Paketverbrauch, Parallelbetrieb), Terminal-Tab Forschung mit Baum, Warteschlange und Fortschritt, Team-Wissen = Forschungen, Rezeptsperre für Werkbank und Maschinen, Migration der Baupläne (alte `unlocked`-Einträge werden verworfen, neue Welt empfohlen), EMI-Anzeige gesperrter Rezepte | A1, A3 |
| **U3 – Rot** ✅ | Items und Rezepte aus §4.1–4.3 bis Rot: Steinofen, Stein-Erzfeld, Zahnrad, Kabel, Schaltkreis, Rohr, Brenner-Greifarm, Kisten, Kessel, Dampfmaschine, Offshore-Pumpe (ohne Flüssigkeitsnetz: Kessel mit Wasser direkt aus der Pumpe, siehe U6), Labor, Rotes Paket, Montagemaschine 1; Handarbeit an der Werkbank; Presse entfernen | A1 |
| **U4 – Logistik** ✅ | Unterflurband, Splitter mit Priorität und Filter, langer, schneller und Filter-Greifarm, Bandstufen nach §4.3, steigende Bänder (Backlog B2) | B1, B2 |
| **U5 – Grün und Stahl** ✅ | Grünes Paket, Stahl, Stahlofen, mittlerer Strommast, Montagemaschine 2, Motor, Solarpanel (Tag/Nacht) | A1 |
| **U6 – Flüssigkeiten** ✅ | Flüssigkeitsnetz (Rohre, Unterführung, Pumpe, Tank; NeoForge `IFluidHandler`, eigene Fluids Rohöl, Schweröl, Leichtöl, Petroleum, Schwefelsäure, Schmiermittel, Dampf), Kessel/Dampfmaschine auf Dampf umstellen | – |
| **U7 – Öl und Höhlen** ✅ | Ölquellen in den Höhlen, Pumpjack, Raffinerie, Chemiefabrik, Kunststoff, Schwefel, Batterie, Akku (Backlog C1), fortschrittlicher Schaltkreis, Blaues Paket, Höhleneingang über Forschung; Zinn/Blei/Titan usw. entfernen | C1 |
| **U8 – Militär und TD** ✅ | Magazine, Granate, Mauer, Graues Paket, Geschütz-, Laser- und Flammenwerfer-Turm in der Arena, Einspeiser mit Flüssigkeit, Arena-Siegel als Forschungsschlüssel | – |
| **U9 – Leitfaden und Balancing** ✅ | Leitfaden aus Meilensteinen (§10), kleine Belohnungen, Progressionstest (jede Forschung mit dem bis dahin Freigeschalteten erfüllbar), Zeitmessung im Terminal, Handbuch-Seiten | A2 |
| **U10 – Pflanzen** ✅ | Gewächshaus, Pflanzenrezepte, Bio-Brennstoff | – |
| **U11 – Endgame** | Ausformuliert in **§11** (Pakete U11a–U11g): Bereinigung, Uran, Kernkraft, Module, Lila und Gelb, TD-Endgame, Leitfaden | – |

Weiterhin gültig aus dem Backlog: A4 (Beschreibungen), B3 (Lager-Depot), D7 (Arena-Karten), E1–E3 (Höhlen).

**Automatische Absicherung (Pflicht ab U2):**
- **Progressionstest:** Start mit der Startmenge (Handabbau, Start-Rezepte). Dann Forschungen in einer gültigen
  Reihenfolge abschließen; jede muss mit bereits herstellbaren Paketen und Schlüsseln bezahlbar sein. Jede Rezeptzutat
  muss herstellbar sein, bevor das Rezept freigeschaltet ist.
- **Rezepttabellen-Test:** Die Tabelle aus §4 liegt als Testdaten vor; der Test vergleicht die generierten Rezepte
  damit (Mengen und Zeiten), sodass Abweichungen von Factorio auffallen.

---

## 9. Migration bestehender Inhalte

| Bisher | Neu |
|---|---|
| Baupläne + Credits-Freischaltung | Forschung; Handelsposten/Credits nur noch für die TD |
| Werkbank Stufe 1–3 + Aufrüstsätze | eine Werkbank; Aufrüstsätze entfallen |
| `craftorio:iron_plate`, Presse | entfallen (Eisenbarren = Eisenplatte) |
| Kupferkabel, Zahnrad, Schaltkreis | bleiben (Rezepte nach Factorio) |
| Motor | = Engine unit (Rezept nach Factorio) |
| Schmelzofen elektrisch | = Elektroofen (Rezept nach Factorio) |
| Kohle-Generator | ersetzt durch Kessel + Dampfmaschine |
| Reaktor, Brennstäbe, Uran-Pellet | ersetzt durch Kernreaktor, Uran-Brennstoffzelle, U-235/U-238 (§11.10) |
| Zinn, Blei, Titan, Schwefel-Feld, Quarz, Diamant, Gold-Feld, Kristall, Energiekristall, Resonanzkristall, Drill Core, Deep Core | entfallen bzw. werden zu Arena-Siegeln |
| Leitfaden (33 Schritte) | neu aus Meilensteinen |

**Bestehende Welten:** Der Umbau ist nicht abwärtskompatibel. Eine neue Welt wird empfohlen. Entfernte Items über
`MissingMappingsEvent` auf Ersatz-Items abbilden oder entfernen, damit alte Welten wenigstens laden.

---

## 10. Leitfaden (Entwurf)

1. Eisenerz per Hand abbauen (16)
2. Stein abbauen, Steinofen bauen
3. Eisenplatten schmelzen (Meilenstein: 20 Eisenplatten)
4. Brenner-Bohrer bauen und auf Eisen setzen
5. Förderband und Brenner-Greifarm bauen
6. Kessel, Dampfmaschine, Offshore-Pumpe und Strommast bauen (erster Strom)
7. Labor bauen, 10 rote Pakete herstellen
8. Forschung „Automatisierung"
9. Montagemaschine baut rote Pakete
10. Handelsposten bauen und das erste Mal verkaufen
11. Arena-Tor bauen, TD-Level 1
12. Forschung „Logistik-Wissenschaftspaket" (TD-Level 5 für das Bronze-Siegel)
13. Grüne Pakete automatisieren
14. Stahl …

Weitere Schritte folgen dem Forschungsbaum. Belohnungen nur als Items (Kohle, Zahnräder, Bänder) oder kleine Credits
für die TD.

---

## 11. U11 – Endgame (ausformuliert)

Ziel: Das Factorio-1.1-Endgame bis einschließlich **Lila (Produktion)** und **Gelb (Nutzlast)** mit Uran, Kernkraft,
Modulen, Montagemaschine 3 und Beacons, dazu das passende TD-Endgame bis Level 50.
**Nicht** enthalten: Raketensilo und Weltraum-Paket (Entscheidung U11-E1), Roboter, Züge, Schaltungsnetz.

Alle Zahlen sind Factorio-1.1-Referenzwerte aus dem Gedächtnis. **Vor der Umsetzung jede Zeile gegen
wiki.factorio.com (Version 1.1) prüfen**, Abweichungen im Code dokumentieren und die Testdaten des
Rezepttabellen-Tests (§8) um alle Zeilen dieses Abschnitts erweitern. Unsichere Werte sind mit *(prüfen)* markiert.

### 11.1 Befund: Stand nach U10

| Thema | Stand im Code | Problem für U11 |
|---|---|---|
| Forschungen `mine_shaft`, `deep_mining`, `express_belts`, `nuclear_power` | `ModResearch`: nur **rote** Pakete | Endgame kostet nur Rot, das passt nicht zu Factorio |
| `electric_smelting` | 50 × R, 30 s | Factorio: „Fortgeschrittene Materialverarbeitung 2" 250 × R+G+B |
| Fortgeschrittene Ölverarbeitung, Cracking, Schmiermittel | Fluide `HEAVY_OIL`, `LIGHT_OIL`, `LUBRICANT` registriert, **keine Rezepte**; `FluidRecipes.Recipe` hat nur **einen** Flüssigkeitsausgang | Express-Band, Elektromotor und Beacons brauchen Schmiermittel bzw. davon abhängige Teile |
| Express-Band | Bauplan: 4 schnelle Bänder + 2 Stahl + 1 fortschrittlicher Schaltkreis | Factorio: 10 Zahnräder + 1 schnelles Band + 20 Schmiermittel |
| Uran | Uranerz-Feld in den Minen; `uranium_pellet` (Assembler, 1 Erz), `fuel_rod` (3 Pellets + 2 Stahl) | Kein Schwefelsäure-Abbau, keine Zentrifuge, kein U-235/U-238 |
| Reaktor | `GeneratorType.REACTOR`: erzeugt direkt 8.000 FE/t, Brennstab 6.000 Ticks | Factorio: Wärme → Wärmetauscher → Dampf → Turbine |
| Maschinen | `MachineType`: Montagemaschine 1/2, Öfen; `FluidMachineType`: Chemiefabrik, Raffinerie, Gewächshaus | Keine Modul-Slots, keine Montagemaschine 3, kein Beacon, keine Zentrifuge |
| Arena-Siegel | `LevelPlan.keyReward`: 5/10/20/30 | Keine Siegel für Lila und Gelb |
| Bohrer | `DrillBlockEntity` ohne Flüssigkeitseingang | Uranabbau braucht Schwefelsäure |

### 11.2 Neue Rohstoffe, Items und Flüssigkeiten

| Item/Flüssigkeit | ID (Vorschlag) | Art |
|---|---|---|
| Beton | `craftorio:concrete` | Item **und** platzierbarer Block (Deko in Minecraft) |
| Schiene | `minecraft:rail` (Vanilla-Item, Rezept ersetzen) | Zwischenprodukt für Lila |
| Uran-235 | `craftorio:uranium_235` | Item |
| Uran-238 | `craftorio:uranium_238` | Item |
| Uran-Brennstoffzelle | `craftorio:uranium_fuel_cell` | Item (Reaktor-Brennstoff) |
| Verbrauchte Brennstoffzelle | `craftorio:used_up_fuel_cell` | Item |
| Uran-Magazin | `craftorio:uranium_magazine` | Munition (Geschützturm) |
| Prozessor (blauer Schaltkreis) | `craftorio:processing_unit` | Item |
| Elektromotor | `craftorio:electric_engine` | Item |
| Flugrahmen | `craftorio:flying_robot_frame` | Item (nur Zwischenprodukt, keine Roboter) |
| Leichtbaustruktur | `craftorio:low_density_structure` | Item |
| Produktions-Wissenschaftspaket (lila) | `craftorio:production_science` | Item |
| Nutzlast-Wissenschaftspaket (gelb) | `craftorio:utility_science` | Item |
| Module Geschwindigkeit/Effizienz/Produktivität 1–3 | `craftorio:speed_module_1` … | Items (9 Stück) |
| Diamant-Siegel, Sternen-Siegel | `craftorio:diamond_seal`, `craftorio:star_seal` | Arena-Schlüssel |
| Hochdruckdampf (500 °C) | `craftorio:hot_steam` | Flüssigkeit, siehe §11.5 |

### 11.3 Rezepte

**Zwischenprodukte und Wissenschaft**

| Item | Rezept | Wo |
|---|---|---|
| Beton ×10 | 10 · 5 Steinziegel + 1 Eisenerz + 100 Wasser | Montagemaschine 2/3 (Flüssigkeitseingang) |
| Schiene ×2 | 0,5 · 1 Stein + 1 Eisenstange + 1 Stahl | Hand/Assembler |
| Prozessor | 10 · 20 Schaltkreise + 2 fortschrittliche Schaltkreise + 5 Schwefelsäure | Montagemaschine 2/3 |
| Elektromotor | 10 · 1 Motor + 2 Schaltkreise + 15 Schmiermittel | Montagemaschine 2/3 |
| Flugrahmen | 20 · 1 Elektromotor + 2 Batterien + 1 Stahl + 3 Schaltkreise | Assembler |
| Leichtbaustruktur | 20 · 2 Stahl + 20 Kupferplatten + 5 Kunststoff *(prüfen: Zeit)* | Assembler |
| Produktions-Paket ×3 | 21 · 1 Elektroofen + 1 Produktivitätsmodul 1 + 30 Schienen | Assembler |
| Nutzlast-Paket ×3 | 21 · 2 Flugrahmen + 1 Leichtbaustruktur + 2 Prozessoren | Assembler |

**Öl (Nachtrag, Voraussetzung für Schmiermittel)**

| Rezept | Werte | Wo |
|---|---|---|
| Fortgeschrittene Ölverarbeitung | 5 · 100 Rohöl + 50 Wasser → 25 Schweröl + 45 Leichtöl + 55 Petroleum | Raffinerie |
| Schweröl-Cracking | 2 · 40 Schweröl + 30 Wasser → 30 Leichtöl | Chemiefabrik |
| Leichtöl-Cracking | 2 · 30 Leichtöl + 30 Wasser → 20 Petroleum | Chemiefabrik |
| Schmiermittel | 1 · 10 Schweröl → 10 Schmiermittel | Chemiefabrik |
| Fester Brennstoff | 2 · 10 Leichtöl → 1 (12 MJ) *(prüfen)* | Chemiefabrik |

Technisch: `FluidRecipes.Recipe` auf **mehrere Flüssigkeitsausgänge** erweitern (Liste statt einzelnem `FluidStack`).
Die Raffinerie bekommt drei Ausgangstanks. Hat ein Ausgang keinen Platz, stoppt die Raffinerie wie in Factorio.

**Uran**

| Rezept | Werte | Wo |
|---|---|---|
| Uranabbau | Abbauzeit 2 (halbe Rate gegenüber Eisen) + **Schwefelsäure pro Erz** *(prüfen: 10 oder 1 Einheit)* | Elektro-/Tiefenbohrer mit Flüssigkeitseingang |
| Uranverarbeitung | 12 · 10 Uranerz → 1 Uran mit **0,7 % U-235 / 99,3 % U-238** | Zentrifuge |
| Uran-Brennstoffzelle ×10 | 10 · 10 Eisenplatten + 1 U-235 + 19 U-238 | Assembler |
| Kernbrennstoff-Wiederaufbereitung | 60 · 5 verbrauchte Zellen → 3 U-238 | Zentrifuge |
| Kovarex-Anreicherung | 60 · 40 U-235 + 5 U-238 → 41 U-235 + 2 U-238 | Zentrifuge |
| Uran-Magazin | 10 · 1 panzerbrechendes Magazin + 1 U-238 | Assembler |

Technisch: Ergebnisse mit **Wahrscheinlichkeit** (Uranverarbeitung) als Rezeptfeld `chance` pro Ausgang. Der Zufall
kommt aus dem Level-RNG, der Test prüft über 10.000 Durchläufe 0,7 % ± 0,2 %.

**Maschinen und Gebäude**

| Maschine | Rezept | Kennwerte (Factorio) |
|---|---|---|
| Zentrifuge | 4 · 100 Beton + 50 Stahl + 100 fortschrittliche Schaltkreise + 100 Zahnräder | Geschw. 1, 350 kW, 2 Modul-Slots; Einzelblock wie Chemiefabrik |
| Kernreaktor | 8 · 500 Beton + 500 Stahl + 500 fortschrittliche Schaltkreise + 500 Kupferplatten | 40 MW Wärme, Brennstoffzelle 200 s, Nachbarbonus +100 % pro angrenzendem aktivem Reaktor, max. 1000 °C |
| Wärmerohr | 1 · 10 Stahl + 20 Kupferplatten | leitet Wärme, max. 1000 °C |
| Wärmetauscher | 3 · 10 Stahl + 100 Kupferplatten + 10 Rohre | 10 MW, braucht ≥ 500 °C, Wasser → Hochdruckdampf |
| Dampfturbine | 3 · 50 Zahnräder + 50 Kupferplatten + 20 Rohre | 5,8 MW, verbraucht 60 Hochdruckdampf/s |
| Montagemaschine 3 | 0,5 · 2 Montagemaschinen 2 + 4 Geschwindigkeitsmodule 1 | Geschw. 1,25, 375 kW, 4 Modul-Slots |
| Beacon | 15 · 20 Schaltkreise + 20 fortschrittliche Schaltkreise + 10 Stahl + 10 Kupferkabel | 480 kW, 2 Modul-Slots, gibt 50 % der Modulwirkung weiter, Reichweite 9×9 um den Beacon |

**Module**

| Modul | Rezept | Wirkung |
|---|---|---|
| Geschwindigkeit 1 | 15 · 5 fortschrittliche Schaltkreise + 5 Schaltkreise | +20 % Tempo, +50 % Energie |
| Geschwindigkeit 2 | 30 · 4 Geschw. 1 + 5 fortschrittliche Schaltkreise + 5 Prozessoren | +30 % Tempo, +60 % Energie |
| Geschwindigkeit 3 | 60 · 4 Geschw. 2 + 5 fortschrittliche Schaltkreise + 5 Prozessoren | +50 % Tempo, +70 % Energie |
| Effizienz 1/2/3 | wie Geschwindigkeit (jeweilige Vorstufe) | −30 % / −40 % / −50 % Energie |
| Produktivität 1/2/3 | wie Geschwindigkeit (jeweilige Vorstufe) | +4 % / +6 % / +10 % Produktivität, −5 % / −10 % / −15 % Tempo, +40 % / +60 % / +80 % Energie |

### 11.4 Modul-System (Regeln)

- **Slots:**

  | Maschine | Slots |
  |---|---|
  | Montagemaschine 1 | 0 |
  | Montagemaschine 2 | 2 |
  | Montagemaschine 3 | 4 |
  | Elektroofen | 2 |
  | Elektro-Bohrer | 3 |
  | Tiefenbohrer (eigene Stufe) | 4 |
  | Labor | 2 |
  | Chemiefabrik | 3 |
  | Raffinerie | 3 |
  | Zentrifuge | 2 |
  | Pumpjack | 2 |
  | Beacon | 2 |

  Brennstoff-Maschinen, Gewächshaus und Türme haben keine Slots.
- **Wirkung:** Alle Modul- und Beacon-Effekte einer Maschine werden addiert.
  - Tempo und Energie haben jeweils **mindestens 20 %**.
  - Produktivität füllt einen Bonusbalken; bei 100 % gibt es einen Extra-Durchlauf ohne Zutaten.
- **Produktivität** ist nur für **Zwischenprodukte** erlaubt: alle Items aus §4.1 und §11.3 „Zwischenprodukte",
  Wissenschaftspakete, Uranverarbeitung, Kovarex-Anreicherung und Ölrezepte, nicht für Gebäude, Module oder Munition.
  Die Liste als Item-Tag `craftorio:productivity_allowed` führen. Die Maschine lehnt ein unerlaubtes Rezept mit
  Produktivitätsmodul ab (Meldung im GUI).
- **Beacons** nehmen nur Geschwindigkeits- und Effizienzmodule. Sie wirken auf alle Maschinen mit Modul-Slots, deren
  Block im 9×9×3-Bereich um den Beacon liegt. Mehrere Beacons addieren sich.
- **Umsetzung:**
  - Reine Klasse `ModuleEffects` (Summe, Grenzen, Produktivitätsbalken) mit Unit-Tests.
  - Ein Interface `ModuleHost` für alle Maschinen mit Slots.
  - Ein Beacon-Index pro Level, damit nicht jede Maschine jeden Tick sucht (Neuberechnung bei Platzieren/Abbauen
    eines Beacons oder Moduls).
- Bohrer: Tempo wirkt auf die Abbaurate. Produktivität erhöht die Ausbeute pro Feldblock; die Maximaldurchsatz-Regel
  der Erzfelder (Konzept §3.3) bleibt.

### 11.5 Kernkraft (Regeln)

- **Wärmenetz:**
  - Reaktor, Wärmerohr und Wärmetauscher haben eine Temperatur (15–1000 °C) und eine Wärmekapazität.
  - Pro Tick fließt Wärme zwischen angrenzenden Wärmeblöcken proportional zur Temperaturdifferenz.
  - Reine Logik in `HeatNetwork`/`HeatLogic` mit Unit-Tests: Leitung, Grenzwerte, Nachbarbonus.
- **Reaktor:**
  - Verbrennt eine Brennstoffzelle in 200 s und gibt dabei 40 MW × (1 + Anzahl angrenzender aktiver Reaktoren)
    als Wärme ab.
  - Wie in Factorio verbrennt er die Zelle **unabhängig von der Last**.
  - Es gibt keine Schaltkreise, daher eine Reaktor-Steuerung im GUI: „Neue Zelle nur einlegen, wenn Temperatur unter
    X °C" (Regler, Standard 900 °C).
  - Verbrauchte Zellen kommen in einen Ausgangsslot (per Greifarm entnehmbar).
- **Wärmetauscher:** Nimmt Wasser (Flüssigkeitsnetz), gibt bei ≥ 500 °C bis 10 MW ab und erzeugt Hochdruckdampf.
  Faustregel Factorio: 1 Reaktor : 4 Wärmetauscher : 7 Turbinen.
- **Hochdruckdampf** ist ein eigenes Fluid (500 °C). Temperatur als Fluid-Eigenschaft wäre ein großer Umbau des
  Netzes, daher dieser Kompromiss. Dampfturbinen nehmen nur Hochdruckdampf, Dampfmaschinen nur normalen Dampf.
- **Einheiten:** 1 kW = 1 FE/t, also 40 MW = 40.000 FE/t. Stromnetz, Akkus und Anzeige auf `long` prüfen, wo Summen
  über `Integer.MAX_VALUE` wachsen können.
- Keine Kernschmelze und keine Strahlung, wie in Factorio.
- **Migration:** Der alte Reaktor (`GeneratorType.REACTOR`), `fuel_rod` und `uranium_pellet` entfallen
  (`MissingMappingsEvent`: `fuel_rod` → `uranium_fuel_cell`, `uranium_pellet` → `uranium_238`, alter Reaktor → neuer
  Kernreaktor).

### 11.6 Forschung

Kosten Factorio 1.1 *(alle prüfen)*. Abkürzungen: R rot, G grün, B blau, M Militär, P Produktion (lila), U Nutzlast (gelb).
Der Paket-Enum `Research.Pack` bekommt `PRODUCTION('P')` und `UTILITY('U')`; Labor und Terminal zeigen sie an.

**Korrekturen bestehender Forschungen**

| Forschung | Bisher | Neu |
|---|---|---|
| `electric_smelting` → „Fortgeschrittene Materialverarbeitung 2" | 50 × R, 30 s | 250 × R+G+B, 30 s; Voraussetzung Fortgeschrittene Materialverarbeitung, Chemie-Paket |
| `mine_shaft` | 300 × R, Platin-Siegel | 300 × R+G+B, 30 s, Platin-Siegel; Voraussetzung Aufzüge, Chemie-Paket |
| `deep_mining` | 200 × R | 200 × R+G+B, 30 s; Voraussetzung Minenschacht |
| `express_belts` | 150 × R, Minenschacht | wird **Logistik 3**: 300 × R+G+B+P, 15 s; Voraussetzung Logistik 2, Schmiermittel, Produktions-Paket; Express-Band mit Schmiermittel-Rezept (§4.3) |
| `nuclear_power` | 300 × R, schaltet alten Reaktor frei | siehe unten |

**Neue Forschungen**

| Forschung | Kosten | Voraussetzung | Schaltet frei |
|---|---|---|---|
| Fortgeschrittene Ölverarbeitung | 75 × R+G+B, 30 s | Chemie-Paket | Fortgeschrittene Ölverarbeitung, Cracking, fester Brennstoff |
| Schmiermittel | 50 × R+G+B, 30 s | Fortgeschrittene Ölverarbeitung | Schmiermittel |
| Beton | 250 × R+G, 10 s | Fortgeschrittene Materialverarbeitung, Automatisierung 2 | Beton |
| Schienenbau (statt „Eisenbahn") | 75 × R+G, 30 s | Logistik 2, Motor | Schiene |
| Elektromotor | 50 × R+G+B, 30 s | Schmiermittel | Elektromotor |
| Robotik (nur Bauteile) | 75 × R+G+B, 30 s | Elektromotor, Batterie | Flugrahmen |
| Fortschrittliche Elektronik 2 | 300 × R+G+B, 30 s | Fortschrittliche Elektronik, Chemie-Paket | Prozessor |
| Geschwindigkeitsmodul | 50 × R+G, 30 s | Fortschrittliche Elektronik | Geschwindigkeitsmodul 1 |
| Effizienzmodul | 50 × R+G, 30 s | Fortschrittliche Elektronik | Effizienzmodul 1 |
| Produktivitätsmodul | 50 × R+G, 30 s | Fortschrittliche Elektronik | Produktivitätsmodul 1 |
| Module 2 (je Art) | 75 × R+G+B, 30 s | Modul 1 der Art, Fortschrittliche Elektronik 2 | Modul 2 der Art |
| Uranverarbeitung | 200 × R+G+B, 30 s | Beton, Chemie-Paket, **Minenschacht** | Zentrifuge, Uranverarbeitung, Abbau von Uranerz (Bohrer mit Säure) |
| Kernkraft | 800 × R+G+B, 30 s | Uranverarbeitung | Kernreaktor, Wärmerohr, Wärmetauscher, Dampfturbine, Uran-Brennstoffzelle |
| Uran-Munition | 1000 × R+G+B+M+P+U, 45 s *(prüfen; ggf. mit `pacing.researchCost` senken)* | Uranverarbeitung, Nutzlast-Paket | Uran-Magazin |
| **Produktions-Wissenschaftspaket** | 100 × R+G+B, 5 s + **Diamant-Siegel** | Produktivitätsmodul, Fortgeschrittene Materialverarbeitung 2, Schienenbau | Lila Paket |
| Leichtbaustruktur | 300 × R+G+B+P, 45 s | Fortgeschrittene Materialverarbeitung 2, Produktions-Paket | Leichtbaustruktur |
| **Nutzlast-Wissenschaftspaket** | 100 × R+G+B, 5 s + **Sternen-Siegel** | Robotik, Fortschrittliche Elektronik 2, Leichtbaustruktur | Gelbes Paket |
| Automatisierung 3 | 150 × R+G+B+P, 60 s | Geschwindigkeitsmodul, Produktions-Paket | Montagemaschine 3 |
| Effektübertragung | 75 × R+G+B+P, 30 s | Fortschrittliche Elektronik 2, Produktions-Paket | Beacon |
| Kovarex-Anreicherung | 1500 × R+G+B+P, 30 s | Kernkraft, Produktions-Paket | Kovarex-Anreicherung |
| Kernbrennstoff-Wiederaufbereitung | 50 × R+G+B+P, 30 s | Kernkraft, Produktions-Paket | Wiederaufbereitung |
| Module 3 (je Art) | 300 × R+G+B+P+U, 60 s | Modul 2 der Art, Produktions- und Nutzlast-Paket | Modul 3 der Art |
| Bergbauproduktivität (endlos, optional) | Stufe n: 250 × n × R+G+B+P+U, 60 s *(eigene Vereinfachung)* | Nutzlast-Paket | +10 % Bohrer-Ausbeute je Stufe |

### 11.7 Tower Defense im Endgame

- **Siegel:**
  - Platin (Level 30) bleibt beim Minenschacht.
  - **Diamant-Siegel (Level 40)** für das Produktions-Paket, **Sternen-Siegel (Level 50)** für das Nutzlast-Paket.
  - `LevelPlan.KeyReward` und `TowerDefense.keyItem` erweitern; Texturen im Stil der bisherigen Siegel.
- **Munition:** Uran-Magazin für den Geschützturm (Factorio-Verhältnis: Schaden etwa ×2,4 gegenüber panzerbrechend;
  Werte in `TowerStats`). Der Arena-Einspeiser nimmt es an.
- **Gegner Level 31–50:**
  - Neuer Gegnertyp **„Behemoth"** ab Level 35: hohe HP, Panzerung, die flachen Schaden reduziert (erst
    panzerbrechende, Uran-Munition, Laser oder Flammen sind wirksam).
  - Level 50 ist ein **Finale** mit einer „Schwarmkönigin" (Boss, mehrere Phasen).
  - Ziel der Kurve: Level 40 ist mit Laser- und Flammentürmen der Stufe 2 schaffbar, Level 50 braucht Uran-Munition
    oder Module/Beacons in der Fabrik für genug Munition und Strom.
  - `LevelPlan`-Kurve mit Unit-Test absichern (erwarteter Gesamtschaden der Welle vs. Referenz-Turmset).
- **Credits im Endgame:**
  - Neue Ausgaben: Turmstufe 4 und 5 (teuer), „Arena-Upgrade: zweites Tor" optional später.
  - Verkaufspreise der neuen Items nach der bestehenden Regel aus `BalanceGameTests`: jede Verarbeitungsstufe ≥ 10 %
    Mehrwert.
- **Tesla-Turm:** Bleibt wie er ist (Entscheidung aus §4.4 vertagt, siehe U11-E3).

### 11.8 Minen

- Die Minen bleiben die **Uran-Ebene**.
- Neu: Uranfelder brauchen **Schwefelsäure**. Der Elektro- und Tiefenbohrer bekommt einen Flüssigkeitseingang
  (Rohr von hinten oder von oben, `IFluidHandler`-Capability) und einen Tank (z. B. 1.000 Einheiten); ohne Säure baut
  er Uran nicht ab (Status im GUI).
- Anschluss nach oben: Der Aufzug (`ElevatorBlockEntity`) transportiert heute **keine** Flüssigkeiten. Empfehlung:
  eine **Flüssigkeits-Aufzug**-Variante (Sender/Empfänger wie beim Item-Aufzug, `IFluidHandler`, z. B. 1.000
  Einheiten/s). Alternative: Schwefelsäure in den Minen herstellen (Schwefel und Eisen per Item-Aufzug hinunter,
  Wasser über eine Offshore-Pumpe an einem Minensee). Beides ist erlaubt; der Flüssigkeits-Aufzug ist Pflicht, weil
  auch Schmiermittel und Wasser später Ebenen wechseln sollen.
- Optional: Mehr Deko und eine eigene Palette in der Minenschicht (Beton-Ruinen, Warnschilder) als Hinweis auf das
  Uran-Thema.

### 11.9 Spieltempo (Ziele für Spieltests)

Fortsetzung der Tabelle aus §2, grob wie ein erster Factorio-Durchlauf ohne Rakete:

| Meilenstein | Zielzeit (Gesamtspielzeit) |
|---|---|
| Minenschacht offen (TD-Level 30) | 12–15 h |
| Uran gefördert und verarbeitet | 14–17 h |
| Kernkraft läuft | 16–20 h |
| Lila automatisiert (TD-Level 40) | 18–24 h |
| Gelb automatisiert (TD-Level 50) | 24–32 h |
| Kovarex läuft | 26–34 h |

- Die TD-Level 30/40/50 müssen zeitlich zu den Paketstufen passen. Richtwert: 1 TD-Level ≈ 15–25 Minuten inklusive
  Aufbau; im Spieltest über die Terminal-Zeitmessung (U9) prüfen.
- Justieren zuerst über `pacing.researchCost` und die TD-Kurve, nicht über einzelne Rezepte.

### 11.10 Migration

| Bisher | Neu |
|---|---|
| `craftorio:reactor` (8 MW direkt Strom) | Kernreaktor mit Wärmenetz (gleiche ID beibehalten, BlockEntity neu) |
| `craftorio:fuel_rod` | `craftorio:uranium_fuel_cell` |
| `craftorio:uranium_pellet` | `craftorio:uranium_238` |
| Forschung `express_belts` | `logistics_3` (alte ID beim Laden auf neue abbilden) |
| Forschung `electric_smelting` | `advanced_material_processing_2` (alte ID abbilden) |
| Express-Band-Bauplan (Stahl + fortschrittlicher Schaltkreis) | Rezept nach Factorio mit Schmiermittel |

Abgeschlossene Forschungen mit geänderten Kosten bleiben abgeschlossen.

### 11.11 Pakete (Reihenfolge, je ein Commit)

| Paket | Inhalt | Akzeptanz |
|---|---|---|
| **U11a – Bereinigung und Öl-Nachtrag** | Forschungskorrekturen (§11.6 oben), mehrere Flüssigkeitsausgänge, fortgeschrittene Ölverarbeitung, Cracking, Schmiermittel, fester Brennstoff, Express-Band mit Schmiermittel, Beton (Item + Block), Schiene, Prozessor, Elektromotor, Flugrahmen, Leichtbaustruktur, Montagemaschine 2 mit Flüssigkeitseingang für Beton/Prozessor | Rezepttabellen-Test um §11.3 erweitert und grün; Progressionstest grün; GameTest: Raffinerie liefert drei Fluide, Cracking wandelt um |
| **U11b – Uran** | Bohrer mit Flüssigkeitseingang, Uranabbau mit Säure, Säure in die Minen (§11.8), Zentrifuge (Block, Menü), Uranverarbeitung mit Wahrscheinlichkeit, U-235/U-238, Brennstoffzelle, Uran-Magazin, Forschung Uranverarbeitung | GameTests: Bohrer ohne Säure fördert nichts, mit Säure schon; Zentrifuge 10.000 Läufe ≈ 0,7 % U-235 |
| **U11c – Kernkraft** | Wärmenetz (`HeatLogic` + Unit-Tests), Kernreaktor mit Steuerung und Nachbarbonus, Wärmerohr, Wärmetauscher, Hochdruckdampf, Dampfturbine, Wiederaufbereitung, alter Reaktor migriert | GameTest: 1 Reaktor + 4 Tauscher + 7 Turbinen versorgen 40 MW Last stabil; Nachbarbonus verdoppelt die Wärme; Zelle hält 200 s |
| **U11d – Module, Montagemaschine 3, Beacon** | `ModuleEffects` + Unit-Tests, Slots in allen Maschinen aus §11.4, Produktivitäts-Tag, Montagemaschine 3, Beacon mit Index, Module 1–3 (Stufe 3 erst nutzbar nach U11e), Anzeige der Effekte im Maschinen-GUI und in Jade | Unit-Tests: Grenzen 20 %, Summe, Produktivitätsbalken; GameTest: Geschwindigkeitsmodul verkürzt Laufzeit um 20 %, Produktivitätsmodul in Gebäude-Rezept abgelehnt, Beacon wirkt mit 50 % |
| **U11e – Lila und Gelb** | Pakete `PRODUCTION`/`UTILITY` im Enum, Labor und Terminal; Rezepte der Pakete; neue Forschungen aus §11.6; Diamant- und Sternen-Siegel; Kovarex; Module 3; Bergbauproduktivität (optional) | Progressionstest bis Gelb grün (mit Siegeln); GameTest: Labor akzeptiert Lila/Gelb |
| **U11f – TD-Endgame** | Behemoth, Schwarmkönigin (Level 50), Kurve 31–50, Uran-Munition im Einspeiser und in `TowerStats`, Turmstufen 4–5, Siegel-Belohnungen 40/50 | Unit-Test der Level-Kurve; GameTest: Level 50 vergibt das Sternen-Siegel; Uran-Munition wird verbraucht |
| **U11g – Leitfaden, Handbuch, Balancing** | Leitfaden-Schritte (§11.12), Handbuch-Seiten zu Modulen, Kernkraft und Uran, Verkaufspreise, Zeitmessung um die Meilensteine aus §11.9, README-Abschnitt „Endgame" | Alle Tests grün; Leitfaden bis Gelb durchspielbar (Progressionstest deckt Leitfaden-Reihenfolge ab) |

### 11.12 Leitfaden (Fortsetzung, Entwurf)

1. TD-Level 30 schaffen und den Minenschacht erforschen (Platin-Siegel)
2. Schwefelsäure in die Minen leiten, erstes Uranerz fördern
3. Zentrifuge bauen, erstes U-235 gewinnen
4. Kernreaktor mit Wärmetauschern und Turbinen in Betrieb nehmen
5. Module erforschen und in Montagemaschinen einsetzen
6. TD-Level 40 → Diamant-Siegel → Lila Pakete automatisieren
7. Montagemaschine 3 und Beacons bauen
8. TD-Level 50 (Schwarmkönigin) → Sternen-Siegel → Gelbe Pakete automatisieren
9. Kovarex-Anreicherung starten
10. Module 3 erforschen

Belohnungen wie bisher klein (Items oder Credits für die TD).

### 11.13 Offene Entscheidungen U11

| ID | Frage | Empfehlung (gilt, falls nichts anderes entschieden wird) |
|---|---|---|
| U11-E1 | Gibt es ein Endziel wie die Rakete in Factorio (Raketensilo, Raketenteile, Satellit, Weltraum-Paket)? | Für U11 **nein**; Endziel ist TD-Level 50 (Schwarmkönigin) + Gelb automatisiert. Rakete als mögliches **U12** später. |
| U11-E2 | Temperatur als echte Fluid-Eigenschaft statt eigenem Hochdruckdampf? | **Nein**, eigenes Fluid (einfacher, reicht für Factorio-Verhältnisse). |
| U11-E3 | Tesla-Turm im Endgame aufwerten (z. B. Kettenblitz mit Modulen) oder unverändert lassen? | Unverändert lassen; Laser, Flammen und Uran-Munition tragen das Endgame. |
| U11-E4 | Endlose Forschung „Bergbauproduktivität" aufnehmen? | **Ja**, als optionaler Teil von U11e (Beschäftigung nach Gelb). |
