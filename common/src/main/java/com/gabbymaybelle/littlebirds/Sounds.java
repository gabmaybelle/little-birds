package com.gabbymaybelle.littlebirds;

import com.gabbymaybelle.littlebirds.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The birds' sound events. Which files play is {@code assets/little_birds/sounds.json}: a few
 * variants of each, cut from recordings of real budgies ({@code sounds/budgie/}).
 */
public final class Sounds {
	/** A budgie going about its day. */
	public static final Supplier<SoundEvent> BUDGIE_CHATTER = sound("budgie_chatter");
	/** The chirp when its owner comes home. */
	public static final Supplier<SoundEvent> BUDGIE_GREETING = sound("budgie_greeting");
	/** The murmur as it settles on its perch for the night. */
	public static final Supplier<SoundEvent> BUDGIE_SETTLE = sound("budgie_settle");

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
