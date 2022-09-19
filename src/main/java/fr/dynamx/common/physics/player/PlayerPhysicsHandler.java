package fr.dynamx.common.physics.player;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Handles player's rigid body
 */
public class PlayerPhysicsHandler {
    private final EntityPlayer playerIn;
    private PhysicsRigidBody bodyIn;

    private PlayerBodyState state = PlayerBodyState.DISABLED;
    private byte removedCountdown;

    public RagdollEntity ragdollEntity;

    public PlayerPhysicsHandler(EntityPlayer playerIn) {
        this.playerIn = playerIn;
        Quaternion localQuat = new Quaternion(0.0F, 1.0F, 0.0F, playerIn.rotationYaw);
        Transform localTransform = new Transform(new Vector3f((float) playerIn.posX, (float) playerIn.posY + 0.8f, (float) playerIn.posZ), localQuat);
        bodyIn = DynamXPhysicsHelper.createRigidBody(60f, localTransform, new BoxCollisionShape(0.35f, 0.8f, 0.35f),
                new BulletShapeType<>(EnumBulletShapeType.PLAYER, this));
        bodyIn.setKinematic(true);
        bodyIn.setEnableSleep(false);
    }

    public void update(IPhysicsWorld world) {
        if(playerIn.isDead)
            removeFromWorld(true);
        if (removedCountdown > 0)
            removedCountdown--;
        switch (state) {
            case DISABLED:
                if (removedCountdown == 0)
                    state = PlayerBodyState.ACTIONABLE;
                break;
            case ACTIONABLE:
                if (removedCountdown == 0 && !playerIn.isSpectator()) {
                    if (bodyIn == null)
                        throw new IllegalStateException("Body is null while adding " + removedCountdown + " " + state + " " + playerIn);
                    DynamXContext.getPhysicsWorld().addCollisionObject(bodyIn);
                    state = PlayerBodyState.ACTIVATING;
                }
                break;
            case ACTIVATING:
                if(playerIn.isSpectator())
                    removeFromWorld(false);
                else if(bodyIn.isInWorld()) {
                    DynamXContext.getPhysicsWorld().schedule(() -> bodyIn.setGravity(Vector3fPool.get()));
                    state = PlayerBodyState.ACTIVATED;
                }
                break;
            case ACTIVATED:
                if(playerIn.isSpectator())
                    removeFromWorld(false);
                else if(bodyIn != null) {
                    Vector3f position = Vector3fPool.get();
                    position.set((float) playerIn.posX, (float) playerIn.posY + 0.8f, (float) playerIn.posZ);
                    if (Vector3f.isValidVector(position) && playerIn.fallDistance < 10) { //fixes a crash with elytra
                        bodyIn.setPhysicsLocation(position);
                        bodyIn.setPhysicsRotation(QuaternionPool.get().fromAngleNormalAxis((float) Math.toRadians(-playerIn.rotationYaw), Vector3f.UNIT_Y));
                        bodyIn.setContactResponse(true);
                    } else
                        bodyIn.setContactResponse(false);
                }
                break;
        }
    }

    public void handleCollision(PhysicsCollisionEvent collisionEvent, BulletShapeType<?> with) {
        //System.out.println("collision " + event.getAppliedImpulse());
        if (with.getObjectIn() instanceof BaseVehicleEntity && state == PlayerBodyState.ACTIVATED) {
            //System.out.println(event.getAppliedImpulse());
            if (DynamXConfig.ragdollSpawnMinForce != -1 && ((Entity) with.getObjectIn()).ticksExisted > 160 && collisionEvent.getAppliedImpulse() > DynamXConfig.ragdollSpawnMinForce) {
                //if (Math.abs(event.getDistance1()) < 0.08f) {// && event.getDistance1() < 0){// && playerIn.isUser()) {
                PhysicsEntity<?> e = (PhysicsEntity<?>) with.getObjectIn();
                playerIn.motionX += e.motionX;
                playerIn.motionY += e.motionY;
                playerIn.motionZ += e.motionZ;

                if (!playerIn.world.isRemote) {
                    //System.out.println("SPAWN RADDOLL");
                    RagdollEntity e1 = new RagdollEntity(playerIn.world, collisionEvent.getPositionWorldOnB(new Vector3f()).add(new Vector3f(0.5f, 0.5f, 0)), playerIn.rotationYaw % 360.0F,
                            playerIn.getName(), (short) (20 * 12), playerIn);
                    playerIn.world.spawnEntity(e1);

                    playerIn.setInvisible(true);
                    removeFromWorld(false);
                }
            }
        }
    }

    public void addToWorld() {
        if (state == PlayerBodyState.DISABLED)
            state = PlayerBodyState.ACTIONABLE;
    }

    public void removeFromWorld(boolean delete) {
        removedCountdown = 30;
        if (bodyIn != null && state == PlayerBodyState.ACTIVATED)
            DynamXContext.getPhysicsWorld().removeCollisionObject(bodyIn);
        if (delete) {
            bodyIn = null;
            DynamXContext.getPlayerToCollision().remove(playerIn);
            state = PlayerBodyState.DELETED;
        } else
            state = PlayerBodyState.DISABLED;
    }

    public PhysicsRigidBody getBodyIn() {
        return bodyIn;
    }

    public enum PlayerBodyState {
        DISABLED,
        ACTIONABLE,
        ACTIVATING,
        ACTIVATED,
        DELETED
    }
}
