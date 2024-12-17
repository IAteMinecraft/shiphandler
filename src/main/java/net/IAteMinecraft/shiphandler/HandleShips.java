package net.IAteMinecraft.shiphandler;

import net.IAteMinecraft.shiphandler.util.EntityUtils;
import net.IAteMinecraft.shiphandler.util.MathUtils;

import net.IAteMinecraft.shiphandler.util.TextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.*;

public class HandleShips {
    private static Set<Ship> previousShips = new HashSet<>(); // Track ship IDs from previous tick

    // Static method to handle deleting ships
    public static void deleteShips(MinecraftServer server, List<Ship> ships) {
        try {
            for (Ship ship : ships) {
                ServerShipWorldCore shipWorld = VSGameUtilsKt.getVsPipeline(server).getShipWorld();
                ServerShip deleteShip = (ServerShip) ship;
                shipWorld.deleteShip(deleteShip);
            }
        } catch (Exception e) {
            ShiphandlerMod.getLogger().error("", e.getCause());
        }
    }

    // Static method that performs ship handling
    public static void handle() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ShipDataStore shipDataStore = ShipDataStore.get(server.overworld());
        List<Long> storedShipIds = shipDataStore.getAllRegisteredShipIDs();

        List<Ship> shipsToDelete = new ArrayList<>();
        for (ServerShip ship : VSGameUtilsKt.getVsPipeline(server).getShipWorld().getAllShips()) {
            if (!storedShipIds.contains(ship.getId()) || ship.getInertiaData().getMass() == 0.0) {
                shipsToDelete.add(ship);
            }
        }
        // Delete all ships not in ShipDataStore
        deleteShips(server, shipsToDelete);

    }

    // Method to get all current ships in the server
    public static Set<Ship> getCurrentShips() {
        Set<Ship> shipIds = new HashSet<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        for (ServerLevel level : server.getAllLevels()) {
            shipIds.addAll(VSGameUtilsKt.getAllShips(level));
        }
        return shipIds;
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        ShiphandlerMod.getLogger().debug("----------------------------------------------------------------");
        ShiphandlerMod.getLogger().debug("Server initialised");
        if (event.getLevel() instanceof ServerLevel level) {
            ShipDataStore shipDataStore = ShipDataStore.get(level.getServer().overworld());
            previousShips = getCurrentShips(); // Initialize ship IDs when level loads

            // Remove ships that weren't picked up when they were deleted
                HashMap<Long, ShipDataStore.ShipData> copiedMap = new HashMap<>(shipDataStore.getShipData());
                ShiphandlerMod.getLogger().debug(String.valueOf(copiedMap));
                for (Ship ship : previousShips) {
                    copiedMap.remove(ship.getId());
                    ShiphandlerMod.getLogger().debug(String.valueOf(ship.getId()));
                }
                ShiphandlerMod.getLogger().debug(String.valueOf(copiedMap));
                for (Long remainingId : copiedMap.keySet()) {
                    shipDataStore.removeShip(remainingId);
                    ShiphandlerMod.getLogger().debug(String.valueOf(remainingId));
                }
                ShiphandlerMod.getLogger().debug(String.valueOf(copiedMap));

            ShiphandlerMod.getLogger().debug("Level loaded: " + event.getLevel().toString());
        }
        ShiphandlerMod.getLogger().debug("----------------------------------------------------------------");
    }
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        ShipDataStore shipDataStore = ShipDataStore.get(event.getEntity().getServer().overworld());

        if (!shipDataStore.hasPlayer(player)) {
            shipDataStore.addPlayer(player, ShiphandlerConfig.maxShips.get(), ShiphandlerConfig.autoRegister.get());
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Set<Ship> currentShips = getCurrentShips();

            if (currentShips != previousShips) {
                // Ships added
                Set<Ship> newShips = new HashSet<>(currentShips);
                newShips.removeAll(previousShips);

                // Ships removed
                Set<Ship> removedShips = new HashSet<>(previousShips);
                removedShips.removeAll(currentShips);

                // Handle new ships
                for (Ship ship : newShips) {
                    ShiphandlerMod.getLogger().debug("New ship detected with Slug: {}", ship.getSlug());
                    //EntityUtils.sendChatMessage(event.getServer(), Component.literal("New ship detected with Slug: " + ship.getSlug()));

                    Level level = event.getServer().overworld();
                    //EntityUtils.sendChatMessage(event.getServer(), Component.literal("Ship Dimension: " + ship.getChunkClaimDimension()));
                    //EntityUtils.sendChatMessage(event.getServer(), Component.literal("Dim id function: " + TextUtils.formatDimensionId(level.dimension().toString())));
                    for (Level level_ : event.getServer().getAllLevels()) {
                        if (TextUtils.formatDimensionId(level_.dimension().toString()).equals(ship.getChunkClaimDimension())) {
                            level = event.getServer().getLevel(level_.dimension());
                            break;
                        }
                        //EntityUtils.sendChatMessage(event.getServer(), Component.literal("Current Dimension iteration (Dimension): " + level_.dimension().toString()));
                        //EntityUtils.sendChatMessage(event.getServer(), Component.literal("Current Dimension iteration (DimensionType): " + level_.dimensionType().toString()));
                        //EntityUtils.sendChatMessage(event.getServer(), Component.literal("Current Dimension iteration (DimensionTypeId): " + level_.dimensionTypeId().toString()));
                        //EntityUtils.sendChatMessage(event.getServer(), Component.literal("Current Dimension iteration (DimensionTypeRegistration): " + level_.dimensionTypeRegistration().toString()));
                    }
                    //EntityUtils.sendChatMessage(event.getServer(), Component.literal("1-1: " + ship.getChunkClaimDimension()));
                    Player player = null;

                    ShipDataStore dataStore = ShipDataStore.get(event.getServer().overworld());
                    int inflateSize = 0;
                    while (player == null) {
                        if (inflateSize > ShiphandlerConfig.maxShipFindDistance.get()) {
                            break;
                        }
                        player = EntityUtils.getNearestPlayerToBlock(level,
                            MathUtils.getCenterPosition(
                                new BlockPos((int) ship.getWorldAABB().minX(),
                                    (int) ship.getWorldAABB().minY(),
                                    (int) ship.getWorldAABB().minZ()
                                ),
                                new BlockPos((int) ship.getWorldAABB().maxX(),
                                    (int) ship.getWorldAABB().maxY(),
                                    (int) ship.getWorldAABB().maxZ()
                                )
                            ),
                            MathUtils.AABBdc2AABB(ship.getWorldAABB()).inflate(inflateSize, inflateSize, inflateSize)
                        );

                        /*EntityUtils.sendChatMessage(event.getServer(),
                            Component.literal("Centered Position: " +
                                MathUtils.getCenterPosition(
                                    new BlockPos((int) ship.getWorldAABB().minX(),
                                        (int) ship.getWorldAABB().minY(),
                                        (int) ship.getWorldAABB().minZ()
                                    ),
                                    new BlockPos((int) ship.getWorldAABB().maxX(),
                                        (int) ship.getWorldAABB().maxY(),
                                        (int) ship.getWorldAABB().maxZ()
                                    )
                                )
                                + "; Start Pos: " +
                                new BlockPos((int) ship.getWorldAABB().minX(),
                                    (int) ship.getWorldAABB().minY(),
                                    (int) ship.getWorldAABB().minZ()
                                )
                                + "; End Pos: " +
                                new BlockPos((int) ship.getWorldAABB().maxX(),
                                    (int) ship.getWorldAABB().maxY(),
                                    (int) ship.getWorldAABB().maxZ()
                                )
                                + "; Ship AABB: " +
                                MathUtils.AABBdc2AABB(ship.getWorldAABB()).inflate(inflateSize, inflateSize, inflateSize).toString()
                            )
                        );*/

                        inflateSize++;
                    }
                    if (player != null) {
                        //EntityUtils.sendChatMessage(event.getServer(), Component.literal("1-3: ").append(player.getDisplayName()));
                        ShiphandlerMod.getLogger().info("Added newly-created ship: " + ship.getSlug() + ", made by player: " + player.getDisplayName().getString());

                        dataStore.addShip(player, ship);

                        //EntityUtils.sendChatMessage(event.getServer(), Component.literal("1-3-1: " + dataStore.usesAutoRegister(player) + ", ").append(player.getDisplayName()));
                        if (dataStore.usesAutoRegister(player)) {
                            //EntityUtils.sendChatMessage(event.getServer(), Component.literal("1-3-2: Attempted autoRegister"));
                            dataStore.registerShip(player, ship);
                        }
                    } else {
                        EntityUtils.sendChatMessage(event.getServer(), Component.literal("§4Unable to find player"));

                        dataStore.addShip(null, ship);
                    }
                }

                // Handle removed ships
                for (Ship ship : removedShips) {
                    ShiphandlerMod.getLogger().debug("Ship removed with Slug: {}", ship.getSlug());
                    //EntityUtils.sendChatMessage(event.getServer(), Component.literal("Ship removed with Slug: " + ship.getSlug()));

                    ShipDataStore dataStore = ShipDataStore.get(event.getServer().overworld());
                    dataStore.removeShip(ship.getId());
                }
            }

            // Update the previousShipIds for the next tick
            previousShips = currentShips;
        }
    }
}
