package com.skys.cobblemoncosmetics.hunt;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Runic Cipher Tablet - found at the end of the Ocean Trial Chamber.
 * Initially faded, becomes readable when glow ink sac is applied.
 */
public class RunicCipherTabletItem extends Item {

    public RunicCipherTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tabletStack = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);

        // Check if player is holding glow ink sac in other hand
        if (otherStack.is(Items.GLOW_INK_SAC) && !isGlowing(tabletStack)) {
            if (!level.isClientSide) {
                // Consume glow ink sac
                otherStack.shrink(1);

                // Make tablet glow
                setGlowing(tabletStack, true);

                // Play sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLOW_INK_SAC_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

                // Send message
                player.sendSystemMessage(Component.literal("The runes begin to glow with ethereal light...")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            }

            return InteractionResultHolder.sidedSuccess(tabletStack, level.isClientSide);
        }

        return InteractionResultHolder.pass(tabletStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        boolean glowing = isGlowing(stack);

        if (!glowing) {
            // Faded state - can't read the text
            tooltipComponents.add(Component.literal(HuntConfig.DESC_TABLET_FADED)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("Perhaps something could brighten the text...")
                .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            // Glowing state - reveals the cipher
            tooltipComponents.add(Component.literal(HuntConfig.DESC_TABLET_GLOWING)
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            tooltipComponents.add(Component.empty());

            // The cipher key/translation
            tooltipComponents.add(Component.literal("=== RUNIC CIPHER ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltipComponents.add(Component.empty());

            // Show the rune to letter mapping
            tooltipComponents.add(Component.literal(HuntConfig.UNSCRAMBLED_RUNES)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltipComponents.add(Component.literal("        ↓")
                .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal(HuntConfig.DECODED_MESSAGE)
                .withStyle(ChatFormatting.GREEN));

            tooltipComponents.add(Component.empty());

            // The coordinate hints
            tooltipComponents.add(Component.literal("=== TO FIND THE PATH ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("X - Look to the stars")
                .withStyle(ChatFormatting.YELLOW));
            tooltipComponents.add(Component.literal("Y - Hidden within these very words")
                .withStyle(ChatFormatting.YELLOW));
            tooltipComponents.add(Component.literal("Z - Return to the world's origin")
                .withStyle(ChatFormatting.YELLOW));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Enchant glint when glowing
        return isGlowing(stack);
    }

    // === Helper methods for data component access ===

    public static boolean isGlowing(ItemStack stack) {
        Boolean glowing = stack.get(HuntDataComponents.TABLET_GLOWING.get());
        return glowing != null && glowing;
    }

    public static void setGlowing(ItemStack stack, boolean glowing) {
        stack.set(HuntDataComponents.TABLET_GLOWING.get(), glowing);
    }
}
