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
  | Uranverarbeitung (später) | Platin-Siegel (TD-Level 30) |

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
| **U5 – Grün und Stahl** | Grünes Paket, Stahl, Stahlofen, mittlerer Strommast, Montagemaschine 2, Motor, Solarpanel (Tag/Nacht) | A1 |
| **U6 – Flüssigkeiten** | Flüssigkeitsnetz (Rohre, Unterführung, Pumpe, Tank; NeoForge `IFluidHandler`, eigene Fluids Rohöl, Schweröl, Leichtöl, Petroleum, Schwefelsäure, Schmiermittel, Dampf), Kessel/Dampfmaschine auf Dampf umstellen | – |
| **U7 – Öl und Höhlen** | Ölquellen in den Höhlen, Pumpjack, Raffinerie, Chemiefabrik, Kunststoff, Schwefel, Batterie, Akku (Backlog C1), fortschrittlicher Schaltkreis, Blaues Paket, Höhleneingang über Forschung; Zinn/Blei/Titan usw. entfernen | C1 |
| **U8 – Militär und TD** | Magazine, Granate, Mauer, Graues Paket, Geschütz-, Laser- und Flammenwerfer-Turm in der Arena, Einspeiser mit Flüssigkeit, Arena-Siegel als Forschungsschlüssel | – |
| **U9 – Leitfaden und Balancing** | Leitfaden aus Meilensteinen (§10), kleine Belohnungen, Progressionstest (jede Forschung mit dem bis dahin Freigeschalteten erfüllbar), Zeitmessung im Terminal, Handbuch-Seiten | A2 |
| **U10 – Pflanzen** | Gewächshaus, Pflanzenrezepte, Bio-Brennstoff | – |
| **U11 – Endgame (später)** | Uran in den Minen, Lila und Gelb, Montagemaschine 3/Module, Kernkraft | – |

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
| Reaktor, Brennstäbe, Uran-Pellet | Endgame U11 |
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
