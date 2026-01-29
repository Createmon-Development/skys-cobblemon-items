package com.skys.cobblemoncosmetics.items;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.blocks.ModBlocks;
import com.skys.cobblemoncosmetics.hunt.MysteriousOrbItem;
import com.skys.cobblemoncosmetics.hunt.MysteriousParchmentItem;
import com.skys.cobblemoncosmetics.hunt.RunicCipherTabletItem;
import com.skys.cobblemoncosmetics.hunt.TempleTreasureMapItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for all custom items in the mod.
 * Add new items here following the example pattern.
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkysCobblemonCosmetics.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkysCobblemonCosmetics.MOD_ID);

    // Custom gym badge items
    public static final DeferredItem<Item> ENGINE_BADGE = ITEMS.register("engine_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    public static final DeferredItem<Item> HEARTBOUND_BADGE = ITEMS.register("heartbound_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    public static final DeferredItem<Item> BUTTON_BADGE = ITEMS.register("button_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    public static final DeferredItem<Item> FOCUS_BADGE = ITEMS.register("focus_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    public static final DeferredItem<Item> WISTERIA_BADGE = ITEMS.register("wisteria_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    public static final DeferredItem<Item> ROCK_BADGE = ITEMS.register("rock_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    public static final DeferredItem<Item> MIDNIGHT_BADGE = ITEMS.register("midnight_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    public static final DeferredItem<Item> BEAT_BADGE = ITEMS.register("beat_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    // Iron Maiden Mega Bracelet - Broom-styled mega bracelet
    public static final DeferredItem<Item> IRON_MAIDEN_MEGA_BRACELET = ITEMS.register("iron_maiden_mega_bracelet",
        () -> new IronMaidenMegaBracelet(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> GOOMY_BADGE = ITEMS.register("goomy_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    // Fishing items
    public static final DeferredItem<Item> MISSING_CLOVER = ITEMS.register("missing_clover",
        () -> new MissingCloverItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> RUSTED_GOLD_POKEBALL = ITEMS.register("rusted_gold_pokeball",
        () -> new TooltipItem(new Item.Properties()));

    public static final DeferredItem<Item> POLISHED_GOLD_POKEBALL = ITEMS.register("polished_gold_pokeball",
        () -> new TooltipItem(new Item.Properties()));

    // Event items
    public static final DeferredItem<Item> EVENT_TICKET_RED = ITEMS.register("event_ticket_red",
        () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> EVENT_TICKET_PURPLE = ITEMS.register("event_ticket_purple",
        () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> EVENT_TICKET_GREEN = ITEMS.register("event_ticket_green",
        () -> new Item(new Item.Properties()));

    // Cobalt Ascendancy Hunt items
    public static final DeferredItem<Item> MYSTERIOUS_ORB = ITEMS.register("mysterious_orb",
        () -> new MysteriousOrbItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> RUNIC_CIPHER_TABLET = ITEMS.register("runic_cipher_tablet",
        () -> new RunicCipherTabletItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> MYSTERIOUS_PARCHMENT = ITEMS.register("mysterious_parchment",
        () -> new MysteriousParchmentItem(new Item.Properties().stacksTo(1)));

    // Temple Treasure Map - Given by Treasure Hunter's Assistant during step 3
    // NOTE: This creates a vanilla filled_map with custom decorations when given to player
    public static final DeferredItem<Item> TEMPLE_TREASURE_MAP = ITEMS.register("temple_treasure_map",
        () -> new TempleTreasureMapItem(new Item.Properties().stacksTo(1)));

    // Liquid Assimilation Potion - Brewed in a Keg from Brewin' n Chewin'
    // Made with black concrete liquid, black dye, and slime ball
    public static final DeferredItem<Item> LIQUID_ASSIMILATION_POTION = ITEMS.register("liquid_assimilation_potion",
        () -> new LiquidAssimilationPotionItem(new Item.Properties().stacksTo(16), false, 0));

    // Lingering variant - throwable, made by adding dragon's breath to the base potion
    // Default duration: 45 seconds
    public static final DeferredItem<Item> LINGERING_LIQUID_ASSIMILATION_POTION = ITEMS.register("lingering_liquid_assimilation_potion",
        () -> new LiquidAssimilationPotionItem(new Item.Properties().stacksTo(16), true, 45));

    // Extended lingering variant - made by adding redstone to the lingering potion
    // Extended duration: 2 minutes (120 seconds)
    public static final DeferredItem<Item> LINGERING_LIQUID_ASSIMILATION_POTION_EXTENDED = ITEMS.register("lingering_liquid_assimilation_potion_extended",
        () -> new LiquidAssimilationPotionItem(new Item.Properties().stacksTo(16), true, 120));

    // Creeping variant - made by adding vine to the lingering potion
    // Can be renamed in anvil to target a specific player (case insensitive)
    // Default duration: 45 seconds
    public static final DeferredItem<Item> CREEPING_LIQUID_ASSIMILATION_POTION = ITEMS.register("creeping_liquid_assimilation_potion",
        () -> new CreepingAssimilationPotionItem(new Item.Properties().stacksTo(16), 45));

    // Extended creeping variant - made by adding redstone to the creeping potion
    // Extended duration: 2 minutes (120 seconds)
    public static final DeferredItem<Item> CREEPING_LIQUID_ASSIMILATION_POTION_EXTENDED = ITEMS.register("creeping_liquid_assimilation_potion_extended",
        () -> new CreepingAssimilationPotionItem(new Item.Properties().stacksTo(16), 120));

    // Creative tab for the mod items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COBBLEMON_COSMETICS_TAB = CREATIVE_MODE_TABS.register("cobblemon_cosmetics_tab", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.skyscobblemonitems"))
        .icon(() -> new ItemStack(ENGINE_BADGE.get()))
        .displayItems((parameters, output) -> {
            output.accept(ENGINE_BADGE.get());
            output.accept(HEARTBOUND_BADGE.get());
            output.accept(BUTTON_BADGE.get());
            output.accept(FOCUS_BADGE.get());
            output.accept(WISTERIA_BADGE.get());
            output.accept(ROCK_BADGE.get());
            output.accept(MIDNIGHT_BADGE.get());
            output.accept(BEAT_BADGE.get());
            output.accept(IRON_MAIDEN_MEGA_BRACELET.get());
            output.accept(GOOMY_BADGE.get());
            output.accept(MISSING_CLOVER.get());
            output.accept(RUSTED_GOLD_POKEBALL.get());
            output.accept(POLISHED_GOLD_POKEBALL.get());
            output.accept(EVENT_TICKET_RED.get());
            output.accept(EVENT_TICKET_PURPLE.get());
            output.accept(EVENT_TICKET_GREEN.get());
            output.accept(MYSTERIOUS_ORB.get());
            output.accept(RUNIC_CIPHER_TABLET.get());
            output.accept(MYSTERIOUS_PARCHMENT.get());
            output.accept(TEMPLE_TREASURE_MAP.get());
            output.accept(LIQUID_ASSIMILATION_POTION.get());
            output.accept(LINGERING_LIQUID_ASSIMILATION_POTION.get());
            output.accept(LINGERING_LIQUID_ASSIMILATION_POTION_EXTENDED.get());
            output.accept(CREEPING_LIQUID_ASSIMILATION_POTION.get());
            output.accept(CREEPING_LIQUID_ASSIMILATION_POTION_EXTENDED.get());
            // Block items
            output.accept(ModBlocks.RUNIC_ALTAR_ITEM.get());
        }).build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
        SkysCobblemonCosmetics.LOGGER.info("Registered {} custom items", ITEMS.getEntries().size());
    }
}
