package z.fP_Addon.utils;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HEXColor {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static Component colorize(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacySection()
                .deserialize(translateHex(text))
                .decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> colorizeList(List<String> lines) {
        return lines.stream()
                .map(HEXColor::colorize)
                .collect(Collectors.toList());
    }

    public static String translateHex(String text) {
        if (text == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return sb.toString().replace('&', '§');
    }

    public static String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)§x(§[0-9A-Fa-f]){6}", "")
                .replaceAll("§[0-9A-Fa-fk-orK-OR]", "")
                .replaceAll("&#[A-Fa-f0-9]{6}", "")
                .replaceAll("&[0-9A-Fa-fk-orK-OR]", "");
    }
}
