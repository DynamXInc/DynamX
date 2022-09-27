package fr.dynamx.common.physics.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.util.ResourceLocation;

public enum EnumRagdollBodyPart {
    HEAD("head_joint", 10, RagdollEntity.HEAD_BOX_SIZE, RagdollEntity.HEAD_BODY_ATTACH_POINT, RagdollEntity.HEAD_ATTACH_POINT),
    CHEST("chest_joint", 30, RagdollEntity.CHEST_BOX_SIZE, new Vector3f(), new Vector3f()),
    RIGHT_ARM("right_arm_joint", 4, RagdollEntity.LIMB_BOX_SIZE, RagdollEntity.RIGHT_ARM_ATTACH_POINT, RagdollEntity.LIMB_ATTACH_POINT),
    LEFT_ARM("left_arm_joint", 4, RagdollEntity.LIMB_BOX_SIZE, RagdollEntity.LEFT_ARM_ATTACH_POINT, RagdollEntity.LIMB_ATTACH_POINT),
    RIGHT_LEG("right_leg_joint", 6, RagdollEntity.LIMB_BOX_SIZE, RagdollEntity.RIGHT_LEG_ATTACH_POINT, RagdollEntity.LIMB_ATTACH_POINT),
    LEFT_LEG("left_leg_joint", 6, RagdollEntity.LIMB_BOX_SIZE, RagdollEntity.LEFT_LEG_ATTACH_POINT, RagdollEntity.LIMB_ATTACH_POINT);

    private final ResourceLocation name;
    private final float mass;
    private final Vector3f boxSize;
    private final Vector3f chestAttachPoint;
    private final Vector3f bodyPartAttachPoint;

    EnumRagdollBodyPart(String name, float mass, Vector3f boxSize, Vector3f chestAttachPoint, Vector3f bodyPartAttachPoint) {
        this.name = new ResourceLocation(DynamXConstants.ID, name);
        this.mass = mass;
        this.boxSize = boxSize;
        this.chestAttachPoint = chestAttachPoint;
        this.bodyPartAttachPoint = bodyPartAttachPoint;
    }

    public ResourceLocation getResourceLocation() {
        return name;
    }

    public float getMass() {
        return mass;
    }

    public Vector3f getBoxSize() {
        return boxSize;
    }

    public Vector3f getChestAttachPoint() {
        return chestAttachPoint;
    }

    public Vector3f getBodyPartAttachPoint() {
        return bodyPartAttachPoint;
    }
}
