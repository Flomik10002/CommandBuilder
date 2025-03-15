package org.flomik.commandBuilder;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FlagManager {

    private final Main plugin;
    private final File flagFile;
    private FileConfiguration flagConfig;

    private final Map<UUID, Map<String, Boolean>> playerFlags = new HashMap<>();

    private final Set<String> defaultFlags = new HashSet<>();

    public FlagManager(Main plugin) {
        this.plugin = plugin;
        flagFile = new File(plugin.getDataFolder(), "flags.yml");

        if (!flagFile.exists()) {
            plugin.saveResource("flags.yml", false);
        }

        flagConfig = YamlConfiguration.loadConfiguration(flagFile);
        loadFlags();
    }

    public void removeFlagFromAll(String flagName) {

        defaultFlags.remove(flagName);


        for (UUID uuid : playerFlags.keySet()) {
            if (playerFlags.get(uuid).containsKey(flagName)) {
                playerFlags.get(uuid).remove(flagName);
            }
        }

        saveFlags();
    }

    public void loadFlags() {

        flagConfig = YamlConfiguration.loadConfiguration(flagFile);

        playerFlags.clear();
        defaultFlags.clear();


        List<String> loadedDefaults = flagConfig.getStringList("defaults");
        defaultFlags.addAll(loadedDefaults);

        if (flagConfig.getConfigurationSection("players") != null) {
            for (String uuidString : flagConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                Map<String, Boolean> flags = new HashMap<>();
                for (String flag : flagConfig.getConfigurationSection("players." + uuidString).getKeys(false)) {
                    boolean value = flagConfig.getBoolean("players." + uuidString + "." + flag);
                    flags.put(flag, value);
                }
                playerFlags.put(uuid, flags);
            }
        }
    }

    private void saveFlags() {
        flagConfig.set("defaults", new ArrayList<>(defaultFlags));

        for (UUID uuid : playerFlags.keySet()) {
            Map<String, Boolean> flagsMap = playerFlags.get(uuid);
            for (Map.Entry<String, Boolean> entry : flagsMap.entrySet()) {
                String flag = entry.getKey();
                boolean value = entry.getValue();
                flagConfig.set("players." + uuid + "." + flag, value);
            }
        }

        try {
            flagConfig.save(flagFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllFlags() {
        Set<String> allFlags = new HashSet<>();
        for (Map<String, Boolean> flags : playerFlags.values()) {
            allFlags.addAll(flags.keySet());
        }
        allFlags.addAll(defaultFlags);

        return new ArrayList<>(allFlags);
    }

    public void setFlag(UUID playerUUID, String flag, boolean value) {
        playerFlags.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(flag, value);
        saveFlags();
    }

    public void removeFlag(UUID playerUUID, String flag) {
        if (playerFlags.containsKey(playerUUID)) {
            playerFlags.get(playerUUID).remove(flag);
            saveFlags();
        }
    }

    public boolean hasFlag(UUID playerUUID, String flag) {
        return playerFlags.getOrDefault(playerUUID, new HashMap<>()).getOrDefault(flag, false);
    }

    public Set<String> getDefaultFlags() {
        return Collections.unmodifiableSet(defaultFlags);
    }

    public void addDefaultFlag(String flagName) {
        if (!defaultFlags.contains(flagName)) {
            defaultFlags.add(flagName);
            for (UUID uuid : playerFlags.keySet()) {
                setFlag(uuid, flagName, false);
            }
            saveFlags();
        }
    }

    public void removeDefaultFlag(String flagName) {
        if (defaultFlags.contains(flagName)) {
            defaultFlags.remove(flagName);
            for (UUID uuid : playerFlags.keySet()) {
                removeFlag(uuid, flagName);
            }
            saveFlags();
        }
    }
}