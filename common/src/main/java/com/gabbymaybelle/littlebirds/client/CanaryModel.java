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
 * The canary's model: a finch, a touch smaller than the budgie and built the same way (a body
 * leaning forward, a capped head, thin wings along the body), but with a short straight beak
 * instead of a hooked one and a notched tail of two feathers. When it sings the head lifts, the
 * beak opens and closes in time, and the tail flicks. The texture is 32x32;
 * {@code tools/draw-art.py} paints it box by box in the same layout.
 */
public class CanaryModel extends EntityModel<BirdRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(LittleBirds.id("canary"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart beak;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart leftTail;
	private final ModelPart rightTail;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public CanaryModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.beak = this.head.getChild("beak");
		this.leftWing = root.getChild("left_wing");
		this.rightWing = root.getChild("right_wing");
		this.leftTail = root.getChild("left_tail");
		this.rightTail = root.getChild("right_tail");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// y runs down from 0 at the top of a block to 24 at the ground. Texture offsets are the
		// layout tools/draw-art.py paints.
		root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, 18.8F, -1.8F, 0.55F, 0.0F, 0.0F));
		// The notched tail: two feathers side by side, splayed a little apart.
		root.addOrReplaceChild("left_tail",
				CubeListBuilder.create().texOffs(22, 6).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(0.55F, 21.3F, 0.8F, 1.15F, 0.12F, 0.0F));
		root.addOrReplaceChild("right_tail",
				CubeListBuilder.create().texOffs(22, 6).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(-0.55F, 21.3F, 0.8F, 1.15F, -0.12F, 0.0F));
		root.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(12, 6).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 3.0F, 3.0F),
				PartPose.offsetAndRotation(1.5F, 19.2F, -1.6F, -0.65F, (float) -Math.PI, 0.0F));
		root.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(12, 6).mirror().addBox(-0.5F, 0.0F, -1.5F, 1.0F, 3.0F, 3.0F),
				PartPose.offsetAndRotation(-1.5F, 19.2F, -1.6F, -0.65F, (float) -Math.PI, 0.0F));
		// A small round head: a 2x2x2 with a cap for the crown, and a short straight beak set into the face.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
				PartPose.offset(0.0F, 17.9F, -2.1F));
		head.addOrReplaceChild("cap",
				CubeListBuilder.create().texOffs(10, 0).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F),
				PartPose.offset(0.0F, -1.5F, 0.0F));
		head.addOrReplaceChild("beak",
				CubeListBuilder.create().texOffs(20, 0).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 1.0F),
				PartPose.offset(0.0F, 0.2F, -1.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(12, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		root.addOrReplaceChild("left_leg", leg, PartPose.offset(0.7F, 22.0F, -1.0F));
		root.addOrReplaceChild("right_leg", leg, PartPose.offset(-0.7F, 22.0F, -1.0F));
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
			this.head.y += Math.abs(wave) * 1.0F;
			this.head.xRot += wave * 0.25F;
			this.leftWing.zRot = -0.0873F - Math.abs(wave) * 0.5F;
			this.rightWing.zRot = 0.0873F + Math.abs(wave) * 0.5F;
		}
		if (state.singing > 0.0F) {
			// The song: the head lifts, the beak opens and closes in time, and the tail flicks.
			float beat = Mth.sin(state.singing * 1.4F);
			this.head.xRot -= 0.45F;
			this.head.y -= 0.3F;
			this.beak.xRot = Math.abs(beat) * 0.35F;
			float flick = Math.abs(Mth.sin(state.singing * 0.35F)) * 0.25F;
			this.leftTail.xRot += flick;
			this.rightTail.xRot += flick;
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
				this.leftTail.y += bob;
				this.rightTail.y += bob;
				this.leftWing.y += bob;
				this.rightWing.y += bob;
			}
			case SITTING, ROOSTING -> {
				if (state.pose == BirdRenderState.Pose.ROOSTING) {
					// Asleep: the head sinks into the shoulders, turned a little to one side.
					this.head.xRot = 0.5F;
					this.head.yRot = 0.6F;
					this.head.y += 0.7F;
				}
				this.body.y += 1.4F;
				this.head.y += 1.4F;
				this.leftTail.y += 1.4F;
				this.rightTail.y += 1.4F;
				this.leftTail.xRot += 0.3F;
				this.rightTail.xRot += 0.3F;
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
