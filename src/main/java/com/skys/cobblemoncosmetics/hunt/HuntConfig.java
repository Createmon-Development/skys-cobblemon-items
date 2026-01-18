package com.skys.cobblemoncosmetics.hunt;

/**
 * Configuration constants for the Crystal Ascendancy hunt
 */
public class HuntConfig {
    // Gameplay balance
    public static final int KILLS_PER_RUNE = 5;          // Pokemon defeats needed to reveal 1 rune
    public static final int TOTAL_RUNES = 10;            // Total runes to unscramble
    public static final long FAILURE_COOLDOWN_MS = 5 * 60 * 60 * 1000; // 5 hours

    // Rune thresholds for orb state changes
    public static final int RUNES_FOR_STAGE_1 = 1;       // After first rune revealed
    public static final int RUNES_FOR_HALF = 5;          // Halfway point
    public static final int RUNES_FOR_FINAL = 10;        // All runes revealed

    // Coordinate puzzle
    public static final int STAR_BEEP_RANGE = 30;        // Degrees from correct direction for audio cue
    public static final int ORIGIN_HUM_RANGE = 100;      // Blocks from world origin for audio cue

    // Runic text
    public static final String SCRAMBLED_RUNES = "ᛯᛡᛤᛢ ᛠᛣ ᛥᛦᛧᛨ - ᛩᛪ᛫᛬᛭ᛮ ᛰᛱ";
    public static final String UNSCRAMBLED_RUNES = "ᛛᛙᛞᛟ ᛝᛓ ᛕᛖᛊᛜ - ᛘᚴᛘᛪᚱᛆ ᛎᚱ";
    public static final String DECODED_MESSAGE = "xxxx yy zzzz - Awaken me";

    // Orb descriptions
    public static final String DESC_EMPTY = "The orb wishes to feel alive";
    public static final String DESC_FILLING = "Symbols of light writhe inside the orb";
    public static final String DESC_FILLED = "The symbols have aligned. Perhaps someone can read them...";

    // Tablet descriptions
    public static final String DESC_TABLET_FADED = "Ancient runes cover the surface, but they are too faded to read...";
    public static final String DESC_TABLET_GLOWING = "The runes glow with an ethereal light, revealing their secrets";
}
