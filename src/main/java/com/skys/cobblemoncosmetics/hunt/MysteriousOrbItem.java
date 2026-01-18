package com.skys.cobblemoncosmetics.hunt;

import com.skys.cobblemoncosmetics.hunt.HuntDataComponents.OrbState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Random;

/**
 * The Mysterious Orb - main quest item for the Crystal Ascendancy hunt.
 * Changes appearance based on how many runes have been revealed through battle.
 */
public class MysteriousOrbItem extends Item {

    // Characters used for scrambled rune display
    private static final char[] SCRAMBLE_CHARS = {
        'ᛯ', 'ᛡ', 'ᛤ', 'ᛢ', 'ᛠ', 'ᛣ', 'ᛥ', 'ᛦ', 'ᛧ', 'ᛨ', 'ᛩ', 'ᛪ', '᛫', '᛬', '᛭', 'ᛮ', 'ᛰ', 'ᛱ'
    };

    // The actual rune sequence (without spaces/dashes)
    private static final String RUNE_SEQUENCE = "ᛛᛙᛞᛟᛝᛓᛕᛖᛊᛜᛘᚴᛘᛪᚱᛆᛎᚱ";

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
        int killCount = getKillCount(stack);

        // Add description based on state
        switch (state) {
            case EMPTY -> tooltipComponents.add(
                Component.literal(HuntConfig.DESC_EMPTY).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            case STAGE_1, HALF -> {
                tooltipComponents.add(
                    Component.literal(HuntConfig.DESC_FILLING).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
                tooltipComponents.add(Component.empty());
                tooltipComponents.add(createRuneDisplay(revealedRunes));
            }
            case FINAL -> {
                tooltipComponents.add(
                    Component.literal(HuntConfig.DESC_FILLED).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                tooltipComponents.add(Component.empty());
                tooltipComponents.add(createRuneDisplay(revealedRunes));
            }
        }

        // Debug info (only with advanced tooltips)
        if (tooltipFlag.isAdvanced()) {
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.literal("Defeats: " + killCount).withStyle(ChatFormatting.DARK_GRAY));
            tooltipComponents.add(Component.literal("Runes: " + revealedRunes + "/" + HuntConfig.TOTAL_RUNES).withStyle(ChatFormatting.DARK_GRAY));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * Creates the rune display with revealed and scrambled characters
     */
    private Component createRuneDisplay(int revealedCount) {
        StringBuilder display = new StringBuilder();
        Random rand = new Random(System.currentTimeMillis() / 150); // Shift every 150ms

        // Format: "ᛛᛙᛞᛟ ᛝᛓ ᛕᛖᛊᛜ - ᛘᚴᛘᛪᚱᛆ ᛎᚱ"
        String fullPattern = "#### ## #### - ###### ##";
        int runeIndex = 0;

        for (char c : fullPattern.toCharArray()) {
            if (c == '#') {
                if (runeIndex < revealedCount && runeIndex < RUNE_SEQUENCE.length()) {
                    // Revealed rune
                    display.append(RUNE_SEQUENCE.charAt(runeIndex));
                } else {
                    // Scrambled rune (shifting)
                    display.append(SCRAMBLE_CHARS[rand.nextInt(SCRAMBLE_CHARS.length)]);
                }
                runeIndex++;
            } else {
                // Spaces and dashes
                display.append(c);
            }
        }

        return Component.literal(display.toString()).withStyle(ChatFormatting.LIGHT_PURPLE);
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
