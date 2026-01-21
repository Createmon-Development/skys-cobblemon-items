package com.skys.cobblemoncosmetics.hunt;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the coordinate puzzle mechanics for the Crystal Ascendancy hunt.
 * - Star puzzle: Look at sky at night to find X coordinate digits (position feedback, not actual number)
 * - Origin puzzle: Travel to exactly (0,0,0) to reveal Z coordinate (with proximity feedback)
 */
@EventBusSubscriber
public class CoordinatePuzzleHandler {

    // Track last beep/hum time per player to control sound frequency
    private static final Map<UUID, Long> lastStarBeepTime = new HashMap<>();
    private static final Map<UUID, Long> lastStarCloseMessageTime = new HashMap<>();
    private static final Map<UUID, Long> lastOriginHumTime = new HashMap<>();
    private static final Map<UUID, Long> lastOriginRevealTime = new HashMap<>();
    private static final Map<UUID, Integer> lastProximityState = new HashMap<>();

    // Target directions for each X digit (moon phase determines which digit)
    // Moon phases: 0=full, 1, 2, 3, 4=new, 5, 6, 7
    private static final float[] TARGET_YAWS = {45.0f, 135.0f, 225.0f, 315.0f}; // NE, SE, SW, NW
    private static final float TARGET_PITCH = -60.0f; // Looking up at 60 degrees

    // Track if star close message should override origin hum message
    private static final Map<UUID, Long> starCloseOverrideTime = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Only check every 5 ticks (0.25 seconds) for more responsive feedback
        if (player.tickCount % 5 != 0) {
            return;
        }

        // Check if player is holding the mysterious orb in hand
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack orbStack = null;
        if (mainHand.is(ModItems.MYSTERIOUS_ORB.get())) {
            orbStack = mainHand;
        } else if (offHand.is(ModItems.MYSTERIOUS_ORB.get())) {
            orbStack = offHand;
        }

        if (orbStack == null) {
            // Clear proximity state when not holding orb
            UUID playerId = player.getUUID();
            if (lastProximityState.containsKey(playerId)) {
                lastProximityState.remove(playerId);
            }
            return;
        }

        // Check orb state - only works when orb is filled (has all runes)
        HuntDataComponents.OrbState state = MysteriousOrbItem.getOrbState(orbStack);
        if (state != HuntDataComponents.OrbState.FINAL) {
            return;
        }

        // Check for star puzzle (nighttime, looking up) - this can set override for action bar
        boolean starCloseActive = checkStarPuzzle(player, orbStack);

        // Check for origin puzzle (proximity to 0,0,0)
        checkOriginPuzzle(player, orbStack, starCloseActive);
    }

    /**
     * @return true if the star "close" message is currently being shown (to override origin hum)
     */
    private static boolean checkStarPuzzle(ServerPlayer player, ItemStack orbStack) {
        ServerLevel level = player.serverLevel();
        UUID playerId = player.getUUID();

        // Must be night time
        long dayTime = level.getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime <= 23000;
        if (!isNight) {
            return false;
        }

        // Must be looking upward (negative pitch)
        float pitch = player.getXRot();
        if (pitch > -30.0f) {
            // Not looking up enough
            return false;
        }

        // Get moon phase to determine which digit position we're searching for
        int moonPhase = level.getMoonPhase();
        int digitPosition = moonPhase / 2; // 0-3 for 4 digit positions

        // Skip if this digit is already fully revealed
        if (MysteriousOrbItem.isXDigitRevealed(orbStack, digitPosition)) {
            return false;
        }

        // Get target direction for this digit
        float targetYaw = TARGET_YAWS[digitPosition];

        // Calculate how close player is looking to the target direction
        float yaw = normalizeAngle(player.getYRot());
        float normalizedTargetYaw = normalizeAngle(targetYaw);

        float yawDiff = Math.abs(angleDifference(yaw, normalizedTargetYaw));
        float pitchDiff = Math.abs(pitch - TARGET_PITCH);

        // Combined accuracy (0 = perfect, higher = further off)
        float accuracy = (yawDiff + pitchDiff) / 2.0f;

        long currentTime = System.currentTimeMillis();
        boolean showingCloseMessage = false;

        // Check if player is close enough for the exact "ding" (found it!)
        if (accuracy < HuntConfig.STAR_EXACT_RANGE) {
            // Player found the exact direction!
            Long lastBeep = lastStarBeepTime.get(playerId);
            if (lastBeep == null || currentTime - lastBeep > 2000) {
                // Play loud success ding
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.8F);
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.5F);

                // Reveal the X digit (exact)
                MysteriousOrbItem.revealXDigit(orbStack, digitPosition, true);

                // Send success message
                player.sendSystemMessage(Component.literal(
                    "§a✦ The stars align! The inscription on the orb becomes a little more clear. ✦"
                ));

                // Play magical reveal sound
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.5F);

                lastStarBeepTime.put(playerId, currentTime);
            }
        } else if (accuracy < HuntConfig.STAR_CLOSE_RANGE) {
            // Player is close - yellow feedback with hovering message
            Long lastBeep = lastStarBeepTime.get(playerId);
            if (lastBeep == null || currentTime - lastBeep > 500) {
                // Mark as "close" (yellow) if not already revealed
                MysteriousOrbItem.revealXDigit(orbStack, digitPosition, false);

                // High-pitched chime for close
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8F, 1.5F);

                lastStarBeepTime.put(playerId, currentTime);
            }

            // Show hovering action bar message (overrides origin hum)
            player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.literal("§e§lThe orb shimmers happily. You must be close...")
            ));
            showingCloseMessage = true;
            starCloseOverrideTime.put(playerId, currentTime);

        } else if (accuracy < HuntConfig.STAR_BEEP_RANGE) {
            // Player is getting closer - play beeping that speeds up
            Long lastBeep = lastStarBeepTime.get(playerId);

            // Beep interval based on accuracy (closer = faster beeping)
            long beepInterval = (long) (150 + (accuracy / HuntConfig.STAR_BEEP_RANGE) * 600);

            if (lastBeep == null || currentTime - lastBeep > beepInterval) {
                // Calculate pitch based on accuracy (closer = higher pitch)
                float soundPitch = 0.6f + (1.0f - accuracy / HuntConfig.STAR_BEEP_RANGE) * 1.2f;

                // Louder and more prominent bell sound
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.9F, soundPitch);

                lastStarBeepTime.put(playerId, currentTime);
            }
        }

        return showingCloseMessage;
    }

    private static void checkOriginPuzzle(ServerPlayer player, ItemStack orbStack, boolean starCloseActive) {
        BlockPos playerPos = player.blockPosition();

        // Calculate distance to world origin (0, any Y, 0) for proximity feedback
        double distanceXZ = Math.sqrt(playerPos.getX() * playerPos.getX() + playerPos.getZ() * playerPos.getZ());

        // Check if player is EXACTLY at origin (X=0, Y=0, and Z=0)
        boolean atExactOrigin = playerPos.getX() == 0 && playerPos.getY() == 0 && playerPos.getZ() == 0;

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        // Determine proximity state
        int newProximity;
        String actionBarMessage;

        if (atExactOrigin) {
            newProximity = 4; // Exact - at origin
            actionBarMessage = "§a§l✦ The orb resonates with the origin ✦";
        } else if (distanceXZ <= HuntConfig.ORIGIN_HUM_CLOSE) {
            newProximity = 3; // Close (25 blocks)
            actionBarMessage = "§e§lThe orb hums even louder...";
        } else if (distanceXZ <= HuntConfig.ORIGIN_HUM_MEDIUM) {
            newProximity = 2; // Medium (50 blocks)
            actionBarMessage = "§6The orb hums loudly...";
        } else if (distanceXZ <= HuntConfig.ORIGIN_HUM_RANGE) {
            newProximity = 1; // Far (100 blocks)
            actionBarMessage = "§cThe orb hums softly...";
        } else {
            newProximity = 0; // Out of range
            actionBarMessage = null;
        }

        // Update proximity state on the orb (for display coloring)
        int currentProximity = MysteriousOrbItem.getProximity(orbStack);
        if (currentProximity != newProximity) {
            MysteriousOrbItem.setProximity(orbStack, newProximity);
        }

        // Show action bar message if within range (but not if star close message is active)
        if (actionBarMessage != null && !starCloseActive) {
            player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.literal(actionBarMessage)
            ));
        }

        // Handle audio feedback based on proximity (not at exact origin)
        if (newProximity > 0 && newProximity < 4) {
            Long lastHum = lastOriginHumTime.get(playerId);

            // Calculate hum interval and volume based on proximity
            // Closer = louder and more frequent
            long humInterval;
            float volume;
            float pitch;

            switch (newProximity) {
                case 3 -> { // Close (25 blocks)
                    humInterval = 300;
                    volume = 1.2F;
                    pitch = 0.7F;
                }
                case 2 -> { // Medium (50 blocks)
                    humInterval = 600;
                    volume = 0.9F;
                    pitch = 0.5F;
                }
                default -> { // Far (100 blocks)
                    humInterval = 1000;
                    volume = 0.6F;
                    pitch = 0.4F;
                }
            }

            if (lastHum == null || currentTime - lastHum > humInterval) {
                // Use a shorter, more responsive sound
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, volume, pitch);

                lastOriginHumTime.put(playerId, currentTime);
            }
        }

        // Check if player reached the exact origin
        if (atExactOrigin) {
            Long lastReveal = lastOriginRevealTime.get(playerId);
            if (lastReveal == null || currentTime - lastReveal > 10000) {
                revealZCoordinate(player, orbStack);
                lastOriginRevealTime.put(playerId, currentTime);
            }
        }

        // Track state changes for logging
        Integer lastState = lastProximityState.get(playerId);
        if (lastState == null || lastState != newProximity) {
            lastProximityState.put(playerId, newProximity);
        }
    }

    private static void revealZCoordinate(ServerPlayer player, ItemStack orbStack) {
        // Check if Z digits already revealed
        if (MysteriousOrbItem.getZDigits(orbStack) == 0b1111) {
            // Already revealed, just play a gentle confirmation
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.0F);
            return;
        }

        // Play epic discovery sequence
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.2F);

        // Reveal all Z digits
        MysteriousOrbItem.revealAllZDigits(orbStack);

        // Send triumphant message
        player.sendSystemMessage(Component.literal(
            "§a✦ The orb swirls with satisfaction. The inscription becomes ever clearer. ✦"
        ));

        // Play level up sound for achievement feel
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);

        // Delayed chime for extra impact
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    // Utility: Normalize angle to 0-360 range
    private static float normalizeAngle(float angle) {
        angle = angle % 360.0f;
        if (angle < 0) {
            angle += 360.0f;
        }
        return angle;
    }

    // Utility: Calculate shortest angle difference
    private static float angleDifference(float a, float b) {
        float diff = a - b;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return diff;
    }
}
