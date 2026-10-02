package de.craftorio;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side settings; stored in config/craftorio-client.toml. */
public final class CraftorioClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PRESELECT_WORLD_TYPE = BUILDER
            .comment("Preselect the Craftorio world type in the create-world screen.")
            .define("world.preselectWorldType", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CraftorioClientConfig() {
    }

    public static boolean preselectWorldType() {
        try {
            return PRESELECT_WORLD_TYPE.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
