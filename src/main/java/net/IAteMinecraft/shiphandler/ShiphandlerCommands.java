package net.IAteMinecraft.shiphandler;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.ShipArgument;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class ShiphandlerCommands {

    // Static reference to hold the single instance of HandleShips
    private static HandleShips currentHandleShipsInstance = null;
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("ship-handler")
            .then(Commands.literal("ship")
                .then(Commands.literal("add")
                .requires((player) -> player.hasPermission(2))
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> {
                            return registerShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), true);
                        })
                    )
                )
                .then(Commands.literal("remove")
                .requires((player) -> player.hasPermission(2))
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> {
                            return unregisterShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), true);
                        })
                    )
                )
                .then(Commands.literal("listAllShips")
                .requires((player) -> player.hasPermission(2))
                    .executes(ShiphandlerCommands::listShips)
                )
                .then(Commands.literal("register")
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> {
                            return registerShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), false);
                        })
                    )
                )
                .then(Commands.literal("unregister")
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> {
                            return unregisterShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), false);
                        })
                    )
                )
                .then(Commands.literal("list")
                    .executes(ShiphandlerCommands::list)
                )
            )
            .then(Commands.literal("scheduler")
            .requires((player) -> player.hasPermission(2))
                .then(Commands.literal("run")
                    .executes(ShiphandlerCommands::schedulerRun)
                )
                .then(Commands.literal("start")
                    .executes(ShiphandlerCommands::schedulerStart)
                )
                .then(Commands.literal("stop")
                    .executes(ShiphandlerCommands::schedulerStop)
                )
            )
            .then(Commands.literal("get-id")
                .then(Commands.argument("ship", ShipArgument.Companion.ships())
                    .executes(command -> {
                        return getId(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"));
                    })
                )
            )
        );
    }

    private static int schedulerRun(CommandContext<CommandSourceStack> command) {
        HandleShips.handle();
        command.getSource().sendSystemMessage(Lang.text("Successfully ran the scheduler!").component());
        return Command.SINGLE_SUCCESS;
    }

    // Start the scheduler, but ensure only one instance is running
    private static int schedulerStart(CommandContext<CommandSourceStack> command) {
        if (currentHandleShipsInstance != null) {
            command.getSource().sendFailure(Lang.text("Scheduler is already running!").component());
            return Command.SINGLE_SUCCESS;
        }

        /*currentHandleShipsInstance = new HandleShips(
                ShiphandlerConfig.server().timeZone.get(),
                ShiphandlerConfig.server().DaysToRun.get(),
                HandleShips.parseTime(ShiphandlerConfig.server().time.get())
        );
        executor.submit(currentHandleShipsInstance::schedule);*/
        //command.getSource().sendSystemMessage(Lang.text("Successfully started the scheduler!").component());
        command.getSource().sendSystemMessage(Lang.text("Not implemented").component());
        return Command.SINGLE_SUCCESS;
    }

    // Stop the scheduler and reset the instance
    private static int schedulerStop(CommandContext<CommandSourceStack> command) {
        if (currentHandleShipsInstance != null) {
            currentHandleShipsInstance.stop();
            currentHandleShipsInstance = null;
            command.getSource().sendSystemMessage(Lang.text("Successfully stopped the scheduler!").component());
        } else {
            command.getSource().sendFailure(Lang.text("No scheduler is currently running.").component());
        }
        return Command.SINGLE_SUCCESS;
    }

    // Clean up HandleShips when the world is unloaded
    public static void onWorldUnload() {
        if (currentHandleShipsInstance != null) {
            currentHandleShipsInstance.stop();
            currentHandleShipsInstance = null;
        }
    }

    private static int listShips(CommandContext<CommandSourceStack> command) {
        if(command.getSource().getEntity() instanceof Player player){
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            player.sendSystemMessage(Lang.text("Registered Ships: \n" + dataStore.getAllShipsWithPlayerName()).component());
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }

    private static int list(CommandContext<CommandSourceStack> command) {
        if(command.getSource().getEntity() instanceof Player player){
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            player.sendSystemMessage(Lang.text("Registered Ships: " + dataStore.getShips(player)).component());
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }

    private static int execute(CommandContext<CommandSourceStack> command, Ship ship){
        if (command.getSource().getEntity() instanceof Player player) {
            player.sendSystemMessage(Lang.text(ship.getSlug()).component());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int registerShip(CommandContext<CommandSourceStack> command, Ship ship, boolean op){
        if (command.getSource().getEntity() instanceof Player player) {
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            if (dataStore.getShipsId(player).contains(ship.getId()) && !op) {
                player.sendSystemMessage(Lang.text("§4You already own that ship").component());
                return Command.SINGLE_SUCCESS;
            } else if (dataStore.getAllShipsId().contains(ship.getId()) && !op) {
                player.sendSystemMessage(Lang.text("§4That Ship is already owned by another player").component());
                return Command.SINGLE_SUCCESS;
            } else {
                player.sendSystemMessage(Lang.text("Registered Ship: " + ship.getSlug() + " for player: ").component().append(player.getDisplayName()));
                if (dataStore.add(player, ship, ShiphandlerConfig.server().infOpShips.get() ? -1 : ShiphandlerConfig.server().maxShips.get()))
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
            if (dataStore.getShipsId(player).contains(ship.getId()) || op) {
                player.sendSystemMessage(Lang.text("Unregistered Ship: " + ship.getSlug() + " for player: " + dataStore.getPlayerNameFromShip(ship.getId())).component());
                if (dataStore.removeShipById(ship.getId()))
                    return Command.SINGLE_SUCCESS;
                else
                    return 0;
            } else {
                player.sendSystemMessage(Lang.text("§4You don't own that ship").component());
                return Command.SINGLE_SUCCESS;
            }
        } else
            return 0;
    }

    private static int getId(CommandContext<CommandSourceStack> command, Ship ship){
        if (command.getSource().getEntity() instanceof Player player) {
            player.sendSystemMessage(Lang.text(String.valueOf(ship.getId())).component());
            return Command.SINGLE_SUCCESS;
        } else
            return 0;
    }
}