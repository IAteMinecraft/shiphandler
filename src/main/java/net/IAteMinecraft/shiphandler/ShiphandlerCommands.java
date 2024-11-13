package net.IAteMinecraft.shiphandler;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.ShipArgument;

public class ShiphandlerCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("ship-handler")
            .then(Commands.literal("ship")
                .then(Commands.literal("add")
                .requires((player) -> player.hasPermission(2))
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> registerShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), true))
                    )
                )
                .then(Commands.literal("remove")
                .requires((player) -> player.hasPermission(2))
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> unregisterShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), true))
                    )
                )
                .then(Commands.literal("register")
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> registerShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), false))
                    )
                )
                .then(Commands.literal("unregister")
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> unregisterShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), false))
                    )
                )
                .then(Commands.literal("autoRegister")
                    .then(Commands.argument("true|false", BoolArgumentType.bool())
                        .executes(command -> setAutoRegister(command, BoolArgumentType.getBool(command, "true|false")))
                    )
                    .executes(ShiphandlerCommands::getAutoRegister)
                )
            )
            .then(Commands.literal("handler")
            .requires((player) -> player.hasPermission(2))
                .then(Commands.literal("run")
                    .executes(ShiphandlerCommands::handlerRun)
                )
            )
            .then(Commands.literal("get-id")
                .then(Commands.argument("ship", ShipArgument.Companion.ships())
                    .executes(command -> getId(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship")))
                )
            )
            .then(Commands.literal("list")
                .then(Commands.literal("all-created-ships")
                    .requires((player) -> player.hasPermission(4))
                    .executes(ShiphandlerCommands::debugListAll)
                )
                .then(Commands.literal("all-registered-ships")
                    .requires((player) -> player.hasPermission(4))
                    .executes(ShiphandlerCommands::listShips)
                )
                .then(Commands.literal("created-ships")
                    .executes(ShiphandlerCommands::debugList)
                )
                .then(Commands.literal("list")
                    .executes(ShiphandlerCommands::list)
                )
            )
        );
    }

    private static int handlerRun(CommandContext<CommandSourceStack> command) {
        HandleShips.handle();
        command.getSource().sendSystemMessage(Component.literal("Successfully ran the scheduler!"));
        return Command.SINGLE_SUCCESS;
    }
    // Clean up HandleShips when the world is unloaded
    public static void onWorldUnload() {
        /*if (currentHandleShipsInstance != null) {
            currentHandleShipsInstance.stop();
            currentHandleShipsInstance = null;
        }*/
    }

    private static int listShips(CommandContext<CommandSourceStack> command) {
        if(command.getSource().getEntity() instanceof Player player){
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            player.sendSystemMessage(Component.literal("Registered Ships: \n" + dataStore.getAllRegisteredShipSlugsWithPlayerName()));
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }

    private static int list(CommandContext<CommandSourceStack> command) {
        if(command.getSource().getEntity() instanceof Player player){
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            player.sendSystemMessage(Component.literal("Registered Ships: " + dataStore.getRegisteredShipsSlug(player)));
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }

    private static int debugListAll(CommandContext<CommandSourceStack> command) {
        if(command.getSource().getEntity() instanceof Player player){
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            player.sendSystemMessage(Component.literal("Created Ships: \n" + dataStore.getAllShipSlugsWithPlayerName()));
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }

    private static int debugList(CommandContext<CommandSourceStack> command) {
        if(command.getSource().getEntity() instanceof Player player){
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            player.sendSystemMessage(Component.literal("Created Ships: " + dataStore.getShipsSlug(player)));
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }

    private static int registerShip(CommandContext<CommandSourceStack> command, Ship ship, boolean op){
        if (command.getSource().getEntity() instanceof Player player) {
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            if (dataStore.getRegisteredShipsId(player).contains(ship.getId()) && !op) {
                player.sendSystemMessage(Component.literal("§4You already own that ship"));
                return Command.SINGLE_SUCCESS;
            } else if (dataStore.getAllRegisteredShipIDs().contains(ship.getId()) && !op) {
                player.sendSystemMessage(Component.literal("§4That Ship is already owned by another player"));
                return Command.SINGLE_SUCCESS;
            } else if ((dataStore.getMaxShips(player) == -1 || dataStore.getCurrentRegisteredShipCount(player)+1 > dataStore.getMaxShips(player)) && !(op ? ShiphandlerConfig.infOpShips.get() : false)) {
                player.sendSystemMessage(Component.literal("§4You have reached your maximum amount of ships!\nConsider removing some ships, or turn off autoRegister"));
                return Command.SINGLE_SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("Registered Ship: " + ship.getSlug() + " for player: ").append(player.getDisplayName()));
                if (dataStore.registerShip(player, ship))
                    return Command.SINGLE_SUCCESS;
                else
                    return 0;
            }
        } else
            return 0;
    }

    private static int unregisterShip(CommandContext<CommandSourceStack> command, Ship ship, boolean op){
        if (command.getSource().getEntity() instanceof Player player) {
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            if (dataStore.getRegisteredShipsId(player).contains(ship.getId()) || op) {
                player.sendSystemMessage(Component.literal("Unregistered Ship: " + ship.getSlug() + " for player: " + dataStore.getPlayerName(ship.getId())));
                if (dataStore.unregisterShip(player, ship.getId()))
                    return Command.SINGLE_SUCCESS;
                else
                    return 0;
            } else {
                player.sendSystemMessage(Component.literal("§4You don't own that ship"));
                return Command.SINGLE_SUCCESS;
            }
        } else
            return 0;
    }

    private static int getId(CommandContext<CommandSourceStack> command, Ship ship){
        if (command.getSource().getEntity() instanceof Player player) {
            player.sendSystemMessage(Component.literal(String.valueOf(ship.getId())));
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }

    private static int setAutoRegister(CommandContext<CommandSourceStack> command, boolean autoRegister) {
        if (command.getSource().getEntity() instanceof Player player) {
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);

            dataStore.setAutoRegister(player, autoRegister);
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }

    private static int getAutoRegister(CommandContext<CommandSourceStack> command) {
        if (command.getSource().getEntity() instanceof Player player) {
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);

            player.sendSystemMessage(Component.literal(String.valueOf(dataStore.usesAutoRegister(player))));
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }
}