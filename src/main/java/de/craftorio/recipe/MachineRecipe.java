package de.craftorio.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/** Recipe for furnaces (counted smelting) and the assembling machine: counted ingredients, one result, the crafting time in ticks at speed 1 (Factorio seconds × 20). */
public record MachineRecipe(MachineRecipeKind kind, List<SizedIngredient> ingredients, ItemStack result, int time)
        implements Recipe<RecipeInput> {

    public static final int MAX_INGREDIENTS = 4;

    public static MapCodec<MachineRecipe> codec(MachineRecipeKind kind) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                SizedIngredient.FLAT_CODEC.listOf(1, MAX_INGREDIENTS).fieldOf("ingredients").forGetter(MachineRecipe::ingredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MachineRecipe::result),
                Codec.intRange(1, 72_000).optionalFieldOf("time", 40).forGetter(MachineRecipe::time)
        ).apply(instance, (ingredients, result, time) -> new MachineRecipe(kind, ingredients, result, time)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec(MachineRecipeKind kind) {
        return StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::ingredients,
                ItemStack.STREAM_CODEC, MachineRecipe::result,
                ByteBufCodecs.VAR_INT, MachineRecipe::time,
                (ingredients, result, time) -> new MachineRecipe(kind, ingredients, result, time));
    }

    /** True if the inputs hold enough of every ingredient (items may be spread over several slots). */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        for (SizedIngredient ingredient : ingredients) {
            int available = 0;
            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);
                if (ingredient.ingredient().test(stack)) {
                    available += stack.getCount();
                }
            }
            if (available < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return kind.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return kind.type();
    }

    @Override
    public boolean isSpecial() {
        return true; // keeps these out of the vanilla recipe book
    }
}
