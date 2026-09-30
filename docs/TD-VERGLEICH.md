# Tower Defense: Vergleich mit Bloons TD 6 und Vorschlag

Bloons TD 6 (BTD6) gilt als eines der bestbewerteten Tower-Defense-Spiele und ist das Vorbild. Die Wiki-Seiten (bloons.fandom.com,
bloonswiki.com) waren aus dieser Umgebung nicht abrufbar. **Schwierigkeitsstufen, Leben und Preisfaktoren sind per Websuche bestätigt**,
die übrigen BTD6-Zahlen stammen aus dem Gedächtnis und sind mit *(Gedächtnis)* markiert; vor der Umsetzung gegen das Spiel prüfen.

## 1. Was BTD6 ausmacht

| Bereich | BTD6 |
|---|---|
| Gegner | Schichtblasen (Rot → Blau → Grün → Gelb → Pink), die beim Platzen die nächste Schicht freigeben; dazu **Eigenschaften mit festen Gegenmitteln**: Camo (nur mit Erkennung), Blei (immun gegen scharfe Geschosse), Schwarz/Weiß/Zebra (Explosion/Kälte), Keramik (viel Leben, gibt 2 Regenbogen frei), Regrow, Fortified *(Gedächtnis)*; ab Runde ~40 Blimps (MOAB → BFB → ZOMG → DDT → BAD), die kleinere Blimps freigeben |
| Türme | 23+ Türme in 4 Klassen (Primär, Militär, Magie, Unterstützung), je **3 Upgrade-Pfade × 5 Stufen**; nur ein Pfad bis 5, ein zweiter bis 2 (Kreuzpfad-Regel, „5/2/0") |
| Wirtschaft | **Geld kommt aus dem Spiel selbst**: je geplatzter Blase plus Bonus am Rundenende; Türme und Upgrades kosten Geld, Verkauf 70 % zurück *(Gedächtnis)* |
| Runden | Feste, skriptbare Runden (Gegnergruppen mit Typ, Anzahl, Abstand, Camo/Regrow-Flags); **Türme bleiben über alle Runden stehen** |
| Schwierigkeit | Easy 40 Runden, 200 Leben, Preise 85 %; Medium 60 Runden, 150 Leben; Hard 80 Runden, Preise 108 %; Impoppable 100 Runden, 1 Leben, Preise 120 % |
| Wege | **Feste Wege je Karte**; der Spieler wählt nur, wo er baut (Land/Wasser, Hindernisse, Bauflächen) |
| Komfort | Schnellvorlauf, Auto-Start, Zielmodi (Erste/Letzte/Nah/Stark), aktive Fähigkeiten mit Abklingzeit, ein Held mit Stufen, Dauerfortschritt (Monkey Knowledge) |

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
- Kills geben **Level-Credits** (Kriecher 2, Brecher 6, Golem 15 usw.), zusätzlich Bonus am Rundenende; die Credits gelten nur in der Arena und werden am Levelende in echte Credits umgerechnet (z. B. 25 %), damit die Fabrik-Wirtschaft nicht aus dem Gleichgewicht gerät.
- Turm bauen kostet **Level-Credits statt Material** *oder* beides (Entscheidung unten). Verkauf gibt 70 % zurück.

**TD-C: Upgrade-Pfade**
- Jeder Turm bekommt **3 Pfade × 4 Stufen** (statt linear I–V) mit Kreuzpfad-Regel 4/2/0; Beispiele: Armbrust (Mehrfachschuss / Durchschlag / Tarnerkennung), Geschütz (Feuerrate / Uran-Munition / Reichweite), Flamme (Breite / Brandschaden / Kältedüse), Laser (Durchdringung / Schaden / Kettenblitz), Tesla (Ketten / Stun / Energiesparen).
- Kosten in Level-Credits, eskalierend (etwa 100 → 400 → 1.500 → 6.000), Fabrik-Materialien nur für die Turm-Basis.

**TD-D: Gegner-Eigenschaften und Gegenmittel**
- Eigenschaften **Tarnung** (nur Türme mit Erkennung oder Laser sehen sie), **Regeneration**, **Panzerung**, **gepanzert/fortified**; Brut-Gegner, die beim Tod kleinere freigeben (Brutmutter → Kriecher, wie Keramik → Regenbogen).
- Runden aus **festen Skripten** (Gruppen mit Typ, Anzahl, Abstand, Flags) statt der reinen Formel; Level 10/20/30/40/50 bleiben Boss-Level.

**TD-E: Komfort**
- Schnellvorlauf, aktive Fähigkeit je Turm-Pfad (Stufe 4), Turm-Verkauf, Zielmodi um „Stark" und „Nah" erweitern.

## 5. Entscheidungen, die ich von dir brauche

1. **Persistenz:** Karte 10 Level stehen lassen (TD-A)? Oder sogar ein Durchgang bis Level 50 am Stück?
2. **Weg:** fest vorgegeben (BTD6) oder weiter selbst legen? Mein Vorschlag: fest, der Stab entfällt.
3. **Turmkosten:** nur Level-Credits (BTD6-Gefühl, Fabrik liefert nur Munition und Strom), nur Material (Fabrik-Gefühl wie jetzt) oder beides?
4. **Umfang:** Alle Stufen A–E nacheinander, oder erst A und B testen? Mein Vorschlag: A + B, Spieltest, dann C und D.
5. **Verknüpfung mit der Forschung:** Die Siegel bei Level 5/10/20/30/40/50 bleiben Pflicht für neue Pakete; dann dürfen Level nicht zu schwer werden, sonst blockiert die Verteidigung den Forschungsfortschritt (Op-Befehl `/craftorio arena clear` gibt es als Notausgang).
