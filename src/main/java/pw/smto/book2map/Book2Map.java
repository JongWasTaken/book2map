package pw.smto.book2map;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
public class Book2Map implements ModInitializer {
	public static final String MOD_ID = "book2map";
	public static final Path CONFIG_BASE_DIR = Path.of(FabricLoader.getInstance().getConfigDir().toString(),MOD_ID);
	public static final Path CONFIG_TEXTURES_DIR = Path.of(CONFIG_BASE_DIR.toString(),"textures");
	public static final Path CONFIG_FONTS_DIR = Path.of(CONFIG_BASE_DIR.toString(),"fonts");


	@Override
	public void onInitialize() {
		// create directories
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

		// unpack included fonts
		var monocraftFile = Path.of(CONFIG_FONTS_DIR.toString(),"Monocraft.ttf");
		if (!Files.exists(monocraftFile)) {
			try {
				Files.copy(Path.of(Book2Map.class.getResource("/fonts/Monocraft.ttf").toURI()), monocraftFile);
			} catch (Exception e) {
				Book2Map.Logger.error("Error while extracting included fonts: " + e.toString());
			}
		}
		var minecraftFontFile = Path.of(CONFIG_FONTS_DIR.toString(),"Minecraft.otf");
		if (!Files.exists(minecraftFontFile)) {
			try {
				Files.copy(Path.of(Book2Map.class.getResource("/fonts/Minecraft.otf").toURI()), minecraftFontFile);
			} catch (Exception e) {
				Book2Map.Logger.error("Error while extracting included fonts: " + e.toString());
			}
		}
		var minecraftFontBoldFile = Path.of(CONFIG_FONTS_DIR.toString(),"Minecraft-Bold.otf");
		if (!Files.exists(minecraftFontBoldFile)) {
			try {
				Files.copy(Path.of(Book2Map.class.getResource("/fonts/Minecraft-Bold.otf").toURI()), minecraftFontBoldFile);
			} catch (Exception e) {
				Book2Map.Logger.error("Error while extracting included fonts: " + e.toString());
			}
		}

		// register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, x, environment) -> {
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