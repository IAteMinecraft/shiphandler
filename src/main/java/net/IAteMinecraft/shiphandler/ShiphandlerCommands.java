package net.IAteMinecraft.shiphandler;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.IAteMinecraft.shiphandler.util.ShipUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.command.ShipArgument;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ShiphandlerCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("ship-handler")
            .then(Commands.literal("ship")
                .then(Commands.literal("add")
                .requires((player) -> player.hasPermission(4))
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> registerShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), true, null))
                    )
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(command -> registerShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), true, EntityArgument.getPlayer(command, "player")))
                        )
                    )
                )
                .then(Commands.literal("remove")
                .requires((player) -> player.hasPermission(4))
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> unregisterShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), true))
                    )
                )
                .then(Commands.literal("register")
                    .then(Commands.argument("ship", ShipArgument.Companion.ships())
                        .executes(command -> registerShip(command, ShipArgument.Companion.getShip(((CommandContext) command), "ship"), false, null))
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
            .requires((player) -> player.hasPermission(4))
                .then(Commands.literal("run")
                    .executes(ShiphandlerCommands::handlerRun)
                )
                .then(Commands.literal("deleteShip")
                    .then(Commands.argument("ShipID", LongArgumentType.longArg(0))
                        .executes(command -> deleteShip(command, LongArgumentType.getLong(command, "ShipID")))
                    )
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
                .then(Commands.literal("debugListAllShips")
                    .executes(ShiphandlerCommands::debugListShips)
                )
            )
        );
    }

    private static int handlerRun(CommandContext<CommandSourceStack> command) {
        HandleShips.handle();
        command.getSource().sendSystemMessage(Component.literal("Successfully ran the scheduler!"));
        return Command.SINGLE_SUCCESS;
    }

    private static int deleteShip(CommandContext<CommandSourceStack> command, long shipId) {
        ServerShipWorldCore shipWorld = VSGameUtilsKt.getVsPipeline(command.getSource().getServer()).getShipWorld();
        ServerShip ship = ShipUtils.getShip(command.getSource().getServer(), shipId);
        shipWorld.deleteShip(ship);
        command.getSource().sendSystemMessage(Component.literal("Successfully deleted ship: "+ ship.getSlug()));
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

    private static int debugListShips(CommandContext<CommandSourceStack> command) {
        if(command.getSource().getEntity() instanceof Player player){
            MinecraftServer server = player.getServer();
            player.sendSystemMessage(Component.literal("Created Ships: "));
            for (ServerShip ship : VSGameUtilsKt.getVsPipeline(server).getShipWorld().getAllShips()) {
                player.sendSystemMessage(Component.literal(ship.getSlug()));
                player.sendSystemMessage(Component.literal(ship.getChunkClaimDimension()));
                player.sendSystemMessage(Component.literal(VSGameUtilsKt.getLevelFromDimensionId(server, ship.getChunkClaimDimension()).dimension().toString()));
            }
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

    private static int registerShip(CommandContext<CommandSourceStack> command, Ship ship, boolean op, @Nullable Player player){
        if (player != null) {
            ServerLevel level = player.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            if (dataStore.getRegisteredShipsId(player).contains(ship.getId()) && !op) {
                player.sendSystemMessage(Component.literal("§4You already own that ship"));
                return Command.SINGLE_SUCCESS;
            } else if (dataStore.getAllRegisteredShipIDs().contains(ship.getId()) && !op) {
                player.sendSystemMessage(Component.literal("§4That Ship is already owned by another player"));
                return Command.SINGLE_SUCCESS;
            } else if (((dataStore.getMaxShips(player) == -1) && dataStore.getCurrentRegisteredShipCount(player)+1 > dataStore.getMaxShips(player)) && !(op ? ShiphandlerConfig.infOpShips.get() : false)) {
                player.sendSystemMessage(Component.literal(
                        dataStore.getMaxShips(player) + "; " + (dataStore.getCurrentRegisteredShipCount(player)+1) + "; " + dataStore.getMaxShips(player) + "\n" +
                        (dataStore.getMaxShips(player) == -1) + (dataStore.getCurrentRegisteredShipCount(player)+1 > dataStore.getMaxShips(player)) + "\n" +
                        op + ShiphandlerConfig.infOpShips.get() + "\n" +
                        !(op ? ShiphandlerConfig.infOpShips.get() : false) + "\n" +
                        (((dataStore.getMaxShips(player) == -1) && dataStore.getCurrentRegisteredShipCount(player)+1 > dataStore.getMaxShips(player)) && !(op ? ShiphandlerConfig.infOpShips.get() : false))
                ));
                player.sendSystemMessage(Component.literal("§4You have reached your maximum amount of ships!\nConsider removing some ships, or turn off autoRegister"));
                return Command.SINGLE_SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("Registered Ship: " + ship.getSlug() + " for player: ").append(player.getDisplayName()));
                if (dataStore.registerShip(player, ship))
                    return Command.SINGLE_SUCCESS;
                else
                    return 0;
            }
        } else if (command.getSource().getEntity() instanceof Player player_) {
            ServerLevel level = player_.getServer().overworld();
            ShipDataStore dataStore = ShipDataStore.get(level);
            if (dataStore.getRegisteredShipsId(player_).contains(ship.getId()) && !op) {
                player_.sendSystemMessage(Component.literal("§4You already own that ship"));
                return Command.SINGLE_SUCCESS;
            } else if (dataStore.getAllRegisteredShipIDs().contains(ship.getId()) && !op) {
                player_.sendSystemMessage(Component.literal("§4That Ship is already owned by another player"));
                return Command.SINGLE_SUCCESS;
            } else if ((!(dataStore.getMaxShips(player_) == -1) && dataStore.getCurrentRegisteredShipCount(player_)+1 > dataStore.getMaxShips(player_)) && !(op ? ShiphandlerConfig.infOpShips.get() : false)) {
                player_.sendSystemMessage(Component.literal(
                        dataStore.getMaxShips(player_) + "; " + (dataStore.getCurrentRegisteredShipCount(player_)+1) + "; " + dataStore.getMaxShips(player_) + "\n" +
                        (dataStore.getMaxShips(player_) == -1) + (dataStore.getCurrentRegisteredShipCount(player_)+1 > dataStore.getMaxShips(player_)) + "\n" +
                        op + ShiphandlerConfig.infOpShips.get() + "\n" +
                        !(op ? ShiphandlerConfig.infOpShips.get() : false) + "\n" +
                        ((!(dataStore.getMaxShips(player_) == -1) && dataStore.getCurrentRegisteredShipCount(player_)+1 > dataStore.getMaxShips(player_)) && !(op ? ShiphandlerConfig.infOpShips.get() : false))
                ));
                player_.sendSystemMessage(Component.literal("§4You have reached your maximum amount of ships!\nConsider removing some ships, or turn off autoRegister"));
                return Command.SINGLE_SUCCESS;
            } else {
                player_.sendSystemMessage(Component.literal("Registered Ship: " + ship.getSlug() + " for player: ").append(player_.getDisplayName()));
                if (dataStore.registerShip(player_, ship))
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
            player.sendSystemMessage(Component.literal("Successfully set autoRegister to: " + autoRegister));
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