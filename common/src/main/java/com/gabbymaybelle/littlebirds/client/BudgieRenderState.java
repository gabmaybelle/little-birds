package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.Budgie;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the renderer hands the model each frame: the bird's colour, how hard it's flapping, its pose, and whether it's saying hello. */
public class BudgieRenderState extends LivingEntityRenderState {
	public enum Pose {
		FLYING, STANDING, SITTING,
		/** Asleep on its perch: sitting, with the head tucked down. */
		ROOSTING
	}

	public Budgie.Colour colour = Budgie.Colour.GREEN;
	public float flapAngle;
	public Pose pose = Pose.STANDING;
	/** How far into its greeting the bird is, in ticks with the frame's fraction; 0 when it isn't greeting. */
	public float greeting;
	/** On the shoulder of the player looking out of their own eyes: not drawn. */
	public boolean hidden;
}
