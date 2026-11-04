package com.gabbymaybelle.littlebirds;

import com.gabbymaybelle.littlebirds.platform.Services;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;

/** The birds, their spawn eggs, attributes and natural spawns, registered through the loader's {@link Services#PLATFORM}. */
public final class Birds {
	public static final Identifier BUDGIE_ID = LittleBirds.id("budgie");
	public static final Identifier COCKATIEL_ID = LittleBirds.id("cockatiel");
	private static final ResourceKey<Item> BUDGIE_EGG_KEY = ResourceKey.create(Registries.ITEM, LittleBirds.id("budgie_spawn_egg"));
	private static final ResourceKey<Item> COCKATIEL_EGG_KEY = ResourceKey.create(Registries.ITEM, LittleBirds.id("cockatiel_spawn_egg"));

	/**
	 * Where wild budgies live: savanna (the real bird's home, open grassland) and jungle edges.
	 * The weights sit beside vanilla's: a savanna's horses are 1 and its sheep 12; a jungle's
	 * parrots are 40. NeoForge reads the same numbers from its biome modifier files.
	 */
	public static final List<MobSpec.Spawn> BUDGIE_SPAWNS = List.of(
			new MobSpec.Spawn(BiomeTags.IS_SAVANNA, 15, 2, 4),
			new MobSpec.Spawn(BiomeTags.IS_JUNGLE, 10, 2, 4));

	/** Cockatiels share the budgies' savanna, in smaller, rarer groups. */
	public static final List<MobSpec.Spawn> COCKATIEL_SPAWNS = List.of(
			new MobSpec.Spawn(BiomeTags.IS_SAVANNA, 8, 2, 3));

	/** A budgie is a little smaller than a parrot (0.5 x 0.9): it needs to fit on a perch. */
	public static final Supplier<EntityType<Budgie>> BUDGIE = Services.PLATFORM.registerMob(new MobSpec<>(
			BUDGIE_ID, Budgie::new, MobCategory.CREATURE, 0.4F, 0.7F, 0.5F, Budgie::createAttributes,
			SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, LittleBird::checkSpawnRules, BUDGIE_SPAWNS));

	/** A cockatiel is about a parrot's size. */
	public static final Supplier<EntityType<Cockatiel>> COCKATIEL = Services.PLATFORM.registerMob(new MobSpec<>(
			COCKATIEL_ID, Cockatiel::new, MobCategory.CREATURE, 0.5F, 0.9F, 0.65F, Cockatiel::createAttributes,
			SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, LittleBird::checkSpawnRules, COCKATIEL_SPAWNS));

	public static final Supplier<Item> BUDGIE_SPAWN_EGG = Services.PLATFORM.register(Registries.ITEM, BUDGIE_EGG_KEY.identifier(),
			() -> new SpawnEggItem(new Item.Properties().setId(BUDGIE_EGG_KEY).spawnEgg(BUDGIE.get())));

	public static final Supplier<Item> COCKATIEL_SPAWN_EGG = Services.PLATFORM.register(Registries.ITEM, COCKATIEL_EGG_KEY.identifier(),
			() -> new SpawnEggItem(new Item.Properties().setId(COCKATIEL_EGG_KEY).spawnEgg(COCKATIEL.get())));

	private Birds() {
	}

	/** Touching the class registers the birds; this puts the eggs in the creative menu. */
	public static void register() {
		ResourceKey<CreativeModeTab> eggs = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("spawn_eggs"));
		Services.PLATFORM.addToCreativeTab(eggs, BUDGIE_SPAWN_EGG);
		Services.PLATFORM.addToCreativeTab(eggs, COCKATIEL_SPAWN_EGG);
	}
}
