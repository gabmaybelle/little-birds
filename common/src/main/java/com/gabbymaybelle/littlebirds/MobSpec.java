package com.gabbymaybelle.littlebirds;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Everything a bird needs registering, in one place, so each loader can do it its own way (Fabric
 * builds the spawn restriction into the entity type; NeoForge has an event for it). The natural
 * spawns are listed here for Fabric; NeoForge reads the same numbers from
 * {@code data/little_birds/neoforge/biome_modifier/}, and the game tests check both agree.
 */
public record MobSpec<T extends Mob>(
		Identifier id,
		EntityType.EntityFactory<T> factory,
		MobCategory category,
		float width,
		float height,
		float eyeHeight,
		Supplier<AttributeSupplier.Builder> attributes,
		SpawnPlacementType placement,
		Heightmap.Types heightmap,
		SpawnPlacements.SpawnPredicate<T> spawnRule,
		List<Spawn> spawns) {

	/** A natural spawn: in the biomes of a tag, with vanilla's weight and flock size. */
	public record Spawn(TagKey<Biome> biomes, int weight, int minGroup, int maxGroup) {
	}
}
