package com.skys.cobblemoncosmetics.items;

import com.skys.cobblemoncosmetics.entity.ThrownCreepingAssimilationPotionEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Creeping Liquid Assimilation Potion - A throwable potion that creeps toward a target player.
 * Made by brewing Lingering Liquid Assimilation Potion with a vine.
 * Can be renamed in an anvil to target a specific player (case insensitive).
 */
public class CreepingAssimilationPotionItem extends Item {

    // Effect durations in ticks (same as base potion)
    private static final int BLINDNESS_DURATION = 20 * 60;      // 1 minute
    private static final int TIPSY_DURATION = 20 * 60 * 2;      // 2 minutes
    private static final int ABSORPTION_DURATION = 20 * 30;     // 30 seconds

    private final int lingeringDuration; // in ticks

    public CreepingAssimilationPotionItem(Properties properties, int lingeringDurationSeconds) {
        super(properties);
        this.lingeringDuration = lingeringDurationSeconds * 20;
    }

    /**
     * Get the target player name from the item's custom name.
     * Returns null if no custom name or the item isn't renamed.
     */
    public static String getTargetPlayerName(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            Component customName = stack.get(DataComponents.CUSTOM_NAME);
            if (customName != null) {
                return customName.getString();
            }
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            ThrownCreepingAssimilationPotionEntity thrownPotion = new ThrownCreepingAssimilationPotionEntity(level, player);
            thrownPotion.setItem(stack);
            thrownPotion.setLingeringDuration(lingeringDuration);

            // Set target player name if item is renamed
            String targetName = getTargetPlayerName(stack);
            if (targetName != null) {
                thrownPotion.setTargetPlayerName(targetName);
            }

            thrownPotion.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(thrownPotion);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LINGERING_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // Add effect tooltips in vanilla style (blue text)
        tooltipComponents.add(formatEffectTooltip(MobEffects.BLINDNESS, BLINDNESS_DURATION));
        tooltipComponents.add(formatEffectTooltip(MobEffects.ABSORPTION, ABSORPTION_DURATION));

        // Tipsy effect from Brewin' n Chewin'
        Optional<Holder.Reference<MobEffect>> tipsyEffect = BuiltInRegistries.MOB_EFFECT
            .getHolder(ResourceLocation.fromNamespaceAndPath("brewinandchewin", "tipsy"));
        if (tipsyEffect.isPresent()) {
            tooltipComponents.add(formatEffectTooltip(tipsyEffect.get(), TIPSY_DURATION));
        } else {
            tooltipComponents.add(Component.literal("Tipsy (2:00)")
                .withStyle(ChatFormatting.BLUE));
        }

        tooltipComponents.add(Component.empty());

        // Show target info
        String targetName = getTargetPlayerName(stack);
        if (targetName != null) {
            tooltipComponents.add(Component.literal("Target: " + targetName)
                .withStyle(ChatFormatting.RED));
        } else {
            tooltipComponents.add(Component.literal("Rename in anvil to target a player")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        // Show lingering duration
        int seconds = lingeringDuration / 20;
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        String timeStr = minutes > 0
            ? String.format("%d:%02d", minutes, remainingSeconds)
            : String.format("0:%02d", seconds);
        tooltipComponents.add(Component.literal("When Thrown:")
            .withStyle(ChatFormatting.DARK_PURPLE));
        tooltipComponents.add(Component.literal("  Lingers for " + timeStr)
            .withStyle(ChatFormatting.BLUE));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private Component formatEffectTooltip(Holder<MobEffect> effect, int durationTicks) {
        int totalSeconds = durationTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeStr = String.format("%d:%02d", minutes, seconds);

        MutableComponent effectName = Component.translatable(effect.value().getDescriptionId());
        return effectName.append(" (" + timeStr + ")").withStyle(ChatFormatting.BLUE);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false; // No enchantment glint
    }

    public int getLingeringDuration() {
        return lingeringDuration;
    }
}
