package de.zscoreboad.zscoreboad;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {

    private final SidebarManager sidebarManager;

    public PlayerListener(SidebarManager sidebarManager) {
        this.sidebarManager = sidebarManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.sidebarManager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.sidebarManager.handleQuit(event.getPlayer());
    }
}
