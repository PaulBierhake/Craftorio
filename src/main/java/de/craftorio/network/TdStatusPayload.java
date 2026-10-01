package de.craftorio.network;

import de.craftorio.Craftorio;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client, once a second: the team's tower defense status for the HUD and the terminal.
 *
 * @param coins       arena coins the team has
 * @param preview     the next round as pairs (enemy code, count), see LevelPlan#previewCode
 * @param canCallWave the next round can be started early right now
 * @param unsupplied  towers in the arena that can not fire because neither they nor the arena reserve hold ammunition or energy
 * @param difficulty  ordinal of the team's difficulty
 * @param maxLives    lives at the start of a level on this difficulty
 * @param warChestLeft credits that can still be exchanged for coins in this level
 * @param campaignStarted a level has been started, so the difficulty can only be made easier
 * @param challenge   ordinal of the challenge chosen for the next level
 * @param masters     bit mask of the challenges the team has mastered
 * @param bestRound   the highest round the team has completed
 */
public record TdStatusPayload(boolean hasZone, int level, boolean running, int wave, int waves, int lives,
                              int enemiesLeft, boolean auto, long coins, int theme, int challenge, int lastStars,
                              List<Integer> preview, long energy, int bolts, int cartridges, boolean canCallWave, int unsupplied,
                              int difficulty, int maxLives, long warChestLeft, boolean campaignStarted, int masters, int bestRound)
        implements CustomPacketPayload {
    public static final TdStatusPayload NONE = new TdStatusPayload(false, 1, false, 0, 0, 0, 0, false, 0, 0, 0, 0, List.of(), 0, 0, 0, false, 0,
            1, 150, 0, false, 0, 0);
    public static final Type<TdStatusPayload> TYPE = new Type<>(Craftorio.id("td_status"));

    public static final StreamCodec<ByteBuf, TdStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                FriendlyByteBuf out = new FriendlyByteBuf(buf);
                out.writeBoolean(payload.hasZone);
                out.writeVarInt(payload.level);
                out.writeBoolean(payload.running);
                out.writeVarInt(payload.wave);
                out.writeVarInt(payload.waves);
                out.writeVarInt(payload.lives);
                out.writeVarInt(payload.enemiesLeft);
                out.writeBoolean(payload.auto);
                out.writeVarLong(payload.coins);
                out.writeVarInt(payload.theme);
                out.writeVarInt(payload.challenge);
                out.writeVarInt(payload.lastStars);
                out.writeVarInt(payload.preview.size());
                payload.preview.forEach(out::writeVarInt);
                out.writeVarLong(payload.energy);
                out.writeVarInt(payload.bolts);
                out.writeVarInt(payload.cartridges);
                out.writeBoolean(payload.canCallWave);
                out.writeVarInt(payload.unsupplied);
                out.writeVarInt(payload.difficulty);
                out.writeVarInt(payload.maxLives);
                out.writeVarLong(payload.warChestLeft);
                out.writeBoolean(payload.campaignStarted);
                out.writeVarInt(payload.masters);
                out.writeVarInt(payload.bestRound);
            },
            buf -> {
                FriendlyByteBuf in = new FriendlyByteBuf(buf);
                boolean hasZone = in.readBoolean();
                int level = in.readVarInt();
                boolean running = in.readBoolean();
                int wave = in.readVarInt();
                int waves = in.readVarInt();
                int lives = in.readVarInt();
                int enemiesLeft = in.readVarInt();
                boolean auto = in.readBoolean();
                long coins = in.readVarLong();
                int theme = in.readVarInt();
                int challenge = in.readVarInt();
                int lastStars = in.readVarInt();
                int size = in.readVarInt();
                List<Integer> preview = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    preview.add(in.readVarInt());
                }
                long energy = in.readVarLong();
                int bolts = in.readVarInt();
                int cartridges = in.readVarInt();
                boolean canCallWave = in.readBoolean();
                int unsupplied = in.readVarInt();
                int difficulty = in.readVarInt();
                int maxLives = in.readVarInt();
                long warChestLeft = in.readVarLong();
                boolean campaignStarted = in.readBoolean();
                int masters = in.readVarInt();
                int bestRound = in.readVarInt();
                return new TdStatusPayload(hasZone, level, running, wave, waves, lives, enemiesLeft, auto, coins, theme, challenge,
                        lastStars, List.copyOf(preview), energy, bolts, cartridges, canCallWave, unsupplied, difficulty, maxLives,
                        warChestLeft, campaignStarted, masters, bestRound);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
