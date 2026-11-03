package com.gabbymaybelle.littlebirds.platform;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
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

	/**
	 * Registers something (an entity type, an item...). Fabric does it at once; NeoForge waits
	 * for its registry event, so the result is a supplier that is only valid once the game has
	 * finished loading.
	 */
	<T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, Identifier id, Supplier<T> factory);

	/** Gives a mob its default attributes (health, speed...). Each loader has its own moment for this. */
	void registerAttributes(Supplier<? extends EntityType<? extends LivingEntity>> type, Supplier<AttributeSupplier.Builder> attributes);

	/** Puts an item in one of the vanilla creative tabs. */
	void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item);
}
