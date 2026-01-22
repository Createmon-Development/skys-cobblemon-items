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

            // The cipher key/translation - Full alphabet
            tooltipComponents.add(Component.literal("══ RUNIC CIPHER ══")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltipComponents.add(Component.empty());

            // Display alphabet in rows of 9
            tooltipComponents.add(Component.literal("Letters:")
                .withStyle(ChatFormatting.GRAY));

            // Row 1: A-I
            StringBuilder row1 = new StringBuilder();
            for (int i = 0; i < 9; i++) {
                row1.append(HuntConfig.RUNIC_ALPHABET[i]);
                if (i < 8) row1.append(" ");
            }
            tooltipComponents.add(Component.literal(row1.toString())
                .withStyle(ChatFormatting.LIGHT_PURPLE));

            // Row 2: J-R
            StringBuilder row2 = new StringBuilder();
            for (int i = 9; i < 18; i++) {
                row2.append(HuntConfig.RUNIC_ALPHABET[i]);
                if (i < 17) row2.append(" ");
            }
            tooltipComponents.add(Component.literal(row2.toString())
                .withStyle(ChatFormatting.LIGHT_PURPLE));

            // Row 3: S-Z
            StringBuilder row3 = new StringBuilder();
            for (int i = 18; i < 26; i++) {
                row3.append(HuntConfig.RUNIC_ALPHABET[i]);
                if (i < 25) row3.append(" ");
            }
            tooltipComponents.add(Component.literal(row3.toString())
                .withStyle(ChatFormatting.LIGHT_PURPLE));

            tooltipComponents.add(Component.empty());

            // Display numbers
            tooltipComponents.add(Component.literal("Numbers:")
                .withStyle(ChatFormatting.GRAY));

            // Numbers 0-9 in one row
            StringBuilder numRow = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                numRow.append(HuntConfig.RUNIC_NUMBERS[i]);
                if (i < 9) numRow.append(" ");
            }
            tooltipComponents.add(Component.literal(numRow.toString())
                .withStyle(ChatFormatting.AQUA));
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
