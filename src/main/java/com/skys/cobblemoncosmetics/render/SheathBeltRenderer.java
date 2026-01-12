package com.skys.cobblemoncosmetics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * TEST CLASS - Renderer for sheathed items on belt/waist
 * Positions the item vertically on the player's side like a sheathed sword
 *
 * REMOVE THIS FILE when testing is complete
 */
public class SheathBeltRenderer implements AccessoryRenderer {
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

        // Check for armor to adjust positioning
        ItemStack chestArmor = entity.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggingArmor = entity.getItemBySlot(EquipmentSlot.LEGS);

        // Adjust z-axis (depth) based on armor
        float zOffset = -0.25f; // Base offset from body
        if (!chestArmor.isEmpty() || !leggingArmor.isEmpty()) {
            zOffset = -0.32f; // Push further out if wearing armor
        }

        poseStack.pushPose();

        // Position on left hip/waist area
        // X: negative = left side, Y: down from center, Z: forward from body
        poseStack.translate(-0.35f, 0.45f, zOffset);

        // Rotate to hang vertically like a sheathed sword
        poseStack.mulPose(Axis.YP.rotationDegrees(90));  // Face forward
        poseStack.mulPose(Axis.ZP.rotationDegrees(10));  // Slight tilt outward

        // Scale to make it look like a proper sheathed item
        poseStack.scale(0.5f, 0.5f, 0.5f);

        // Adjust for crouching
        if (entity.isCrouching()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-25)); // Tilt with crouch
            poseStack.translate(0f, -0.3f, 0.2f);
        }

        // Render the item using third-person display context
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, // Use third-person for better angles
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
