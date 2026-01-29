package com.skys.cobblemoncosmetics.mixin;

import com.mojang.logging.LogUtils;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to make our custom potion items recognized as valid brewing stand containers in the GUI.
 * This allows our items to be placed in the bottom slots through the player inventory interface.
 *
 * Based on Cobblemon's PotionSlotMixin approach.
 */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public class BrewingStandMenuMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Inject at the head of mayPlace to allow our custom items in potion slots.
     * This is the instance method that Slot uses for validation.
     */
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true, remap = false)
    private void skyscobblemon$mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        LOGGER.info("[BrewingStandMenuMixin] mayPlace called - item: {}", stack.getItem());

        if (!stack.isEmpty()) {
            // Check if it's one of our custom potion items
            if (stack.is(ModItems.LIQUID_ASSIMILATION_POTION.get()) ||
                stack.is(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get()) ||
                stack.is(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION_EXTENDED.get()) ||
                stack.is(ModItems.CREEPING_LIQUID_ASSIMILATION_POTION.get()) ||
                stack.is(ModItems.CREEPING_LIQUID_ASSIMILATION_POTION_EXTENDED.get())) {
                LOGGER.info("[BrewingStandMenuMixin] Allowing our custom potion: {}", stack.getItem());
                cir.setReturnValue(true);
            }
        }
    }
}
