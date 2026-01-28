package com.skys.cobblemoncosmetics;

import com.skys.cobblemoncosmetics.blocks.ModBlocks;
import com.skys.cobblemoncosmetics.entity.ModEntities;
import com.skys.cobblemoncosmetics.fluid.ModFluids;
import com.skys.cobblemoncosmetics.hunt.BattleFaintHandler;
import com.skys.cobblemoncosmetics.hunt.HuntDataComponents;
import com.skys.cobblemoncosmetics.hunt.KyogreBattleManager;
import com.skys.cobblemoncosmetics.items.ModItems;
import com.skys.cobblemoncosmetics.loot.ModLootModifiers;
import com.skys.cobblemoncosmetics.recipe.ModBrewingRecipes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
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

        // Register entities
        ModEntities.register(modEventBus);

        // Register fluids
        ModFluids.register(modEventBus);

        // Register hunt data components
        HuntDataComponents.register(modEventBus);

        // TEST - Client setup for renderer registration
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }

        // Register loot modifiers
        ModLootModifiers.register(modEventBus);

        // Brewing recipes disabled - using crafting recipes instead
        // ModBrewingRecipes.register();

        // Register battle faint handler for Cobalt Ascendancy hunt
        BattleFaintHandler.register();

        // Register Kyogre battle manager for boss encounter
        KyogreBattleManager.register();

        // Register entity tick event for Kyogre summoning detection
        NeoForge.EVENT_BUS.addListener(this::onEntityTick);

        // Register player death event for Kyogre despawn
        NeoForge.EVENT_BUS.addListener(this::onPlayerDeath);

        LOGGER.info("Sky's Cobblemon Items initialized successfully");
    }

    private void onEntityTick(EntityTickEvent.Post event) {
        KyogreBattleManager.onEntityTick(event);
    }

    private void onPlayerDeath(LivingDeathEvent event) {
        KyogreBattleManager.onPlayerDeath(event);
    }

    // TEST - Remove when testing is complete
    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SkysCobblemonCosmeticsClient::init);
    }
}
