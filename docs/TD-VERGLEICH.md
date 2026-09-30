# Tower Defense: Vergleich mit Bloons TD 6 und Vorschlag

Bloons TD 6 (BTD6) gilt als eines der bestbewerteten Tower-Defense-Spiele und ist das Vorbild. **Alle Zahlen in diesem Dokument sind
jetzt gegen bloonswiki.com (Stand Version 56.3) geprüft**; bloons.fandom.com blieb gesperrt (HTTP 403), wurde aber nicht mehr gebraucht.
Quelle der Tabellen: die Wiki-Seiten „Bloons TD 6", „Rounds (BTD6)" und die Turmseiten („Costs and sell values").

## 1. Was BTD6 ausmacht

| Bereich | BTD6 |
|---|---|
| Gegner | Schichtblasen (Rot → Blau → Grün → Gelb → Pink), die beim Platzen die nächste Schicht freigeben; dazu **Eigenschaften mit festen Gegenmitteln**: Camo (nur mit Erkennung), Blei (immun gegen scharfe Geschosse), Schwarz/Weiß/Zebra (Explosion/Kälte), Keramik (viel Leben, gibt 2 Regenbogen frei), Regrow, Fortified; ab Runde ~40 Blimps (MOAB → BFB → ZOMG → DDT → BAD), die kleinere Blimps freigeben |
| Türme | 23+ Türme in 4 Klassen (Primär, Militär, Magie, Unterstützung), je **3 Upgrade-Pfade × 5 Stufen**; nur ein Pfad bis 5, ein zweiter bis 2 (Kreuzpfad-Regel, „5/2/0") |
| Wirtschaft | **Geld kommt aus dem Spiel selbst**: je geplatzter Blase plus Bonus am Rundenende; Türme und Upgrades kosten Geld, Verkauf 70 % zurück |
| Runden | Feste, skriptbare Runden (Gegnergruppen mit Typ, Anzahl, Abstand, Camo/Regrow-Flags); **Türme bleiben über alle Runden stehen** |
| Schwierigkeit | Easy 40 Runden, 200 Leben, Preise 85 %; Medium 60 Runden, 150 Leben; Hard 80 Runden, Preise 108 %; Impoppable 100 Runden, 1 Leben, Preise 120 % |
| Wege | **Feste Wege je Karte**; der Spieler wählt nur, wo er baut (Land/Wasser, Hindernisse, Bauflächen) |
| Komfort | Schnellvorlauf, Auto-Start, Zielmodi (Erste/Letzte/Nah/Stark), aktive Fähigkeiten mit Abklingzeit, ein Held mit Stufen, Dauerfortschritt (Monkey Knowledge) |

### 1a. Die geprüften Zahlen

**Blasen** (Gesundheit = Schichten; RBE = Treffer, die die Blase samt Kindern aushält; Tempo in Einheiten/s)

| Blase | RBE | Tempo | Immun gegen | Kinder |
|---|---|---|---|---|
| Rot | 1 | 25 | – | – |
| Blau / Grün / Gelb / Pink | 2 / 3 / 4 / 5 | 35 / 45 / 80 / 87,5 | – | je die Vorstufe |
| Schwarz | 11 | 45 | Explosion | 2 × Pink |
| Weiß | 11 | 50 | Kälte | 2 × Pink |
| Lila | 11 | 75 | Energie, Plasma, Feuer, Frost | 2 × Pink |
| Blei | 23 | 25 | Scharf (Darts), Kälte, Energie | 2 × Schwarz |
| Zebra | 23 | 45 | Explosion, Kälte | Weiß + Schwarz |
| Regenbogen | 47 | 55 | – | 2 × Zebra |
| Keramik | 104 (10 Leben) | 62,5 | – | 2 × Regenbogen |
| MOAB | 616 (200 Leben) | 25 | – | 4 × Keramik |
| BFB | 3.164 (700) | 6,25 | – | 4 × MOAB |
| ZOMG | 16.656 (4.000) | 4,5 | – | 4 × BFB |
| DDT | 816 (400) | 66 | fast alle (Camo, Regrow, Blei) | 4 × Keramik |
| BAD | 55.760 (20.000) | 4,5 | Verlangsamung, Rückstoß | 2 × ZOMG + 3 × DDT |

Eigenschaften: **Camo** (nur Türme mit Erkennung sehen sie), **Regrow** (eine Schicht alle 3 s zurück), **Fortified** (doppelte Schichtgesundheit,
Blei vierfach). Ein Leck kostet so viele Leben, wie die Blase RBE hat (ein MOAB also 616).

**Wann was kommt (Standardrunden):** Regrow ab Runde 17, Camo 24, Zebra 26, Blei 28, Regenbogen 35, Keramik 38, erster MOAB 40, Fortified 45,
BFB 60, ZOMG 80, DDT 90, BAD 100. Jede Neuerung kommt in einem eigenen Schritt, meist im Abstand von 4 bis 10 Runden, und wird am Rundenende
per Hinweis erklärt.

**Geld:** Jede geplatzte Schicht gibt 1 $; am Rundenende gibt es **100 $ + Rundennummer** (Runde 1: 101 $, Runde 40: 140 $). Summe bis Runde 40 etwa
12.000 $ aus Abschüssen plus 4.800 $ Rundenbonus, bis Runde 60 rund 46.700 $ plus 7.800 $. Ab Runde 51 gibt jede Blase nur noch die Hälfte, ab 61 20 %,
ab 86 10 %. **Verkauf gibt 70 % zurück** (Dart: 200 $ → 140 $).

**Türme** (Medium): 24 Stück in vier Klassen, Grundpreise von 200 $ (Dart), 250 (Wizard), 350 (Sniper), 375 (Bombe), 400 (Ninja, Druide) über
800–850 (Ace, Dartling) bis 1.250 (Bananenfarm) und 2.500 (Super-Affe). Jeder Turm hat 3 Pfade × 5 Stufen, **eine Stufe-5-Pfad plus einen bis Stufe 2**.
Gesamtkosten: Dart 340 $ (1-0-0), 860 (3-0-0), 2.660 (4-0-0), **17.660 (5-0-0)**; Sniper 42.500; Bombe 60.175; Super-Affe 627.000.
Stufe 1 kostet etwa 70 % des Grundpreises, Stufe 4 das Fünf- bis Zehnfache, **Stufe 5 das Fünf- bis Zehnfache von Stufe 4**.

**Schwierigkeit:** Easy 40 Runden, 200 Leben, Preise 85 %, langsamere Blasen; Medium 60 Runden, 150 Leben, Preise 100 %; Hard 80 Runden, Preise 108 %;
Impoppable 100 Runden, 1 Leben, Preise 120 %. Ab Runde 81 steigen Blasentempo und MOAB-Gesundheit je Runde (+2 %), später stufenweise mehr.

**Was das für die Kurve bedeutet:** Ein Stufe-5-Dart (17.660 $) kostet so viel wie das gesamte Einkommen bis Runde ~40: **Stufe-5-Türme sind
Spätspiel-Ziele**, bis dahin entscheidet die Mischung aus Stufe-2- bis Stufe-4-Türmen. Der Einzeltreffer-Schaden steigt dabei kaum; Stärke
kommt aus **Durchschlag (Pierce), Reichweite und Spezialfunktionen**.

## 2. Wo Craftorio heute steht

| Bereich | Craftorio |
|---|---|
| Gegner | 7 flache Typen (Kriecher, Brecher, Spucker, Golem mit 35 % physisch, Behemoth mit Flachpanzerung, Brutmutter, Schwarmkönigin); Brut-Phasen nur bei der Königin; **keine Tarnung, keine Regeneration, keine Schichten** |
| Türme | 5 Typen (Armbrust, Geschütz, Tesla, Laser, Flamme); Upgrade **linear** Stufe I–V (je +30 % Schaden, +25 % Leben); kein Pfad, keine Entscheidung |
| Wirtschaft | Turm bauen kostet **Material aus der Fabrik**; Upgrade kostet Credits plus Material; Credits nur als Levelbelohnung; **im Level kein Geld durch Abschüsse** |
| Runden | Level mit 3–8 Wellen; **nach jedem Level kommt eine neue Karte, alle Türme wandern ins Depot** und müssen neu gesetzt werden |
| Schwierigkeit | 10 Leben, Leck kostet 1–10 Leben je Gegnertyp; Level-Formel unbegrenzt, Gesundheit +12 % je Level |
| Wege | **Spieler legt den Weg selbst** mit dem Pfadstab, Karte gibt nur die Route vor |
| Komfort | Zielmodi ✓, Welle früh rufen ✓, Auto-Start ✓, Mutatoren und Kartenthemen ✓ (mehr als BTD6), Flüssigkeits- und Strom-Nachschub ✓ (Fabrikanbindung) |

## 3. Warum es sich „nicht rund" anfühlt (Vermutung, zu prüfen)

1. **Kein Aufbau über Zeit:** Türme verschwinden nach jedem Level. In BTD6 wächst die Verteidigung über 40–100 Runden.
2. **Keine Entscheidungen:** lineare Upgrades und Material statt Geld lassen nichts abwägen; in BTD6 prägt die Pfadwahl jeden Turm.
3. **Keine Gegenmittel-Logik:** Jeder Gegner wird mit jedem Turm bekämpft, nur Golem und Behemoth unterscheiden sich. BTD6 zwingt zu gemischten Verteidigungen (Camo, Blei, Keramik, Blimps).
4. **Kein Fluss im Level:** Kein Geld aus Abschüssen, daher keine Spannung „noch schnell einen Turm kaufen".
5. **Weg-Bau als Pflichtaufgabe:** Jedes Level erst einen Weg legen ist Arbeit ohne Spielwert, wenn der Weg ohnehin eindeutig ist.
6. **Zu wenig Verzeihung:** 10 Leben bei Lecks von bis zu 10 Leben ist abrupt; BTD6 gibt 150–200 Leben und zieht nach Restlebensmenge ab.

## 4. Vorschlag (in Stufen, je ein Commit, ein Spieltest dazwischen)

**TD-A: Persistenz und Wege** *(klein, größter Effekt)*
- Die Karte bleibt **10 Level lang** stehen (ein „Durchgang" = Level 1–10, 11–20, …); Türme bleiben stehen, repariert wird zwischen den Levels. Neue Karte erst nach jedem Boss-Level.
- Der Weg ist je Karte **fest vorgegeben und automatisch gelegt** (der Pfadstab entfällt); gebaut wird nur an den Flächen daneben.
- Leben: 100 statt 10, Leck kostet die Gesundheit des Gegners in Leben-Einheiten (Brutmutter viel, Kriecher 1).

**TD-B: Wirtschaft im Level**
- Kills geben **Level-Credits** nach Gesundheit des Gegners (1 je 10 Gesundheitspunkte, mindestens 1; Kriecher ≈ 1–2, Brecher ≈ 6, Golem ≈ 15), zusätzlich am Wellenende **100 + Levelnummer** wie bei BTD6; die Credits gelten nur in der Arena und werden am Levelende in echte Credits umgerechnet (z. B. 25 %), damit die Fabrik-Wirtschaft nicht aus dem Gleichgewicht gerät.
- Turm bauen kostet **Level-Credits statt Material** *oder* beides (Entscheidung unten). Verkauf gibt 70 % zurück.

**TD-C: Upgrade-Pfade**
- Jeder Turm bekommt **3 Pfade × 5 Stufen** (wie BTD6) mit Kreuzpfad-Regel 5/2/0; Stufe 5 ist das teure Spätspiel-Ziel; Beispiele: Armbrust (Mehrfachschuss / Durchschlag / Tarnerkennung), Geschütz (Feuerrate / Uran-Munition / Reichweite), Flamme (Breite / Brandschaden / Kältedüse), Laser (Durchdringung / Schaden / Kettenblitz), Tesla (Ketten / Stun / Energiesparen).
- Kosten in Level-Credits nach BTD6-Muster: Stufe 1 ≈ 70 % des Turm-Grundpreises, Stufe 2 ≈ 100 %, Stufe 3 ≈ 2–3 ×, Stufe 4 ≈ 6–10 ×, Stufe 5 ≈ 50–100 × (Grundpreise: Armbrust 200, Geschütz 350, Flamme 400, Tesla 500, Laser 600); Fabrik-Materialien nur für die Turm-Basis. Verkauf 70 %.

**TD-D: Gegner-Eigenschaften und Gegenmittel**
- Eigenschaften **Tarnung** (nur Türme mit Erkennung oder Laser sehen sie), **Regeneration** (alle 3 s ein Gesundheitsabschnitt zurück), **Panzerung**, **Fortified** (doppelte Gesundheit); Brut-Gegner, die beim Tod kleinere freigeben (Brutmutter → Kriecher, wie Keramik → Regenbogen).
- **Einführungsplan nach BTD6:** jede Neuerung in einem eigenen Level mit Hinweis am Levelende: Regeneration ab Level 8, Tarnung ab 12, Panzerung (Blei-Ersatz) ab 15, Golem ab 20, Brut-Gegner ab 25, erster Riese (Brutmutter-Klasse) ab 30, Fortified ab 36, Behemoth ab 40, Königin bei 50.
- Leben-Verlust nach Gesundheit statt nach Typ (1 Leben je 10 Gesundheitspunkte, Obergrenze 25 je Leck) bei 100 Leben.
- Runden aus **festen Skripten** (Gruppen mit Typ, Anzahl, Abstand, Flags) statt der reinen Formel; Level 10/20/30/40/50 bleiben Boss-Level.

**TD-E: Komfort**
- Schnellvorlauf, aktive Fähigkeit je Turm-Pfad (Stufe 4), Turm-Verkauf, Zielmodi um „Stark" und „Nah" erweitern.

## 5. Entscheidungen, die ich von dir brauche

1. **Persistenz:** Karte 10 Level stehen lassen (TD-A)? Oder sogar ein Durchgang bis Level 50 am Stück?
2. **Weg:** fest vorgegeben (BTD6) oder weiter selbst legen? Mein Vorschlag: fest, der Stab entfällt.
3. **Turmkosten:** nur Level-Credits (BTD6-Gefühl, Fabrik liefert nur Munition und Strom), nur Material (Fabrik-Gefühl wie jetzt) oder beides?
4. **Umfang:** Alle Stufen A–E nacheinander, oder erst A und B testen? Mein Vorschlag: A + B, Spieltest, dann C und D.
5. **Verknüpfung mit der Forschung:** Die Siegel bei Level 5/10/20/30/40/50 bleiben Pflicht für neue Pakete; dann dürfen Level nicht zu schwer werden, sonst blockiert die Verteidigung den Forschungsfortschritt (Op-Befehl `/craftorio arena clear` gibt es als Notausgang).
