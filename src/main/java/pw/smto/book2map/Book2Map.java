package pw.smto.book2map;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Book2Map implements ModInitializer {
	public static final String MOD_ID = "book2map";
	public static final Path CONFIG_BASE_DIR = Path.of(FabricLoader.getInstance().getConfigDir().toString(),MOD_ID);
	public static final Path CONFIG_TEXTURES_DIR = Path.of(CONFIG_BASE_DIR.toString(),"textures");
	public static final Path CONFIG_FONTS_DIR = Path.of(CONFIG_BASE_DIR.toString(),"fonts");


	@Override
	public void onInitialize() {
		if (!Files.exists(CONFIG_TEXTURES_DIR)) {
			try {
				Files.createDirectories(CONFIG_TEXTURES_DIR);
			} catch (Exception e) {
				Book2Map.Logger.error("Error while creating config directories: " + e.toString());
			}
		}
		if (!Files.exists(CONFIG_FONTS_DIR)) {
			try {
				Files.createDirectories(CONFIG_FONTS_DIR);
			} catch (Exception e) {
				Book2Map.Logger.error("Error while creating config directories: " + e.toString());
			}
		}
		var monocraftFile = Path.of(CONFIG_FONTS_DIR.toString(),"Monocraft.ttf");
		if (!Files.exists(monocraftFile)) {
			try {
				Files.copy(Path.of(Book2Map.class.getResource("/fonts/Monocraft.ttf").toURI()), monocraftFile);
			} catch (Exception e) {
				Book2Map.Logger.error("Error while extracting included fonts: " + e.toString());
			}
		}


		//ServerLifecycleEvents.SERVER_STARTED.register(Commands::register);

		CommandRegistrationCallback.EVENT.register((dispatcher, environment) -> {
			Commands.register(dispatcher);
		});
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