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
        String message = event.getMessage();
        String[] split = message.substring(1).split(" ", 2);
        String commandLabel = split[0].toLowerCase();

        // Если это не наша кастомная команда — пропускаем
        if (commandLabel.equalsIgnoreCase("commandbuilder")) {
            return;
        }

        // Пытаемся взять список действий из commandStorage
        List<String> actions = commandStorage.getActions(commandLabel);
        if (actions.isEmpty()) {
            return; // не наша команда
        }

        // Отменяем стандартную механику выполнения
        event.setCancelled(true);

        // Запускаем «шаг за шагом» выполнение
        processActions(event.getPlayer(), actions, 0);
    }

    /**
     * Рекурсивно (или пошагово) обрабатывает список actions с позиции index.
     * Если встречает delay, откладывает продолжение через N секунд.
     * Если встречает checkflag, внутри него может быть своя «мини-цепочка» действий (включая delay).
     */
    private void processActions(Player player, List<String> actions, int index) {
        if (index >= actions.size()) {
            return; // Дошли до конца цепочки
        }

        String actionLine = actions.get(index);
        // Разбиваем по двоеточию, чтобы понять тип
        String[] parts = actionLine.split(":", 2);
        if (parts.length < 2) {
            // Неверный формат "тип:значение" — переходим к следующему
            processActions(player, actions, index + 1);
            return;
        }

        String type = parts[0].toLowerCase();
        // Подставляем %player%, если оно вдруг есть
        String rawValue = parts[1].replace("%player%", player.getName());

        switch (type) {
            case "message" -> {
                // Просто сообщение
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', rawValue));
                processActions(player, actions, index + 1);
            }

            case "command" -> {
                // Выполним от имени консоли
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rawValue);
                processActions(player, actions, index + 1);
            }

            case "setflag" -> {
                // Сейчас ставим флаг = true по умолчанию,
                // если не встретили =true/false. Можно оставить так или сделать парсинг, как ранее показывалось.
                getFlagManager().setFlag(player.getUniqueId(), rawValue, true);
                processActions(player, actions, index + 1);
            }

            case "removeflag" -> {
                getFlagManager().removeFlag(player.getUniqueId(), rawValue);
                processActions(player, actions, index + 1);
            }

            case "delay" -> {
                // Формат: "delay:5" -> ждем 5 секунд и продолжаем
                int delaySeconds;
                try {
                    delaySeconds = Integer.parseInt(rawValue);
                } catch (NumberFormatException e) {
                    // Неверное число — пропускаем
                    processActions(player, actions, index + 1);
                    return;
                }

                // Планируем продолжение цепочки через delaySeconds * 20 тиков
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    processActions(player, actions, index + 1);
                }, delaySeconds * 20L);
            }

            case "checkflag" -> {
                // Формат: "checkflag:flagName=bool <под-действия>"
                // Пример: "checkflag:talked_to_homeless=false delay:5 message:\"&7Привет, игрок!\""
                // Сначала отделим "talked_to_homeless=false" (условие) и всё остальное
                String[] splitted = rawValue.split(" ", 2);
                if (splitted.length < 2) {
                    // Нет второй части — ничего не делаем
                    processActions(player, actions, index + 1);
                    return;
                }

                String conditionPart = splitted[0];   // "talked_to_homeless=false"
                String subActionsRaw = splitted[1];   // "delay:5 message:\"&7Привет, игрок!\""

                // Парсим условие
                String[] condSplit = conditionPart.split("=", 2);
                if (condSplit.length < 2) {
                    processActions(player, actions, index + 1);
                    return;
                }
                String flagName = condSplit[0];
                boolean requiredValue = Boolean.parseBoolean(condSplit[1]);

                boolean actualValue = getFlagManager().hasFlag(player.getUniqueId(), flagName);
                if (actualValue == requiredValue) {
                    // Условие совпало — нужно выполнить subActionsRaw
                    // Разберём их через parseSubActions, где учитываем кавычки
                    List<String> subActions = parseSubActions(subActionsRaw);
                    if (!subActions.isEmpty()) {
                        processSubActions(player, subActions, 0, () -> {
                            processActions(player, actions, index + 1);
                        });
                        return;
                    }
                }
                // Если условие НЕ совпало, или subActions пуст — просто идём дальше
                processActions(player, actions, index + 1);
            }

            default -> {
                // неизвестный тип
                processActions(player, actions, index + 1);
            }
        }
    }

    /**
     * Новый метод, который умеет распознавать кавычки.
     * Если находим "слово в кавычках", то берём всё как одно целое, игнорируя пробелы внутри кавычек.
     *
     * Пример входа: "delay:5 message:\"&7Привет, игрок!\" command:/say_тест"
     * Мы получим токены:
     *  1) delay:5
     *  2) message:"&7Привет, игрок!"
     *  3) command:/say_тест
     */
    private List<String> parseSubActions(String input) {
        List<String> result = new ArrayList<>();
        int pos = 0;
        while (pos < input.length()) {
            // Пропускаем пробелы
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
            if (pos >= input.length()) break;

            // Если символ — кавычка ", собираем всё до следующей
            if (input.charAt(pos) == '"') {
                // начинаем собирать
                int startPos = ++pos; // пропускаем первую кавычку
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (pos < input.length()) {
                    if (input.charAt(pos) == '"') {
                        closed = true;
                        pos++; // пропускаем закрывающую кавычку
                        break;
                    }
                    sb.append(input.charAt(pos));
                    pos++;
                }
                // Если не нашли вторую кавычку, sb так и будет содержать весь оставшийся текст
                // Добавляем sb в result
                result.add(sb.toString());
            } else {
                // Если не кавычка, то токен идёт до пробела
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
     * Аналог processActions, но для "под-действий" внутри checkflag.
     * Когда под-действия закончатся, вызовем callback.
     */
    private void processSubActions(Player player, List<String> actions, int index, Runnable callback) {
        if (index >= actions.size()) {
            // Всё выполнили — вызываем callback
            callback.run();
            return;
        }

        String actionLine = actions.get(index);

        // Разбиваем по двоеточию (тип:значение)
        String[] parts = actionLine.split(":", 2);
        if (parts.length < 2) {
            // Формат не "тип:значение" — пропускаем
            processSubActions(player, actions, index + 1, callback);
            return;
        }

        String type = parts[0].toLowerCase();
        String rawValue = parts[1].replace("%player%", player.getName());

        // Если rawValue обёрнут в кавычки, снимем их:
        // message:"&7Привет" => rawValue = "\"&7Привет\""
        // Посмотрим, начинается ли и заканчивается ли на кавычку
        if (rawValue.startsWith("\"") && rawValue.endsWith("\"") && rawValue.length() >= 2) {
            // убираем первую и последнюю кавычку
            rawValue = rawValue.substring(1, rawValue.length() - 1);
        }

        switch (type) {
            case "delay" -> {
                int delaySeconds;
                try {
                    delaySeconds = Integer.parseInt(rawValue);
                } catch (NumberFormatException e) {
                    processSubActions(player, actions, index + 1, callback);
                    return;
                }
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    processSubActions(player, actions, index + 1, callback);
                }, delaySeconds * 20L);
            }
            case "message" -> {
                // Теперь rawValue может содержать пробелы
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', rawValue));
                processSubActions(player, actions, index + 1, callback);
            }
            case "command" -> {
                // Выполняем от имени консоли
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rawValue);
                processSubActions(player, actions, index + 1, callback);
            }
            case "setflag" -> {
                // Если хотите, сделайте поддержку "=true/false", но оставим как в вашем коде
                getFlagManager().setFlag(player.getUniqueId(), rawValue, true);
                processSubActions(player, actions, index + 1, callback);
            }
            case "removeflag" -> {
                getFlagManager().removeFlag(player.getUniqueId(), rawValue);
                processSubActions(player, actions, index + 1, callback);
            }
            default -> {
                // неизвестный тип
                processSubActions(player, actions, index + 1, callback);
            }
        }
    }

    public void reloadPlugin() {
        getLogger().info("Перезагрузка плагина CommandBuilder...");

        commandStorage.loadCommands();
        flagManager.loadFlags();

        getLogger().info("Плагин CommandBuilder успешно перезагружен!");
    }
}
