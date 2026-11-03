package com.gabbymaybelle.littlebirds.neoforge;

import com.gabbymaybelle.littlebirds.MobSpec;
import com.gabbymaybelle.littlebirds.platform.Platform;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge only lets mods register during its registry events, so everything common code
 * registers is queued here and handed over when the matching event fires. Biome spawns are data
 * on NeoForge ({@code data/little_birds/neoforge/biome_modifier/}), so a mob's spawn list is not used here.
 */
public class NeoForgePlatform implements Platform {
	private record Pending<T>(ResourceKey<? extends Registry<T>> registry, Identifier id, Supplier<T> factory) {
		void offer(RegisterEvent event) {
			if (event.getRegistryKey().equals(registry)) {
				event.register(registry, id, factory);
			}
		}
	}

	private record TabEntry(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
	}

	private record PendingMob<T extends Mob>(MobSpec<T> spec, Supplier<EntityType<T>> type) {
		void attributes(EntityAttributeCreationEvent event) {
			event.put(type.get(), spec.attributes().get().build());
		}

		void placement(RegisterSpawnPlacementsEvent event) {
			event.register(type.get(), spec.placement(), spec.heightmap(), spec.spawnRule(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
		}
	}

	private final List<Pending<?>> pending = new ArrayList<>();
	private final List<TabEntry> tabEntries = new ArrayList<>();
	private final List<PendingMob<?>> mobs = new ArrayList<>();

	@Override
	public String name() {
		return "NeoForge";
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, Identifier id, Supplier<T> factory) {
		pending.add(new Pending<>(registry, id, factory));
		ResourceKey<T> key = ResourceKey.create(registry, id);
		return new Supplier<>() {
			private T value;

			@Override
			public T get() {
				if (value == null) {
					Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(registry.identifier());
					value = target.getValue(key);
					if (value == null) {
						throw new IllegalStateException(id + " isn't registered yet");
					}
				}
				return value;
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Mob> Supplier<EntityType<T>> registerMob(MobSpec<T> spec) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, spec.id());
		Supplier<EntityType<?>> registered = register(Registries.ENTITY_TYPE, spec.id(), () -> EntityType.Builder.of(spec.factory(), spec.category())
				.sized(spec.width(), spec.height())
				.eyeHeight(spec.eyeHeight())
				.clientTrackingRange(8)
				.build(key));
		Supplier<EntityType<T>> type = () -> (EntityType<T>) registered.get();
		mobs.add(new PendingMob<>(spec, type));
		return type;
	}

	@Override
	public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
		tabEntries.add(new TabEntry(tab, item));
	}

	void onRegister(RegisterEvent event) {
		for (Pending<?> entry : pending) {
			entry.offer(event);
		}
	}

	void onAttributes(EntityAttributeCreationEvent event) {
		for (PendingMob<?> mob : mobs) {
			mob.attributes(event);
		}
	}

	void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
		for (PendingMob<?> mob : mobs) {
			mob.placement(event);
		}
	}

	void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
		for (TabEntry entry : tabEntries) {
			if (event.getTabKey().equals(entry.tab())) {
				event.accept(entry.item().get());
			}
		}
	}
}
