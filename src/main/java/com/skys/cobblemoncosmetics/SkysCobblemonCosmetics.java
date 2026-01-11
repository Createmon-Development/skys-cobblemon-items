package com.skys.cobblemoncosmetics;

import com.skys.cobblemoncosmetics.hunt.CobaltAscendancyEvents;
import com.skys.cobblemoncosmetics.hunt.HuntDataComponents;
import com.skys.cobblemoncosmetics.items.ModItems;
import com.skys.cobblemoncosmetics.loot.ModLootModifiers;
import com.skys.cobblemoncosmetics.sounds.ModSounds;
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

        // Register data components (must be before items that use them)
        HuntDataComponents.register(modEventBus);

        // Register items
        ModItems.register(modEventBus);

        // Register loot modifiers
        ModLootModifiers.register(modEventBus);

        // Register sounds
        ModSounds.register(modEventBus);

        // Register Cobalt Ascendancy hunt events (subscribes to Cobblemon events)
        new CobaltAscendancyEvents();

        LOGGER.info("Sky's Cobblemon Items initialized successfully");
    }
}
