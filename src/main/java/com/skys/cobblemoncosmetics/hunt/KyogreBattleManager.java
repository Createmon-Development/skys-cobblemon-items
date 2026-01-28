package com.skys.cobblemoncosmetics.hunt;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.items.ModItems;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the Kyogre boss encounter for the Cobalt Ascendancy hunt.
 *
 * Trigger: Player throws a powered orb (X and Z revealed) into liquid at the Cove.
 * Outcomes:
 * - Catch: Player completes hunt, gets edition number, broadcast sent
 * - Kill/Flee: Orb resets to base form, player can try again
 */
public class KyogreBattleManager {

    // Cove detection radius (blocks from center)
    private static final int COVE_RADIUS = 50;

    // Ritual animation duration in ticks (5 seconds = 100 ticks)
    private static final int RITUAL_DURATION_TICKS = 100;
    // How high the orb rises (in blocks)
    private static final double ORB_RISE_HEIGHT = 4.0;

    // Track spawned Kyogre entities and their associated player/orb data
    private static final Map<UUID, BattleContext> spawnedKyogres = new HashMap<>();

    // Track players currently in a Kyogre battle
    private static final Set<UUID> playersInKyogreBattle = new HashSet<>();

    // Track active rituals (orb rising animation before spawn)
    private static final Map<UUID, RitualContext> activeRituals = new HashMap<>();

    // Track orb entity UUIDs that have already been processed to prevent duplicate ritual starts
    private static final Set<UUID> processedOrbEntities = new HashSet<>();

    // Track last game tick to ensure rituals only tick once per game tick
    private static long lastRitualTickTime = 0;

    public static void register() {
        // Subscribe to Pokemon captured event
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            handlePokemonCaptured(event);
            return Unit.INSTANCE;
        });

        // Subscribe to battle victory event to handle defeat outcomes
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            handleBattleVictory(event);
            return Unit.INSTANCE;
        });

        // Subscribe to battle fled event to handle player fleeing
        CobblemonEvents.BATTLE_FLED.subscribe(Priority.NORMAL, event -> {
            handleBattleFled(event);
            return Unit.INSTANCE;
        });

        SkysCobblemonCosmetics.LOGGER.info("Registered Kyogre battle manager for Cobalt Ascendancy hunt");
    }

    /**
     * Called from EntityTickEvent to check thrown orbs and tick active rituals
     */
    public static void onEntityTick(EntityTickEvent.Post event) {
        // First, process active rituals if we're on server side
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            tickRituals(serverLevel);
        }

        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        if (!(itemEntity.level() instanceof ServerLevel serverLevel)) return;

        ItemStack stack = itemEntity.getItem();
        if (!stack.is(ModItems.MYSTERIOUS_ORB.get())) return;

        // Check if this specific orb entity was already processed (prevents duplicate triggers)
        if (processedOrbEntities.contains(itemEntity.getUUID())) return;

        // Only check every 10 ticks to reduce overhead
        if (itemEntity.tickCount % 10 != 0) return;

        // Check if orb is in liquid
        BlockPos pos = itemEntity.blockPosition();
        if (!serverLevel.getFluidState(pos).is(Fluids.WATER) &&
            !serverLevel.getFluidState(pos).is(Fluids.FLOWING_WATER)) {
            return;
        }

        // Check if orb is at the Cove
        if (!isAtCove(pos)) return;

        // FIRST: Check if orb already has an edition (already completed) - this must be checked first
        // to prevent any completed orb from triggering a new ritual
        Integer edition = stack.get(HuntDataComponents.ORB_EDITION.get());
        if (edition != null && edition > 0) return;

        // Check if orb is powered (X and Z digits all revealed)
        if (!isOrbPowered(stack)) return;

        // Find the player who threw the orb (owner)
        UUID throwerUUID = itemEntity.getOwner() != null ? itemEntity.getOwner().getUUID() : null;
        if (throwerUUID == null) return;

        // Check if player has already completed the hunt BEFORE any other processing
        // This prevents any action if the player is already done
        CrystalAscendancyManager manager = CrystalAscendancyManager.get(serverLevel);
        if (manager.hasCompleted(throwerUUID)) {
            // Player already completed - show message if they're online
            ServerPlayer completedPlayer = serverLevel.getServer().getPlayerList().getPlayer(throwerUUID);
            if (completedPlayer != null) {
                completedPlayer.sendSystemMessage(Component.literal("")
                    .append(Component.literal("You have already captured Kyogre and completed the hunt.")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)));
                completedPlayer.sendSystemMessage(Component.literal("")
                    .append(Component.literal("The orb's power has been fulfilled - Kyogre will not answer again.")
                        .withStyle(ChatFormatting.GRAY)));
            }
            return;
        }

        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(throwerUUID);
        if (player == null) return;

        // Check if player already has an active Kyogre or ritual
        if (playersInKyogreBattle.contains(throwerUUID)) return;
        if (activeRituals.containsKey(throwerUUID)) return;

        // Mark this orb entity as processed BEFORE discarding to prevent duplicate triggers
        processedOrbEntities.add(itemEntity.getUUID());

        // Store the orb data and start the ritual animation
        ItemStack orbCopy = stack.copy();
        Vec3 startPos = itemEntity.position();
        itemEntity.discard();

        // Start ritual animation instead of spawning immediately
        startRitual(serverLevel, player, orbCopy, startPos);
    }

    /**
     * Checks if a position is within the Cove area
     */
    private static boolean isAtCove(BlockPos pos) {
        double dx = pos.getX() - HuntConfig.COVE_X;
        double dy = pos.getY() - HuntConfig.COVE_Y;
        double dz = pos.getZ() - HuntConfig.COVE_Z;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= COVE_RADIUS * COVE_RADIUS;
    }

    /**
     * Checks if the orb is powered (X and Z coordinates fully revealed)
     */
    private static boolean isOrbPowered(ItemStack stack) {
        // Check orb is in FINAL state
        HuntDataComponents.OrbState state = MysteriousOrbItem.getOrbState(stack);
        if (state != HuntDataComponents.OrbState.FINAL) return false;

        // Check all X digits revealed (bits 0-3 set = 0b1111 = 15)
        int xDigits = MysteriousOrbItem.getXDigits(stack);
        if ((xDigits & 0b1111) != 0b1111) return false;

        // Check all Z digits revealed (bits 0-3 set = 0b1111 = 15)
        int zDigits = MysteriousOrbItem.getZDigits(stack);
        if ((zDigits & 0b1111) != 0b1111) return false;

        return true;
    }

    /**
     * Starts the ritual animation before spawning Kyogre
     */
    private static void startRitual(ServerLevel level, ServerPlayer player, ItemStack orbStack, Vec3 startPos) {
        RitualContext ritual = new RitualContext(
            player.getUUID(),
            orbStack,
            startPos,
            level,
            0
        );
        activeRituals.put(player.getUUID(), ritual);

        // Send dramatic message to the player only (broadcast happens at trade time via cobblemon-custom-merchants)
        player.sendSystemMessage(Component.literal("").append(
            Component.literal("The waters churn violently as an ancient power awakens...")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));

        // Play initial sound
        level.playSound(null, BlockPos.containing(startPos),
            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 1.0f, 0.5f);

        SkysCobblemonCosmetics.LOGGER.info("Started Kyogre ritual for player {} at {}",
            player.getName().getString(), startPos);
    }

    /**
     * Ticks all active rituals (only once per game tick)
     */
    private static void tickRituals(ServerLevel level) {
        // Only tick rituals once per game tick, not per entity
        long currentTime = level.getGameTime();
        if (currentTime == lastRitualTickTime) {
            return; // Already processed this tick
        }
        lastRitualTickTime = currentTime;

        Iterator<Map.Entry<UUID, RitualContext>> iterator = activeRituals.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, RitualContext> entry = iterator.next();
            RitualContext ritual = entry.getValue();

            // Increment tick
            ritual.tickCount++;

            // Calculate progress (0.0 to 1.0)
            float progress = (float) ritual.tickCount / RITUAL_DURATION_TICKS;

            // Calculate current position (rising from water)
            double currentY = ritual.startPos.y + (ORB_RISE_HEIGHT * progress);
            Vec3 currentPos = new Vec3(ritual.startPos.x, currentY, ritual.startPos.z);

            // Spawn particles around the rising orb position
            spawnRitualParticles(level, currentPos, progress);

            // Play escalating sounds at intervals
            if (ritual.tickCount % 20 == 0) { // Every second
                playRitualSound(level, currentPos, progress);
            }

            // Check if player is still online
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(ritual.playerUUID);
            if (player == null) {
                // Player disconnected - cancel ritual and return orb later
                SkysCobblemonCosmetics.LOGGER.info("Ritual cancelled - player disconnected");
                iterator.remove();
                continue;
            }

            // Check if ritual is complete
            if (ritual.tickCount >= RITUAL_DURATION_TICKS) {
                // Ritual complete - spawn Kyogre!
                iterator.remove();
                BlockPos spawnPos = BlockPos.containing(currentPos);
                spawnKyogre(level, player, ritual.orbStack, spawnPos);
            }
        }
    }

    /**
     * Spawns particles for the ritual animation
     */
    private static void spawnRitualParticles(ServerLevel level, Vec3 pos, float progress) {
        // Number of particles increases with progress
        int particleCount = (int) (5 + progress * 20);

        // Spiral effect around the orb
        double time = level.getGameTime() * 0.1;
        for (int i = 0; i < particleCount; i++) {
            double angle = time + (i * (Math.PI * 2 / particleCount));
            double radius = 0.5 + progress * 1.5;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            // Soul flame particles (mystical blue)
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                pos.x + offsetX, pos.y, pos.z + offsetZ,
                1, 0.1, 0.1, 0.1, 0.01);

            // End rod particles (sparkles)
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD,
                    pos.x + offsetX * 0.5, pos.y + 0.5, pos.z + offsetZ * 0.5,
                    1, 0.05, 0.1, 0.05, 0.02);
            }
        }

        // Water splash particles below
        level.sendParticles(ParticleTypes.SPLASH,
            pos.x, pos.y - 0.5, pos.z,
            3, 0.3, 0.1, 0.3, 0.1);

        // Bubble column effect as progress increases
        if (progress > 0.5) {
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                pos.x, pos.y - 1, pos.z,
                2, 0.5, 0, 0.5, 0.05);
        }
    }

    /**
     * Plays escalating sounds during the ritual
     */
    private static void playRitualSound(ServerLevel level, Vec3 pos, float progress) {
        BlockPos blockPos = BlockPos.containing(pos);

        // Volume and pitch increase with progress
        float volume = 0.5f + progress * 1.0f;
        float pitch = 0.5f + progress * 0.5f;

        // Different sounds at different stages
        if (progress < 0.33f) {
            level.playSound(null, blockPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, volume, pitch);
        } else if (progress < 0.66f) {
            level.playSound(null, blockPos, SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, volume, pitch);
            level.playSound(null, blockPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, volume * 0.7f, pitch);
        } else {
            level.playSound(null, blockPos, SoundEvents.BEACON_POWER_SELECT, SoundSource.AMBIENT, volume, pitch);
            level.playSound(null, blockPos, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.WEATHER, volume * 0.5f, 0.8f);
        }

        // Final dramatic sound just before spawn
        if (progress > 0.9f) {
            level.playSound(null, blockPos, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0f, 0.6f);
        }
    }

    /**
     * Spawns Kyogre as a wild Pokemon that the player can battle
     */
    private static void spawnKyogre(ServerLevel level, ServerPlayer player, ItemStack orbStack, BlockPos pos) {
        // Get Kyogre species
        Species kyogreSpecies = PokemonSpecies.INSTANCE.getByName("kyogre");
        if (kyogreSpecies == null) {
            SkysCobblemonCosmetics.LOGGER.error("Could not find Kyogre species!");
            // Give the orb back to the player
            if (!player.getInventory().add(orbStack)) {
                player.drop(orbStack, false);
            }
            return;
        }

        // Create the Kyogre Pokemon
        Pokemon kyogre = kyogreSpecies.create(70); // Level 70
        kyogre.setShiny(false);

        // Play dramatic sound effects
        level.playSound(null, pos, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0f, 0.5f);
        level.playSound(null, pos, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.5f, 0.8f);

        // Spawn the Kyogre entity as a wild Pokemon
        PokemonEntity kyogreEntity = kyogre.sendOut(level, pos.above().getCenter(), null, pokemon -> Unit.INSTANCE);

        if (kyogreEntity == null) {
            SkysCobblemonCosmetics.LOGGER.error("Failed to spawn Kyogre entity!");
            if (!player.getInventory().add(orbStack)) {
                player.drop(orbStack, false);
            }
            return;
        }

        // Store context for this Kyogre
        BattleContext context = new BattleContext(player.getUUID(), orbStack, kyogreEntity.getUUID());
        spawnedKyogres.put(kyogreEntity.getUUID(), context);
        playersInKyogreBattle.add(player.getUUID());

        // Immediately start battle with the player
        player.sendSystemMessage(Component.literal("")
            .append(Component.literal("KYOGRE ").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD))
            .append(Component.literal("challenges you to battle!").withStyle(ChatFormatting.WHITE)));

        // Use Cobblemon's BattleBuilder to start a PvE battle
        try {
            BattleBuilder.INSTANCE.pve(player, kyogreEntity).ifSuccessful(battle -> {
                SkysCobblemonCosmetics.LOGGER.info("Kyogre battle started for player {}", player.getName().getString());
                return Unit.INSTANCE;
            }).ifErrored(error -> {
                SkysCobblemonCosmetics.LOGGER.error("Failed to start Kyogre battle: {}", error);
                // Clean up and return orb
                kyogreEntity.discard();
                spawnedKyogres.remove(kyogreEntity.getUUID());
                playersInKyogreBattle.remove(player.getUUID());
                if (!player.getInventory().add(orbStack)) {
                    player.drop(orbStack, false);
                }
                player.sendSystemMessage(Component.literal("Failed to start battle - orb returned.")
                    .withStyle(ChatFormatting.RED));
                return Unit.INSTANCE;
            });
        } catch (Exception e) {
            SkysCobblemonCosmetics.LOGGER.error("Exception starting Kyogre battle: {}", e.getMessage());
            kyogreEntity.discard();
            spawnedKyogres.remove(kyogreEntity.getUUID());
            playersInKyogreBattle.remove(player.getUUID());
            if (!player.getInventory().add(orbStack)) {
                player.drop(orbStack, false);
            }
        }

        SkysCobblemonCosmetics.LOGGER.info("Spawned Kyogre for player {} at {}",
            player.getName().getString(), pos);
    }

    /**
     * Handles Pokemon captured event - check if it's our Kyogre
     */
    private static void handlePokemonCaptured(PokemonCapturedEvent event) {
        Pokemon pokemon = event.getPokemon();
        ServerPlayer player = event.getPlayer();

        // Check if this is a Kyogre
        if (!pokemon.getSpecies().getName().equalsIgnoreCase("kyogre")) return;

        // Check if this player was in a Kyogre battle
        BattleContext context = null;
        for (BattleContext ctx : spawnedKyogres.values()) {
            if (ctx.playerUUID.equals(player.getUUID())) {
                context = ctx;
                break;
            }
        }

        if (context == null) return;

        // Player caught our Kyogre!
        handleKyogreCaught(player, context);

        // Clean up
        spawnedKyogres.remove(context.kyogreEntityUUID);
        playersInKyogreBattle.remove(player.getUUID());
    }

    /**
     * Handles battle victory event to check for Kyogre defeat
     */
    private static void handleBattleVictory(BattleVictoryEvent event) {
        // Check winners - if player won, they defeated Kyogre
        for (var actor : event.getWinners()) {
            if (actor instanceof PlayerBattleActor playerActor) {
                ServerPlayer player = playerActor.getEntity();
                if (player == null) continue;

                // Check if player was in a Kyogre battle
                BattleContext context = null;
                for (BattleContext ctx : spawnedKyogres.values()) {
                    if (ctx.playerUUID.equals(player.getUUID())) {
                        context = ctx;
                        break;
                    }
                }

                if (context != null) {
                    // Player defeated Kyogre (didn't catch it) - reset orb
                    handleKyogreDefeated(player, context);
                    spawnedKyogres.remove(context.kyogreEntityUUID);
                    playersInKyogreBattle.remove(player.getUUID());
                }
            }
        }

        // Check losers - if player lost (fled or fainted), reset orb
        for (var actor : event.getLosers()) {
            if (actor instanceof PlayerBattleActor playerActor) {
                ServerPlayer player = playerActor.getEntity();
                if (player == null) continue;

                // Check if player was in a Kyogre battle
                BattleContext context = null;
                for (BattleContext ctx : spawnedKyogres.values()) {
                    if (ctx.playerUUID.equals(player.getUUID())) {
                        context = ctx;
                        break;
                    }
                }

                if (context != null) {
                    // Player fled or lost - reset orb and despawn Kyogre
                    handleKyogreDefeated(player, context);
                    despawnKyogre(player.serverLevel(), context);
                    spawnedKyogres.remove(context.kyogreEntityUUID);
                    playersInKyogreBattle.remove(player.getUUID());
                }
            }
        }
    }

    /**
     * Handles battle fled event to handle player fleeing from Kyogre
     */
    private static void handleBattleFled(BattleFledEvent event) {
        // Get the battle and check all actors
        var battle = event.getBattle();

        for (var actor : battle.getActors()) {
            if (actor instanceof PlayerBattleActor playerActor) {
                ServerPlayer player = playerActor.getEntity();
                if (player == null) continue;

                // Check if this player was in a Kyogre battle
                BattleContext context = null;
                for (BattleContext ctx : spawnedKyogres.values()) {
                    if (ctx.playerUUID.equals(player.getUUID())) {
                        context = ctx;
                        break;
                    }
                }

                if (context != null) {
                    SkysCobblemonCosmetics.LOGGER.info("Player {} fled from Kyogre battle", player.getName().getString());

                    // Player fled - reset orb and despawn Kyogre
                    handleKyogreDefeated(player, context);
                    despawnKyogre(player.serverLevel(), context);
                    spawnedKyogres.remove(context.kyogreEntityUUID);
                    playersInKyogreBattle.remove(player.getUUID());
                }
            }
        }
    }

    /**
     * Handles player death event - despawn Kyogre if the player was in a battle
     */
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Check if this player was in a Kyogre battle
        BattleContext context = null;
        for (BattleContext ctx : spawnedKyogres.values()) {
            if (ctx.playerUUID.equals(player.getUUID())) {
                context = ctx;
                break;
            }
        }

        if (context == null) return;

        SkysCobblemonCosmetics.LOGGER.info("Player {} died during Kyogre battle - despawning Kyogre",
            player.getName().getString());

        // Despawn Kyogre and reset orb
        handleKyogreDefeated(player, context);
        despawnKyogre(player.serverLevel(), context);
        spawnedKyogres.remove(context.kyogreEntityUUID);
        playersInKyogreBattle.remove(player.getUUID());
    }

    /**
     * Despawns the Kyogre entity
     */
    private static void despawnKyogre(ServerLevel level, BattleContext context) {
        Entity entity = level.getEntity(context.kyogreEntityUUID);
        if (entity != null) {
            entity.discard();
            SkysCobblemonCosmetics.LOGGER.info("Despawned Kyogre entity {}", context.kyogreEntityUUID);
        }
    }

    /**
     * Handles successful Kyogre catch - player completes the hunt
     */
    private static void handleKyogreCaught(ServerPlayer player, BattleContext context) {
        ServerLevel level = player.serverLevel();
        CrystalAscendancyManager manager = CrystalAscendancyManager.get(level);

        // Record completion and get placement
        int placement = manager.recordCompletion(player.getUUID());

        // Create completed orb with edition
        ItemStack completedOrb = context.orbStack.copy();
        completedOrb.set(HuntDataComponents.ORB_EDITION.get(), placement);

        // Give the orb back to player
        if (!player.getInventory().add(completedOrb)) {
            player.drop(completedOrb, false);
        }

        // Apply the Cobalt Champion mark to the captured Kyogre
        applyChampionMark(player, placement);

        // Send success message to player
        player.sendSystemMessage(Component.literal("")
            .append(Component.literal("Your orb and Kyogre now bear the mark of completion: ").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
            .append(Component.literal("Edition #" + placement).withStyle(ChatFormatting.GOLD)));

        // Play victory sounds
        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Broadcast to server
        String ordinalSuffix = getOrdinalSuffix(placement);
        Component broadcast = Component.literal("")
            .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .append(Component.literal(" has completed the path for the ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal("Cobalt Ascendancy").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD))
            .append(Component.literal(". They have claimed ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(placement + ordinalSuffix + " place").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal("!").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

        for (ServerPlayer serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            serverPlayer.sendSystemMessage(broadcast);
        }

        SkysCobblemonCosmetics.LOGGER.info("Player {} completed Cobalt Ascendancy in {} place",
            player.getName().getString(), placement);
    }

    /**
     * Applies the appropriate Cobalt Champion mark to the player's most recently caught Kyogre.
     * Uses the /givemark command to apply the mark.
     */
    private static void applyChampionMark(ServerPlayer player, int placement) {
        // Find the Kyogre in the player's party
        var partyStore = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
        int kyogreSlot = -1;

        for (int i = 0; i < partyStore.size(); i++) {
            var pokemon = partyStore.get(i);
            if (pokemon != null && pokemon.getSpecies().getName().equalsIgnoreCase("kyogre")) {
                kyogreSlot = i + 1; // Command uses 1-indexed slots
                SkysCobblemonCosmetics.LOGGER.info("Found Kyogre in slot {} for player {}", kyogreSlot, player.getName().getString());
                break;
            }
        }

        if (kyogreSlot == -1) {
            SkysCobblemonCosmetics.LOGGER.warn("Could not find Kyogre in player's party to apply mark. Party size: {}", partyStore.size());
            return;
        }

        // Determine mark name based on placement (max 10 pre-created marks)
        // Must use cobblemon namespace as marks are loaded from data/cobblemon/marks/
        String markName = "cobblemon:cobalt_champion_" + Math.min(placement, 10);

        // Execute the givemark command
        String command = String.format("givemark %s %d %s", player.getName().getString(), kyogreSlot, markName);
        SkysCobblemonCosmetics.LOGGER.info("Executing mark command: {}", command);

        try {
            var commandSource = player.server.createCommandSourceStack()
                .withPermission(4) // OP level to bypass permission checks
                .withSuppressedOutput();
            player.server.getCommands().performPrefixedCommand(commandSource, command);
            SkysCobblemonCosmetics.LOGGER.info("Mark command executed successfully");
        } catch (Exception e) {
            SkysCobblemonCosmetics.LOGGER.error("Failed to apply champion mark via command: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles Kyogre defeat or player flee - reset the orb
     */
    private static void handleKyogreDefeated(ServerPlayer player, BattleContext context) {
        // Reset orb to base form (empty, no progress)
        ItemStack resetOrb = new ItemStack(ModItems.MYSTERIOUS_ORB.get());
        MysteriousOrbItem.setOrbState(resetOrb, HuntDataComponents.OrbState.EMPTY);
        MysteriousOrbItem.setKillCount(resetOrb, 0);
        MysteriousOrbItem.setRevealedRunes(resetOrb, 0);
        MysteriousOrbItem.setXDigits(resetOrb, 0);
        MysteriousOrbItem.setYDigits(resetOrb, 0);
        MysteriousOrbItem.setZDigits(resetOrb, 0);

        // Give the reset orb back
        if (!player.getInventory().add(resetOrb)) {
            player.drop(resetOrb, false);
        }

        // Send failure message
        player.sendSystemMessage(Component.literal("").append(
            Component.literal("Kyogre has returned to the depths...")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        player.sendSystemMessage(Component.literal("").append(
            Component.literal("Your orb has lost its power. You must begin again.")
                .withStyle(ChatFormatting.RED)));

        // Play sad sound
        player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 0.8f, 0.5f);

        SkysCobblemonCosmetics.LOGGER.info("Player {} failed Kyogre battle - orb reset",
            player.getName().getString());
    }

    /**
     * Gets ordinal suffix for a number (1st, 2nd, 3rd, etc.)
     */
    private static String getOrdinalSuffix(int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    /**
     * Context for tracking a spawned Kyogre
     */
    private static class BattleContext {
        final UUID playerUUID;
        final ItemStack orbStack;
        final UUID kyogreEntityUUID;

        BattleContext(UUID playerUUID, ItemStack orbStack, UUID kyogreEntityUUID) {
            this.playerUUID = playerUUID;
            this.orbStack = orbStack;
            this.kyogreEntityUUID = kyogreEntityUUID;
        }
    }

    /**
     * Context for tracking an active ritual animation
     */
    private static class RitualContext {
        final UUID playerUUID;
        final ItemStack orbStack;
        final Vec3 startPos;
        final ServerLevel level;
        int tickCount;

        RitualContext(UUID playerUUID, ItemStack orbStack, Vec3 startPos, ServerLevel level, int tickCount) {
            this.playerUUID = playerUUID;
            this.orbStack = orbStack;
            this.startPos = startPos;
            this.level = level;
            this.tickCount = tickCount;
        }
    }
}
