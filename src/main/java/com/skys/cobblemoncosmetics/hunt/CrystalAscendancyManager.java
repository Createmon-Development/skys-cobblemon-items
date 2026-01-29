package com.skys.cobblemoncosmetics.hunt;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent world data for tracking the Cobalt Ascendancy race state.
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
    private final Map<UUID, Integer> playerStages = new HashMap<>(); // Hunt stage progress (1-6)

    // Completion tracking - ordered list of players who caught Kyogre
    private final List<UUID> completedPlayers = new ArrayList<>();

    // Dialogue tracking - which dialogue lines have been seen by each player
    // Key: UUID, Value: Set of "actionId:lineId" strings
    private final Map<UUID, Set<String>> seenDialogueLines = new HashMap<>();

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
            SkysCobblemonCosmetics.LOGGER.info("Cobalt Ascendancy race has started!");
        }
    }

    public void completeRace(UUID winner) {
        if (!raceComplete) {
            raceComplete = true;
            winnerUUID = winner;
            setDirty();
            SkysCobblemonCosmetics.LOGGER.info("Cobalt Ascendancy race completed! Winner: {}", winner);
        }
    }

    public void resetRace() {
        raceStarted = false;
        raceComplete = false;
        winnerUUID = null;
        raceStartTime = 0;
        playersInRace.clear();
        playerCooldowns.clear();
        completedPlayers.clear();
        setDirty();
        SkysCobblemonCosmetics.LOGGER.info("Cobalt Ascendancy race has been reset!");
    }

    // === Completion Tracking Methods ===

    /**
     * Records a player completing the hunt (catching Kyogre).
     * @return The player's placement (1st, 2nd, 3rd, etc.)
     */
    public int recordCompletion(UUID playerUUID) {
        if (!completedPlayers.contains(playerUUID)) {
            completedPlayers.add(playerUUID);
            setDirty();

            // First completion also sets the winner
            if (completedPlayers.size() == 1) {
                completeRace(playerUUID);
            }

            SkysCobblemonCosmetics.LOGGER.info("Player {} completed the hunt in position {}",
                playerUUID, completedPlayers.size());
        }
        return getPlayerPlacement(playerUUID);
    }

    /**
     * Gets a player's completion placement (1-indexed).
     * @return The placement number, or 0 if not completed.
     */
    public int getPlayerPlacement(UUID playerUUID) {
        int index = completedPlayers.indexOf(playerUUID);
        return index >= 0 ? index + 1 : 0;
    }

    /**
     * Checks if a player has completed the hunt.
     */
    public boolean hasCompleted(UUID playerUUID) {
        return completedPlayers.contains(playerUUID);
    }

    /**
     * Gets the total number of players who have completed the hunt.
     */
    public int getCompletionCount() {
        return completedPlayers.size();
    }

    /**
     * Gets all completed players in order.
     */
    public List<UUID> getCompletedPlayers() {
        return new ArrayList<>(completedPlayers);
    }

    /**
     * Clears a player's completion status (for testing purposes).
     * @return true if the player was in the completed list and was removed.
     */
    public boolean clearCompletion(UUID playerUUID) {
        if (completedPlayers.remove(playerUUID)) {
            // If this was the winner, clear that too
            if (playerUUID.equals(winnerUUID)) {
                winnerUUID = null;
                // If there are still other completed players, the next one becomes the winner
                if (!completedPlayers.isEmpty()) {
                    winnerUUID = completedPlayers.get(0);
                } else {
                    // No one has completed anymore
                    raceComplete = false;
                }
            }
            setDirty();
            SkysCobblemonCosmetics.LOGGER.info("Cleared completion status for player {}", playerUUID);
            return true;
        }
        return false;
    }

    // === Dialogue Tracking Methods ===

    /**
     * Marks a dialogue line as seen by a player.
     * @param playerUUID The player's UUID
     * @param actionId The action resource location (e.g., "cobblemoncustommerchants:treasure_hunter_dialogue")
     * @param lineId The dialogue line ID
     */
    public void markDialogueSeen(UUID playerUUID, String actionId, String lineId) {
        String key = actionId + ":" + lineId;
        seenDialogueLines.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(key);
        setDirty();
        SkysCobblemonCosmetics.LOGGER.debug("Marked dialogue line as seen for player {}: {}", playerUUID, key);
    }

    /**
     * Checks if a player has seen a specific dialogue line.
     */
    public boolean hasSeenDialogue(UUID playerUUID, String actionId, String lineId) {
        String key = actionId + ":" + lineId;
        Set<String> seen = seenDialogueLines.get(playerUUID);
        return seen != null && seen.contains(key);
    }

    /**
     * Gets all dialogue lines a player has seen.
     */
    public Set<String> getSeenDialogueLines(UUID playerUUID) {
        Set<String> seen = seenDialogueLines.get(playerUUID);
        return seen != null ? new HashSet<>(seen) : new HashSet<>();
    }

    /**
     * Clears all seen dialogue lines for a player.
     * Called when /hunt stage or /hunt reset is used.
     */
    public void clearSeenDialogue(UUID playerUUID) {
        if (seenDialogueLines.remove(playerUUID) != null) {
            setDirty();
            SkysCobblemonCosmetics.LOGGER.info("Cleared seen dialogue lines for player {}", playerUUID);
        }
    }

    /**
     * Clears a specific seen dialogue line for a player.
     * @param playerUUID The player's UUID
     * @param actionId The action resource location (e.g., "trade_action:mysterious_orb_trade")
     * @param lineId The dialogue line ID
     * @return true if the line was present and removed
     */
    public boolean clearSpecificDialogue(UUID playerUUID, String actionId, String lineId) {
        String key = actionId + ":" + lineId;
        Set<String> seen = seenDialogueLines.get(playerUUID);
        if (seen != null && seen.remove(key)) {
            setDirty();
            SkysCobblemonCosmetics.LOGGER.info("Cleared specific dialogue line for player {}: {}", playerUUID, key);
            return true;
        }
        return false;
    }

    /**
     * Clears all seen dialogue lines for all players.
     */
    public void clearAllSeenDialogue() {
        if (!seenDialogueLines.isEmpty()) {
            seenDialogueLines.clear();
            setDirty();
            SkysCobblemonCosmetics.LOGGER.info("Cleared all seen dialogue lines");
        }
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

    // === Stage Progress Methods ===

    /**
     * Gets the current hunt stage for a player.
     * Stages: 0=Pre-hunt (can trade), 1=Not started (traded already), 2=Has orb, 3=Orb filled, 4=Has tablet, 5=Complete, 6=X&Z solved
     */
    public int getPlayerStage(UUID playerUUID) {
        return playerStages.getOrDefault(playerUUID, 0);
    }

    /**
     * Sets the hunt stage for a player.
     * Stages: 0=Pre-hunt (can trade), 1=Not started (traded already), 2=Has orb, 3=Orb filled, 4=Has tablet, 5=Complete, 6=X&Z solved
     */
    public void setPlayerStage(UUID playerUUID, int stage) {
        if (stage < 0) stage = 0;
        if (stage > 6) stage = 6;
        playerStages.put(playerUUID, stage);
        setDirty();
        SkysCobblemonCosmetics.LOGGER.info("Player {} hunt stage set to {}", playerUUID, stage);
    }

    /**
     * Gets all player stages for debugging/admin purposes.
     */
    public Map<UUID, Integer> getAllPlayerStages() {
        return new HashMap<>(playerStages);
    }

    /**
     * Clears a player's stage progress.
     */
    public void clearPlayerStage(UUID playerUUID) {
        if (playerStages.remove(playerUUID) != null) {
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

        // Save player stages
        ListTag stageList = new ListTag();
        for (Map.Entry<UUID, Integer> entry : playerStages.entrySet()) {
            CompoundTag stageTag = new CompoundTag();
            stageTag.putUUID("uuid", entry.getKey());
            stageTag.putInt("stage", entry.getValue());
            stageList.add(stageTag);
        }
        tag.put("playerStages", stageList);

        // Save completed players (in order)
        ListTag completedList = new ListTag();
        for (UUID uuid : completedPlayers) {
            CompoundTag completedTag = new CompoundTag();
            completedTag.putUUID("uuid", uuid);
            completedList.add(completedTag);
        }
        tag.put("completedPlayers", completedList);

        // Save seen dialogue lines
        ListTag dialogueList = new ListTag();
        for (Map.Entry<UUID, Set<String>> entry : seenDialogueLines.entrySet()) {
            CompoundTag dialogueTag = new CompoundTag();
            dialogueTag.putUUID("uuid", entry.getKey());
            ListTag linesList = new ListTag();
            for (String line : entry.getValue()) {
                CompoundTag lineTag = new CompoundTag();
                lineTag.putString("line", line);
                linesList.add(lineTag);
            }
            dialogueTag.put("lines", linesList);
            dialogueList.add(dialogueTag);
        }
        tag.put("seenDialogueLines", dialogueList);

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

        // Load player stages
        ListTag stageList = tag.getList("playerStages", Tag.TAG_COMPOUND);
        for (int i = 0; i < stageList.size(); i++) {
            CompoundTag stageTag = stageList.getCompound(i);
            manager.playerStages.put(
                stageTag.getUUID("uuid"),
                stageTag.getInt("stage")
            );
        }

        // Load completed players (in order)
        ListTag completedList = tag.getList("completedPlayers", Tag.TAG_COMPOUND);
        for (int i = 0; i < completedList.size(); i++) {
            CompoundTag completedTag = completedList.getCompound(i);
            manager.completedPlayers.add(completedTag.getUUID("uuid"));
        }

        // Load seen dialogue lines
        ListTag dialogueList = tag.getList("seenDialogueLines", Tag.TAG_COMPOUND);
        for (int i = 0; i < dialogueList.size(); i++) {
            CompoundTag dialogueTag = dialogueList.getCompound(i);
            UUID uuid = dialogueTag.getUUID("uuid");
            Set<String> lines = new HashSet<>();
            ListTag linesList = dialogueTag.getList("lines", Tag.TAG_COMPOUND);
            for (int j = 0; j < linesList.size(); j++) {
                CompoundTag lineTag = linesList.getCompound(j);
                lines.add(lineTag.getString("line"));
            }
            manager.seenDialogueLines.put(uuid, lines);
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
