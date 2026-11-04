package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.Cockatiel;
import com.gabbymaybelle.littlebirds.LittleBirds;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Draws a cockatiel with {@link CockatielModel}, in the texture of its colour. */
public class CockatielRenderer extends BirdRenderer<Cockatiel, CockatielModel> {
	private static final Identifier[] TEXTURES = {
			LittleBirds.id("textures/entity/cockatiel/cockatiel_grey.png"),
			LittleBirds.id("textures/entity/cockatiel/cockatiel_lutino.png"),
			LittleBirds.id("textures/entity/cockatiel/cockatiel_whiteface.png")
	};

	public CockatielRenderer(EntityRendererProvider.Context context) {
		super(context, new CockatielModel(context.bakeLayer(CockatielModel.LAYER)), 0.3F, TEXTURES);
	}
}
