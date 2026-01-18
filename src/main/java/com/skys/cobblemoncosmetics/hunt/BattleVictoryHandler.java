package com.skys.cobblemoncosmetics.hunt;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.items.ModItems;
import kotlin.Unit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Handles battle victory events for the Crystal Ascendancy hunt.
 * When a player wins a battle while holding the Mysterious Orb, increment its kill count.
 */
public class BattleVictoryHandler {

    public static void register() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            handleBattleVictory(event);
            return Unit.INSTANCE;
        });
        SkysCobblemonCosmetics.LOGGER.info("Registered battle victory handler for Crystal Ascendancy hunt");
    }

    private static void handleBattleVictory(BattleVictoryEvent event) {
        // Get the winners from the battle
        event.getWinners().forEach(battleActor -> {
            // Check if the winner is a player actor
            if (battleActor instanceof PlayerBattleActor playerActor) {
                ServerPlayer player = playerActor.getEntity();
                if (player != null) {
                    processPlayerVictory(player, event);
                }
            }
        });
    }

    private static void processPlayerVictory(ServerPlayer player, BattleVictoryEvent event) {
        // Check player's inventory for Mysterious Orb
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (stack.is(ModItems.MYSTERIOUS_ORB.get())) {
                HuntDataComponents.OrbState currentState = MysteriousOrbItem.getOrbState(stack);

                // Only process if orb is not fully filled
                if (currentState != HuntDataComponents.OrbState.FINAL) {
                    incrementOrbProgress(player, stack);
                }
                return; // Only process one orb per battle
            }
        }
    }

    private static void incrementOrbProgress(ServerPlayer player, ItemStack orbStack) {
        // Get current progress
        int killCount = MysteriousOrbItem.getKillCount(orbStack);
        int revealedRunes = MysteriousOrbItem.getRevealedRunes(orbStack);

        // Increment kill count
        killCount++;

        // Check if we've reached enough kills for a new rune
        if (killCount >= HuntConfig.KILLS_PER_RUNE) {
            killCount = 0; // Reset kill counter
            revealedRunes++;

            // Notify player of rune reveal
            player.sendSystemMessage(Component.literal("§dA new rune materializes within the orb... (" + revealedRunes + "/" + HuntConfig.TOTAL_RUNES + ")"));

            // Play mystical sound
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);

            // Update orb state based on runes revealed
            HuntDataComponents.OrbState newState = calculateOrbState(revealedRunes);
            HuntDataComponents.OrbState oldState = MysteriousOrbItem.getOrbState(orbStack);

            if (newState != oldState) {
                orbStack.set(HuntDataComponents.ORB_STATE.get(), newState.getValue());
                notifyStateChange(player, newState);
            }
        }

        // Save progress to orb
        orbStack.set(HuntDataComponents.ORB_KILL_COUNT.get(), killCount);
        orbStack.set(HuntDataComponents.ORB_REVEALED_RUNES.get(), revealedRunes);
    }

    private static HuntDataComponents.OrbState calculateOrbState(int revealedRunes) {
        if (revealedRunes >= HuntConfig.RUNES_FOR_FINAL) {
            return HuntDataComponents.OrbState.FINAL;
        } else if (revealedRunes >= HuntConfig.RUNES_FOR_HALF) {
            return HuntDataComponents.OrbState.HALF;
        } else if (revealedRunes >= HuntConfig.RUNES_FOR_STAGE_1) {
            return HuntDataComponents.OrbState.STAGE_1;
        }
        return HuntDataComponents.OrbState.EMPTY;
    }

    private static void notifyStateChange(ServerPlayer player, HuntDataComponents.OrbState newState) {
        String message = switch (newState) {
            case STAGE_1 -> "§5The orb begins to stir with faint light...";
            case HALF -> "§dThe orb pulses with growing power, halfway filled!";
            case FINAL -> "§6The orb blazes with complete radiance! The runes have aligned!";
            default -> "";
        };

        if (!message.isEmpty()) {
            player.sendSystemMessage(Component.literal(message));

            // Extra fanfare for completion
            if (newState == HuntDataComponents.OrbState.FINAL) {
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }
}
