package dev.smto.book2map.content;

import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import net.minecraft.ChatFormatting;

@SuppressWarnings("DataFlowIssue")
public class Colors {
    public static final HashMap<String, Color> STRING_TO_COLOR_MAP = new HashMap<>() {{
        this.put("black", Color.BLACK);
        this.put("white", Color.WHITE);
        this.put("red", Color.RED);
        this.put("darkred", new Color(139, 0, 0));
        this.put("dark_red", new Color(139, 0, 0));
        this.put("green", Color.GREEN);
        this.put("blue", Color.BLUE);
        this.put("lightblue", new Color(173, 216, 230));
        this.put("light_blue", new Color(173, 216, 230));
        this.put("yellow", Color.YELLOW);
        this.put("magenta", Color.MAGENTA);
        this.put("cyan", Color.CYAN);
        this.put("gray", Color.GRAY);
        this.put("lightgray", Color.LIGHT_GRAY);
        this.put("darkgray", Color.DARK_GRAY);
        this.put("light_gray", Color.LIGHT_GRAY);
        this.put("dark_gray", Color.DARK_GRAY);
        this.put("orange", Color.ORANGE);
        this.put("pink", Color.PINK);
        this.put("purple", new Color(160, 32, 240));
        this.put("lime", new Color(50, 205, 50));
        this.put("brown", new Color(165, 42, 42));
    }};

    public static Color fromString(String color) {
        return Colors.fromString(color, Color.BLACK);
    }
    public static Color fromString(String color, Color defaultColor) {
        // none
        if (color.isEmpty())
        {
            return defaultColor;
        }

        // hex
        if (color.startsWith("#")) {
            color = color.substring(1);
            return new Color(Integer.parseInt(color, 16));
        }

        // in list
        var c = Colors.STRING_TO_COLOR_MAP.getOrDefault(color, null);
        if (c != null) return c;

        // minecraft color conversion
        var temp = ChatFormatting.getByName(color.toUpperCase(Locale.ROOT));
        if (temp != null) {
            if (temp.equals(ChatFormatting.RESET)) return defaultColor;
            if (temp.equals(ChatFormatting.BOLD)) return defaultColor;
            if (temp.equals(ChatFormatting.ITALIC)) return defaultColor;
            if (temp.equals(ChatFormatting.OBFUSCATED)) return defaultColor;
            if (temp.equals(ChatFormatting.STRIKETHROUGH)) return defaultColor;
            return new Color((temp.getColor() / 256 / 256) % 256, (temp.getColor() / 256) % 256, temp.getColor() % 256);
        }
        // fallback
        return defaultColor;
    }
}
