package com.skys.cobblemoncosmetics.hunt;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;

/**
 * Treasure map leading to the Temple location.
 * When used (right-clicked), converts itself into a proper vanilla filled map
 * with an X marking the Temple coordinates.
 *
 * NOTE: This item will be given by the Treasure Hunter's Assistant merchant
 * during step 3 of the hunt (not yet implemented).
 */
public class TempleTreasureMapItem extends Item {

    public TempleTreasureMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // Create the actual filled map
            ItemStack mapStack = createFilledMap(serverLevel);

            // Replace this item with the actual map
            if (stack.getCount() == 1) {
                return InteractionResultHolder.sidedSuccess(mapStack, level.isClientSide());
            } else {
                stack.shrink(1);
                if (!player.getInventory().add(mapStack)) {
                    player.drop(mapStack, false);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("A map marking the location of an ancient temple...")
            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("Right-click to unfurl the map.")
            .withStyle(ChatFormatting.GRAY));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Enchantment glint to make it look special
        return true;
    }

    /**
     * Creates a filled treasure map pointing to the Temple location.
     * This creates a vanilla filled_map item that works like any other map.
     *
     * @param level The server level
     * @return A filled map ItemStack pointing to the Temple
     */
    public static ItemStack createFilledMap(ServerLevel level) {
        BlockPos templePos = new BlockPos(HuntConfig.TEMPLE_X, HuntConfig.TEMPLE_Y, HuntConfig.TEMPLE_Z);

        // Create a filled map centered on the temple (scale 2 = 1:4, good for exploration)
        ItemStack mapStack = MapItem.create(level, templePos.getX(), templePos.getZ(), (byte) 2, true, true);

        // Get the map data to add decorations
        MapItemSavedData mapData = MapItem.getSavedData(mapStack, level);
        if (mapData != null) {
            // Add decoration to the map data
            MapDecorations decorations = mapStack.get(DataComponents.MAP_DECORATIONS);
            if (decorations == null) {
                decorations = MapDecorations.EMPTY;
            }

            // Create the target X marker at the temple location
            MapDecorations.Entry targetEntry = new MapDecorations.Entry(
                MapDecorationTypes.TARGET_X,
                templePos.getX(),
                templePos.getZ(),
                0.0f
            );

            // Add the target X marker
            mapStack.set(DataComponents.MAP_DECORATIONS,
                decorations.withDecoration("temple_target", targetEntry));

            // Set custom name
            mapStack.set(DataComponents.CUSTOM_NAME,
                Component.literal("Temple Treasure Map").withStyle(ChatFormatting.GOLD));
        }

        return mapStack;
    }

    /**
     * Convenience method to give a player the temple map directly.
     * Use this when the Treasure Hunter merchant gives the map.
     *
     * @param player The player to give the map to
     */
    public static void giveMapToPlayer(ServerPlayer player) {
        ItemStack map = createFilledMap(player.serverLevel());
        if (!player.getInventory().add(map)) {
            player.drop(map, false);
        }
    }
}
