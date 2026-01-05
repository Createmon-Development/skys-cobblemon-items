package com.skys.cobblemoncosmetics.events;

import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles the secret sandpaper polishing mechanic for the rusted gold pokeball.
 * This is a hidden easter egg that doesn't show up in JEI.
 */
@EventBusSubscriber
public class SandpaperPolishingHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack heldItem = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherItem = player.getItemInHand(otherHand);

        // Check if player is holding rusted pokeball and sandpaper (in either hand combination)
        boolean mainIsRusted = heldItem.is(ModItems.RUSTED_GOLD_POKEBALL.get());
        boolean offIsSandpaper = isSandpaper(otherItem);
        boolean mainIsSandpaper = isSandpaper(heldItem);
        boolean offIsRusted = otherItem.is(ModItems.RUSTED_GOLD_POKEBALL.get());

        if ((mainIsRusted && offIsSandpaper) || (mainIsSandpaper && offIsRusted)) {
            // Determine which hand has the rusted pokeball and which has sandpaper
            InteractionHand rustedHand = mainIsRusted ? hand : otherHand;
            InteractionHand sandpaperHand = mainIsSandpaper ? hand : otherHand;

            ItemStack rustedStack = player.getItemInHand(rustedHand);
            ItemStack sandpaperStack = player.getItemInHand(sandpaperHand);

            // Only process on server side
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                // Consume one rusted pokeball
                rustedStack.shrink(1);

                // Damage the sandpaper
                sandpaperStack.hurtAndBreak(1, (ServerLevel) player.level(), serverPlayer, (item) -> {});

                // Give the polished pokeball
                ItemStack polishedBall = new ItemStack(ModItems.POLISHED_GOLD_POKEBALL.get());
                if (!player.addItem(polishedBall)) {
                    // If inventory is full, drop it
                    player.drop(polishedBall, false);
                }

                // Play a satisfying sound
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

                // Send success message in yellow gold text
                serverPlayer.sendSystemMessage(
                    Component.literal("Success!! You polished the old ball, it sparkles as good as new.")
                        .withStyle(ChatFormatting.GOLD)
                );
            }

            // Cancel the event to prevent other interactions
            event.setCanceled(true);
        }
    }

    /**
     * Check if an item is sandpaper from the Create mod.
     */
    private static boolean isSandpaper(ItemStack stack) {
        // Check if the item's ID contains "sand_paper"
        String itemId = stack.getItem().toString();
        return itemId.contains("sand_paper");
    }
}
