package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.LittleBirds;
import com.gabbymaybelle.littlebirds.Lovebird;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Draws a lovebird with {@link LovebirdModel}, in the texture of its colour. */
public class LovebirdRenderer extends BirdRenderer<Lovebird, LovebirdModel> {
	private static final Identifier[] TEXTURES = {
			LittleBirds.id("textures/entity/lovebird/lovebird_peach.png"),
			LittleBirds.id("textures/entity/lovebird/lovebird_fischer.png"),
			LittleBirds.id("textures/entity/lovebird/lovebird_blue.png")
	};

	public LovebirdRenderer(EntityRendererProvider.Context context) {
		super(context, new LovebirdModel(context.bakeLayer(LovebirdModel.LAYER)), 0.25F, TEXTURES);
	}
}
