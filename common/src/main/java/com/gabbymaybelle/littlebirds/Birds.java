package com.gabbymaybelle.littlebirds;

import com.gabbymaybelle.littlebirds.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/** The birds, their spawn eggs and their attributes, registered through the loader's {@link Services#PLATFORM}. */
public final class Birds {
	public static final Identifier BUDGIE_ID = LittleBirds.id("budgie");
	private static final ResourceKey<EntityType<?>> BUDGIE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, BUDGIE_ID);
	private static final ResourceKey<Item> BUDGIE_EGG_KEY = ResourceKey.create(Registries.ITEM, LittleBirds.id("budgie_spawn_egg"));

	/** A budgie is a little smaller than a parrot (0.5 x 0.9): it needs to fit on a perch. */
	public static final Supplier<EntityType<Budgie>> BUDGIE = entity(BUDGIE_ID,
			() -> EntityType.Builder.of(Budgie::new, MobCategory.CREATURE).sized(0.4F, 0.7F).eyeHeight(0.5F).clientTrackingRange(8).build(BUDGIE_KEY));

	public static final Supplier<Item> BUDGIE_SPAWN_EGG = Services.PLATFORM.register(Registries.ITEM, BUDGIE_EGG_KEY.identifier(),
			() -> new SpawnEggItem(new Item.Properties().setId(BUDGIE_EGG_KEY).spawnEgg(BUDGIE.get())));

	private Birds() {
	}

	/** The entity registry holds {@code EntityType<?>}; this keeps the bird's own type on the way out. */
	@SuppressWarnings("unchecked")
	private static <T extends Entity> Supplier<EntityType<T>> entity(Identifier id, Supplier<EntityType<T>> factory) {
		Supplier<EntityType<?>> registered = Services.PLATFORM.register(Registries.ENTITY_TYPE, id, factory::get);
		return () -> (EntityType<T>) registered.get();
	}

	/** Touching the class registers the birds; this adds what needs a later moment: attributes and the creative menu. */
	public static void register() {
		Services.PLATFORM.registerAttributes(BUDGIE, Budgie::createAttributes);
		ResourceKey<CreativeModeTab> eggs = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("spawn_eggs"));
		Services.PLATFORM.addToCreativeTab(eggs, BUDGIE_SPAWN_EGG);
	}
}
