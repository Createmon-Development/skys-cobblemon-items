package com.skys.cobblemoncosmetics.hunt;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.items.ModItems;
import com.skys.cobblemoncosmetics.sounds.ModSounds;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Handles Cobblemon events for the Cobalt Ascendancy scavenger hunt.
 * Tracks Pokemon defeats to reveal letters on the Mystery Orb.
 */
public class CobaltAscendancyEvents {

    public CobaltAscendancyEvents() {
        // Register the battle fainted event
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL, this::onPokemonFainted);
        SkysCobblemonCosmetics.LOGGER.info("Registered Cobalt Ascendancy battle events");
    }

    /**
     * Called when a Pokemon faints in battle.
     * Increments the kill count on any Mystery Orbs held by the player.
     */
    private Unit onPokemonFainted(BattleFaintedEvent event) {
        // Find all players involved in the battle
        event.getBattle().getActors().forEach(actor -> {
            if (actor instanceof PlayerBattleActor playerActor) {
                ServerPlayer player = playerActor.getEntity();
                if (player != null) {
                    handlePokemonDefeated(player);
                }
            }
        });
        return Unit.INSTANCE;
    }

    /**
     * Processes a Pokemon defeat for a player, updating any Mystery Orb they have.
     */
    private void handlePokemonDefeated(ServerPlayer player) {
        // Search for Mystery Orb in player's inventory
        ItemStack orbStack = findMysteryOrb(player);
        if (orbStack == null || orbStack.isEmpty()) {
            return;
        }

        MysteryOrbItem.OrbState state = MysteryOrbItem.getOrbState(orbStack);

        // Only track kills for unfilled or filling orbs
        if (state == MysteryOrbItem.OrbState.FILLED) {
            return;
        }

        // Increment kill count
        int kills = MysteryOrbItem.getKillCount(orbStack);
        kills++;
        MysteryOrbItem.setKillCount(orbStack, kills);

        // First kill transitions from UNFILLED to FILLING
        if (state == MysteryOrbItem.OrbState.UNFILLED) {
            MysteryOrbItem.setOrbState(orbStack, MysteryOrbItem.OrbState.FILLING);
            player.sendSystemMessage(Component.literal("The orb stirs with energy...")
                .withStyle(ChatFormatting.DARK_PURPLE));
        }

        // Check if we should reveal a letter
        if (kills % HuntConfig.KILLS_PER_LETTER == 0) {
            int revealedIndex = MysteryOrbItem.revealNextLetter(orbStack);

            if (revealedIndex >= 0) {
                // Play whisper sound
                player.level().playSound(null, player.blockPosition(),
                    ModSounds.ORB_WHISPER.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

                // Send message
                player.sendSystemMessage(Component.literal(HuntConfig.WHISPER_MESSAGE)
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));

                // Check if all letters are now revealed
                if (MysteryOrbItem.allLettersRevealed(orbStack)) {
                    // Transition to FILLED state
                    MysteryOrbItem.setOrbState(orbStack, MysteryOrbItem.OrbState.FILLED);

                    // Play completion sound
                    player.level().playSound(null, player.blockPosition(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);

                    // Send completion message
                    player.sendSystemMessage(Component.literal(HuntConfig.ORB_FILLED_MESSAGE)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

                    SkysCobblemonCosmetics.LOGGER.info("Player {} completed filling their Mystery Orb after {} kills",
                        player.getName().getString(), kills);
                }
            }
        }
    }

    /**
     * Finds a Mystery Orb in the player's inventory.
     * Checks main hand, off hand, then inventory slots.
     */
    private ItemStack findMysteryOrb(ServerPlayer player) {
        // Check main hand first
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(ModItems.MYSTERY_ORB.get())) {
            return mainHand;
        }

        // Check off hand
        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(ModItems.MYSTERY_ORB.get())) {
            return offHand;
        }

        // Check inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.MYSTERY_ORB.get())) {
                return stack;
            }
        }

        return null;
    }
}
