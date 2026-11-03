package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.Budgie;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the renderer hands the model each frame: the bird's colour, how hard it's flapping and its pose. */
public class BudgieRenderState extends LivingEntityRenderState {
	public enum Pose {
		FLYING, STANDING, SITTING
	}

	public Budgie.Colour colour = Budgie.Colour.GREEN;
	public float flapAngle;
	public Pose pose = Pose.STANDING;
}
