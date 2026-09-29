package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.defense.EnemyType;
import de.craftorio.defense.TdEnemy;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

@EventBusSubscriber(modid = Craftorio.MOD_ID)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Craftorio.MOD_ID);
    private static final Map<EnemyType, DeferredHolder<EntityType<?>, EntityType<TdEnemy>>> BY_TYPE = new EnumMap<>(EnemyType.class);

    public static final DeferredHolder<EntityType<?>, EntityType<TdEnemy>> CRAWLER = enemy("crawler", EnemyType.CRAWLER, 0.7F, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<TdEnemy>> BREAKER = enemy("breaker", EnemyType.BREAKER, 1.0F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<TdEnemy>> SPITTER = enemy("spitter", EnemyType.SPITTER, 0.6F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<TdEnemy>> BROOD_MOTHER = enemy("brood_mother", EnemyType.BROOD_MOTHER, 1.8F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<TdEnemy>> CRYSTAL_GOLEM = enemy("crystal_golem", EnemyType.CRYSTAL_GOLEM, 0.8F, 2.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<TdEnemy>> BEHEMOTH = enemy("behemoth", EnemyType.BEHEMOTH, 1.3F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<TdEnemy>> SWARM_QUEEN = enemy("swarm_queen", EnemyType.SWARM_QUEEN, 2.6F, 1.8F);

    private ModEntities() {
    }

    private static DeferredHolder<EntityType<?>, EntityType<TdEnemy>> enemy(String name, EnemyType type, float width, float height) {
        DeferredHolder<EntityType<?>, EntityType<TdEnemy>> holder = ENTITIES.register(name, () -> EntityType.Builder
                .<TdEnemy>of(TdEnemy::new, MobCategory.MISC)
                .sized(width, height)
                .clientTrackingRange(10)
                .updateInterval(2)
                .build(Craftorio.id(name).toString()));
        BY_TYPE.put(type, holder);
        return holder;
    }

    public static EntityType<TdEnemy> entityType(EnemyType type) {
        return BY_TYPE.get(type).get();
    }

    public static EnemyType enemyType(EntityType<?> entityType) {
        for (Map.Entry<EnemyType, DeferredHolder<EntityType<?>, EntityType<TdEnemy>>> entry : BY_TYPE.entrySet()) {
            if (entry.getValue().get() == entityType) {
                return entry.getKey();
            }
        }
        return EnemyType.CRAWLER;
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        BY_TYPE.values().forEach(holder -> event.put(holder.get(), TdEnemy.createAttributes().build()));
    }
}
