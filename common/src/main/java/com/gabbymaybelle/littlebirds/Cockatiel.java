package com.gabbymaybelle.littlebirds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * A cockatiel: a parrot-sized little bird with a crest that lifts when it says hello, in three
 * colours, with a whistle for a voice. Everything it does as a pet (the perch, the greeting, the
 * shoulder) is {@link LittleBird}'s.
 */
public class Cockatiel extends LittleBird {
	/** The three colours, in the order of {@link #COLOURS}; a wild cockatiel picks one at random. */
	public enum Colour {
		/** The normal grey: grey body, yellow face and crest, orange cheeks, a white wing bar. */
		GREY,
		/** Lutino: soft yellow and white all over, the orange cheeks, red eyes. */
		LUTINO,
		/** White-faced grey: the grey with a white face and crest and no cheek patch. */
		WHITEFACE;

		public static final Colour[] COLOURS = values();

		public static Colour byId(int id) {
			return COLOURS[Mth.clamp(id, 0, COLOURS.length - 1)];
		}
	}

	public Cockatiel(EntityType<? extends Cockatiel> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return birdAttributes(6.0);
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

	/** A whistle takes a moment longer than a chirp, and the crest stays up for it. */
	@Override
	protected int greetingTicks() {
		return 40;
	}

	@Override
	public SoundEvent chatterSound() {
		return Sounds.COCKATIEL_CHATTER.get();
	}

	@Override
	public SoundEvent greetingSound() {
		return Sounds.COCKATIEL_GREETING.get();
	}

	@Override
	public SoundEvent settleSound() {
		return Sounds.COCKATIEL_SETTLE.get();
	}
}
