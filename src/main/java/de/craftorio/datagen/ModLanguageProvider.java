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
            add(ModBlocks.TRADING_POST.get(), "Trading Post");

            add("craftorio.tooltip.sell_price", "Sell value: %s");
            add("craftorio.tooltip.sell_price_stack", "Sell value: %s (stack: %s)");
            add("craftorio.trading_post.sold", "Sold for %s");
            add("craftorio.trading_post.info", "Trading post of team %s – earned so far: %s");

            add("craftorio.command.credits", "Team %s has %s");
            add("craftorio.command.team.info", "Team %s – balance: %s – members: %s");
            add("craftorio.command.team.list_entry", "%s (%s members)");
            add("craftorio.command.team.created", "Team %s created.");
            add("craftorio.command.team.invited", "%s invited you to team %s.");
            add("craftorio.command.team.invite_sent", "Invitation sent to %s.");
            add("craftorio.command.team.joined", "You joined team %s.");
            add("craftorio.command.team.left", "You left your team and now play as team %s.");

            add("craftorio.team.error.name_taken", "A team named %s already exists.");
            add("craftorio.team.error.invalid_name", "Team names must be 1–%s characters: letters, digits, spaces, _ and -.");
            add("craftorio.team.error.no_team", "You are not in a team.");
            add("craftorio.team.error.already_member", "That player is already in this team.");
            add("craftorio.team.error.unknown_team", "There is no team named %s.");
            add("craftorio.team.error.not_invited", "You have not been invited to team %s.");
            add("craftorio.team.error.alone", "You are the only member of your team.");
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
            add(ModBlocks.TRADING_POST.get(), "Handelsposten");

            add("craftorio.tooltip.sell_price", "Verkaufswert: %s");
            add("craftorio.tooltip.sell_price_stack", "Verkaufswert: %s (Stapel: %s)");
            add("craftorio.trading_post.sold", "Verkauft für %s");
            add("craftorio.trading_post.info", "Handelsposten von Team %s – bisher eingenommen: %s");

            add("craftorio.command.credits", "Team %s hat %s");
            add("craftorio.command.team.info", "Team %s – Kontostand: %s – Mitglieder: %s");
            add("craftorio.command.team.list_entry", "%s (%s Mitglieder)");
            add("craftorio.command.team.created", "Team %s gegründet.");
            add("craftorio.command.team.invited", "%s hat dich in Team %s eingeladen.");
            add("craftorio.command.team.invite_sent", "Einladung an %s gesendet.");
            add("craftorio.command.team.joined", "Du bist Team %s beigetreten.");
            add("craftorio.command.team.left", "Du hast dein Team verlassen und spielst jetzt als Team %s.");

            add("craftorio.team.error.name_taken", "Ein Team namens %s existiert bereits.");
            add("craftorio.team.error.invalid_name", "Teamnamen müssen 1–%s Zeichen lang sein: Buchstaben, Ziffern, Leerzeichen, _ und -.");
            add("craftorio.team.error.no_team", "Du bist in keinem Team.");
            add("craftorio.team.error.already_member", "Dieser Spieler ist bereits in diesem Team.");
            add("craftorio.team.error.unknown_team", "Es gibt kein Team namens %s.");
            add("craftorio.team.error.not_invited", "Du wurdest nicht in Team %s eingeladen.");
            add("craftorio.team.error.alone", "Du bist das einzige Mitglied deines Teams.");
        }
    }
}
