package com.gabbymaybelle.littlebirds;

import com.gabbymaybelle.littlebirds.platform.Services;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Little Birds: small pet birds, budgies first, that live on a perch in your house. The loader
 * entrypoints ({@code fabric} and {@code neoforge}) call {@link #init()} once; everything that
 * doesn't care which loader it runs on lives here in {@code common}. {@link Birds} registers
 * the birds and their spawn eggs; {@link Budgie} is the bird itself.
 */
public final class LittleBirds {
	public static final String MOD_ID = "little_birds";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private LittleBirds() {
	}

	public static void init() {
		Settings.load();
		Birds.register();
		Perch.register();
		Sounds.register();
		LOGGER.info("Little Birds: ready on {}", Services.PLATFORM.name());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
