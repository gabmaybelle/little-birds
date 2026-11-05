package com.gabbymaybelle.littlebirds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A canary: a song bird a touch smaller than a budgie, in three colours, that sings. Every
 * morning, from sunrise through the first hours of the day, a tamed canary sings its song once
 * from wherever it sits; a millet spray gets a song too. It keeps to its perch and never rides
 * a shoulder. Everything else it does as a pet (the perch, the greeting, the call, chicks) is
 * {@link LittleBird}'s.
 */
public class Canary extends LittleBird {
	/** The three colours, in the order of {@link #COLOURS}; a wild canary picks one at random. */
	public enum Colour {
		/** The classic canary yellow. */
		YELLOW,
		/** The red factor: orange-red all over. */
		RED,
		/** White, with a grey shadow on the wings. */
		WHITE;

		public static final Colour[] COLOURS = values();

		public static Colour byId(int id) {
			return COLOURS[Mth.clamp(id, 0, COLOURS.length - 1)];
		}
	}

	/** The morning, on the day clock: from sunrise (0) for the first three hours (3000 ticks). */
	public static final long MORNING_FROM = 0L;
	public static final long MORNING_UNTIL = 3000L;
	/** The day it last sang its morning song on, so it sings once a morning; -1 before its first. */
	private long lastSongDay = -1L;

	public Canary(EntityType<? extends Canary> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return birdAttributes(4.0);
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

	/** A canary keeps to its perch: it never rides a shoulder. */
	@Override
	public boolean ridesShoulders() {
		return false;
	}

	@Override
	public SoundEvent songSound() {
		return Sounds.CANARY_SONG.get();
	}

	/** The song runs several seconds; the pose holds for about seven. */
	@Override
	protected int songTicks() {
		return 140;
	}

	/** A treat gets a song rather than a bob. */
	@Override
	protected void enjoyTreat() {
		this.sing();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.tickCount % 20 == 0) {
			this.singInTheMorning(level);
		}
	}

	/** Whether it is morning on the day clock: the first hours after sunrise. */
	public static boolean isMorning(long clockTime) {
		long hour = Math.floorMod(clockTime, 24000L);
		return hour >= MORNING_FROM && hour < MORNING_UNTIL;
	}

	/** Once each morning, a tamed, grown canary that is awake and on its feet (or its seat) sings. */
	private void singInTheMorning(ServerLevel level) {
		if (!this.isTame() || this.isBaby() || this.isFlying() || this.isRoosting() || this.isOnShoulder()) {
			return;
		}
		long clock = level.getOverworldClockTime();
		long day = Math.floorDiv(clock, 24000L);
		if (!isMorning(clock) || this.lastSongDay == day) {
			return;
		}
		this.lastSongDay = day;
		this.sing();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putLong("LastSongDay", this.lastSongDay);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.lastSongDay = input.getLongOr("LastSongDay", -1L);
	}

	@Override
	public SoundEvent chatterSound() {
		return Sounds.CANARY_CHATTER.get();
	}

	@Override
	public SoundEvent greetingSound() {
		return Sounds.CANARY_GREETING.get();
	}

	@Override
	public SoundEvent settleSound() {
		return Sounds.CANARY_SETTLE.get();
	}
}
