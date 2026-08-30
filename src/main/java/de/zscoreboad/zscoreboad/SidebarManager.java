package de.zscoreboad.zscoreboad;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class SidebarManager {

    private static final String OBJECTIVE_NAME = "zscoreboad";
    private static final String HIDDEN_PLAYERS_PATH = "hidden-players";

    private final JavaPlugin plugin;
    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private final Map<UUID, ScheduledTask> foliaTasks = new HashMap<>();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private final PacketSidebarAdapter packetSidebarAdapter;
    private BukkitTask paperTask;

    public SidebarManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.packetSidebarAdapter = new PacketSidebarAdapter(plugin);
        loadHiddenPlayers();
    }

    public void start() {
        if (!isSupported()) {
            return;
        }

        if (isFolia()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                scheduleFoliaTask(player);
            }
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            show(player);
        }

        this.paperTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::updateAll, 20L, 20L);
    }

    public void stop() {
        if (this.paperTask != null) {
            this.paperTask.cancel();
            this.paperTask = null;
        }

        for (ScheduledTask task : this.foliaTasks.values()) {
            task.cancel();
        }
        this.foliaTasks.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            this.packetSidebarAdapter.clear(player, OBJECTIVE_NAME);
        }
    }

    public void reload() {
        if (!isSupported()) {
            return;
        }

        if (isFolia()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!this.hiddenPlayers.contains(player.getUniqueId())) {
                    runFoliaUpdate(player);
                }
            }
            return;
        }

        updateAll();
    }

    public void handleJoin(Player player) {
        if (!isSupported() || this.hiddenPlayers.contains(player.getUniqueId())) {
            return;
        }

        if (isFolia()) {
            scheduleFoliaTask(player);
            return;
        }

        show(player);
    }

    public void handleQuit(Player player) {
        ScheduledTask task = this.foliaTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        this.packetSidebarAdapter.clear(player, OBJECTIVE_NAME);
    }

    public boolean toggle(Player player) {
        if (!isSupported()) {
            return false;
        }

        UUID uniqueId = player.getUniqueId();
        if (this.hiddenPlayers.contains(uniqueId)) {
            this.hiddenPlayers.remove(uniqueId);
            saveHiddenPlayers();
            if (isFolia()) {
                scheduleFoliaTask(player);
            } else {
                show(player);
            }
            return true;
        }

        this.hiddenPlayers.add(uniqueId);
        saveHiddenPlayers();
        ScheduledTask task = this.foliaTasks.remove(uniqueId);
        if (task != null) {
            task.cancel();
        }

        this.packetSidebarAdapter.clear(player, OBJECTIVE_NAME);
        return false;
    }

    public boolean isSupported() {
        return this.packetSidebarAdapter.isAvailable();
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!this.hiddenPlayers.contains(player.getUniqueId())) {
                show(player);
            }
        }
    }

    private void scheduleFoliaTask(Player player) {
        UUID uniqueId = player.getUniqueId();
        ScheduledTask oldTask = this.foliaTasks.remove(uniqueId);
        if (oldTask != null) {
            oldTask.cancel();
        }

        player.getScheduler().run(this.plugin, task -> show(player), null);
        ScheduledTask scheduledTask = player.getScheduler().runAtFixedRate(this.plugin, task -> show(player), null, 20L, 20L);
        this.foliaTasks.put(uniqueId, scheduledTask);
    }

    private void runFoliaUpdate(Player player) {
        player.getScheduler().run(this.plugin, task -> show(player), null);
    }

    private boolean isFolia() {
        return Bukkit.getServer().getName().equalsIgnoreCase("Folia");
    }

    private void show(Player player) {
        FileConfiguration config = this.plugin.getConfig();
        String title = ColorUtil.colorize(PlaceholderUtil.apply(player, config.getString("title", "&bScoreboard")));
        List<String> configuredLines = config.getStringList("lines");
        Component titleComponent = this.legacySerializer.deserialize(title);

        this.packetSidebarAdapter.update(player, OBJECTIVE_NAME, titleComponent, configuredLines);
    }

    private void loadHiddenPlayers() {
        for (String value : this.plugin.getConfig().getStringList(HIDDEN_PLAYERS_PATH)) {
            try {
                this.hiddenPlayers.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                this.plugin.getLogger().warning("Ignoring invalid hidden player UUID in config: " + value);
            }
        }
    }

    private void saveHiddenPlayers() {
        List<String> serialized = new ArrayList<>(this.hiddenPlayers.size());
        for (UUID uniqueId : this.hiddenPlayers) {
            serialized.add(uniqueId.toString());
        }

        this.plugin.getConfig().set(HIDDEN_PLAYERS_PATH, serialized);
        this.plugin.saveConfig();
    }
}
