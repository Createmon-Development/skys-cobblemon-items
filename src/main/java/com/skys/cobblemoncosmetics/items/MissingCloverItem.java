package com.skys.cobblemoncosmetics.items;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Missing Clover - A consumable item that grants trainer luck.
 * Each use consumes 1 durability and grants +1 trainer luck for 1 battle.
 * Requires Radical Cobblemon Trainers mod to be installed.
 */
public class MissingCloverItem extends TooltipItem {

    private static final int DEFAULT_MAX_USES = 32;
    private static final String RCT_MOD_ID = "rctmod";

    public MissingCloverItem(Properties properties) {
        super(properties
            .durability(getMaxUses())
            .setNoRepair() // Cannot be repaired in anvil
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Check if RCT mod is loaded
            if (!ModList.get().isLoaded(RCT_MOD_ID)) {
                serverPlayer.sendSystemMessage(
                    Component.literal("The clover's magic fizzles out... (Radical Cobblemon Trainers mod required)")
                        .withStyle(ChatFormatting.RED)
                );
                return InteractionResultHolder.fail(stack);
            }

            try {
                // Use reflection to call the RCT API (to avoid compile-time dependency)
                boolean success = addTrainerLuckViaReflection(player);

                if (success) {
                    // Damage the item by 1 (will break if durability reaches 0)
                    stack.hurtAndBreak(1, (ServerLevel) level, serverPlayer, (item) -> {
                        // Play break sound when item is consumed
                        level.playSound(null, player.blockPosition(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    });

                    // Play use sound
                    level.playSound(null, player.blockPosition(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);

                    // Send success message
                    serverPlayer.sendSystemMessage(
                        Component.literal("The clover's luck flows through you! (+1 Trainer Luck for 1 battle)")
                            .withStyle(ChatFormatting.GREEN)
                    );

                    return InteractionResultHolder.success(stack);
                } else {
                    serverPlayer.sendSystemMessage(
                        Component.literal("The clover's magic fizzles out... (Failed to apply trainer luck)")
                            .withStyle(ChatFormatting.RED)
                    );
                    return InteractionResultHolder.fail(stack);
                }
            } catch (Exception e) {
                // If RCT mod is not loaded or API fails, show error message
                SkysCobblemonCosmetics.LOGGER.error("Failed to apply trainer luck from Missing Clover", e);
                serverPlayer.sendSystemMessage(
                    Component.literal("The clover's magic fizzles out... (Error: " + e.getMessage() + ")")
                        .withStyle(ChatFormatting.RED)
                );
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    /**
     * Use reflection to call the RCT API to add trainer luck.
     * This avoids a compile-time dependency on the RCT mod.
     */
    private boolean addTrainerLuckViaReflection(Player player) {
        try {
            // Get RCTMod class
            Class<?> rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");

            // Get getInstance() method
            Method getInstanceMethod = rctModClass.getMethod("getInstance");
            Object rctModInstance = getInstanceMethod.invoke(null);

            // Get getTrainerLuckAPI() method
            Method getApiMethod = rctModClass.getMethod("getTrainerLuckAPI");
            Object api = getApiMethod.invoke(rctModInstance);

            // Get ITrainerLuckAPI interface
            Class<?> apiClass = Class.forName("com.gitlab.srcmc.rctmod.api.ITrainerLuckAPI");

            // Get current luck battles remaining to determine if we should add luck or battles
            Method getLuckBattlesMethod = apiClass.getMethod("getLuckBattlesRemaining", Player.class);
            int currentLuckBattles = (Integer) getLuckBattlesMethod.invoke(api, player);

            // Get addTrainerLuck(Player, int, int) method
            Method addLuckMethod = apiClass.getMethod("addTrainerLuck", Player.class, int.class, int.class);

            // If player already has luck active, extend the duration by 1 battle
            // Otherwise, grant 1 luck for 1 battle
            if (currentLuckBattles > 0) {
                // Player already has luck, extend by 1 more battle
                // NOTE: The API sets battles (not adds), so we need to add 1 to current
                addLuckMethod.invoke(api, player, 0, currentLuckBattles + 1);
            } else {
                // No active luck, grant 1 luck for 1 battle
                addLuckMethod.invoke(api, player, 1, 1);
            }

            return true;
        } catch (Exception e) {
            SkysCobblemonCosmetics.LOGGER.error("Reflection error calling RCT API", e);
            return false;
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false; // Cannot be enchanted
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false; // Cannot be enchanted with books
    }

    /**
     * Get the maximum number of uses for the Missing Clover.
     * This can be configured via datapack in the future.
     */
    public static int getMaxUses() {
        // TODO: Load from config/datapack
        // For now, return default value
        return DEFAULT_MAX_USES;
    }
}
