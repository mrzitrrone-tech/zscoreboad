package de.zscoreboad.zscoreboad;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ScoreboardCommand implements CommandExecutor, TabCompleter {

    private static final String RELOAD_PERMISSION = "zscoreboad.reload";

    private final Zscoreboad plugin;
    private final SidebarManager sidebarManager;

    public ScoreboardCommand(Zscoreboad plugin, SidebarManager sidebarManager) {
        this.plugin = plugin;
        this.sidebarManager = sidebarManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("realod"))) {
            if (!sender.hasPermission(RELOAD_PERMISSION)) {
                sender.sendMessage(ColorUtil.colorize("&cDafuer hast du keine Rechte."));
                return true;
            }

            this.plugin.reloadPlugin();
            sender.sendMessage(ColorUtil.colorize("&aDie Scoreboard-Config wurde neu geladen."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.colorize("&cNur Spieler koennen das Scoreboard toggeln."));
            return true;
        }

        if (!this.sidebarManager.isSupported()) {
            player.sendMessage(ColorUtil.colorize("&cDas Sidebar-Scoreboard ist auf diesem Server aktuell nicht verfuegbar."));
            return true;
        }

        boolean enabled = this.sidebarManager.toggle(player);
        if (enabled) {
            player.sendMessage(ColorUtil.colorize("&aScoreboard aktiviert."));
        } else {
            player.sendMessage(ColorUtil.colorize("&cScoreboard deaktiviert."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission(RELOAD_PERMISSION)) {
            String input = args[0].toLowerCase();
            if ("reload".startsWith(input)) {
                suggestions.add("reload");
            }
            if ("realod".startsWith(input)) {
                suggestions.add("realod");
            }
        }
        return suggestions;
    }
}
