package fr.dynamx.common.physics.player;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.joints.Anchor;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.objects.infos.Aero;
import com.jme3.bullet.objects.infos.Sbcp;
import com.jme3.bullet.objects.infos.SoftBodyConfig;
import com.jme3.bullet.objects.infos.SoftBodyMaterial;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.bb.OBBModelBone;
import fr.dynamx.bb.OBBModelBox;
import fr.dynamx.bb.OBBPlayerManager;
import fr.dynamx.client.renders.mesh.GLMesh;
import fr.dynamx.client.renders.mesh.shapes.ClothGLGrid;
import fr.dynamx.client.renders.mesh.shapes.FacesMesh;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;
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

    private List<PhysicsRigidBody> bodies = new ArrayList<>();

    private PlayerBodyState state = PlayerBodyState.DISABLED;
    private byte removedCountdown;

    public RagdollEntity ragdollEntity;

    public PhysicsSoftBody softBody;
    public RigidBodyTransform softTransform = new RigidBodyTransform();
    public RigidBodyTransform prevSoftTransform = new RigidBodyTransform();
    public Map.Entry<OBBModelBox, OBBModelBone> chest = null;
    public List<Anchor> anchors = new ArrayList<>();

    public PlayerPhysicsHandler(EntityPlayer playerIn) {
        this.playerIn = playerIn;
        OBBPlayerManager.PlayerOBBModelObject playerOBBObject = OBBPlayerManager.playerOBBObjectMap.get(playerIn.getName());

        if (playerOBBObject.boneBinding == null) {
            return;
        }

        int chestId = 0;
        int i = 0;
        for (Map.Entry<OBBModelBox, OBBModelBone> entry : playerOBBObject.boneBinding.entrySet()) {

            OBBModelBox box = entry.getKey();
            OBBModelBone bone = entry.getValue();

            Matrix4f rotation = bone.currentPose;

            Quaternion localQuat = new Quaternion().fromRotationMatrix(rotation.m00, rotation.m01, rotation.m02, rotation.m10, rotation.m11, rotation.m12, rotation.m20, rotation.m21, rotation.m22);
            Transform localTransform = new Transform(new Vector3f(box.center.x, box.center.y, box.center.z), localQuat);
            BoxCollisionShape shape = new BoxCollisionShape(box.size.x * (1 / 16f), box.size.y * (1 / 16f), box.size.z * (1 / 16f));
            //shape.setScale(0.0001f);

            PhysicsRigidBody rigidBody = DynamXPhysicsHelper.createRigidBody(60f, localTransform, shape,
                    new BulletShapeType<>(EnumBulletShapeType.PLAYER, this));
            rigidBody.setKinematic(true);
            rigidBody.setEnableSleep(false);
            bodies.add(rigidBody);

            if (entry.getKey().name.contains("body")) {
                chest = entry;
                chestId = i;
            }
            i++;
            //DynamXContext.getPhysicsWorld(MC.world).addCollisionObject(rigidBody);
        }

        int xLines = 20;
        int zLines = 2 * xLines;
        float width = 2f;
        float lineSpacing = width / zLines;
        GLMesh mesh = new ClothGLGrid(xLines, zLines, lineSpacing);

        // Create a soft rectangle for the flag.
        softBody = new PhysicsSoftBody();
        NativeSoftBodyUtil.appendFromTriMesh(mesh, softBody);
        softBody.setMargin(0.05f);
        softBody.setMass(1f);

        SoftBodyMaterial softMaterial = softBody.getSoftMaterial();
        softMaterial.setAngularStiffness(0f);

        SoftBodyConfig config = softBody.getSoftConfig();
        config.setAerodynamics(Aero.F_TwoSidedLiftDrag);
        config.set(Sbcp.Damping, 0.01f); // default = 0
        config.set(Sbcp.Drag, 0.5f); // default = 0.2
        config.set(Sbcp.Lift, 1f); // default = 0

        config.setPositionIterations(3);
        softBody.generateBendingConstraints(2, softMaterial);


        FacesMesh facesMesh = new FacesMesh(softBody, prevSoftTransform, softTransform);
        DynamXContext.getSOFTBODY_ENTITY_MESH_2().put(softBody, facesMesh);
        softBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.PLAYER, this));


        Vector3f off = Vector3fPool.get(-chest.getKey().size.x - 5 , chest.getKey().size.y, -chest.getKey().size.z);

        PhysicsRigidBody body = bodies.get(chestId);
        Anchor anchor;

        float numAnchor = xLines/2f;

        for (int j = 0; j < numAnchor; j++) {
            Vector3f anchorPos = off.add(new Vector3f( -j / numAnchor, 0, 0)).mult(1 / 16f);
            anchor = new Anchor(softBody, j, body, anchorPos, false);
            anchors.add(anchor);
        }


        off = Vector3fPool.get(chest.getKey().size.x + 5, chest.getKey().size.y, -chest.getKey().size.z);

        for (int j = 0; j < numAnchor; j++) {
            int index = (int) (xLines - numAnchor + j);
            Vector3f anchorPos = off.add(new Vector3f( j / numAnchor, 0, 0)).mult(1 / 16f);
            anchor = new Anchor(softBody, index, body, anchorPos, false);
            anchors.add(anchor);
        }


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
                    physicsWorld.addCollisionObject(softBody);
                    anchors.forEach(physicsWorld::addJoint);
                    softBody.setPhysicsLocation(Vector3fPool.get(playerIn.posX, playerIn.posY, playerIn.posZ));
                    softBody.applyRotation(QuaternionPool.get().fromAngles(playerIn.rotationPitch * DynamXMath.TO_RADIAN, playerIn.rotationYaw * DynamXMath.TO_RADIAN, 0));
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

                    if (playerOBBObject == null) {
                        return;
                    }
                    for (int i = 0; i < bodies.size(); i++) {
                        if (Mouse.isButtonDown(4)) {
                            bodies.get(i).setKinematic(false);
                            bodies.get(i).setGravity(Vector3fPool.get(0, -DynamXPhysicsHelper.GRAVITY, 0));
                            EntityPlayerSP player = Minecraft.getMinecraft().player;
                            //bodies.get(i).setLinearVelocity(Vector3fPool.get(player.motionX, player.motionY, player.motionZ).multLocal(10f));
                        } else {
                            bodies.get(i).setKinematic(true);
                            PhysicsRigidBody body = bodies.get(i);
                            Map.Entry<OBBModelBox, OBBModelBone> entry = (Map.Entry<OBBModelBox, OBBModelBone>) playerOBBObject.boneBinding.entrySet().toArray()[i];
                            OBBModelBox box = entry.getKey();
                            OBBModelBone bone = entry.getValue();

                            Matrix4f rotation = bone.currentPose;

                            EntityPlayerSP player = Minecraft.getMinecraft().player;
                            //body.setLinearVelocity(Vector3fPool.get(player.motionX, player.motionY, player.motionZ).multLocal(10f));
                            body.setPhysicsLocation(Vector3fPool.get(box.center.x, box.center.y, box.center.z));
                            body.setPhysicsRotation(QuaternionPool.get().fromRotationMatrix(rotation.m00, rotation.m01, rotation.m02, rotation.m10, rotation.m11, rotation.m12, rotation.m20, rotation.m21, rotation.m22).inverse());


                        }
                    }
                    prevSoftTransform.set(softTransform);
                    softTransform.setPosition(softBody.getPhysicsLocation(Vector3fPool.get()));
                    softTransform.setRotation(softBody.getPhysicsRotation(QuaternionPool.get()));

                   // softBody.setWindVelocity(Vector3fPool.get(0, 0, 0));


                   /* Vector3f off = Vector3fPool.get(chest.getKey().size.x, chest.getKey().size.y, -chest.getKey().size.z);
                    Vector3f chestPos = Vector3fPool.get(chest.getKey().center.x, chest.getKey().center.y, chest.getKey().center.z);
                    Vector3f finalPos = Vector3fPool.get();

                    DynamXMath.transform(chest.getValue().currentPose, off, off);

                    finalPos.addLocal(chestPos.add(off));*/

                    //anchors.get(0).setPivotInB(finalPos.subtract(Vector3fPool.get(playerIn.posX, playerIn.posY + 1, playerIn.posZ)));

                    /*off = Vector3fPool.get(-chest.getKey().size.x, chest.getKey().size.y, -chest.getKey().size.z);
                    finalPos = Vector3fPool.get();

                    DynamXMath.transform(chest.getValue().currentPose, off, off);

                    finalPos.addLocal(chestPos.add(off));

                    anchors.get(1).setPivotInB(finalPos.subtract(Vector3fPool.get(playerIn.posX, playerIn.posY + 1, playerIn.posZ+1)));*/

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
