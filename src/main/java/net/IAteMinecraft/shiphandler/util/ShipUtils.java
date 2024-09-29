package net.IAteMinecraft.shiphandler.util;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Objects;

public class ShipUtils {
    public static String getShip(Player player, long shipId) {
        for (ServerLevel level : player.getServer().getAllLevels()) {
            for (Ship ship : VSGameUtilsKt.getAllShips(level)) {
                if (ship.getId() == shipId) {
                    return ship.getSlug();
                }
            }
        }
        return null;
    }

    public static String getShip(Player player, String shipSlug) {
        for (ServerLevel level : player.getServer().getAllLevels()) {
            for (Ship ship : VSGameUtilsKt.getAllShips(level)) {
                if (Objects.equals(ship.getSlug(), shipSlug)) {
                    return ship.getSlug();
                }
            }
        }
        return null;
    }
}