package fr.dynamx.common.entities.modules.movables;

import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.entity.player.EntityPlayer;

public class PickObjects extends MovableModule {

    public Point2PointJoint joint;
    public EntityPlayer mover;
    public float pickDistance;
    public PhysicsRigidBody hitBody;
    public float initialMass;
    private Vector3f localPickPosition = new Vector3f();

    public PickObjects(PhysicsEntity<?> entity) {
        super(entity);
    }

    public Point2PointJoint createWeldJoint() {
        joint = new Point2PointJoint(hitBody != null ? hitBody : (PhysicsRigidBody) entity.physicsHandler.getCollisionObject(), getLocalPickPosition());

        joint.setImpulseClamp(30f);
        joint.setTau(0.001f);

        return joint;
    }

    public void pickObject(EntityPlayer playerPicking, PhysicsEntity<?> rayCastHitEntity, PhysicsRigidBody rayCastHitBody, Vector3f rayCastHitPos, float pickDistance) {
        if (mover == null) {
            Vector3f localPickPos = DynamXPhysicsHelper.getBodyLocalPoint(rayCastHitBody, rayCastHitPos);

            this.setLocalPickPosition(localPickPos);
            this.mover = playerPicking;
            this.pickDistance = pickDistance;
            this.hitBody = rayCastHitBody;
            if (rayCastHitBody.getMass() > 0) {
                this.initialMass = rayCastHitBody.getMass();
            } else {
                rayCastHitBody.setMass(this.initialMass);
            }

            DynamXContext.getPlayerPickingObjects().put(playerPicking.getEntityId(), rayCastHitEntity.getEntityId());

            DynamXContext.getPhysicsWorld(playerPicking.world).schedule(() -> JointHandlerRegistry.createJointWithSelf(JOINT_NAME, rayCastHitEntity, (byte) 0));

            entity.getNetwork().onPlayerStartControlling(mover, false);
        }
    }

    public void unPickObject() {
        if (mover != null && entity.getJointsHandler() != null) {
            DynamXContext.getPlayerPickingObjects().remove(mover.getEntityId());
            entity.getJointsHandler().removeJointWith(entity, MovableModule.JOINT_NAME, (byte) 0);
        }
    }

    @Override
    public void preUpdatePhysics(boolean b) {
        //System.out.println("Pd pos "+joint+" "+mover);
        if (joint != null && mover != null) {
            Vector3fPool.openPool();
            Vector3f playerPosition = Vector3fPool.get(
                    (float) mover.posX,
                    (float) mover.posY + mover.getEyeHeight(),
                    (float) mover.posZ);
            Vector3f pickRaw = DynamXUtils.calculateRay(mover, 64, Vector3fPool.get());

            Vector3f newRayTo = Vector3fPool.get(pickRaw);
            Vector3f eyePos = Vector3fPool.get(playerPosition);
            Vector3f dir = newRayTo.subtract(eyePos);
            dir = dir.normalize();
            dir.multLocal(pickDistance);

            Vector3f newPos = eyePos.add(dir);
            joint.setPivotInB(newPos);
            Vector3fPool.closePool();

            /*if(!(mover.getHeldItemMainhand().getItem() instanceof ItemWrench)){
                if(entity.getJointsHandler() != null){
                    entity.getJointsHandler().removeJointWith(JOINT_NAME,entity, getJointID());
                    mover = null;
                }
            }*/
        }
    }

    @Override
    public boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId) {
        return jointId == 0 && joint == null;
    }

    @Override
    public void onJointDestroyed(EntityJoint<?> joint) {
        if (mover != null) {
            entity.getNetwork().onPlayerStopControlling(mover, false);
        }
        this.joint = null;
        if (this.mover != null) {
            DynamXContext.getPlayerPickingObjects().remove(mover.getEntityId());
            this.mover = null;
        }
    }

    public Vector3f getLocalPickPosition() {
        return localPickPosition;
    }

    public void setLocalPickPosition(Vector3f localPickPosition) {
        this.localPickPosition = localPickPosition;
    }

}
