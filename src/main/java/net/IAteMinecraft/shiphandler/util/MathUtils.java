package net.IAteMinecraft.shiphandler.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;

public class MathUtils {
    /**
     * @param key   First value
     * @param value Second value
     */
    public record Pair<K, V> (K key, V value) {

        @Override
        public String toString() {
            return "{" + key + ", " + value + "}\n";
        }
    }

    public static BlockPos getCenterPosition(BlockPos minPos, BlockPos maxPos) {
        int centerX = (minPos.getX() + maxPos.getX()) / 2;
        int centerY = (minPos.getY() + maxPos.getY()) / 2;
        int centerZ = (minPos.getZ() + maxPos.getZ()) / 2;
        return new BlockPos(centerX, centerY, centerZ);
    }

    public static AABB AABBi2AABB(AABBi AABBi_) {
        return new AABB(AABBi_.minX(), AABBi_.minY(), AABBi_.minZ(), AABBi_.maxX(), AABBi_.maxY(), AABBi_.maxZ());
    }

    public static AABB AABBic2AABB(AABBic AABBic_) {
        return new AABB(AABBic_.minX(), AABBic_.minY(), AABBic_.minZ(), AABBic_.maxX(), AABBic_.maxY(), AABBic_.maxZ());
    }

    public static AABB AABBdc2AABB(AABBdc AABBdc_) {
        return new AABB(AABBdc_.minX(), AABBdc_.minY(), AABBdc_.minZ(), AABBdc_.maxX(), AABBdc_.maxY(), AABBdc_.maxZ());
    }

    public static int mid(int x, int y){
        return (x+y)/2;
    }

}
