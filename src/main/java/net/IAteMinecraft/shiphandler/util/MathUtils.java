package net.IAteMinecraft.shiphandler.util;

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

}
