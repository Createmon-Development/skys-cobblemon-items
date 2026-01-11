package com.skys.cobblemoncosmetics;

import com.skys.cobblemoncosmetics.items.ModItems;
import com.skys.cobblemoncosmetics.render.IronMaidenMegaBraceletRenderer;
import com.skys.cobblemoncosmetics.render.SheathBeltRenderer;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;

/**
 * Client-side initialization
 * TEST: Contains test keystone bracelet renderer registration
 */
public class SkysCobblemonCosmeticsClient {
    public static void init() {
        // TEST - Remove when testing is complete
        // Using SheathBeltRenderer to position corn stalk like a sheathed sword on belt
        AccessoriesRendererRegistry.registerRenderer(
            ModItems.TEST_KEYSTONE_BRACELET.get(),
            SheathBeltRenderer::new
        );

        // Iron Maiden Mega Bracelet - renders broom in off-hand
        AccessoriesRendererRegistry.registerRenderer(
            ModItems.IRON_MAIDEN_MEGA_BRACELET.get(),
            IronMaidenMegaBraceletRenderer::new
        );

        SkysCobblemonCosmetics.LOGGER.info("Client initialization complete");
    }
}
