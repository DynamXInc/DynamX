package fr.dynamx.common.entities.modules;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.joints.HingeJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.client.ClientProxy;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.ObjModelClient;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.network.sync.vars.AttachedBodySynchronizedVariable;
import fr.dynamx.common.network.sync.vars.AttachedDoorsSynchronizedVariable;
import fr.dynamx.common.network.sync.vars.VehicleSynchronizedVariables;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.JointHandler;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoorsModule implements IPhysicsModule<AbstractEntityPhysicsHandler<?, ?>>, AttachModule.AttachToSelfModule,
        IPhysicsModule.IEntityPosUpdateListener, IPhysicsModule.IPhysicsUpdateListener,
        AttachedBodySynchronizedVariable.AttachedBodySynchronizer, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>> {
    public static final ResourceLocation JOINT_NAME = new ResourceLocation(DynamXConstants.ID, "door_module");

    static {
        JointHandlerRegistry.register(new JointHandler(JOINT_NAME, BaseVehicleEntity.class, BaseVehicleEntity.class, DoorsModule.class));
    }

    public final BaseVehicleEntity<?> entity;
    private final Map<Byte, DoorPhysicsHandler> attachedDoors = new HashMap<>();
    private final HashMap<Byte, SynchronizedRigidBodyTransform> attachedBodiesTransform = new HashMap<>();
    protected Map<Byte, Boolean> doorsState = new HashMap<>();

    public DoorsModule(BaseVehicleEntity<?> entity) {
        this.entity = entity;
    }

    @Override
    public boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId) {
        return !attachedDoors.containsKey(jointId);
    }

    @Override
    public void onJointDestroyed(EntityJoint<?> joint) {
        //TODO SPAWN DOOR ENTITY IF BROKEN
        //((IModuleContainer.IDoorContainer)entity).getDoors().detachDoor(doorID);
        //System.out.println("RM DOOR " + joint);
        attachedBodiesTransform.remove(joint.getJointId());
        attachedDoors.remove(joint.getJointId());
        if (entity.physicsHandler != null) {
            entity.physicsHandler.getCollisionObject().removeFromIgnoreList(joint.getJoint().getBodyB());
        }
    }

    @Override
    public Constraint createJoint(byte jointId) {
        PartDoor door1 = getPartDoor(jointId);
        Vector3f p1 = Vector3fPool.get(door1.getCarAttachPoint());
        Vector3f p2 = Vector3fPool.get(door1.getDoorAttachPoint());

        Quaternion localQuat = QuaternionPool.get(entity.getPhysicsHandler().getRotation());
        Vector3f doorPos = DynamXGeometry.rotateVectorByQuaternion(p1.subtract(p2).addLocal(entity.getPackInfo().getCenterOfMass()), localQuat).addLocal(entity.physicsPosition);
        PhysicsRigidBody doorBody = DynamXPhysicsHelper.fastCreateRigidBody(entity, 40, new BoxCollisionShape(door1.getScale()), doorPos, entity.getPhysicsHandler().getSpawnRotationAngle());
        DynamXContext.getPhysicsWorld().addCollisionObject(doorBody);
        doorBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.BULLET_ENTITY, new DoorContainer(this, door1.getId())));

        HingeJoint hingeJoint = new HingeJoint(entity.physicsHandler.getCollisionObject(), doorBody, p1.addLocal(entity.getPackInfo().getCenterOfMass()), p2, DynamXMath.Y_AXIS, DynamXMath.Y_AXIS);
        hingeJoint.setLimit(0.0f, 0f);
        hingeJoint.setCollisionBetweenLinkedBodies(false);
        //doorBody.addToIgnoreList(entity.physicsHandler.getRigidBody());
        //hingeJoint.setBreakingImpulseThreshold(400);

        //System.out.println("ADD DOOR " + jointId + " " + entity);
        attachedBodiesTransform.put(jointId, new SynchronizedRigidBodyTransform(new RigidBodyTransform(doorBody)));
        attachedDoors.put(jointId, new DoorPhysicsHandler(doorBody, hingeJoint));
        return hingeJoint;
    }

    @Override
    public void initEntityProperties() {
        for (PartDoor s : entity.getPackInfo().getPartsByType(PartDoor.class)) {
            doorsState.put(s.getId(), false);
        }
    }

    @Override
    public void addSynchronizedVariables(Side side, SimulationHolder simulationHolder, List<ResourceLocation> variables) {
        if (simulationHolder.isPhysicsAuthority(side)) {
            variables.add(AttachedDoorsSynchronizedVariable.NAME);
        }
        if (side.isServer()) {
            variables.add(VehicleSynchronizedVariables.DoorsStatus.NAME);
        }
    }

    @Override
    public void initPhysicsEntity(AbstractEntityPhysicsHandler<?, ?> handler) {
        if (!entity.world.isRemote) {
            entity.getPackInfo().getPartsByType(PartDoor.class).forEach(this::spawnDoor);
        }
    }

    public PartDoor getPartDoor(byte doorID) {
        return entity.getPackInfo().getPartByTypeAndId(PartDoor.class, doorID);
    }

    public boolean isDoorAttached(byte doorID) {
        for (EntityJoint<?> j : entity.getJointsHandler().getJoints()) {
            if (j.getType() == JOINT_NAME && j.getJointId() == doorID) {
                return true;
            }
        }
        return false;//attachedDoors.containsKey(doorID);
    }

    public DoorPhysicsHandler getDoorPhysics(byte doorId) {
        return attachedDoors.get(doorId);
    }

    public Map<Byte, Boolean> getDoorsState() {
        return doorsState;
    }

    public void spawnDoor(PartDoor door) {
        if (door.isEnabled()) {
            DynamXMain.proxy.scheduleTask(entity.world, () -> JointHandlerRegistry.createJointWithSelf(JOINT_NAME, entity, door.getId()));
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        //TODO ALLOW THIS IN SOLO TOO
        if (simulatingPhysics && !entity.world.isRemote) {
            for (byte doorID : attachedDoors.keySet()) {
                DoorPhysicsHandler physics = attachedDoors.get(doorID);
                if (isDoorOpened(getPartDoor(doorID).getId()) && physics.joint.getHingeAngle() <= 0 && physics.timer == -1) {
                    doorsState.put(doorID, false);
                    PartDoor door = getPartDoor(doorID);
                    physics.joint.setLimit(door.getCloseLimit().x, door.getCloseLimit().y);
                }
                if (physics.timer >= 0) {
                    physics.timer--;
                }
            }
        }
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            for (byte door : attachedDoors.keySet()) {
                PhysicsRigidBody body = attachedDoors.get(door).body;
                attachedBodiesTransform.get(door).getPhysicTransform().set(body);
                //attachedBodiesTransform.get(door).updatePos();
            }
        } else {
            /*for (byte door : attachedDoors.keySet()) {
                attachedBodiesTransform.get(door).updatePos();
            }*/
        }
    }

    public boolean isDoorOpened(byte doorID) {
        if (doorsState == null)
            return true;
        return !doorsState.containsKey(doorID) || doorsState.get(doorID);
    }

    public void setDoorState(byte doorID, boolean opened) {
        DoorPhysicsHandler physicsHandler = attachedDoors.get(doorID);
        PartDoor door = getPartDoor(doorID);
        if (physicsHandler != null) {
            if (opened) {
                doorsState.put(doorID, true);
                playDoorSound(false);
                physicsHandler.joint.setLimit(door.getOpenLimit().x, door.getOpenLimit().y);
                physicsHandler.joint.enableMotor(true, door.getOpenMotor().x, door.getOpenMotor().y);
                entity.forcePhysicsActivation();
                physicsHandler.timer = 20 * 3;
            } else {
                physicsHandler.joint.enableMotor(true, door.getCloseMotor().x, door.getCloseMotor().y);
                entity.forcePhysicsActivation();
                if (entity.getNetwork().getSimulationHolder() != SimulationHolder.SERVER_SP) //TODO SERVER_SP SPLITTED IN SERVER_SP AND DRIVE_SP
                    doorsState.put(doorID, false); //Closes the door (animation) and the plays the closing sound
                TaskScheduler.schedule(new TaskScheduler.ScheduledTask(door.getDoorCloseTime()) {
                    @Override
                    public void run() {
                        if (entity.getNetwork().getSimulationHolder() == SimulationHolder.SERVER_SP)
                            doorsState.put(doorID, false); //Only plays the closing sound
                        playDoorSound(true);
                        physicsHandler.joint.setLimit(door.getCloseLimit().x, door.getCloseLimit().y);
                    }
                });
            }
        } else {
            if (opened) {
                doorsState.put(doorID, true);
                playDoorSound(false);
            } else {
                //if (entity.getNetwork().getSimulationHolder() != SimulationHolder.SERVER_SP)
                doorsState.put(doorID, false); //Closes the door (animation) and the plays the closing sound
                TaskScheduler.schedule(new TaskScheduler.ScheduledTask(door.getDoorCloseTime()) {
                    @Override
                    public void run() {
                        //if (entity.getNetwork().getSimulationHolder() == SimulationHolder.SERVER_SP)
                        //  doorsStatus.put(doorID, false); //Only plays the closing sound
                        playDoorSound(true);
                    }
                });
            }
        }
    }

    public void playDoorSound(boolean close) {
        if (entity.world.isRemote) {
            ClientProxy.SOUND_HANDLER.playSingleSound(entity.physicsPosition, close ? "door_close" : "door_open", 1, 1);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        if (DynamXMain.proxy.shouldUseBulletSimulation(entity.world)) {
            for (DoorPhysicsHandler body : attachedDoors.values())
                DynamXContext.getPhysicsWorld().removeCollisionObject(body.body);
        }
    }

    @Override
    public HashMap<Byte, SynchronizedRigidBodyTransform> getTransforms() {
        return attachedBodiesTransform;
    }

    @Override
    public void setPhysicsTransform(byte jointId, RigidBodyTransform transform) {
        if (attachedDoors.containsKey(jointId)) {
            attachedDoors.get(jointId).body.setPhysicsLocation(transform.getPosition());
            attachedDoors.get(jointId).body.setPhysicsRotation(transform.getRotation());
        }
    }

    @Override
    public void updateEntityPos() {
        attachedBodiesTransform.values().forEach(SynchronizedRigidBodyTransform::updatePos);
    }

    @Override
    public void drawParts(RenderPhysicsEntity<?> render, float partialTicks, BaseVehicleEntity<?> carEntity) {
        DoorsModule doorsModule = this;
        List<PartDoor> doors = carEntity.getPackInfo().getPartsByType(PartDoor.class);
        for (byte id = 0; id < doors.size(); id++) {
            PartDoor door = doors.get(id);
            if (!door.isEnabled()) {
                GlStateManager.pushMatrix();
                Vector3f pos = Vector3fPool.get().addLocal(door.getCarAttachPoint());
                pos.subtract(door.getDoorAttachPoint(), pos);

                /*GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(carEntity.prevRenderRotation, carEntity.renderRotation, partialTicks, true));
                GlStateManager.translate(
                        (float) -(carEntity.prevPosX + (carEntity.posX - carEntity.prevPosX) * partialTicks),
                        (float) -(carEntity.prevPosY + (carEntity.posY - carEntity.prevPosY) * partialTicks),
                        (float) -(carEntity.prevPosZ + (carEntity.posZ - carEntity.prevPosZ) * partialTicks));*/
                GlStateManager.translate(pos.x, pos.y, pos.z);

                ObjModelClient vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
                GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
                render.renderModelGroup(vehicleModel, door.getPartName(), carEntity, carEntity.getEntityTextureID());
                GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);

                GlStateManager.popMatrix();
            } else if (doorsModule.getTransforms().containsKey(id)) {
                SynchronizedRigidBodyTransform sync = doorsModule.getTransforms().get(id);
                RigidBodyTransform transform = sync.getTransform();
                RigidBodyTransform prev = sync.getPrevTransform();

                GlStateManager.pushMatrix();
                Vector3f pos = Vector3fPool.get(prev.getPosition()).addLocal(transform.getPosition().subtract(prev.getPosition(), Vector3fPool.get()).multLocal(partialTicks));

                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(carEntity.prevRenderRotation, carEntity.renderRotation, partialTicks, true));
                GlStateManager.translate(
                        (float) -(carEntity.prevPosX + (carEntity.posX - carEntity.prevPosX) * partialTicks),
                        (float) -(carEntity.prevPosY + (carEntity.posY - carEntity.prevPosY) * partialTicks),
                        (float) -(carEntity.prevPosZ + (carEntity.posZ - carEntity.prevPosZ) * partialTicks));
                GlStateManager.translate(pos.x, pos.y, pos.z);
                GlStateManager.rotate(ClientDynamXUtils.computeInterpolatedGlQuaternion(prev.getRotation(), transform.getRotation(), partialTicks));

                ObjModelClient vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
                GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);
                render.renderModelGroup(vehicleModel, door.getPartName(), carEntity, carEntity.getEntityTextureID());
                GlStateManager.scale(1 / carEntity.getPackInfo().getScaleModifier().x, 1 / carEntity.getPackInfo().getScaleModifier().y, 1 / carEntity.getPackInfo().getScaleModifier().z);

                GlStateManager.popMatrix();
            }
        }
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class DoorContainer {
        private DoorsModule module;
        private byte doorID;
    }

    public static class DoorPhysicsHandler {
        private final PhysicsRigidBody body;
        private final HingeJoint joint;
        private int timer;

        public DoorPhysicsHandler(PhysicsRigidBody body, HingeJoint joint) {
            this.body = body;
            this.joint = joint;
        }

        public PhysicsRigidBody getBody() {
            return body;
        }

        public HingeJoint getJoint() {
            return joint;
        }

        public int getTimer() {
            return timer;
        }

        public void setTimer(int timer) {
            this.timer = timer;
        }
    }
}
