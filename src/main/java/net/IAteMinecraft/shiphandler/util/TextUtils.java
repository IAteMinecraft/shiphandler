package net.IAteMinecraft.shiphandler.util;

public class TextUtils {
    public static String formatDimensionId(String input) {
        // Remove the "ResourceKey[" prefix and "]" suffix
        String trimmedInput = input.replace("ResourceKey[", "").replace("]", "").trim();

        // Replace " / " with ":"
        return trimmedInput.replace(" / ", ":");
    }
}
