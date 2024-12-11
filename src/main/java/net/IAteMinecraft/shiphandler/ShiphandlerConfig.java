package net.IAteMinecraft.shiphandler;

import net.minecraftforge.common.ForgeConfigSpec;

public class ShiphandlerConfig {
    public static final ForgeConfigSpec GENERAL_SPEC;

    public static ForgeConfigSpec.BooleanValue infOpShips;
    public static ForgeConfigSpec.BooleanValue autoRegister;
    public static ForgeConfigSpec.IntValue maxShips;
    public static ForgeConfigSpec.IntValue maxShipFindDistance;


    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        setupConfig(configBuilder);
        GENERAL_SPEC = configBuilder.build();
    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        builder.push("General");

        infOpShips = builder
                .comment("Allows Operators to register infinite ships.")
                .define("Infinite OP Ships", true);

        autoRegister = builder
                .comment("Make newly created ships automatically register to their owner.")
                .define("Auto Register Ships", true);

        maxShips = builder
                .comment("The Maximum number of ships a player can register. -1 for no limit.")
                .defineInRange("Max ships", -1, -1, Integer.MAX_VALUE);

        maxShipFindDistance = builder
                .comment("The Maximum distance that will be used when looking for a player when a ship is created.\nNOTE: Large values may cause large amounts of lag!")
                .defineInRange("Max ship Find Distance", 150, 1, Integer.MAX_VALUE);

        builder.pop();
    }
}
