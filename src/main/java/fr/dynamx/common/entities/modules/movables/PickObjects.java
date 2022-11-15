package fr.dynamx.common.entities.modules.movables;

import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.v3.SynchronizationRules;
import fr.dynamx.api.network.sync.v3.SynchronizedEntityVariable;
import fr.dynamx.api.network.sync.v3.SynchronizedVariableSerializer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class PickObjects extends MovableModule {

    //TODO PRIVATISER

    public Point2PointJoint joint;
    public final SynchronizedEntityVariable<EntityPlayer> mover = new SynchronizedEntityVariable<>((variable, value) -> {
        if(value != null)
            entity.getSynchronizer().onPlayerStartControlling(value, false);
    }, SynchronizationRules.SERVER_TO_CLIENTS, new SynchronizedVariableSerializer<EntityPlayer>() {
        @Override
        public void writeObject(ByteBuf buffer, EntityPlayer object) {
            buffer.writeInt(object == null ? -1 : object.getEntityId());
        }

        @Override
        public EntityPlayer readObject(ByteBuf buffer, EntityPlayer currentValue) {
            //TODO RENDRE SAFE et mettre dans factory
            int id = buffer.readInt();
            if(id == -1)
                return null;
            if (DynamXContext.getPlayerPickingObjects().containsKey(id))
                return (EntityPlayer) Minecraft.getMinecraft().world.getEntityByID(id);
            return currentValue;
        }
    }, "mover");
    public final SynchronizedEntityVariable<Float> pickDistance = new SynchronizedEntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, null, 0f, "pickDistance");
    public PhysicsRigidBody hitBody;
    public float initialMass;
    public final SynchronizedEntityVariable<Vector3f> localPickPosition = new SynchronizedEntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, null, new Vector3f(), "localPickPosition");

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

            this.setLocalPickPosition(localPickPos);
            this.mover.set(playerPicking);
            this.pickDistance.set(pickDistance);
            this.hitBody = rayCastHitBody;
            if (rayCastHitBody.getMass() > 0) {
                this.initialMass = rayCastHitBody.getMass();
            } else {
                rayCastHitBody.setMass(this.initialMass);
            }

            DynamXContext.getPlayerPickingObjects().put(playerPicking.getEntityId(), rayCastHitEntity.getEntityId());

            DynamXContext.getPhysicsWorld().schedule(() -> JointHandlerRegistry.createJointWithSelf(JOINT_NAME, rayCastHitEntity, (byte) 0));

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
        //System.out.println("Pd pos "+joint+" "+mover);
        EntityPlayer mover = this.mover.get();
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
            dir.multLocal(pickDistance.get());

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
        if (mover.get() != null) {
            entity.getSynchronizer().onPlayerStopControlling(mover.get(), false);
        }
        this.joint = null;
        if (this.mover.get() != null) {
            DynamXContext.getPlayerPickingObjects().remove(mover.get().getEntityId());
            this.mover.set(null);
        }
    }

    public Vector3f getLocalPickPosition() {
        return localPickPosition.get();
    }

    public void setLocalPickPosition(Vector3f localPickPosition) {
        this.localPickPosition.set(localPickPosition);
    }

}
