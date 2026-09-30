package de.craftorio.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.craftorio.Craftorio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * Draws the enemies of the tower defense as coloured blocks along the path: there are no entities, so thousands of
 * them cost next to nothing. Size and colour tell the kind, transparency camouflage, a pink cap regrowing and a dark
 * frame fortification.
 */
@EventBusSubscriber(modid = Craftorio.MOD_ID, value = Dist.CLIENT)
public final class TdEnemyRenderer {
    private static final double MAX_DISTANCE = 160;

    /** Edge length in blocks and colour per enemy index (see EnemyDefs.IDS). */
    private static final float[] SIZE = {0.35F, 0.4F, 0.45F, 0.5F, 0.55F, 0.55F, 0.55F, 0.55F, 0.7F, 0.6F, 0.65F, 0.9F, 1.6F, 2.2F, 3.0F, 1.3F, 3.6F};
    private static final float[][] COLOR = {
            {0.95F, 0.15F, 0.15F}, {0.2F, 0.4F, 1F}, {0.2F, 0.85F, 0.25F}, {1F, 0.9F, 0.2F}, {1F, 0.5F, 0.75F},
            {0.12F, 0.12F, 0.14F}, {0.9F, 0.95F, 1F}, {0.6F, 0.2F, 0.85F}, {0.42F, 0.45F, 0.5F}, {0.55F, 0.55F, 0.55F},
            {1F, 0.5F, 0.5F}, {0.9F, 0.6F, 0.4F}, {0.25F, 0.4F, 0.75F}, {0.85F, 0.2F, 0.2F}, {0.4F, 0.85F, 0.4F},
            {0.08F, 0.08F, 0.12F}, {0.5F, 0.1F, 0.6F}};

    private TdEnemyRenderer() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        double time = minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true);
        List<ClientTdEnemies.View> views = ClientTdEnemies.views(time);
        if (views.isEmpty()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.debugFilledBox());
        double limit = MAX_DISTANCE * MAX_DISTANCE;
        for (ClientTdEnemies.View view : views) {
            double dx = view.x() - camera.x;
            double dy = view.y() - camera.y;
            double dz = view.z() - camera.z;
            if (dx * dx + dy * dy + dz * dz > limit) {
                continue;
            }
            int index = view.def().index();
            float size = SIZE[Math.floorMod(index, SIZE.length)];
            float[] color = COLOR[Math.floorMod(index, COLOR.length)];
            float r = color[0];
            float g = color[1];
            float b = color[2];
            if (index == 10) { // shimmer crawlers shift through the colours of the rainbow
                float hue = (float) ((time * 0.02 + view.id() * 0.13) % 1.0);
                int rgb = Mth.hsvToRgb(hue, 0.6F, 1F);
                r = (rgb >> 16 & 255) / 255F;
                g = (rgb >> 8 & 255) / 255F;
                b = (rgb & 255) / 255F;
            }
            float alpha = (view.flags() & 1) != 0 ? 0.4F : 1F;
            double half = size / 2;
            double bottom = dy + 0.05;
            LevelRenderer.addChainedFilledBoxVertices(pose, consumer, dx - half, bottom, dz - half, dx + half, bottom + size, dz + half, r, g, b, alpha);
            if ((view.flags() & 4) != 0) { // fortified: a dark band around the middle
                double band = half + 0.04;
                LevelRenderer.addChainedFilledBoxVertices(pose, consumer, dx - band, bottom + size * 0.4, dz - band, dx + band, bottom + size * 0.6, dz + band,
                        0.1F, 0.1F, 0.1F, alpha);
            }
            if ((view.flags() & 2) != 0) { // regrowing: a pink cap
                double cap = Math.max(0.1, size * 0.3) / 2;
                LevelRenderer.addChainedFilledBoxVertices(pose, consumer, dx - cap, bottom + size, dz - cap, dx + cap, bottom + size + cap * 2, dz + cap,
                        1F, 0.3F, 0.7F, alpha);
            }
        }
        buffers.endBatch(RenderType.debugFilledBox());
    }
}
