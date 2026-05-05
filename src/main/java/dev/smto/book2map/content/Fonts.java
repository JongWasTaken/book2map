package dev.smto.book2map.content;

import dev.smto.book2map.Book2Map;

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
                if (Fonts.isFontFile(file)) {
                    try {
                        env.registerFont(Font.createFont(Font.TRUETYPE_FONT, file));
                    } catch (Exception e) {
                        Book2Map.LOGGER.error("Error while loading user font \"{}\": {}", file.getName(), e.toString());
                    }
                }
                else {
                    Book2Map.LOGGER.warn("Found non-font file \"{}\"", file.getName());
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
    private static ArrayList<Font> availableFonts = Fonts.findAllFonts();

    public static void reload() {
        Fonts.availableFonts = Fonts.findAllFonts();
    }

    private static boolean isFontFile(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf('.');
        if (lastIndexOf == -1) {
            return false; // empty extension
        }
        if (name.substring(lastIndexOf).equals(".ttf")) return true;
        return name.substring(lastIndexOf).equals(".otf");
    }

    public static ArrayList<Font> getAvailableFonts() {
        return Fonts.availableFonts;
    }
}

