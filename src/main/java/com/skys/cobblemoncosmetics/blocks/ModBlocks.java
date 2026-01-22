package com.skys.cobblemoncosmetics.blocks;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for all custom blocks in the mod.
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SkysCobblemonCosmetics.MOD_ID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(SkysCobblemonCosmetics.MOD_ID);

    // Runic Altar - Gives players the faded tablet when interacted with
    public static final DeferredBlock<Block> RUNIC_ALTAR = BLOCKS.register("runic_altar",
        () -> new RunicAltarBlock(BlockBehaviour.Properties.of()
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .lightLevel(state -> 7)
            .noOcclusion()));

    // Block item for runic altar
    public static final DeferredItem<BlockItem> RUNIC_ALTAR_ITEM = BLOCK_ITEMS.register("runic_altar",
        () -> new RunicAltarBlockItem(RUNIC_ALTAR.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
        SkysCobblemonCosmetics.LOGGER.info("Registered {} custom blocks", BLOCKS.getEntries().size());
    }
}
