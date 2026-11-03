package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.Budgie;
import com.gabbymaybelle.littlebirds.LittleBirds;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Draws a budgie with {@link BudgieModel}, in the texture of its colour. */
public class BudgieRenderer extends MobRenderer<Budgie, BudgieRenderState, BudgieModel> {
	private static final Identifier[] TEXTURES = {
			LittleBirds.id("textures/entity/budgie/budgie_green.png"),
			LittleBirds.id("textures/entity/budgie/budgie_blue.png"),
			LittleBirds.id("textures/entity/budgie/budgie_yellow.png"),
			LittleBirds.id("textures/entity/budgie/budgie_white.png")
	};

	public BudgieRenderer(EntityRendererProvider.Context context) {
		super(context, new BudgieModel(context.bakeLayer(BudgieModel.LAYER)), 0.25F);
	}

	@Override
	public Identifier getTextureLocation(BudgieRenderState state) {
		return TEXTURES[state.colour.ordinal()];
	}

	@Override
	public BudgieRenderState createRenderState() {
		return new BudgieRenderState();
	}

	@Override
	public void extractRenderState(Budgie bird, BudgieRenderState state, float partialTicks) {
		super.extractRenderState(bird, state, partialTicks);
		state.colour = bird.getColour();
		float flap = Mth.lerp(partialTicks, bird.oFlap, bird.flap);
		float flapSpeed = Mth.lerp(partialTicks, bird.oFlapSpeed, bird.flapSpeed);
		state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
		state.pose = bird.isInSittingPose() ? BudgieRenderState.Pose.SITTING : bird.isFlying() ? BudgieRenderState.Pose.FLYING : BudgieRenderState.Pose.STANDING;
	}
}
