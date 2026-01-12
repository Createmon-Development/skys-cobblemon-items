package com.skys.cobblemoncosmetics.items;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Iron Maiden Mega Bracelet - A broom-styled mega bracelet
 * Enables Mega Evolution when equipped in the mega_slot
 */
public class IronMaidenMegaBracelet extends Item {
    public IronMaidenMegaBracelet(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        AccessoriesCapability capability = AccessoriesCapability.get(player);

        if (level.isClientSide || capability == null) {
            return InteractionResultHolder.pass(stack);
        }

        AccessoriesContainer slot = capability.getContainer(SlotTypeLoader.getSlotType(level, "mega_slot"));

        if (slot == null) {
            return InteractionResultHolder.pass(stack);
        }

        ExpandedSimpleContainer accessories = slot.getAccessories();

        if (accessories == null) {
            return InteractionResultHolder.pass(stack);
        }

        // Find empty slot and equip the bracelet
        for (int i = 0; i < accessories.getContainerSize(); i++) {
            if (accessories.getItem(i).isEmpty()) {
                accessories.setItem(i, stack.copy());
                player.setItemInHand(hand, ItemStack.EMPTY);

                // Play equip sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                break;
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        tooltipComponents.add(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath() + ".tooltip"));
        super.appendHoverText(itemStack, tooltipContext, tooltipComponents, tooltipFlag);
    }
}
