package de.zscoreboad.zscoreboad;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern ANGLE_HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>(.*?)</#([A-Fa-f0-9]{6})>");

    private ColorUtil() {
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }

        String processed = applyGradients(text);
        processed = processed.replaceAll("</#[A-Fa-f0-9]{6}>", "");

        Matcher angleMatcher = ANGLE_HEX_PATTERN.matcher(processed);
        StringBuilder angleBuilder = new StringBuilder();

        while (angleMatcher.find()) {
            String color = angleMatcher.group(1);
            angleMatcher.appendReplacement(angleBuilder, Matcher.quoteReplacement(ChatColor.of("#" + color).toString()));
        }

        angleMatcher.appendTail(angleBuilder);

        Matcher matcher = HEX_PATTERN.matcher(angleBuilder.toString());
        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            String color = matcher.group(1);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(ChatColor.of("#" + color).toString()));
        }

        matcher.appendTail(builder);
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

    private static String applyGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            String replacement = createGradient(matcher.group(1), matcher.group(2), matcher.group(3));
            matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String createGradient(String startHex, String content, String endHex) {
        String translated = ChatColor.translateAlternateColorCodes('&', content);
        String plain = ChatColor.stripColor(translated);
        if (plain == null || plain.isEmpty()) {
            return translated;
        }

        Color start = Color.decode("#" + startHex);
        Color end = Color.decode("#" + endHex);
        StringBuilder builder = new StringBuilder();
        String activeFormatting = "";
        int visibleIndex = 0;
        int visibleLength = plain.length();

        for (int index = 0; index < translated.length(); index++) {
            char current = translated.charAt(index);
            if (current == ChatColor.COLOR_CHAR && index + 1 < translated.length()) {
                char code = translated.charAt(index + 1);
                String formatCode = String.valueOf(ChatColor.COLOR_CHAR) + code;
                if (isFormattingCode(code)) {
                    activeFormatting += formatCode;
                } else {
                    activeFormatting = "";
                }
                index++;
                continue;
            }

            float ratio = visibleLength == 1 ? 0.0F : (float) visibleIndex / (visibleLength - 1);
            int red = interpolate(start.getRed(), end.getRed(), ratio);
            int green = interpolate(start.getGreen(), end.getGreen(), ratio);
            int blue = interpolate(start.getBlue(), end.getBlue(), ratio);

            builder.append(ChatColor.of(new Color(red, green, blue)));
            builder.append(activeFormatting);
            builder.append(current);
            visibleIndex++;
        }

        return builder.toString();
    }

    private static int interpolate(int start, int end, float ratio) {
        return Math.round(start + (end - start) * ratio);
    }

    private static boolean isFormattingCode(char code) {
        char lower = Character.toLowerCase(code);
        return lower == 'k' || lower == 'l' || lower == 'm' || lower == 'n' || lower == 'o';
    }
}
