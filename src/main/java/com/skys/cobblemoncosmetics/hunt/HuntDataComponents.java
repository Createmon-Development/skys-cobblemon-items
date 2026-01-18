package com.skys.cobblemoncosmetics.hunt;

import com.mojang.serialization.Codec;
import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components for the Crystal Ascendancy hunt items
 */
@SuppressWarnings("removal")
public class HuntDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(SkysCobblemonCosmetics.MOD_ID);

    // Orb state: 0=EMPTY, 1=STAGE_1, 2=HALF, 3=FINAL (filled)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ORB_STATE =
        DATA_COMPONENTS.registerComponentType("orb_state", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    // Pokemon defeated count (for tracking progress toward next rune)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ORB_KILL_COUNT =
        DATA_COMPONENTS.registerComponentType("orb_kill_count", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    // Bitmask of revealed runes (which rune positions are unscrambled)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ORB_REVEALED_RUNES =
        DATA_COMPONENTS.registerComponentType("orb_revealed_runes", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    // Tablet state: false=faded, true=glowing (after glow ink applied)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TABLET_GLOWING =
        DATA_COMPONENTS.registerComponentType("tablet_glowing", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // Orb states enum for clarity
    public enum OrbState {
        EMPTY(0),
        STAGE_1(1),
        HALF(2),
        FINAL(3);

        private final int value;

        OrbState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static OrbState fromValue(int value) {
            return switch (value) {
                case 1 -> STAGE_1;
                case 2 -> HALF;
                case 3 -> FINAL;
                default -> EMPTY;
            };
        }
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
        SkysCobblemonCosmetics.LOGGER.info("Registered hunt data components");
    }
}
