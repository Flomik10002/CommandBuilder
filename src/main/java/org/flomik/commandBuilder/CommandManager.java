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

                plugin.getFlagManager().removeDefaultFlag(flagName);
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
                        // Показываем индекс, чтобы потом проще было removeaction
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

        List<String> completions = new ArrayList<>();

        // Главное меню автодополнения
        if (args.length == 1) {
            completions.add("reload");
            completions.add("createcommand");
            completions.add("deletecommand");
            completions.add("addaction");
            completions.add("removeaction");
            completions.add("addflag");
            completions.add("removeflag");
            completions.add("setflag");
            completions.add("checkflag");
            completions.add("listcommands");
            completions.add("listactions");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "deletecommand", "addaction", "removeaction", "listactions" ->
                    // Предлагаем уже созданные команды
                        completions.addAll(plugin.getCommandStorage().getStoredCommands());
                case "setflag", "checkflag" ->
                    // Предлагаем онлайн-игроков
                        completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                case "removeflag" ->
                    // Предлагаем флаги, уже добавленные в defaultFlags
                        completions.addAll(plugin.getFlagManager().getDefaultFlags());
                // addflag <flagName> — подсказывать нечего, пусть сам вводит
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "addaction" -> {
                    // Предлагаем тип действия: message, command, setflag, removeflag
                    completions.add("message");
                    completions.add("command");
                    completions.add("setflag");
                    completions.add("removeflag");
                }
                case "setflag" -> {
                    // Вводит <флаг>
                    completions.addAll(plugin.getFlagManager().getAllFlags());
                }
                case "checkflag" -> {
                    // Вводит <флаг>
                    completions.addAll(plugin.getFlagManager().getAllFlags());
                }
                case "removeaction" -> {
                    // Предлагаем индексы действующий у команды
                    String cmdName = args[1];
                    List<String> actions = plugin.getCommandStorage().getActions(cmdName);
                    for (int i = 0; i < actions.size(); i++) {
                        completions.add(String.valueOf(i));
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("setflag")) {
            // setflag <nick> <flag> <true|false>
            completions.add("true");
            completions.add("false");
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}