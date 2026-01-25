package fr.dynamx.core.common.physics.player;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.entities.RagdollEntity;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.hermes.forge.JmeVector3fPool;
import fr.dynamx.core.utils.physics.DynamXPhysicsHelper;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.world.HmWorld;
import lombok.Getter;

/**
 * Handles player's rigid body
 */
public class PlayerPhysicsHandler {
    private final HmPlayerEntity playerIn;
    @Getter
    private PhysicsRigidBody bodyIn;

    private PlayerBodyState state = PlayerBodyState.DISABLED;
    private byte removedCountdown;

    public RagdollEntity ragdollEntity;

    public PlayerPhysicsHandler(HmPlayerEntity playerIn) {
        this.playerIn = playerIn;
        Quaternion localQuat = new Quaternion(0.0F, 1.0F, 0.0F, playerIn.hm$getRotationYaw());
        Transform localTransform = new Transform(new Vector3f((float) playerIn.hm$getPosX(), (float) playerIn.hm$getPosY() + 0.8f, (float) playerIn.hm$getPosZ()), localQuat);
        BoxCollisionShape shape = new BoxCollisionShape(0.35f, 0.8f, 0.35f);
        bodyIn = DynamXPhysicsHelper.createRigidBody(60f, localTransform, shape,
                new BulletShapeType<>(EnumBulletShapeType.PLAYER, this));
        bodyIn.setKinematic(true);
        bodyIn.setEnableSleep(false);
    }

    public void update(HmWorld world) {
        if (playerIn.hm$isDead())
            removeFromWorld(true, world);
        if (removedCountdown > 0)
            removedCountdown--;
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
        switch (state) {
            case DISABLED:
                if (removedCountdown == 0)
                    state = PlayerBodyState.ACTIONABLE;
                break;
            case ACTIONABLE:
                if (removedCountdown == 0 && !playerIn.hm$isSpectator()) {
                    if (bodyIn == null)
                        throw new IllegalStateException("Body is null while adding " + removedCountdown + " " + state + " " + playerIn);
                    physicsWorld.addCollisionObject(bodyIn);
                    state = PlayerBodyState.ACTIVATING;
                }
                break;
            case ACTIVATING:
                if (playerIn.hm$isSpectator())
                    removeFromWorld(false, world);
                else if (bodyIn.isInWorld()) {
                    physicsWorld.schedule(() -> {
                        if(bodyIn != null)
                            bodyIn.setGravity(JmeVector3fPool.get());
                    });
                    state = PlayerBodyState.ACTIVATED;
                }
                break;
            case ACTIVATED:
                if (playerIn.hm$isSpectator())
                    removeFromWorld(false, world);
                else if (bodyIn != null) {
                    Vector3f position = JmeVector3fPool.get();
                    position.set((float) playerIn.hm$getPosX(), (float) playerIn.hm$getPosY() + 0.8f, (float) playerIn.hm$getPosZ());
                    if (Vector3f.isValidVector(position) && playerIn.hm$getFallDistance() < 10) { //fixes a crash with elytra
                        bodyIn.setPhysicsLocation(position);
                        bodyIn.setPhysicsRotation(QuaternionPool.get().fromAngleNormalAxis((float) Math.toRadians(-playerIn.hm$getRotationYaw()), Vector3f.UNIT_Y));
                        bodyIn.setContactResponse(true);
                    } else
                        bodyIn.setContactResponse(false);
                }
                break;
        }
    }

    /*public void handleCollision(PhysicsCollisionEvent collisionEvent, BulletShapeType<?> with) {
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
    }*/

    public void addToWorld() {
        if (state == PlayerBodyState.DISABLED)
            state = PlayerBodyState.ACTIONABLE;
    }

    public void removeFromWorld(boolean delete, HmWorld world) {
        removedCountdown = 30;
        if (bodyIn != null && state == PlayerBodyState.ACTIVATED)
            DynamXContext.getPhysicsWorld(world).removeCollisionObject(bodyIn);
        if (delete) {
            bodyIn = null;
            DynamXContext.getPlayerToCollision().remove(playerIn);
            state = PlayerBodyState.DELETED;
        } else
            state = PlayerBodyState.DISABLED;
    }

    public enum PlayerBodyState {
        DISABLED,
        ACTIONABLE,
        ACTIVATING,
        ACTIVATED,
        DELETED
    }
}
