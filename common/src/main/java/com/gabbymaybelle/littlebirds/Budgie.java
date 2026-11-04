package com.gabbymaybelle.littlebirds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * A budgie: the smallest of the little birds, in four colours, with a chirpy voice. Everything
 * it does as a pet (the perch, the greeting, the shoulder) is {@link LittleBird}'s.
 */
public class Budgie extends LittleBird {
	/** The four colours, in the order of {@link #COLOURS}; a wild budgie picks one at random. */
	public enum Colour {
		GREEN, BLUE, YELLOW, WHITE;

		public static final Colour[] COLOURS = values();

		public static Colour byId(int id) {
			return COLOURS[Mth.clamp(id, 0, COLOURS.length - 1)];
		}
	}

	public Budgie(EntityType<? extends Budgie> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return birdAttributes(5.0);
	}

	@Override
	public int variantCount() {
		return Colour.COLOURS.length;
	}

	public Colour getColour() {
		return Colour.byId(this.getVariant());
	}

	public void setColour(Colour colour) {
		this.setVariant(colour.ordinal());
	}

	@Override
	public SoundEvent chatterSound() {
		return Sounds.BUDGIE_CHATTER.get();
	}

	@Override
	public SoundEvent greetingSound() {
		return Sounds.BUDGIE_GREETING.get();
	}

	@Override
	public SoundEvent settleSound() {
		return Sounds.BUDGIE_SETTLE.get();
	}
}
