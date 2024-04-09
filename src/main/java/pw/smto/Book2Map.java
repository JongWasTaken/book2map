package pw.smto;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;

public class Book2Map implements ModInitializer {
	public static final String MOD_ID = "book2map";
	public static final ArrayList<Font> FONTS = new ArrayList<Font>();

	@Override
	public void onInitialize() {
		var fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for (Font font : fonts) {
			if (font.getFontName().contains("Mono")) {
				if (!font.getFontName().contains("Bold") && !font.getFontName().contains("Italic")) {
					FONTS.add(font);
				}
			}
		}
		ServerLifecycleEvents.SERVER_STARTED.register(Commands::register);
		Logger.info("book2map loaded!");
	}

	public static class Logger {
		private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
		public static void info(String s, Object... objects) {
			LOGGER.info(s, objects);
		}
		public static void warn(String s, Object... objects) {
			LOGGER.warn(s, objects);
		}
		public static void error(String s, Object... objects) {
			LOGGER.error(s, objects);
		}
	}
}