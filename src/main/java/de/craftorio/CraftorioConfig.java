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

    public static final ModConfigSpec.DoubleValue CRAFTING_SPEED = BUILDER
            .comment("Speed factor of all machines and smelting (1.0 = Factorio times, 2.0 = twice as fast). Belts are not affected.")
            .defineInRange("pacing.craftingSpeed", 1.0, 0.05, 100.0);

    public static final ModConfigSpec.DoubleValue MINING_SPEED = BUILDER
            .comment("Speed factor of all drills (1.0 = Factorio rates).")
            .defineInRange("pacing.miningSpeed", 1.0, 0.05, 100.0);

    public static final ModConfigSpec.DoubleValue RESEARCH_COST = BUILDER
            .comment("Factor on the number of science packs every research costs (1.0 = Factorio; used from the research system on).")
            .defineInRange("pacing.researchCost", 1.0, 0.05, 100.0);

    public static final ModConfigSpec.IntValue CHUNKLOADER_CHUNKS = BUILDER
            .comment("How many chunks with machines of a team are kept loaded so the factory keeps running. 0 turns the chunk loader off.")
            .defineInRange("chunkloader.chunksPerTeam", 64, 0, 1024);

    public static final ModConfigSpec.BooleanValue CHUNKLOADER_ONLINE_ONLY = BUILDER
            .comment("Only keep a team's factory loaded while at least one team member is online.")
            .define("chunkloader.onlyWhileOnline", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CraftorioConfig() {
    }

    public static double craftingSpeed() {
        return value(CRAFTING_SPEED, 1.0);
    }

    public static double miningSpeed() {
        return value(MINING_SPEED, 1.0);
    }

    public static double researchCost() {
        return value(RESEARCH_COST, 1.0);
    }

    /** A crafting or smelting time in ticks with the pacing factor applied (at least one tick). */
    public static int craftingTicks(int ticks) {
        return Pacing.scaledTicks(ticks, craftingSpeed());
    }

    /** Config values are only readable once the world's config is loaded; before that the default applies. */
    private static double value(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return fallback;
        }
    }
}
