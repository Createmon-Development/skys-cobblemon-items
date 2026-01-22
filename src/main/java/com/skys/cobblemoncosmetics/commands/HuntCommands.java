package com.skys.cobblemoncosmetics.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.skys.cobblemoncosmetics.SkysCobblemonCosmetics;
import com.skys.cobblemoncosmetics.hunt.CrystalAscendancyManager;
import com.skys.cobblemoncosmetics.hunt.HuntDataComponents;
import com.skys.cobblemoncosmetics.hunt.MysteriousOrbItem;
import com.skys.cobblemoncosmetics.hunt.RunicCipherTabletItem;
import com.skys.cobblemoncosmetics.hunt.HuntConfig;
import com.skys.cobblemoncosmetics.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Commands for managing the Crystal Ascendancy Hunt stages.
 *
 * Usage:
 * /hunt stage <player> <stage>  - Set player to a specific hunt stage
 * /hunt progress <player>       - View player's current hunt progress
 * /hunt reset <player>          - Reset player's hunt progress
 *
 * Stages:
 * 1 - Not started
 * 2 - Has empty orb (start of hunt)
 * 3 - Has filled orb (runes revealed)
 * 4 - Has filled orb + faded tablet
 * 5 - Has filled orb + glowing tablet + parchment (complete)
 * 6 - Same as 5, but with X and Z coordinates solved (green)
 */
public class HuntCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hunt")
            .requires(source -> source.hasPermission(2)) // OP level 2 required
            .then(Commands.literal("stage")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("stage", IntegerArgumentType.integer(1, 6))
                        .executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            int stage = IntegerArgumentType.getInteger(context, "stage");
                            return setPlayerStage(context.getSource(), player, stage);
                        }))))
            .then(Commands.literal("progress")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        return showProgress(context.getSource(), player);
                    })))
            .then(Commands.literal("reset")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        return resetProgress(context.getSource(), player);
                    }))));

        SkysCobblemonCosmetics.LOGGER.info("Registered hunt commands");
    }

    private static int setPlayerStage(CommandSourceStack source, ServerPlayer player, int stage) {
        CrystalAscendancyManager manager = CrystalAscendancyManager.get(source.getServer());

        // Clear existing hunt items from inventory
        clearHuntItems(player);

        // Give items based on stage
        switch (stage) {
            case 2 -> {
                // Stage 2: Empty orb
                ItemStack orb = new ItemStack(ModItems.MYSTERIOUS_ORB.get());
                MysteriousOrbItem.setOrbState(orb, HuntDataComponents.OrbState.EMPTY);
                MysteriousOrbItem.setKillCount(orb, 0);
                MysteriousOrbItem.setRevealedRunes(orb, 0);
                player.getInventory().add(orb);
            }
            case 3 -> {
                // Stage 3: Filled orb (all runes revealed)
                ItemStack orb = new ItemStack(ModItems.MYSTERIOUS_ORB.get());
                MysteriousOrbItem.setOrbState(orb, HuntDataComponents.OrbState.FINAL);
                MysteriousOrbItem.setKillCount(orb, 0);
                MysteriousOrbItem.setRevealedRunes(orb, HuntConfig.TOTAL_RUNES);
                player.getInventory().add(orb);
            }
            case 4 -> {
                // Stage 4: Filled orb + faded tablet
                ItemStack orb = new ItemStack(ModItems.MYSTERIOUS_ORB.get());
                MysteriousOrbItem.setOrbState(orb, HuntDataComponents.OrbState.FINAL);
                MysteriousOrbItem.setKillCount(orb, 0);
                MysteriousOrbItem.setRevealedRunes(orb, HuntConfig.TOTAL_RUNES);
                player.getInventory().add(orb);

                ItemStack tablet = new ItemStack(ModItems.RUNIC_CIPHER_TABLET.get());
                RunicCipherTabletItem.setGlowing(tablet, false);
                player.getInventory().add(tablet);
            }
            case 5 -> {
                // Stage 5: Filled orb + glowing tablet + parchment
                ItemStack orb = new ItemStack(ModItems.MYSTERIOUS_ORB.get());
                MysteriousOrbItem.setOrbState(orb, HuntDataComponents.OrbState.FINAL);
                MysteriousOrbItem.setKillCount(orb, 0);
                MysteriousOrbItem.setRevealedRunes(orb, HuntConfig.TOTAL_RUNES);
                player.getInventory().add(orb);

                ItemStack tablet = new ItemStack(ModItems.RUNIC_CIPHER_TABLET.get());
                RunicCipherTabletItem.setGlowing(tablet, true);
                player.getInventory().add(tablet);

                ItemStack parchment = new ItemStack(ModItems.MYSTERIOUS_PARCHMENT.get());
                player.getInventory().add(parchment);
            }
            case 6 -> {
                // Stage 6: Same as 5, but with X and Z coordinates solved (green)
                ItemStack orb = new ItemStack(ModItems.MYSTERIOUS_ORB.get());
                MysteriousOrbItem.setOrbState(orb, HuntDataComponents.OrbState.FINAL);
                MysteriousOrbItem.setKillCount(orb, 0);
                MysteriousOrbItem.setRevealedRunes(orb, HuntConfig.TOTAL_RUNES);
                // Reveal all X digits (bits 0-3 = 0b1111 = 15)
                MysteriousOrbItem.setXDigits(orb, 0b1111);
                // Reveal all Z digits (bits 0-3 = 0b1111 = 15)
                MysteriousOrbItem.setZDigits(orb, 0b1111);
                player.getInventory().add(orb);

                ItemStack tablet = new ItemStack(ModItems.RUNIC_CIPHER_TABLET.get());
                RunicCipherTabletItem.setGlowing(tablet, true);
                player.getInventory().add(tablet);

                ItemStack parchment = new ItemStack(ModItems.MYSTERIOUS_PARCHMENT.get());
                player.getInventory().add(parchment);
            }
            // Stage 1: No items (just reset)
        }

        // Update tracked stage
        manager.setPlayerStage(player.getUUID(), stage);

        source.sendSuccess(() -> Component.literal("Set ")
            .append(player.getDisplayName())
            .append(Component.literal(" to hunt stage " + stage))
            .withStyle(ChatFormatting.GREEN), true);

        // Notify player
        player.sendSystemMessage(Component.literal("Your hunt progress has been set to stage " + stage)
            .withStyle(ChatFormatting.GOLD));

        return 1;
    }

    private static int showProgress(CommandSourceStack source, ServerPlayer player) {
        CrystalAscendancyManager manager = CrystalAscendancyManager.get(source.getServer());
        int stage = manager.getPlayerStage(player.getUUID());

        String stageName = switch (stage) {
            case 1 -> "Not Started";
            case 2 -> "Has Empty Orb";
            case 3 -> "Orb Filled (Runes Revealed)";
            case 4 -> "Has Tablet (Faded)";
            case 5 -> "Complete (Has Parchment)";
            case 6 -> "Complete (X & Z Solved)";
            default -> "Unknown";
        };

        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Hunt Progress for ").withStyle(ChatFormatting.GOLD))
            .append(player.getDisplayName())
            .append(Component.literal(":").withStyle(ChatFormatting.GOLD)), false);

        source.sendSuccess(() -> Component.literal("  Stage: ")
            .append(Component.literal(stage + " - " + stageName).withStyle(ChatFormatting.YELLOW)), false);

        // Check inventory for hunt items
        boolean hasOrb = false;
        boolean hasTablet = false;
        boolean hasParchment = false;
        int orbRunes = 0;
        boolean tabletGlowing = false;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.MYSTERIOUS_ORB.get())) {
                hasOrb = true;
                orbRunes = MysteriousOrbItem.getRevealedRunes(stack);
            } else if (stack.is(ModItems.RUNIC_CIPHER_TABLET.get())) {
                hasTablet = true;
                tabletGlowing = RunicCipherTabletItem.isGlowing(stack);
            } else if (stack.is(ModItems.MYSTERIOUS_PARCHMENT.get())) {
                hasParchment = true;
            }
        }

        final boolean fHasOrb = hasOrb;
        final int fOrbRunes = orbRunes;
        final boolean fHasTablet = hasTablet;
        final boolean fTabletGlowing = tabletGlowing;
        final boolean fHasParchment = hasParchment;

        source.sendSuccess(() -> Component.literal("  Orb: ")
            .append(Component.literal(fHasOrb ? "Yes (" + fOrbRunes + "/" + HuntConfig.TOTAL_RUNES + " runes)" : "No")
                .withStyle(fHasOrb ? ChatFormatting.GREEN : ChatFormatting.RED)), false);

        source.sendSuccess(() -> Component.literal("  Tablet: ")
            .append(Component.literal(fHasTablet ? (fTabletGlowing ? "Yes (Glowing)" : "Yes (Faded)") : "No")
                .withStyle(fHasTablet ? ChatFormatting.GREEN : ChatFormatting.RED)), false);

        source.sendSuccess(() -> Component.literal("  Parchment: ")
            .append(Component.literal(fHasParchment ? "Yes" : "No")
                .withStyle(fHasParchment ? ChatFormatting.GREEN : ChatFormatting.RED)), false);

        return 1;
    }

    private static int resetProgress(CommandSourceStack source, ServerPlayer player) {
        CrystalAscendancyManager manager = CrystalAscendancyManager.get(source.getServer());

        // Clear hunt items
        clearHuntItems(player);

        // Reset stage
        manager.clearPlayerStage(player.getUUID());

        source.sendSuccess(() -> Component.literal("Reset hunt progress for ")
            .append(player.getDisplayName())
            .withStyle(ChatFormatting.GREEN), true);

        player.sendSystemMessage(Component.literal("Your hunt progress has been reset.")
            .withStyle(ChatFormatting.GOLD));

        return 1;
    }

    /**
     * Removes all hunt-related items from player's inventory
     */
    private static void clearHuntItems(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.MYSTERIOUS_ORB.get()) ||
                stack.is(ModItems.RUNIC_CIPHER_TABLET.get()) ||
                stack.is(ModItems.MYSTERIOUS_PARCHMENT.get())) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
