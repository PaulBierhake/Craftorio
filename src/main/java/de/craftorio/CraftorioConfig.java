package de.craftorio;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side settings; stored per world in serverconfig/craftorio-server.toml. */
public final class CraftorioConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.LongValue STARTING_CREDITS = BUILDER
            .comment("Credits a newly created team starts with.")
            .defineInRange("economy.startingCredits", 0L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue STARTER_KIT = BUILDER
            .comment("Give every player a starter kit (construction workbench, iron, gold, chest, coal) with the handbook on the first login.")
            .define("economy.starterKit", true);

    public static final ModConfigSpec.BooleanValue PROTECTION = BUILDER
            .comment("Protect placed machines, containers and towers from players of other teams.")
            .define("protection.enabled", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CraftorioConfig() {
    }
}
