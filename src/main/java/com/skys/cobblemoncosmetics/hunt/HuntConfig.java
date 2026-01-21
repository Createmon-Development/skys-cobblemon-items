package com.skys.cobblemoncosmetics.hunt;

/**
 * Configuration constants for the Crystal Ascendancy hunt
 */
public class HuntConfig {
    // Gameplay balance
    public static final int KILLS_PER_RUNE = 1;          // Pokemon defeats needed to reveal 1 rune
    public static final int TOTAL_RUNES = 18;            // Total runes to unscramble (xxxx yy zzzz - Awaken me)
    public static final long FAILURE_COOLDOWN_MS = 5 * 60 * 60 * 1000; // 5 hours

    // Rune thresholds for orb state changes (based on message structure)
    // Left side: "xxxx yy zzzz" = 10 runes (coordinates)
    // Right side: "Awaken me" = 8 runes (message)
    public static final int RUNES_FOR_STAGE_1 = 1;       // After first rune revealed
    public static final int RUNES_FOR_HALF = 10;         // Left side complete (coordinates revealed)
    public static final int RUNES_FOR_FINAL = 18;        // All runes revealed (full message)

    // Coordinate puzzle - Star puzzle (X coordinate)
    public static final int STAR_BEEP_RANGE = 45;        // Degrees from correct direction for audio cue (increased from 30)
    public static final int STAR_CLOSE_RANGE = 20;       // Degrees for "close" feedback (yellow) - more lenient
    public static final int STAR_EXACT_RANGE = 10;       // Degrees for "exact" feedback (green) - more lenient

    // Coordinate puzzle - Origin puzzle (Z coordinate)
    public static final int ORIGIN_HUM_RANGE = 100;      // Blocks from world origin for "soft" hum
    public static final int ORIGIN_HUM_MEDIUM = 50;      // Blocks from world origin for "loud" hum
    public static final int ORIGIN_HUM_CLOSE = 25;       // Blocks from world origin for "even louder" hum
    // Note: Origin exact detection requires standing exactly at 0,0 (X and Z both 0)

    // Cove of Origin coordinates (the target location for the hunt)
    public static final int COVE_X = 2137;               // X coordinate (revealed via stars)
    public static final int COVE_Y = -12;                // Y coordinate (hidden in custom tag)
    public static final int COVE_Z = -847;               // Z coordinate (revealed at world origin)

    // Individual digits for coordinate puzzles
    public static final String[] X_DIGITS = {"2", "1", "3", "7"};  // COVE_X = 2137
    public static final String[] Y_DIGITS = {"1", "2"};             // COVE_Y = -12 (shown as 12)
    public static final String[] Z_DIGITS = {"0", "8", "4", "7"};   // COVE_Z = -847 (shown as 0847)

    // Runic text - The revealed runes that decode to "xxxx yy zzzz - Awaken me" by default
    // Coordinates are revealed dynamically through puzzles
    // Using cipher: x=ᛪ y=ᛦ z=ᛉ - A=ᚨ w=ᚹ a=ᚨ k=ᚴ e=ᛖ n=ᚾ m=ᛗ e=ᛖ
    public static final String DEFAULT_RUNES = "ᛪᛪᛪᛪ ᛦᛦ ᛉᛉᛉᛉ - ᚨᚹᚨᚴᛖᚾ ᛗᛖ";
    public static final String DEFAULT_MESSAGE = "xxxx yy zzzz - Awaken me";

    // The actual rune sequence for coordinates (used when digits are revealed)
    // 2=ᛮ 1=ᛝ 3=ᛯ 7=ᚩ | 1=ᛝ 2=ᛮ | 0=ᛜ 8=ᚪ 4=ᛰ 7=ᚩ
    public static final String UNSCRAMBLED_RUNES = "ᛮᛝᛯᚩ ᛝᛮ ᛜᚪᛰᚩ - ᚨᚹᚨᚴᛖᚾ ᛗᛖ";
    public static final String DECODED_MESSAGE = "2137 12 0847 - Awaken me";

    // Full runic cipher alphabet (rune=letter format, lowercase) - ALL UNIQUE
    public static final String[] RUNIC_ALPHABET = {
        "ᚨ=a", "ᛒ=b", "ᚲ=c", "ᛞ=d", "ᛖ=e", "ᚠ=f", "ᚷ=g", "ᚺ=h", "ᛁ=i",
        "ᛃ=j", "ᚴ=k", "ᛚ=l", "ᛗ=m", "ᚾ=n", "ᛟ=o", "ᛈ=p", "ᚳ=q", "ᚱ=r",
        "ᛊ=s", "ᛏ=t", "ᚢ=u", "ᚡ=v", "ᚹ=w", "ᛪ=x", "ᛦ=y", "ᛉ=z"
    };

    // Numeric runes (rune=number format) - ALL UNIQUE, no overlap with letters
    public static final String[] RUNIC_NUMBERS = {
        "ᛜ=0", "ᛝ=1", "ᛮ=2", "ᛯ=3", "ᛰ=4", "ᚦ=5", "ᚧ=6", "ᚩ=7", "ᚪ=8", "ᚫ=9"
    };

    // Orb descriptions
    public static final String DESC_EMPTY = "The orb wishes to feel alive";
    public static final String DESC_FILLING = "Symbols of light writhe inside the orb";
    public static final String DESC_FILLED = "The symbols have aligned. Perhaps someone can read them...";

    // Tablet descriptions
    public static final String DESC_TABLET_FADED = "Ancient runes cover the surface, but they are too faded to read.";
    public static final String DESC_TABLET_GLOWING = "The runes glow with an ethereal light, revealing their secrets.";
}
