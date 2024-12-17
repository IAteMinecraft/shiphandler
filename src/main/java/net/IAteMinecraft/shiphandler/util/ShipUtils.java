package net.IAteMinecraft.shiphandler.util;

import net.minecraft.server.MinecraftServer;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Objects;

public class ShipUtils {
    public static ServerShip getShip(MinecraftServer server, long shipId) {
        for (ServerShip ship : VSGameUtilsKt.getVsPipeline(server).getShipWorld().getAllShips()) {
            if (ship.getId() == shipId) {
                return ship;
            }
        }
        return null;
    }

    public static ServerShip getShip(MinecraftServer server, String shipSlug) {
        for (ServerShip ship : VSGameUtilsKt.getVsPipeline(server).getShipWorld().getAllShips()) {
            if (Objects.equals(ship.getSlug(), shipSlug)) {
                return ship;
            }
        }
        return null;
    }
}