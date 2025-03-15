package org.flomik.commandBuilder;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CommandStorage {

    private final Main plugin;
    private final File commandFile;
    private FileConfiguration commandConfig;
    public final Map<String, List<String>> commandActions = new HashMap<>();

    public CommandStorage(Main plugin) {
        this.plugin = plugin;
        commandFile = new File(plugin.getDataFolder(), "commands.yml");

        if (!commandFile.exists()) {
            plugin.saveResource("commands.yml", false);
        }

        commandConfig = YamlConfiguration.loadConfiguration(commandFile);
        loadCommands();
    }

    public void loadCommands() {

        commandConfig = YamlConfiguration.loadConfiguration(commandFile);

        commandActions.clear();
        if (commandConfig.getConfigurationSection("commands") == null) {
            return;
        }

        for (String cmdName : commandConfig.getConfigurationSection("commands").getKeys(false)) {
            List<String> acts = commandConfig.getStringList("commands." + cmdName + ".actions");
            commandActions.put(cmdName.toLowerCase(), new ArrayList<>(acts));
        }
    }

    public void saveCommands() {
        for (String cmdName : commandActions.keySet()) {
            commandConfig.set("commands." + cmdName + ".actions", commandActions.get(cmdName));
        }

        try {
            commandConfig.save(commandFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getStoredCommands() {
        return new ArrayList<>(commandActions.keySet());
    }

    public void addCommand(String name) {
        if (!commandActions.containsKey(name)) {
            commandActions.put(name, new ArrayList<>());
            saveCommands();
        }
    }

    public void removeCommand(String command) {
        if (commandActions.containsKey(command)) {
            commandActions.remove(command);
            commandConfig.set("commands." + command, null);
            saveCommands();
        }
    }

    public void addAction(String command, String action) {
        List<String> actions = commandActions.getOrDefault(command, new ArrayList<>());
        actions.add(action);
        commandActions.put(command, actions);
        saveCommands();
    }

    public void removeAction(String command, int index) {
        List<String> actions = commandActions.get(command);
        if (actions != null && index >= 0 && index < actions.size()) {
            actions.remove(index);
            commandActions.put(command, actions);
            saveCommands();
        }
    }

    public List<String> getActions(String command) {
        return commandActions.getOrDefault(command, new ArrayList<>());
    }
}
