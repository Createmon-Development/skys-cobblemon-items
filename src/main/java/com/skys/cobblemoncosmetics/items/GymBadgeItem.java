package com.skys.cobblemoncosmetics.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Base class for gym badge items.
 * These are collectible items with custom sprites.
 */
public class GymBadgeItem extends Item {
    public GymBadgeItem(Properties properties) {
        super(properties
            .rarity(Rarity.UNCOMMON) // Makes them show up in purple
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        // Use item-specific tooltip key (e.g., "item.skyscobblemoncosmetics.dieselton_badge.tooltip")
        String tooltipKey = stack.getDescriptionId() + ".tooltip";
        tooltipComponents.add(Component.translatable(tooltipKey));
    }
}
