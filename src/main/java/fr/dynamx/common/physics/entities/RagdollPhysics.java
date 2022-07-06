package fr.dynamx.common.physics.entities;


import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.JointHandler;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;

public class RagdollPhysics<T extends RagdollEntity> extends EntityPhysicsHandler<T> implements IPhysicsModule<EntityPhysicsHandler<T>>
{
    private HashMap<EnumRagdollBodyPart, PhysicsRigidBody> bodyParts;

    public RagdollPhysics(T entity) {
        super(entity);
    }

    public static Point2PointJoint createBodyPartJoint(RagdollEntity ragdollEntity, EnumRagdollBodyPart bodyPart) {
        Point2PointJoint joint = new Point2PointJoint(ragdollEntity.physicsHandler.getBodyParts().get(EnumRagdollBodyPart.CHEST), ragdollEntity.physicsHandler.getBodyParts().get(bodyPart),
                Vector3fPool.get(bodyPart.getChestAttachPoint().x, bodyPart.getChestAttachPoint().y, bodyPart.getChestAttachPoint().z), bodyPart.getBodyPartAttachPoint());
        joint.setDamping(1);
        // marche pas avec la sync. todo : activer en solo ? joint.setBreakingImpulseThreshold(350);
       // joint.setCollisionBetweenLinkedBodies(false);

        return joint;
    }

    @Override
    public PhysicsRigidBody createShape(Vector3f position, Quaternion rotation, float spawnRotation) {
        spawnRotation = spawnRotation-180;
        bodyParts = new HashMap<>(); //SHOULD BE INIT HERE BEFORE CALLED BY THE CONSTRUCTOR

        createBodyPart(EnumRagdollBodyPart.CHEST,  position, spawnRotation);
        createBodyPart(EnumRagdollBodyPart.HEAD,  position, spawnRotation);
        createBodyPart(EnumRagdollBodyPart.RIGHT_ARM,  position, spawnRotation);
        createBodyPart(EnumRagdollBodyPart.LEFT_ARM,  position, spawnRotation);
        createBodyPart(EnumRagdollBodyPart.RIGHT_LEG,  position, spawnRotation);
        createBodyPart(EnumRagdollBodyPart.LEFT_LEG,  position, spawnRotation);

        return bodyParts.get(EnumRagdollBodyPart.CHEST);
    }

    private void createBodyPart(EnumRagdollBodyPart enumBodyPart, Vector3f position, float spawnRotation) {
        Quaternion localQuat = new Quaternion().fromAngleNormalAxis((float) Math.toRadians(-spawnRotation), Vector3fPool.get(0,1,0));
        Vector3f pos = DynamXGeometry.rotateVectorByQuaternion(enumBodyPart.getChestAttachPoint().subtract(enumBodyPart.getBodyPartAttachPoint()), localQuat);
        PhysicsRigidBody bodyPart = DynamXPhysicsHelper.fastCreateRigidBody(handledEntity, enumBodyPart.getMass(), new BoxCollisionShape(enumBodyPart.getBoxSize()),
                Vector3fPool.get(position).addLocal(pos), spawnRotation);
        bodyPart.setCcdMotionThreshold(0.1f);
        bodyPart.setCcdSweptSphereRadius(0.1f);
       // bodyPart.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
        bodyParts.put(enumBodyPart, bodyPart);
    }

    public HashMap<EnumRagdollBodyPart, PhysicsRigidBody> getBodyParts() {
        return bodyParts;
    }

    @Override
    public void addToWorld() {
        // chest is already in bodyParts super.addToWorld();
        bodyParts.values().forEach(physicsRigidBody -> DynamXContext.getPhysicsWorld().addCollisionObject(physicsRigidBody));
    }

    @Override
    public void removePhysicsEntity() {
        super.removePhysicsEntity();
        bodyParts.values().forEach(physicsRigidBody -> DynamXContext.getPhysicsWorld().removeCollisionObject(physicsRigidBody));
    }

    @Override
    public Vector3f getPosition() {
        Vector3f vec = Vector3fPool.get(super.getPosition());
        vec.addLocal(DynamXGeometry.rotateVectorByQuaternion(Vector3fPool.get(0, 0f, 0), getRotation()));
        return vec;
    }

    @Override
    public void updatePhysicsState(Vector3f pos, Quaternion rotation, Vector3f linearVel, Vector3f rotationalVel) {
        //pos.addLocal(Trigonometry.rotateVectorByQuaternion(Vector3fPool.get(0, 0f, 0), rotation).multLocal(-1));
       // if(pos.subtract(this.physicsPosition).length() > 0.5 || Trigonometry.angle(Trigonometry.rotateVectorByQuaternion(Vector3fPool.get(1, 0, 0), rotation), Trigonometry.rotateVectorByQuaternion(Vector3fPool.get(1, 0, 0), this.physicsRotation)) > 20)
            //super.updatePhysicsState(pos, rotation, linearVel, rotationalVel);
    }

    @Override
    public void updatePhysicsStateFromNet(Vector3f pos, Quaternion rotation, Vector3f linearVel, Vector3f rotationalVel) {
        //pos.addLocal(Trigonometry.rotateVectorByQuaternion(Vector3fPool.get(0, 0f, 0), rotation).multLocal(-1));
       // if(pos.subtract(this.physicsPosition).length() > 0.5 || Trigonometry.angle(Trigonometry.rotateVectorByQuaternion(Vector3fPool.get(1, 0, 0), rotation), Trigonometry.rotateVectorByQuaternion(Vector3fPool.get(1, 0, 0), this.physicsRotation)) > 20)
         //   super.updatePhysicsStateFromNet(pos, rotation, linearVel, rotationalVel);
    }
}
