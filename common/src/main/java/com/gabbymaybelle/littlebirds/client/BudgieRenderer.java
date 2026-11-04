package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.Budgie;
import com.gabbymaybelle.littlebirds.LittleBirds;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Draws a budgie with {@link BudgieModel}, in the texture of its colour. */
public class BudgieRenderer extends BirdRenderer<Budgie, BudgieModel> {
	private static final Identifier[] TEXTURES = {
			LittleBirds.id("textures/entity/budgie/budgie_green.png"),
			LittleBirds.id("textures/entity/budgie/budgie_blue.png"),
			LittleBirds.id("textures/entity/budgie/budgie_yellow.png"),
			LittleBirds.id("textures/entity/budgie/budgie_white.png")
	};

	public BudgieRenderer(EntityRendererProvider.Context context) {
		super(context, new BudgieModel(context.bakeLayer(BudgieModel.LAYER)), 0.25F, TEXTURES);
	}
}
