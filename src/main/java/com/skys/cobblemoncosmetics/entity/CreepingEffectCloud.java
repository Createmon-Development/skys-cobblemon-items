package com.skys.cobblemoncosmetics.entity;

import com.skys.cobblemoncosmetics.items.LiquidAssimilationPotionItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A custom area effect cloud that slowly creeps toward a target player.
 * If renamed, only targets the player with that name (case insensitive).
 * Climbs up and down blocks to follow the target.
 * When it reaches a player, it crawls up their body.
 */
public class CreepingEffectCloud extends Entity {

    private static final EntityDataAccessor<String> TARGET_PLAYER_NAME =
        SynchedEntityData.defineId(CreepingEffectCloud.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> RADIUS =
        SynchedEntityData.defineId(CreepingEffectCloud.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ATTACHED =
        SynchedEntityData.defineId(CreepingEffectCloud.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> CRAWL_HEIGHT =
        SynchedEntityData.defineId(CreepingEffectCloud.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<java.util.Optional<UUID>> ATTACHED_PLAYER_UUID =
        SynchedEntityData.defineId(CreepingEffectCloud.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> EFFECTS_APPLIED =
        SynchedEntityData.defineId(CreepingEffectCloud.class, EntityDataSerializers.BOOLEAN);

    private static final double CREEP_SPEED = 0.06; // Blocks per tick (slow crawl)
    private static final double VERTICAL_SPEED = 0.04; // Slower vertical movement
    private static final double TARGET_RANGE = 20.0; // Max range to detect target
    private static final int EFFECT_APPLICATION_INTERVAL = 10; // Apply effects every half second
    private static final double ATTACH_DISTANCE = 1.0; // Distance to attach to player
    private static final float CRAWL_SPEED = 0.03F; // How fast it crawls up per tick
    private static final float DEFAULT_MAX_CRAWL_HEIGHT = 1.8F; // Fallback if player not available
    private static final float ATTACHED_RADIUS = 0.4F; // Small radius when attached
    private static final float RADIUS_SHRINK_SPEED = 0.1F; // How fast radius shrinks during transition
    private static final int TRANSITION_TICKS = 20; // Ticks for smooth transition to player

    /**
     * Whether water can remove the creeping effect cloud.
     * Default: true (enabled). Can be toggled via datapack in the future.
     * When enabled, splashing water on the cloud or placing water nearby will remove it.
     */
    public static boolean WATER_REMOVAL_ENABLED = true;

    private int duration;
    private int waitTime = 10;
    private int ticksExisted = 0;
    private float radiusPerTick = 0;
    private float radiusOnUse = -0.3F;
    private float baseRadius = 3.0F; // Store the base radius for when we detach
    private int transitionTicks = 0; // Ticks since starting attachment transition
    private boolean inTransition = false; // Whether we're transitioning to attached state

    @Nullable
    private LivingEntity owner;
    private UUID ownerUUID;

    @Nullable
    private Player attachedPlayer;
    private UUID attachedPlayerUUID;

    // Track entities that have been affected to apply effects properly
    private final Set<UUID> recentlyAffected = new HashSet<>();
    private int recentlyAffectedClearTimer = 0;

    public CreepingEffectCloud(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false; // Allow physics for terrain following
        this.duration = 600; // Default 30 seconds
    }

    public CreepingEffectCloud(Level level, double x, double y, double z) {
        this(ModEntities.CREEPING_EFFECT_CLOUD.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_PLAYER_NAME, "");
        builder.define(RADIUS, 3.0F);
        builder.define(ATTACHED, false);
        builder.define(CRAWL_HEIGHT, 0.0F);
        builder.define(ATTACHED_PLAYER_UUID, java.util.Optional.empty());
        builder.define(EFFECTS_APPLIED, false);
    }

    public void setTargetPlayerName(String name) {
        this.entityData.set(TARGET_PLAYER_NAME, name != null ? name : "");
    }

    public String getTargetPlayerName() {
        return this.entityData.get(TARGET_PLAYER_NAME);
    }

    public void setRadius(float radius) {
        float clampedRadius = Math.max(0.0F, radius);
        this.entityData.set(RADIUS, clampedRadius);
        // Store base radius for restoring after detachment
        if (!isAttached() && clampedRadius > ATTACHED_RADIUS) {
            this.baseRadius = clampedRadius;
        }
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public boolean isAttached() {
        return this.entityData.get(ATTACHED);
    }

    public void setAttached(boolean attached) {
        this.entityData.set(ATTACHED, attached);
    }

    public float getCrawlHeight() {
        return this.entityData.get(CRAWL_HEIGHT);
    }

    /**
     * Get the maximum crawl height based on the attached player's current eye height.
     * This dynamically adapts to player size modifications (e.g., shrinking enchantments).
     */
    public float getMaxCrawlHeight() {
        Player player = getAttachedPlayerClient();
        if (player != null) {
            // Use the player's actual eye height, which adapts to size modifications
            return player.getEyeHeight();
        }
        return DEFAULT_MAX_CRAWL_HEIGHT;
    }

    public void setCrawlHeight(float height) {
        float maxHeight = getMaxCrawlHeight();
        this.entityData.set(CRAWL_HEIGHT, Math.max(0.0F, Math.min(maxHeight, height)));
    }

    public void setDuration(int duration) {
        this.duration = duration;
        this.radiusPerTick = -getRadius() / (float) duration;
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner != null ? owner.getUUID() : null;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity living) {
                this.owner = living;
            }
        }
        return this.owner;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public void setRadiusOnUse(float radiusOnUse) {
        this.radiusOnUse = radiusOnUse;
    }

    @Override
    public void tick() {
        super.tick();
        ticksExisted++;

        // Clear recently affected list periodically
        recentlyAffectedClearTimer++;
        if (recentlyAffectedClearTimer >= 40) { // Every 2 seconds
            recentlyAffected.clear();
            recentlyAffectedClearTimer = 0;
        }

        if (this.level().isClientSide) {
            // Spawn particles on client
            spawnParticles();
        } else {
            // Server-side logic
            if (ticksExisted >= duration) {
                this.discard();
                return;
            }

            // Check for water - removes the effect cloud (including water nearby/above)
            // Only active if WATER_REMOVAL_ENABLED is true (can be toggled via datapack)
            if (WATER_REMOVAL_ENABLED && checkForWater()) {
                // Play extinguish particles
                for (int i = 0; i < 10; i++) {
                    this.level().addParticle(
                        ParticleTypes.SPLASH,
                        this.getX() + (this.random.nextDouble() - 0.5) * 2,
                        this.getY() + this.random.nextDouble(),
                        this.getZ() + (this.random.nextDouble() - 0.5) * 2,
                        0, 0.1, 0
                    );
                }
                this.discard();
                return;
            }

            // Find target player
            Player target = findTarget();

            // Handle attachment state
            if (isAttached()) {
                // Validate attached player still exists
                Player attached = getAttachedPlayer();
                if (attached == null || !attached.isAlive()) {
                    detachFromPlayer();
                } else {
                    // Check if splash water bottle hit the attached player
                    if (WATER_REMOVAL_ENABLED && checkForSplashWaterBottle(attached)) {
                        // Play extinguish particles around the player
                        for (int i = 0; i < 15; i++) {
                            this.level().addParticle(
                                ParticleTypes.SPLASH,
                                attached.getX() + (this.random.nextDouble() - 0.5) * attached.getBbWidth() * 2,
                                attached.getY() + this.random.nextDouble() * attached.getBbHeight(),
                                attached.getZ() + (this.random.nextDouble() - 0.5) * attached.getBbWidth() * 2,
                                0, 0.1, 0
                            );
                        }
                        this.discard();
                        return;
                    }

                    // Stay attached permanently - follow the player
                    updateAttachedPosition(attached);
                    // Increase crawl height over time, capped at player's eye height
                    float playerEyeHeight = attached.getEyeHeight();
                    float currentCrawl = getCrawlHeight();
                    if (currentCrawl < playerEyeHeight) {
                        // Scale crawl speed proportionally to player size for consistent feel
                        float scaledCrawlSpeed = CRAWL_SPEED * (playerEyeHeight / DEFAULT_MAX_CRAWL_HEIGHT);
                        float newCrawlHeight = Math.min(currentCrawl + scaledCrawlSpeed, playerEyeHeight);
                        setCrawlHeight(newCrawlHeight);
                    }
                }
            } else if (inTransition) {
                // Transitioning to attached state - smooth approach to player
                Player attached = getAttachedPlayer();
                if (attached == null || !attached.isAlive()) {
                    cancelTransition();
                } else {
                    transitionTicks++;

                    // Smoothly shrink radius
                    float currentRadius = getRadius();
                    if (currentRadius > ATTACHED_RADIUS) {
                        setRadius(Math.max(ATTACHED_RADIUS, currentRadius - RADIUS_SHRINK_SPEED));
                    }

                    // Smoothly move toward player's feet
                    Vec3 targetPos = attached.position();
                    Vec3 currentPos = this.position();
                    double lerpFactor = Math.min(1.0, transitionTicks / (double) TRANSITION_TICKS);
                    double newX = currentPos.x + (targetPos.x - currentPos.x) * 0.15;
                    double newY = currentPos.y + (targetPos.y - currentPos.y) * 0.15;
                    double newZ = currentPos.z + (targetPos.z - currentPos.z) * 0.15;
                    this.setPos(newX, newY, newZ);

                    // Check if transition is complete
                    if (transitionTicks >= TRANSITION_TICKS || this.distanceTo(attached) < 0.3) {
                        completeTransition();
                    }
                }
            } else {
                // Not attached - normal creeping behavior
                // Shrink radius over time (only when not attached)
                float currentRadius = getRadius();
                currentRadius += radiusPerTick;
                if (currentRadius <= 0) {
                    this.discard();
                    return;
                }
                setRadius(currentRadius);

                if (target != null) {
                    double distance = this.distanceTo(target);
                    if (distance <= ATTACH_DISTANCE) {
                        // Close enough - start transition to attach
                        startTransition(target);
                    } else {
                        // Move toward target
                        moveTowardTarget(target);
                    }
                }
            }

            // Apply effects to entities in range
            if (ticksExisted > waitTime && ticksExisted % EFFECT_APPLICATION_INTERVAL == 0) {
                applyEffectsToEntities();
            }
        }
    }

    /**
     * Start smooth transition to attach to a player.
     */
    private void startTransition(Player player) {
        this.inTransition = true;
        this.transitionTicks = 0;
        this.attachedPlayer = player;
        this.attachedPlayerUUID = player.getUUID();
        // Sync to client so particles can follow the player
        this.entityData.set(ATTACHED_PLAYER_UUID, java.util.Optional.of(player.getUUID()));
        this.entityData.set(EFFECTS_APPLIED, false);
    }

    /**
     * Complete the transition and fully attach to the player.
     */
    private void completeTransition() {
        setAttached(true);
        this.inTransition = false;
        this.transitionTicks = 0;
        setCrawlHeight(0.0F);
        setRadius(ATTACHED_RADIUS);
    }

    /**
     * Cancel transition if player died or left.
     */
    private void cancelTransition() {
        this.inTransition = false;
        this.transitionTicks = 0;
        this.attachedPlayer = null;
        this.attachedPlayerUUID = null;
        this.entityData.set(ATTACHED_PLAYER_UUID, java.util.Optional.empty());
        this.entityData.set(EFFECTS_APPLIED, false);
    }

    /**
     * Detach from player (only happens if player dies/disconnects).
     */
    private void detachFromPlayer() {
        setAttached(false);
        this.inTransition = false;
        this.attachedPlayer = null;
        this.attachedPlayerUUID = null;
        this.entityData.set(ATTACHED_PLAYER_UUID, java.util.Optional.empty());
        this.entityData.set(EFFECTS_APPLIED, false);
        setCrawlHeight(0.0F);
        // Restore base radius when detaching
        setRadius(Math.max(baseRadius, 1.5F));
    }

    /**
     * Get the currently attached player.
     */
    @Nullable
    private Player getAttachedPlayer() {
        if (this.attachedPlayer == null && this.attachedPlayerUUID != null && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.attachedPlayerUUID);
            if (entity instanceof Player player) {
                this.attachedPlayer = player;
            }
        }
        return this.attachedPlayer;
    }

    /**
     * Update position to stay with attached player.
     */
    private void updateAttachedPosition(Player player) {
        // Position at player's feet + crawl height
        this.setPos(player.getX(), player.getY() + getCrawlHeight(), player.getZ());
    }

    /**
     * Find the target player to creep toward.
     */
    @Nullable
    private Player findTarget() {
        String targetName = getTargetPlayerName();

        if (targetName != null && !targetName.isEmpty()) {
            // Look for specific player by name (case insensitive)
            for (Player player : this.level().players()) {
                if (player.getName().getString().equalsIgnoreCase(targetName)) {
                    double distance = this.distanceTo(player);
                    if (distance <= TARGET_RANGE) {
                        return player;
                    }
                }
            }
            return null; // Named target not found or not in range
        } else {
            // Find nearest player within range
            return this.level().getNearestPlayer(this, TARGET_RANGE);
        }
    }

    /**
     * Move slowly toward the target player, following terrain.
     */
    private void moveTowardTarget(Player target) {
        Vec3 targetPos = target.position();
        Vec3 currentPos = this.position();

        // Calculate horizontal direction
        double dx = targetPos.x - currentPos.x;
        double dz = targetPos.z - currentPos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance > 0.5) {
            // Normalize horizontal movement
            double moveX = (dx / horizontalDistance) * CREEP_SPEED;
            double moveZ = (dz / horizontalDistance) * CREEP_SPEED;

            // Calculate new position
            double newX = currentPos.x + moveX;
            double newZ = currentPos.z + moveZ;

            // Find the ground level at the new position
            double newY = findGroundLevel(newX, currentPos.y, newZ);

            // Smoothly adjust Y to follow terrain
            double targetY = newY;
            double currentY = currentPos.y;
            double yDiff = targetY - currentY;

            // Clamp vertical movement speed
            if (Math.abs(yDiff) > VERTICAL_SPEED) {
                yDiff = Math.signum(yDiff) * VERTICAL_SPEED;
            }

            // Set new position
            this.setPos(newX, currentY + yDiff, newZ);
        }
    }

    /**
     * Check if the cloud is in contact with water (includes nearby water blocks).
     * Water splashes or water buckets poured on the cloud will remove it.
     */
    private boolean checkForWater() {
        // Check if directly in water
        if (this.isInWater()) return true;

        // Check nearby blocks for water (including above, in case water is poured)
        BlockPos center = this.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) { // Check up to 2 blocks above
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = center.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(checkPos);
                    // Check for water or waterlogged blocks
                    if (state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.FLOWING_WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if a splash water bottle has recently landed near the attached player.
     * This allows players to remove the creeping effect by splashing themselves with water.
     */
    private boolean checkForSplashWaterBottle(Player attachedPlayer) {
        // Check for nearby ThrownPotion entities that are water bottles and about to land/have landed
        AABB searchArea = attachedPlayer.getBoundingBox().inflate(3.0);
        List<ThrownPotion> nearbyPotions = this.level().getEntitiesOfClass(
            ThrownPotion.class,
            searchArea,
            potion -> {
                // Check if this is a water bottle (splash or lingering)
                ItemStack potionStack = potion.getItem();
                if (potionStack.is(Items.SPLASH_POTION) || potionStack.is(Items.LINGERING_POTION)) {
                    // Check if it's water
                    PotionContents contents = potionStack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
                    if (contents != null && contents.potion().isPresent()) {
                        return contents.potion().get() == Potions.WATER;
                    }
                }
                return false;
            }
        );

        // If there's a water bottle potion landing nearby (low velocity = about to impact or just impacted)
        for (ThrownPotion potion : nearbyPotions) {
            Vec3 velocity = potion.getDeltaMovement();
            double speed = velocity.length();
            // Check if potion is near impact (low speed and close to ground/player)
            if (speed < 0.5 || potion.onGround() || potion.distanceTo(attachedPlayer) < 2.0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find the ground level at the given X/Z position.
     * Searches up and down from the current Y level.
     */
    private double findGroundLevel(double x, double currentY, double z) {
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        int startY = (int) Math.floor(currentY);

        // Search downward first (up to 3 blocks)
        for (int yOffset = 0; yOffset >= -3; yOffset--) {
            checkPos.set((int) Math.floor(x), startY + yOffset, (int) Math.floor(z));
            BlockState stateBelow = this.level().getBlockState(checkPos.below());
            BlockState stateAt = this.level().getBlockState(checkPos);

            // Found solid ground with air above
            if (!stateBelow.isAir() && stateBelow.isSolid() && (stateAt.isAir() || !stateAt.isSolid())) {
                return checkPos.getY();
            }
        }

        // Search upward (up to 2 blocks) for climbing
        for (int yOffset = 1; yOffset <= 2; yOffset++) {
            checkPos.set((int) Math.floor(x), startY + yOffset, (int) Math.floor(z));
            BlockState stateBelow = this.level().getBlockState(checkPos.below());
            BlockState stateAt = this.level().getBlockState(checkPos);

            // Found solid ground with air above
            if (!stateBelow.isAir() && stateBelow.isSolid() && (stateAt.isAir() || !stateAt.isSolid())) {
                return checkPos.getY();
            }
        }

        // Default to current Y if no good ground found
        return currentY;
    }

    /**
     * Apply effects to all living entities within the cloud radius.
     * When attached to a player, continuously reapplies effects once particles reach the head.
     * Dynamically adapts to player size modifications (e.g., shrinking enchantments).
     */
    private void applyEffectsToEntities() {
        // If attached to a player, apply effects continuously once particles reach head level
        if (isAttached() && attachedPlayerUUID != null) {
            Player attached = getAttachedPlayer();
            if (attached != null && attached.isAlive()) {
                // Use the player's actual eye height for size-modified players
                float playerEyeHeight = attached.getEyeHeight();
                // Apply effects once crawl height reaches ~90% of eye level (slightly before fully covered)
                // This makes the effect feel more responsive while still visually covering the head
                if (getCrawlHeight() >= playerEyeHeight * 0.9f) {
                    applyEffectsToEntity(attached);
                }
            }
            return; // When attached, only affect the attached player (when at head)
        }

        // During transition, don't apply effects yet - wait until fully crawled up
        if (inTransition) {
            return;
        }

        float radius = getRadius();

        // Create AABB for the cloud's effect area - extend vertically to catch players
        AABB effectArea = new AABB(
            this.getX() - radius, this.getY() - 0.5, this.getZ() - radius,
            this.getX() + radius, this.getY() + 2.5, this.getZ() + radius
        );

        // Get all living entities in range - let addEffect handle potion immunity
        List<LivingEntity> entities = this.level().getEntitiesOfClass(
            LivingEntity.class,
            effectArea,
            entity -> {
                // Skip the owner who threw it
                if (ownerUUID != null && entity.getUUID().equals(ownerUUID)) return false;

                // Check horizontal distance (circular area)
                double dx = entity.getX() - this.getX();
                double dz = entity.getZ() - this.getZ();
                double horizontalDistSq = dx * dx + dz * dz;
                return horizontalDistSq <= radius * radius;
            }
        );

        for (LivingEntity entity : entities) {
            applyEffectsToEntity(entity);
        }
    }

    /**
     * Apply effects to a single entity.
     */
    private void applyEffectsToEntity(LivingEntity entity) {
        // Skip if recently affected (prevents rapid re-application)
        if (recentlyAffected.contains(entity.getUUID())) return;

        // Apply all effects - addEffect handles immunity checks internally
        List<MobEffectInstance> effects = LiquidAssimilationPotionItem.getEffects();
        boolean appliedAny = false;
        for (MobEffectInstance effect : effects) {
            // Create a new instance to avoid issues with shared references
            boolean applied = entity.addEffect(new MobEffectInstance(
                effect.getEffect(),
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon()
            ));
            if (applied) appliedAny = true;
        }

        // Only mark as affected and shrink if effects were actually applied
        if (appliedAny) {
            // Mark as recently affected
            recentlyAffected.add(entity.getUUID());

            // Shrink radius when used (only if not attached)
            if (!isAttached() && !inTransition) {
                float newRadius = getRadius() + radiusOnUse;
                if (newRadius <= 0.5F) {
                    this.discard();
                    return;
                }
                setRadius(newRadius);
            }
        }
    }

    /**
     * Spawn particles around the cloud.
     * When attached to a player, particles crawl up their body and follow them.
     */
    private void spawnParticles() {
        if (inTransition) {
            // Get the target player for transition particles
            Player targetPlayer = getAttachedPlayerClient();
            double baseX = targetPlayer != null ? targetPlayer.getX() : this.getX();
            double baseY = targetPlayer != null ? targetPlayer.getY() : this.getY();
            double baseZ = targetPlayer != null ? targetPlayer.getZ() : this.getZ();

            // Transitioning - particles converge toward the player
            float radius = getRadius();
            for (int i = 0; i < 6; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double dist = radius * (0.3 + this.random.nextDouble() * 0.7);
                double offsetX = Math.cos(angle) * dist;
                double offsetZ = Math.sin(angle) * dist;
                double offsetY = this.random.nextDouble() * 0.8;

                // Particles move inward toward center
                double velX = -offsetX * 0.05;
                double velZ = -offsetZ * 0.05;
                double velY = 0.02;

                this.level().addParticle(
                    ParticleTypes.SQUID_INK,
                    baseX + offsetX,
                    baseY + offsetY,
                    baseZ + offsetZ,
                    velX, velY, velZ
                );
            }
        } else if (isAttached()) {
            // Get the attached player directly for precise particle positioning
            Player attachedPlayer = getAttachedPlayerClient();
            if (attachedPlayer == null) return; // Can't spawn particles without player reference

            // Use player's current position for particles (follows them precisely)
            double playerX = attachedPlayer.getX();
            double playerY = attachedPlayer.getY();
            double playerZ = attachedPlayer.getZ();

            // Get dynamic player dimensions for size-modified players
            float playerEyeHeight = attachedPlayer.getEyeHeight();
            float playerWidth = attachedPlayer.getBbWidth();

            float crawlHeight = getCrawlHeight();
            // Scale particle radius based on player width (smaller players = tighter ring)
            float attachedRadius = Math.max(ATTACHED_RADIUS, playerWidth * 0.6f);

            // Spawn particles at the crawl height level around the player
            for (int i = 0; i < 8; i++) {
                // Spawn in a tight ring around the player at the current crawl height
                double angle = this.random.nextDouble() * Math.PI * 2;
                double ringRadius = attachedRadius * (0.5 + this.random.nextDouble() * 0.5);
                double offsetX = Math.cos(angle) * ringRadius;
                double offsetZ = Math.sin(angle) * ringRadius;

                // Particles move slightly upward to give crawling effect
                // Scale velocity based on player size for consistent visual
                double velY = (0.02 + this.random.nextDouble() * 0.02) * (playerEyeHeight / DEFAULT_MAX_CRAWL_HEIGHT);

                this.level().addParticle(
                    ParticleTypes.SQUID_INK,
                    playerX + offsetX,
                    playerY + crawlHeight + (this.random.nextDouble() * 0.1 * (playerEyeHeight / DEFAULT_MAX_CRAWL_HEIGHT)),
                    playerZ + offsetZ,
                    0, velY, 0
                );
            }

            // Spawn trail particles below the crawl line to show it crawling up
            // Scale threshold based on player size
            if (crawlHeight > 0.2F * (playerEyeHeight / DEFAULT_MAX_CRAWL_HEIGHT)) {
                for (int i = 0; i < 5; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double trailRadius = attachedRadius * (0.6 + this.random.nextDouble() * 0.4);
                    double offsetX = Math.cos(angle) * trailRadius;
                    double offsetZ = Math.sin(angle) * trailRadius;
                    // Trail particles spread from feet up to current crawl height
                    double trailY = this.random.nextDouble() * crawlHeight;

                    this.level().addParticle(
                        ParticleTypes.SQUID_INK,
                        playerX + offsetX,
                        playerY + trailY,
                        playerZ + offsetZ,
                        0, 0.005 * (playerEyeHeight / DEFAULT_MAX_CRAWL_HEIGHT), 0
                    );
                }
            }
        } else {
            // Normal spreading particles when not attached
            float radius = getRadius();
            for (int i = 0; i < 5; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * radius * 2;
                double offsetY = this.random.nextDouble() * 1.0;
                double offsetZ = (this.random.nextDouble() - 0.5) * radius * 2;

                this.level().addParticle(
                    ParticleTypes.SQUID_INK,
                    this.getX() + offsetX,
                    this.getY() + offsetY,
                    this.getZ() + offsetZ,
                    0, 0.01, 0
                );
            }
        }
    }

    /**
     * Get the attached player on the client side using synced UUID.
     */
    @Nullable
    private Player getAttachedPlayerClient() {
        java.util.Optional<UUID> syncedUUID = this.entityData.get(ATTACHED_PLAYER_UUID);
        if (syncedUUID.isPresent()) {
            return this.level().getPlayerByUUID(syncedUUID.get());
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ticksExisted = tag.getInt("Age");
        this.duration = tag.getInt("Duration");
        this.waitTime = tag.getInt("WaitTime");
        this.radiusPerTick = tag.getFloat("RadiusPerTick");
        this.radiusOnUse = tag.getFloat("RadiusOnUse");
        this.baseRadius = tag.getFloat("BaseRadius");
        this.inTransition = tag.getBoolean("InTransition");
        this.transitionTicks = tag.getInt("TransitionTicks");
        this.setRadius(tag.getFloat("Radius"));
        this.setTargetPlayerName(tag.getString("TargetPlayer"));
        this.setAttached(tag.getBoolean("Attached"));
        this.setCrawlHeight(tag.getFloat("CrawlHeight"));
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        if (tag.hasUUID("AttachedPlayer")) {
            this.attachedPlayerUUID = tag.getUUID("AttachedPlayer");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.ticksExisted);
        tag.putInt("Duration", this.duration);
        tag.putInt("WaitTime", this.waitTime);
        tag.putFloat("RadiusPerTick", this.radiusPerTick);
        tag.putFloat("RadiusOnUse", this.radiusOnUse);
        tag.putFloat("BaseRadius", this.baseRadius);
        tag.putBoolean("InTransition", this.inTransition);
        tag.putInt("TransitionTicks", this.transitionTicks);
        tag.putFloat("Radius", this.getRadius());
        tag.putString("TargetPlayer", this.getTargetPlayerName());
        tag.putBoolean("Attached", this.isAttached());
        tag.putFloat("CrawlHeight", this.getCrawlHeight());
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        if (this.attachedPlayerUUID != null) {
            tag.putUUID("AttachedPlayer", this.attachedPlayerUUID);
        }
    }
}
