package com.skys.cobblemoncosmetics.hunt;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent world data for tracking the Crystal Ascendancy race state.
 * Tracks which players have joined, cooldowns, and the winner.
 */
public class CrystalAscendancyManager extends SavedData {

    private static final String DATA_NAME = SkysCobblemonCosmetics.MOD_ID + "_crystal_ascendancy";

    // Race state
    private boolean raceStarted = false;
    private boolean raceComplete = false;
    private UUID winnerUUID = null;
    private long raceStartTime = 0;

    // Player tracking
    private final Set<UUID> playersInRace = new HashSet<>();
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public CrystalAscendancyManager() {
    }

    // === Race State Methods ===

    public boolean isRaceStarted() {
        return raceStarted;
    }

    public boolean isRaceComplete() {
        return raceComplete;
    }

    public UUID getWinner() {
        return winnerUUID;
    }

    public void startRace() {
        if (!raceStarted) {
            raceStarted = true;
            raceStartTime = System.currentTimeMillis();
            setDirty();
            SkysCobblemonCosmetics.LOGGER.info("Crystal Ascendancy race has started!");
        }
    }

    public void completeRace(UUID winner) {
        if (!raceComplete) {
            raceComplete = true;
            winnerUUID = winner;
            setDirty();
            SkysCobblemonCosmetics.LOGGER.info("Crystal Ascendancy race completed! Winner: {}", winner);
        }
    }

    public void resetRace() {
        raceStarted = false;
        raceComplete = false;
        winnerUUID = null;
        raceStartTime = 0;
        playersInRace.clear();
        playerCooldowns.clear();
        setDirty();
        SkysCobblemonCosmetics.LOGGER.info("Crystal Ascendancy race has been reset!");
    }

    // === Player Tracking Methods ===

    public boolean hasPlayerJoinedRace(UUID playerUUID) {
        return playersInRace.contains(playerUUID);
    }

    public void addPlayerToRace(UUID playerUUID) {
        if (playersInRace.add(playerUUID)) {
            setDirty();
        }
    }

    public Set<UUID> getPlayersInRace() {
        return new HashSet<>(playersInRace);
    }

    public int getPlayerCount() {
        return playersInRace.size();
    }

    // === Cooldown Methods ===

    public boolean isOnCooldown(UUID playerUUID) {
        Long cooldownEnd = playerCooldowns.get(playerUUID);
        if (cooldownEnd == null) {
            return false;
        }
        if (System.currentTimeMillis() >= cooldownEnd) {
            playerCooldowns.remove(playerUUID);
            setDirty();
            return false;
        }
        return true;
    }

    public void setCooldown(UUID playerUUID, long durationMs) {
        playerCooldowns.put(playerUUID, System.currentTimeMillis() + durationMs);
        setDirty();
    }

    public long getCooldownRemaining(UUID playerUUID) {
        Long cooldownEnd = playerCooldowns.get(playerUUID);
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void clearCooldown(UUID playerUUID) {
        if (playerCooldowns.remove(playerUUID) != null) {
            setDirty();
        }
    }

    // === Serialization ===

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("raceStarted", raceStarted);
        tag.putBoolean("raceComplete", raceComplete);
        tag.putLong("raceStartTime", raceStartTime);

        if (winnerUUID != null) {
            tag.putUUID("winner", winnerUUID);
        }

        // Save players in race
        ListTag playerList = new ListTag();
        for (UUID uuid : playersInRace) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", uuid);
            playerList.add(playerTag);
        }
        tag.put("playersInRace", playerList);

        // Save cooldowns
        ListTag cooldownList = new ListTag();
        for (Map.Entry<UUID, Long> entry : playerCooldowns.entrySet()) {
            CompoundTag cooldownTag = new CompoundTag();
            cooldownTag.putUUID("uuid", entry.getKey());
            cooldownTag.putLong("endTime", entry.getValue());
            cooldownList.add(cooldownTag);
        }
        tag.put("cooldowns", cooldownList);

        return tag;
    }

    public static CrystalAscendancyManager load(CompoundTag tag, HolderLookup.Provider registries) {
        CrystalAscendancyManager manager = new CrystalAscendancyManager();

        manager.raceStarted = tag.getBoolean("raceStarted");
        manager.raceComplete = tag.getBoolean("raceComplete");
        manager.raceStartTime = tag.getLong("raceStartTime");

        if (tag.hasUUID("winner")) {
            manager.winnerUUID = tag.getUUID("winner");
        }

        // Load players in race
        ListTag playerList = tag.getList("playersInRace", Tag.TAG_COMPOUND);
        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerTag = playerList.getCompound(i);
            manager.playersInRace.add(playerTag.getUUID("uuid"));
        }

        // Load cooldowns
        ListTag cooldownList = tag.getList("cooldowns", Tag.TAG_COMPOUND);
        for (int i = 0; i < cooldownList.size(); i++) {
            CompoundTag cooldownTag = cooldownList.getCompound(i);
            manager.playerCooldowns.put(
                cooldownTag.getUUID("uuid"),
                cooldownTag.getLong("endTime")
            );
        }

        return manager;
    }

    // === Static Access ===

    public static CrystalAscendancyManager get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(CrystalAscendancyManager::new, CrystalAscendancyManager::load),
            DATA_NAME
        );
    }

    public static CrystalAscendancyManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(CrystalAscendancyManager::new, CrystalAscendancyManager::load),
            DATA_NAME
        );
    }

    // === Utility ===

    /**
     * Formats remaining cooldown time as a human-readable string
     */
    public static String formatCooldownTime(long remainingMs) {
        long seconds = remainingMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds % 60);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
}
