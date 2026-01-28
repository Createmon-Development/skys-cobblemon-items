package com.skys.cobblemoncosmetics.entity;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, SkysCobblemonCosmetics.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownAssimilationPotionEntity>> THROWN_ASSIMILATION_POTION =
        ENTITY_TYPES.register("thrown_assimilation_potion", () ->
            EntityType.Builder.<ThrownAssimilationPotionEntity>of(ThrownAssimilationPotionEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build("thrown_assimilation_potion"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownCreepingAssimilationPotionEntity>> THROWN_CREEPING_ASSIMILATION_POTION =
        ENTITY_TYPES.register("thrown_creeping_assimilation_potion", () ->
            EntityType.Builder.<ThrownCreepingAssimilationPotionEntity>of(ThrownCreepingAssimilationPotionEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build("thrown_creeping_assimilation_potion"));

    public static final DeferredHolder<EntityType<?>, EntityType<CreepingEffectCloud>> CREEPING_EFFECT_CLOUD =
        ENTITY_TYPES.register("creeping_effect_cloud", () ->
            EntityType.Builder.<CreepingEffectCloud>of(CreepingEffectCloud::new, MobCategory.MISC)
                .sized(6.0F, 0.5F) // Wide but flat like an area effect cloud
                .clientTrackingRange(10)
                .updateInterval(Integer.MAX_VALUE) // Don't sync position updates (we handle movement ourselves)
                .build("creeping_effect_cloud"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        SkysCobblemonCosmetics.LOGGER.info("Registered custom entities");
    }
}
