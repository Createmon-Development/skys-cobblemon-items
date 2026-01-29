package com.skys.cobblemoncosmetics.entity;

import com.skys.cobblemoncosmetics.items.LiquidAssimilationPotionItem;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown creeping assimilation potion that creates a moving area effect cloud on impact.
 */
public class ThrownCreepingAssimilationPotionEntity extends ThrowableItemProjectile {

    private int lingeringDuration = 45 * 20; // Default 45 seconds in ticks
    private String targetPlayerName = null;

    public ThrownCreepingAssimilationPotionEntity(EntityType<? extends ThrownCreepingAssimilationPotionEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownCreepingAssimilationPotionEntity(Level level, LivingEntity thrower) {
        super(ModEntities.THROWN_CREEPING_ASSIMILATION_POTION.get(), thrower, level);
    }

    public ThrownCreepingAssimilationPotionEntity(Level level, double x, double y, double z) {
        super(ModEntities.THROWN_CREEPING_ASSIMILATION_POTION.get(), x, y, z, level);
    }

    public void setLingeringDuration(int durationTicks) {
        this.lingeringDuration = durationTicks;
    }

    public void setTargetPlayerName(String name) {
        this.targetPlayerName = name;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CREEPING_LIQUID_ASSIMILATION_POTION.get();
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

            // Create creeping effect cloud
            createCreepingCloud();

            // Remove the entity
            this.discard();
        }
    }

    /**
     * Creates a CreepingEffectCloud at the impact location.
     */
    private void createCreepingCloud() {
        CreepingEffectCloud cloud = new CreepingEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());

        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner) {
            cloud.setOwner(livingOwner);
        }

        // Set cloud properties
        cloud.setRadius(3.0F);
        cloud.setDuration(lingeringDuration);
        cloud.setWaitTime(10);
        cloud.setRadiusOnUse(-0.5F);

        // Set target player if specified
        if (targetPlayerName != null && !targetPlayerName.isEmpty()) {
            cloud.setTargetPlayerName(targetPlayerName);
        }

        this.level().addFreshEntity(cloud);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05; // Same as vanilla potions
    }
}
