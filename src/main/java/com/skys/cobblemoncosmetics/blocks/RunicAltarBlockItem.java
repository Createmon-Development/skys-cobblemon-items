package com.skys.cobblemoncosmetics.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Block item for the Runic Altar with enchantment glint.
 */
public class RunicAltarBlockItem extends BlockItem {

    public RunicAltarBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Always show enchantment glint
        return true;
    }
}
