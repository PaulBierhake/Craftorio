# Craftorio – Tower Defense nach Bloons TD 6

Status: **Entwurf zur Freigabe** (Entscheidungen in §12). Dieses Dokument ersetzt die TD-Balancing-Regeln aus
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
| Feste Strecke | Spieler legt den Pfad mit dem Pfadstab, aber mit **Längenfenster** (§5.4) |

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

Ab Runde 81 gelten die BTD6-Vereinfachungen *(prüfen)*:
- Golems werden zu **Superkristallgolems** (60 HP, geben nur 1 Schimmer frei).
- Schimmer, Zwielicht, Eisenbrecher und Ruß/Frost/Glut geben nur noch **ein** Kind frei (RBE sinkt entsprechend,
  siehe Tabellen der Wiki-Seiten).

**Bestehende Gegner:**
- Spitter (Fernangriff auf Türme) **entfällt** (§4.6).
- Die bisherigen Typen werden auf die Tabelle abgebildet: Krabbler → Krabbler-Stufen, Brecher → Eisenbrecher,
  Kristallgolem → Kristallgolem, Brutmutter, Behemoth, Schwarmkönigin wie oben.
- Aussehen: Krabbler in 5 Farben und Größen; Ruß, Frost und Glut als Farbvarianten; Eisenbrecher mit Metallpanzer usw.

### 3.3 Eigenschaften (Modifikatoren)

| Eigenschaft | BTD6 | Regel |
|---|---|---|
| **Getarnt** | Camo | Nur Türme mit Tarnungserkennung zielen auf ihn (§6.5). Flächenschaden trifft ihn trotzdem, wenn er ein anderes Ziel trifft. |
| **Nachwachsend** | Regrow | Stellt alle **3 s** die nächsthöhere verlorene Schicht wieder her, bis zur Ausgangsstufe *(prüfen)*. |
| **Gepanzert** | Fortified | Doppelte HP für Eisenbrecher, Golems und Boss-Klasse (Eisenbrecher 1 → 4). |

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

Nach bloons.fandom.com „Late Game and Freeplay":

| Runden | Geschwindigkeit aller Gegner | HP der Boss-Klasse |
|---|---|---|
| 81–100 | +2 % pro Runde (Runde 100: ×1,4) | +2 % pro Runde (Runde 100: ×1,4) |
| 101–124 | Sprung auf ×1,6, dann +2 % pro Runde | +5 % pro Runde (Runde 124: ×2,6) |
| 125–150 | +2 % pro Runde | +15 % pro Runde (Runde 150: ×6,5) |
| 151–250 | +2 % pro Runde | +35 % pro Runde (Runde 250: ×41,5) |

---

## 4. Runden, Wirtschaft, Leben

### 4.1 Runden

- **Level L = Runden 2L−1 und 2L.** Zwischen den Runden eine kurze Pause (Standard 10 s) oder Start per Knopf.
  Die nächste Runde darf wie in BTD6 jederzeit früher gestartet werden (ohne Bonus).
- **Rundeninhalt 1:1 aus der BTD6-Standardliste.** Runden 1–40 unten; Runden 41–100 von
  `https://www.bloonswiki.com/List_of_rounds_in_BTD6` übernehmen, als Datendatei
  `data/craftorio/td_rounds/standard.json` (Gruppen: Anzahl, Typ, Eigenschaften, Abstand, Startzeit).
- Ab Runde 101 **Endlosmodus** (Freeplay) mit den Regeln aus §3.6 und zufälligen Runden (BTD6-Logik ab 141 *(prüfen)*).

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
| 14 | gemischt rot/blau/grün/gelb *(Gruppen aus Wiki)* | 145 |
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
| 38 | 17 Frost, 42 rosa, 14 Eisenbrecher, 10 Zwielicht, 2 Golems | 1.139 |
| 39 | 10 Ruß, 10 Frost, 20 Zwielicht, 18 Schimmer, 2 Schimmer nachwachsend | 1.620 |
| 40 | 1 Brutmutter | 616 |

Erstes Auftreten (Kontrolle): Nachwachsend R17, Ruß R20, Frost R22, Tarnung R24, Zwielicht R26, Eisenbrecher R28,
Schimmer R35, Golem R38, Brutmutter R40, Gepanzert R45, Behemoth R60, Koloss R80, Schattenjäger R90, Schwarmkönigin R100.
Referenz-RBE: R45 2.289, R50 3.540, R60 3.164, R80 16.656, R100 67.200.

### 4.2 Arena-Münzen (⛁)

| Regel | Wert (BTD6) |
|---|---|
| Startguthaben der Kampagne | 650 ⛁ *(prüfen)* |
| Pro zerstörter Schicht | 1 ⛁ × Einkommensfaktor |
| Rundenbonus | **100 + Rundennummer** (bestätigt: Runde 1 bringt 20 + 101 = 121) |
| Einkommensfaktor | R1–50: 100 %; R51–60: 50 %; R61–85: 20 %; R86–100: 10 %; R101–120: 8 %; R121–140: 5 %; R141+: 2 % |
| Verkauf eines Turms | 70 % des bezahlten Preises (Turm + Aufrüstungen) |
| Münzen | gehören dem Team, gelten über alle Level; Anzeige im HUD in der Arena |

Referenz zum Prüfen: Die Wiki-Rundenliste hat eine Spalte „Cash" pro Runde. Ein Unit-Test vergleicht die berechneten
Rundeneinnahmen mit dieser Spalte für Runde 1–100 (Toleranz ±2 %).

### 4.3 Schwierigkeitsgrade

Pro Team im Terminal wählbar, bevor Level 1 startet (danach nur nach unten änderbar):

| Stufe | Leben pro Level | Preisfaktor | Belohnung (Credits) |
|---|---|---|---|
| Leicht | 200 | ×0,85 | ×0,75 |
| Mittel (Standard) | 150 | ×1,0 | ×1,0 |
| Schwer | 100 | ×1,08 | ×1,25 |
| Unbesiegbar | 1 | ×1,2 | ×1,5 |

**Leben** gelten pro Level (siehe Entscheidung TD-E3). Ein durchgelassener Gegner kostet so viele Leben wie seine
**RBE inklusive Kinder**. Für die Boss-Klasse gelten eigene Werte laut Wiki *(prüfen: Brutmutter-Leak)*; ein Leak der
Boss-Klasse beendet auf Schwer und Unbesiegbar das Level praktisch sofort, wie in BTD6.

### 4.4 Credits und Münzen

- **Credits** (Fabrik, Handelsposten) und **Münzen** (Arena) sind getrennt.
- **Kriegskasse:** Im Terminal lassen sich Credits in Münzen tauschen, **1 : 1, höchstens 100 × Level Münzen pro
  Level** (Level 20: 2.000 ⛁). Die Fabrik hilft also wie eine kleine Farm, trivialisiert die Arena aber nicht.
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

### 5.4 Pfadlänge

Die BTD6-Balance setzt eine feste Streckenlänge voraus. Der Pfadstab erzwingt deshalb ein **Längenfenster**:
- Mindestens **100**, höchstens **160 Blöcke** (Standard in BTD6-Karten grob 1.000–1.600 Einheiten *(prüfen)*).
- Das Level startet nur mit einem Pfad in diesem Fenster; der Kern zeigt die aktuelle Länge an.
- Die bisherige Grenze `PathTracer.MIN_LENGTH`/`MAX_LENGTH` (20/400) wird ersetzt.
- `ArenaLayout.generate` muss garantieren, dass ein Pfad von mindestens 100 Blöcken möglich ist (Test über alle Themen
  und Seeds).

---

## 6. Türme

### 6.1 Aufrüstsystem

- **3 Pfade × 5 Stufen.** Ein Pfad bis Stufe 5, ein zweiter bis Stufe 2, der dritte gesperrt
  (Beispiele: 5-2-0, 2-0-5, 0-3-2 nicht erlaubt).
- Kosten in Münzen = BTD6-Mittel-Preis × Schwierigkeitsfaktor.
- **Freischaltung durch Forschung** (ersetzt BTD6-XP):

  | Stufe | Forschung | Kosten *(Vorschlag)* | Voraussetzung |
  |---|---|---|---|
  | 1–2 | – | – | – |
  | 3 | „Turmtechnik I" | 75 × R+G | Turm-Forschung des jeweiligen Turms |
  | 4 | „Turmtechnik II" | 150 × R+G+M | Militär-Paket |
  | 5 | „Turmtechnik III" | 300 × R+G+M+B | Chemie-Paket |
- **Fähigkeiten** (z. B. Schneesturm, Versorgungsabwurf, Boss-Attentäter): Knopf im Turm-GUI und eine Taste, wenn der
  Spieler in der Arena ist; Abklingzeit wie BTD6.
- **Verkaufen:** 70 % (§4.2).

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

**Armbrustturm (Dart Monkey)**

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
| **T1 – Gegner-Engine** | `TdSimulation` (Bewegung, Schichten, Kinder, Überschaden, Eigenschaften, Immunitäten, Leaks, spätes Spiel), Gegnerdaten §3 als JSON, Netzwerkpaket, Client-Renderer, Entfernen der Gegner-Entities | Unit-Tests für jeden Gegner (RBE aus Kindern = Tabelle), Nachwachsen alle 3 s, Tarnung; Leistungstest 1.000 Gegner |
| **T2 – Runden, Münzen, Leben** | Rundendaten 1–100, Level = 2 Runden, Münzen und Einkommensfaktor, Rundenbonus, Schwierigkeitsgrade, Leben pro Level, Snapshot/Wiederholung, Kriegskasse, Siegel-Zuordnung, Entfernen von Turm-HP/Ruinen/Reparatur und Spieler-Skalierung, Pfadlängen-Fenster | Unit-Test Rundeneinnahmen gegen Wiki (±2 %); GameTest Level 1 komplett; Pfad < 100 Blöcke wird abgelehnt |
| **T3 – Turmsystem** | Pfade × Stufen mit Kreuzpfad-Regel, Münzpreis beim Platzieren, Verkauf 70 %, Zielmodi, Tarnungserkennung, Fähigkeiten, Forschung Turmtechnik I–III, Bauteile ab Stufe 3, neues Turm-GUI mit drei Pfaden | Unit-Tests Kreuzpfad-Regel und Verkaufswert; GameTest: Stufe 3 ohne Forschung gesperrt |
| **T4 – Türme Teil 1** | Armbrust, Geschütz (mit Magazinarten), Mörser (neu), Flammenwerfer, Nachschublager (neu) mit allen 15 Aufrüstungen | Simulator: Referenz-Set R1–40 ohne Leak |
| **T5 – Türme Teil 2** | Frost, Leim, Tesla, Laser, Kommandoposten mit allen Aufrüstungen | Simulator: Referenz-Set R41–100 ohne Leak; Immunitätstabelle §6.4 als Test |
| **T6 – Modi und Endlos** | Herausforderungen §7, Endlosmodus ab Runde 101 mit §3.6, Arena-Wissen §8, Rangliste (höchste Runde pro Team) | GameTest je Modus; Freeplay-Skalierung als Unit-Test |
| **T7 – Leitfaden, Handbuch, Feinschliff** | Handbuch-Seiten: Gegner (mit Immunitäten), Türme (Pfade), Wirtschaft; Leitfaden-Hinweise; README; Migration §10 | Alle Tests grün; Kampagne im Simulator mit Referenz-Sets bis Runde 100 schaffbar |

---

## 12. Entscheidungen

| ID | Frage | Empfehlung (gilt, falls nichts anderes entschieden wird) |
|---|---|---|
| TD-E1 | Vorbild Bloons TD 6? | **Ja**, meistbewertetes TD-Spiel mit 97 % |
| TD-E2 | Eigene Arena-Münzen statt Credits in der Arena? | **Ja**, sonst lässt sich die BTD6-Wirtschaft nicht übernehmen; Kriegskasse als begrenzte Brücke |
| TD-E3 | Leben pro Level oder über die ganze Kampagne? | **Pro Level** (mit Snapshot), sonst kann ein früher Fehler die Kampagne unspielbar machen |
| TD-E4 | Nachschublager-Früchte einsammeln müssen? | **Nein**, automatisch; Pfad 3 bleibt als Wertsteigerung |
| TD-E5 | Exotische Stufe-4/5-Effekte (Nekromant, Sonnentempel-Opfer, Fanclub-Verwandlung) 1:1? | Soweit mit der Gegner-Engine machbar ja; sonst gleichwertiger Ersatz mit denselben Kosten, im Dokument festhalten |
| TD-E6 | Gegner greifen keine Türme mehr an (Turm-HP, Ruinen, Reparatur entfallen)? | **Ja**, wie BTD6 |
| TD-E7 | Helden (BTD6-Heroes) übernehmen? | **Später** (eigenes Paket T8), weil sie viel eigene Mechanik haben |
| TD-E8 | Pfadlängen-Fenster 100–160 Blöcke? | **Ja**, im Spieltest mit dem Simulator nachjustieren |
