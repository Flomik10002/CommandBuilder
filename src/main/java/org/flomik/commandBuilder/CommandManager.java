package org.flomik.commandBuilder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду можно выполнять только игроком!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Использование: /" + label + " <сабкоманда>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                if (!player.hasPermission("commandbuilder.admin")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды!");
                    return true;
                }
                plugin.reloadPlugin();
                player.sendMessage(ChatColor.GREEN + "Плагин CommandBuilder успешно перезагружен!");
                return true;
            }

            case "createcommand" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /commandbuilder createcommand <название>");
                    return true;
                }
                String commandName = args[1].toLowerCase();
                plugin.getCommandStorage().addCommand(commandName);
                player.sendMessage(ChatColor.GREEN + "Кастомная команда " + commandName + " создана.");
                return true;
            }

            case "deletecommand" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /commandbuilder deletecommand <название>");
                    return true;
                }
                String commandName = args[1].toLowerCase();
                plugin.getCommandStorage().removeCommand(commandName);
                player.sendMessage(ChatColor.GREEN + "Команда " + commandName + " удалена.");
                return true;
            }

            case "addaction" -> {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Использование: /commandbuilder addaction <команда> <тип> <значение>");
                    return true;
                }
                String commandName = args[1].toLowerCase();
                String actionType = args[2];

                String actionValue = String.join(" ", args).substring(
                        args[0].length() + args[1].length() + args[2].length() + 3
                );

                plugin.getCommandStorage().addAction(commandName, actionType + ":" + actionValue);
                player.sendMessage(ChatColor.GREEN + "Добавлено действие '" + actionType + "' в команду " + commandName + ".");
                return true;
            }

            case "removeaction" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Использование: /commandbuilder removeaction <команда> <номер>");
                    return true;
                }
                String commandName = args[1].toLowerCase();
                int index;
                try {
                    index = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Номер действия должен быть числом.");
                    return true;
                }
                plugin.getCommandStorage().removeAction(commandName, index);
                player.sendMessage(ChatColor.GREEN + "Удалено действие #" + index + " из команды " + commandName + ".");
                return true;
            }

            case "addflag" -> {
                if (!player.hasPermission("commandbuilder.admin")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /commandbuilder addflag <flagName>");
                    return true;
                }
                String flagName = args[1].toLowerCase();

                plugin.getFlagManager().addDefaultFlag(flagName);
                player.sendMessage(ChatColor.GREEN + "Флаг '" + flagName + "' добавлен и установлен в false всем игрокам (и новым тоже).");
                return true;
            }

            case "removeflag" -> {
                if (!player.hasPermission("commandbuilder.admin")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /commandbuilder removeflag <flagName>");
                    return true;
                }
                String flagName = args[1].toLowerCase();

                plugin.getFlagManager().removeFlagFromAll(flagName);
                player.sendMessage(ChatColor.GREEN + "Флаг '" + flagName + "' убран из дефолтов и удалён у всех игроков.");
                return true;
            }

            case "setflag" -> {
                if (!player.hasPermission("commandbuilder.admin")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды!");
                    return true;
                }
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Использование: /commandbuilder setflag <ник> <флаг> <true|false>");
                    return true;
                }
                String targetName = args[1];
                String flagName = args[2].toLowerCase();
                String boolStr = args[3].toLowerCase();

                boolean value;
                if (boolStr.equals("true")) {
                    value = true;
                } else if (boolStr.equals("false")) {
                    value = false;
                } else {
                    player.sendMessage(ChatColor.RED + "Третий аргумент должен быть true или false!");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Игрок " + targetName + " не найден (не в сети).");
                    return true;
                }

                plugin.getFlagManager().setFlag(target.getUniqueId(), flagName, value);
                player.sendMessage(ChatColor.GREEN + "Флаг '" + flagName + "' у " + targetName + " теперь = " + value);
                return true;
            }

            case "listcommands" -> {
                if (!player.hasPermission("commandbuilder.admin")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды!");
                    return true;
                }

                List<String> cmds = plugin.getCommandStorage().getStoredCommands();
                if (cmds.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "Пока нет ни одной кастомной команды.");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Список кастомных команд:");
                    for (String c : cmds) {
                        player.sendMessage(ChatColor.AQUA + " - " + c);
                    }
                }
                return true;
            }

            case "listactions" -> {
                if (!player.hasPermission("commandbuilder.admin")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /commandbuilder listactions <команда>");
                    return true;
                }
                String cmdName = args[1].toLowerCase();
                List<String> acts = plugin.getCommandStorage().getActions(cmdName);
                if (acts.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "У команды '" + cmdName + "' нет действий или она не существует.");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Действия команды '" + cmdName + "':");
                    for (int i = 0; i < acts.size(); i++) {

                        player.sendMessage(ChatColor.AQUA + "" + i + ". " + ChatColor.WHITE + acts.get(i));
                    }
                }
                return true;
            }

            default -> {
                player.sendMessage(ChatColor.RED + "Неизвестная сабкоманда. Используйте: /commandbuilder <subcommand>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }


        if (args.length == 0) {
            return List.of("reload", "createcommand", "deletecommand", "addaction", "removeaction",
                    "addflag", "removeflag", "setflag", "listcommands", "listactions");
        }

        List<String> completions = new ArrayList<>();
        String firstArg = args[0].toLowerCase();


        if (args.length == 1) {


            completions.add("reload");
            completions.add("createcommand");
            completions.add("deletecommand");
            completions.add("addaction");
            completions.add("removeaction");
            completions.add("addflag");
            completions.add("removeflag");
            completions.add("setflag");
            completions.add("listcommands");
            completions.add("listactions");

            return filter(completions, args[0]);
        }


        switch (firstArg) {




            case "reload" -> {


                return List.of();
            }

            case "createcommand" -> {



                return List.of();
            }

            case "deletecommand" -> {

                if (args.length == 2) {

                    completions.addAll(plugin.getCommandStorage().getStoredCommands());
                    return filter(completions, args[1]);
                }

                return List.of();
            }

            case "addflag", "removeflag" -> {


                if (args.length == 2) {


                    if (firstArg.equals("removeflag")) {
                        completions.addAll(plugin.getFlagManager().getAllFlags());
                    }

                    return filter(completions, args[1]);
                }
                return List.of();
            }

            case "listcommands" -> {


                return List.of();
            }




            case "listactions" -> {
                if (args.length == 2) {

                    completions.addAll(plugin.getCommandStorage().getStoredCommands());
                    return filter(completions, args[1]);
                }
                return List.of();
            }




            case "removeaction" -> {
                if (args.length == 2) {

                    completions.addAll(plugin.getCommandStorage().getStoredCommands());
                    return filter(completions, args[1]);
                }
                if (args.length == 3) {

                    String cmdName = args[1].toLowerCase();
                    List<String> acts = plugin.getCommandStorage().getActions(cmdName);
                    for (int i = 0; i < acts.size(); i++) {
                        completions.add(String.valueOf(i));
                    }
                    return filter(completions, args[2]);
                }
                return List.of();
            }




            case "addaction" -> {
                if (args.length == 2) {

                    completions.addAll(plugin.getCommandStorage().getStoredCommands());
                    return filter(completions, args[1]);
                }
                if (args.length == 3) {


                    completions.add("message");
                    completions.add("command");
                    completions.add("setflag");
                    completions.add("removeflag");
                    completions.add("delay");
                    completions.add("checkflag");
                    return filter(completions, args[2]);
                }
                if (args.length == 4) {

                    String actionType = args[2].toLowerCase();
                    switch (actionType) {
                        case "message" -> {

                            completions.add("\"&7Привет, %player%!\"");
                            completions.add("&aТестСообщение");
                        }
                        case "command" -> {
                            completions.add("\"give %player% apple 5\"");
                            completions.add("kill %player%");
                        }
                        case "setflag" -> {

                            for (String f : plugin.getFlagManager().getAllFlags()) {
                                completions.add(f + "=true");
                                completions.add(f + "=false");
                            }
                        }
                        case "removeflag" -> {

                            completions.addAll(plugin.getFlagManager().getAllFlags());
                        }
                        case "delay" -> {

                            completions.add("3");
                            completions.add("5");
                            completions.add("10");
                        }
                        case "checkflag" -> {

                            for (String f : plugin.getFlagManager().getAllFlags()) {
                                completions.add(f + "=true");
                                completions.add(f + "=false");
                            }
                        }
                    }
                    return filter(completions, args[3]);
                }


                return List.of();
            }




            case "setflag" -> {
                if (args.length == 2) {

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                    return filter(completions, args[1]);
                }
                if (args.length == 3) {

                    completions.addAll(plugin.getFlagManager().getAllFlags());
                    return filter(completions, args[2]);
                }
                if (args.length == 4) {

                    completions.add("true");
                    completions.add("false");
                    return filter(completions, args[3]);
                }
                return List.of();
            }







            default -> {

                return List.of();
            }
        }
    }


    private List<String> filter(List<String> suggestions, String input) {
        String lower = input.toLowerCase();
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}