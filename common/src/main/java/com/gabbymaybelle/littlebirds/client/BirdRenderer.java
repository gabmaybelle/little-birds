package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.LittleBird;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Draws a little bird with its model, in the texture of its colour; each bird gives its model and textures. */
public class BirdRenderer<T extends LittleBird, M extends EntityModel<BirdRenderState>> extends MobRenderer<T, BirdRenderState, M> {
	private final Identifier[] textures;

	public BirdRenderer(EntityRendererProvider.Context context, M model, float shadow, Identifier[] textures) {
		super(context, model, shadow);
		this.textures = textures;
	}

	@Override
	public Identifier getTextureLocation(BirdRenderState state) {
		return this.textures[Mth.clamp(state.variant, 0, this.textures.length - 1)];
	}

	@Override
	public BirdRenderState createRenderState() {
		return new BirdRenderState();
	}

	@Override
	public void extractRenderState(T bird, BirdRenderState state, float partialTicks) {
		super.extractRenderState(bird, state, partialTicks);
		state.variant = bird.getVariant();
		float flap = Mth.lerp(partialTicks, bird.oFlap, bird.flap);
		float flapSpeed = Mth.lerp(partialTicks, bird.oFlapSpeed, bird.flapSpeed);
		state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
		state.pose = bird.isRoosting() ? BirdRenderState.Pose.ROOSTING
				: bird.isInSittingPose() || bird.isOnShoulder() ? BirdRenderState.Pose.SITTING
				: bird.isFlying() ? BirdRenderState.Pose.FLYING : BirdRenderState.Pose.STANDING;
		state.greeting = bird.greetTicks > 0 ? bird.greetTicks - partialTicks : 0.0F;
		Minecraft minecraft = Minecraft.getInstance();
		state.hidden = bird.isOnShoulder() && bird.getOwner() == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
	}

	/** A chick is drawn at its age's size (vanilla's models do this inside the model; these do it here). */
	@Override
	protected void scale(BirdRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		if (state.ageScale != 1.0F) {
			poseStack.scale(state.ageScale, state.ageScale, state.ageScale);
		}
	}

	/** A bird on your own shoulder isn't drawn while you look out of your own eyes, like vanilla's shoulder parrots. */
	@Override
	public void submit(BirdRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.hidden) {
			super.submit(state, poseStack, collector, camera);
		}
	}
}
