# Craftorio – Tower Defense nach Bloons TD 6

Status: **entschieden** (alle Entscheidungen in §12 wie empfohlen). Dieses Dokument ersetzt die TD-Balancing-Regeln aus
`docs/KONZEPT.md` §6 und `docs/FACTORIO-UMBAU.md` §4.4/§6/§11.7. Arena, Pfadstab, Kartenthemen, Arena-Tor,
Einspeiser, Siegel und Forschung bleiben und werden angebunden.

Es ist als Arbeitsauftrag für eine KI-Coding-Session gedacht. Arbeitsweise, Befehle und Definition of Done wie in
`docs/BACKLOG-Testrunde-1.md` Abschnitt 0.

---

## 1. Warum Bloons TD 6

| Spiel | Steam-Bewertung | Einordnung |
|---|---|---|
| **Bloons TD 6** | 97 % positiv von ca. 387.000 Bewertungen („Äußerst positiv") | meistbewertetes und zugleich eines der bestbewerteten TD-Spiele; lange Motivation durch Aufrüstpfade und klare Gegnerstufen |
| Kingdom Rush | 97 % von ca. 16.500 | sehr gut, aber viel kleiner; Kaserne und Helden passen schlecht zu Minecraft |
| Plants vs. Zombies | 98 % von ca. 152.000 | Spurensystem (Lanes), passt nicht zu unserer freien Arena mit Pfad |

**Entscheidung (Vorschlag):** Die Mechaniken, Gegnerwerte, Rundenfolge, Wirtschaft und Aufrüstpfade von **Bloons TD 6
(BTD6)** übernehmen wir so genau wie möglich. Namen, Grafiken und Texte bleiben eigene (keine Ballons, keine Affen).
Wir übernehmen Regeln und Zahlen, keine Inhalte.

**Quellen:** bloonswiki.com (Gegner- und Turmseiten, Rundenliste) und bloons.fandom.com (Late Game/Freeplay). Alle
Werte vor der Umsetzung gegen **https://www.bloonswiki.com** prüfen; die Seiten sind ohne Anmeldung abrufbar (Muster
`https://www.bloonswiki.com/<Name>_(BTD6)`, Rundenliste `https://www.bloonswiki.com/List_of_rounds_in_BTD6`). Mit
*(prüfen)* markierte Werte sind aus Suchergebnissen oder dem Gedächtnis und besonders zu kontrollieren.

---

## 2. Grundidee der Übertragung

| BTD6 | Craftorio |
|---|---|
| Ein Spiel mit 100 Runden (Impoppable/CHIMPS), 80 (Hard), 60 (Medium), 40 (Easy) | **Arena-Kampagne mit 100 Runden = 50 Levels**, jedes Level = **2 Runden** auf einer neuen Karte |
| Bloons mit Schichten, die beim Platzen Kinder freigeben | **Schwarm-Gegner mit Schichten** (Krabbler-Stufen, Panzer, Golem, Brutmutter …) |
| Geld durch Platzen und Rundenbonus, pro Spiel | **Arena-Münzen (⛁)** pro Team, gelten über die ganze Kampagne (wie Geld in einem BTD6-Spiel) |
| Türme mit 3 Pfaden × 5 Stufen | Arena-Türme mit 3 Pfaden × 5 Stufen, Stufen 3–5 durch **Forschung** freigeschaltet |
| Affen-Wissen (Meta-Fortschritt) | Forschungszweig **„Arena-Wissen"** (Militär-Pakete) |
| XP-Freischaltung von Aufrüstungen | Forschung in der Fabrik |
| Feste Karten mit fester Strecke | **Feste Karten nach BTD6-Vorbild** mit vorgegebener Strecke, Kategorien Anfänger bis Experte (§13, Paket T8). Bis T8 umgesetzt ist: Pfadstab mit Längenfenster (§5.4) |

Die Fabrik bleibt mit der Arena verbunden:
- Türme werden in der Fabrik **gebaut** (Item, Forschung).
- Munition, Strom und Öl kommen über den **Arena-Einspeiser**.
- Späte Aufrüstungen brauchen **Bauteile** aus der Fabrik (§6.3).
- Credits lassen sich begrenzt in Münzen tauschen (§4.4).

Die Seal-Level passen genau auf die BTD6-Meilensteine:

| Level | Runde | BTD6-Meilenstein | Siegel |
|---|---|---|---|
| 5 | 10 | erste Masse (60+ blaue) | Bronze |
| 10 | 20 | erste schwarze Gegner | Silber |
| 20 | 40 | erster MOAB | Gold |
| 30 | 60 | erster BFB | Platin |
| 40 | 80 | erster ZOMG | Diamant |
| 50 | 100 | BAD (Finale) | Sternen |

---

## 3. Gegner

### 3.1 Einheiten

- **1 Block = 10 BTD6-Einheiten.** Das Arena-Feld (41×41) entspricht damit ungefähr einer BTD6-Karte (ca. 300 Einheiten
  breit). Die Konstante `TdUnits.UNITS_PER_BLOCK = 10` zentral führen und im Spieltest prüfen.
- Geschwindigkeit: rote Grundstufe 25 Einheiten/s = **2,5 Blöcke/s** = 0,125 Blöcke/Tick.
- Zeiten: 1 s = 20 Ticks.

### 3.2 Gegnertabelle

Schichten = eigene HP; bei 0 HP gibt der Gegner seine Kinder frei (Überschaden geht wie in BTD6 an die Kinder weiter).

| Craftorio | BTD6 | Eigene HP | RBE | Geschw. (× Rot) | Kinder | Immun gegen |
|---|---|---|---|---|---|---|
| Krabbler rot | Red | 1 | 1 | 1,0 | – | – |
| Krabbler blau | Blue | 1 | 2 | 1,4 | 1 rot | – |
| Krabbler grün | Green | 1 | 3 | 1,8 | 1 blau | – |
| Krabbler gelb | Yellow | 1 | 4 | 3,2 | 1 grün | – |
| Krabbler rosa | Pink | 1 | 5 | 3,5 | 1 gelb | – |
| Rußkrabbler | Black | 1 | 11 | 1,8 | 2 rosa | Explosion |
| Frostkrabbler | White | 1 | 11 | 2,0 | 2 rosa | Kälte |
| Glutkrabbler | Purple | 1 | 11 | 3,0 | 2 rosa | Energie, Feuer |
| Eisenbrecher | Lead | 1 (gepanzert 4) | 23 | 1,0 | 2 Ruß | Stich, Kälte, Energie |
| Zwielichtkrabbler | Zebra | 1 | 23 | 1,8 | 1 Ruß + 1 Frost | Explosion, Kälte |
| Schimmerkrabbler | Rainbow | 1 | 47 | 2,2 | 2 Zwielicht | – |
| Kristallgolem | Ceramic | 10 (gepanzert 20) | 104 | 2,5 | 2 Schimmer | – |
| Brutmutter | MOAB | 200 (400) | 616 | 1,0 | 4 Golems | Boss-Klasse (§3.4) |
| Behemoth | BFB | 700 (1.400) | 3.164 | 0,25 | 4 Brutmütter | Boss-Klasse |
| Koloss (neu) | ZOMG | 4.000 (8.000) | 16.656 | 0,18 | 4 Behemoths | Boss-Klasse |
| Schattenjäger (neu) | DDT | 400 (800) | 816 | 2,64 | 4 getarnte, nachwachsende Golems | Boss-Klasse, getarnt, Stich, Kälte, Energie, Explosion |
| Schwarmkönigin | BAD | 20.000 (40.000) | 55.760 | 0,18 | 2 Kolosse + 3 Schattenjäger | Boss-Klasse; immun gegen Verlangsamen, Betäuben, Rückstoß |

Alle Werte dieser Tabelle sind gegen bloonswiki.com geprüft (Geschwindigkeiten, HP, Kinder, RBE; Immunitäten aus der
Tabelle der Schadensarten dort). Die Immunitäten stammen aus den Bit-Feldern der Wiki-Seite „Damage type": Eisenbrecher
sind gegen Stich, Kälte und Energie immun (die Wiki-Seite nennt außerdem „Shatter", das hier in Stich aufgeht),
Glutkrabbler gegen Energie und Feuer, Frostkrabbler gegen Kälte, Rußkrabbler gegen Explosion, Zwielicht gegen
Explosion und Kälte, der Schattenjäger zusätzlich zu seinen Eigenschaften (getarnt) gegen Stich, Kälte, Energie und
Explosion.

Ab Runde 81 gelten die BTD6-Vereinfachungen (bestätigt: die Wiki-RBE der Runden 81–100 wird damit exakt getroffen):
- Golems werden zu **Superkristallgolems** (60 HP, gepanzert 120, geben nur 1 Schimmer frei).
- Schimmer, Zwielicht, Eisenbrecher und Ruß/Frost/Glut geben nur noch **ein** Kind frei (Ruß/Frost/Glut → 1 rosa,
  Eisenbrecher → 1 Ruß, Zwielicht → 1 Ruß, Schimmer → 1 Zwielicht); die RBE sinkt entsprechend (Golem 68 statt 104,
  Schimmer 8 statt 47).
- Die HP der Boss-Klasse wachsen (§3.6); der Faktor gilt für jede Schicht der Boss-Klasse.

**Bestehende Gegner:**
- Spitter (Fernangriff auf Türme) **entfällt** (§4.6).
- Die bisherigen Typen werden auf die Tabelle abgebildet: Krabbler → Krabbler-Stufen, Brecher → Eisenbrecher,
  Kristallgolem → Kristallgolem, Brutmutter, Behemoth, Schwarmkönigin wie oben.
- Aussehen: Krabbler in 5 Farben und Größen; Ruß, Frost und Glut als Farbvarianten; Eisenbrecher mit Metallpanzer usw.

### 3.3 Eigenschaften (Modifikatoren)

| Eigenschaft | BTD6 | Regel |
|---|---|---|
| **Getarnt** | Camo | Nur Türme mit Tarnungserkennung zielen auf ihn (§6.5). Flächenschaden trifft ihn trotzdem, wenn er ein anderes Ziel trifft. |
| **Nachwachsend** | Regrow | Stellt alle **3 s** die nächsthöhere verlorene Schicht wieder her, bis zur Ausgangsstufe (Wiki bestätigt). Auch Kinder wachsen zurück zum Elternteil und vermehren sich damit. Nachgewachsene Schichten bringen beim erneuten Platzen keine Münzen; Gepanzert kommt nicht zurück. Schattenjäger setzen getarnte, nachwachsende Golems frei, die nicht zum Schattenjäger zurückwachsen. Nicht für die Boss-Klasse. |
| **Gepanzert** | Fortified | Doppelte HP für Golems und Boss-Klasse, Eisenbrecher 1 → 4 (Wiki bestätigt). Kinder sind nur gepanzert, wenn sie selbst gepanzert sein können (gepanzerter Golem → normale Schimmer; gepanzerte Brutmutter → gepanzerte Golems). |

Die bisherige Themen-Mechanik „Tarnung im Dickicht" entfällt; Tarnung ist jetzt eine Gegnereigenschaft.

### 3.4 Boss-Klasse

- Wird von reinen Nicht-Boss-Effekten (Einfrieren, Leim ohne Upgrade) nicht beeinflusst.
- Viele Aufrüstungen haben **Bonus-Schaden gegen Boss-Klasse** (§6).

### 3.5 Schadensarten

| Schadensart | BTD6 | Wer (Beispiele) |
|---|---|---|
| Stich | Sharp | Armbrust, Geschütz (Normalmagazin), Flammenwerfer vor Stufe 3 |
| Explosion | Explosion | Mörser |
| Kälte | Cold | Frostturm |
| Energie | Energy/Plasma | Tesla, Laser |
| Feuer | Fire | Flammenwerfer ab „Glühende Stacheln", Tesla-Feuerpfad |
| Normal | Normal | trifft alles: panzerbrechende und Uran-Magazine, Kommandoposten-Stufe 3 (§6.4) |

Die bisherige Rüstungslogik (`physicalFactor`, `flatArmour`) wird durch **Immunitäten** ersetzt.

### 3.6 Spätes Spiel (Runde 81+)

Nach bloonswiki.com „Freeplay" (BTD6). Zwischenwerte sind linear zwischen Start- und Endwert der Stufe; im Code
(`TdSimulation.lateHpFactor/lateSpeedFactor`) als Tabelle hinterlegt.

| Runden | HP-Faktor der Boss-Klasse (Start → Ende) | Geschwindigkeit aller Gegner (Start → Ende) |
|---|---|---|
| 81–100 | 1,02 → 1,40 (+2 % pro Runde) | 1,02 → 1,40 (+2 %) |
| 101–125 | 1,45 → 2,75 (+5 %) | 1,60 → (bis 150) 2,58 (Sprung bei 101) |
| 126–150 | 2,90 → 6,50 (+15 %) | weiter bis 2,58 |
| 151–250 | 6,85 → 41,50 (+35 %) | 151–200: 3,00 → 3,98; 201–251: 4,50 → 5,50 |
| 251–300 | 42,50 → 91,50 (+100 %) | ab 252: 6,00, dann +2 % pro Runde |
| 301–400 | 93,0 → 241,5 (+150 %) | |
| 401–500 | 244 → 491,5 (+250 %) | |
| ab 501 | +500 % pro Runde | |

Außerdem nach Runde 140 zufällige Runden, und die Dauer von Verlangsamung/Betäubung sinkt ab Runde 150 (−10 % je
50 Runden, höchstens −50 %). Leaks kosten ab Runde 81 weniger Leben (Wiki-Tabelle „Freeplay lives cost", z. B. Golem 65
statt 104, Brutmutter 460 statt 616); das wird in T2 mit dem Leben-System umgesetzt.

---|---|---|
| 81–100 | +2 % pro Runde (Runde 100: ×1,4) | +2 % pro Runde (Runde 100: ×1,4) |
| 101–124 | Sprung auf ×1,6, dann +2 % pro Runde | +5 % pro Runde (Runde 124: ×2,6) |
| 125–150 | +2 % pro Runde | +15 % pro Runde (Runde 150: ×6,5) |
| 151–250 | +2 % pro Runde | +35 % pro Runde (Runde 250: ×41,5) |

---

## 4. Runden, Wirtschaft, Leben

### 4.1 Runden

- **Level L = Runden 2L−1 und 2L.** Zwischen den Runden eine kurze Pause (Standard 10 s) oder Start per Knopf.
  Die nächste Runde darf wie in BTD6 jederzeit früher gestartet werden (ohne Bonus).
- **Rundeninhalt 1:1 aus der BTD6-Standardliste** (`https://www.bloonswiki.com/Rounds_(BTD6)`, Stand Version 56.3).
  Alle Runden 1–140 liegen als Datendatei `data/craftorio/td_rounds/standard.json` (Gruppen: Gegner, Anzahl,
  Eigenschaften; dazu Dauer, RBE und Münzen je Runde als Prüfwerte aus dem Wiki). Die Gruppen der Wiki-Liste haben nur
  die Gesamtdauer der Runde; die Gegner treten deshalb **gleichmäßig über die Dauer verteilt** in die Strecke ein
  (`RoundDef.spawns()`).
- Ab Runde 101 **Endlosmodus** (Freeplay) mit den Regeln aus §3.6 und zufälligen Runden (Wiki: ab Runde 141 zufällig
  erzeugt; 163 und 263 mit eingeschränkter Auswahl; 200 nur Schwarmkönigin).
- **Prüfung (erledigt in T1):** Für jede der Runden 1–100 stimmt die aus den Gegnerdaten berechnete RBE exakt mit der
  Wiki-Spalte „Base RBE" überein (`RoundDefsTest`). Damit sind Gegnertabelle, Kinder, Gepanzert-Regel und die
  Spätspiel-Regeln ab Runde 81 gegen die Wiki-Zahlen bestätigt.
- IDs: Die Gruppen in den Runden-Tabellen unten nennen Stufen wie im Spiel (`rot` = `red_crawler`, `Ruß` =
  `soot_crawler`, `Golem` = `crystal_golem` …); die Datei `td_enemies/<id>.json` gehört je zu einem Gegner.

**Runden 1–40 (BTD6 Standard):**

| R | Gruppen | RBE |
|---|---|---|
| 1 | 20 rot | 20 |
| 2 | 35 rot | 35 |
| 3 | 10 rot, 5 blau, 15 rot | 35 |
| 4 | 25 rot, 18 blau, 10 rot | 71 |
| 5 | 12 blau, 5 rot, 15 blau | 59 |
| 6 | 4 grün, 15 rot, 15 blau | 57 |
| 7 | 10 blau, 5 grün, 20 rot, 10 blau | 75 |
| 8 | 20 blau, 2 grün, 10 rot, 12 grün | 92 |
| 9 | 30 grün | 90 |
| 10 | 60 blau, 20 blau, 22 blau | 204 |
| 11 | 3 gelb, 12 grün, 10 blau, 10 rot | 78 |
| 12 | 10 grün, 15 blau, 5 gelb | 80 |
| 13 | 50 blau, 23 grün | 169 |
| 14 | 18 rot, 5 blau, 5 grün, 4 gelb, 31 rot, 10 blau, 5 grün, 5 gelb | 145 |
| 15 | 20 rot, 15 blau, 12 grün, 10 gelb, 5 rosa | 151 |
| 16 | 20 grün, 20 grün, 8 gelb | 152 |
| 17 | 12 gelb nachwachsend | 48 |
| 18 | 60 grün, 20 grün | 240 |
| 19 | 10 grün, 5 gelb nachwachsend, 15 rosa, 4 gelb | 141 |
| 20 | 6 Ruß | 66 |
| 21 | 40 gelb, 10 rosa, 4 rosa | 230 |
| 22 | 16 Frost | 176 |
| 23 | 7 Ruß, 7 Frost | 154 |
| 24 | 1 grün getarnt, 20 blau | 43 |
| 25 | 25 gelb nachwachsend, 10 Glut | 210 |
| 26 | 23 rosa, 4 Zwielicht | 207 |
| 27 | 100 rot, 60 blau, 45 grün, 45 gelb | 535 |
| 28 | 6 Eisenbrecher | 138 |
| 29 | 50 gelb, 15 gelb nachwachsend | 260 |
| 30 | 9 Eisenbrecher | 207 |
| 31 | 8 Ruß, 8 Frost, 8 Zwielicht, 2 Zwielicht nachwachsend | 406 |
| 32 | 15 Ruß, 20 Frost, 10 Glut | 495 |
| 33 | 20 rot getarnt, 13 gelb getarnt | 72 |
| 34 | 160 gelb, 6 Zwielicht | 778 |
| 35 | 25 Frost, 5 Schimmer, 35 rosa, 30 Ruß | 1.015 |
| 36 | 40 rosa, 10 grün getarnt+nachwachsend, 40 rosa, 10 grün getarnt+nachwachsend, 60 rosa | 760 |
| 37 | 25 Ruß, 25 Frost, 15 Eisenbrecher, 10 Zwielicht, 7 Frost getarnt | 1.202 |
| 38 | 17 Frost, 42 rosa, 14 Eisenbrecher, 10 Zwielicht, 2 Golems | 1.157 |
| 39 | 10 Ruß, 10 Frost, 20 Zwielicht, 18 Schimmer, 2 Schimmer nachwachsend | 1.620 |
| 40 | 1 Brutmutter | 616 |

**Runden 41–100 (BTD6 Standard, aus dem Wiki übernommen):**

| R | Gruppen (wie im Wiki, in Reihenfolge) | RBE | Dauer |
|---|---|---|---|
| 41 | 60 Ruß, 60 Zwielicht  | 2.040 | 46,20 s |
| 42 | 6 Schimmer nachwachsend, 5 Schimmer getarnt  | 517 | 11,60 s |
| 43 | 10 Schimmer, 7 Golem  | 1.198 | 9,26 s |
| 44 | 10 Zwielicht, 10 Zwielicht, 10 Zwielicht, 10 Zwielicht, 10 Zwielicht  | 1.150 | 23,67 s |
| 45 | 25 Schimmer, 10 Glut getarnt, 180 rosa, 4 Eisenbrecher gepanzert  | 2.289 | 53,10 s |
| 46 | 6 Golem gepanzert  | 684 | 7,00 s |
| 47 | 12 Golem, 70 rosa getarnt  | 1.598 | 24,65 s |
| 48 | 40 rosa nachwachsend, 30 Glut getarnt+nachwachsend, 40 Schimmer, 3 Golem gepanzert  | 2.752 | 55,72 s |
| 49 | 343 grün, 10 Schimmer, 18 Golem, 20 Zwielicht, 10 Schimmer, 10 Schimmer nachwachsend  | 4.771 | 50,00 s |
| 50 | 1 Brutmutter, 8 Eisenbrecher gepanzert, 20 rot, 20 Golem, 1 Brutmutter  | 3.540 | 28,98 s |
| 51 | 15 Golem getarnt, 10 Schimmer nachwachsend  | 2.030 | 24,14 s |
| 52 | 25 Schimmer, 1 Brutmutter, 5 Golem, 1 Brutmutter, 5 Golem  | 3.447 | 20,56 s |
| 53 | 80 rosa getarnt, 1 Brutmutter, 1 Brutmutter, 1 Brutmutter  | 2.248 | 35,00 s |
| 54 | 35 Golem, 1 Brutmutter, 1 Brutmutter  | 4.872 | 19,41 s |
| 55 | 10 Golem, 10 Golem, 10 Golem, 15 Golem, 1 Brutmutter  | 5.296 | 29,78 s |
| 56 | 40 Schimmer getarnt, 1 Brutmutter  | 2.496 | 16,18 s |
| 57 | 2 Brutmutter, 40 Schimmer, 2 Brutmutter  | 4.344 | 26,23 s |
| 58 | 5 Brutmutter, 15 Golem, 10 Golem gepanzert  | 5.780 | 43,98 s |
| 59 | 20 Golem, 50 Eisenbrecher getarnt, 10 Golem nachwachsend  | 4.270 | 26,16 s |
| 60 | 1 Behemoth  | 3.164 | 1,00 s |
| 61 | 150 Zwielicht nachwachsend, 5 Brutmutter  | 6.530 | 20,00 s |
| 62 | 250 Glut, 5 Brutmutter, 2 Brutmutter gepanzert, 15 Schimmer getarnt+nachwachsend  | 8.247 | 48,29 s |
| 63 | 75 Eisenbrecher, 40 Golem, 40 Golem, 42 Golem  | 14.413 | 42,25 s |
| 64 | 6 Brutmutter, 3 Brutmutter gepanzert  | 6.264 | 9,53 s |
| 65 | 100 Zwielicht, 70 Schimmer, 50 Golem, 3 Brutmutter, 2 Behemoth  | 18.966 | 62,00 s |
| 66 | 2 Brutmutter, 2 Brutmutter, 4 Brutmutter, 3 Brutmutter gepanzert  | 7.496 | 22,75 s |
| 67 | 4 Brutmutter, 13 Golem getarnt+nachwachsend+gepanzert, 4 Brutmutter  | 6.410 | 26,44 s |
| 68 | 4 Brutmutter, 1 Behemoth  | 5.628 | 8,44 s |
| 69 | 40 Eisenbrecher gepanzert, 40 Ruß nachwachsend, 50 Golem  | 6.680 | 42,13 s |
| 70 | 200 Schimmer, 120 Frost getarnt+nachwachsend, 4 Brutmutter  | 13.184 | 41,14 s |
| 71 | 30 Golem, 10 Brutmutter  | 9.280 | 16,55 s |
| 72 | 38 Golem nachwachsend, 1 Behemoth, 1 Behemoth  | 10.280 | 21,70 s |
| 73 | 7 Brutmutter, 2 Behemoth, 1 Brutmutter  | 11.256 | 26,95 s |
| 74 | 50 Golem, 25 Golem getarnt+nachwachsend+gepanzert, 1 Behemoth, 60 Golem gepanzert  | 18.054 | 82,38 s |
| 75 | 1 Behemoth, 14 Eisenbrecher, 1 Brutmutter gepanzert, 3 Behemoth, 14 Eisenbrecher gepanzert, 2 Brutmutter gepanzert, 3 Behemoth  | 25.402 | 22,59 s |
| 76 | 60 Golem nachwachsend  | 6.240 | 1,78 s |
| 77 | 11 Brutmutter, 5 Behemoth  | 22.596 | 58,92 s |
| 78 | 150 Schimmer, 75 Golem, 1 Behemoth, 80 Glut, 72 Golem getarnt  | 26.382 | 90,00 s |
| 79 | 500 Schimmer nachwachsend, 4 Behemoth, 2 Behemoth gepanzert  | 45.804 | 60,00 s |
| 80 | 1 Koloss  | 16.656 | 2,00 s |
| 81 | 9 Behemoth, 8 Behemoth  | 44.506 | 26,47 s |
| 82 | 10 Behemoth, 5 Behemoth gepanzert  | 52.320 | 35,68 s |
| 83 | 40 Golem, 40 Golem nachwachsend, 40 Golem gepanzert, 30 Brutmutter  | 25.080 | 60,20 s |
| 84 | 50 Brutmutter, 10 Behemoth  | 51.480 | 25,00 s |
| 85 | 2 Koloss  | 30.704 | 10,00 s |
| 86 | 5 Behemoth gepanzert  | 27.040 | 20,85 s |
| 87 | 4 Koloss  | 63.008 | 10,00 s |
| 88 | 8 Behemoth, 18 Brutmutter, 2 Koloss  | 63.600 | 14,55 s |
| 89 | 20 Brutmutter gepanzert, 8 Behemoth gepanzert  | 64.384 | 20,74 s |
| 90 | 50 Eisenbrecher getarnt+nachwachsend+gepanzert, 3 Schattenjäger  | 2.756 | 11,90 s |
| 91 | 100 Golem gepanzert, 20 Behemoth  | 71.160 | 30,00 s |
| 92 | 50 Brutmutter gepanzert, 4 Koloss  | 117.408 | 35,00 s |
| 93 | 10 Behemoth gepanzert, 6 Schattenjäger  | 62.936 | 20,00 s |
| 94 | 25 Behemoth, 6 Koloss  | 178.112 | 15,00 s |
| 95 | 500 Glut getarnt+nachwachsend, 250 Eisenbrecher getarnt+nachwachsend+gepanzert, 50 Brutmutter gepanzert, 30 Schattenjäger  | 80.860 | 50,81 s |
| 96 | 10 Behemoth, 20 Brutmutter gepanzert, 10 Behemoth, 20 Brutmutter gepanzert, 10 Behemoth, 6 Koloss  | 238.952 | 32,12 s |
| 97 | 2 Koloss gepanzert  | 69.984 | 5,00 s |
| 98 | 30 Behemoth gepanzert, 8 Koloss  | 327.456 | 30,00 s |
| 99 | 60 Brutmutter, 9 Schattenjäger gepanzert  | 47.424 | 12,00 s |
| 100 | 1 Schwarmkönigin  | 67.200 | 0,10 s |

Erstes Auftreten (Kontrolle): Nachwachsend R17, Ruß R20, Frost R22, Tarnung R24, Zwielicht R26, Eisenbrecher R28,
Schimmer R35, Golem R38, Brutmutter R40, Gepanzert R45, Behemoth R60, Koloss R80, Schattenjäger R90, Schwarmkönigin R100.
Referenz-RBE: R45 2.289, R50 3.540, R60 3.164, R80 16.656, R100 67.200.

### 4.2 Arena-Münzen (⛁)

| Regel | Wert (BTD6) |
|---|---|
| Startguthaben der Kampagne | 650 ⛁ (Wiki „Cash" bestätigt) |
| Pro zerstörter Schicht | 1 ⛁ × Einkommensfaktor (bestätigt: jede Schicht zählt 1, auch die Schicht eines Golems oder einer Brutmutter; Golem mit Kindern 95, Brutmutter 381, Behemoth 1.525) |
| Rundenbonus | **100 + Rundennummer** (bestätigt: Runde 1 bringt 20 + 101 = 121) |
| Einkommensfaktor | R1–50: 100 %; R51–60: 50 %; R61–85: 20 %; R86–100: 10 %; R101–120: 5 %; R121–140: 4 %; R141+: 2 % (Wiki „Freeplay" bestätigt) |
| Verkauf eines Turms | 70 % des bezahlten Preises (Turm + Aufrüstungen) |
| Münzen | gehören dem Team, gelten über alle Level; Anzeige im HUD in der Arena |

Referenz zum Prüfen: Die Wiki-Rundenliste hat eine Spalte „Cash" pro Runde. Ein Unit-Test vergleicht die berechneten
Rundeneinnahmen mit dieser Spalte für Runde 1–100 (Toleranz ±2 %).

### 4.3 Schwierigkeitsgrade

Pro Team im Terminal wählbar, bevor Level 1 startet (danach nur nach unten änderbar):

| Stufe | Leben pro Level | Preisfaktor | Gegnertempo | Belohnung (Credits) |
|---|---|---|---|---|
| Leicht | 200 | ×0,85 | ×1,0 | ×0,75 |
| Mittel (Standard) | 150 | ×1,0 | ×1,1 | ×1,0 |
| Schwer | 100 | ×1,08 | ×1,25 | ×1,25 |
| Unbesiegbar | 1 | ×1,2 | ×1,25 | ×1,5 |

Leben, Preisfaktoren und Gegnertempo sind gegen die Wiki-Seiten Easy, Medium, Hard und Impoppable geprüft (Leicht:
Gegner im Grundtempo; Mittel: 10 % schneller als Leicht; Schwer: 25 % schneller als Leicht, rund 13,6 % schneller als
Mittel; Unbesiegbar: Schwer mit 1 Leben und Preis ×1,2 gegenüber Mittel). Nicht übernommen: Die Brutmutter von Runde 40
hat in BTD6 nur auf Leicht 133 statt 200 HP; Leicht endet dort in BTD6 auch nach Runde 40, hier laufen alle Stufen bis
Runde 100. Die Belohnungsfaktoren sind Craftorio-Werte (BTD6 hat keine Credits).

**Leben** gelten pro Level (siehe Entscheidung TD-E3). Ein durchgelassener Gegner kostet so viele Leben wie seine
**RBE inklusive Kinder**; ab Runde 81 gilt die Tabelle „Freeplay lives cost" des Wiki (Golem 65, gepanzert 75; Brutmutter
460/700; Behemoth 2.540/4.200; Koloss 14.160/24.800; Schattenjäger 660/1.100; Schwarmkönigin 50.300/92.900), im Code
`EnemyDefs.leak`. Ein Leak der Boss-Klasse beendet auf Schwer und Unbesiegbar das Level praktisch sofort, wie in BTD6.
Ein angeschlagener Gegner kostet nur, was noch von ihm übrig ist.

### 4.4 Credits und Münzen

- **Credits** (Fabrik, Handelsposten) und **Münzen** (Arena) sind getrennt.
- **Kriegskasse:** Im Terminal lassen sich Credits in Münzen tauschen, **1 : 1, höchstens 100 × Level Münzen pro
  Level** (Level 20: 2.000 ⛁). Die Fabrik hilft also wie eine kleine Farm, trivialisiert die Arena aber nicht.
- **Warenlieferung an das Nachschublager** (entschieden): Jedes Nachschublager holt zu Rundenbeginn einen
  **Warenkorb** aus der Arena-Reserve (Einspeiser, per Band/Greifarm aus der Fabrik beliefert). Mit vollständigem Korb
  bringt es in dieser Runde **+50 % Münzen**; ohne Korb läuft es normal.

  | Stufe von Pfad 1 | Warenkorb pro Runde und Lager |
  |---|---|
  | 0–2 | 10 Schaltkreise |
  | 3 | 10 fortschrittliche Schaltkreise |
  | 4–5 | 5 Prozessoren |

  Mengen und Bonus sind Startwerte für den Simulator. Das HUD zeigt pro Runde, wie viele Lager beliefert wurden.
- **Obergrenze der Kriegskasse** ist ebenfalls ein Startwert: Der Simulator soll prüfen, dass Kasse + Lieferbonus die
  Referenz-Aufstellungen nicht mehr als ca. 30 % schneller bezahlbar machen als reines BTD6-Einkommen.
- Pro geschafftem Level gibt es weiterhin **Credits** (bisherige Formel × Schwierigkeitsfaktor) und die Siegel.
- Sterne bleiben:
  - 3 Sterne: kein Leben verloren
  - 2 Sterne: mindestens die Hälfte der Leben übrig
  - 1 Stern: sonst
  - Sternbonus wie bisher in Credits

### 4.5 Niederlage und Wiederholung

- Beim Start eines Levels wird ein **Snapshot** gespeichert: Münzen, Depot-Inhalt (Türme mit Aufrüstungen).
- Verliert das Team das Level, wird der Snapshot wiederhergestellt; das Level kann erneut versucht werden.
  Das ersetzt die „Fortsetzen"-Funktion von BTD6.

### 4.6 Was entfällt

| Bisher | Warum |
|---|---|
| Gegner greifen Türme an, Turm-HP, Ruinen, Reparatur | Gibt es in BTD6 nicht; verzerrt das Balancing stark |
| Spitter (Fernangriff) | Nur für Turmangriffe gedacht |
| Skalierung pro zusätzlichem Spieler (+35 % HP, +2 Krabbler) | BTD6-Koop skaliert die Gegner nicht; das Team teilt sich die Münzen |
| Bonus für früh gerufene Wellen | BTD6 hat keinen; früher Start bleibt erlaubt |
| Themen-Effekte Tarnung im Dickicht, Bremsen in Furt/Geröll | Durch Gegnereigenschaften ersetzt. Das Plateau (+Reichweite) bleibt als einziger Gelände-Effekt (+10 % Reichweite). |
| Mutatoren Nebel/Eile/Gehärtet | Durch BTD6-Herausforderungen ersetzt (§7) |

---

## 5. Arena und Pfad

### 5.1 Karten

Die Themen (Wald, Berge, Feuer, Wasser, Kolosseum) bleiben als Optik und Hindernisse. Alles, was eine Kachel
blockiert, blockiert weiterhin Pfad und Türme.

### 5.2 Türme platzieren

- Ein Turm braucht sein **Turm-Item** (in der Fabrik gebaut) **und** den **Münzpreis** aus §6.
- Beim Kartenwechsel wandern Türme mit allen Aufrüstungen ins Depot; das erneute Aufstellen kostet keine Münzen.
- Im Depot und als Item stapeln nur Türme mit identischem Aufrüststand.

### 5.3 Zielmodi

Wie BTD6: **Erster, Letzter, Nächster, Stärkster** (ersetzt Schwächster). Tarnung hat keine Sonderpriorität.

### 5.4 Pfadlänge (wird durch T8 ersetzt)

> Gilt nur bis Paket T8. Danach gibt es keinen Pfadstab mehr; die Länge kommt aus der Karte (§13).


Die BTD6-Balance setzt eine feste Streckenlänge voraus. Der Pfadstab erzwingt deshalb ein **Längenfenster**:
- Mindestens **100**, höchstens **160 Blöcke**. Prüfung gegen das Wiki: Die Seiten der Karten geben die Streckenlänge
  in „Red Bloon Seconds" an (Monkey Meadow: Leicht 36,5 s, Mittel 33,2 s, Schwer 29,2 s – das bestätigt nebenbei die
  Gegnertempi ×1,0 / ×1,1 / ×1,25). Bei 2,5 Blöcken/s für Rot (Leicht) entsprechen 100–160 Blöcke 40–64 s auf Leicht und
  36–58 s auf Mittel, also dem Bereich der BTD6-Karten.
- Das Level startet nur mit einem Pfad in diesem Fenster (`PathTracer.MIN_LENGTH`/`MAX_LENGTH` = 100/160); die Meldung
  nennt die Grenzen, „Weg OK" zeigt die Länge.
- Jede Karte enthält eine **garantierte Schlangenroute** von 104–124 Blöcken (`ArenaLayout.route()`): drei Bahnen
  (Ost, West, Ost) mit mindestens drei Reihen Abstand; der Generator räumt sie frei. Der Pfadstab zeigt sie mit
  Partikeln (Schleichen + Klick in die Luft). Ein Unit-Test prüft für 60 Startwerte × 50 Level und alle Themen, dass die
  Route im Fenster liegt, keine Kachel blockiert ist und der `PathTracer` sie akzeptiert.

---

## 6. Türme

### 6.1 Aufrüstsystem

- **3 Pfade × 5 Stufen.** Ein Pfad bis Stufe 5, ein zweiter bis Stufe 2, der dritte gesperrt
  (Beispiele: 5-2-0, 2-0-5, 0-2-5 erlaubt; 3-3-0, 2-2-2, 0-3-2 nicht). Das ergibt wie im Wiki 64 Zustände je Turm
  (`TowerRules.canUpgrade`, Unit-Test zählt sie).
- Kosten in Münzen = BTD6-Mittel-Preis × Schwierigkeitsfaktor. **Geprüft (T3):** Grundpreis und alle Aufrüstpreise
  stehen in `data/craftorio/td_towers/*.json`; ein Unit-Test vergleicht für alle 10 Türme und alle 64 Zustände den
  Gesamtpreis und den Verkaufswert mit der Wiki-Tabelle „Costs and sell values" (`wiki_tower_costs.json`).
- **Platzieren:** Ein frischer Turm aus der Fabrik kostet beim Bauen seinen Grundpreis in Münzen; ein Turm mit
  Aufrüstungen aus dem Depot ist schon bezahlt (das Turm-Item merkt sich Aufrüstungen und den bezahlten Betrag).
  Platzieren bleibt während einer Runde gesperrt, Aufrüsten und Verkaufen gehen jederzeit.
- **Freischaltung durch Forschung** (ersetzt BTD6-XP):

  | Stufe | Forschung | Kosten *(Vorschlag)* | Voraussetzung |
  |---|---|---|---|
  | 1–2 | – | – | – |
  | 3 | „Turmtechnik I" (`tower_tech_1`) | 75 × R+G | Geschütztürme, Logistik-Paket |
  | 4 | „Turmtechnik II" (`tower_tech_2`) | 150 × R+G+M | Turmtechnik I, Militär-Paket |
  | 5 | „Turmtechnik III" (`tower_tech_3`) | 300 × R+G+M+B | Turmtechnik II, Chemie-Paket |

  Ab Stufe 3 kommen **Bauteile** aus der Fabrik dazu (Tabelle in §6.3), die der Spieler beim Kauf im Inventar hat.
- **Fähigkeiten** (z. B. Schneesturm, Versorgungsabwurf, Boss-Attentäter): Knopf im Turm-GUI und eine Taste, wenn der
  Spieler in der Arena ist; Abklingzeit wie BTD6.
- **Verkaufen:** 70 % des bezahlten Betrags, gerundet (§4.2); Nachschublager mit „Bananenbergung" (Pfad 3, Stufe 2)
  zahlen 80 % zurück (so steht es in der Wiki-Kostentabelle). Der Turm kommt als frisches Turm-Item zurück.

### 6.2 Turmliste

10 Türme, die alle Rollen von BTD6 abdecken:
- alle Schadensarten
- Tarnung und Eisen
- Boss-Schaden
- Verlangsamen
- Unterstützung und Einkommen

| Craftorio | BTD6-Vorbild | Basispreis ⛁ | Schaden | Durchschlag | Abklingzeit | Reichweite | Art | Forschung | Versorgung |
|---|---|---|---|---|---|---|---|---|---|
| Armbrustturm | Dart Monkey | 200 | 1 | 2 | 0,95 s | 3,2 Blöcke | Stich | Start | Bolzen |
| Geschützturm | Sniper Monkey | 350 | 2 | 1 | 1,59 s | unendlich | Stich/Normal (Magazin) | Geschütztürme | Magazine |
| Mörserturm (neu) | Bomb Shooter | 375 | 1 | 22 (Radius 1,2) | 1,5 s | 4,0 | Explosion | Militär 2 | Granaten |
| Flammenwerfer | Tack Shooter | 260 | 1 | 1 × 8 Richtungen | 1,12 s | 2,3 | Stich → Feuer ab Pfad 1 Stufe 3 | Flammenwerfer | Öl |
| Frostturm (neu) | Ice Monkey | 400 | 1 | 40 | 2,4 s | 2,0 (Aura) | Kälte, friert 1,5 s | „Kältetechnik" (R+G) | Strom |
| Leimwerfer (neu) | Glue Gunner | 225 | 0 (50 % langsamer, 11 s) | 1 | 1,0 s | 4,6 | – | Kunststoffe | Kunststoff |
| Tesla-Turm | Wizard Monkey | 250 | 1 | 3 | 1,1 s | 4,0 | Energie | Energie-Türme | Strom |
| Laser-Turm | Super Monkey | 2.500 | 1 | 1 | 0,045 s | 5,0 | Stich → Energie ab Pfad 1 Stufe 1 | Laser-Türme | Strom |
| Kommandoposten (neu) | Monkey Village | 1.200 | – | – | – | 4,0 (+10 % Reichweite für Türme im Radius) | Unterstützung | Militär-Paket | – |
| Nachschublager (neu) | Banana Farm | 1.250 | – | – | – | – | 80 ⛁ pro Runde | Handelsposten/Start | – |

Die Reichweiten sind aus BTD6-Einheiten (÷10) umgerechnet. Bolzen, Magazine, Granaten, Öl, Kunststoff und Strom werden
pro **Schuss** verbraucht (Richtwert: 1 Munitions-Item pro 10 Schuss, 1 Kunststoff pro 20 Leimschüssen; Strom
10 FE × Schaden pro Treffer). Ist die Versorgung leer, schießt der Turm nicht; das HUD warnt.

### 6.3 Aufrüstpfade

Kosten in ⛁ (Mittel). Eigene Namen; die BTD6-Namen stehen zur Prüfung dabei. **Bauteile** aus der Fabrik kommen
ab Stufe 3 dazu, als Verbindung zur Fabrik:

| Stufe | Bauteile |
|---|---|
| 3 | 5 Schaltkreise |
| 4 | 10 fortschrittliche Schaltkreise |
| 5 | 5 Prozessoren + 2 Elektromotoren |

**Hinweis:** Die Namen, Preise und Wirkungen aller 150 Aufrüstungen stehen in `data/craftorio/td_towers/<turm>.json`
(Quelle: die Turmseiten und die „Stats:"-Seiten von bloonswiki.com, Version 55–56). Die folgende Tabelle beschreibt den
**Kommandoposten (Monkey Village)** – im ersten Entwurf stand sie fälschlich unter „Armbrustturm". Die Armbrust (Dart
Monkey) hat die Pfade Durchschlag / Tempo / Reichweite (Scharfe Schüsse … Ultra-Juggernaut, Schnelle Schüsse …
Plasma-Affenclub, Weitschuss … Armbrustmeister).

**Kommandoposten (Monkey Village)**

| Pfad | Wirkung (Stufen laut Wiki übernehmen) |
|---|---|
| 1 Primär | Stufen 1–5: Reichweite, Durchschlag und Geschosstempo für primäre Türme (Armbrust, Mörser, Flammenwerfer, Leim, Frost) |
| 2 Aufklärung | Stufe 2 **Radar**: alle Türme im Radius erkennen Tarnung; Stufe 3 **Geheimdienst**: Türme im Radius machen Normal-Schaden (treffen alles) |
| 3 Wirtschaft | Stufe 1 **Handelshaus**: 10 % Rabatt auf Türme und Aufrüstungen bis Stufe 3; Stufe 3 **Marktstadt**: +50 % Münzen pro Schicht |

**Nachschublager (Banana Farm)**: 80 ⛁ pro Runde.

| Pfad | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| 1 Produktion | 500: 120 ⛁/Runde | 600: 160 | Plantage 3.000: 320 | Forschungsanlage 19.000: 1.500 | Zentrale 115.000: 6.000 |
| 2 Bank | 300 | 800: +25 % | Bank 3.650: 15 % Zinsen | Kredit 7.200: Fähigkeit 9.000 ⛁ | Wirtschaftswunder 100.000 |
| 3 Automatik | Selbstabholung 250: 50 % | Bergung 400: 85 % | Markt 2.700: 320 automatisch | Großmarkt 15.000: 1.120 | Börse 70.000: 4.000 Rundenbonus |

In BTD6 muss man die Früchte einsammeln. Hier landen sie automatisch mit dem Wert aus Pfad 3 (ohne Pfad 3: 100 %,
weil Einsammeln in der Arena mühsam wäre) *(Entscheidung TD-E4)*.

### 6.4 Wer trifft was (Kontrolle für den Simulator)

| Gegner | Stich | Explosion | Kälte | Energie | Feuer | Normal |
|---|---|---|---|---|---|---|
| Ruß | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ |
| Frost | ✓ | ✓ | ✗ | ✓ | ✓ | ✓ |
| Glut | ✓ | ✓ | ✓ | ✗ | ✗ | ✓ |
| Eisenbrecher | ✗ | ✓ | ✗ | ✗ | ✓ | ✓ |
| Zwielicht | ✓ | ✗ | ✗ | ✓ | ✓ | ✓ |
| Schattenjäger | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |

### 6.5 Tarnungserkennung

Nur mit diesen Aufrüstungen (sonst gar nicht):
- Armbrust 3-2
- Geschütz 2-1
- Frost 1-2
- Tesla 3-2
- Laser 3-2
- Kommandoposten-Radar (für alle Türme im Radius)

Flächenschaden (Mörser, Frost-Aura, Flammenring) trifft getarnte Gegner mit, wenn er ausgelöst wird.

---

## 7. Modi und Herausforderungen

Ersetzen die Mutatoren; pro Level vor dem Start wählbar, geben mehr Credits und zählen für einen „Meister-Stern":

| Modus | BTD6-Vorbild | Wirkung | Belohnung |
|---|---|---|---|
| Doppelte Boss-HP | Double HP MOABs | Boss-Klasse ×2 HP | +50 % |
| Halbe Münzen | Half Cash | Münzen pro Schicht und Rundenbonus ×0,5 | +50 % |
| Alternative Runden | Alternate Bloons Rounds | eigene Rundenliste laut Wiki | +30 % |
| Apokalypse | Apopalypse | Runden laufen ohne Pause durch | +40 % |
| CHIMPS | CHIMPS | 1 Leben, keine Münzen durch Verkauf, keine Kriegskasse, kein Nachschublager | +100 % |

---

## 8. Arena-Wissen (Forschung)

BTD6 hat „Affen-Wissen" (dauerhafte Boni). Bei uns ist es ein Forschungszweig mit **Militär-Paketen**
(R+G+M, Kosten 50–300 Einheiten *(Vorschlag)*):

| Forschung | Wirkung | BTD6-Vorbild |
|---|---|---|
| Kriegsvorrat | +200 ⛁ Startguthaben der Kampagne | Starting cash |
| Bessere Ausrüstung | Verkauf 75 % statt 70 % | Better Sell Deals |
| Sparsame Bauweise | −5 % auf Stufe-1-und-2-Aufrüstungen | Cheaper upgrades |
| Starke Bolzen | Armbrust +1 Durchschlag | Sharp Shooting-Zweig |
| Feldlazarett | +25 Leben pro Level | Bonus lives |
| Zinseszins | Nachschublager +10 % | Farm knowledge |
| Wachsamkeit | Kommandoposten-Radius +10 % | Village knowledge |

Die Liste ist ein Startpunkt; weitere BTD6-Wissenspunkte nur, wenn sie ohne neue Mechanik umsetzbar sind.

---

## 9. Technik

### 9.1 Gegner-Engine ohne Entities (Pflicht)

BTD6-Runden haben Hunderte Gegner gleichzeitig (z. B. Runde 34: 160 gelbe plus Kinder), und jedes Platzen erzeugt neue.
Minecraft-Entities pro Gegner sind dafür zu teuer.

- **Gegner als Datenobjekte** in `LevelRun`: Typ, Eigenschaften, HP, Fortschritt auf dem Pfad (Abstand in Blöcken),
  Verlangsamung, Einfrieren, Leim, Tarnung, Nachwachs-Timer.
- Reine Logik `TdSimulation` ohne Minecraft-Klassen (Bewegung, Treffer, Kinder, Überschaden, Immunitäten, Leaks),
  voll unit-testbar.
- **Netzwerk:** Pro Tick ein kompaktes Paket an Spieler in der Arena (ID, Typ, Fortschritt; Delta-Kodierung).
- **Rendering:** Eigener Client-Renderer zeichnet die Gegner entlang der bekannten Pfadpunkte (instanziert, ohne Entity).
- Treffer und Projektile ebenfalls als Daten; Partikel nur clientseitig.
- **Akzeptanz:** 1.000 gleichzeitige Gegner bei 20 TPS auf einem Testserver (Messung im GameTest mit Zeitbudget pro
  Tick < 10 ms).

### 9.2 Balancing-Simulator (Pflicht)

- `TdSimulation` spielt Runden ohne Minecraft durch (Headless).
- **Referenz-Aufstellungen** pro Rundenabschnitt, z. B. R1–20 zwei Armbrüste 2-0-3 und ein Mörser 0-2-3, bis R100 ein
  typisches BTD6-Late-Game-Set.
- **Unit-Tests** prüfen:
  - Rundeneinnahmen gegen die Wiki-Spalte „Cash" (±2 %)
  - dass die Referenz-Aufstellung jede Runde ohne Leak schafft
  - dass ein bewusst zu schwaches Set in der erwarteten Runde scheitert (z. B. ohne Tarnung in R24/R33, ohne
    Eisen-Konter in R28)

  So bleibt das Balancing nach jeder Änderung nachprüfbar.

### 9.3 Datengetrieben

| Daten | Datei |
|---|---|
| Runden | `data/craftorio/td_rounds/<modus>.json` |
| Gegner | `data/craftorio/td_enemies/*.json` (HP, RBE, Tempo, Kinder, Immunitäten) |
| Türme | `data/craftorio/td_towers/*.json` (Basiswerte, Pfade, Stufen, Kosten, Effekte) |

Werte sind damit ohne Code anpassbar (Datapack, `/reload`).

---

## 10. Migration

| Bisher | Neu |
|---|---|
| Turm-Upgrade-Stufen 1–5 (`TowerStats`) | Pfadsystem; alte Stufe n → Pfad 1 Stufe n−1, Rest verfällt, Münzen-Erstattung 70 % des alten Credit-Werts |
| Turm-HP, Ruinen, Reparatur | entfallen; Ruinen im Depot werden zu intakten Türmen |
| Level-Fortschritt der Teams | bleibt; Runde = 2 × Level − 1 beim nächsten Start; Münzen-Startguthaben nach Level (Richtwert: Summe der BTD6-Einnahmen bis zu dieser Runde × 0,5) |
| Mutatoren | Herausforderungen (§7) |
| Spitter-Entity | entfällt |
| TD-Level-Schritte im Leitfaden | bleiben (Level 1, 5, 10, 20, 30, 40, 50) |

---

## 11. Pakete (Reihenfolge, je ein Commit)

| Paket | Inhalt | Akzeptanz |
|---|---|---|
| **T1 – Gegner-Engine** ✅ | `TdSimulation` (Bewegung, Schichten, Kinder, Überschaden, Eigenschaften, Immunitäten, Leaks, spätes Spiel), Gegnerdaten §3 als JSON, Netzwerkpaket, Client-Renderer, Entfernen der Gegner-Entities | Unit-Tests für jeden Gegner (RBE aus Kindern = Tabelle), Nachwachsen alle 3 s, Tarnung; Leistungstest 1.000 Gegner |
| **T2 – Runden, Münzen, Leben** ✅ | Rundendaten 1–100, Level = 2 Runden, Münzen und Einkommensfaktor, Rundenbonus, Schwierigkeitsgrade, Leben pro Level, Snapshot/Wiederholung, Kriegskasse, Siegel-Zuordnung, Entfernen von Turm-HP/Ruinen/Reparatur und Spieler-Skalierung, Pfadlängen-Fenster | Unit-Test Rundeneinnahmen gegen Wiki (±2 %); GameTest Level 1 komplett; Pfad < 100 Blöcke wird abgelehnt |
| **T3 – Turmsystem** ✅ | Pfade × Stufen mit Kreuzpfad-Regel, Münzpreis beim Platzieren, Verkauf 70 %, Zielmodi, Tarnungserkennung, Fähigkeiten, Forschung Turmtechnik I–III, Bauteile ab Stufe 3, neues Turm-GUI mit drei Pfaden | Unit-Tests Kreuzpfad-Regel und Verkaufswert; GameTest: Stufe 3 ohne Forschung gesperrt |
| **T4 – Türme Teil 1** ✅ | Armbrust, Geschütz (mit Magazinarten), Mörser (neu), Flammenwerfer, Nachschublager (neu, mit Warenkorb §4.4) mit allen 15 Aufrüstungen | Simulator: Referenz-Set R1–40 ohne Leak |
| **T5 – Türme Teil 2** ✅ | Frost, Leim, Tesla, Laser, Kommandoposten mit allen Aufrüstungen | Simulator: Referenz-Set R41–100 ohne Leak; Immunitätstabelle §6.4 als Test |
| **T6 – Modi und Endlos** ✅ | Herausforderungen §7, Endlosmodus ab Runde 101 mit §3.6, Arena-Wissen §8, Rangliste (höchste Runde pro Team) | GameTest je Modus; Freeplay-Skalierung als Unit-Test |
| **T7 – Leitfaden, Handbuch, Feinschliff** ✅ (bis auf Runde 90–100 des Referenz-Satzes) | Handbuch-Seiten: Gegner (mit Immunitäten), Türme (Pfade), Wirtschaft; Leitfaden-Hinweise; README; Migration §10 | Alle Tests grün; Kampagne im Simulator mit Referenz-Sets bis Runde 100 schaffbar |

### T4 – Umsetzungsnotizen

- **Zuordnung** (Stats-Seiten der Wiki, Version 55.1, 10 Einheiten = 1 Block): Armbrust = Dart Monkey, Geschütz = Sniper Monkey,
  Flammenwerfer = Tack Shooter, **Mörser = Bomb Shooter (neu, Munition: Granaten)**, **Nachschublager = Banana Farm (neu)**.
  Alle 15 Aufrüstungen je Turm stehen als Effekte in `td_towers/*.json` (Abkühlzeit-Faktoren relativ zur Vorstufe, damit Pfade
  multiplikativ zusammenwirken; Reichweite in Blöcken).
- **Engine-Erweiterungen:** Fähigkeiten mit Buff für den Turm selbst und bis zu *n* Türme der gleichen Art im Umkreis (Fan Club: 10, Plasma:
  19), Auren (`aura`: Buff für Türme im Umkreis oder überall, z. B. Elite-Scharfschütze ×0,75 Abklingzeit für alle Geschütze), passive
  Fähigkeiten (Elite-Verteidiger startet bei Lebensverlust), Fähigkeits-Angriffe ohne Ziel (Klingensturm), unbegrenzte Reichweite
  (Meteor, Attentäter), `soak` (Schaden geht durch Behemoth-Schichten), `follow_range` (Ring des Feuers wächst mit der Reichweite),
  Turm-Register je Lauf (`LevelRun.towers()`), Münzen aus Fähigkeiten (`addCoins`).
- **Nachschublager:** Einkommen pro Rundenende aus Pfad 1 (80/120/160/320/1.500/6.000) × Wertfaktor Pfad 2 (1,25) + Markt Pfad 3
  (320/1.120) + Börse (4.000); Werte gegen die Einkommenstabelle der Wiki geprüft. **Bank** (Pfad 2, ab Stufe 3): 400 pro Runde,
  15 % Zinsen, Obergrenze 7.000/10.000/20.000, Fähigkeit „Bank leeren“; **Kredit** (Stufe 4: 9.000, Stufe 5: 25.000) wird zuerst vom Einkommen
  zurückgezahlt. **Warenkorb:** 10 Schaltkreise (Stufe 3 von Pfad 1: fortschrittliche, ab Stufe 4: 5 Prozessoren) aus der Arena-Reserve am Rundenende
  geben +50 % (Einspeiser nimmt Schaltkreise, Prozessoren und Granaten an).
- **Vereinfachungen** (gleichwertiger Ersatz, TD-E5): Elite-Verteidiger ohne Geschwindigkeitsbonus nach Streckenfortschritt;
  „Elite“-Zielmodus des Scharfschützen entfällt; Nachschubabwurf zahlt sofort aus (kein Einsammeln); Kreuzpfad-Bonusse einzelner
  Stufen (z. B. mehr Splitter beim Mörser) entfallen.
- **Akzeptanz:** `ReferenceSetsTest` spielt Runde 1–40 mit einem festen Kaufplan (Simulator `BalanceSimulator`: echte Arena-Routen aus
  `ArenaLayout`, Türme auf den Stellen mit der größten Pfadabdeckung, Runden nacheinander, Plan wird gekauft, sobald die Münzen
  reichen) auf zehn Layouts; der Plan wurde mit `PlanSearchScratchTest` (Hill-Climbing über Kaufreihenfolgen auf den Layouts 1–10) gefunden und hält auch auf sechs weiteren Layouts mit höchstens einem Leak. Das **Ziel
  „ohne Leak“** gilt für diese Aufstellung; ein Spieler ohne Plan verliert in den Runden 4, 9 und 12 vereinzelt Leben.

### T5 – Umsetzungsnotizen

- **Zuordnung:** Frostturm = Ice Monkey, Leimwerfer = Glue Gunner, Tesla-Turm = Wizard Monkey, Laser-Turm = Super Monkey,
  Kommandoposten = Monkey Village. Alle 75 Aufrüstungen als Effekte in den Turmdaten; die neuen Blöcke Frostturm, Leimwerfer
  (nimmt Kunststoffbarren) und Kommandoposten kommen mit Modell, Blaupause, Beute, Forschung (Frost: neue Forschung „Kältetechnik“,
  Leim: Kunststoffe, Kommandoposten: Militär-Paket) und Namen.
- **Neue Engine-Bausteine:** Schadensarten *Gletscher* (Kälte, trifft Eisen) und *Plasma* (Energie, trifft Eisen); Leim mit Stufen (stärkerer Leim ersetzt
  schwächeren, ein Leimwerfer ignoriert schon gleich stark verleimte Gegner), Leim auf Behemoth-Klasse, Versprödung, reine Tarnungsziele (Flimmern),
  Angriffe ohne Verbrauch (`free`), Kommandoposten-Auren (Reichweite, Tempo, Tarnung, Normal-Schaden, Training für „primäre“ Türme), Preisnachlass
  und Einkommen des Kommandopostens (wirken auch zwischen den Leveln, weil gebaut wird, wenn keine Runde läuft).
- **Immunitätstabelle §6.4** ist jetzt ein Test (`T5TowersTest.whoGetsHitByWhatFollowsTheTableOfTheConcept`).
- **Vereinfachungen** (gleichwertiger Ersatz, TD-E5): Nekromant (Untote Armee) als zusätzliche Flächenangriffe, Phönix als freier Angriff,
  Sonnentempel ohne Opfermechanik, „Dunkler Ritter“-Fähigkeiten und Entfernen von Immunitäten (Frost/Leim) entfallen, Gefrieren der
  Behemoth-Klasse nur über Verlangsamung, Kommandoposten-Pfad 3 als Rabatt (10/15 %), Einkommen (700/1.250/2.250 ⛁ je Runde) und Bonus für
  Nachschublager (+50/+100 %).

### T6 – Umsetzungsnotizen

- **Herausforderungen (§7)** ersetzen die Mutatoren (Klasse `Mutator` entfernt): `Challenge` mit Doppelte Boss-HP (×2 HP der Boss-Klasse, +50 %),
  Halbe Münzen (+50 %), Alternative Runden (+30 %), Apokalypse (keine Pause, kein frühes Rufen, +40 %) und CHIMPS (1 Leben, Verkauf 0,
  keine Kriegskasse, kein Nachschublager, +100 %). Das Terminal hat einen Knopf „Modus“; ein gewonnenes Level bringt einen **Meister-Stern**
  für den Modus (Bitmaske je Team, Anzeige „Meister-Sterne n/5“). Der Status-Netzwerkpaket-Protokoll steigt auf „3“.
- **Alternative Runden:** `td_rounds/alternate.json` (Runden 1–140) aus der Seite „Alternate Bloons Rounds“. **bloonswiki.com war beim Einlesen
  mit Fehler 521 nicht erreichbar**, die Daten stammen deshalb aus der Fandom-Kopie `bloons.fandom.com` (Stand 24.08.2026) und müssen bei
  Gelegenheit noch einmal gegen bloonswiki.com geprüft werden. Der Test vergleicht die RBE jeder Runde 3–100 mit dem Wiki-Wert (±0,1 %). Die Runden
  1 und 2 der Alternativliste kommen nur in Herausforderungen vor; die Dauer je Runde übernimmt die Standardliste.
- **Endlosmodus:** Level 71 an (Runde 141) zieht `FreeplayRounds` zufällig, aber pro Runde fest (Seed = Rundennummer, für alle Teams gleich): Budget
  800.000 RBE × 1,03 je Runde, Gruppen aus Behemoth-Klasse, Schattenjägern, Kristall-Golems und Glut/Eisenbrechern mit Tarnung, jede zehnte Runde
  Schwarmkönigin. Die Skalierung nach §3.6 (HP, Tempo) ist als Unit-Test gegen die Wiki-Tabelle hinterlegt. Die Zusammensetzung der Zufallsrunden ist
  ein **gleichwertiger Ersatz** (die Wiki nennt dafür keine Gruppentabelle).
- **Arena-Wissen (§8):** sieben Forschungen mit Militär-Paketen (`Knowledge`): Kriegsvorrat, Bessere Ausrüstung, Feldlazarett, Starke Bolzen,
  Sparsame Bauweise, Zinseszins, Wachsamkeit. Die Wirkung auf Türme steckt in einem eigenen Profil jedes Turms (Kopie des geteilten Profils),
  das neu gebaut wird, wenn sich das Wissen des Teams ändert.
- **Rangliste:** `/craftorio ranking` zeigt die zehn Teams mit der höchsten abgeschlossenen Runde; die Terminal-Zeile zeigt die eigene beste Runde.

### T7 – Umsetzungsnotizen

- **Handbuch:** Neben den Leitfaden-Schritten hat das Buch einen Nachschlageteil (`Lexicon`, aus den Spieldaten gebaut): Wirtschaft, Schwierigkeitsgrade,
  Herausforderungen, Arena-Wissen, alle 17 Gegner mit Leben, Tempo, erster Runde und Immunitäten (vier je Seite) und alle zehn Türme mit Preis,
  Reichweite, Versorgung und den drei Pfaden samt Preisen. Knopf *Nachschlagen* / *Leitfaden*, lange Seiten lassen sich mit dem Mausrad scrollen.
- **Leitfaden-Hinweise:** die Texte der Tower-Defense-Schritte (Level 1, 10, 20, 30, 40, 50) und des Schritts „Verteidigung“ sind auf die neuen Regeln
  umgestellt (Münzen, Gegner greifen keine Türme an, Immunitäten, Kreuzpfad-Regel, Endlosmodus).
- **Migration (§10):** Türme der alten Aufrüststufen (NBT „level“) laden als Pfad 1, Stufe n−1; ihr Wert zum Verkaufen ist der Grundpreis plus die Preise der
  übrigen Stufen. Die Startmünzen alter Arenen gibt `Coins.migratedStart`, der Münzstand und die Kampagnen-Flags kommen aus T2. Ruinen gibt es nicht mehr.
- **Referenz-Aufstellungen:** `PLAN_1_40` (Runde 1–40, zehn Layouts, sechs weitere mit höchstens einem Leben Verlust) und `PLAN_1_89` (Runde 1–89,
  sechs Layouts) ohne Leak; ein bewusst zu schwacher Satz ohne Tarnungserkennung scheitert genau in Runde 24. **Runde 90–100 sind offen:** Schattenjäger
  (Runde 90 und 93) und die zehn verstärkten Behemoths von Runde 93 überfordern jede Aufstellung, die das Hill-Climbing findet; mehrere Nachschublager,
  Kältetechnik (Absoluter Nullpunkt) und Attentäter-Mörser helfen in der Simulation allein nicht genug. Hier fehlt eine bewusst gebaute Wirtschaftsphase
  mit Zeitplan (Farmen zwischen Runde 30 und 50) und ein Satz aus mehreren Boss-Konterntürmen; das ist die Nacharbeit für das Balancing.

---

## 12. Entscheidungen (getroffen: alle wie empfohlen)

| ID | Frage | Entscheidung |
|---|---|---|
| TD-E1 | Vorbild Bloons TD 6? | **Ja**, meistbewertetes TD-Spiel mit 97 % |
| TD-E2 | Eigene Arena-Münzen statt Credits in der Arena? | **Ja**, sonst lässt sich die BTD6-Wirtschaft nicht übernehmen; Fabrik-Ertrag über die begrenzte Kriegskasse **und** Warenlieferungen an das Nachschublager (§4.4) |
| TD-E3 | Leben pro Level oder über die ganze Kampagne? | **Pro Level** (mit Snapshot), sonst kann ein früher Fehler die Kampagne unspielbar machen |
| TD-E4 | Nachschublager-Früchte einsammeln müssen? | **Nein**, automatisch; Pfad 3 bleibt als Wertsteigerung |
| TD-E5 | Exotische Stufe-4/5-Effekte (Nekromant, Sonnentempel-Opfer, Fanclub-Verwandlung) 1:1? | Soweit mit der Gegner-Engine machbar ja; sonst gleichwertiger Ersatz mit denselben Kosten, im Dokument festhalten |
| TD-E6 | Gegner greifen keine Türme mehr an (Turm-HP, Ruinen, Reparatur entfallen)? | **Ja**, wie BTD6 |
| TD-E7 | Helden (BTD6-Heroes) übernehmen? | **Später** (eigenes Paket T8), weil sie viel eigene Mechanik haben |
| TD-E8 | Pfadlängen-Fenster 100–160 Blöcke? | ~~Ja~~ → **ersetzt durch T8** (feste Karten, §13) |

---

## 13. T8 – Karten nach BTD6 (feste Strecken)

In BTD6 legt der Spieler **keinen** Weg: Jede Karte hat eine feste Strecke, die Abwechslung kommt aus vielen Karten in
vier Schwierigkeitskategorien. Das übernehmen wir. Der Pfadstab und der Zufallsgenerator für das Feldinnere
(`ArenaLayout.generate`) entfallen. Die Themen-Paletten aus D7 bleiben für die Optik.

### 13.1 Was wir übernehmen und was nicht

| Übernehmen (Regeln und Zahlen) | Nicht übernehmen |
|---|---|
| Kategorien Anfänger, Fortgeschritten, Profi, Experte | Kartennamen, Grafiken, Texte |
| Pro Karte: Anzahl Eingänge, Ausgänge, Kreuzungen, **Streckenlänge in „Red Bloon Seconds" (RBS)**, Wasseranteil, Sichtblocker, entfernbare Hindernisse mit Preisen, Sondermechanik | Die exakte Streckenform abpausen |

Unsere Karten werden **nachgebaut**: eigene Strecke, aber mit denselben Kennzahlen wie ein bestimmtes BTD6-Vorbild,
damit die BTD6-Balance stimmt. Die Vorbilder stehen intern in den Kartendaten (Feld `reference`), nicht im Spiel.

### 13.2 Kategorien

Laut bloonswiki.com, „List of maps in BTD6": 89 Karten; Spannweite der Streckenlänge (Schwer, RBS):

| Kategorie | Anzahl in BTD6 | Länge (Schwer) | Typische Merkmale |
|---|---|---|---|
| Anfänger | 26 | 26,5–53,4 s | ein langer Weg, viel Platz, kaum Hindernisse |
| Fortgeschritten | 26 | 16,0–35,5 s | Wasser, erste Sichtblocker, teils 2 Wege |
| Profi | 23 | 8,5–25,2 s | kurze Wege, wenig Platz, entfernbare Hindernisse, Sondermechaniken |
| Experte | 14 | 3,5–19,4 s | sehr kurz oder viele Eingänge (z. B. 4 Eingänge → 1 Ausgang), Spezialregeln |

**Umrechnung:**
- Streckenlänge in Blöcken = RBS (Leicht) × 2,5. Wiki-Werte für Mittel ×1,1, für Schwer ×1,25 auf Leicht umrechnen.
- Beispiele:
  - Vorbild Monkey Meadow: Leicht 36,5 s → **91 Blöcke**
  - Vorbild Infernal: 2 Wege à Leicht 24,4/24,1 s → je ca. **61 Blöcke**, Eingänge im Wechsel pro Runde
  - Vorbild Dark Castle: 4 Eingänge (Leicht 12,5/11,4/11,4/12,5 s → 31/29/29/31 Blöcke) zu 1 Ausgang, 2 Kreuzungen;
    Bäume über den Eingängen machen Gegner unverwundbar und kosten je 1.000 ⛁ zum Entfernen

### 13.3 Kartenformat (datengetrieben)

- `data/craftorio/td_maps/<id>.json`:
  - `name`: Sprachschlüssel
  - `category`, `theme` (Wald/Berge/Feuer/Wasser/Kolosseum)
  - `size`: Standard 41 × 41, Anfänger-Karten bis 61 × 61
  - `reference`: BTD6-Vorbild, nur intern
  - `grid`: ASCII-Raster, ein Zeichen pro Block

    | Zeichen | Bedeutung |
    |---|---|
    | `.` | Boden, Turm erlaubt |
    | `#` | Hindernis, blockiert Sicht |
    | `~` | Wasser, kein Turm |
    | `^` | Plateau, Turm erlaubt, blockiert Sicht von unten |
    | `R` | entfernbar (Kosten in `removables`) |
    | `=` | Strecke (nur zur Kontrolle, siehe `paths`) |
  - `paths`: Liste von Wegen, jeweils Eingang, Wegpunkte (gerade Abschnitte zwischen Blockkoordinaten) und Ausgang;
    Kreuzungen über gemeinsame Wegpunkte
  - `removables`: Position/Fläche, Preis in ⛁, was sich danach ändert (Sichtblocker weg, Bauplatz frei, Typ wechselt)
  - `mechanics`: optionale Sondermechanik, siehe §13.5
- Der `ArenaBuilder` baut die Karte aus Raster + Themen-Palette (D7). Strecke als Pfadblöcke, Eingänge als Portale in
  der Wand, Ausgänge als **Leck-Tore** zum Kern (mehrere Ausgänge teilen sich die Leben).
- Das ASCII-Format lässt sich gut ohne Spiel erstellen und prüfen; jede Karte hat eine Vorschau als PNG
  (`docs/screenshots/maps/<id>.png`, Datagen-Skript).

### 13.4 Erster Kartensatz (20 Karten)

Pro Karte ein BTD6-Vorbild. Die Kennzahlen (Wege, RBS je Weg, Wasser, Sichtblocker, entfernbare Hindernisse mit
Preisen, Mechanik) liest die Umsetzung von der Wiki-Seite `https://www.bloonswiki.com/<Name>_(BTD6)` ab und trägt sie
in die JSON ein.

| Kategorie | Anzahl | Vorbilder (Vorschlag) |
|---|---|---|
| Anfänger | 6 | Monkey Meadow, Logs, Tree Stump, Town Center, Alpine Run, Park Path |
| Fortgeschritten | 6 | Balance, Encrypted, Bazaar, Spring Spring, Moon Landing, Streambed |
| Profi | 5 | Cornfield, Dark Path, Spillway, Cargo, Peninsula |
| Experte | 3 | Infernal, Dark Castle, Ravine |

Themen-Zuordnung frei nach Optik (z. B. Feuer für Infernal, Wasser für Streambed). Nicht zu Minecraft passende
Mechaniken bekommen einen gleichwertigen Ersatz, im JSON dokumentiert.

### 13.5 Kartenmechaniken

| Mechanik | BTD6 | Umsetzung |
|---|---|---|
| Sichtlinie | Hindernisse und höheres Gelände blockieren das Zielen | Strahlprüfung Turm → Gegner über das Raster; **Mörser** (schießt nach oben), zielsuchendes Tesla (Pfad 1 Stufe 1) und Frost-Aura ignorieren Sichtlinien; Geschütz trotz unendlicher Reichweite **nicht** |
| Entfernbare Hindernisse | Preise fest, unabhängig vom Schwierigkeitsgrad | Rechtsklick in der Arena → Bestätigung → ⛁ abziehen, Blöcke ersetzen |
| Mehrere Eingänge gleichzeitig | Gruppen einer Runde werden auf die Eingänge verteilt (Wiki je Karte) | `paths` mit Gewichten; Standard: reihum |
| Wechselnde Eingänge | Infernal: ungerade Runden oben, gerade unten | `mechanics: alternate_entrances` |
| Deckung am Eingang | Dark Castle: unter Bäumen unverwundbar, bis entfernt | `mechanics: entrance_cover` + Entfernbar |
| Wasser | Bauplatz nur für Wasser-Türme | kein Wasserturm im Turmsatz → Wasser ist nicht bebaubar (später ggf. ein Boot-Turm) |

### 13.6 Kartenwahl in der Kampagne

| Variante | Ablauf | Nähe zu BTD6 |
|---|---|---|
| **A – Akte (Empfehlung)** | Die Kampagne hat **5 Akte à 10 Level** (Level 1–10, 11–20 …). Zu Beginn jedes Akts wählt das Team **eine Karte aus drei angebotenen**; sie bleibt für den ganzen Akt (20 Runden) | hoch: In BTD6 spielt man ein ganzes Spiel auf einer Karte und baut seine Verteidigung dort aus |
| B – Neue Karte pro Level | Jedes Level (2 Runden) eine neue Karte aus dem Pool, wie bisher | geringer: Verteidigung muss alle 2 Runden neu aufgestellt werden |

- Angeboten werden Karten aus Kategorien, die zum Akt passen: Akt 1 nur Anfänger; ab Akt 2 je eine Karte aus zwei
  Kategorien; ab Akt 4 auch Experte.
- **Credits-Belohnung nach Kategorie:** Anfänger ×1,0; Fortgeschritten ×1,15; Profi ×1,3; Experte ×1,5 (Vorschlag,
  angelehnt an die höheren Belohnungen schwerer Karten in BTD6).
- Sterne und Rangliste werden pro Karte gespeichert (wie Medaillen in BTD6).
- Bei Variante A wandern Türme nur beim **Aktwechsel** ins Depot; innerhalb eines Akts bleibt alles stehen.
- Variante A **ändert** eine frühere Entscheidung (Arena-Umbau: „Das Innere der Zone soll sich jedes Level
  verändern"). Abwechslung entsteht dann durch die Kartenwahl pro Akt statt durch eine neue Karte alle 2 Runden.

### 13.7 Paket T8

| Inhalt | Akzeptanz |
|---|---|
| Kartenformat §13.3 inkl. Loader und Validierung | Unit-Test pro Karte: Strecke zusammenhängend, alle Eingänge erreichen einen Ausgang, Längen je Weg = Vorbild-RBS × 2,5 ± 10 %, Kreuzungen und Eingänge wie Vorbild, mindestens so viel Bauland wie das Vorbild grob vorgibt (≥ 150 Blöcke Anfänger … ≥ 60 Experte) |
| 20 Karten nach §13.4 | Vorschau-PNGs im Repo |
| `ArenaBuilder` baut Karten; Leck-Tore; Mechaniken §13.5 | GameTests: Sichtblocker verhindert Zielen; Entfernen kostet ⛁ und öffnet Bauplatz; wechselnde Eingänge wechseln pro Runde |
| Kartenwahl §13.6 (Akte), Belohnungsfaktor, Sterne pro Karte | GameTest: Akt-Start bietet 3 Karten an; Wahl bleibt 10 Level |
| Pfadstab und `ArenaLayout.generate` entfernen; Migration: Pfadstab-Items verschwinden, laufende Zonen bekommen beim nächsten Level-Start eine Karte | alle bisherigen Tests angepasst und grün |
| Balancing-Simulator nutzt die echten Kartenlängen | Referenz-Sets schaffen Runde 1–100 auf je einer Anfänger- und einer Profi-Karte; Experte nur mit erweitertem Set |
| Handbuch-Seite „Karten", README | – |

### 13.8 Entscheidungen T8

| ID | Frage | Empfehlung (gilt, falls nichts anderes entschieden wird) |
|---|---|---|
| T8-E1 | Feste Karten statt Pfadstab? | **Ja** (BTD6 1:1) |
| T8-E2 | Nachbauen mit gleichen Kennzahlen statt Strecken 1:1 abpausen? | **Ja**: gleiche Balance, eigene Gestaltung, keine Übernahme fremder Kartendesigns |
| T8-E3 | Kartenwahl: Akte à 10 Level (A) oder neue Karte pro Level (B)? | **A**, näher an BTD6 und weniger Neuaufbau in Minecraft |
| T8-E4 | Umfang zum Start? | 20 Karten (6/6/5/3), später erweiterbar |
| T8-E5 | Wasser-Türme (Boot) für Wasserflächen? | **Später**; vorerst ist Wasser nicht bebaubar |

