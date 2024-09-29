package net.IAteMinecraft.shiphandler;

import net.IAteMinecraft.shiphandler.util.MathUtils.Pair;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import org.valkyrienskies.core.api.ships.Ship;


import java.util.*;

public class ShipDataStore extends SavedData {
    private static final String DATA_NAME = "ship_data";
    private final HashMap<UUID, PlayerData> playerDataMap;

    public ShipDataStore() {
        this.playerDataMap = new HashMap<>();
    }

    // Inner class to store ship data (ID and slug)
    private static class ShipData {
        long shipId;
        String shipSlug;

        ShipData(long shipId, String shipSlug) {
            this.shipId = shipId;
            this.shipSlug = shipSlug;
        }

        // Convert ShipData to NBT
        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("shipId", shipId);
            tag.putString("shipSlug", shipSlug);
            return tag;
        }

        // Read ShipData from NBT
        public static ShipData fromNbt(CompoundTag tag) {
            long shipId = tag.getLong("shipId");
            String shipSlug = tag.getString("shipSlug");
            return new ShipData(shipId, shipSlug);
        }
    }

    // Inner class to store player data
    private static class PlayerData {
        int maxShips;
        String playerName;
        ArrayList<ShipData> ships;

        PlayerData(String playerName, int maxShips) {
            this.maxShips = maxShips;
            this.playerName = playerName;
            this.ships = new ArrayList<>();
        }

        // Convert PlayerData to NBT
        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("maxShips", maxShips);
            tag.putString("playerName", playerName);

            ListTag shipsList = new ListTag();
            for (ShipData ship : ships) {
                shipsList.add(ship.toNbt());
            }
            tag.put("ships", shipsList);
            return tag;
        }

        // Read PlayerData from NBT
        public static PlayerData fromNbt(CompoundTag tag) {
            int maxShips = tag.getInt("maxShips");
            String playerName = tag.getString("playerName");
            PlayerData data = new PlayerData(playerName, maxShips);

            ListTag shipsList = tag.getList("ships", Tag.TAG_COMPOUND);
            for (Tag shipTag : shipsList) {
                data.ships.add(ShipData.fromNbt((CompoundTag) shipTag));
            }
            return data;
        }
    }

    // Add a player with their maxShips
    public void addPlayer(Player player, int maxShips) {
        playerDataMap.put(player.getUUID(), new PlayerData(player.getDisplayName().getString(), maxShips));
        setDirty();
    }

    // Add a ship to a player's list of ships
    public boolean addShip(UUID playerUUID, long shipId, String shipSlug) {
        PlayerData data = playerDataMap.get(playerUUID);
        if (data != null && (data.maxShips == -1 || data.ships.size() < data.maxShips) && data.ships.stream().noneMatch(s -> s.shipId == shipId)) {
            data.ships.add(new ShipData(shipId, shipSlug));
            setDirty();
            return true;
        }
        return false; // Either player does not exist or maxShips is reached
    }

    // Add a ship to a player's list of ships
    public boolean add(Player player, Ship ship, int maxShips) {
        if (!playerDataMap.containsKey(player.getUUID())) {
            addPlayer(player, maxShips);
        }
        return addShip(player.getUUID(), ship.getId(), ship.getSlug());
    }

    // Remove a ship from a player's list of ships
    public boolean removeShip(UUID playerUUID, long shipId) {
        PlayerData data = playerDataMap.get(playerUUID);
        if (data != null) {
            boolean result = data.ships.removeIf(ship -> ship.shipId == shipId);
            setDirty();
            return result;
        }
        return false; // Either player does not exist or shipId not found
    }

    // Change the maxShips for a player
    public boolean setMaxShips(UUID playerUUID, int newMaxShips) {
        PlayerData data = playerDataMap.get(playerUUID);
        if (data != null) {
            data.maxShips = newMaxShips;
            setDirty();
            return true;
        }
        return false; // Player does not exist
    }

    // Get the list of ship slugs for a player
    public ArrayList<String> getShips(Player player) {
        PlayerData data = playerDataMap.get(player.getUUID());
        ArrayList<String> ships = new ArrayList<>();
        if (data != null) {
            for (ShipData ship : data.ships) {
                ships.add(ship.shipSlug);
            }
        }
        return ships; // Return a copy of the ship list or empty if not found
    }

    // Get the list of ship slugs for a player
    public ArrayList<Long> getShipsId(Player player) {
        PlayerData data = playerDataMap.get(player.getUUID());
        ArrayList<Long> ships = new ArrayList<>();
        if (data != null) {
            for (ShipData ship : data.ships) {
                ships.add(ship.shipId);
            }
        }
        return ships; // Return a copy of the ship list or empty if not found
    }

    // Get all ships from all players
    public ArrayList<String> getAllShips() {
        ArrayList<String> allShips = new ArrayList<>();
        for (PlayerData data : playerDataMap.values()) {
            for (ShipData ship : data.ships) {
                allShips.add(ship.shipSlug);
            }
        }
        return allShips;
    }

    // Get all ships with player names from all players
    public ArrayList<Pair<String, String>> getAllShipsWithPlayerName() {
        ArrayList<Pair<String, String>> allShipsWithPlayer = new ArrayList<>();

        // Loop through all player data
        for (PlayerData data : playerDataMap.values()) {
            // For each ship in the player's data, add the ship slug and player name to the list
            for (ShipData ship : data.ships) {
                allShipsWithPlayer.add(new Pair<>(ship.shipSlug, data.playerName));
            }
        }

        return allShipsWithPlayer;
    }

    // Get all ships with player names from all players
    public ArrayList<Pair<Long, String>> getAllShipsIdWithPlayerName() {
        ArrayList<Pair<Long, String>> allShipsWithPlayer = new ArrayList<>();

        // Loop through all player data
        for (PlayerData data : playerDataMap.values()) {
            // For each ship in the player's data, add the ship slug and player name to the list
            for (ShipData ship : data.ships) {
                allShipsWithPlayer.add(new Pair<>(ship.shipId, data.playerName));
            }
        }

        return allShipsWithPlayer;
    }

    // Get all ships from all players
    public ArrayList<Long> getAllShipsId() {
        ArrayList<Long> allShips = new ArrayList<>();
        for (PlayerData data : playerDataMap.values()) {
            for (ShipData ship : data.ships) {
                allShips.add(ship.shipId);
            }
        }
        return allShips;
    }

    // Get the player's UUID from a ship ID
    public UUID getPlayerUUIDFromShip(long shipId) {
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            if (entry.getValue().ships.stream().anyMatch(ship -> ship.shipId == shipId)) {
                return entry.getKey();
            }
        }
        return null; // Ship ID not found
    }

    // Get the player's UUID from a ship Slug
    public UUID getPlayerUUIDFromShip(String shipSlug) {
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            if (entry.getValue().ships.stream().anyMatch(ship -> Objects.equals(ship.shipSlug, shipSlug))) {
                return entry.getKey();
            }
        }
        return null; // Ship ID not found
    }

    // Get the player's name from a ship ID
    public String getPlayerNameFromShip(long shipId) {
        UUID playerUUID = getPlayerUUIDFromShip(shipId);
        if (playerUUID != null) {
            PlayerData data = playerDataMap.get(playerUUID);
            if (data != null) {
                return data.playerName;
            }
        }
        return null; // Ship ID or player not found
    }

    // Get the player's name from a ship Slug
    public String getPlayerNameFromShip(String shipSlug) {
        UUID playerUUID = getPlayerUUIDFromShip(shipSlug);
        if (playerUUID != null) {
            PlayerData data = playerDataMap.get(playerUUID);
            if (data != null) {
                return data.playerName;
            }
        }
        return null; // Ship ID or player not found
    }

    // Remove a ship by its ID, regardless of the player
    public boolean removeShipById(long shipId) {
        UUID playerUUID = getPlayerUUIDFromShip(shipId);
        if (playerUUID != null) {
            return removeShip(playerUUID, shipId);
        }
        return false; // Ship ID not found
    }

    // Save data to NBT
    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag playersList = new ListTag();
        for (UUID uuid : playerDataMap.keySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", uuid);
            playerTag.put("data", playerDataMap.get(uuid).toNbt());
            playersList.add(playerTag);
        }
        compound.put("players", playersList);
        return compound;
    }

    // Read data from NBT
    public static ShipDataStore load(CompoundTag compound) {
        ShipDataStore dataStore = new ShipDataStore();
        ListTag playersList = compound.getList("players", Tag.TAG_COMPOUND);
        for (Tag tag : playersList) {
            CompoundTag playerTag = (CompoundTag) tag;
            UUID uuid = playerTag.getUUID("uuid");
            PlayerData playerData = PlayerData.fromNbt(playerTag.getCompound("data"));
            dataStore.playerDataMap.put(uuid, playerData);
        }
        return dataStore;
    }

    // Get or create the data store
    public static ShipDataStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ShipDataStore::load, ShipDataStore::new, DATA_NAME);
    }
}
