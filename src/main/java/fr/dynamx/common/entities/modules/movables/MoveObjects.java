package fr.dynamx.common.entities.modules.movables;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SynchronizedEntityVariable.SynchronizedPhysicsModule()
public class MoveObjects extends MovableModule {
    //TODO PRIVATISER

    @SynchronizedEntityVariable(name = "picker")
    public final EntityVariable<EntityPlayer> picker = new EntityVariable<>((variable, value) -> {
        if(value != null && DynamXContext.getPlayerPickingObjects().containsKey(value.getEntityId()))
            entity.getSynchronizer().onPlayerStartControlling(value, false);
    }, SynchronizationRules.SERVER_TO_CLIENTS);
    @SynchronizedEntityVariable(name = "isPicked")
    public final EntityVariable<Boolean> isPicked = new EntityVariable<>((variable, value) -> {
        if(picker.get() != null)
            entity.getSynchronizer().onPlayerStopControlling(picker.get(), false);
    }, SynchronizationRules.SERVER_TO_CLIENTS, false);
    @SynchronizedEntityVariable(name = "pickedEntity")
    public final EntityVariable<PhysicsEntity<?>> pickedEntity = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, null);
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
        if (simulatingPhysics) {
            PhysicsEntity<?> pickedEntity = this.pickedEntity.get();
            if (picker.get() != null && pickedEntity != null && isPicked.get()) {
                PhysicsRigidBody rigidBody = (PhysicsRigidBody) pickedEntity.getPhysicsHandler().getCollisionObject();
                Vector3f playerLookPos = DynamXUtils.toVector3f(picker.get().getLookVec());
                rigidBody.setGravity(Vector3fPool.get());
                rigidBody.setAngularVelocity(Vector3fPool.get());
                rigidBody.setLinearVelocity(Vector3fPool.get());
                Vector3f playerPos = DynamXUtils.toVector3f(picker.get().getPositionEyes(1.0f));
                Vector3f finalPos = playerPos.addLocal(playerLookPos);
                pickedEntity.getPhysicsHandler().setPhysicsPosition(finalPos);
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
