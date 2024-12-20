package net.IAteMinecraft.shiphandler;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("shiphandler")
public class ShiphandlerMod
{
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Logger getLogger() {
        return LOGGER;
    }

    public static final String MOD_ID = "shiphandler";

    public ShiphandlerMod() { onCtor(); }

    public void onCtor() {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ShiphandlerConfig.GENERAL_SPEC, "shiphandler.toml");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ModEventListener {

        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event){
            ShiphandlerCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void registerOnLevelLoad(LevelEvent.Load event) {
            HandleShips.onLevelLoad(event);
        }

        @SubscribeEvent
        public static void registerOnServerTick(TickEvent.ServerTickEvent event) {
            HandleShips.onServerTick(event);
        }

        @SubscribeEvent
        public static void registerOnPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            HandleShips.onPlayerJoin(event);
        }
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(format, args);
    }

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
