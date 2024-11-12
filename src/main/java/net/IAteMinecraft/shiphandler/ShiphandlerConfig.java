package net.IAteMinecraft.shiphandler;

import com.simibubi.create.foundation.config.ConfigBase;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import org.apache.commons.lang3.tuple.Pair;

//import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Supplier;

public class ShiphandlerConfig {
    private static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);
    private static TServer server;
    //private static TClient client;

    public static class TServer extends ConfigBase {

        public final ConfigBase.ConfigBool infOpShips = this.b(true, "Infinite OP Ships", "Allows Operators to register infinite ships.");
        public final ConfigBase.ConfigBool autoRegister = this.b(true, "Auto Register Ships", "Make newly created ships automatically register to their owner.");
        public final ConfigBase.ConfigInt maxShips = this.i(-1, -1, "Max ships", "The Maximum number of ships a player can register. -1 for no limit.");

        @Override
        public String getName() {
            return "server";
        }
    }

    /*public static class TClient extends ConfigBase {
        //public final ConfigBase.ConfigGroup e = this.group(1, "e");

        @Override
        public String getName() {
            return "client";
        }
    }*/

    //public static TClient client() { return ShiphandlerConfig.client; }
    public static TServer server() { return ShiphandlerConfig.server; }

    private static <T extends ConfigBase> T register(Supplier<T> factory, ModConfig.Type side) {
        Pair<T, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    public static void register(ModLoadingContext context) {
        server = register(TServer::new, ModConfig.Type.SERVER);
        //client = register(TClient::new, ModConfig.Type.CLIENT);

        for (Map.Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet())
            context.registerConfig(pair.getKey(), pair.getValue().specification);
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onLoad();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onReload();
    }
}
