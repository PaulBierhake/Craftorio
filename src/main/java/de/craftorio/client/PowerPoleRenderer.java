package de.craftorio.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.craftorio.energy.PowerGrid;
import de.craftorio.energy.PowerPoleBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Sagging copper wires between connected poles. */
public final class PowerPoleRenderer implements BlockEntityRenderer<PowerPoleBlockEntity> {
    private static final double ATTACH_HEIGHT = 16.25 / 16.0;
    private static final int SEGMENTS = 12;
    private static final double SAG = 0.35;

    public PowerPoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PowerPoleBlockEntity pole, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose last = pose.last();
        for (BlockPos other : pole.wires()) {
            Vec3 delta = Vec3.atLowerCornerOf(other.subtract(pole.getBlockPos()));
            Vec3 previous = new Vec3(0.5, ATTACH_HEIGHT, 0.5);
            for (int i = 1; i <= SEGMENTS; i++) {
                double t = (double) i / SEGMENTS;
                Vec3 point = new Vec3(0.5 + delta.x * t, ATTACH_HEIGHT + delta.y * t - SAG * 4 * t * (1 - t), 0.5 + delta.z * t);
                Vec3 normal = point.subtract(previous).normalize();
                lines.addVertex(last, (float) previous.x, (float) previous.y, (float) previous.z)
                        .setColor(0.72F, 0.42F, 0.2F, 1F).setNormal(last, (float) normal.x, (float) normal.y, (float) normal.z);
                lines.addVertex(last, (float) point.x, (float) point.y, (float) point.z)
                        .setColor(0.72F, 0.42F, 0.2F, 1F).setNormal(last, (float) normal.x, (float) normal.y, (float) normal.z);
                previous = point;
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(PowerPoleBlockEntity pole) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(PowerPoleBlockEntity pole) {
        return new AABB(pole.getBlockPos()).inflate(PowerGrid.WIRE_RANGE + 1);
    }
}
