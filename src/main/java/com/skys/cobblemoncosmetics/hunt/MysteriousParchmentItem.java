package com.skys.cobblemoncosmetics.hunt;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Mysterious Parchment - A cryptic poem item for the Cobalt Ascendancy hunt.
 * Right-clicking opens it as a book with the runic prophecy.
 */
public class MysteriousParchmentItem extends Item {

    public MysteriousParchmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Ensure the book content is set (do this on both sides for consistency)
        if (!stack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
            setBookContent(stack);
        }

        if (level.isClientSide()) {
            // Client side: Open the book GUI
            openBookScreen(stack);
        } else if (player instanceof ServerPlayer) {
            // Server side: Award stat
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Opens the book view screen on the client side.
     * This must be called from client-side code only.
     */
    private void openBookScreen(ItemStack stack) {
        // Use the static factory method to create the appropriate BookAccess
        BookViewScreen.BookAccess bookAccess = BookViewScreen.BookAccess.fromItem(stack);
        Minecraft.getInstance().setScreen(new BookViewScreen(bookAccess));
    }

    /**
     * Sets up the written book content for this parchment
     */
    private void setBookContent(ItemStack stack) {
        List<Component> pages = new ArrayList<>();

        // Build pages with the runic poem
        // Page 1: Title and first stanza
        StringBuilder page1 = new StringBuilder();
        page1.append(encodeToRunes("An orb from another time,")).append("\n");
        page1.append(encodeToRunes("lost to the salty brine,")).append("\n");
        page1.append(encodeToRunes("will one day return from the waters foam,")).append("\n");
        page1.append(encodeToRunes("hiding the path to its owners home."));
        pages.add(Component.literal(page1.toString()).withStyle(ChatFormatting.DARK_PURPLE));

        // Page 2: Third stanza (sky/veil) - swapped with stanza 2
        StringBuilder page2 = new StringBuilder();
        page2.append(encodeToRunes("The sky a connecting veil,")).append("\n");
        page2.append(encodeToRunes("stowing the parallel,")).append("\n");
        page2.append(encodeToRunes("let sight confer a boon")).append("\n");
        page2.append(encodeToRunes("from lights of many a moon."));
        pages.add(Component.literal(page2.toString()).withStyle(ChatFormatting.DARK_PURPLE));

        // Page 3: Second stanza (new homes) - swapped with stanza 3
        StringBuilder page3 = new StringBuilder();
        page3.append(encodeToRunes("A new homes fathom,")).append("\n");
        page3.append(encodeToRunes("cast out from sea to stratum.")).append("\n");
        page3.append(encodeToRunes("A hunters brag,")).append("\n");
        page3.append(encodeToRunes("within a tag."));
        pages.add(Component.literal(page3.toString()).withStyle(ChatFormatting.DARK_PURPLE));

        // Page 4: Fourth stanza
        StringBuilder page4 = new StringBuilder();
        page4.append(encodeToRunes("Where axis meet,")).append("\n");
        page4.append(encodeToRunes("its third secretes.")).append("\n");
        page4.append(encodeToRunes("the worlds heart,")).append("\n");
        page4.append(encodeToRunes("the final mark."));
        pages.add(Component.literal(page4.toString()).withStyle(ChatFormatting.DARK_PURPLE));

        // Create the written book content
        WrittenBookContent bookContent = new WrittenBookContent(
            net.minecraft.server.network.Filterable.passThrough(encodeToRunes("Prophecy")), // title in runes
            "???", // author
            0, // generation
            pages.stream()
                .map(component -> net.minecraft.server.network.Filterable.passThrough(component))
                .toList(),
            true // resolved
        );

        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, bookContent);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("An ancient prophecy written in runic script...")
            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("Right-click to read")
            .withStyle(ChatFormatting.GRAY));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Enchantment glint to make it look magical
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        // Return the translated name, not the book title
        return Component.translatable(this.getDescriptionId(stack));
    }

    /**
     * Gets the parchment content as runic text.
     * Each line of the poem is converted to runes using the cipher.
     */
    public static String[] getRunicContent() {
        return new String[] {
            encodeToRunes("An orb from another time,"),
            encodeToRunes("lost to the salty brine,"),
            encodeToRunes("will one day return from the waters foam,"),
            encodeToRunes("hiding the path to its owners home."),
            "",
            encodeToRunes("The sky a connecting veil,"),
            encodeToRunes("stowing the parallel,"),
            encodeToRunes("let sight confer a boon"),
            encodeToRunes("from lights of many a moon."),
            "",
            encodeToRunes("A new homes fathom,"),
            encodeToRunes("cast out from sea to stratum."),
            encodeToRunes("A hunters brag,"),
            encodeToRunes("within a tag."),
            "",
            encodeToRunes("Where axis meet,"),
            encodeToRunes("its third secretes."),
            encodeToRunes("the worlds heart,"),
            encodeToRunes("the final mark.")
        };
    }

    /**
     * Gets the original English content for reference/debug.
     */
    public static String[] getEnglishContent() {
        return new String[] {
            "An orb from another time,",
            "lost to the salty brine,",
            "will one day return from the waters foam,",
            "hiding the path to its owners home.",
            "",
            "The sky a connecting veil,",
            "stowing the parallel,",
            "let sight confer a boon",
            "from lights of many a moon.",
            "",
            "A new homes fathom,",
            "cast out from sea to stratum.",
            "A hunters brag,",
            "within a tag.",
            "",
            "Where axis meet,",
            "its third secretes.",
            "the worlds heart,",
            "the final mark."
        };
    }

    /**
     * Encodes a string to runic characters using the cipher from HuntConfig.
     */
    private static String encodeToRunes(String text) {
        StringBuilder result = new StringBuilder();

        for (char c : text.toLowerCase().toCharArray()) {
            if (c == ' ' || c == ',' || c == '.' || c == '\'' || c == '-') {
                result.append(c);
            } else if (c >= 'a' && c <= 'z') {
                int index = c - 'a';
                // Extract just the rune from "ᚨ=a" format
                String mapping = HuntConfig.RUNIC_ALPHABET[index];
                result.append(mapping.charAt(0));
            } else if (c >= '0' && c <= '9') {
                int index = c - '0';
                String mapping = HuntConfig.RUNIC_NUMBERS[index];
                result.append(mapping.charAt(0));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
