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
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class MoveObjects extends MovableModule {

    @SynchronizedEntityVariable(name = "picker")
    private final EntityVariable<EntityPlayer> picker = new EntityVariable<>((variable, value) -> {
        if(value != null && DynamXContext.getPlayerPickingObjects().containsKey(value.getEntityId()))
            entity.getSynchronizer().onPlayerStartControlling(value, false);
    }, SynchronizationRules.SERVER_TO_CLIENTS);
    @SynchronizedEntityVariable(name = "isPicked")
    private final EntityVariable<Boolean> isPicked = new EntityVariable<>((variable, value) -> {
        if(picker.get() != null)
            entity.getSynchronizer().onPlayerStopControlling(picker.get(), false);
    }, SynchronizationRules.SERVER_TO_CLIENTS, false);
    @SynchronizedEntityVariable(name = "pickedEntity")
    private final EntityVariable<PhysicsEntity<?>> pickedEntity = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, null);

    public MoveObjects(PhysicsEntity<?> entity) {
        super(entity);
    }

    public void pickObject(EntityPlayer picker, PhysicsEntity<?> pickedEntity) {
        if (pickedEntity.getPhysicsHandler() == null) {
            return;
        }
        PhysicsCollisionObject rigidBody = pickedEntity.getPhysicsHandler().getCollisionObject();
        if (rigidBody instanceof PhysicsRigidBody && ((PhysicsRigidBody) rigidBody).getMass() < 100) {
            this.picker.set(picker);
            this.pickedEntity.set(pickedEntity);
            Vector3f basePos = rigidBody.getPhysicsLocation(null);

            this.isPicked.set(true);

            DynamXContext.getPlayerPickingObjects().put(picker.getEntityId(), pickedEntity.getEntityId());
            entity.getSynchronizer().onPlayerStartControlling(picker, false);
        }
    }

    public void throwObject(float force) {
        if (picker.get() == null || pickedEntity.get() == null) {
            return;
        }
        ((PhysicsRigidBody) entity.getPhysicsHandler().getCollisionObject()).setGravity(Vector3fPool.get(0, -DynamXPhysicsHelper.GRAVITY,0));
        PhysicsRigidBody rigidBody = (PhysicsRigidBody) pickedEntity.get().getPhysicsHandler().getCollisionObject();
        Vector3f playerLookPos = DynamXUtils.toVector3f(picker.get().getLookVec());
        rigidBody.setLinearVelocity(playerLookPos.multLocal(force));
        unPickObject();
    }

    public void unPickObject() {
        if (picker.get() == null) {
            return;
        }
        ((PhysicsRigidBody) entity.getPhysicsHandler().getCollisionObject()).setGravity(Vector3fPool.get(0, -DynamXPhysicsHelper.GRAVITY,0));
        DynamXContext.getPlayerPickingObjects().remove(picker.get().getEntityId());
        isPicked.set(false);
        entity.getSynchronizer().onPlayerStopControlling(picker.get(), false);
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
        }
    }
}
