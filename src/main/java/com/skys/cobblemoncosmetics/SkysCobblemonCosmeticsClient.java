package com.skys.cobblemoncosmetics;

import com.skys.cobblemoncosmetics.hunt.HuntItemProperties;
import com.skys.cobblemoncosmetics.items.ModItems;
import com.skys.cobblemoncosmetics.render.IronMaidenMegaBraceletRenderer;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;

/**
 * Client-side initialization
 */
public class SkysCobblemonCosmeticsClient {
    public static void init() {
        // Iron Maiden Mega Bracelet - renders broom in off-hand
        AccessoriesRendererRegistry.registerRenderer(
            ModItems.IRON_MAIDEN_MEGA_BRACELET.get(),
            IronMaidenMegaBraceletRenderer::new
        );

        // Register hunt item properties for model overrides (orb states, tablet glow)
        HuntItemProperties.register();

        SkysCobblemonCosmetics.LOGGER.info("Client initialization complete");
    }
}
