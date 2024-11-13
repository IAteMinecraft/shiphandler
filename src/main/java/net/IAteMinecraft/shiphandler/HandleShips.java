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
import org.valkyrienskies.core.apigame.world.ShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HandleShips {
    private static Set<Ship> previousShips = new HashSet<>(); // Track ship IDs from previous tick

    // Static method to handle deleting ships
    public static void deleteShips(ShipWorldCore world, List<Ship> ships) {
        try {
            ServerShipWorldCore shipWorld = (ServerShipWorldCore) world;
            for (Ship ship : ships) {
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

        for (ServerLevel level : server.getAllLevels()) {
            List<Ship> shipsToDelete = new ArrayList<>();
            for (Ship ship : VSGameUtilsKt.getAllShips(level)) {
                if (!storedShipIds.contains(ship.getId())) {
                    shipsToDelete.add(ship);
                }
            }
            // Delete all ships not in ShipDataStore
            deleteShips(VSGameUtilsKt.getShipWorldNullable(level), shipsToDelete);
        }
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
        if (event.getLevel() instanceof ServerLevel) {
            previousShips = getCurrentShips(); // Initialize ship IDs when level loads
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
        //event.getServer().sendSystemMessage(Component.literal("Ticked"));
        if (event.phase == TickEvent.Phase.END) {
            Set<Ship> currentShips = getCurrentShips();

            if (currentShips != previousShips) {
                //EntityUtils.sendChatMessage(event.getServer(), Component.literal("Ticked"));
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
                    //EntityUtils.sendChatMessage(event.getServer(), Component.literal("Dim id function: " + TextUtils.formatDimensoinId(level.dimension().toString())));
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

                    //TODO: Fix this
                    /*// Iterate through each coordinate within the bounds
                    if (ModList.get().isLoaded("vs_eureka")) {
                        EntityUtils.sendChatMessage(event.getServer(), Component.literal("1-2-1: Eureka attempted"));
                        for (int x = (int) ship.getWorldAABB().minX(); x <= (int) ship.getWorldAABB().maxX(); x++) {
                            for (int y = (int) ship.getWorldAABB().minY(); y <= (int) ship.getWorldAABB().maxY(); y++) {
                                for (int z = (int) ship.getWorldAABB().minZ(); z <= (int) ship.getWorldAABB().maxZ(); z++) {
                                    BlockPos pos = new BlockPos(x, y, z);
                                    BlockState blockstate = level.getBlockState(pos);
                                    Block block = blockstate.getBlock();
                                    BlockEntity blockEntity = level.getBlockEntity(pos);

                                    if (ModList.get().isLoaded("vs_eureka") &&
                                            blockEntity instanceof ShipHelmBlockEntityIntergrationUtils.isInstanceOfModBlock(block, "org.valkyrienskies.eureka.block.ShipHelmBlock")) {
                                        player = EntityUtils.getNearestPlayerToBlock(level, pos, MathUtils.AABBdc2AABB(ship.getWorldAABB()).inflate(10));
                                        EntityUtils.sendChatMessage(event.getServer(), Component.literal("1-2-2: Eureka succeeded"));
                                    }
                                }
                            }
                        }
                    }*/
                    int inflateSize = 0;
                    while (player == null) {
                        if (inflateSize > 150) {
                            break;
                        }
                        player = EntityUtils.getNearestPlayerToBlock(level,
                                MathUtils.getCenterPosition(
                                        new BlockPos((int) ship.getWorldAABB().minX(),
                                                (int) ship.getWorldAABB().minY(),
                                                (int) ship.getWorldAABB().minZ()),
                                        new BlockPos((int) ship.getWorldAABB().maxX(),
                                                (int) ship.getWorldAABB().maxY(),
                                                (int) ship.getWorldAABB().maxZ())),
                                MathUtils.AABBdc2AABB(ship.getWorldAABB()).inflate(
                                        MathUtils.mid((int) ship.getWorldAABB().maxX(), (int) ship.getWorldAABB().minX()) + inflateSize,
                                        MathUtils.mid((int) ship.getWorldAABB().maxY(), (int) ship.getWorldAABB().minY()) + inflateSize,
                                        MathUtils.mid((int) ship.getWorldAABB().maxZ(), (int) ship.getWorldAABB().minZ()) + inflateSize));

                        inflateSize++;
                    }
                    if (player != null) {
                        //EntityUtils.sendChatMessage(event.getServer(), Component.literal("1-3: ").append(player.getDisplayName()));
                        ShiphandlerMod.getLogger().info("Added newly-created ship: " + ship.getSlug() + ", made by player: " + player.getDisplayName().getString());

                        dataStore.addShip(player, ship);

                        //EntityUtils.sendChatMessage(event.getServer(), Component.literal("1-1: " + dataStore.usesAutoRegister(player) + ", ").append(player.getDisplayName()));
                        if (dataStore.usesAutoRegister(player)) {
                            //EntityUtils.sendChatMessage(event.getServer(), Component.literal("1-3: Attempted autoRegister"));
                            dataStore.registerShip(player, ship);
                        }
                    } else {
                        EntityUtils.sendChatMessage(event.getServer(), Component.literal("§4Unable to find player"));

                        //dataStore.addShip(null, ship);
                    }
                }

                // Handle removed ships
                for (Ship ship : removedShips) {
                    ShiphandlerMod.getLogger().debug("Ship removed with Slug: {}", ship.getSlug());
                    EntityUtils.sendChatMessage(event.getServer(), Component.literal("Ship removed with Slug: " + ship.getSlug()));

                    ShipDataStore dataStore = ShipDataStore.get(event.getServer().overworld());
                    dataStore.removeShip(ship.getId());
                }
            }

            // Update the previousShipIds for the next tick
            previousShips = currentShips;
        }
    }
}
