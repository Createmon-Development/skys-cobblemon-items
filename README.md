# Sky's Cobblemon Items

Mod that adds in multiple items including badges, fishing treasure, event tickets, and more.

# Artwork credit

All non-badge sprites made by [Sharcys](gaywhiteboy.com)

## Badges:
Button Badge - Tokki
Engine Badge - Spectrum
Focus Badge - BlakeCollie
Heartbound Badge - CinnaSwirls
Midnight Badge - Tenchu 
Rock Badge - ImAnAnt
Wisteria Badge - WoolFeathers

## Features

- **Custom Gym Badges**: Collectible badge items that can be awarded to players
  - Dieselton Badge - "Smells of smog and soot"
  - Heartbound Badge - "Rewarded to those whose hearts are unbound by their desires"

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1+

## Installation

1. Download the mod JAR from the releases page
2. Place it in your `mods` folder
3. Launch Minecraft with NeoForge

## For Mod Developers

### Adding New Badges

1. Register the badge in `ModItems.java`
2. Create a model JSON file in `src/main/resources/assets/skyscobblemoncosmetics/models/item/`
3. Add a texture PNG file in `src/main/resources/assets/skyscobblemoncosmetics/textures/item/`
4. Add translations in `src/main/resources/assets/skyscobblemoncosmetics/lang/en_us.json`

## Building

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/skys-cobblemon-cosmetics-1.0.0.jar`

## License

MIT
