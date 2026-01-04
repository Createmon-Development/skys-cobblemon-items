package com.skys.cobblemoncosmetics.items;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
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

    public static final DeferredItem<Item> GOOMY_BADGE = ITEMS.register("goomy_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    // Fishing items
    public static final DeferredItem<Item> MISSING_CLOVER = ITEMS.register("missing_clover",
        () -> new Item(new Item.Properties().stacksTo(1)));

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
            output.accept(GOOMY_BADGE.get());
            output.accept(MISSING_CLOVER.get());
            output.accept(RUSTED_GOLD_POKEBALL.get());
            output.accept(POLISHED_GOLD_POKEBALL.get());
            output.accept(EVENT_TICKET_RED.get());
            output.accept(EVENT_TICKET_PURPLE.get());
            output.accept(EVENT_TICKET_GREEN.get());
        }).build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
        SkysCobblemonCosmetics.LOGGER.info("Registered {} custom items", ITEMS.getEntries().size());
    }
}
