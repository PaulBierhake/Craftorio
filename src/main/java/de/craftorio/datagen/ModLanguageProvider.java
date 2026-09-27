package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class ModLanguageProvider {
    private ModLanguageProvider() {
    }

    public static final class English extends LanguageProvider {
        public English(PackOutput output) {
            super(output, Craftorio.MOD_ID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add("itemGroup." + Craftorio.MOD_ID, "Craftorio");
            add(ModBlocks.CAP_ROCK.get(), "Cap Rock");
        }
    }

    public static final class German extends LanguageProvider {
        public German(PackOutput output) {
            super(output, Craftorio.MOD_ID, "de_de");
        }

        @Override
        protected void addTranslations() {
            add("itemGroup." + Craftorio.MOD_ID, "Craftorio");
            add(ModBlocks.CAP_ROCK.get(), "Deckgestein");
        }
    }
}
