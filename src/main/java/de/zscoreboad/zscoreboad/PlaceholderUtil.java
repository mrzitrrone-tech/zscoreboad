package de.zscoreboad.zscoreboad;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderUtil {

    private PlaceholderUtil() {
    }

    public static String apply(Player player, String text) {
        if (text == null) {
            return "";
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text;
        }

        try {
            Class<?> placeholderApiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = placeholderApiClass.getMethod("setPlaceholders", Player.class, String.class);
            return (String) method.invoke(null, player, text);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return text;
        }
    }
}
