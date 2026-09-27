package de.craftorio.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.craftorio.logistics.BeltLane;
import de.craftorio.logistics.ConveyorBeltBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
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

    private final ItemRenderer itemRenderer;

    public ConveyorBeltRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ConveyorBeltBlockEntity belt, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = belt.facing();
        Direction left = facing.getCounterClockWise();
        for (int lane = 0; lane < 2; lane++) {
            double sideways = lane == ConveyorBeltBlockEntity.LEFT ? LANE_OFFSET : -LANE_OFFSET;
            for (BeltLane.Entry<ItemStack> entry : belt.lane(lane).entries()) {
                double along = entry.renderProgress(partialTick) - 0.5;
                pose.pushPose();
                pose.translate(
                        0.5 + facing.getStepX() * along + left.getStepX() * sideways,
                        ITEM_HEIGHT,
                        0.5 + facing.getStepZ() * along + left.getStepZ() * sideways);
                pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                itemRenderer.renderStatic(entry.item(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, belt.getLevel(), 0);
                pose.popPose();
            }
        }
    }
}
