package me.junioraww.packmanager;

import me.junioraww.packmanager.commands.PackCommand;
import me.junioraww.packmanager.listeners.EventsListener;
import me.junioraww.packmanager.managers.PackManager;
import me.junioraww.packmanager.utils.Config;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
  private static Main instance;
  private PackManager packManager;
  private Config packsConfig;
  private Config playersConfig;

  @Override
  public void onEnable() {
    instance = this;

    packsConfig = new Config();
    packsConfig.load("packs.yml");

    playersConfig = new Config();
    playersConfig.load("players.yml");

    packManager = new PackManager(this);
    getLogger().info("Загрузка паков...");
    packManager.loadPacks();

    PackCommand cmdExecutor = new PackCommand(packManager);
    getCommand("pack").setExecutor(cmdExecutor);
    getCommand("pack").setTabCompleter(cmdExecutor);

    getServer().getPluginManager().registerEvents(new EventsListener(packManager), this);
  }

  @Override
  public void onDisable() {
    if (playersConfig != null) playersConfig.save();
    if (packsConfig != null) packsConfig.save();
  }

  public static Main getPlugin() { return instance; }
  public Config getPacksConfig() { return packsConfig; }
  public Config getPlayersConfig() { return playersConfig; }
}