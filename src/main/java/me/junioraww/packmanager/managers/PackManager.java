package me.junioraww.packmanager.managers;

import me.junioraww.packmanager.Main;
import me.junioraww.packmanager.utils.ResourcePackUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class PackManager {
  private final Main plugin;
  private final Map<String, PackData> loadedPacks = new LinkedHashMap<>();
  public static final String MENU_TITLE = "Настройки ресурспаков";

  public PackManager(Main plugin) {
    this.plugin = plugin;
  }

  public static class PackData {
    public String name;
    List<String> urls;
    public boolean isDefault;
    byte[] hash;
    UUID uuid;
    Material icon;
    String description;

    public PackData(String name, List<String> urls, boolean isDefault, Material icon, String description) {
      this.name = name;
      this.urls = urls;
      this.isDefault = isDefault;
      this.icon = icon;
      this.description = description;
    }
  }

  public void loadPacks() {
    loadedPacks.clear();
    ConfigurationSection section = plugin.getPacksConfig().getConfigurationSection("packs");
    if (section == null) return;

    for (String key : section.getKeys(false)) {
      List<String> urls = section.getStringList(key + ".urls");
      if (urls.isEmpty()) continue;

      boolean isDef = section.getBoolean(key + ".default", false);
      String matName = section.getString(key + ".material", "PAPER");
      Material mat = Material.getMaterial(matName);
      if (mat == null) mat = Material.PAPER;

      String desc = section.getString(key + ".description", "Без описания");

      PackData pack = new PackData(key, urls, isDef, mat, desc);

      try {
        String url = urls.getFirst();
        File cacheDir = new File(plugin.getDataFolder(), "cache");
        File tempFile = new File(cacheDir, key + ".zip");

        if (!tempFile.exists()) {
          plugin.getLogger().info("Скачивание " + key + "...");
          ResourcePackUtil.download(url, tempFile);
        }

        pack.hash = ResourcePackUtil.sha1(tempFile);
        pack.uuid = ResourcePackUtil.uuidFromHash(pack.hash);

        plugin.getLogger().warning("Hash " + Arrays.toString(pack.hash));

        loadedPacks.put(key, pack);
      } catch (Exception e) {
        plugin.getLogger().warning("Ошибка загрузки пака " + key + ": " + e.getMessage());
      }
    }
  }

  public Map<String, PackData> getPacks() {
    return loadedPacks;
  }

  public List<PackData> getPlayerActivePacks(Player player) {
    List<PackData> active = new ArrayList<>();
    List<String> disabled = plugin.getPlayersConfig().getStringList(player.getUniqueId() + ".disabled");
    List<String> enabled = plugin.getPlayersConfig().getStringList(player.getUniqueId() + ".enabled");

    for (PackData pack : loadedPacks.values()) {
      boolean isActive = pack.isDefault;
      if (pack.isDefault && disabled.contains(pack.name)) isActive = false;
      if (!pack.isDefault && enabled.contains(pack.name)) isActive = true;

      if (isActive) active.add(pack);
    }
    return active;
  }

  public void openMenu(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, Component.text(MENU_TITLE));

    Set<String> activeNames = getPlayerActivePacks(player).stream()
            .map(p -> p.name).collect(Collectors.toSet());

    for (PackData pack : loadedPacks.values()) {
      ItemStack item = new ItemStack(pack.icon);
      ItemMeta meta = item.getItemMeta();

      boolean isActive = activeNames.contains(pack.name);

      Component nameComp = Component.text(pack.name)
              .color(isActive ? NamedTextColor.GREEN : NamedTextColor.RED)
              .decoration(TextDecoration.ITALIC, false);

      meta.displayName(nameComp);

      List<Component> lore = new ArrayList<>();
      lore.add(Component.text(pack.description).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.empty());
      lore.add(Component.text("Статус: ").color(NamedTextColor.GRAY)
              .append(isActive
                      ? Component.text("включено").color(NamedTextColor.GREEN)
                      : Component.text("выключено").color(NamedTextColor.RED))
              .decoration(TextDecoration.ITALIC, false));
      lore.add(Component.text("По умолчанию: " + (pack.isDefault ? "да" : "нет"))
              .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      meta.lore(lore);

      if (isActive) {
        meta.addEnchant(Enchantment.POWER, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      }

      item.setItemMeta(meta);
      inv.addItem(item);
    }
    player.openInventory(inv);
  }

  public void sendPacksToPlayer(Player player) {
    player.clearResourcePacks();
    for (PackData pack : getPlayerActivePacks(player)) {
      if (!pack.urls.isEmpty()) {
        player.addResourcePack(
                pack.uuid,
                pack.urls.getFirst(),
                pack.hash,
                LegacyComponentSerializer.legacySection().serialize(
                        Component.text("Вы сможете настроить ресурспаки командой /pack")
                ),
                false
        );
      }
    }
  }

  public void savePlayerSelection(Player player, Set<String> newActivePacks) {
    List<String> disabled = new ArrayList<>();
    List<String> enabled = new ArrayList<>();

    for (PackData pack : loadedPacks.values()) {
      if (pack.isDefault && !newActivePacks.contains(pack.name)) {
        disabled.add(pack.name);
      } else if (!pack.isDefault && newActivePacks.contains(pack.name)) {
        enabled.add(pack.name);
      }
    }

    plugin.getPlayersConfig().set(player.getUniqueId() + ".disabled", disabled);
    plugin.getPlayersConfig().set(player.getUniqueId() + ".enabled", enabled);
    plugin.getPlayersConfig().save();
  }

  public void setPackIcon(String name, Material material) {
    if (!loadedPacks.containsKey(name)) return;
    plugin.getPacksConfig().set("packs." + name + ".material", material.name());
    plugin.getPacksConfig().save();
    loadPacks();
  }

  public void setPackDescription(String name, String description) {
    if (!loadedPacks.containsKey(name)) return;
    plugin.getPacksConfig().set("packs." + name + ".description", description);
    plugin.getPacksConfig().save();
    loadPacks();
  }

  public void createPack(String name, String url) {
    plugin.getPacksConfig().set("packs." + name + ".urls", Collections.singletonList(url));
    plugin.getPacksConfig().set("packs." + name + ".default", false);
    plugin.getPacksConfig().save();
    loadPacks();
  }
  public void deletePack(String name) {
    plugin.getPacksConfig().set("packs." + name, null);
    plugin.getPacksConfig().save();
    loadPacks();
  }
  public void addMirror(String name, String url) {
    List<String> urls = plugin.getPacksConfig().getStringList("packs." + name + ".urls");
    urls.add(url);
    plugin.getPacksConfig().set("packs." + name + ".urls", urls);
    plugin.getPacksConfig().save();
    loadPacks();
  }
  public boolean swapDefault(String name) {
    boolean current = plugin.getPacksConfig().getBoolean("packs." + name + ".default");
    plugin.getPacksConfig().set("packs." + name + ".default", !current);
    plugin.getPacksConfig().save();
    loadPacks();
    return !current;
  }
  public boolean updateCache(String name) {
    var pack = loadedPacks.get(name);
    if (pack == null) return false;
    try {
      File cacheDir = new File(plugin.getDataFolder(), "cache");
      File tempFile = new File(cacheDir, pack.name + ".zip");

      plugin.getLogger().info("Скачивание " + pack.name + "...");
      ResourcePackUtil.download(pack.urls.getFirst(), tempFile);

      pack.hash = ResourcePackUtil.sha1(tempFile);
      pack.uuid = ResourcePackUtil.uuidFromHash(pack.hash);

      return true;
    } catch (Exception e) {
      plugin.getLogger().warning("Ошибка загрузки пака " + pack.name + ": " + e.getMessage());
    }

    return false;
  }
}