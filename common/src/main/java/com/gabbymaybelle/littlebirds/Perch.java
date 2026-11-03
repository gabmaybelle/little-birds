package com.gabbymaybelle.littlebirds;

import com.gabbymaybelle.littlebirds.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The perch block and its item, registered through the loader's {@link Services#PLATFORM}. */
public final class Perch {
	public static final Identifier ID = LittleBirds.id("perch");
	private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, ID);
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);

	public static final Supplier<Block> BLOCK = Services.PLATFORM.register(Registries.BLOCK, ID, () -> new PerchBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5F).sound(SoundType.WOOD).noOcclusion().setId(BLOCK_KEY)));

	public static final Supplier<Item> ITEM = Services.PLATFORM.register(Registries.ITEM, ID,
			() -> new BlockItem(BLOCK.get(), new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix()));

	private Perch() {
	}

	/** Touching the class registers the perch; this puts it in the creative menu beside the other furniture. */
	public static void register() {
		ResourceKey<CreativeModeTab> functional = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("functional_blocks"));
		Services.PLATFORM.addToCreativeTab(functional, ITEM);
	}
}
