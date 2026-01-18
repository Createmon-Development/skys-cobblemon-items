package com.skys.cobblemoncosmetics.hunt;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * - Star puzzle: Look at sky at night to find X coordinate digits
 * - Origin puzzle: Travel to (0,0,0) to reveal Z coordinate
 */
@EventBusSubscriber
public class CoordinatePuzzleHandler {

    // Track last beep/hum time per player to control sound frequency
    private static final Map<UUID, Long> lastStarBeepTime = new HashMap<>();
    private static final Map<UUID, Long> lastOriginHumTime = new HashMap<>();

    // Target direction for star puzzle (in degrees, 0 = south, 90 = west, etc.)
    // This can be configured per-server or per-moon-phase
    private static final float TARGET_YAW = 45.0f; // Northeast
    private static final float TARGET_PITCH = -60.0f; // Looking up at 60 degrees

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Only check every 10 ticks (0.5 seconds) to reduce overhead
        if (player.tickCount % 10 != 0) {
            return;
        }

        // Check if player is holding the mysterious orb
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack orbStack = null;
        if (mainHand.is(ModItems.MYSTERIOUS_ORB.get())) {
            orbStack = mainHand;
        } else if (offHand.is(ModItems.MYSTERIOUS_ORB.get())) {
            orbStack = offHand;
        }

        if (orbStack == null) {
            return;
        }

        // Check orb state - only works when orb is filled (has all runes)
        HuntDataComponents.OrbState state = MysteriousOrbItem.getOrbState(orbStack);
        if (state != HuntDataComponents.OrbState.FINAL) {
            return;
        }

        // Check for star puzzle (nighttime, looking up)
        checkStarPuzzle(player, orbStack);

        // Check for origin puzzle (proximity to 0,0,0)
        checkOriginPuzzle(player, orbStack);
    }

    private static void checkStarPuzzle(ServerPlayer player, ItemStack orbStack) {
        ServerLevel level = player.serverLevel();

        // Must be night time
        long dayTime = level.getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime <= 23000;
        if (!isNight) {
            return;
        }

        // Must be looking upward (negative pitch)
        float pitch = player.getXRot();
        if (pitch > -30.0f) {
            // Not looking up enough
            return;
        }

        // Calculate how close player is looking to the target direction
        float yaw = normalizeAngle(player.getYRot());
        float targetYaw = normalizeAngle(TARGET_YAW);

        float yawDiff = Math.abs(angleDifference(yaw, targetYaw));
        float pitchDiff = Math.abs(pitch - TARGET_PITCH);

        // Combined accuracy (0 = perfect, higher = further off)
        float accuracy = (yawDiff + pitchDiff) / 2.0f;

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        // Check if player is close enough for the "ding" (found it!)
        if (accuracy < 5.0f) {
            // Player found the correct direction!
            Long lastBeep = lastStarBeepTime.get(playerId);
            if (lastBeep == null || currentTime - lastBeep > 3000) {
                // Play success ding
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.5F);

                // Reveal the X digit based on moon phase
                revealXDigit(player, level);

                lastStarBeepTime.put(playerId, currentTime);
            }
        } else if (accuracy < HuntConfig.STAR_BEEP_RANGE) {
            // Player is getting closer - play beeping that speeds up
            Long lastBeep = lastStarBeepTime.get(playerId);

            // Beep interval based on accuracy (closer = faster beeping)
            long beepInterval = (long) (200 + (accuracy / HuntConfig.STAR_BEEP_RANGE) * 800);

            if (lastBeep == null || currentTime - lastBeep > beepInterval) {
                // Calculate pitch based on accuracy (closer = higher pitch)
                float soundPitch = 0.5f + (1.0f - accuracy / HuntConfig.STAR_BEEP_RANGE) * 1.0f;

                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.5F, soundPitch);

                lastStarBeepTime.put(playerId, currentTime);
            }
        }
    }

    private static void revealXDigit(ServerPlayer player, ServerLevel level) {
        // Get moon phase (0-7, cycles every ~8 nights)
        int moonPhase = level.getMoonPhase();

        // Each moon phase reveals a different digit position
        // Moon phases: 0=full, 1, 2, 3, 4=new, 5, 6, 7
        int digitPosition = moonPhase / 2; // 0-3 for 4 digit positions

        // The actual X coordinate digits would be configured per-server
        // For now, we'll use placeholder values
        String[] xDigits = {"2", "1", "3", "7"}; // Example: X = 2137

        String digit = xDigits[digitPosition];
        String positionName = switch (digitPosition) {
            case 0 -> "first";
            case 1 -> "second";
            case 2 -> "third";
            case 3 -> "fourth";
            default -> "unknown";
        };

        player.sendSystemMessage(Component.literal(
            "§6✦ The stars reveal a number: the " + positionName + " digit is §e" + digit + "§6 ✦"
        ));

        // Play magical reveal sound
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    private static void checkOriginPuzzle(ServerPlayer player, ItemStack orbStack) {
        BlockPos playerPos = player.blockPosition();

        // Calculate distance to world origin (0, any Y, 0)
        double distanceXZ = Math.sqrt(playerPos.getX() * playerPos.getX() + playerPos.getZ() * playerPos.getZ());

        if (distanceXZ > HuntConfig.ORIGIN_HUM_RANGE) {
            return; // Too far from origin
        }

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastHum = lastOriginHumTime.get(playerId);

        // Calculate hum intensity and interval based on distance
        // Closer = louder and more frequent
        float normalizedDistance = (float) (distanceXZ / HuntConfig.ORIGIN_HUM_RANGE);
        long humInterval = (long) (200 + normalizedDistance * 1500); // 200ms to 1700ms
        float volume = 0.3f + (1.0f - normalizedDistance) * 0.7f; // 0.3 to 1.0

        if (lastHum == null || currentTime - lastHum > humInterval) {
            // Play humming sound
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, volume, 0.5F);

            lastOriginHumTime.put(playerId, currentTime);
        }

        // Check if player is at the origin (within 3 blocks)
        if (distanceXZ < 3.0) {
            // Player reached the origin!
            Long lastReveal = lastOriginHumTime.get(playerId);
            if (lastReveal == null || currentTime - lastReveal > 5000) {
                revealZCoordinate(player);
                lastOriginHumTime.put(playerId, currentTime);
            }
        }
    }

    private static void revealZCoordinate(ServerPlayer player) {
        // Play resonance sound
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);

        // The actual Z coordinate would be configured per-server
        // For now, placeholder
        String zCoord = "-847"; // Example Z coordinate

        player.sendSystemMessage(Component.literal(
            "§b✦ The orb pulses with recognition at the world's origin... ✦"
        ));
        player.sendSystemMessage(Component.literal(
            "§b✦ The final coordinate emerges: Z = §e" + zCoord + " §b✦"
        ));

        // Play completion sound
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
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
