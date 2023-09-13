package fr.dynamx.common.entities.modules.movables;

import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;

@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class PickObjects extends MovableModule {

    private Point2PointJoint joint;
    @SynchronizedEntityVariable(name = "mover")
    private final EntityVariable<EntityPlayer> mover = new EntityVariable<>((variable, value) -> {
        if(value != null && DynamXContext.getPlayerPickingObjects().containsKey(value.getEntityId()))
            entity.getSynchronizer().onPlayerStartControlling(value, false);
    }, SynchronizationRules.SERVER_TO_CLIENTS);
    @SynchronizedEntityVariable(name = "pickDistance")
    @Getter
    private final EntityVariable<Float> pickDistance = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, 0f);
    @Getter
    private PhysicsRigidBody hitBody;
    private float initialMass;
    @SynchronizedEntityVariable(name = "localPickPosition")
    private final EntityVariable<Vector3f> localPickPosition = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, new Vector3f());

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
        if (mover.get() == null) {
            Vector3f localPickPos = DynamXPhysicsHelper.getBodyLocalPoint(rayCastHitBody, rayCastHitPos);

            setLocalPickPosition(localPickPos);
            mover.set(playerPicking);
            hitBody = rayCastHitBody;
            this.pickDistance.set(pickDistance);
            if (rayCastHitBody.getMass() > 0) {
                initialMass = rayCastHitBody.getMass();
            } else {
                rayCastHitBody.setMass(initialMass);
            }

            DynamXContext.getPlayerPickingObjects().put(playerPicking.getEntityId(), rayCastHitEntity.getEntityId());

            DynamXContext.getPhysicsWorld(playerPicking.world).schedule(() -> JointHandlerRegistry.createJointWithSelf(JOINT_NAME, rayCastHitEntity, (byte) 0));

            entity.getSynchronizer().onPlayerStartControlling(mover.get(), false);
        }
    }

    public void unPickObject() {
        if (mover.get() != null && entity.getJointsHandler() != null) {
            DynamXContext.getPlayerPickingObjects().remove(mover.get().getEntityId());
            entity.getJointsHandler().removeJointWith(entity, MovableModule.JOINT_NAME, (byte) 0);
        }
    }

    @Override
    public void preUpdatePhysics(boolean b) {
        EntityPlayer mover = this.mover.get();
        if (b && joint != null && mover != null) {
            Vector3fPool.openPool();
            Vector3f playerPosition = Vector3fPool.get(
                    (float) mover.posX,
                    (float) mover.posY + mover.getEyeHeight(),
                    (float) mover.posZ);
            Vector3f pickRaw = DynamXUtils.calculateRay(mover, 64, Vector3fPool.get());

            Vector3f newRayTo = Vector3fPool.get(pickRaw);
            Vector3f eyePos = Vector3fPool.get(playerPosition);
            Vector3f dir = newRayTo.subtractLocal(eyePos.x, eyePos.y, eyePos.z);
            dir = dir.normalize();
            dir.multLocal(pickDistance.get());

            Vector3f newPos = eyePos.addLocal(dir);
            joint.setPivotInB(newPos);
            Vector3fPool.closePool();
        }
    }

    @Override
    public boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId) {
        return jointId == 0 && joint == null;
    }

    @Override
    public void onJointDestroyed(EntityJoint<?> joint) {
        if (mover.get() != null) {
            entity.getSynchronizer().onPlayerStopControlling(mover.get(), false);
        }
        this.joint = null;
        if (mover.get() != null) {
            DynamXContext.getPlayerPickingObjects().remove(mover.get().getEntityId());
            mover.set(null);
        }
    }

    public Vector3f getLocalPickPosition() {
        return localPickPosition.get();
    }

    public void setLocalPickPosition(Vector3f localPickPosition) {
        this.localPickPosition.set(localPickPosition);
    }

}
