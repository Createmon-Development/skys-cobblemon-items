package com.skys.cobblemoncosmetics.entity;

import com.skys.cobblemoncosmetics.items.LiquidAssimilationPotionItem;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * A thrown lingering assimilation potion that creates an area effect cloud on impact.
 */
public class ThrownAssimilationPotionEntity extends ThrowableItemProjectile {

    private int lingeringDuration = 45 * 20; // Default 45 seconds in ticks

    public ThrownAssimilationPotionEntity(EntityType<? extends ThrownAssimilationPotionEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownAssimilationPotionEntity(Level level, LivingEntity thrower) {
        super(ModEntities.THROWN_ASSIMILATION_POTION.get(), thrower, level);
    }

    public ThrownAssimilationPotionEntity(Level level, double x, double y, double z) {
        super(ModEntities.THROWN_ASSIMILATION_POTION.get(), x, y, z, level);
    }

    public void setLingeringDuration(int durationTicks) {
        this.lingeringDuration = durationTicks;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.LINGERING_LIQUID_ASSIMILATION_POTION.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            // Apply effects directly to hit entity
            LiquidAssimilationPotionItem.applyEffects(livingEntity);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (!this.level().isClientSide) {
            // Play break sound
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SPLASH_POTION_BREAK, this.getSoundSource(), 1.0F, 1.0F);

            // Create lingering effect cloud
            createAreaEffectCloud();

            // Remove the entity
            this.discard();
        }
    }

    /**
     * Creates an AreaEffectCloud at the impact location with black particles.
     */
    private void createAreaEffectCloud() {
        AreaEffectCloud cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());

        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner) {
            cloud.setOwner(livingOwner);
        }

        // Set cloud properties - radius stays constant for standard lingering
        cloud.setRadius(3.0F);
        cloud.setRadiusOnUse(-0.5F);
        cloud.setWaitTime(10);
        cloud.setRadiusPerTick(0); // Keep radius constant - no shrinking over time
        cloud.setDuration(lingeringDuration);

        // Use dark colored particles (squid ink or soul fire flame for black effect)
        cloud.setParticle(ParticleTypes.SQUID_INK);

        // Add all potion effects
        List<MobEffectInstance> effects = LiquidAssimilationPotionItem.getEffects();
        for (MobEffectInstance effect : effects) {
            cloud.addEffect(new MobEffectInstance(effect));
        }

        this.level().addFreshEntity(cloud);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05; // Same as vanilla potions
    }
}
