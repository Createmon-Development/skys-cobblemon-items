package com.skys.cobblemoncosmetics.mixin;

import com.mojang.logging.LogUtils;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to make our custom potion items recognized as valid brewing stand containers.
 * This allows our items to be placed in the bottom slots of the brewing stand and brewed.
 *
 * Based on Cobblemon's BrewingStandBlockEntityMixin approach.
 */
@Mixin(BrewingStandBlockEntity.class)
public class PotionBrewingMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Inject at the head of canPlaceItem to allow our custom items in the potion slots (0-2).
     * Slot 3 is the ingredient slot, slot 4 is the fuel slot.
     */
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void skyscobblemon$canPlaceItem(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        LOGGER.info("[PotionBrewingMixin] canPlaceItem called - slot: {}, item: {}", slot, stack.getItem());

        // Only intercept for potion slots (0-2)
        if (slot >= 0 && slot <= 2 && !stack.isEmpty()) {
            if (skyscobblemon$isOurPotionItem(stack)) {
                LOGGER.info("[PotionBrewingMixin] Allowing our custom potion in slot {}: {}", slot, stack.getItem());
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Inject at the head of isBrewable to allow brewing when our items are present.
     * This is the key fix - vanilla doesn't recognize our lingering items as brewable inputs.
     */
    @Inject(method = "isBrewable", at = @At("HEAD"), cancellable = true, remap = false)
    private static void skyscobblemon$isBrewable(PotionBrewing potionBrewing, NonNullList<ItemStack> items, CallbackInfoReturnable<Boolean> cir) {
        // items: slots 0-2 are potion slots, slot 3 is ingredient, slot 4 is fuel
        ItemStack ingredient = items.get(3);

        LOGGER.info("[PotionBrewingMixin] isBrewable called - ingredient: {}", ingredient.getItem());

        if (ingredient.isEmpty()) {
            return; // No ingredient, let vanilla handle it
        }

        // Check each potion slot (0-2) for our custom items
        for (int i = 0; i < 3; i++) {
            ItemStack potionSlot = items.get(i);
            if (!potionSlot.isEmpty() && skyscobblemon$hasValidRecipe(potionSlot, ingredient)) {
                LOGGER.info("[PotionBrewingMixin] isBrewable returning true - found valid recipe for {} + {}",
                    potionSlot.getItem(), ingredient.getItem());
                cir.setReturnValue(true);
                return;
            }
        }
    }

    /**
     * Check if this is one of our custom potion items.
     */
    @Unique
    private static boolean skyscobblemon$isOurPotionItem(ItemStack stack) {
        return stack.is(ModItems.LIQUID_ASSIMILATION_POTION.get()) ||
               stack.is(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get()) ||
               stack.is(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION_EXTENDED.get()) ||
               stack.is(ModItems.CREEPING_LIQUID_ASSIMILATION_POTION.get()) ||
               stack.is(ModItems.CREEPING_LIQUID_ASSIMILATION_POTION_EXTENDED.get());
    }

    /**
     * Check if there's a valid recipe for the given input + ingredient combination.
     * This mirrors the recipes registered in ModBrewingRecipes.
     */
    @Unique
    private static boolean skyscobblemon$hasValidRecipe(ItemStack input, ItemStack ingredient) {
        // Base potion + Gunpowder = Lingering (using gunpowder to avoid "lingering type" flag)
        if (input.is(ModItems.LIQUID_ASSIMILATION_POTION.get()) && ingredient.is(Items.GUNPOWDER)) {
            return true;
        }

        // Lingering + Redstone = Extended Lingering
        if (input.is(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get()) && ingredient.is(Items.REDSTONE)) {
            return true;
        }

        // Lingering + Vine = Creeping
        if (input.is(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get()) && ingredient.is(Items.VINE)) {
            return true;
        }

        // Creeping + Redstone = Extended Creeping
        if (input.is(ModItems.CREEPING_LIQUID_ASSIMILATION_POTION.get()) && ingredient.is(Items.REDSTONE)) {
            return true;
        }

        // Extended Lingering + Vine = Extended Creeping
        if (input.is(ModItems.LINGERING_LIQUID_ASSIMILATION_POTION_EXTENDED.get()) && ingredient.is(Items.VINE)) {
            return true;
        }

        return false;
    }
}
