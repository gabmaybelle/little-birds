package com.gabbymaybelle.littlebirds;

import com.gabbymaybelle.littlebirds.platform.Services;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** The birds, their spawn eggs, attributes and natural spawns, registered through the loader's {@link Services#PLATFORM}. */
public final class Birds {
	public static final Identifier BUDGIE_ID = LittleBirds.id("budgie");
	public static final Identifier COCKATIEL_ID = LittleBirds.id("cockatiel");
	public static final Identifier LOVEBIRD_ID = LittleBirds.id("lovebird");
	public static final Identifier CANARY_ID = LittleBirds.id("canary");
	public static final Identifier ZEBRA_FINCH_ID = LittleBirds.id("zebra_finch");
	private static final ResourceKey<Item> BUDGIE_EGG_KEY = ResourceKey.create(Registries.ITEM, LittleBirds.id("budgie_spawn_egg"));
	private static final ResourceKey<Item> COCKATIEL_EGG_KEY = ResourceKey.create(Registries.ITEM, LittleBirds.id("cockatiel_spawn_egg"));
	private static final ResourceKey<Item> LOVEBIRD_EGG_KEY = ResourceKey.create(Registries.ITEM, LittleBirds.id("lovebird_spawn_egg"));
	private static final ResourceKey<Item> CANARY_EGG_KEY = ResourceKey.create(Registries.ITEM, LittleBirds.id("canary_spawn_egg"));
	private static final ResourceKey<Item> ZEBRA_FINCH_EGG_KEY = ResourceKey.create(Registries.ITEM, LittleBirds.id("zebra_finch_spawn_egg"));

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

	/** Lovebirds live in pairs: a savanna spawns them two at a time. */
	public static final List<MobSpec.Spawn> LOVEBIRD_SPAWNS = List.of(
			new MobSpec.Spawn(BiomeTags.IS_SAVANNA, 6, 2, 2));

	/**
	 * Where wild canaries live: the flowery, grassy places, as a biome tag of the mod's own
	 * ({@code #little_birds:canary_spawns}: flower forests, meadows, cherry groves), in twos and threes.
	 */
	public static final TagKey<Biome> CANARY_BIOMES = TagKey.create(Registries.BIOME, LittleBirds.id("canary_spawns"));
	public static final List<MobSpec.Spawn> CANARY_SPAWNS = List.of(
			new MobSpec.Spawn(CANARY_BIOMES, 10, 2, 3));

	/** Where wild zebra finches live: open grassland ({@code #little_birds:zebra_finch_spawns}: plains, sunflower plains), always in threes. */
	public static final TagKey<Biome> ZEBRA_FINCH_BIOMES = TagKey.create(Registries.BIOME, LittleBirds.id("zebra_finch_spawns"));
	public static final List<MobSpec.Spawn> ZEBRA_FINCH_SPAWNS = List.of(
			new MobSpec.Spawn(ZEBRA_FINCH_BIOMES, 6, 3, 3));

	/** A budgie is a little smaller than a parrot (0.5 x 0.9): it needs to fit on a perch. */
	public static final Supplier<EntityType<Budgie>> BUDGIE = Services.PLATFORM.registerMob(new MobSpec<>(
			BUDGIE_ID, Budgie::new, MobCategory.CREATURE, 0.4F, 0.7F, 0.5F, Budgie::createAttributes,
			SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, LittleBird::checkSpawnRules, BUDGIE_SPAWNS));

	/** A cockatiel is about a parrot's size. */
	public static final Supplier<EntityType<Cockatiel>> COCKATIEL = Services.PLATFORM.registerMob(new MobSpec<>(
			COCKATIEL_ID, Cockatiel::new, MobCategory.CREATURE, 0.5F, 0.9F, 0.65F, Cockatiel::createAttributes,
			SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, LittleBird::checkSpawnRules, COCKATIEL_SPAWNS));

	/** A lovebird is a budgie's size, a touch rounder; two fit on one bar. */
	public static final Supplier<EntityType<Lovebird>> LOVEBIRD = Services.PLATFORM.registerMob(new MobSpec<>(
			LOVEBIRD_ID, Lovebird::new, MobCategory.CREATURE, 0.4F, 0.7F, 0.5F, Lovebird::createAttributes,
			SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, LittleBird::checkSpawnRules, LOVEBIRD_SPAWNS));

	/** A canary is a touch smaller than a budgie. */
	public static final Supplier<EntityType<Canary>> CANARY = Services.PLATFORM.registerMob(new MobSpec<>(
			CANARY_ID, Canary::new, MobCategory.CREATURE, 0.35F, 0.5F, 0.4F, Canary::createAttributes,
			SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, LittleBird::checkSpawnRules, CANARY_SPAWNS));

	/** A zebra finch is the smallest of the birds. */
	public static final Supplier<EntityType<ZebraFinch>> ZEBRA_FINCH = Services.PLATFORM.registerMob(new MobSpec<>(
			ZEBRA_FINCH_ID, ZebraFinch::new, MobCategory.CREATURE, 0.3F, 0.45F, 0.35F, ZebraFinch::createAttributes,
			SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, LittleBird::checkSpawnRules, ZEBRA_FINCH_SPAWNS));

	public static final Supplier<Item> BUDGIE_SPAWN_EGG = Services.PLATFORM.register(Registries.ITEM, BUDGIE_EGG_KEY.identifier(),
			() -> new SpawnEggItem(new Item.Properties().setId(BUDGIE_EGG_KEY).spawnEgg(BUDGIE.get())));

	public static final Supplier<Item> COCKATIEL_SPAWN_EGG = Services.PLATFORM.register(Registries.ITEM, COCKATIEL_EGG_KEY.identifier(),
			() -> new SpawnEggItem(new Item.Properties().setId(COCKATIEL_EGG_KEY).spawnEgg(COCKATIEL.get())));

	public static final Supplier<Item> LOVEBIRD_SPAWN_EGG = Services.PLATFORM.register(Registries.ITEM, LOVEBIRD_EGG_KEY.identifier(),
			() -> new SpawnEggItem(new Item.Properties().setId(LOVEBIRD_EGG_KEY).spawnEgg(LOVEBIRD.get())));

	public static final Supplier<Item> CANARY_SPAWN_EGG = Services.PLATFORM.register(Registries.ITEM, CANARY_EGG_KEY.identifier(),
			() -> new SpawnEggItem(new Item.Properties().setId(CANARY_EGG_KEY).spawnEgg(CANARY.get())));

	public static final Supplier<Item> ZEBRA_FINCH_SPAWN_EGG = Services.PLATFORM.register(Registries.ITEM, ZEBRA_FINCH_EGG_KEY.identifier(),
			() -> new SpawnEggItem(new Item.Properties().setId(ZEBRA_FINCH_EGG_KEY).spawnEgg(ZEBRA_FINCH.get())));

	private Birds() {
	}

	/** Touching the class registers the birds; this puts the eggs in the creative menu. */
	public static void register() {
		ResourceKey<CreativeModeTab> eggs = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("spawn_eggs"));
		Services.PLATFORM.addToCreativeTab(eggs, BUDGIE_SPAWN_EGG);
		Services.PLATFORM.addToCreativeTab(eggs, COCKATIEL_SPAWN_EGG);
		Services.PLATFORM.addToCreativeTab(eggs, LOVEBIRD_SPAWN_EGG);
		Services.PLATFORM.addToCreativeTab(eggs, CANARY_SPAWN_EGG);
		Services.PLATFORM.addToCreativeTab(eggs, ZEBRA_FINCH_SPAWN_EGG);
	}
}
