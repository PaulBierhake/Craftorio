package de.craftorio.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.craftorio.Craftorio;
import de.craftorio.economy.Credits;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamException;
import de.craftorio.team.TeamRegistry;
import de.craftorio.world.cave.CaveAreas;
import de.craftorio.world.cave.Layer;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** {@code /craftorio team ...}, {@code /craftorio credits ...} and the admin command {@code /craftorio layer unlock ...}. */
@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class CraftorioCommands {
    private CraftorioCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal(Craftorio.MOD_ID)
                .then(literal("credits")
                        .executes(ctx -> run(ctx, CraftorioCommands::showCredits))
                        .then(literal("add").requires(source -> source.hasPermission(2))
                                .then(argument("player", EntityArgument.player())
                                        .then(argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> run(ctx, c -> changeCredits(c, false))))))
                        .then(literal("set").requires(source -> source.hasPermission(2))
                                .then(argument("player", EntityArgument.player())
                                        .then(argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> run(ctx, c -> changeCredits(c, true)))))))
                .then(literal("team")
                        .executes(ctx -> run(ctx, CraftorioCommands::teamInfo))
                        .then(literal("info").executes(ctx -> run(ctx, CraftorioCommands::teamInfo)))
                        .then(literal("list").executes(ctx -> run(ctx, CraftorioCommands::teamList)))
                        .then(literal("create")
                                .then(argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> run(ctx, CraftorioCommands::teamCreate))))
                        .then(literal("invite")
                                .then(argument("player", EntityArgument.player())
                                        .executes(ctx -> run(ctx, CraftorioCommands::teamInvite))))
                        .then(literal("join")
                                .then(argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> run(ctx, CraftorioCommands::teamJoin))))
                        .then(literal("leave").executes(ctx -> run(ctx, CraftorioCommands::teamLeave)))
                        .then(literal("kick")
                                .then(argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> run(ctx, CraftorioCommands::teamKick))))
                        .then(literal("leader")
                                .then(argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> run(ctx, CraftorioCommands::teamLeader))))
                        .then(literal("spending")
                                .then(literal("all").executes(ctx -> run(ctx, c -> teamSpending(c, true))))
                                .then(literal("leader").executes(ctx -> run(ctx, c -> teamSpending(c, false))))))
                // Admin shortcut for tests: lays the shortest possible path in the own arena.
                .then(literal("arena").requires(source -> source.hasPermission(2))
                        .then(literal("route").executes(ctx -> run(ctx, CraftorioCommands::arenaRoute))))
                // Admin shortcut (tests, repairing old worlds): unlocks a layer area without building an entrance.
                .then(literal("layer").requires(source -> source.hasPermission(2))
                        .then(literal("unlock")
                                .then(literal("caves").executes(ctx -> unlockLayer(ctx, Layer.CAVES)))
                                .then(literal("mines").executes(ctx -> unlockLayer(ctx, Layer.MINES))))));
    }

    private static int arenaRoute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var defense = de.craftorio.defense.TowerDefense.get(player.server);
        var zone = defense.ensureArena(player.server, teamOf(player).id());
        var arena = de.craftorio.defense.TowerDefense.arena(player.server);
        int placed = 0;
        for (int[] tile : defense.layout(zone).route()) {
            arena.setBlock(de.craftorio.defense.arena.Arenas.field(zone.slot(), tile[0], tile[1], de.craftorio.defense.arena.Arenas.BUILD_Y),
                    de.craftorio.registry.ModBlocks.PATH_BLOCK.get().defaultBlockState(), 3);
            placed++;
        }
        int count = placed;
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.td.path_ok", count), false);
        return placed;
    }

    private static int unlockLayer(CommandContext<CommandSourceStack> ctx, Layer layer) {
        BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
        int added = CaveAreas.get(ctx.getSource().getServer()).unlockAround(layer, pos);
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.layer_unlocked", added,
                layer.minY(), layer.maxY()), true);
        return added;
    }

    private static int showCredits(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Team team = teamOf(ctx.getSource().getPlayerOrException());
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.credits",
                team.name(), Credits.format(team.balance())), false);
        return 1;
    }

    private static int changeCredits(CommandContext<CommandSourceStack> ctx, boolean set) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        long amount = LongArgumentType.getLong(ctx, "amount");
        Team team = teamOf(target);
        TeamRegistry registry = registry(ctx);
        if (set) {
            registry.setBalance(team.id(), amount);
        } else {
            registry.deposit(team.id(), amount);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.credits",
                team.name(), Credits.format(team.balance())), true);
        return 1;
    }

    private static int teamInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Team team = teamOf(ctx.getSource().getPlayerOrException());
        MinecraftServer server = ctx.getSource().getServer();
        String members = team.members().stream().map(id -> playerName(server, id)).collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.team.info",
                team.name(), Credits.format(team.balance()), members), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.team.rights",
                playerName(server, team.leader()), Component.translatable(team.membersCanSpend()
                        ? "craftorio.command.team.spending.all" : "craftorio.command.team.spending.leader")), false);
        return 1;
    }

    private static int teamKick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        GameProfile target = single(ctx);
        registry(ctx).kick(player.getUUID(), target.getId(), target.getName());
        ServerPlayer online = ctx.getSource().getServer().getPlayerList().getPlayer(target.getId());
        if (online != null) {
            online.sendSystemMessage(Component.translatable("craftorio.command.team.kicked_you", teamOf(player).name())
                    .withStyle(ChatFormatting.RED));
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.team.kicked", target.getName()), false);
        return 1;
    }

    private static int teamLeader(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        GameProfile target = single(ctx);
        registry(ctx).transferLeadership(player.getUUID(), target.getId());
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.team.new_leader", target.getName()), false);
        return 1;
    }

    private static int teamSpending(CommandContext<CommandSourceStack> ctx, boolean all) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        registry(ctx).setMembersCanSpend(player.getUUID(), all);
        ctx.getSource().sendSuccess(() -> Component.translatable(all
                ? "craftorio.command.team.spending.set_all" : "craftorio.command.team.spending.set_leader"), false);
        return 1;
    }

    private static GameProfile single(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var profiles = GameProfileArgument.getGameProfiles(ctx, "player");
        if (profiles.size() != 1) {
            throw new TeamException("craftorio.team.error.one_player");
        }
        return profiles.iterator().next();
    }

    private static int teamList(CommandContext<CommandSourceStack> ctx) {
        for (Team team : registry(ctx).teams()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.team.list_entry",
                    team.name(), team.members().size()), false);
        }
        return registry(ctx).teams().size();
    }

    private static int teamCreate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Team team = registry(ctx).create(player.getUUID(), StringArgumentType.getString(ctx, "name"));
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.team.created", team.name()), false);
        return 1;
    }

    private static int teamInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        registry(ctx).invite(player.getUUID(), target.getUUID());
        Team team = teamOf(player);
        String joinCommand = "/" + Craftorio.MOD_ID + " team join " + team.name();
        target.sendSystemMessage(Component.translatable("craftorio.command.team.invited", player.getGameProfile().getName(), team.name())
                .append(" ")
                .append(Component.literal("[" + joinCommand + "]").withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, joinCommand)))));
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.team.invite_sent", target.getGameProfile().getName()), false);
        return 1;
    }

    private static int teamJoin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Team team = registry(ctx).join(player.getUUID(), StringArgumentType.getString(ctx, "name"));
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.team.joined", team.name()), false);
        return 1;
    }

    private static int teamLeave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Team team = registry(ctx).leave(player.getUUID(), player.getGameProfile().getName());
        ctx.getSource().sendSuccess(() -> Component.translatable("craftorio.command.team.left", team.name()), false);
        return 1;
    }

    private static TeamRegistry registry(CommandContext<CommandSourceStack> ctx) {
        return TeamData.registry(ctx.getSource().getServer());
    }

    private static Team teamOf(ServerPlayer player) {
        return TeamData.registry(player.server).ensureTeam(player.getUUID(), player.getGameProfile().getName());
    }

    private static String playerName(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        var cache = server.getProfileCache();
        return cache == null ? id.toString() : cache.get(id).map(profile -> profile.getName()).orElse(id.toString());
    }

    @FunctionalInterface
    private interface Action {
        int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    /** Turns rule violations from the team registry into a red chat message instead of a stack trace. */
    private static int run(CommandContext<CommandSourceStack> ctx, Action action) throws CommandSyntaxException {
        try {
            return action.run(ctx);
        } catch (TeamException e) {
            ctx.getSource().sendFailure(Component.translatable(e.translationKey(), e.args()));
            return 0;
        }
    }
}
