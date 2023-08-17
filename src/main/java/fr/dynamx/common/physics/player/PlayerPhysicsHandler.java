package fr.dynamx.common.physics.player;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.bb.OBBModelBone;
import fr.dynamx.bb.OBBModelBox;
import fr.dynamx.bb.OBBPlayerManager;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.lwjgl.util.vector.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles player's rigid body
 */
public class PlayerPhysicsHandler {
    private final EntityPlayer playerIn;
    private PhysicsRigidBody bodyIn1;

    @Getter
    private List<PhysicsRigidBody> bodies = new ArrayList<>();

    private PlayerBodyState state = PlayerBodyState.DISABLED;
    private byte removedCountdown;

    public RagdollEntity ragdollEntity;

    public PlayerPhysicsHandler(EntityPlayer playerIn) {
        this.playerIn = playerIn;
        OBBPlayerManager.PlayerOBBModelObject playerOBBObject = OBBPlayerManager.playerOBBObjectMap.get(playerIn.getName());

        if (playerOBBObject.boneBinding == null) {
            return;
        }
        for (Map.Entry<OBBModelBox, OBBModelBone> entry : playerOBBObject.boneBinding.entrySet()) {

            OBBModelBox box = entry.getKey();
            OBBModelBone bone = entry.getValue();

            Matrix4f rotation = bone.currentPose;

            Quaternion localQuat = new Quaternion().fromRotationMatrix(rotation.m00, rotation.m01, rotation.m02, rotation.m10, rotation.m11, rotation.m12, rotation.m20, rotation.m21, rotation.m22);
            Transform localTransform = new Transform(new Vector3f(box.center.x, box.center.y, box.center.z), localQuat);
            BoxCollisionShape shape = new BoxCollisionShape(box.size.x, box.size.y, box.size.z);
            shape.setScale(1 / 16f);

            PhysicsRigidBody rigidBody = DynamXPhysicsHelper.createRigidBody(60f, localTransform, shape,
                    new BulletShapeType<>(EnumBulletShapeType.PLAYER, this, shape));
            rigidBody.setKinematic(true);
            rigidBody.setEnableSleep(false);
            bodies.add(rigidBody);
            //DynamXContext.getPhysicsWorld(MC.world).addCollisionObject(rigidBody);
        }

       /* Quaternion localQuat = new Quaternion(0.0F, 1.0F, 0.0F, playerIn.rotationYaw);
        Transform localTransform = new Transform(new Vector3f((float) playerIn.posX, (float) playerIn.posY + 0.8f, (float) playerIn.posZ), localQuat);
        bodyIn = DynamXPhysicsHelper.createRigidBody(60f, localTransform, shape,
                new BulletShapeType<>(EnumBulletShapeType.PLAYER, this, shape));*/
    }

    public void update(World world) {
        if (playerIn.isDead)
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
                if (removedCountdown == 0 && !playerIn.isSpectator()) {
                    if (bodies.isEmpty())
                        return;
                    //throw new IllegalStateException("Body is null while adding " + removedCountdown + " " + state + " " + playerIn);
                    bodies.forEach(physicsWorld::addCollisionObject);
                    state = PlayerBodyState.ACTIVATING;
                }
                break;
            case ACTIVATING:
                if (playerIn.isSpectator())
                    removeFromWorld(false, world);
                else if (!bodies.isEmpty() && bodies.get(0).isInWorld()) {
                    physicsWorld.schedule(() -> {
                        bodies.forEach(bodyIn -> bodyIn.setGravity(Vector3fPool.get()));
                    });
                    state = PlayerBodyState.ACTIVATED;
                }
                break;
            case ACTIVATED:
                if (playerIn.isSpectator())
                    removeFromWorld(false, world);
                else if (!bodies.isEmpty()) {
                    OBBPlayerManager.PlayerOBBModelObject playerOBBObject = OBBPlayerManager.playerOBBObjectMap.get(playerIn.getName());

                    if(playerOBBObject == null){
                        return;
                    }
                    for (int i = 0; i < bodies.size(); i++) {
                        PhysicsRigidBody body = bodies.get(i);
                        if(!body.isKinematic()){
                            continue;
                        }
                        Map.Entry<OBBModelBox, OBBModelBone> entry = (Map.Entry<OBBModelBox, OBBModelBone>) playerOBBObject.boneBinding.entrySet().toArray()[i];
                        OBBModelBox box = entry.getKey();
                        OBBModelBone bone = entry.getValue();

                        Matrix4f rotation = bone.currentPose;

                        EntityPlayerSP player = Minecraft.getMinecraft().player;
                        body.setPhysicsLocation(Vector3fPool.get(
                                box.center.x + (player.motionX*2.5) * Minecraft.getMinecraft().getRenderPartialTicks(),
                                box.center.y + (player.motionY*2.5) * Minecraft.getMinecraft().getRenderPartialTicks(),
                                box.center.z + (player.motionZ*2.5) * Minecraft.getMinecraft().getRenderPartialTicks()));
                        body.setPhysicsRotation(QuaternionPool.get().fromRotationMatrix(rotation.m00, rotation.m01, rotation.m02, rotation.m10, rotation.m11, rotation.m12, rotation.m20, rotation.m21, rotation.m22).inverse());

                    }
                   /* Vector3f position = Vector3fPool.get();
                    position.set((float) playerIn.posX, (float) playerIn.posY + 0.8f, (float) playerIn.posZ);
                    if (Vector3f.isValidVector(position) && playerIn.fallDistance < 10) { //fixes a crash with elytra
                        bodyIn.setPhysicsLocation(position);
                        bodyIn.setPhysicsRotation(QuaternionPool.get().fromAngleNormalAxis((float) Math.toRadians(-playerIn.rotationYaw), Vector3f.UNIT_Y));
                        bodyIn.setContactResponse(true);
                    } else
                        bodyIn.setContactResponse(false);*/
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

    public void removeFromWorld(boolean delete, World world) {
        removedCountdown = 30;
        /*if (bodyIn != null && state == PlayerBodyState.ACTIVATED)
            DynamXContext.getPhysicsWorld(world).removeCollisionObject(bodyIn);
        if (delete) {
            bodyIn = null;
            DynamXContext.getPlayerToCollision().remove(playerIn);
            state = PlayerBodyState.DELETED;
        } else
            state = PlayerBodyState.DISABLED;*/
    }

   /* public PhysicsRigidBody getBodyIn() {
        return bodyIn;
    }*/

    public enum PlayerBodyState {
        DISABLED,
        ACTIONABLE,
        ACTIVATING,
        ACTIVATED,
        DELETED
    }
}
