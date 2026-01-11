package com.skys.cobblemoncosmetics.hunt;

import com.mojang.serialization.Codec;
import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Data components for the Mystery Orb item used in the Cobalt Ascendancy hunt.
 */
public class HuntDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, SkysCobblemonCosmetics.MOD_ID);

    // Orb state: 0 = UNFILLED, 1 = FILLING, 2 = FILLED
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ORB_STATE =
        DATA_COMPONENTS.register("orb_state", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT)
            .build());

    // Number of Pokemon defeated while holding the orb
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ORB_KILL_COUNT =
        DATA_COMPONENTS.register("orb_kill_count", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT)
            .build());

    // Bitmask of which letters have been revealed in the secret message
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ORB_REVEALED_LETTERS =
        DATA_COMPONENTS.register("orb_revealed_letters", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT)
            .build());

    // UUID of the last player who held this orb (for tracking who threw it)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> LAST_HOLDER =
        DATA_COMPONENTS.register("last_holder", () -> DataComponentType.<UUID>builder()
            .persistent(UUIDUtil.CODEC)
            .networkSynchronized(UUIDUtil.STREAM_CODEC)
            .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
        SkysCobblemonCosmetics.LOGGER.info("Registered hunt data components");
    }
}
