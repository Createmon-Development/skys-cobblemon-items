package com.skys.cobblemoncosmetics.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item that displays a tooltip from the language file.
 */
public class TooltipItem extends Item {
    public TooltipItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        // Use item-specific tooltip key (e.g., "item.skyscobblemonitems.rusted_gold_pokeball.tooltip")
        String tooltipKey = stack.getDescriptionId() + ".tooltip";
        tooltipComponents.add(Component.translatable(tooltipKey));
    }
}
