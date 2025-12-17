package me.junioraww.packmanager.utils;


import me.junioraww.packmanager.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class Config extends YamlConfiguration {
  private File settingsFile;

  public boolean save() {
    try {
      this.save(settingsFile);
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  @Override
  public void load(@NotNull String filename) {
    Main plugin = Main.getPlugin();
    settingsFile = new File(plugin.getDataPath().toFile(), filename);
    try {
      this.load(settingsFile);
    } catch (Exception e) {
      plugin.getLogger().warning(e.getMessage());
    }
  }
}
