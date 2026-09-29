package de.craftorio.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.craftorio.logistics.BeltLane;
import de.craftorio.logistics.ConveyorBeltBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the items lying on a belt, interpolated between ticks. */
public final class ConveyorBeltRenderer implements BlockEntityRenderer<ConveyorBeltBlockEntity> {
    private static final double LANE_OFFSET = 0.23;
    private static final double ITEM_HEIGHT = 3.2 / 16.0;
    private static final float ITEM_SCALE = 0.38F;

    private static final int FILTER_ALPHA = 150;
    private static final double FILTER_SHIFT = 0.27;

    private final ItemRenderer itemRenderer;

    public ConveyorBeltRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ConveyorBeltBlockEntity belt, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = belt.facing();
        Direction left = facing.getCounterClockWise();
        renderFilter(belt, pose, buffers, light);
        for (int lane = 0; lane < 2; lane++) {
            double sideways = lane == ConveyorBeltBlockEntity.LEFT ? LANE_OFFSET : -LANE_OFFSET;
            for (BeltLane.Entry<ItemStack> entry : belt.lane(lane).entries()) {
                float progress = entry.renderProgress(partialTick);
                double along = progress - 0.5;
                // On a slope the surface climbs (or falls) one block along the belt.
                double height = ITEM_HEIGHT + switch (belt.slope()) {
                    case FLAT -> 0.0;
                    case UP -> progress;
                    case DOWN -> 1.0 - progress;
                };
                pose.pushPose();
                pose.translate(
                        0.5 + facing.getStepX() * along + left.getStepX() * sideways,
                        height,
                        0.5 + facing.getStepZ() * along + left.getStepZ() * sideways);
                pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                itemRenderer.renderStatic(entry.item(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, belt.getLevel(), 0);
                pose.popPose();
            }
        }
    }

    /**
     * A splitter's filter item lies translucent on the block, on the side its items leave by: ahead of the middle for
     * "straight on", near the left or right edge otherwise.
     */
    private void renderFilter(ConveyorBeltBlockEntity belt, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemStack filter = belt.filter();
        if (filter.isEmpty()) {
            return;
        }
        Direction facing = belt.facing();
        double along = 0;
        double sideways = 0;
        switch (belt.filterOutput()) {
            case FRONT -> along = FILTER_SHIFT;
            case LEFT -> sideways = FILTER_SHIFT;
            case RIGHT -> sideways = -FILTER_SHIFT;
        }
        Direction left = facing.getCounterClockWise();
        pose.pushPose();
        pose.translate(0.5 + facing.getStepX() * along + left.getStepX() * sideways, ITEM_HEIGHT + 0.03,
                0.5 + facing.getStepZ() * along + left.getStepZ() * sideways);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.5F, 0.5F, 0.5F);
        itemRenderer.renderStatic(filter, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose,
                new TranslucentBuffers(buffers, FILTER_ALPHA), belt.getLevel(), 0);
        pose.popPose();
    }

    /** Draws everything with a fixed transparency in the block atlas' translucent render type. */
    private record TranslucentBuffers(MultiBufferSource delegate, int alpha) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return new Faded(delegate.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)), alpha);
        }
    }

    private record Faded(VertexConsumer delegate, int alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int a) {
            delegate.setColor(red, green, blue, a * alpha / 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
