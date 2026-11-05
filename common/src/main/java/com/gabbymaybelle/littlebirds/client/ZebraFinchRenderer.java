package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.LittleBirds;
import com.gabbymaybelle.littlebirds.ZebraFinch;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Draws a zebra finch with {@link ZebraFinchModel}, in the texture of its colour. */
public class ZebraFinchRenderer extends BirdRenderer<ZebraFinch, ZebraFinchModel> {
	private static final Identifier[] TEXTURES = {
			LittleBirds.id("textures/entity/zebra_finch/zebra_finch_grey.png"),
			LittleBirds.id("textures/entity/zebra_finch/zebra_finch_fawn.png"),
			LittleBirds.id("textures/entity/zebra_finch/zebra_finch_white.png")
	};

	public ZebraFinchRenderer(EntityRendererProvider.Context context) {
		super(context, new ZebraFinchModel(context.bakeLayer(ZebraFinchModel.LAYER)), 0.18F, TEXTURES);
	}
}
