package com.skys.cobblemoncosmetics.commands;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Handles registration of all mod commands.
 */
@EventBusSubscriber
public class CommandRegistration {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HuntCommands.register(event.getDispatcher());
    }
}
