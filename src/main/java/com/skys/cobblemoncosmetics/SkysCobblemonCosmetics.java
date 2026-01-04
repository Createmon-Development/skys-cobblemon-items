package com.skys.cobblemoncosmetics;

import com.skys.cobblemoncosmetics.items.ModItems;
import com.skys.cobblemoncosmetics.loot.ModLootModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SkysCobblemonCosmetics.MOD_ID)
public class SkysCobblemonCosmetics {
    public static final String MOD_ID = "skyscobblemonitems";
    public static final Logger LOGGER = LoggerFactory.getLogger(SkysCobblemonCosmetics.class);

    public SkysCobblemonCosmetics(IEventBus modEventBus) {
        LOGGER.info("Initializing Sky's Cobblemon Items");

        // Register items
        ModItems.register(modEventBus);

        // Register loot modifiers
        ModLootModifiers.register(modEventBus);

        LOGGER.info("Sky's Cobblemon Items initialized successfully");
    }
}
