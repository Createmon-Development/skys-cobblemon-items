package com.skys.cobblemoncosmetics.client;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.entity.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SkysCobblemonCosmetics.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register thrown potion renderer - uses the item texture
        event.registerEntityRenderer(ModEntities.THROWN_ASSIMILATION_POTION.get(), ThrownItemRenderer::new);

        // Register thrown creeping potion renderer
        event.registerEntityRenderer(ModEntities.THROWN_CREEPING_ASSIMILATION_POTION.get(), ThrownItemRenderer::new);

        // Register creeping effect cloud renderer (particles handle the visual, no model needed)
        event.registerEntityRenderer(ModEntities.CREEPING_EFFECT_CLOUD.get(), NoopRenderer::new);
    }
}
