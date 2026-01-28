package com.skys.cobblemoncosmetics.items;

import com.skys.cobblemoncosmetics.entity.ThrownAssimilationPotionEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Liquid Assimilation Potion - A special potion brewed in a Keg from Brewin' n Chewin'.
 * Made with black concrete liquid, black dye, and slime ball.
 *
 * Effects:
 * - Blindness (1 minute)
 * - Tipsy (2 minutes) - from Brewin' n Chewin'
 * - Absorption (30 seconds)
 */
public class LiquidAssimilationPotionItem extends Item {

    // Effect durations in ticks
    private static final int BLINDNESS_DURATION = 20 * 60;      // 1 minute
    private static final int TIPSY_DURATION = 20 * 60 * 2;      // 2 minutes
    private static final int ABSORPTION_DURATION = 20 * 30;     // 30 seconds

    private final boolean isLingering;
    private final int lingeringDuration; // in ticks

    public LiquidAssimilationPotionItem(Properties properties, boolean isLingering, int lingeringDurationSeconds) {
        super(properties);
        this.isLingering = isLingering;
        this.lingeringDuration = lingeringDurationSeconds * 20; // Convert to ticks
    }

    /**
     * Get the list of effects this potion applies.
     */
    public static List<MobEffectInstance> getEffects() {
        List<MobEffectInstance> effects = new ArrayList<>();

        // Blindness - 1 minute
        effects.add(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, 0));

        // Absorption - 30 seconds
        effects.add(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_DURATION, 0));

        // Tipsy from Brewin' n Chewin' - 2 minutes
        Optional<Holder.Reference<MobEffect>> tipsyEffect = BuiltInRegistries.MOB_EFFECT
            .getHolder(ResourceLocation.fromNamespaceAndPath("brewinandchewin", "tipsy"));
        tipsyEffect.ifPresent(effect ->
            effects.add(new MobEffectInstance(effect, TIPSY_DURATION, 0)));

        return effects;
    }

    /**
     * Apply effects to a living entity.
     */
    public static void applyEffects(LivingEntity entity) {
        for (MobEffectInstance effect : getEffects()) {
            entity.addEffect(new MobEffectInstance(effect));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isLingering) {
            // Throwable version - throw like a lingering potion
            if (!level.isClientSide) {
                ThrownAssimilationPotionEntity thrownPotion = new ThrownAssimilationPotionEntity(level, player);
                thrownPotion.setItem(stack);
                thrownPotion.setLingeringDuration(lingeringDuration);
                thrownPotion.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
                level.addFreshEntity(thrownPotion);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LINGERING_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        } else {
            // Drinkable version
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!isLingering) {
            if (!level.isClientSide) {
                // Play drinking sound
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);

                // Apply potion effects
                applyEffects(entity);
            }

            if (entity instanceof Player player) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    // Return empty glass bottle
                    ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (!player.getInventory().add(bottle)) {
                        player.drop(bottle, false);
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isLingering ? 0 : 32; // Drink duration like a potion
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isLingering ? UseAnim.NONE : UseAnim.DRINK;
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
            // Fallback if Brewin' n Chewin' not loaded
            tooltipComponents.add(Component.literal("Tipsy (2:00)")
                .withStyle(ChatFormatting.BLUE));
        }

        // Show lingering duration for throwable variants
        if (isLingering) {
            tooltipComponents.add(Component.empty());
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
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * Format an effect tooltip in vanilla Minecraft style.
     */
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

    public boolean isLingering() {
        return isLingering;
    }

    public int getLingeringDuration() {
        return lingeringDuration;
    }
}
