package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.Canary;
import com.gabbymaybelle.littlebirds.LittleBirds;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Draws a canary with {@link CanaryModel}, in the texture of its colour. */
public class CanaryRenderer extends BirdRenderer<Canary, CanaryModel> {
	private static final Identifier[] TEXTURES = {
			LittleBirds.id("textures/entity/canary/canary_yellow.png"),
			LittleBirds.id("textures/entity/canary/canary_red.png"),
			LittleBirds.id("textures/entity/canary/canary_white.png")
	};

	public CanaryRenderer(EntityRendererProvider.Context context) {
		super(context, new CanaryModel(context.bakeLayer(CanaryModel.LAYER)), 0.2F, TEXTURES);
	}
}
