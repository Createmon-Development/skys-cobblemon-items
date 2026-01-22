package com.skys.cobblemoncosmetics;

import com.skys.cobblemoncosmetics.blocks.ModBlocks;
import com.skys.cobblemoncosmetics.hunt.BattleFaintHandler;
import com.skys.cobblemoncosmetics.hunt.HuntDataComponents;
import com.skys.cobblemoncosmetics.items.ModItems;
import com.skys.cobblemoncosmetics.loot.ModLootModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
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

        // Register blocks
        ModBlocks.register(modEventBus);

        // Register hunt data components
        HuntDataComponents.register(modEventBus);

        // TEST - Client setup for renderer registration
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }

        // Register loot modifiers
        ModLootModifiers.register(modEventBus);

        // Register battle faint handler for Crystal Ascendancy hunt
        BattleFaintHandler.register();

        LOGGER.info("Sky's Cobblemon Items initialized successfully");
    }

    // TEST - Remove when testing is complete
    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SkysCobblemonCosmeticsClient::init);
    }
}
