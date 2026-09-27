package ru.hideworld.buildroulette;

import org.bukkit.plugin.java.JavaPlugin;

public final class BuildRoulettePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new RouletteListener(this), this);
        getLogger().info("BuildRoulette enabled.");
    }
}
