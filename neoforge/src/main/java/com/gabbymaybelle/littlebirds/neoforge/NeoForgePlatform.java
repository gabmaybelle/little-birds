package com.gabbymaybelle.littlebirds.neoforge;

import com.gabbymaybelle.littlebirds.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge only lets mods register during its registry events, so everything common code
 * registers is queued here and handed over when the matching event fires.
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

	private record Attributes(Supplier<? extends EntityType<? extends LivingEntity>> type, Supplier<AttributeSupplier.Builder> attributes) {
	}

	private final List<Pending<?>> pending = new ArrayList<>();
	private final List<TabEntry> tabEntries = new ArrayList<>();
	private final List<Attributes> attributes = new ArrayList<>();

	@Override
	public String name() {
		return "NeoForge";
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
	public void registerAttributes(Supplier<? extends EntityType<? extends LivingEntity>> type, Supplier<AttributeSupplier.Builder> builder) {
		attributes.add(new Attributes(type, builder));
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
		for (Attributes entry : attributes) {
			event.put(entry.type().get(), entry.attributes().get().build());
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
