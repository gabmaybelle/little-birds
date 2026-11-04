package com.gabbymaybelle.littlebirds;

import com.gabbymaybelle.littlebirds.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The birds' sound events. Which files play is {@code assets/little_birds/sounds.json}: a few
 * variants of each, cut from recordings of the real birds ({@code sounds/<bird>/}).
 */
public final class Sounds {
	/** A budgie going about its day. */
	public static final Supplier<SoundEvent> BUDGIE_CHATTER = sound("budgie_chatter");
	/** The chirp when its owner comes home. */
	public static final Supplier<SoundEvent> BUDGIE_GREETING = sound("budgie_greeting");
	/** The murmur as it settles on its perch for the night. */
	public static final Supplier<SoundEvent> BUDGIE_SETTLE = sound("budgie_settle");

	/** A cockatiel's everyday chirps and whistles. */
	public static final Supplier<SoundEvent> COCKATIEL_CHATTER = sound("cockatiel_chatter");
	/** The whistle when its owner comes home. */
	public static final Supplier<SoundEvent> COCKATIEL_GREETING = sound("cockatiel_greeting");
	/** The soft call as it settles on its perch for the night. */
	public static final Supplier<SoundEvent> COCKATIEL_SETTLE = sound("cockatiel_settle");

	/** A lovebird's sharp little chirps. */
	public static final Supplier<SoundEvent> LOVEBIRD_CHATTER = sound("lovebird_chatter");
	/** The chirp when its owner comes home. */
	public static final Supplier<SoundEvent> LOVEBIRD_GREETING = sound("lovebird_greeting");
	/** The soft call as it settles on its perch for the night. */
	public static final Supplier<SoundEvent> LOVEBIRD_SETTLE = sound("lovebird_settle");

	private Sounds() {
	}

	private static Supplier<SoundEvent> sound(String name) {
		Identifier id = LittleBirds.id(name);
		return Services.PLATFORM.register(Registries.SOUND_EVENT, id, () -> SoundEvent.createVariableRangeEvent(id));
	}

	/** Touching the class registers the sounds. */
	public static void register() {
	}
}
