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
        /*protected <T extends Enum<T>> ConfigListEnum<T> le(List<? extends Enum<T>> current, String name, String... comment) {
            return new ConfigListEnum<>(name, (List<Enum<T>>) current, comment);
        }

        protected ConfigString s(String current, String name, String... comment) {
            return new ConfigString(name, current, comment);
        }

        public class ConfigListEnum<T extends Enum<T>> extends CValue<List<? extends Enum<T>>, ForgeConfigSpec.ConfigValue<List<? extends Enum<T>>>> {
            public ConfigListEnum(String name, List<Enum<T>> current, String... comment) {
                super(name, (builder) -> {
                    return builder.defineList(name, current, entry -> true);
                }, comment);
            }
        }

        public class ConfigString extends CValue<String, ForgeConfigSpec.ConfigValue<String>> {
            public ConfigString(String name, String current, String... comment) {
                super(name, (builder) -> {
                    return builder.define(name, current);
                }, comment);
            }
        }*/

        public final ConfigBase.ConfigBool infOpShips = this.b(true, "Infinite OP Ships", "Allows Operators to register infinite ships.");
        public final ConfigBase.ConfigInt maxShips = this.i(-1, -1, "Max ships", "The Maximum number of ships a player can register. -1 for no limit.");

        /*public final ConfigBase.ConfigGroup scheduler = this.group(1, "Scheduler", "NOTE: Changing any of these values REQUIRES you to RESTART the server\nThis is currently broken!");
        public final ConfigBase.ConfigBool useScheduler = this.b(true, "Use Scheduler", "Whether or not to use the scheduler");
        public final ConfigListEnum<DayOfWeek> DaysToRun = this.le(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), "Days To Run", "The days of the week to run the scheduler, \n Use 'Insert Day here, in CAPS'");
        public final ConfigString timeZone = this.s("GMT", "Time Zone", "The Time Zone to use when scheduling times");
        public final ConfigString time = this.s("00:00", "Time", "The time that the scheduler runs on each day");*/

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
