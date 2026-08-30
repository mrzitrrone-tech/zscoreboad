package de.zscoreboad.zscoreboad;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PacketSidebarAdapter {

    private static final String[] ENTRIES = {
        "\u00A70", "\u00A71", "\u00A72", "\u00A73", "\u00A74", "\u00A75", "\u00A76", "\u00A77",
        "\u00A78", "\u00A79", "\u00A7a", "\u00A7b", "\u00A7c", "\u00A7d", "\u00A7e", "\u00A7f"
    };

    private final JavaPlugin plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, SidebarState> activeSidebars = new HashMap<>();

    private boolean available;
    private Method asVanillaMethod;
    private Constructor<?> scoreboardConstructor;
    private Object objectiveCriteriaDummy;
    private Object renderTypeInteger;
    private Constructor<?> objectivePacketConstructor;
    private Constructor<?> displayObjectivePacketConstructor;
    private Constructor<?> setScorePacketConstructor;
    private Constructor<?> playerTeamConstructor;
    private Object blankNumberFormat;
    private Object emptyScoreDisplay;
    private Method addObjectiveMethod;
    private Method addPlayerToTeamMethod;
    private Method setPlayerPrefixMethod;
    private Method setPlayerSuffixMethod;
    private Method createTeamPacketMethod;
    private Method createRemoveTeamPacketMethod;
    private Method getHandleMethod;
    private Field connectionField;
    private Method sendPacketMethod;
    private Class<?> packetClass;

    public PacketSidebarAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.available = bootstrap();
    }

    public boolean isAvailable() {
        return this.available;
    }

    public void update(Player player, String objectiveName, Component title, List<String> configuredLines) {
        if (!this.available || !player.isOnline()) {
            return;
        }

        try {
            clear(player, objectiveName);

            Object scoreboard = this.scoreboardConstructor.newInstance();
            Object objective = this.addObjectiveMethod.invoke(
                scoreboard,
                objectiveName,
                this.objectiveCriteriaDummy,
                asVanilla(title),
                this.renderTypeInteger,
                false,
                this.blankNumberFormat
            );

            sendPacket(player, this.objectivePacketConstructor.newInstance(objective, 0));
            sendPacket(player, this.displayObjectivePacketConstructor.newInstance(getSidebarDisplaySlot(), objective));

            int maxLines = Math.min(configuredLines.size(), ENTRIES.length);
            List<Object> teams = new ArrayList<>(maxLines);
            for (int index = 0; index < maxLines; index++) {
                String coloredLine = ColorUtil.colorize(PlaceholderUtil.apply(player, configuredLines.get(index)));
                LineParts parts = splitLine(coloredLine);
                Object team = this.playerTeamConstructor.newInstance(scoreboard, "line_" + index);
                String entry = ENTRIES[index];

                this.setPlayerPrefixMethod.invoke(team, asVanilla(this.legacySerializer.deserialize(parts.prefix())));
                this.setPlayerSuffixMethod.invoke(team, asVanilla(this.legacySerializer.deserialize(parts.suffix())));
                this.addPlayerToTeamMethod.invoke(scoreboard, entry, team);

                sendPacket(player, this.createTeamPacketMethod.invoke(null, team, true));
                sendPacket(
                    player,
                    this.setScorePacketConstructor.newInstance(
                        entry,
                        objectiveName,
                        0,
                        Optional.of(this.emptyScoreDisplay),
                        Optional.of(this.blankNumberFormat)
                    )
                );
                teams.add(team);
            }

            this.activeSidebars.put(player.getUniqueId(), new SidebarState(objective, teams));
        } catch (ReflectiveOperationException exception) {
            this.available = false;
            this.plugin.getLogger().severe("Failed to update packet sidebar: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    public void clear(Player player, String objectiveName) {
        if (!this.available || !player.isOnline()) {
            return;
        }

        SidebarState state = this.activeSidebars.remove(player.getUniqueId());
        if (state == null) {
            return;
        }

        try {
            for (Object team : state.teams()) {
                sendPacket(player, this.createRemoveTeamPacketMethod.invoke(null, team));
            }
            sendPacket(player, this.objectivePacketConstructor.newInstance(state.objective(), 1));
        } catch (ReflectiveOperationException exception) {
            this.plugin.getLogger().warning("Failed to clear packet sidebar for " + player.getName() + ": " + exception.getMessage());
        }
    }

    private boolean bootstrap() {
        try {
            Class<?> paperAdventureClass = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            this.asVanillaMethod = paperAdventureClass.getMethod("asVanilla", Component.class);

            Class<?> scoreboardClass = Class.forName("net.minecraft.world.scores.Scoreboard");
            Class<?> objectiveClass = Class.forName("net.minecraft.world.scores.Objective");
            Class<?> objectiveCriteriaClass = Class.forName("net.minecraft.world.scores.criteria.ObjectiveCriteria");
            Class<?> renderTypeClass = Class.forName("net.minecraft.world.scores.criteria.ObjectiveCriteria$RenderType");
            Class<?> numberFormatClass = Class.forName("net.minecraft.network.chat.numbers.NumberFormat");
            Class<?> blankFormatClass = Class.forName("net.minecraft.network.chat.numbers.BlankFormat");
            Class<?> vanillaComponentClass = Class.forName("net.minecraft.network.chat.Component");
            Class<?> displaySlotClass = Class.forName("net.minecraft.world.scores.DisplaySlot");
            Class<?> playerTeamClass = Class.forName("net.minecraft.world.scores.PlayerTeam");
            Class<?> setObjectivePacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetObjectivePacket");
            Class<?> setDisplayObjectivePacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket");
            Class<?> setScorePacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetScorePacket");
            Class<?> setPlayerTeamPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket");
            this.packetClass = Class.forName("net.minecraft.network.protocol.Packet");

            this.scoreboardConstructor = scoreboardClass.getConstructor();
            this.objectiveCriteriaDummy = objectiveCriteriaClass.getField("DUMMY").get(null);
            this.renderTypeInteger = Enum.valueOf((Class<Enum>) renderTypeClass.asSubclass(Enum.class), "INTEGER");
            this.blankNumberFormat = blankFormatClass.getField("INSTANCE").get(null);
            this.emptyScoreDisplay = asVanilla(Component.empty());
            this.addObjectiveMethod = scoreboardClass.getMethod(
                "addObjective",
                String.class,
                objectiveCriteriaClass,
                vanillaComponentClass,
                renderTypeClass,
                boolean.class,
                numberFormatClass
            );
            this.playerTeamConstructor = playerTeamClass.getConstructor(scoreboardClass, String.class);
            this.addPlayerToTeamMethod = scoreboardClass.getMethod("addPlayerToTeam", String.class, playerTeamClass);
            this.setPlayerPrefixMethod = playerTeamClass.getMethod("setPlayerPrefix", vanillaComponentClass);
            this.setPlayerSuffixMethod = playerTeamClass.getMethod("setPlayerSuffix", vanillaComponentClass);
            this.objectivePacketConstructor = setObjectivePacketClass.getConstructor(objectiveClass, int.class);
            this.displayObjectivePacketConstructor = setDisplayObjectivePacketClass.getConstructor(displaySlotClass, objectiveClass);
            this.setScorePacketConstructor = setScorePacketClass.getConstructor(String.class, String.class, int.class, Optional.class, Optional.class);
            this.createTeamPacketMethod = setPlayerTeamPacketClass.getMethod("createAddOrModifyPacket", playerTeamClass, boolean.class);
            this.createRemoveTeamPacketMethod = setPlayerTeamPacketClass.getMethod("createRemovePacket", playerTeamClass);

            this.getHandleMethod = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer").getMethod("getHandle");
            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            this.connectionField = serverPlayerClass.getField("connection");
            this.sendPacketMethod = this.connectionField.getType().getMethod("send", this.packetClass);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException exception) {
            this.plugin.getLogger().severe("Packet sidebar bootstrap failed: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    private Object asVanilla(Component component) throws InvocationTargetException, IllegalAccessException {
        return this.asVanillaMethod.invoke(null, component);
    }

    private Object getSidebarDisplaySlot() throws ClassNotFoundException {
        Class<?> displaySlotClass = Class.forName("net.minecraft.world.scores.DisplaySlot");
        return Enum.valueOf((Class<Enum>) displaySlotClass.asSubclass(Enum.class), "SIDEBAR");
    }

    private void sendPacket(Player player, Object packet) throws ReflectiveOperationException {
        Object serverPlayer = this.getHandleMethod.invoke(player);
        Object connection = this.connectionField.get(serverPlayer);
        this.sendPacketMethod.invoke(connection, packet);
    }

    private LineParts splitLine(String line) {
        if (line.length() <= 64) {
            return new LineParts(line, "");
        }

        String prefix = line.substring(0, 64);
        String suffix = ChatColor.getLastColors(prefix) + line.substring(64, Math.min(line.length(), 128));
        return new LineParts(prefix, suffix);
    }

    private record LineParts(String prefix, String suffix) {
    }

    private record SidebarState(Object objective, List<Object> teams) {
    }
}
