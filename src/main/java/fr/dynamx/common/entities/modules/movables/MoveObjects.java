package fr.dynamx.common.entities.modules.movables;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.v3.SynchronizationRules;
import fr.dynamx.api.network.sync.v3.SynchronizedEntityVariable;
import fr.dynamx.api.network.sync.v3.SynchronizedVariableSerializer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MoveObjects extends MovableModule {
    //TODO PRIVATISER

    public final SynchronizedEntityVariable<EntityPlayer> picker = new SynchronizedEntityVariable<>((variable, value) -> {
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
    });
    public final SynchronizedEntityVariable<Boolean> isPicked = new SynchronizedEntityVariable<>((variable, value) -> {
        if(picker.get() != null)
            entity.getSynchronizer().onPlayerStopControlling(picker.get(), false);
    }, SynchronizationRules.SERVER_TO_CLIENTS, null, false);
    public final SynchronizedEntityVariable<PhysicsEntity<?>> pickedEntity = new SynchronizedEntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, new SynchronizedVariableSerializer<PhysicsEntity<?>>() {
        @Override
        public void writeObject(ByteBuf buffer, PhysicsEntity<?> object) {
            buffer.writeInt(object == null ? -1 : object.getEntityId());
        }

        @Override
        public PhysicsEntity<?> readObject(ByteBuf buffer, PhysicsEntity<?> currentValue) {
            //TODO RENDRE SAFE et mettre dans factory
            int id = buffer.readInt();
            if(id == -1)
                return null;
            return (PhysicsEntity<?>) Minecraft.getMinecraft().world.getEntityByID(id);
        }
    });
    public Vector3f basePos;

    public MoveObjects(PhysicsEntity<?> entity) {
        super(entity);
    }

    public void pickObject(EntityPlayer picker, PhysicsEntity<?> pickedEntity) {
        if (pickedEntity.getPhysicsHandler() != null) {
            PhysicsCollisionObject rigidBody = pickedEntity.getPhysicsHandler().getCollisionObject();
            if (rigidBody instanceof PhysicsRigidBody && ((PhysicsRigidBody) rigidBody).getMass() < 100) {
                this.picker.set(picker);
                this.pickedEntity.set(pickedEntity);
                this.basePos = rigidBody.getPhysicsLocation(null);

                this.isPicked.set(true);

                DynamXContext.getPlayerPickingObjects().put(picker.getEntityId(), pickedEntity.getEntityId());
                entity.getSynchronizer().onPlayerStartControlling(picker, false);
            }
        }
    }

    public void throwObject(float force) {
        if (picker.get() != null && pickedEntity.get() != null) {
            ((PhysicsRigidBody) entity.getPhysicsHandler().getCollisionObject()).setGravity(DynamXPhysicsHelper.GRAVITY);
            PhysicsRigidBody rigidBody = (PhysicsRigidBody) pickedEntity.get().getPhysicsHandler().getCollisionObject();
            Vector3f playerLookPos = DynamXUtils.toVector3f(picker.get().getLookVec());
            rigidBody.setLinearVelocity(playerLookPos.multLocal(force));
            unPickObject();
        }
    }

    public void unPickObject() {
        if (picker.get() != null) {
            ((PhysicsRigidBody) entity.getPhysicsHandler().getCollisionObject()).setGravity(Vector3fPool.get(DynamXPhysicsHelper.GRAVITY));
            DynamXContext.getPlayerPickingObjects().remove(picker.get().getEntityId());
            isPicked.set(false);
            entity.getSynchronizer().onPlayerStopControlling(picker.get(), false);
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        super.preUpdatePhysics(simulatingPhysics);
        if (simulatingPhysics) {
            PhysicsEntity<?> pickedEntity = this.pickedEntity.get();
            if (picker.get() != null && pickedEntity != null && isPicked.get()) {
                PhysicsRigidBody rigidBody = (PhysicsRigidBody) pickedEntity.getPhysicsHandler().getCollisionObject();
                Vector3f playerLookPos = DynamXUtils.toVector3f(picker.get().getLookVec());
                rigidBody.setGravity(new Vector3f());
                rigidBody.setAngularVelocity(new Vector3f());
                rigidBody.setLinearVelocity(new Vector3f());
                Vector3f playerPos = DynamXUtils.toVector3f(picker.get().getPositionEyes(1.0f));
                Vector3f finalPos = playerPos.add(playerLookPos);

                // Quaternion rotatedQuat = DynamXGeometry.eulerToQuaternion(0, 2 * FastMath.PI - (float) Math.toRadians(picker.rotationYaw), 0);
                rigidBody.setPhysicsLocation(finalPos);
                pickedEntity.getPhysicsHandler().setPhysicsPosition(finalPos);
                pickedEntity.physicsPosition.set(finalPos);
                //pickedEntity.updateMinecraftPos();
            }
        } else {
            //updateClient();
        }
    }

    @SideOnly(Side.CLIENT)
    private void updateClient() { //TODO WTF DEMANDER A YANIS
        picker.set(Minecraft.getMinecraft().player);
        if (picker.get() != null) {
            if (picker.get().world.isRemote) {
                if (pickedEntity.get() != null) {
                    if (isPicked.get()) {
                        Vector3f playerLookPos = DynamXUtils.toVector3f(picker.get().getLookVec());
                        Vector3f playerPos = DynamXUtils.toVector3f(picker.get().getPositionEyes(1.0f));
                        Vector3f finalPos = playerPos.add(playerLookPos);
                        pickedEntity.get().physicsPosition.set(finalPos);
                        //pickedEntity.updateMinecraftPos();
                    }
                }
            }
        }
    }

}
