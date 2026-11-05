package com.gabbymaybelle.littlebirds;

import com.gabbymaybelle.littlebirds.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What the birds eat, besides the seeds that tame and breed them. Real pet birds live on seed or
 * pellets, get a little fresh veg and fruit every day, go mad for millet spray (so it has to be
 * rationed), and must never have chocolate.
 *
 * <ul>
 * <li>The <b>millet spray</b>: the one treat. It tames a wild bird on the first try, coaxes a
 * bird to follow the hand that holds it, and gets a happy bob; but a treat is not a meal, and a
 * bird that has just had one won't take another for a while.</li>
 * <li><b>Snacks</b> ({@code #little_birds:snacks}): carrot, apple, melon, berries, beetroot,
 * from the player's own farm. A tamed bird nibbles one (crumbs and all) and is the better for
 * it; a chick fed one grows a little.</li>
 * <li><b>Refused</b> ({@code #little_birds:refused}): cocoa beans, a poisonous potato. The bird
 * shakes its head and takes nothing. (Nothing in the game can hurt a bird by eating.)</li>
 * </ul>
 */
public final class Foods {
	public static final Identifier MILLET_SPRAY_ID = LittleBirds.id("millet_spray");
	private static final ResourceKey<Item> MILLET_SPRAY_KEY = ResourceKey.create(Registries.ITEM, MILLET_SPRAY_ID);

	public static final Supplier<Item> MILLET_SPRAY = Services.PLATFORM.register(Registries.ITEM, MILLET_SPRAY_ID,
			() -> new Item(new Item.Properties().setId(MILLET_SPRAY_KEY)));

	public static final TagKey<Item> SNACKS = TagKey.create(Registries.ITEM, LittleBirds.id("snacks"));
	public static final TagKey<Item> REFUSED = TagKey.create(Registries.ITEM, LittleBirds.id("refused"));

	/** How long a bird is full after a treat (ticks): half a minute. */
	public static final int TREAT_FULL = 600;
	/** How long after a snack before it takes another (ticks). */
	public static final int SNACK_FULL = 100;

	private Foods() {
	}

	public static boolean isTreat(ItemStack stack) {
		return stack.is(MILLET_SPRAY.get());
	}

	public static boolean isSnack(ItemStack stack) {
		return stack.is(SNACKS);
	}

	public static boolean isRefused(ItemStack stack) {
		return stack.is(REFUSED);
	}

	/** Touching the class registers the item; this puts it with the other foods in the creative menu. */
	public static void register() {
		ResourceKey<CreativeModeTab> food = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("food_and_drinks"));
		Services.PLATFORM.addToCreativeTab(food, MILLET_SPRAY);
	}
}
