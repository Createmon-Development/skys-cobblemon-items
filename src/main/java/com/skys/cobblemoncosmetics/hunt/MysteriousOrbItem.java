package com.skys.cobblemoncosmetics.hunt;

import com.skys.cobblemoncosmetics.hunt.HuntDataComponents.OrbState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Random;

/**
 * The Mysterious Orb - main quest item for the Cobalt Ascendancy hunt.
 * Changes appearance based on how many runes have been revealed through battle.
 * Coordinate digits are revealed through puzzles and displayed with color coding.
 */
public class MysteriousOrbItem extends Item {

    // Characters used for scrambled rune display (must NOT overlap with cipher runes)
    private static final char[] SCRAMBLE_CHARS = {
        'ᛡ', 'ᛤ', 'ᛢ', 'ᛠ', 'ᛣ', 'ᛥ', 'ᛧ', 'ᛨ', 'ᛩ', '᛫', '᛬', '᛭', 'ᛱ', 'ᚬ', 'ᚭ', 'ᚮ', 'ᚯ', 'ᚰ'
    };

    // Placeholder runes for unrevealed coordinates
    private static final char X_PLACEHOLDER = 'ᛪ'; // x
    private static final char Y_PLACEHOLDER = 'ᛦ'; // y
    private static final char Z_PLACEHOLDER = 'ᛉ'; // z

    // The actual rune sequence for "Awaken me" (always shown after orb is filled)
    private static final String AWAKEN_ME_RUNES = "ᚨᚹᚨᚴᛖᚾ ᛗᛖ";

    // Runic digits: 0=ᛜ 1=ᛝ 2=ᛮ 3=ᛯ 4=ᛰ 5=ᚦ 6=ᚧ 7=ᚩ 8=ᚪ 9=ᚫ
    private static final char[] RUNIC_DIGITS = {'ᛜ', 'ᛝ', 'ᛮ', 'ᛯ', 'ᛰ', 'ᚦ', 'ᚧ', 'ᚩ', 'ᚪ', 'ᚫ'};

    // Moon phase to X digit position mapping
    // Each moon phase (0-7) reveals a different X digit position
    // Phase 0=3rd, 1=2nd, 2=4th, 3=1st, 4=2nd, 5=1st, 6=4th, 7=3rd (positions are 0-indexed)
    private static final int[] MOON_PHASE_TO_X_POSITION = {2, 1, 3, 0, 1, 0, 3, 2};

    public MysteriousOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        OrbState state = getOrbState(stack);

        if (state == OrbState.EMPTY) {
            // Glitchy/shifting name effect for unfilled orb
            return createGlitchyName();
        }

        return Component.translatable(this.getDescriptionId(stack));
    }

    /**
     * Creates a glitchy name that shifts characters randomly
     */
    private Component createGlitchyName() {
        String baseName = "Mysterious Orb";
        StringBuilder result = new StringBuilder();
        Random rand = new Random(System.currentTimeMillis() / 100); // Changes every 100ms

        for (char c : baseName.toCharArray()) {
            if (c == ' ') {
                result.append(' ');
            } else if (rand.nextFloat() < 0.25f) {
                // 25% chance to glitch each character
                result.append(SCRAMBLE_CHARS[rand.nextInt(SCRAMBLE_CHARS.length)]);
            } else {
                result.append(c);
            }
        }

        return Component.literal(result.toString()).withStyle(ChatFormatting.DARK_PURPLE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        OrbState state = getOrbState(stack);
        int revealedRunes = getRevealedRunes(stack);
        int proximity = getProximity(stack);

        // Check if orb is completed (has edition) - show simplified trophy text
        Integer edition = getEdition(stack);
        if (edition != null && edition > 0) {
            // Completed orb - show only trophy description and edition
            tooltipComponents.add(
                Component.literal("A trophy to those who have calmed the writhing waves.")
                    .withStyle(ChatFormatting.AQUA));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("Edition: #" + edition).withStyle(ChatFormatting.DARK_GRAY));
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
            return;
        }

        // Add description based on state (for non-completed orbs)
        switch (state) {
            case EMPTY -> tooltipComponents.add(
                Component.literal(HuntConfig.DESC_EMPTY).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            case STAGE_1, HALF -> {
                tooltipComponents.add(
                    Component.literal(HuntConfig.DESC_FILLING).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
                tooltipComponents.add(Component.empty());
                tooltipComponents.add(createRuneDisplay(stack, revealedRunes, proximity));
                // No coordinate line yet - orb not filled
            }
            case FINAL -> {
                tooltipComponents.add(
                    Component.literal(HuntConfig.DESC_FILLED).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                tooltipComponents.add(Component.empty());
                tooltipComponents.add(createRuneDisplay(stack, revealedRunes, proximity));
                // Show the shifting coordinate line below the static template
                tooltipComponents.add(createCoordinateDisplay(stack, proximity));
            }
        }

        // Hidden Y coordinate tag - poem fitting "fathom" reference
        // The poem says "A new homes fathom" - a fathom is 6 feet, 12 fathoms = 72 feet
        // But we hide the Y value (-12) as "fathom_mark"
        if (tooltipFlag.isAdvanced() && state == OrbState.FINAL) {
            tooltipComponents.add(Component.empty());
            String fathomMark = getFathomMark(stack);
            if (fathomMark != null && !fathomMark.isEmpty()) {
                tooltipComponents.add(Component.literal("fathom_mark: " + fathomMark).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * Creates the decoded message template line: "xxxx yy zzzz - Awaken me"
     * - EMPTY: Everything scrambles
     * - STAGE_1/HALF (filling): xxxx yy zzzz scrambles, "Awaken me" is stable
     * - FINAL (filled): xxxx yy zzzz becomes static placeholder runes, "Awaken me" stable
     */
    private Component createRuneDisplay(ItemStack stack, int revealedCount, int proximity) {
        MutableComponent display = Component.empty();
        OrbState state = getOrbState(stack);
        Random rand = new Random(System.currentTimeMillis() / 150); // Shift every 150ms

        if (state == OrbState.FINAL) {
            // FINAL state: Show STATIC placeholder runes for coordinates
            // X placeholder (4 chars) - static
            for (int i = 0; i < 4; i++) {
                display.append(Component.literal(String.valueOf(X_PLACEHOLDER)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            display.append(Component.literal(" ").withStyle(ChatFormatting.LIGHT_PURPLE));
            // Y placeholder (2 chars) - static
            for (int i = 0; i < 2; i++) {
                display.append(Component.literal(String.valueOf(Y_PLACEHOLDER)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            display.append(Component.literal(" ").withStyle(ChatFormatting.LIGHT_PURPLE));
            // Z placeholder (4 chars) - static
            for (int i = 0; i < 4; i++) {
                display.append(Component.literal(String.valueOf(Z_PLACEHOLDER)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            display.append(Component.literal(" - ").withStyle(ChatFormatting.LIGHT_PURPLE));
            // "Awaken me" is stable
            display.append(Component.literal(AWAKEN_ME_RUNES).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (revealedCount > 0) {
            // STAGE_1/HALF: Coordinates still scramble, but "Awaken me" is stable
            // X placeholder (4 chars) - scrambling
            for (int i = 0; i < 4; i++) {
                char scrambled = SCRAMBLE_CHARS[rand.nextInt(SCRAMBLE_CHARS.length)];
                display.append(Component.literal(String.valueOf(scrambled)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            display.append(Component.literal(" ").withStyle(ChatFormatting.LIGHT_PURPLE));
            // Y placeholder (2 chars) - scrambling
            for (int i = 0; i < 2; i++) {
                char scrambled = SCRAMBLE_CHARS[rand.nextInt(SCRAMBLE_CHARS.length)];
                display.append(Component.literal(String.valueOf(scrambled)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            display.append(Component.literal(" ").withStyle(ChatFormatting.LIGHT_PURPLE));
            // Z placeholder (4 chars) - scrambling
            for (int i = 0; i < 4; i++) {
                char scrambled = SCRAMBLE_CHARS[rand.nextInt(SCRAMBLE_CHARS.length)];
                display.append(Component.literal(String.valueOf(scrambled)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            display.append(Component.literal(" - ").withStyle(ChatFormatting.LIGHT_PURPLE));
            // "Awaken me" is now stable/revealed
            display.append(Component.literal(AWAKEN_ME_RUNES).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            // EMPTY: Everything scrambles including "Awaken me"
            // Format: "xxxx yy zzzz - awaken me"
            int totalLength = 4 + 1 + 2 + 1 + 4 + 3 + AWAKEN_ME_RUNES.length();
            StringBuilder scrambled = new StringBuilder();
            for (int i = 0; i < totalLength; i++) {
                // Keep spaces and dash in their positions
                if (i == 4 || i == 7 || i == 12 || i == 14 || i == 21) {
                    scrambled.append(' ');
                } else if (i == 13) {
                    scrambled.append('-');
                } else {
                    scrambled.append(SCRAMBLE_CHARS[rand.nextInt(SCRAMBLE_CHARS.length)]);
                }
            }
            display.append(Component.literal(scrambled.toString()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        return display;
    }

    /**
     * Creates the coordinate display line (xxxx yy zzzz).
     * Only called when coordinates need to be shown.
     */
    private Component createCoordinateDisplay(ItemStack stack, int proximity) {
        MutableComponent display = Component.empty();
        Random rand = new Random(System.currentTimeMillis() / 150); // Shift every 150ms

        int xDigits = getXDigits(stack);
        int yDigits = getYDigits(stack);
        int zDigits = getZDigits(stack);

        // Get current moon phase for X digit visibility
        int moonPhase = getCurrentMoonPhase();

        // Build X coordinate section (4 digits)
        for (int i = 0; i < 4; i++) {
            display.append(createXDigitComponent(i, xDigits, moonPhase, rand));
        }
        display.append(Component.literal(" ").withStyle(ChatFormatting.LIGHT_PURPLE));

        // Build Y coordinate section (2 digits)
        for (int i = 0; i < 2; i++) {
            display.append(createYDigitComponent(i, yDigits, rand));
        }
        display.append(Component.literal(" ").withStyle(ChatFormatting.LIGHT_PURPLE));

        // Build Z coordinate section (4 digits) - colored based on origin proximity
        for (int i = 0; i < 4; i++) {
            display.append(createZDigitComponent(i, zDigits, proximity, rand));
        }

        return display;
    }

    /**
     * Gets the current moon phase from the client world.
     * Returns 0-7, or 0 if not available.
     */
    private int getCurrentMoonPhase() {
        try {
            if (Minecraft.getInstance().level != null) {
                return Minecraft.getInstance().level.getMoonPhase();
            }
        } catch (Exception e) {
            // Fallback if called from server side or level is unavailable
        }
        return 0;
    }

    /**
     * Creates a component for a single X digit based on discovery state and moon phase.
     * The moon phase determines which digit position is visible during that phase.
     */
    private Component createXDigitComponent(int position, int revealedMask, int moonPhase, Random rand) {
        boolean isRevealed = (revealedMask & (1 << position)) != 0;
        // Check for "close" state (bit in upper nibble)
        boolean isClose = (revealedMask & (1 << (position + 4))) != 0;

        // Check if this position is the one shown for the current moon phase
        int moonPhasePosition = MOON_PHASE_TO_X_POSITION[moonPhase];
        boolean isCurrentMoonPosition = (position == moonPhasePosition);

        if (isRevealed) {
            // Fully revealed - green with correct digit
            char runicDigit = digitToRune(HuntConfig.X_DIGITS[position]);
            return Component.literal(String.valueOf(runicDigit)).withStyle(ChatFormatting.GREEN);
        } else if (isClose && isCurrentMoonPosition) {
            // Close AND this is the moon phase's position - yellow with nearby digit (offset by 1-2)
            int actualDigit = Integer.parseInt(HuntConfig.X_DIGITS[position]);
            int offset = (rand.nextBoolean() ? 1 : -1) * (rand.nextInt(2) + 1);
            int nearbyDigit = Math.floorMod(actualDigit + offset, 10);
            char runicDigit = RUNIC_DIGITS[nearbyDigit];
            return Component.literal(String.valueOf(runicDigit)).withStyle(ChatFormatting.YELLOW);
        } else {
            // Not revealed or not the current moon phase's position - scrambling placeholder
            char scrambled = SCRAMBLE_CHARS[rand.nextInt(SCRAMBLE_CHARS.length)];
            return Component.literal(String.valueOf(scrambled)).withStyle(ChatFormatting.LIGHT_PURPLE);
        }
    }

    /**
     * Creates a component for a single Y digit based on discovery state
     */
    private Component createYDigitComponent(int position, int revealedMask, Random rand) {
        boolean isRevealed = (revealedMask & (1 << position)) != 0;

        if (isRevealed) {
            // Fully revealed - green with correct digit
            char runicDigit = digitToRune(HuntConfig.Y_DIGITS[position]);
            return Component.literal(String.valueOf(runicDigit)).withStyle(ChatFormatting.GREEN);
        } else {
            // Not revealed - scrambling placeholder
            char scrambled = SCRAMBLE_CHARS[rand.nextInt(SCRAMBLE_CHARS.length)];
            return Component.literal(String.valueOf(scrambled)).withStyle(ChatFormatting.LIGHT_PURPLE);
        }
    }

    /**
     * Creates a component for a single Z digit based on origin proximity
     */
    private Component createZDigitComponent(int position, int revealedMask, int proximity, Random rand) {
        boolean isRevealed = (revealedMask & (1 << position)) != 0;

        if (isRevealed) {
            // Fully revealed (at origin) - green with correct digit
            char runicDigit = digitToRune(HuntConfig.Z_DIGITS[position]);
            return Component.literal(String.valueOf(runicDigit)).withStyle(ChatFormatting.GREEN);
        } else {
            // Color based on proximity to origin
            ChatFormatting color = switch (proximity) {
                case 1 -> ChatFormatting.RED;      // Far (100 blocks)
                case 2 -> ChatFormatting.GOLD;     // Medium (50 blocks)
                case 3 -> ChatFormatting.YELLOW;   // Close (25 blocks)
                default -> ChatFormatting.LIGHT_PURPLE; // Not near origin
            };

            char scrambled = SCRAMBLE_CHARS[rand.nextInt(SCRAMBLE_CHARS.length)];
            return Component.literal(String.valueOf(scrambled)).withStyle(color);
        }
    }

    /**
     * Converts a digit string to its runic equivalent
     */
    private char digitToRune(String digit) {
        int value = Integer.parseInt(digit);
        return RUNIC_DIGITS[value];
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Enchant glint when fully filled
        return getOrbState(stack) == OrbState.FINAL;
    }

    // === Helper methods for data component access ===

    public static OrbState getOrbState(ItemStack stack) {
        Integer stateValue = stack.get(HuntDataComponents.ORB_STATE.get());
        return OrbState.fromValue(stateValue != null ? stateValue : 0);
    }

    public static void setOrbState(ItemStack stack, OrbState state) {
        stack.set(HuntDataComponents.ORB_STATE.get(), state.getValue());
    }

    public static int getKillCount(ItemStack stack) {
        Integer count = stack.get(HuntDataComponents.ORB_KILL_COUNT.get());
        return count != null ? count : 0;
    }

    public static void setKillCount(ItemStack stack, int count) {
        stack.set(HuntDataComponents.ORB_KILL_COUNT.get(), count);
    }

    public static int getRevealedRunes(ItemStack stack) {
        Integer revealed = stack.get(HuntDataComponents.ORB_REVEALED_RUNES.get());
        return revealed != null ? revealed : 0;
    }

    public static void setRevealedRunes(ItemStack stack, int count) {
        stack.set(HuntDataComponents.ORB_REVEALED_RUNES.get(), count);
    }

    public static int getXDigits(ItemStack stack) {
        Integer digits = stack.get(HuntDataComponents.ORB_X_DIGITS.get());
        return digits != null ? digits : 0;
    }

    public static void setXDigits(ItemStack stack, int mask) {
        stack.set(HuntDataComponents.ORB_X_DIGITS.get(), mask);
    }

    /**
     * Reveals a specific X digit position (0-3)
     * @param position The digit position (0 = first/leftmost)
     * @param exact If true, marks as fully revealed (green). If false, marks as close (yellow).
     */
    public static void revealXDigit(ItemStack stack, int position, boolean exact) {
        int current = getXDigits(stack);
        if (exact) {
            // Set the revealed bit (lower nibble)
            current |= (1 << position);
            // Clear the close bit (upper nibble)
            current &= ~(1 << (position + 4));
        } else {
            // Set the close bit (upper nibble) if not already revealed
            if ((current & (1 << position)) == 0) {
                current |= (1 << (position + 4));
            }
        }
        setXDigits(stack, current);
    }

    public static boolean isXDigitRevealed(ItemStack stack, int position) {
        return (getXDigits(stack) & (1 << position)) != 0;
    }

    public static int getYDigits(ItemStack stack) {
        Integer digits = stack.get(HuntDataComponents.ORB_Y_DIGITS.get());
        return digits != null ? digits : 0;
    }

    public static void setYDigits(ItemStack stack, int mask) {
        stack.set(HuntDataComponents.ORB_Y_DIGITS.get(), mask);
    }

    public static void revealYDigit(ItemStack stack, int position) {
        int current = getYDigits(stack);
        current |= (1 << position);
        setYDigits(stack, current);
    }

    public static int getZDigits(ItemStack stack) {
        Integer digits = stack.get(HuntDataComponents.ORB_Z_DIGITS.get());
        return digits != null ? digits : 0;
    }

    public static void setZDigits(ItemStack stack, int mask) {
        stack.set(HuntDataComponents.ORB_Z_DIGITS.get(), mask);
    }

    public static void revealAllZDigits(ItemStack stack) {
        // Reveal all 4 Z digits at once (when at origin)
        setZDigits(stack, 0b1111);
    }

    public static int getProximity(ItemStack stack) {
        Integer prox = stack.get(HuntDataComponents.ORB_PROXIMITY.get());
        return prox != null ? prox : 0;
    }

    public static void setProximity(ItemStack stack, int proximity) {
        stack.set(HuntDataComponents.ORB_PROXIMITY.get(), proximity);
    }

    public static String getFathomMark(ItemStack stack) {
        return stack.get(HuntDataComponents.ORB_FATHOM_MARK.get());
    }

    public static void setFathomMark(ItemStack stack, String mark) {
        stack.set(HuntDataComponents.ORB_FATHOM_MARK.get(), mark);
    }

    public static Integer getEdition(ItemStack stack) {
        return stack.get(HuntDataComponents.ORB_EDITION.get());
    }

    public static void setEdition(ItemStack stack, int edition) {
        stack.set(HuntDataComponents.ORB_EDITION.get(), edition);
    }

    /**
     * Called when a Pokemon is defeated while orb is in player's inventory.
     * Increments kill count and potentially reveals a new rune.
     *
     * @return true if a new rune was revealed
     */
    public static boolean onPokemonDefeated(ItemStack stack) {
        if (getOrbState(stack) == OrbState.FINAL) {
            return false; // Already complete
        }

        int kills = getKillCount(stack) + 1;
        setKillCount(stack, kills);

        // Check if we should reveal a new rune
        int currentRunes = getRevealedRunes(stack);
        int expectedRunes = kills / HuntConfig.KILLS_PER_RUNE;

        if (expectedRunes > currentRunes && currentRunes < HuntConfig.TOTAL_RUNES) {
            int newRuneCount = Math.min(expectedRunes, HuntConfig.TOTAL_RUNES);
            setRevealedRunes(stack, newRuneCount);

            // Update orb state based on rune count
            updateOrbState(stack, newRuneCount);

            // When orb becomes FINAL, set the fathom mark (hidden Y coordinate hint)
            if (newRuneCount >= HuntConfig.RUNES_FOR_FINAL) {
                // The Y coordinate is -12, stored as a cryptic reference
                // "fathom_mark: II below" (II = 12 in Roman-ish, "below" hints negative)
                setFathomMark(stack, "II below");
            }

            return true;
        }

        return false;
    }

    /**
     * Updates the orb's visual state based on revealed rune count
     */
    private static void updateOrbState(ItemStack stack, int runeCount) {
        OrbState newState;
        if (runeCount >= HuntConfig.RUNES_FOR_FINAL) {
            newState = OrbState.FINAL;
        } else if (runeCount >= HuntConfig.RUNES_FOR_HALF) {
            newState = OrbState.HALF;
        } else if (runeCount >= HuntConfig.RUNES_FOR_STAGE_1) {
            newState = OrbState.STAGE_1;
        } else {
            newState = OrbState.EMPTY;
        }
        setOrbState(stack, newState);
    }
}
