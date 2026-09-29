package de.craftorio.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.craftorio.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * A research, loaded from {@code data/<namespace>/craftorio/research/*.json} and synced to clients. Labs turn science
 * packs into units; a research is finished after {@link #units} units, each taking {@link #seconds} seconds in one lab
 * and one pack of every kind in {@link #packs}. Finished researches unlock blueprints (workbench recipes) and machine
 * recipes listed in {@link #unlocks}.
 *
 * @param unlocks     ids of the blueprints and machine recipes this research makes available
 * @param unlockItems key items (arena seals) handed in once when the research is queued for the first time
 * @param requires    researches that must be finished first
 */
public record Research(long units, List<Pack> packs, int seconds, List<ResourceLocation> requires,
                       List<ResourceLocation> unlocks, List<SizedIngredient> unlockItems, int order) {
    public static final int MAX_PACKS = 4;

    /** The four science pack kinds of the first stage, in the order they are shown. */
    public enum Pack implements StringRepresentable {
        RED("red", 'R'),
        GREEN("green", 'G'),
        MILITARY("military", 'M'),
        BLUE("blue", 'B');

        public static final Codec<Pack> CODEC = StringRepresentable.fromEnum(Pack::values);

        private final String key;
        private final char letter;

        Pack(String key, char letter) {
            this.key = key;
            this.letter = letter;
        }

        @Override
        public String getSerializedName() {
            return key;
        }

        /** Short label used in cost lines: R, G, M, B. */
        public char letter() {
            return letter;
        }

        public Item item() {
            return switch (this) {
                case RED -> ModItems.RED_SCIENCE.get();
                case GREEN -> ModItems.GREEN_SCIENCE.get();
                case MILITARY -> ModItems.MILITARY_SCIENCE.get();
                case BLUE -> ModItems.BLUE_SCIENCE.get();
            };
        }
    }

    public static final Codec<Research> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("units").forGetter(Research::units),
            Pack.CODEC.listOf(1, MAX_PACKS).fieldOf("packs").forGetter(Research::packs),
            Codec.intRange(1, 3600).fieldOf("seconds").forGetter(Research::seconds),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Research::requires),
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocks", List.of()).forGetter(Research::unlocks),
            SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("unlock_items", List.of()).forGetter(Research::unlockItems),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Research::order)
    ).apply(instance, Research::new));

    /** Units this research costs with the {@code pacing.researchCost} factor applied (at least one). */
    public long units(double costFactor) {
        return Math.max(1, Math.round(units * costFactor));
    }

    /** Cost line such as "100 × R+G, 30 s". */
    public String costLine(double costFactor) {
        StringBuilder packs = new StringBuilder();
        for (Pack pack : this.packs) {
            if (!packs.isEmpty()) {
                packs.append('+');
            }
            packs.append(pack.letter());
        }
        return units(costFactor) + " × " + packs + ", " + seconds + " s";
    }
}
