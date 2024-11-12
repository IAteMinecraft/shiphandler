package net.IAteMinecraft.shiphandler.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class IntergrationUtils {
    public static boolean isInstanceOfModBlock(Block block, String modClassName) {
        try {
            // Load the class from the other mod by its fully qualified name
            Class<?> modBlockClass = Class.forName(modClassName);

            // Check if the block's class is assignable from the mod's block class
            return modBlockClass.isAssignableFrom(block.getClass());

        } catch (ClassNotFoundException e) {
            // The class doesn't exist, so the mod is not loaded, or the class is invalid
            return false;
        }
    }

    // Method to check if a BlockEntity is an instance of a class from another mod
    public static boolean isInstanceOfBlockEntity(BlockEntity blockEntity, String modBlockEntityClassName) {
        if (blockEntity == null) return false;

        try {
            // Load the class by name
            Class<?> modBlockEntityClass = Class.forName(modBlockEntityClassName);

            // Check if the BlockEntity is an instance of the mod's class
            return modBlockEntityClass.isAssignableFrom(blockEntity.getClass());

        } catch (ClassNotFoundException e) {
            // The class doesn't exist, meaning the mod isn't loaded or the name is incorrect
            return false;
        }
    }
}
