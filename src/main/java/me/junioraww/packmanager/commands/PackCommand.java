package me.junioraww.packmanager.commands;

import me.junioraww.packmanager.listeners.EventsListener;
import me.junioraww.packmanager.managers.PackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PackCommand implements CommandExecutor, TabCompleter {
  private final PackManager packManager;

  public PackCommand(PackManager packManager) {
    this.packManager = packManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      if (sender instanceof Player) {
        packManager.openMenu((Player) sender);
      } else {
        sender.sendMessage("Только для игроков.");
      }
      return true;
    }

    if (args[0].equalsIgnoreCase("list")) {
      sender.sendMessage(Component.text("Список паков:").color(NamedTextColor.GOLD));
      packManager.getPacks().values().forEach(p ->
              sender.sendMessage(Component.text("- " + p.name).color(NamedTextColor.WHITE)
                      .append(Component.text(p.isDefault ? " [Default]" : "").color(NamedTextColor.GRAY)))
      );
      return true;
    }

    if (!sender.hasPermission("customtweaks.admin")) {
      sender.sendMessage(Component.text("Нет прав.").color(NamedTextColor.RED));
      return true;
    }

    try {
      switch (args[0].toLowerCase()) {
        case "create":
          if (args.length < 3) return false;
          packManager.createPack(args[1], args[2]);
          sender.sendMessage(Component.text("Пак создан.").color(NamedTextColor.GREEN));
          break;
        case "delete":
          if (args.length < 2) return false;
          packManager.deletePack(args[1]);
          sender.sendMessage(Component.text("Пак удален.").color(NamedTextColor.GREEN));
          break;
        case "mirror":
          if (args.length < 3) return false;
          packManager.addMirror(args[1], args[2]);
          sender.sendMessage(Component.text("Зеркало добавлено.").color(NamedTextColor.GREEN));
          break;
        case "swap":
          if (args.length < 2) return false;
          boolean status = packManager.swapDefault(args[1]);
          sender.sendMessage(Component.text("Статус по умолчанию изменен: " + status).color(NamedTextColor.GREEN));
          break;
        case "update":
          if (args.length < 2) return false;
          boolean updated = packManager.updateCache(args[1]);
          sender.sendMessage(Component.text("Ответ сервера: " + updated).color(NamedTextColor.GREEN));
          break;
        case "icon":
          if (args.length < 3) {
            sender.sendMessage(Component.text("Использование: /pack icon <name> <material>").color(NamedTextColor.RED));
            return true;
          }
          Material mat = Material.matchMaterial(args[2]);
          if (mat == null) {
            sender.sendMessage(Component.text("Материал не найден.").color(NamedTextColor.RED));
            return true;
          }
          packManager.setPackIcon(args[1], mat);
          sender.sendMessage(Component.text("Иконка изменена.").color(NamedTextColor.GREEN));
          break;
        case "description":
          if (args.length < 3) return false;
          String desc = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
          packManager.setPackDescription(args[1], desc);
          sender.sendMessage(Component.text("Описание обновлено.").color(NamedTextColor.GREEN));
          break;
        default:
          sender.sendMessage(Component.text("Неизвестная подкоманда.").color(NamedTextColor.RED));
      }
    } catch (Exception e) {
      sender.sendMessage(Component.text("Ошибка: " + e.getMessage()).color(NamedTextColor.RED));
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
    List<String> completions = new ArrayList<>();
    List<String> commands = new ArrayList<>();

    if (args.length == 1) {
      commands.add("list");
      if (sender.hasPermission("packmanager.admin")) {
        commands.addAll(Arrays.asList("create", "delete", "mirror", "swap", "icon", "description", "update"));
      }
      StringUtil.copyPartialMatches(args[0], commands, completions);
    } else if (args.length == 2) {
      if (Arrays.asList("delete", "mirror", "swap", "icon", "description", "update").contains(args[0].toLowerCase())
              && sender.hasPermission("packmanager.admin")) {
        StringUtil.copyPartialMatches(args[1], packManager.getPacks().keySet(), completions);
      }
    } else if (args.length == 3) {
      if (args[0].equalsIgnoreCase("icon") && sender.hasPermission("packmanager.admin")) {
        List<String> materials = Arrays.stream(Material.values())
                .map(Material::name)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        StringUtil.copyPartialMatches(args[2], materials, completions);
      }
    }

    Collections.sort(completions);
    return completions;
  }
}