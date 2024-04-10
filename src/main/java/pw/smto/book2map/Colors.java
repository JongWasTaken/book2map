package pw.smto.book2map;

import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.HashMap;
import java.util.Locale;

public class Colors {

    private static HashMap<String, Color> initColors() {
        var t = new HashMap<String, Color>();
        t.put("black", Color.BLACK);
        t.put("white", Color.WHITE);
        t.put("red", Color.RED);
        t.put("darkred", new Color(139, 0, 0));
        t.put("dark_red", new Color(139, 0, 0));
        t.put("green", Color.GREEN);
        t.put("blue", Color.BLUE);
        t.put("lightblue", new Color(173, 216, 230));
        t.put("light_blue", new Color(173, 216, 230));
        t.put("yellow", Color.YELLOW);
        t.put("magenta", Color.MAGENTA);
        t.put("cyan", Color.CYAN);
        t.put("gray", Color.GRAY);
        t.put("lightgray", Color.LIGHT_GRAY);
        t.put("darkgray", Color.DARK_GRAY);
        t.put("light_gray", Color.LIGHT_GRAY);
        t.put("dark_gray", Color.DARK_GRAY);
        t.put("orange", Color.ORANGE);
        t.put("pink", Color.PINK);
        t.put("purple", new Color(160, 32, 240));
        t.put("lime", new Color(50, 205, 50));
        t.put("brown", new Color(165,42,42));
        return t;
    }
    public static final HashMap<String, Color> LIST = initColors();

    public static Color fromString(String color) {
        return fromString(color, Color.BLACK);
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
        var c = LIST.getOrDefault(color, Color.BLACK);
        if (c != Color.BLACK) return c;

        // minecraft color conversion
        var temp = Formatting.byName(color.toUpperCase(Locale.ROOT));
        if (temp != null) {
            return new Color((temp.getColorValue() / 256 / 256) % 256, (temp.getColorValue() / 256) % 256, temp.getColorValue() % 256);
        }

        // fallback
        return defaultColor;
    }
}
