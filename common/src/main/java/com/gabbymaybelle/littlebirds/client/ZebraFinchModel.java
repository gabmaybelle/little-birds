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
 * The zebra finch's model: the canary's finch build at the smallest size, with a rounder body,
 * a short square tail and the stubby red beak. When it sings the head lifts, the beak opens and
 * closes, and the tail flicks. The texture is 32x32; {@code tools/draw-art.py} paints it box by
 * box in the same layout.
 */
public class ZebraFinchModel extends EntityModel<BirdRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(LittleBirds.id("zebra_finch"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart beak;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart tail;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public ZebraFinchModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.beak = this.head.getChild("beak");
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
				CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, 19.6F, -1.5F, 0.5F, 0.0F, 0.0F));
		// A short square tail.
		root.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(22, 6).addBox(-1.0F, -0.5F, -0.5F, 2.0F, 2.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 21.6F, 0.7F, 1.2F, 0.0F, 0.0F));
		root.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(12, 6).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 2.0F, 3.0F),
				PartPose.offsetAndRotation(1.5F, 19.9F, -1.3F, -0.6F, (float) -Math.PI, 0.0F));
		root.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(12, 6).mirror().addBox(-0.5F, 0.0F, -1.5F, 1.0F, 2.0F, 3.0F),
				PartPose.offsetAndRotation(-1.5F, 19.9F, -1.3F, -0.6F, (float) -Math.PI, 0.0F));
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
				PartPose.offset(0.0F, 19.0F, -1.9F));
		head.addOrReplaceChild("cap",
				CubeListBuilder.create().texOffs(10, 0).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F),
				PartPose.offset(0.0F, -1.5F, 0.0F));
		head.addOrReplaceChild("beak",
				CubeListBuilder.create().texOffs(20, 0).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 1.0F),
				PartPose.offset(0.0F, 0.2F, -1.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(12, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		root.addOrReplaceChild("left_leg", leg, PartPose.offset(0.6F, 22.0F, -0.9F));
		root.addOrReplaceChild("right_leg", leg, PartPose.offset(-0.6F, 22.0F, -0.9F));
		return LayerDefinition.create(mesh, 32, 32);
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
		if (state.greeting > 0.0F) {
			// Hello: quick head bobs and a flutter of the wings.
			float wave = Mth.sin(state.greeting * 0.9F);
			this.head.y += Math.abs(wave) * 0.9F;
			this.head.xRot += wave * 0.25F;
			this.leftWing.zRot = -0.0873F - Math.abs(wave) * 0.5F;
			this.rightWing.zRot = 0.0873F + Math.abs(wave) * 0.5F;
		}
		if (state.singing > 0.0F) {
			// A phrase: the head lifts, the beak opens and closes quickly, the tail flicks.
			float beat = Mth.sin(state.singing * 1.8F);
			this.head.xRot -= 0.35F;
			this.head.y -= 0.2F;
			this.beak.xRot = Math.abs(beat) * 0.35F;
			this.tail.xRot += Math.abs(Mth.sin(state.singing * 0.5F)) * 0.3F;
		}
		switch (state.pose) {
			case STANDING -> {
				float step = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
				this.leftLeg.xRot += step;
				this.rightLeg.xRot -= step;
				this.head.y += Math.abs(Mth.sin(state.walkAnimationPos * 0.6662F)) * 0.5F * state.walkAnimationSpeed;
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
					// Asleep: the head sinks into the shoulders, turned a little to one side.
					this.head.xRot = 0.5F;
					this.head.yRot = 0.6F;
					this.head.y += 0.6F;
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
