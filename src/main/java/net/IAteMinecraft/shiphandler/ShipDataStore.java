package net.IAteMinecraft.shiphandler;

import com.mojang.datafixers.util.Pair;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import org.valkyrienskies.core.api.ships.Ship;

import java.util.*;

public class ShipDataStore extends SavedData {
    private static final String DATA_NAME = "ship_data";

    private final HashMap<UUID, PlayerData> playerDataMap; // Stores player data
    private final HashMap<Long, ShipData> shipDataMap;     // Stores ships

    public ShipDataStore() {
        this.playerDataMap = new HashMap<>();
        this.shipDataMap = new HashMap<>();
    }

    // Inner class to store ship data
    private static class ShipData {
        long shipId;
        String shipSlug;
        UUID createdBy;

        ShipData(long shipId, String shipSlug, UUID createdBy) {
            this.shipId = shipId;
            this.shipSlug = shipSlug;
            this.createdBy = createdBy;
        }

        // Convert ShipData to NBT
        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("shipId", shipId);
            tag.putString("shipSlug", shipSlug);
            tag.putUUID("createdBy", createdBy);
            return tag;
        }

        // Read ShipData from NBT
        public static ShipData fromNbt(CompoundTag tag) {
            long shipId = tag.getLong("shipId");
            String shipSlug = tag.getString("shipSlug");
            UUID createdBy = tag.getUUID("createdBy");
            return new ShipData(shipId, shipSlug, createdBy);
        }
    }

    // Inner class to store player data
    private static class PlayerData {
        int maxShips;
        boolean autoRegister;
        String playerName;
        ArrayList<Long> shipIds;

        PlayerData(String playerName, int maxShips, boolean autoRegister) {
            this.playerName = playerName;
            this.maxShips = maxShips;
            this.autoRegister = autoRegister;
            this.shipIds = new ArrayList<>();
        }

        // Convert PlayerData to NBT
        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("maxShips", maxShips);
            tag.putString("playerName", playerName);
            tag.putBoolean("autoRegister", autoRegister);

            ListTag shipIdsList = new ListTag();
            for (long shipId : shipIds) {
                shipIdsList.add(LongTag.valueOf(shipId));
            }
            tag.put("shipIds", shipIdsList);
            return tag;
        }

        // Read PlayerData from NBT
        public static PlayerData fromNbt(CompoundTag tag) {
            int maxShips = tag.getInt("maxShips");
            boolean autoRegister = tag.getBoolean("autoRegister");
            String playerName = tag.getString("playerName");
            PlayerData data = new PlayerData(playerName, maxShips, autoRegister);

            ListTag shipIdsList = tag.getList("shipIds", Tag.TAG_LONG);
            for (Tag idTag : shipIdsList) {
                data.shipIds.add(((LongTag) idTag).getAsLong());
            }
            return data;
        }
    }

    // Add a player to the store
    public void addPlayer(Player player, int maxShips, boolean autoRegister) {
        playerDataMap.put(player.getUUID(), new PlayerData(player.getDisplayName().getString(), maxShips, autoRegister));
        setDirty();
    }

    // Add a new ship to the ship store
    public boolean addShip(Player player, Ship ship) {
        long shipId = ship.getId();
        if (!shipDataMap.containsKey(shipId)) {
            ShipData newShip = new ShipData(shipId, ship.getSlug(), player.getUUID());
            shipDataMap.put(shipId, newShip);
            setDirty();
            return true;
        }
        return false;
    }

    // Register an existing ship to an existing player
    public boolean registerShip(Player player, Ship ship) {
        //player.sendSystemMessage(Component.literal("Register Ship Attempt for ship: " + ship.getSlug() + ", by player: ").append(player.getDisplayName()));
        if (!playerDataMap.containsKey(player.getUUID())) {
            addPlayer(player, ShiphandlerConfig.server().maxShips.get(), ShiphandlerConfig.server().autoRegister.get());
            //player.sendSystemMessage(Component.literal("Player has not been added to dataStore, adding: ").append(player.getDisplayName()));
        }
        PlayerData playerData = playerDataMap.get(player.getUUID());
        if (playerData != null && shipDataMap.containsKey(ship.getId())) {
            playerData.shipIds.add(ship.getId());
            //player.sendSystemMessage(Component.literal("Added ship: " + ship.getSlug() + ", " + ship.getId() + ", for player: ").append(player.getDisplayName()));
            setDirty();
            return true;
        }
        return false;
    }

    // Remove a ship from the store and unregister it from the player
    public boolean removeShip(long shipId) {
        ShipData removedShip = shipDataMap.remove(shipId);
        if (removedShip != null) {
            for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
                for (Long iterShipId : entry.getValue().shipIds) {
                    if (iterShipId == shipId) {
                        entry.getValue().shipIds.remove(shipId);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Unregister a ship from a player without deleting the ship data
    public boolean unregisterShip(Player player, long shipId) {
        PlayerData playerData = playerDataMap.get(player.getUUID());
        if (playerData != null) {
            boolean removed = playerData.shipIds.remove(shipId);
            setDirty();
            return removed;
        }
        return false;
    }

    // Unregister a ship from a player using only the ship ID, without deleting the ship data
    public boolean unregisterShip(long shipId) {
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            for (Long iterShipId : entry.getValue().shipIds) {
                if (iterShipId == shipId) {
                    entry.getValue().shipIds.remove(shipId);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean setMaxShips(Player player, int maxShips) {
        PlayerData playerData = playerDataMap.get(player.getUUID());
        if (playerData != null) {
            playerData.maxShips = maxShips;
            return true;
        }
        return false;
    }

    // Get the ID and slug of all registered ships
    public ArrayList<Pair<Long, String>> getAllRegisteredShipIdsAndSlugs() {
        ArrayList<Pair<Long, String>> shipList = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            for (Long shipId : entry.getValue().shipIds) {
                shipList.add(new Pair<>(shipId, shipDataMap.get(shipId).shipSlug));
            }
        }
        return shipList;
    }

    // Get the ID of all registered ships
    public ArrayList<Long> getAllRegisteredShipIDs() {
        ArrayList<Long> shipList = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            shipList.addAll(entry.getValue().shipIds);
        }
        return shipList;
    }

    // Get the ID of all registered ships
    public ArrayList<String> getAllRegisteredShipSlugs() {
        ArrayList<String> shipList = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            for (Long shipId : entry.getValue().shipIds) {
                shipList.add(shipDataMap.get(shipId).shipSlug);
            }
        }
        return shipList;
    }

    // Get the ID and slug of all registered ships along with the player name of the owner
    public ArrayList<Pair<String, Pair<Long, String>>> getAllRegisteredShipIdsAndSlugsWithPlayerName() {
        ArrayList<Pair<String, Pair<Long, String>>> shipListWithPlayer = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            for (Long shipId : entry.getValue().shipIds) {
                PlayerData playerData = playerDataMap.get(shipDataMap.get(shipId).createdBy);
                if (playerData != null) {
                    shipListWithPlayer.add(new Pair<>(playerData.playerName, new Pair<>(shipId, shipDataMap.get(shipId).shipSlug)));
                }
            }
        }
        return shipListWithPlayer;
    }

    // Get the ID of all registered ships along with the player name of the owner
    public ArrayList<Pair<String, Long>> getAllRegisteredShipIdsWithPlayerName() {
        ArrayList<Pair<String, Long>> shipListWithPlayer = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            for (Long shipId : entry.getValue().shipIds) {
                PlayerData playerData = playerDataMap.get(shipDataMap.get(shipId).createdBy);
                if (playerData != null) {
                    shipListWithPlayer.add(new Pair<>(playerData.playerName, shipId));
                }
            }
        }
        return shipListWithPlayer;
    }

    // Get the ID of all registered ships along with the player name of the owner
    public ArrayList<Pair<String, String>> getAllRegisteredShipSlugsWithPlayerName() {
        ArrayList<Pair<String, String>> shipListWithPlayer = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            for (Long shipId : entry.getValue().shipIds) {
                PlayerData playerData = playerDataMap.get(shipDataMap.get(shipId).createdBy);
                if (playerData != null) {
                    shipListWithPlayer.add(new Pair<>(playerData.playerName, shipDataMap.get(shipId).shipSlug));
                }
            }
        }
        return shipListWithPlayer;
    }

    // Get the ID and slug of all registered ships
    public ArrayList<Pair<Long, String>> getAllShipIdsAndSlugs() {
        ArrayList<Pair<Long, String>> shipList = new ArrayList<>();
        for (ShipData ship : shipDataMap.values()) {
            shipList.add(new Pair<>(ship.shipId, ship.shipSlug));
        }
        return shipList;
    }

    // Get the ID of all registered ships
    public ArrayList<Long> getAllShipIDs() {
        ArrayList<Long> shipList = new ArrayList<>();
        for (ShipData ship : shipDataMap.values()) {
            shipList.add(ship.shipId);
        }
        return shipList;
    }

    // Get the ID of all registered ships
    public ArrayList<String> getAllShipSlugs() {
        ArrayList<String> shipList = new ArrayList<>();
        for (ShipData ship : shipDataMap.values()) {
            shipList.add(ship.shipSlug);
        }
        return shipList;
    }

    // Get the ID and slug of all registered ships along with the player name of the owner
    public ArrayList<Pair<String, Pair<Long, String>>> getAllShipIdsSlugsWithPlayerName() {
        ArrayList<Pair<String, Pair<Long, String>>> shipListWithPlayer = new ArrayList<>();
        for (ShipData ship : shipDataMap.values()) {
            PlayerData playerData = playerDataMap.get(ship.createdBy);
            if (playerData != null) {
                shipListWithPlayer.add(new Pair<>(playerData.playerName, new Pair<>(ship.shipId, ship.shipSlug)));
            }
        }
        return shipListWithPlayer;
    }

    // Get the ID of all registered ships along with the player name of the owner
    public ArrayList<Pair<String, Long>> getAllShipIdsWithPlayerName() {
        ArrayList<Pair<String, Long>> shipListWithPlayer = new ArrayList<>();
        for (ShipData ship : shipDataMap.values()) {
            PlayerData playerData = playerDataMap.get(ship.createdBy);
            if (playerData != null) {
                shipListWithPlayer.add(new Pair<>(playerData.playerName, ship.shipId));
            }
        }
        return shipListWithPlayer;
    }

    // Get the ID of all registered ships along with the player name of the owner
    public ArrayList<Pair<String, String>> getAllShipSlugsWithPlayerName() {
        ArrayList<Pair<String, String>> shipListWithPlayer = new ArrayList<>();
        for (Map.Entry<Long, ShipData> entry: shipDataMap.entrySet()) {
            PlayerData playerData = playerDataMap.get(entry.getValue().createdBy);
            if (playerData != null) {
                shipListWithPlayer.add(new Pair<>(playerData.playerName, entry.getValue().shipSlug));
            }
        }
        return shipListWithPlayer;
    }

    // Get the ID and slug of all ships owned by a specific player
    public ArrayList<Pair<Long, String>> getRegisteredShipIdsAndSlugsByPlayer(Player player) {
        ArrayList<Pair<Long, String>> shipsOwned = new ArrayList<>();
        PlayerData playerData = playerDataMap.get(player.getUUID());
        if (playerData != null) {
            for (long shipId : playerData.shipIds) {
                ShipData shipData = shipDataMap.get(shipId);
                if (shipData != null) {
                    shipsOwned.add(new Pair<>(shipData.shipId, shipData.shipSlug));
                }
            }
        }
        return shipsOwned;
    }

    // Get the ID  of all ships owned by a specific player
    public ArrayList<Long> getRegisteredShipsId(Player player) {
        ArrayList<Long> shipsOwned = new ArrayList<>();
        PlayerData playerData = playerDataMap.get(player.getUUID());
        if (playerData != null) {
            for (long shipId : playerData.shipIds) {
                ShipData shipData = shipDataMap.get(shipId);
                if (shipData != null && shipData.createdBy == player.getUUID()) {
                    shipsOwned.add(shipData.shipId);
                }
            }
        }
        return shipsOwned;
    }

    // Get the slug of all ships owned by a specific player
    public ArrayList<String> getRegisteredShipsSlug(Player player) {
        ArrayList<String> shipsOwned = new ArrayList<>();
        PlayerData playerData = playerDataMap.get(player.getUUID());
        if (playerData != null) {
            for (long shipId : playerData.shipIds) {
                ShipData shipData = shipDataMap.get(shipId);
                if (shipData != null && shipData.createdBy == player.getUUID()) {
                    shipsOwned.add(shipData.shipSlug);
                }
            }
        }
        return shipsOwned;
    }

    // Get the ID and slug of all ships owned by a specific player
    public ArrayList<Pair<Long, String>> getShipIdsAndSlugsByPlayer(Player player) {
        ArrayList<Pair<Long, String>> shipsOwned = new ArrayList<>();
        for (Map.Entry<Long, ShipData> entry : shipDataMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().createdBy == player.getUUID()) {
                shipsOwned.add(new Pair<>(entry.getValue().shipId, entry.getValue().shipSlug));
            }
        }
        return shipsOwned;
    }

    // Get the ID  of all ships owned by a specific player
    public ArrayList<Long> getShipsId(Player player) {
        ArrayList<Long> shipsOwned = new ArrayList<>();
        for (Map.Entry<Long, ShipData> entry : shipDataMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().createdBy == player.getUUID()) {
                shipsOwned.add(entry.getValue().shipId);
            }
        }
        return shipsOwned;
    }

    // Get the slug of all ships owned by a specific player
    public ArrayList<String> getShipsSlug(Player player) {
        ArrayList<String> shipsOwned = new ArrayList<>();
        for (Map.Entry<Long, ShipData> entry : shipDataMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().createdBy == player.getUUID()) {
                shipsOwned.add(entry.getValue().shipSlug);
            }
        }

        return shipsOwned;
    }

    // Get the player name from a ship's ID
    public String getPlayerName(long shipId) {
        ShipData ship = shipDataMap.get(shipId);
        if (ship != null) {
            PlayerData playerData = playerDataMap.get(ship.createdBy);
            if (playerData != null) {
                return playerData.playerName;
            }
        }
        return null; // Ship ID or player not found
    }

    // Get the player name from a ship's slug
    public String getPlayerName(String shipSlug) {
        for (ShipData ship : shipDataMap.values()) {
            if (shipSlug.equals(ship.shipSlug)) {
                PlayerData playerData = playerDataMap.get(ship.createdBy);
                if (playerData != null) {
                    return playerData.playerName;
                }
            }
        }
        return null; // Ship slug or player not found
    }

    public boolean hasPlayer(Player player) {
        return playerDataMap.containsKey(player.getUUID());
    }

    public int getMaxShips(Player player) {
        return playerDataMap.get(player.getUUID()).maxShips;
    }

    public int getCurrentRegisteredShipCount(Player player) {
        return playerDataMap.get(player.getUUID()).shipIds.size();
    }

    public void setAutoRegister(Player player, boolean autoRegister) {
        playerDataMap.get(player.getUUID()).autoRegister = autoRegister;
    }

    public boolean usesAutoRegister(Player player) {
        return playerDataMap.get(player.getUUID()).autoRegister;
    }

    // Methods for saving/loading the store
    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag playerList = new ListTag();
        for (UUID uuid : playerDataMap.keySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", uuid);
            playerTag.put("data", playerDataMap.get(uuid).toNbt());
            playerList.add(playerTag);
        }
        compound.put("players", playerList);

        ListTag shipList = new ListTag();
        for (ShipData shipData : shipDataMap.values()) {
            shipList.add(shipData.toNbt());
        }
        compound.put("ships", shipList);

        return compound;
    }

    public static ShipDataStore load(CompoundTag compound) {
        ShipDataStore dataStore = new ShipDataStore();

        ListTag playerList = compound.getList("players", Tag.TAG_COMPOUND);
        for (Tag playerTag : playerList) {
            CompoundTag tag = (CompoundTag) playerTag;
            UUID uuid = tag.getUUID("uuid");
            PlayerData playerData = PlayerData.fromNbt(tag.getCompound("data"));
            dataStore.playerDataMap.put(uuid, playerData);
        }

        ListTag shipList = compound.getList("ships", Tag.TAG_COMPOUND);
        for (Tag shipTag : shipList) {
            ShipData shipData = ShipData.fromNbt((CompoundTag) shipTag);
            dataStore.shipDataMap.put(shipData.shipId, shipData);
        }

        return dataStore;
    }

    // Get or create the data store
    public static ShipDataStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ShipDataStore::load, ShipDataStore::new, DATA_NAME);
    }
}
