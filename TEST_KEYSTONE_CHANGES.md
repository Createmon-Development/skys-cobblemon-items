# Test Keystone Bracelet - Changes Made

This document tracks all changes made for testing the custom keystone bracelet functionality.
**DELETE ALL THESE WHEN TESTING IS COMPLETE**

## Files to DELETE:

1. **Java Classes:**
   - `src/main/java/com/skys/cobblemoncosmetics/items/TestKeystoneBracelet.java`
   - `src/main/java/com/skys/cobblemoncosmetics/render/TestKeystoneBraceletRenderer.java`
   - `src/main/java/com/skys/cobblemoncosmetics/SkysCobblemonCosmeticsClient.java`

2. **Model JSONs:**
   - `src/main/resources/assets/skyscobblemonitems/models/item/test_keystone_bracelet.json`
   - `src/main/resources/assets/skyscobblemonitems/models/item/test_keystone_bracelet_3d.json`

3. **Item Tag:**
   - `src/main/resources/data/mega_showdown/tags/item/mega_bracelet.json`

4. **This file:**
   - `TEST_KEYSTONE_CHANGES.md`

---

## Files to EDIT (Remove test-related code):

### 1. `build.gradle`
**Lines to remove:**
- Lines 18-20: Maven repositories (Accessories, Modrinth, Cobblemon)
- Lines 42-46: Test dependencies (compileOnly lines)

**Before (lines 16-21):**
```gradle
repositories {
    mavenCentral()
    maven { url 'https://maven.wispforest.io/releases' } // Accessories API
    maven { url 'https://api.modrinth.com/maven' } // Modrinth - for MegaShowdown and Cobblemon
    maven { url 'https://maven.impactdev.net/repository/development/' } // Cobblemon
}
```

**After:**
```gradle
repositories {
    mavenCentral()
}
```

**Before (lines 39-46):**
```gradle
dependencies {
    implementation "net.neoforged:neoforge:21.1.72"

    // TEST DEPENDENCIES - Remove these when done testing
    compileOnly "maven.modrinth:accessories:1.1.0-beta.52+1.21.1" // Accessories API
    compileOnly "maven.modrinth:mega-showdown:1.5.1+1.7.1+1.21.1-b-2" // MegaShowdown
    compileOnly "com.cobblemon:neoforge:1.7.1+1.21.1" // Cobblemon
}
```

**After:**
```gradle
dependencies {
    implementation "net.neoforged:neoforge:21.1.72"
}
```

---

### 2. `src/main/java/com/skys/cobblemoncosmetics/items/ModItems.java`

**Lines to remove:**
- Lines 44-46: TEST_KEYSTONE_BRACELET registration
- Line 60: Creative tab entry

**Before (lines 41-46):**
```java
    public static final DeferredItem<Item> MIDNIGHT_BADGE = ITEMS.register("midnight_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    // TEST ITEM - Remove when testing is complete
    public static final DeferredItem<Item> TEST_KEYSTONE_BRACELET = ITEMS.register("test_keystone_bracelet",
        () -> new TestKeystoneBracelet(new Item.Properties().stacksTo(1)));
```

**After:**
```java
    public static final DeferredItem<Item> MIDNIGHT_BADGE = ITEMS.register("midnight_badge",
        () -> new GymBadgeItem(new Item.Properties()));
```

**Before (lines 52-61):**
```java
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
```

**After:**
```java
        .displayItems((parameters, output) -> {
            output.accept(ENGINE_BADGE.get());
            output.accept(HEARTBOUND_BADGE.get());
            output.accept(BUTTON_BADGE.get());
            output.accept(FOCUS_BADGE.get());
            output.accept(WISTERIA_BADGE.get());
            output.accept(ROCK_BADGE.get());
            output.accept(MIDNIGHT_BADGE.get());
        }).build());
```

---

### 3. `src/main/resources/assets/skyscobblemonitems/lang/en_us.json`

**Lines to remove:** Lines 16-17

**Before:**
```json
  "item.skyscobblemonitems.midnight_badge": "Midnight Badge",
  "item.skyscobblemonitems.midnight_badge.tooltip": "For those that bump in the night.",
  "item.skyscobblemonitems.test_keystone_bracelet": "Test Keystone Bracelet",
  "tooltip.skyscobblemonitems.test_keystone_bracelet.tooltip": "TEST ITEM - Enables Mega Evolution",
  "itemGroup.skyscobblemonitems": "Sky's Cobblemon Items"
```

**After:**
```json
  "item.skyscobblemonitems.midnight_badge": "Midnight Badge",
  "item.skyscobblemonitems.midnight_badge.tooltip": "For those that bump in the night.",
  "itemGroup.skyscobblemonitems": "Sky's Cobblemon Items"
```

---

### 4. `src/main/java/com/skys/cobblemoncosmetics/SkysCobblemonCosmetics.java`

**Lines to remove:** Lines 4-8, 23-34

**Before:**
```java
import com.skys.cobblemoncosmetics.items.ModItems;
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

        // TEST - Client setup for renderer registration
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }

        LOGGER.info("Sky's Cobblemon Items initialized successfully");
    }

    // TEST - Remove when testing is complete
    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SkysCobblemonCosmeticsClient::init);
    }
}
```

**After:**
```java
import com.skys.cobblemoncosmetics.items.ModItems;
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

        LOGGER.info("Sky's Cobblemon Items initialized successfully");
    }
}
```

---

## How to Test:

1. Make sure MegaShowdown and Accessories mods are installed in your mods folder
2. Build and run the mod
3. Get the "Test Keystone Bracelet" from the creative tab
4. Right-click to equip it to your mega_slot
5. In battle with a Pokemon holding a Mega Stone, the mega evolution button should appear
6. Test mega evolution functionality

---

## Summary of Implementation:

The test keystone bracelet works by:
1. **Java Class** (`TestKeystoneBracelet.java`) - Implements right-click to equip functionality using Accessories API
2. **Item Registration** (`ModItems.java`) - Registers the item with the mod
3. **Item Tag** (`mega_bracelet.json`) - Tags our item so MegaShowdown recognizes it as a valid mega bracelet
4. **Resources** (model JSON, texture, lang file) - Provides visuals and text

The critical part is the **item tag** - any item tagged with `mega_showdown:mega_bracelet` will enable mega evolution!
