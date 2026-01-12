package com.skys.cobblemoncosmetics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer for the Iron Maiden Mega Bracelet
 * Positions the broom-styled bracelet in the player's off-hand
 */
public class IronMaidenMegaBraceletRenderer implements AccessoryRenderer {
    @Override
    public <M extends LivingEntity> void render(
            ItemStack stack,
            SlotReference reference,
            PoseStack poseStack,
            EntityModel<M> model,
            MultiBufferSource multiBufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        LivingEntity entity = reference.entity();

        // Hide bracelet if player is holding one in hand (to avoid clipping)
        ItemStack handStack = entity.getItemInHand(entity.getUsedItemHand());
        if (handStack.getItem() instanceof com.skys.cobblemoncosmetics.items.IronMaidenMegaBracelet) {
            return;
        }

        poseStack.pushPose();

        // Determine which arm is the offhand
        HumanoidArm offHandArm = (entity.getMainArm() == HumanoidArm.RIGHT) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;

        if (model instanceof ArmedModel armedModel) {
            // Position at the hand
            armedModel.translateToHand(offHandArm, poseStack);

            // Rotate to position correctly on wrist
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            // Fine-tune position based on which hand
            if (offHandArm == HumanoidArm.RIGHT) {
                poseStack.translate(1.0F / 16.0F, 1.0F / 8.0F, -10.0F / 16.0F);
            } else {
                poseStack.translate(-1.0F / 16.0F, 1.0F / 8.0F, -10.0F / 16.0F);
            }
        }

        // Use the correct display context based on which hand is the offhand
        ItemDisplayContext context = (offHandArm == HumanoidArm.LEFT)
                ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

        // Render the 3D model using the same signature as SheathBeltRenderer
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                context,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                multiBufferSource,
                entity.level(),
                0
        );

        poseStack.popPose();
    }
}
