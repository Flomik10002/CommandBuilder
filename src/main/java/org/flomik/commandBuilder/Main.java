package org.flomik.commandBuilder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Main extends JavaPlugin implements Listener {

    private static Main instance;
    private CommandStorage commandStorage;
    private FlagManager flagManager;

    @Override
    public void onEnable() {
        instance = this;

        commandStorage = new CommandStorage(this);
        flagManager = new FlagManager(this);

        getCommand("commandbuilder").setExecutor(new CommandManager(this));

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("CommandBuilder включен!");
    }

    @Override
    public void onDisable() {

        commandStorage.saveCommands();
        getLogger().info("CommandBuilder выключен!");
    }

    public static Main getInstance() {
        return instance;
    }

    public CommandStorage getCommandStorage() {
        return commandStorage;
    }

    public FlagManager getFlagManager() {
        return flagManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            for (String defaultFlag : getFlagManager().getDefaultFlags()) {
                getFlagManager().setFlag(player.getUniqueId(), defaultFlag, false);
            }
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {

        String message = event.getMessage().substring(1);
        String[] split = message.split(" ", 2);
        String commandLabel = split[0].toLowerCase();


        if (commandLabel.equalsIgnoreCase("commandbuilder")) {
            return;
        }


        List<String> actions = commandStorage.getActions(commandLabel);
        if (actions.isEmpty()) {

            return;
        }


        event.setCancelled(true);


        processActions(event.getPlayer(), actions, 0);
    }

    /**
     * Последовательно обрабатывает список действий.
     * Если встречаем delay => пауза
     * Если checkflag => при совпадении значения флага выполняем под-действия
     * и т.д.
     */
    private void processActions(Player player, List<String> actions, int index) {
        if (index >= actions.size()) {
            return;
        }

        String actionLine = actions.get(index);

        String[] parts = actionLine.split(":", 2);
        if (parts.length < 2) {

            processActions(player, actions, index + 1);
            return;
        }

        String type = parts[0].toLowerCase();

        String rawValue = parts[1].replace("%player%", player.getName());

        switch (type) {
            case "message" -> {

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', rawValue));
                processActions(player, actions, index + 1);
            }
            case "command" -> {

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rawValue);
                processActions(player, actions, index + 1);
            }
            case "delay" -> {

                int delaySec;
                try {
                    delaySec = Integer.parseInt(rawValue);
                } catch (NumberFormatException e) {
                    processActions(player, actions, index + 1);
                    return;
                }
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    processActions(player, actions, index + 1);
                }, delaySec * 20L);
            }
            case "setflag" -> {

                String[] arr = rawValue.split("=", 2);
                if (arr.length < 2) {
                    player.sendMessage(ChatColor.RED + "[ERROR] setflag: нужен формат flagName=true/false");
                } else {
                    String flagName = arr[0];
                    boolean val = Boolean.parseBoolean(arr[1]);
                    flagManager.setFlag(player.getUniqueId(), flagName, val);
                }
                processActions(player, actions, index + 1);
            }
            case "removeflag" -> {

                flagManager.removeFlag(player.getUniqueId(), rawValue);
                processActions(player, actions, index + 1);
            }
            case "checkflag" -> {

                String[] splitted = rawValue.split(" ", 2);
                if (splitted.length < 2) {
                    processActions(player, actions, index + 1);
                    return;
                }


                String conditionPart = splitted[0];
                String subActionsRaw = splitted[1];


                String[] arr = conditionPart.split("=", 2);
                if (arr.length < 2) {
                    player.sendMessage(ChatColor.RED + "[ERROR] checkflag: нужно flagName=true/false ...");
                    processActions(player, actions, index + 1);
                    return;
                }
                String flagName = arr[0];
                boolean requiredVal = Boolean.parseBoolean(arr[1]);

                boolean actualVal = flagManager.hasFlag(player.getUniqueId(), flagName);
                if (actualVal == requiredVal) {

                    List<String> subActions = parseSubActions(subActionsRaw);
                    if (!subActions.isEmpty()) {
                        processSubActions(player, subActions, 0, () -> {
                            processActions(player, actions, index + 1);
                        });
                        return;
                    }
                }

                processActions(player, actions, index + 1);
            }
            default -> {

                processActions(player, actions, index + 1);
            }
        }
    }

    /**
     * Разбивает строку под-действий на список:
     * Пример входа: "message:\"&7Привет\" setflag:myFlag=true command:\"kill %player%\""
     * => токены:
     *   1) message:"&7Привет"
     *   2) setflag:myFlag=true
     *   3) command:"kill %player%"
     */
    private List<String> parseSubActions(String input) {
        List<String> result = new ArrayList<>();
        int pos = 0;

        while (pos < input.length()) {

            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
            if (pos >= input.length()) break;


            if (input.charAt(pos) == '"') {
                pos++;
                StringBuilder sb = new StringBuilder();
                while (pos < input.length() && input.charAt(pos) != '"') {
                    sb.append(input.charAt(pos));
                    pos++;
                }

                if (pos < input.length()) {
                    pos++;
                }
                result.add(sb.toString());
            } else {

                StringBuilder sb = new StringBuilder();
                while (pos < input.length() && !Character.isWhitespace(input.charAt(pos))) {
                    sb.append(input.charAt(pos));
                    pos++;
                }
                result.add(sb.toString());
            }
        }
        return result;
    }

    /**
     * Обрабатываем subActions (которые идут после checkflag).
     * Когда дошли до конца, вызываем callback.
     */
    private void processSubActions(Player player, List<String> actions, int index, Runnable callback) {
        if (index >= actions.size()) {
            callback.run();
            return;
        }

        String actionLine = actions.get(index);
        String[] parts = actionLine.split(":", 2);
        if (parts.length < 2) {
            processSubActions(player, actions, index + 1, callback);
            return;
        }

        String type = parts[0].toLowerCase();
        String rawValue = parts[1];


        if (rawValue.startsWith("\"") && rawValue.endsWith("\"") && rawValue.length() > 1) {
            rawValue = rawValue.substring(1, rawValue.length() - 1);
        }
        rawValue = rawValue.replace("%player%", player.getName());

        switch (type) {
            case "message" -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', rawValue));
                processSubActions(player, actions, index + 1, callback);
            }
            case "command" -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rawValue);
                processSubActions(player, actions, index + 1, callback);
            }
            case "delay" -> {
                try {
                    int sec = Integer.parseInt(rawValue);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        processSubActions(player, actions, index + 1, callback);
                    }, sec * 20L);
                } catch (NumberFormatException e) {
                    processSubActions(player, actions, index + 1, callback);
                }
            }
            case "setflag" -> {
                String[] arr = rawValue.split("=", 2);
                if (arr.length < 2) {
                    player.sendMessage(ChatColor.RED + "[ERROR] setflag: нужен формат flagName=true/false");
                } else {
                    String flagName = arr[0];
                    boolean val = Boolean.parseBoolean(arr[1]);
                    flagManager.setFlag(player.getUniqueId(), flagName, val);
                }
                processSubActions(player, actions, index + 1, callback);
            }
            case "removeflag" -> {
                flagManager.removeFlag(player.getUniqueId(), rawValue);
                processSubActions(player, actions, index + 1, callback);
            }
            default -> {
                processSubActions(player, actions, index + 1, callback);
            }
        }
    }

    public void reloadPlugin() {
        getLogger().info("Перезагрузка CommandBuilder...");
        commandStorage.loadCommands();
        flagManager.loadFlags();
        getLogger().info("Плагин CommandBuilder успешно перезагружен!");
    }
}
