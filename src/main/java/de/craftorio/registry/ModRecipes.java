package de.craftorio.registry;

import com.mojang.serialization.MapCodec;
import de.craftorio.Craftorio;
import de.craftorio.recipe.MachineRecipe;
import de.craftorio.recipe.MachineRecipeKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Craftorio.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Craftorio.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> SMELTING =
            TYPES.register("smelting", () -> RecipeType.simple(Craftorio.id("smelting")));
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> ASSEMBLING =
            TYPES.register("assembling", () -> RecipeType.simple(Craftorio.id("assembling")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>> SMELTING_SERIALIZER =
            SERIALIZERS.register("smelting", () -> serializer(MachineRecipeKind.SMELTING));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>> ASSEMBLING_SERIALIZER =
            SERIALIZERS.register("assembling", () -> serializer(MachineRecipeKind.ASSEMBLING));

    private ModRecipes() {
    }

    private static RecipeSerializer<MachineRecipe> serializer(MachineRecipeKind kind) {
        MapCodec<MachineRecipe> codec = MachineRecipe.codec(kind);
        StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec = MachineRecipe.streamCodec(kind);
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<MachineRecipe> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
                return streamCodec;
            }
        };
    }
}
