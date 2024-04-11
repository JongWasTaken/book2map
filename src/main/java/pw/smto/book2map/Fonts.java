package pw.smto.book2map;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class Fonts {
    private static ArrayList<Font> findAllFonts() {
        var f = new ArrayList<Font>();
        var env = GraphicsEnvironment.getLocalGraphicsEnvironment();

        // user fonts
        var userFontFiles = Book2Map.CONFIG_FONTS_DIR.toFile().listFiles();
        if (userFontFiles != null) {
            for (File file : userFontFiles) {
                try {
                    env.registerFont(Font.createFont(Font.TRUETYPE_FONT, file));
                } catch (Exception e) {
                    Book2Map.Logger.error("Error while loading user font \"" + file.getName() + "\": " + e.toString());
                }
            }
        }

        // system fonts
        var fonts = env.getAllFonts();
        for (Font font : fonts) {
            if (!font.getFontName().contains("Bold") && !font.getFontName().contains("Italic")) {
                f.add(font);
            }
        }

        return f;
    }
    public static ArrayList<Font> LIST = findAllFonts();

    public static void reload() {
        LIST = findAllFonts();
    }
}
