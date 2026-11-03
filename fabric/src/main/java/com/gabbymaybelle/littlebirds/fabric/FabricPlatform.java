package com.gabbymaybelle.littlebirds.fabric;

import com.gabbymaybelle.littlebirds.MobSpec;
import com.gabbymaybelle.littlebirds.platform.Platform;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

public class FabricPlatform implements Platform {
	@Override
	public String name() {
		return "Fabric";
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, Identifier id, Supplier<T> factory) {
		Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(registry.identifier());
		T value = Registry.register(target, id, factory.get());
		return () -> value;
	}

	/** Fabric builds the attributes and spawn restriction into the entity type, and adds biome spawns in code. */
	@Override
	public <T extends Mob> Supplier<EntityType<T>> registerMob(MobSpec<T> spec) {
		EntityType<T> type = FabricEntityType.Builder.createMob(spec.factory(), spec.category(), mob -> mob
						.defaultAttributes(spec.attributes())
						.spawnPlacement(spec.placement(), spec.heightmap(), spec.spawnRule()))
				.sized(spec.width(), spec.height())
				.eyeHeight(spec.eyeHeight())
				.clientTrackingRange(8)
				.build(ResourceKey.create(Registries.ENTITY_TYPE, spec.id()));
		Registry.register(BuiltInRegistries.ENTITY_TYPE, spec.id(), type);
		for (MobSpec.Spawn spawn : spec.spawns()) {
			BiomeModifications.addSpawn(BiomeSelectors.tag(spawn.biomes()), spec.category(), type, spawn.weight(), spawn.minGroup(), spawn.maxGroup());
		}
		return () -> type;
	}

	@Override
	public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
		CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> output.accept(item.get()));
	}
}
