package com.skys.cobblemoncosmetics.sounds;

import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, SkysCobblemonCosmetics.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ORB_WHISPER =
            SOUND_EVENTS.register("orb_whisper",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(SkysCobblemonCosmetics.MOD_ID, "orb_whisper")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
