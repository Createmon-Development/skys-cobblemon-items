package com.skys.cobblemoncosmetics.blocks;

import com.skys.cobblemoncosmetics.hunt.RunicCipherTabletItem;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Runic Altar block - When right-clicked, gives the player a faded tablet
 * if they don't already have one in their inventory.
 * Emits blue sparkling particles.
 */
public class RunicAltarBlock extends Block {

    public RunicAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        // Check if player already has a tablet
        boolean hasTablet = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.is(ModItems.RUNIC_CIPHER_TABLET.get())) {
                hasTablet = true;
                break;
            }
        }

        if (hasTablet) {
            // Player already has a tablet
            player.sendSystemMessage(Component.literal("The altar's glow dims... you already possess its gift.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5f, 1.5f);
        } else {
            // Give player a faded tablet
            ItemStack tablet = new ItemStack(ModItems.RUNIC_CIPHER_TABLET.get());
            RunicCipherTabletItem.setGlowing(tablet, false); // Start as faded

            if (!player.getInventory().add(tablet)) {
                // Inventory full, drop it
                player.drop(tablet, false);
            }

            player.sendSystemMessage(Component.literal("The altar bestows upon you an ancient tablet...")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

            // Play mystical sound
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);

            // Spawn burst of particles on server (will sync to client)
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 30; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                    double offsetY = level.random.nextDouble() * 1.5;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + 0.5 + offsetY,
                        pos.getZ() + 0.5 + offsetZ,
                        1, 0, 0.05, 0, 0.02);
                }
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Delegate to the item interaction handler with empty stack
        ItemInteractionResult result = useItemOn(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hitResult);
        return result == ItemInteractionResult.SUCCESS ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Spawn blue sparkling particles (soul fire flame has a blue tint)
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double y = pos.getY() + 0.8 + random.nextDouble() * 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;

            // Soul fire flame particles (blue)
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.02, 0);
        }

        // Additional enchantment particles
        if (random.nextInt(5) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0, 0);
        }
    }
}
