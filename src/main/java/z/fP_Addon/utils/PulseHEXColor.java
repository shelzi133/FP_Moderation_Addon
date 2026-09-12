package z.fP_Addon.utils;

import net.flectone.pulse.library.adventure.text.Component;
import net.flectone.pulse.library.adventure.text.format.NamedTextColor;
import net.flectone.pulse.library.adventure.text.format.TextColor;
import net.flectone.pulse.library.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//Класс нужен для обратбоки сообщений из FlecnePulse API
//Обычные методы не работают
public class PulseHEXColor {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)(&#[0-9A-F]{6}|&[0-9A-FK-OR])");

    public static Component colorize(String text) {
        if (text == null) return Component.empty();

        Component result = Component.empty();
        TextColor color = null;
        boolean[] deco = new boolean[5];

        Matcher matcher = TOKEN_PATTERN.matcher(text);
        int cursor = 0;

        while (matcher.find(cursor)) {
            if (matcher.start() > cursor) {
                result = result.append(segment(text.substring(cursor, matcher.start()), color, deco));
            }

            String token = matcher.group();

            if (token.length() == 8) {
                color = TextColor.fromHexString(token.substring(1));
                Arrays.fill(deco, false);
            } else {
                char code = Character.toLowerCase(token.charAt(1));
                switch (code) {
                    case '0': color = NamedTextColor.BLACK; Arrays.fill(deco, false); break;
                    case '1': color = NamedTextColor.DARK_BLUE; Arrays.fill(deco, false); break;
                    case '2': color = NamedTextColor.DARK_GREEN; Arrays.fill(deco, false); break;
                    case '3': color = NamedTextColor.DARK_AQUA; Arrays.fill(deco, false); break;
                    case '4': color = NamedTextColor.DARK_RED; Arrays.fill(deco, false); break;
                    case '5': color = NamedTextColor.DARK_PURPLE; Arrays.fill(deco, false); break;
                    case '6': color = NamedTextColor.GOLD; Arrays.fill(deco, false); break;
                    case '7': color = NamedTextColor.GRAY; Arrays.fill(deco, false); break;
                    case '8': color = NamedTextColor.DARK_GRAY; Arrays.fill(deco, false); break;
                    case '9': color = NamedTextColor.BLUE; Arrays.fill(deco, false); break;
                    case 'a': color = NamedTextColor.GREEN; Arrays.fill(deco, false); break;
                    case 'b': color = NamedTextColor.AQUA; Arrays.fill(deco, false); break;
                    case 'c': color = NamedTextColor.RED; Arrays.fill(deco, false); break;
                    case 'd': color = NamedTextColor.LIGHT_PURPLE; Arrays.fill(deco, false); break;
                    case 'e': color = NamedTextColor.YELLOW; Arrays.fill(deco, false); break;
                    case 'f': color = NamedTextColor.WHITE; Arrays.fill(deco, false); break;
                    case 'k': deco[4] = true; break;
                    case 'l': deco[0] = true; break;
                    case 'm': deco[3] = true; break;
                    case 'n': deco[2] = true; break;
                    case 'o': deco[1] = true; break;
                    case 'r': color = null; Arrays.fill(deco, false); break;
                    default: break;
                }
            }

            cursor = matcher.end();
        }

        if (cursor < text.length()) {
            result = result.append(segment(text.substring(cursor), color, deco));
        }

        return result.decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> colorizeList(List<String> lines) {
        List<Component> components = new ArrayList<>();
        for (String line : lines) {
            components.add(colorize(line));
        }
        return components;
    }

    public static String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)&#[0-9A-F]{6}", "").replaceAll("(?i)&[0-9A-FK-OR]", "");
    }

    private static Component segment(String content, TextColor color, boolean[] deco) {
        Component component = Component.text(content, color == null ? NamedTextColor.WHITE : color);
        component = component.decoration(TextDecoration.BOLD, deco[0]);
        component = component.decoration(TextDecoration.ITALIC, deco[1]);
        component = component.decoration(TextDecoration.UNDERLINED, deco[2]);
        component = component.decoration(TextDecoration.STRIKETHROUGH, deco[3]);
        component = component.decoration(TextDecoration.OBFUSCATED, deco[4]);
        return component;
    }
}
