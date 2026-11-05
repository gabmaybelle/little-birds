package com.gabbymaybelle.littlebirds.client;

import com.gabbymaybelle.littlebirds.LittleBirds;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The cockatiel's model: the budgie's build at a parrot's size, with a longer tail and a crest,
 * a thin plate hinged at the back of the skull that lies swept back when the bird is calm and
 * stands up when it says hello or takes off. The texture is 64x32; {@code tools/draw-art.py}
 * paints it box by box in the same layout.
 */
public class CockatielModel extends EntityModel<BirdRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(LittleBirds.id("cockatiel"), "main");

	/** The crest's angle swept back (calm) and standing (alert), as x rotations of the hinge. */
	private static final float CREST_CALM = -1.1F;
	private static final float CREST_UP = -0.25F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart crest;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart tail;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public CockatielModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.crest = this.head.getChild("crest");
		this.leftWing = root.getChild("left_wing");
		this.rightWing = root.getChild("right_wing");
		this.tail = root.getChild("tail");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// y runs down from 0 at the top of a block to 24 at the ground. Texture offsets are the
		// layout tools/draw-art.py paints.
		root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, 16.4F, -3.0F, 0.45F, 0.0F, 0.0F));
		root.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(30, 12).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 7.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 20.8F, 1.4F, 1.05F, 0.0F, 0.0F));
		root.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(18, 12).addBox(-0.5F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F),
				PartPose.offsetAndRotation(2.0F, 16.9F, -2.8F, -0.6F, (float) -Math.PI, 0.0F));
		root.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(18, 12).mirror().addBox(-0.5F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F),
				PartPose.offsetAndRotation(-2.0F, 16.9F, -2.8F, -0.6F, (float) -Math.PI, 0.0F));
		// The head: a 3x4x3 block with a cap for the round forehead, the crest hinged at the top back,
		// and the two-piece hooked beak set into the face.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 4.0F, 3.0F),
				PartPose.offset(0.0F, 15.4F, -2.6F));
		head.addOrReplaceChild("cap",
				CubeListBuilder.create().texOffs(14, 0).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 1.0F, 3.0F),
				PartPose.offset(0.0F, -2.5F, 0.0F));
		head.addOrReplaceChild("crest",
				CubeListBuilder.create().texOffs(26, 0).addBox(-0.5F, -4.0F, -1.0F, 1.0F, 4.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, -3.0F, 0.5F, CREST_CALM, 0.0F, 0.0F));
		head.addOrReplaceChild("beak_top",
				CubeListBuilder.create().texOffs(34, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
				PartPose.offset(0.0F, -0.6F, -1.8F));
		head.addOrReplaceChild("beak_tip",
				CubeListBuilder.create().texOffs(38, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F),
				PartPose.offset(0.0F, -0.1F, -1.6F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(40, 12).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F);
		root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.0F, 21.0F, -1.6F));
		root.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.0F, 21.0F, -1.6F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(BirdRenderState state) {
		super.setupAnim(state);
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		if (state.shaking > 0.0F) {
			// No, thank you: a quick shake of the head.
			this.head.yRot += Mth.sin(state.shaking * 1.3F) * 0.5F;
		}
		float bob = state.flapAngle * 0.3F;
		boolean alert = state.greeting > 0.0F || state.pose == BirdRenderState.Pose.FLYING;
		this.crest.xRot = alert ? CREST_UP : CREST_CALM;
		if (state.greeting > 0.0F) {
			// Hello: the crest stands, the head lifts and bobs, the wings flutter.
			float wave = Mth.sin(state.greeting * 0.7F);
			this.head.y += Math.abs(wave) * 1.0F;
			this.head.xRot += wave * 0.2F - 0.15F;
			this.leftWing.zRot = -0.0873F - Math.abs(wave) * 0.4F;
			this.rightWing.zRot = 0.0873F + Math.abs(wave) * 0.4F;
		}
		switch (state.pose) {
			case STANDING -> {
				float step = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
				this.leftLeg.xRot += step;
				this.rightLeg.xRot -= step;
				this.head.y += Math.abs(Mth.sin(state.walkAnimationPos * 0.6662F)) * 0.6F * state.walkAnimationSpeed;
				if (state.greeting <= 0.0F) {
					this.leftWing.zRot = -0.0873F - state.flapAngle;
					this.rightWing.zRot = 0.0873F + state.flapAngle;
				}
			}
			case FLYING -> {
				this.leftWing.zRot = -0.0873F - state.flapAngle * 1.6F;
				this.rightWing.zRot = 0.0873F + state.flapAngle * 1.6F;
				this.leftLeg.xRot += 0.7F;
				this.rightLeg.xRot += 0.7F;
				this.body.y += bob;
				this.head.y += bob;
				this.tail.y += bob;
				this.leftWing.y += bob;
				this.rightWing.y += bob;
			}
			case SITTING, ROOSTING -> {
				if (state.pose == BirdRenderState.Pose.ROOSTING) {
					// Asleep: the head sinks and turns, the crest lies flat.
					this.head.xRot = 0.5F;
					this.head.yRot = 0.6F;
					this.head.y += 0.8F;
					this.crest.xRot = CREST_CALM - 0.3F;
				}
				this.body.y += 2.0F;
				this.head.y += 2.0F;
				this.tail.y += 2.0F;
				this.tail.xRot += 0.3F;
				this.leftWing.y += 2.0F;
				this.rightWing.y += 2.0F;
				if (state.greeting <= 0.0F) {
					this.leftWing.zRot = -0.0873F;
					this.rightWing.zRot = 0.0873F;
				}
				this.leftLeg.y += 2.0F;
				this.rightLeg.y += 2.0F;
				this.leftLeg.xRot += Mth.HALF_PI;
				this.rightLeg.xRot += Mth.HALF_PI;
			}
		}
	}
}
