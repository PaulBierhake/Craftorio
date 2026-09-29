package de.craftorio.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.craftorio.defense.TdEnemy;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Enemies reuse vanilla models and textures until they get their own (GeckoLib) models. */
public final class TdEnemyRenderer extends MobRenderer<TdEnemy, EntityModel<TdEnemy>> {
    private final ResourceLocation texture;
    private final float scale;

    private TdEnemyRenderer(EntityRendererProvider.Context context, EntityModel<TdEnemy> model, float shadow, ResourceLocation texture, float scale) {
        super(context, model, shadow * scale);
        this.texture = texture;
        this.scale = scale;
    }

    public static TdEnemyRenderer crawler(EntityRendererProvider.Context context) {
        return new TdEnemyRenderer(context, new SpiderModel<>(context.bakeLayer(ModelLayers.CAVE_SPIDER)), 0.7F,
                ResourceLocation.withDefaultNamespace("textures/entity/spider/cave_spider.png"), 0.8F);
    }

    public static TdEnemyRenderer breaker(EntityRendererProvider.Context context) {
        return new TdEnemyRenderer(context, new HoglinModel<>(context.bakeLayer(ModelLayers.ZOGLIN)), 0.7F,
                ResourceLocation.withDefaultNamespace("textures/entity/hoglin/zoglin.png"), 0.7F);
    }

    public static TdEnemyRenderer spitter(EntityRendererProvider.Context context) {
        return new TdEnemyRenderer(context, new BlazeModel<>(context.bakeLayer(ModelLayers.BLAZE)), 0.5F,
                ResourceLocation.withDefaultNamespace("textures/entity/blaze.png"), 0.9F);
    }

    public static TdEnemyRenderer broodMother(EntityRendererProvider.Context context) {
        return new TdEnemyRenderer(context, new HoglinModel<>(context.bakeLayer(ModelLayers.HOGLIN)), 0.9F,
                ResourceLocation.withDefaultNamespace("textures/entity/hoglin/hoglin.png"), 1.3F);
    }

    public static TdEnemyRenderer crystalGolem(EntityRendererProvider.Context context) {
        return new TdEnemyRenderer(context, new PiglinModel<>(context.bakeLayer(ModelLayers.PIGLIN_BRUTE)), 0.6F,
                ResourceLocation.withDefaultNamespace("textures/entity/piglin/piglin_brute.png"), 1.15F);
    }

    public static TdEnemyRenderer behemoth(EntityRendererProvider.Context context) {
        return new TdEnemyRenderer(context, new PiglinModel<>(context.bakeLayer(ModelLayers.ZOMBIFIED_PIGLIN)), 0.9F,
                ResourceLocation.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png"), 1.7F);
    }

    public static TdEnemyRenderer swarmQueen(EntityRendererProvider.Context context) {
        return new TdEnemyRenderer(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 1.2F,
                ResourceLocation.withDefaultNamespace("textures/entity/spider/spider.png"), 2.4F);
    }

    @Override
    protected void scale(TdEnemy enemy, PoseStack pose, float partialTick) {
        pose.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(TdEnemy enemy) {
        return texture;
    }
}
