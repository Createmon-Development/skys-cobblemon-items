package com.skys.cobblemoncosmetics.hunt;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers custom item properties for model overrides (orb state, tablet glow)
 */
public class HuntItemProperties {

    public static void register() {
        // Orb state property (0.0 = empty, 0.25 = stage1, 0.5 = half, 0.75 = final)
        ItemProperties.register(
            ModItems.MYSTERIOUS_ORB.get(),
            ResourceLocation.fromNamespaceAndPath(SkysCobblemonCosmetics.MOD_ID, "orb_state"),
            (stack, level, entity, seed) -> {
                HuntDataComponents.OrbState state = MysteriousOrbItem.getOrbState(stack);
                return state.getValue() * 0.25f;
            }
        );

        // Tablet glowing property (0.0 = faded, 1.0 = glowing)
        ItemProperties.register(
            ModItems.RUNIC_CIPHER_TABLET.get(),
            ResourceLocation.fromNamespaceAndPath(SkysCobblemonCosmetics.MOD_ID, "tablet_glowing"),
            (stack, level, entity, seed) -> RunicCipherTabletItem.isGlowing(stack) ? 1.0f : 0.0f
        );

        SkysCobblemonCosmetics.LOGGER.info("Registered hunt item properties");
    }
}
