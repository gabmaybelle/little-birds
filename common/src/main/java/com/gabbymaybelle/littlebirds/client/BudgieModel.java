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
 * The budgie's model, built the way vanilla builds its parrot rather than as a stack of cubes: a
 * body leaning forward, a small head with a longer skull cap pushed forward (the round budgie
 * forehead), a two-piece beak, thin wings lying along the body at an angle, a flat tail plate and
 * two short legs. About three quarters of a parrot. The texture is 32x32; {@code tools/draw-art.py}
 * paints it box by box in the same layout.
 */
public class BudgieModel extends EntityModel<BirdRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(LittleBirds.id("budgie"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart tail;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public BudgieModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
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
				CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, 17.6F, -2.4F, 0.45F, 0.0F, 0.0F));
		root.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(20, 6).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 21.3F, 0.9F, 1.05F, 0.0F, 0.0F));
		root.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(12, 6).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 4.0F, 3.0F),
				PartPose.offsetAndRotation(1.5F, 18.0F, -2.2F, -0.6F, (float) -Math.PI, 0.0F));
		root.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(12, 6).mirror().addBox(-0.5F, 0.0F, -1.5F, 1.0F, 4.0F, 3.0F),
				PartPose.offsetAndRotation(-1.5F, 18.0F, -2.2F, -0.6F, (float) -Math.PI, 0.0F));
		// The head is a solid 2x3x3 (deeper than the parrot's, for the full budgie face), with the
		// round forehead as a cap on top and a small hooked beak set into the middle of the face.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F),
				PartPose.offset(0.0F, 16.9F, -2.1F));
		head.addOrReplaceChild("cap",
				CubeListBuilder.create().texOffs(10, 0).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F),
				PartPose.offset(0.0F, -2.0F, 0.0F));
		head.addOrReplaceChild("beak_top",
				CubeListBuilder.create().texOffs(20, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
				PartPose.offset(0.0F, -0.4F, -1.8F));
		head.addOrReplaceChild("beak_tip",
				CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F),
				PartPose.offset(0.0F, 0.1F, -1.6F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(12, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		root.addOrReplaceChild("left_leg", leg, PartPose.offset(0.8F, 22.0F, -1.3F));
		root.addOrReplaceChild("right_leg", leg, PartPose.offset(-0.8F, 22.0F, -1.3F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(BirdRenderState state) {
		super.setupAnim(state);
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		float bob = state.flapAngle * 0.3F;
		if (state.greeting > 0.0F) {
			// Hello: quick head bobs and a flutter of the wings, for a second and a half.
			float wave = Mth.sin(state.greeting * 0.9F);
			this.head.y += Math.abs(wave) * 1.2F;
			this.head.xRot += wave * 0.25F;
			this.leftWing.zRot = -0.0873F - Math.abs(wave) * 0.5F;
			this.rightWing.zRot = 0.0873F + Math.abs(wave) * 0.5F;
		}
		switch (state.pose) {
			case STANDING -> {
				// Little steps, and the head bobs along with them the way a walking budgie's does.
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
				// Wings beat out from the body; legs tuck back under the tail.
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
				// Settled down over the feet, wings folded, tail resting lower.
				if (state.pose == BirdRenderState.Pose.ROOSTING) {
					// Asleep: the head sinks into the shoulders, turned a little to one side, and stops following players.
					this.head.xRot = 0.5F;
					this.head.yRot = 0.6F;
					this.head.y += 0.8F;
				}
				this.body.y += 1.4F;
				this.head.y += 1.4F;
				this.tail.y += 1.4F;
				this.tail.xRot += 0.3F;
				this.leftWing.y += 1.4F;
				this.rightWing.y += 1.4F;
				if (state.greeting <= 0.0F) {
					this.leftWing.zRot = -0.0873F;
					this.rightWing.zRot = 0.0873F;
				}
				this.leftLeg.y += 1.4F;
				this.rightLeg.y += 1.4F;
				this.leftLeg.xRot += Mth.HALF_PI;
				this.rightLeg.xRot += Mth.HALF_PI;
			}
		}
	}
}
