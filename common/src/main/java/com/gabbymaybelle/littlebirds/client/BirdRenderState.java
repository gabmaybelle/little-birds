package com.gabbymaybelle.littlebirds.client;


import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the renderer hands the model each frame: which of its colours it is, how hard it's flapping, its pose, and whether it's saying hello. */
public class BirdRenderState extends LivingEntityRenderState {
	public enum Pose {
		FLYING, STANDING, SITTING,
		/** Asleep on its perch: sitting, with the head tucked down. */
		ROOSTING
	}

	public int variant;
	public float flapAngle;
	public Pose pose = Pose.STANDING;
	/** How far into its greeting the bird is, in ticks with the frame's fraction; 0 when it isn't greeting. */
	public float greeting;
	/** How far into a head shake (a refused food) the bird is, in ticks; 0 when it isn't shaking. */
	public float shaking;
	/** How far into its song the bird is, in ticks; 0 when it isn't singing. */
	public float singing;
	/** On the shoulder of the player looking out of their own eyes: not drawn. */
	public boolean hidden;
}
