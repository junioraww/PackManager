package me.junioraww.packmanager.listeners;

import me.junioraww.packmanager.Main;
import me.junioraww.packmanager.managers.PackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EventsListener implements Listener {
  private final PackManager packManager;

  public EventsListener(PackManager packManager) {
    this.packManager = packManager;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Bukkit.getScheduler().runTaskLater(me.junioraww.packmanager.Main.getPlugin(), () -> {
      packManager.sendPacksToPlayer(event.getPlayer());
    }, 20L);
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent e) {
    if (!e.getView().title().equals(Component.text(PackManager.MENU_TITLE))) return;
    e.setCancelled(true);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent e) {
    if (!e.getView().title().equals(Component.text(PackManager.MENU_TITLE))) return;
    e.setCancelled(true);

    ItemStack item = e.getCurrentItem();
    if (item == null || item.getType() == Material.AIR) return;

    ItemMeta meta = item.getItemMeta();
    boolean isEnchanted = meta.hasEnchants();

    String rawName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());

    if (isEnchanted) {
      meta.removeEnchant(Enchantment.POWER);
      meta.displayName(Component.text(rawName).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
      updateLoreStatus(meta, false);
    } else {
      meta.addEnchant(Enchantment.POWER, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      meta.displayName(Component.text(rawName).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
      updateLoreStatus(meta, true);
    }
    item.setItemMeta(meta);
  }

  private void updateLoreStatus(ItemMeta meta, boolean active) {
    List<Component> currentLore = meta.lore();
    if (currentLore != null && currentLore.size() > 2) {
      currentLore.set(currentLore.size() - 2,
              Component.text("Статус: ").color(NamedTextColor.GRAY)
                      .append(active
                              ? Component.text("включено").color(NamedTextColor.GREEN)
                              : Component.text("выключено").color(NamedTextColor.RED))
                      .decoration(TextDecoration.ITALIC, false));
      meta.lore(currentLore);
    }
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (!e.getView().title().equals(Component.text(PackManager.MENU_TITLE))) return;

    Player player = (Player) e.getPlayer();

    Set<String> selectedPacks = new HashSet<>();
    for (ItemStack item : e.getInventory().getContents()) {
      if (item != null && item.getType() != Material.AIR) {
        if (item.getItemMeta().hasEnchants()) {
          String name = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
          selectedPacks.add(name);
        }
      }
    }

    Set<String> currentPacks = packManager.getPlayerActivePacks(player).stream()
            .map(p -> p.name).collect(Collectors.toSet());

    if (selectedPacks.equals(currentPacks)) {
      return;
    }

    packManager.savePlayerSelection(player, selectedPacks);
    player.sendRichMessage("<yellow>Настройки сохранены");
    packManager.sendPacksToPlayer(player);
  }
}