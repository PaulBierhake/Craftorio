package de.craftorio.registry;

import com.mojang.serialization.MapCodec;
import de.craftorio.Craftorio;
import de.craftorio.world.terrain.FactoryChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> TYPES =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Craftorio.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<FactoryChunkGenerator>> FACTORY =
            TYPES.register("factory", () -> FactoryChunkGenerator.CODEC);

    private ModChunkGenerators() {
    }
}
