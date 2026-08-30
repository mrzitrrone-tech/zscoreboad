package de.zscoreboad.zscoreboad;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Zscoreboad extends JavaPlugin {

    private SidebarManager sidebarManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.sidebarManager = new SidebarManager(this);
        this.sidebarManager.start();

        PluginCommand command = getCommand("scoreboad");
        if (command == null) {
            throw new IllegalStateException("Command 'scoreboad' is missing from plugin.yml");
        }

        ScoreboardCommand scoreboardCommand = new ScoreboardCommand(this, this.sidebarManager);
        command.setExecutor(scoreboardCommand);
        command.setTabCompleter(scoreboardCommand);

        getServer().getPluginManager().registerEvents(new PlayerListener(this.sidebarManager), this);
    }

    @Override
    public void onDisable() {
        if (this.sidebarManager != null) {
            this.sidebarManager.stop();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        this.sidebarManager.reload();
    }
}
