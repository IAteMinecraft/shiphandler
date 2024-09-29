package net.IAteMinecraft.shiphandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.server.ServerLifecycleHooks;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.apigame.world.ShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HandleShips {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ZoneId zoneId;
    private final List<? extends Enum<DayOfWeek>> scheduledDays;
    private final LocalTime scheduledTime;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);  // Tracks if the scheduler is running

    // Constructor to initialize time settings (e.g., time zone, days, time)
    public HandleShips(String timeZone, List<? extends Enum<DayOfWeek>> days, LocalTime time) {
        this.zoneId = ZoneId.of(timeZone);
        this.scheduledDays = days;
        this.scheduledTime = time;
    }

    // Method to parse the time string into hours and minutes and return a LocalTime object
    public static LocalTime parseTime(String time) {
        String[] parts = time.split(":");  // Split the string at ":"
        int hour = Integer.parseInt(parts[0]);   // Parse the hour part
        int minute = Integer.parseInt(parts[1]); // Parse the minute part

        return LocalTime.of(hour, minute);       // Create and return a LocalTime object
    }

    // Method to start scheduling the handle method
    public void schedule() {
        if (isRunning.get()) {
            return; // Prevent starting multiple schedulers
        }

        long initialDelay = calculateInitialDelay();
        long period = TimeUnit.DAYS.toMillis(1); // Check daily

        scheduler.scheduleAtFixedRate(HandleShips::handle, initialDelay, period, TimeUnit.MILLISECONDS);
        isRunning.set(true);
    }

    // Method to stop the scheduler and shut down the thread pool
    public void stop() {
        if (!isRunning.get()) {
            return; // If it's not running, no need to stop
        }

        scheduler.shutdownNow();  // Stops all running tasks immediately
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();  // Forcefully terminate if not shut down within 5 seconds
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt(); // Re-interrupt the thread if it was interrupted
        }

        isRunning.set(false);  // Mark the scheduler as stopped
    }

    private long calculateInitialDelay() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime nextRun = getNextRunTime(now);

        return Duration.between(now, nextRun).toMillis();
    }

    private ZonedDateTime getNextRunTime(ZonedDateTime now) {
        ZonedDateTime nextRun = ZonedDateTime.of(LocalDate.now(), scheduledTime, zoneId);

        // If it's a scheduled day and time hasn't passed yet, run today
        if (scheduledDays.contains(now.getDayOfWeek()) && now.toLocalTime().isBefore(scheduledTime)) {
            return nextRun;
        }

        // Otherwise, find the next valid day in the future
        do {
            nextRun = nextRun.plusDays(1);
        } while (!scheduledDays.contains(nextRun.getDayOfWeek()));

        return nextRun;
    }

    // Static method to handle deleting ships
    public static void deleteShips(ShipWorldCore world, List<Ship> ships) {
        try {
            ServerShipWorldCore shipWorld = (ServerShipWorldCore) world;
            for (Ship ship : ships) {
                ServerShip deleteShip = (ServerShip) ship;
                shipWorld.deleteShip(deleteShip);
            }
        } catch (Exception e) {
            ShiphandlerMod.getLogger().error("", e.getCause());
        }
    }

    // Static method that performs ship handling
    public static void handle() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ShipDataStore shipDataStore = ShipDataStore.get(server.overworld());
        List<Long> storedShipIds = shipDataStore.getAllShipsId();

        for (ServerLevel level : server.getAllLevels()) {
            List<Ship> shipsToDelete = new ArrayList<>();
            for (Ship ship : VSGameUtilsKt.getAllShips(level)) {
                if (!storedShipIds.contains(ship.getId())) {
                    shipsToDelete.add(ship);
                }
            }
            // Delete all ships not in ShipDataStore
            deleteShips(VSGameUtilsKt.getShipWorldNullable(level), shipsToDelete);
        }
    }
}
