package fr.dynamx.common.entities.modules.movables;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MoveObjects extends MovableModule {

    public boolean isPicked;
    public EntityPlayer picker;
    public PhysicsEntity<?> pickedEntity;
    public Vector3f basePos;

    public MoveObjects(PhysicsEntity<?> entity) {
        super(entity);
    }

    public void pickObject(EntityPlayer picker, PhysicsEntity<?> pickedEntity) {
        if (pickedEntity.getPhysicsHandler() != null) {
            PhysicsCollisionObject rigidBody = pickedEntity.getPhysicsHandler().getCollisionObject();
            if (rigidBody instanceof PhysicsRigidBody) {
                if (((PhysicsRigidBody) rigidBody).getMass() < 100) {
                    this.picker = picker;
                    this.pickedEntity = pickedEntity;
                    this.basePos = rigidBody.getPhysicsLocation(null);

                    this.isPicked = true;

                    DynamXContext.getPlayerPickingObjects().put(picker.getEntityId(), pickedEntity.getEntityId());
                    entity.getNetwork().onPlayerStartControlling(picker, false);
                }
            }
        }
    }

    public void throwObject(float force) {
        if (picker != null) {
            if (pickedEntity != null) {
                ((PhysicsRigidBody) entity.getPhysicsHandler().getCollisionObject()).setGravity(DynamXPhysicsHelper.GRAVITY);
                PhysicsRigidBody rigidBody = (PhysicsRigidBody) pickedEntity.getPhysicsHandler().getCollisionObject();
                Vector3f playerLookPos = DynamXUtils.toVector3f(picker.getLookVec());
                rigidBody.setLinearVelocity(playerLookPos.multLocal(force));
                unPickObject();
            }
        }
    }

    public void unPickObject() {
        if (picker != null) {
            ((PhysicsRigidBody) entity.getPhysicsHandler().getCollisionObject()).setGravity(Vector3fPool.get(DynamXPhysicsHelper.GRAVITY));
            DynamXContext.getPlayerPickingObjects().remove(picker.getEntityId());
            isPicked = false;
            entity.getNetwork().onPlayerStopControlling(picker, false);
            //System.out.println("Remove");
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        super.preUpdatePhysics(simulatingPhysics);
        if (simulatingPhysics) {
            if (picker != null) {
                if (pickedEntity != null) {
                    if (isPicked) {
                        PhysicsRigidBody rigidBody = (PhysicsRigidBody) pickedEntity.getPhysicsHandler().getCollisionObject();
                        Vector3f playerLookPos = DynamXUtils.toVector3f(picker.getLookVec());
                        rigidBody.setGravity(new Vector3f());
                        rigidBody.setAngularVelocity(new Vector3f());
                        rigidBody.setLinearVelocity(new Vector3f());
                        Vector3f playerPos = DynamXUtils.toVector3f(picker.getPositionEyes(1.0f));
                        Vector3f finalPos = playerPos.add(playerLookPos);

                        // Quaternion rotatedQuat = DynamXGeometry.eulerToQuaternion(0, 2 * FastMath.PI - (float) Math.toRadians(picker.rotationYaw), 0);
                        rigidBody.setPhysicsLocation(finalPos);
                        pickedEntity.getPhysicsHandler().setPhysicsPosition(finalPos);
                        pickedEntity.physicsPosition.set(finalPos);
                        //pickedEntity.updateMinecraftPos();
                    }
                }
            }
        } else {
            //updateClient();
        }
    }

    @SideOnly(Side.CLIENT)
    private void updateClient() {
        picker = Minecraft.getMinecraft().player;
        if (picker != null) {
            if (picker.world.isRemote) {
                if (pickedEntity != null) {
                    if (isPicked) {
                        Vector3f playerLookPos = DynamXUtils.toVector3f(picker.getLookVec());
                        Vector3f playerPos = DynamXUtils.toVector3f(picker.getPositionEyes(1.0f));
                        Vector3f finalPos = playerPos.add(playerLookPos);
                        pickedEntity.physicsPosition.set(finalPos);
                        //pickedEntity.updateMinecraftPos();
                    }
                }
            }
        }
    }

}
