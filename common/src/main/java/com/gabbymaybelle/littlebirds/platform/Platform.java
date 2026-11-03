package com.gabbymaybelle.littlebirds.platform;

import com.gabbymaybelle.littlebirds.MobSpec;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

/**
 * The few things that differ between loaders. {@code fabric} and {@code neoforge} each provide
 * one implementation through {@code META-INF/services}; common code reaches it as
 * {@link Services#PLATFORM}.
 */
public interface Platform {
	/** "Fabric" or "NeoForge", for the log. */
	String name();

	/** The game's config folder, where the settings file lives. */
	Path configDir();

	/**
	 * Registers something (an item, a sound...). Fabric does it at once; NeoForge waits for its
	 * registry event, so the result is a supplier that is only valid once the game has finished
	 * loading.
	 */
	<T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, Identifier id, Supplier<T> factory);

	/**
	 * Registers a mob: its entity type, default attributes, where it may spawn naturally and, on
	 * loaders that do spawns in code, which biomes spawn it.
	 */
	<T extends Mob> Supplier<EntityType<T>> registerMob(MobSpec<T> spec);

	/** Puts an item in one of the vanilla creative tabs. */
	void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item);
}
