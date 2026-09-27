# Craftorio

Factorio-artige Automatisierung, eine Credits-Wirtschaft und Tower Defense für Minecraft.
Das vollständige Spielkonzept steht in [docs/KONZEPT.md](docs/KONZEPT.md).

- Minecraft **1.21.1**, **NeoForge**, Java **21**
- Build-System: Gradle mit [ModDevGradle](https://github.com/neoforged/ModDevGradle)

## Entwicklung

```bash
./gradlew runData      # Datengeneratoren: Modelle, Blockstates, Tags, Übersetzungen -> src/generated/resources
./gradlew runClient    # Minecraft-Client mit der Mod starten
./gradlew runServer    # Dedizierten Server starten
./gradlew build        # Mod-JAR bauen -> build/libs/
```

`runData` muss einmal nach dem Klonen und nach jeder Änderung an den Datengeneratoren
(`de.craftorio.datagen`) laufen. Die generierten Dateien werden nicht eingecheckt.

IntelliJ IDEA: Projekt als Gradle-Projekt öffnen; die Run-Konfigurationen werden beim Sync erzeugt.

## Projektstruktur

```
src/main/java/de/craftorio/
├── Craftorio.java          Mod-Einstiegspunkt
├── CraftorioConfig.java    Server-Konfiguration
├── registry/               Blöcke, Items, Creative-Tab
└── datagen/                Datengeneratoren
src/main/templates/         neoforge.mods.toml (wird aus gradle.properties befüllt)
src/main/resources/         Handgemachte Assets (Texturen)
```
