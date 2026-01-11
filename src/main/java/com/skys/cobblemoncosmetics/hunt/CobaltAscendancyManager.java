package com.skys.cobblemoncosmetics.hunt;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the global state of the Cobalt Ascendancy scavenger hunt.
 * This is saved per-world and tracks hunt progress across all players.
 */
public class CobaltAscendancyManager extends SavedData {
    private static final String DATA_NAME = SkysCobblemonCosmetics.MOD_ID + "_cobalt_ascendancy";

    private boolean huntActive = false;
    private boolean huntComplete = false;
    private UUID winnerUUID = null;
    private Set<UUID> playersWithOrbs = new HashSet<>();
    private long huntStartTime = 0;

    public CobaltAscendancyManager() {
    }

    public static CobaltAscendancyManager get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(CobaltAscendancyManager::new, CobaltAscendancyManager::load),
            DATA_NAME
        );
    }

    public static CobaltAscendancyManager load(CompoundTag tag, HolderLookup.Provider provider) {
        CobaltAscendancyManager manager = new CobaltAscendancyManager();
        manager.huntActive = tag.getBoolean("huntActive");
        manager.huntComplete = tag.getBoolean("huntComplete");
        manager.huntStartTime = tag.getLong("huntStartTime");

        if (tag.hasUUID("winnerUUID")) {
            manager.winnerUUID = tag.getUUID("winnerUUID");
        }

        ListTag playersTag = tag.getList("playersWithOrbs", Tag.TAG_INT_ARRAY);
        for (Tag t : playersTag) {
            manager.playersWithOrbs.add(NbtUtils.loadUUID(t));
        }

        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("huntActive", huntActive);
        tag.putBoolean("huntComplete", huntComplete);
        tag.putLong("huntStartTime", huntStartTime);

        if (winnerUUID != null) {
            tag.putUUID("winnerUUID", winnerUUID);
        }

        ListTag playersTag = new ListTag();
        for (UUID uuid : playersWithOrbs) {
            playersTag.add(NbtUtils.createUUID(uuid));
        }
        tag.put("playersWithOrbs", playersTag);

        return tag;
    }

    // --- Hunt State Management ---

    public void startHunt() {
        if (!huntActive && !huntComplete) {
            huntActive = true;
            huntStartTime = System.currentTimeMillis();
            setDirty();
            SkysCobblemonCosmetics.LOGGER.info("Cobalt Ascendancy hunt has begun!");
        }
    }

    public void completeHunt(UUID winner) {
        if (huntActive && !huntComplete) {
            huntComplete = true;
            huntActive = false;
            winnerUUID = winner;
            setDirty();
            SkysCobblemonCosmetics.LOGGER.info("Cobalt Ascendancy hunt completed by player: {}", winner);
        }
    }

    public void resetHunt() {
        huntActive = false;
        huntComplete = false;
        winnerUUID = null;
        playersWithOrbs.clear();
        huntStartTime = 0;
        setDirty();
        SkysCobblemonCosmetics.LOGGER.info("Cobalt Ascendancy hunt has been reset");
    }

    // --- Player Tracking ---

    public boolean hasPlayerObtainedOrb(UUID playerUUID) {
        return playersWithOrbs.contains(playerUUID);
    }

    public void markPlayerObtainedOrb(UUID playerUUID) {
        if (playersWithOrbs.add(playerUUID)) {
            setDirty();
        }
    }

    // --- Getters ---

    public boolean isHuntActive() {
        return huntActive;
    }

    public boolean isHuntComplete() {
        return huntComplete;
    }

    public UUID getWinner() {
        return winnerUUID;
    }

    public long getHuntStartTime() {
        return huntStartTime;
    }

    public int getParticipantCount() {
        return playersWithOrbs.size();
    }

    public Set<UUID> getPlayersWithOrbs() {
        return new HashSet<>(playersWithOrbs);
    }
}
