package com.skys.cobblemoncosmetics.hunt;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

/**
 * The Mystery Orb item used in the Cobalt Ascendancy scavenger hunt.
 *
 * Features:
 * - Glitching/shifting display name
 * - Scrambled description that reveals letters as Pokemon are defeated
 * - Tracks kill count and revealed letters via data components
 * - Three states: UNFILLED, FILLING, FILLED
 */
public class MysteryOrbItem extends Item {

    public enum OrbState {
        UNFILLED(0),
        FILLING(1),
        FILLED(2);

        private final int value;

        OrbState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static OrbState fromValue(int value) {
            for (OrbState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            return UNFILLED;
        }
    }

    public MysteryOrbItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        // Create glitching text effect for the name
        return createGlitchingName("Mysterious Orb", System.currentTimeMillis(), stack);
    }

    private Component createGlitchingName(String baseName, long time, ItemStack stack) {
        StringBuilder result = new StringBuilder();
        Random rand = new Random(time / 100); // Change every 100ms

        for (char c : baseName.toCharArray()) {
            if (c == ' ') {
                result.append(' ');
            } else if (rand.nextFloat() < 0.25f) { // 25% chance to glitch each character
                result.append(HuntConfig.GLITCH_CHARS[rand.nextInt(HuntConfig.GLITCH_CHARS.length)]);
            } else {
                result.append(c);
            }
        }

        OrbState state = getOrbState(stack);
        ChatFormatting color = switch (state) {
            case UNFILLED -> ChatFormatting.DARK_PURPLE;
            case FILLING -> ChatFormatting.LIGHT_PURPLE;
            case FILLED -> ChatFormatting.GOLD;
        };

        return Component.literal(result.toString()).withStyle(color, ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        OrbState state = getOrbState(stack);
        int killCount = getKillCount(stack);
        int revealedBitmask = getRevealedLetters(stack);

        // Show different tooltips based on state
        switch (state) {
            case UNFILLED -> {
                tooltip.add(Component.literal("The orb wishes to feel alive.")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            case FILLING -> {
                // Show the scrambled/partially revealed message
                tooltip.add(Component.literal("Symbols of light writhe inside the orb :")
                    .withStyle(ChatFormatting.GRAY));
                tooltip.add(createScrambledMessage(revealedBitmask));
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Pokemon defeated: " + killCount)
                    .withStyle(ChatFormatting.DARK_GRAY));

                int lettersRevealed = Integer.bitCount(revealedBitmask);
                int lettersRemaining = HuntConfig.TOTAL_LETTERS - lettersRevealed;
                if (lettersRemaining > 0) {
                    int killsUntilNext = HuntConfig.KILLS_PER_LETTER - (killCount % HuntConfig.KILLS_PER_LETTER);
                    tooltip.add(Component.literal("Next letter in: " + killsUntilNext + "")
                        .withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
            case FILLED -> {
                tooltip.add(Component.literal("The symbols are clear:")
                    .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal(HuntConfig.toRunicString(HuntConfig.SECRET_MESSAGE))
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("A wise treasure hunter might know what this means...")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    /**
     * Creates the scrambled message display with revealed letters shown as runes.
     * Unrevealed letters show as animated scrambled runes.
     */
    private Component createScrambledMessage(int revealedBitmask) {
        MutableComponent result = Component.empty();
        String message = HuntConfig.SECRET_MESSAGE;
        int letterIndex = 0;
        Random rand = new Random(System.currentTimeMillis() / 150); // Animate scramble

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == ' ' || c == '-') {
                // Preserve spaces and dashes
                result.append(Component.literal(String.valueOf(c)));
            } else {
                boolean revealed = (revealedBitmask & (1 << letterIndex)) != 0;
                if (revealed) {
                    // Show the runic cipher version of the character
                    char rune = HuntConfig.toRune(c);
                    result.append(Component.literal(String.valueOf(rune))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                    // Show random scrambled rune with obfuscation effect
                    char scrambled = HuntConfig.SCRAMBLE_CHARS[rand.nextInt(HuntConfig.SCRAMBLE_CHARS.length)];
                    result.append(Component.literal(String.valueOf(scrambled))
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.OBFUSCATED));
                }
                letterIndex++;
            }
        }

        return result;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // Track the last holder for when the orb is thrown
        if (entity instanceof Player player && !level.isClientSide) {
            stack.set(HuntDataComponents.LAST_HOLDER.get(), player.getUUID());
        }
    }

    // --- State Management Helpers ---

    public static OrbState getOrbState(ItemStack stack) {
        Integer state = stack.get(HuntDataComponents.ORB_STATE.get());
        return OrbState.fromValue(state != null ? state : 0);
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

    public static int getRevealedLetters(ItemStack stack) {
        Integer revealed = stack.get(HuntDataComponents.ORB_REVEALED_LETTERS.get());
        return revealed != null ? revealed : 0;
    }

    public static void setRevealedLetters(ItemStack stack, int bitmask) {
        stack.set(HuntDataComponents.ORB_REVEALED_LETTERS.get(), bitmask);
    }

    /**
     * Reveals a random unrevealed letter in the message.
     * @return The index of the newly revealed letter, or -1 if all are revealed.
     */
    public static int revealNextLetter(ItemStack stack) {
        int revealed = getRevealedLetters(stack);

        // Collect all unrevealed letter indices
        java.util.List<Integer> unrevealedIndices = new java.util.ArrayList<>();
        for (int i = 0; i < HuntConfig.TOTAL_LETTERS; i++) {
            if ((revealed & (1 << i)) == 0) {
                unrevealedIndices.add(i);
            }
        }

        if (unrevealedIndices.isEmpty()) {
            return -1; // All letters already revealed
        }

        // Pick a random unrevealed letter
        int randomIndex = new Random().nextInt(unrevealedIndices.size());
        int letterToReveal = unrevealedIndices.get(randomIndex);

        revealed |= (1 << letterToReveal);
        setRevealedLetters(stack, revealed);
        return letterToReveal;
    }

    /**
     * Checks if all letters in the message have been revealed.
     */
    public static boolean allLettersRevealed(ItemStack stack) {
        int revealed = getRevealedLetters(stack);
        int allRevealedMask = (1 << HuntConfig.TOTAL_LETTERS) - 1;
        return revealed == allRevealedMask;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Enchanted glint when filled
        return getOrbState(stack) == OrbState.FILLED;
    }
}
