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

    // TEST ITEM - Remove when testing is complete
    public static final DeferredItem<Item> TEST_KEYSTONE_BRACELET = ITEMS.register("test_keystone_bracelet",
        () -> new TestKeystoneBracelet(new Item.Properties().stacksTo(1)));

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
            output.accept(TEST_KEYSTONE_BRACELET.get()); // TEST - Remove when done
        }).build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
        SkysCobblemonCosmetics.LOGGER.info("Registered {} custom items", ITEMS.getEntries().size());
    }
}
