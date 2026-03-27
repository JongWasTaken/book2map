package dev.smto.book2map;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Book2Map implements ModInitializer {
	public static final String MOD_ID = "book2map";
	public static final Path CONFIG_BASE_DIR = Path.of(FabricLoader.getInstance().getConfigDir().toString(), Book2Map.MOD_ID);
	public static final Path CONFIG_TEXTURES_DIR = Path.of(Book2Map.CONFIG_BASE_DIR.toString(),"textures");
	public static final Path CONFIG_FONTS_DIR = Path.of(Book2Map.CONFIG_BASE_DIR.toString(),"fonts");
	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Book2Map.MOD_ID);

	@Override
	public void onInitialize() {
		// create directories
		if (!Files.exists(Book2Map.CONFIG_TEXTURES_DIR)) {
			try {
				Files.createDirectories(Book2Map.CONFIG_TEXTURES_DIR);
			} catch (Exception e) {
				LOGGER.error("Error while creating config directories: " + e.toString());
			}
		}
		if (!Files.exists(Book2Map.CONFIG_FONTS_DIR)) {
			try {
				Files.createDirectories(Book2Map.CONFIG_FONTS_DIR);
			} catch (Exception e) {
				LOGGER.error("Error while creating config directories: " + e.toString());
			}
		}

		// unpack included fonts
		var monocraftFile = Path.of(Book2Map.CONFIG_FONTS_DIR.toString(),"Monocraft.ttf");
		if (!Files.exists(monocraftFile)) {
			try {
				Files.copy(Path.of(Objects.requireNonNull(Book2Map.class.getResource("/fonts/Monocraft.ttf")).toURI()), monocraftFile);
			} catch (Exception e) {
				LOGGER.error("Error while extracting included fonts: " + e.toString());
			}
		}
		var minecraftFontFile = Path.of(Book2Map.CONFIG_FONTS_DIR.toString(),"Minecraft.otf");
		if (!Files.exists(minecraftFontFile)) {
			try {
				Files.copy(Path.of(Objects.requireNonNull(Book2Map.class.getResource("/fonts/Minecraft.otf")).toURI()), minecraftFontFile);
			} catch (Exception e) {
				LOGGER.error("Error while extracting included fonts: " + e.toString());
			}
		}
		var minecraftFontBoldFile = Path.of(Book2Map.CONFIG_FONTS_DIR.toString(),"Minecraft-Bold.otf");
		if (!Files.exists(minecraftFontBoldFile)) {
			try {
				Files.copy(Path.of(Objects.requireNonNull(Book2Map.class.getResource("/fonts/Minecraft-Bold.otf")).toURI()), minecraftFontBoldFile);
			} catch (Exception e) {
				LOGGER.error("Error while extracting included fonts: " + e.toString());
			}
		}

		// register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, x, environment) -> {
			Commands.register(dispatcher);
		});
		LOGGER.info("book2map loaded!");
	}
}