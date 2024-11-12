package net.IAteMinecraft.shiphandler.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class EntityUtils {

    public static Player getNearestPlayerToBlock(Level level, BlockPos blockPos, double radius) {
        // Create a bounding box centered at the block position
        AABB boundingBox = new AABB(blockPos).inflate(radius);

        // Get all entities in the bounding box
        Player nearestPlayer = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : level.getEntities(null, boundingBox)) {
            if (entity instanceof Player player) {  // Check if the entity is a player
                double distance = player.distanceToSqr(blockPos.getX(), blockPos.getY(), blockPos.getZ());

                // Update the closest player if this one is closer
                if (distance < closestDistance) {
                    closestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }

        return nearestPlayer;
    }

    public static Player getNearestPlayerToBlock(Level level, BlockPos blockPos, AABB boundingBox_) {
        AABB boundingBox = boundingBox_/*.inflate(5)*/;
        // Get all entities in the bounding box
        Player nearestPlayer = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : level.getEntities(null, boundingBox)) {
            if (entity instanceof Player player) {  // Check if the entity is a player
                double distance = player.distanceToSqr(blockPos.getX(), blockPos.getY(), blockPos.getZ());

                // Update the closest player if this one is closer
                if (distance < closestDistance) {
                    closestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }

        return nearestPlayer;
    }

    public static void sendChatMessage(MinecraftServer server, Component component) {
        for (Player player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }
    }
}