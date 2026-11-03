package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.Budgie;
import com.gabbymaybelle.littlebirds.LittleBirds;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Draws a budgie. For now it borrows vanilla's parrot model and poses with budgie textures, so the
 * bird flies, stands and sits on screen while its own model is drawn; the model is the next step.
 */
public class BudgieRenderer extends MobRenderer<Budgie, ParrotRenderState, ParrotModel> {
	private static final Identifier[] TEXTURES = {
			LittleBirds.id("textures/entity/budgie/budgie_green.png"),
			LittleBirds.id("textures/entity/budgie/budgie_blue.png"),
			LittleBirds.id("textures/entity/budgie/budgie_yellow.png"),
			LittleBirds.id("textures/entity/budgie/budgie_white.png")
	};

	public BudgieRenderer(EntityRendererProvider.Context context) {
		super(context, new ParrotModel(context.bakeLayer(ModelLayers.PARROT)), 0.25F);
	}

	/** The render state's own variant field is the parrot's; the budgie's colour rides in {@link BudgieRenderState}. */
	@Override
	public Identifier getTextureLocation(ParrotRenderState state) {
		return TEXTURES[state instanceof BudgieRenderState budgie ? budgie.colour.ordinal() : 0];
	}

	@Override
	public ParrotRenderState createRenderState() {
		return new BudgieRenderState();
	}

	@Override
	public void extractRenderState(Budgie bird, ParrotRenderState state, float partialTicks) {
		super.extractRenderState(bird, state, partialTicks);
		if (state instanceof BudgieRenderState budgie) {
			budgie.colour = bird.getColour();
		}
		float flap = Mth.lerp(partialTicks, bird.oFlap, bird.flap);
		float flapSpeed = Mth.lerp(partialTicks, bird.oFlapSpeed, bird.flapSpeed);
		state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
		state.pose = bird.isInSittingPose() ? ParrotModel.Pose.SITTING : bird.isFlying() ? ParrotModel.Pose.FLYING : ParrotModel.Pose.STANDING;
	}

	/** The parrot's render state plus the budgie's colour. */
	public static class BudgieRenderState extends ParrotRenderState {
		public Budgie.Colour colour = Budgie.Colour.GREEN;
	}
}
