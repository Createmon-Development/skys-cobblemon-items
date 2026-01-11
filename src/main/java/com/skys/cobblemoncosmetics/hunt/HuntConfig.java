package com.skys.cobblemoncosmetics.hunt;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration constants for the Cobalt Ascendancy scavenger hunt.
 * These can be adjusted to customize the hunt experience.
 */
public class HuntConfig {
    // --- Coordinates (to be configured per-server) ---

    // Location of the ocean trial chamber
    public static BlockPos TRIAL_CHAMBER_COORDS = new BlockPos(0, 64, 0);

    // Location of the mystical cove where the hunt concludes
    public static BlockPos COVE_COORDS = new BlockPos(0, 30, 0);

    // --- Gameplay Settings ---

    // Number of Pokemon defeats required to reveal one letter
    public static final int KILLS_PER_LETTER = 1;

    // The secret message encoded in the orb (spaces are preserved, only letters are scrambled)
    // Coordinates and the message. Cords will be added later and all will be scrambled
    public static final String SECRET_MESSAGE = "xxxx xx xxxx - Awaken Me";

    // Total number of letters to reveal (excluding spaces)
    public static final int TOTAL_LETTERS = SECRET_MESSAGE.replace(" ", "").replace("-", "").length();

    // Total kills needed to fully reveal the message
    public static final int TOTAL_KILLS_NEEDED = TOTAL_LETTERS * KILLS_PER_LETTER;

    // --- Pokemon Reward Settings ---

    // The Pokemon species to spawn as the reward
    public static final String REWARD_POKEMON = "kyogre";

    // Level of the reward Pokemon
    public static final int REWARD_POKEMON_LEVEL = 70;

    // Whether the reward Pokemon should be shiny
    public static final boolean REWARD_POKEMON_SHINY = false;

    // --- Runic Cipher System ---
    // Maps characters to runic symbols for the cipher puzzle
    // Version 3 mapping - shuffled for mystery
    public static final Map<Character, Character> RUNE_CIPHER = new HashMap<>();
    static {
        // Letters (A-Z) -> Runic symbols
        RUNE_CIPHER.put('A', 'ᛘ');
        RUNE_CIPHER.put('B', 'ᚾ');
        RUNE_CIPHER.put('C', 'ᛔ');
        RUNE_CIPHER.put('D', 'ᛩ');
        RUNE_CIPHER.put('E', 'ᚱ');
        RUNE_CIPHER.put('F', 'ᛊ');
        RUNE_CIPHER.put('G', 'ᛐ');
        RUNE_CIPHER.put('H', 'ᚢ');
        RUNE_CIPHER.put('I', 'ᚡ');
        RUNE_CIPHER.put('J', 'ᚥ');
        RUNE_CIPHER.put('K', 'ᛪ');
        RUNE_CIPHER.put('L', 'ᚤ');
        RUNE_CIPHER.put('M', 'ᛎ');
        RUNE_CIPHER.put('N', 'ᛆ');
        RUNE_CIPHER.put('O', 'ᛒ');
        RUNE_CIPHER.put('P', 'ᛍ');
        RUNE_CIPHER.put('Q', 'ᛑ');
        RUNE_CIPHER.put('R', 'ᛂ');
        RUNE_CIPHER.put('S', 'ᚠ');
        RUNE_CIPHER.put('T', 'ᚺ');
        RUNE_CIPHER.put('U', 'ᛁ');
        RUNE_CIPHER.put('V', 'ᛃ');
        RUNE_CIPHER.put('W', 'ᚴ');
        RUNE_CIPHER.put('X', 'ᛚ');
        RUNE_CIPHER.put('Y', 'ᚮ');
        RUNE_CIPHER.put('Z', 'ᛗ');

        // Numbers (0-9) -> Runic symbols
        RUNE_CIPHER.put('0', 'ᛜ');
        RUNE_CIPHER.put('1', 'ᛛ');
        RUNE_CIPHER.put('2', 'ᛙ');
        RUNE_CIPHER.put('3', 'ᛞ');
        RUNE_CIPHER.put('4', 'ᛟ');
        RUNE_CIPHER.put('5', 'ᛝ');
        RUNE_CIPHER.put('6', 'ᛓ');
        RUNE_CIPHER.put('7', 'ᛕ');
        RUNE_CIPHER.put('8', 'ᛖ');
        RUNE_CIPHER.put('9', 'ᛊ');
    }

    /**
     * Converts a character to its runic cipher equivalent.
     * Returns the original character if no mapping exists (for spaces, punctuation, etc.)
     */
    public static char toRune(char c) {
        char upper = Character.toUpperCase(c);
        return RUNE_CIPHER.getOrDefault(upper, c);
    }

    /**
     * Converts an entire string to runic cipher.
     */
    public static String toRunicString(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(toRune(c));
        }
        return result.toString();
    }

    // --- Visual Settings ---

    // Characters used for scrambled/unrevealed letters (random runic noise)
    public static final char[] SCRAMBLE_CHARS = {
        'ᛆ', 'ᛒ', 'ᛍ', 'ᛑ', 'ᛂ', 'ᚠ', 'ᚵ', 'ᚺ', 'ᛁ', 'ᛃ',
        'ᚴ', 'ᛚ', 'ᛘ', 'ᚾ', 'ᚮ', 'ᛔ', 'ᛩ', 'ᚱ', 'ᛊ', 'ᛐ'
    };

    // Characters used for the glitching name effect
    public static final char[] GLITCH_CHARS = {
        '\u0E47', '\u0E48', '\u0E49', '\u0E4A', '\u0E4B',
        '\u033F', '\u0346', '\u034A', '\u034B', '\u034C',
        '\u0489', '\u0359', '\u035A', '\u0317', '\u0318'
    };

    // --- Broadcast Messages ---

    public static final String HUNT_START_MESSAGE = "An archaic roar fills the sky. The race for the Cobalt Ascendancy has begun!";
    public static final String PLAYER_JOINED_MESSAGE = "%s has entered the race for the Cobalt Ascendancy!";
    public static final String HUNT_COMPLETE_MESSAGE = "%s has awakened the Cobalt Guardian!";
    public static final String ORB_FILLED_MESSAGE = "The orb glows with clarity. Someone may know more about this...";
    public static final String WHISPER_MESSAGE = "The orb whispers...";
}
