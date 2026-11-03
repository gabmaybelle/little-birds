package com.gabbymaybelle.littlebirds;

import com.gabbymaybelle.littlebirds.platform.Services;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The settings, a plain {@code config/little_birds.properties} file written with its defaults the
 * first time the game starts. Read once at start-up; a change needs a restart.
 * <ul>
 * <li>{@code natural_spawning}: whether wild budgies spawn at all (spawn eggs always work).</li>
 * <li>{@code spawn_chance}: 0 to 100, how often a natural spawn the game picks actually happens.
 * 100 is vanilla's own rate for the weights the mod gives; lower it for rarer birds.</li>
 * </ul>
 */
public final class Settings {
	public static boolean naturalSpawning = true;
	public static int spawnChance = 100;

	private Settings() {
	}

	public static void load() {
		Path file = Services.PLATFORM.configDir().resolve(LittleBirds.MOD_ID + ".properties");
		Properties props = new Properties();
		if (Files.exists(file)) {
			try (InputStream in = Files.newInputStream(file)) {
				props.load(in);
			} catch (IOException e) {
				LittleBirds.LOGGER.warn("Little Birds: couldn't read {}; using the defaults", file, e);
			}
		}
		naturalSpawning = Boolean.parseBoolean(props.getProperty("natural_spawning", "true").trim());
		spawnChance = clamp(props.getProperty("spawn_chance", "100"));
		if (!Files.exists(file)) {
			write(file);
		}
	}

	private static int clamp(String value) {
		try {
			return Math.max(0, Math.min(100, Integer.parseInt(value.trim())));
		} catch (NumberFormatException e) {
			return 100;
		}
	}

	private static void write(Path file) {
		Properties props = new Properties();
		props.setProperty("natural_spawning", Boolean.toString(naturalSpawning));
		props.setProperty("spawn_chance", Integer.toString(spawnChance));
		try {
			Files.createDirectories(file.getParent());
			try (OutputStream out = Files.newOutputStream(file)) {
				props.store(out, "Little Birds. natural_spawning: whether wild budgies spawn (spawn eggs always work). spawn_chance: 0 to 100, how often a natural spawn happens.");
			}
		} catch (IOException e) {
			LittleBirds.LOGGER.warn("Little Birds: couldn't write {}", file, e);
		}
	}
}
